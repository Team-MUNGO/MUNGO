package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.navi.constants.CautionPointType
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.CautionPoint
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import com.example.poco.core.driving.NaviWaypoint
import com.example.poco.core.driving.Reading
import com.example.poco.data.model.CautionKind
import com.example.poco.data.model.NaviEvent
import com.example.poco.data.model.TurnKind
import com.example.poco.data.model.UpcomingCaution
import com.example.poco.data.model.UpcomingTurn
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.BumpApproach
import com.example.poco.data.pleos.datasource.NaviDataSource
import com.example.poco.data.pleos.datasource.NaviRouteUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class NaviDataSourceImpl @Inject constructor(
    private val naviHelper: NaviHelper,
    private val sdkManager: PleosSdkManager,
    @ApplicationContext private val context: Context
) : NaviDataSource {
    override fun observeEvents(): Flow<NaviEvent> = callbackFlow {
        sdkManager.ensureNavi()
        val listener =
            object : NaviHelperEventListener {
                override fun onTBTInfo(tbtInfo: List<TBTInfo>) {
                    trySend(NaviEvent.UpcomingTurns(tbtInfo.map { it.toUpcomingTurn() }))
                }

                override fun onCautionPointInfo(cautionPoints: List<CautionPoint>) {
                    trySend(
                        NaviEvent.UpcomingCautions(
                            cautionPoints.map {
                                it.toUpcomingCaution()
                            }
                        )
                    )
                }

                override fun onDestinationInfo(destinationInfo: DestinationInfo) {
                    trySend(
                        NaviEvent.Destination(
                            name = destinationInfo.name,
                            remainDistanceM = destinationInfo.remainDistance,
                            remainTimeSec = destinationInfo.remainTime
                        )
                    )
                }

                override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
                    trySend(
                        NaviEvent.Arrived(
                            name = destinationArrivedInfo.name,
                            distanceM = destinationArrivedInfo.distance,
                            durationSec = destinationArrivedInfo.duration
                        )
                    )
                }

                override fun onCurrentRoadSpeedLimit(speedLimit: Int) {
                    trySend(NaviEvent.SpeedLimit(speedLimit))
                }

                override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
                    trySend(
                        NaviEvent.RouteStarted(
                            name = routeStartInfo.name,
                            distanceM = routeStartInfo.distance,
                            durationSec = routeStartInfo.duration
                        )
                    )
                    // 새 목적지 · 남은 거리를 바로 받아 둔다
                    runCatching { naviHelper.getDestinationInfo() }
                }

                override fun onRouteEnded() {
                    trySend(NaviEvent.RouteEnded)
                }

                override fun onRouteCancelled() {
                    trySend(NaviEvent.RouteCancelled)
                }

                override fun onError(errorCode: NaviErrorCode) {
                    Timber.w("NaviHelper 오류: $errorCode")
                }
            }
        naviHelper.addListener(listener)
        naviHelper.getDestinationInfo()

        // 회전 안내 · 남은 거리는 계속 바뀌므로 주기적으로 다시 요청
        launch {
            while (isActive) {
                runCatching { naviHelper.getTBTInfo() }.onFailure { Timber.w(it, "TBT 요청 실패") }
                delay(TBT_POLL_MS)
            }
        }
        launch {
            while (isActive) {
                delay(DESTINATION_POLL_MS)
                runCatching { naviHelper.getDestinationInfo() }
                    .onFailure { Timber.w(it, "목적지 요청 실패") }
            }
        }
        awaitClose { naviHelper.removeListener(listener) }
    }

    override suspend fun openNavigation(keyword: String?) {
        sdkManager.ensureNavi()
        val request = if (keyword.isNullOrBlank()) {
            NavigationScreenRequest.Foreground
        } else {
            NavigationScreenRequest.SearchPlace(keyword)
        }
        naviHelper.requestNavigationScreen(request)
    }

    override fun observeBumps() = callbackFlow<Reading<List<BumpApproach>>> {
        // Each collection owns its connection; only the session repository collects it.
        val helper = NaviHelper(context)
        val listener = object : NaviHelperEventListener {
            override fun onCautionPointInfo(cautionPointList: List<CautionPoint>) {
                trySend(
                    Reading.Value(
                        cautionPointList.filter {
                            it.type ==
                                CautionPointType.SPEED_BUMP &&
                                it.remainDistance >= 0
                        }
                            .map { BumpApproach(it.id, it.remainDistance) }
                    )
                )
            }
        }
        try {
            helper.addListener(listener)
            helper.initialize(onConnected = {
                trySend(Reading.Value(emptyList()))
            }, onFailed = { close(it) })
            awaitClose { }
        } finally {
            helper.removeListener(listener)
            helper.release()
        }
    }

    override fun observeRoute() = callbackFlow<NaviRouteUpdate> {
        val helper = NaviHelper(context)
        val listener = object : NaviHelperEventListener {
            override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) {
                trySend(
                    NaviRouteUpdate.Waypoints(
                        waypointInfoList.map {
                            NaviWaypoint(
                                it.name,
                                it.latitude,
                                it.longitude,
                                it.remainTime,
                                it.remainDistance
                            )
                        }
                    )
                )
            }
            override fun onWayPointArrived(waypointArrivedInfo: WaypointArrivedInfo) {
                trySend(
                    NaviRouteUpdate.Arrived(
                        waypointArrivedInfo.name,
                        waypointArrivedInfo.index == WaypointIndex.FIRST
                    )
                )
            }
            override fun onRouteEnded() {
                trySend(NaviRouteUpdate.RouteEnded)
            }
            override fun onRouteCancelled() {
                trySend(NaviRouteUpdate.RouteEnded)
            }
        }
        try {
            helper.addListener(listener)
            helper.initialize(onConnected = {
                // Waypoint ETA is only available as a readback; poll while someone is collecting.
                launch {
                    while (true) {
                        runCatching { helper.getWaypointInfo() }
                        delay(5_000)
                    }
                }
            }, onFailed = { close(it) })
            awaitClose { }
        } finally {
            helper.removeListener(listener)
            helper.release()
        }
    }

    private companion object {
        const val TBT_POLL_MS = 2_000L
        const val DESTINATION_POLL_MS = 10_000L
    }
}

private fun TBTInfo.toUpcomingTurn() = UpcomingTurn(
    kind = when (type) {
        TurnType.TURN_SHARP_R, TurnType.TURN_SHARP_L, TurnType.TURN_UTURN,
        TurnType.TURN_ENTER_ROUNDABOUT_02, TurnType.TURN_ENTER_ROUNDABOUT_03,
        TurnType.TURN_ENTER_ROUNDABOUT_04, TurnType.TURN_ENTER_ROUNDABOUT_06,
        TurnType.TURN_ENTER_ROUNDABOUT_08, TurnType.TURN_ENTER_ROUNDABOUT_09,
        TurnType.TURN_ENTER_ROUNDABOUT_10, TurnType.TURN_ENTER_ROUNDABOUT_12,
        TurnType.TURN_ENTER_IC_R, TurnType.TURN_ENTER_IC_L, TurnType.TURN_ENTER_IC_C,
        TurnType.TURN_EXIT_IC_R, TurnType.TURN_EXIT_IC_L, TurnType.TURN_EXIT_IC_C -> TurnKind.SHARP

        TurnType.TURN_RIGHT, TurnType.TURN_LEFT,
        TurnType.TURN_KEEP_R, TurnType.TURN_KEEP_L -> TurnKind.NORMAL

        TurnType.TURN_RESTAREA, TurnType.TURN_ENTER_SERVICEAREA -> TurnKind.REST_AREA
        else -> TurnKind.OTHER
    },
    distanceM = distance.toInt(),
    description = description
)

private fun CautionPoint.toUpcomingCaution() = UpcomingCaution(
    kind = when (type) {
        CautionPointType.SPEED_BUMP -> CautionKind.SPEED_BUMP
        CautionPointType.SCHOOL_ZONE -> CautionKind.SCHOOL_ZONE
        CautionPointType.DOWNHILL_CAUTION_AREA -> CautionKind.DOWNHILL
        CautionPointType.NARROW_ROAD -> CautionKind.NARROW_ROAD
        CautionPointType.SLIPPERY_ROAD_ALERT -> CautionKind.SLIPPERY
        else -> CautionKind.OTHER
    },
    distanceM = remainDistance
)
