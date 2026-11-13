package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.navi.constants.CautionPointType
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.CautionPoint
import ai.pleos.playground.navi.data.CurrentLocationInfo
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
import com.example.poco.data.pleos.datasource.NaviPlaceHint
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

    override fun observePlace() = callbackFlow<NaviPlaceHint> {
        // Each collection owns its connection; only the session repository collects it.
        val helper = NaviHelper(context)
        var address: String? = null
        var landmark: String? = null
        fun emitHint() {
            trySend(NaviPlaceHint(landmark, address, System.currentTimeMillis()))
        }
        val listener = object : NaviHelperEventListener {
            override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
                address = currentLocationInfo.address.trim().takeIf { it.isNotEmpty() }
                emitHint()
            }

            override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
                landmark = nearbyLandmark(tbtInfoList)
                emitHint()
            }
        }
        fun request() = runCatching {
            helper.getCurrentLocationInfo()
            helper.getTBTInfo()
        }.onFailure { Timber.w(it, "내비 위치 이름 요청 실패") }
        try {
            helper.addListener(listener)
            // 연결되자마자 한 번 묻고, 그 뒤로 주기적으로 (출발 직후 이벤트도 이름을 받게)
            helper.initialize(onConnected = {
                request()
                launch {
                    while (isActive) {
                        delay(PLACE_POLL_MS)
                        request()
                    }
                }
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
        const val PLACE_POLL_MS = 5_000L
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

/** IC · JC · 톨게이트 · 휴게소: 기록에 남길 만한 '지금 있는 곳' 이름 */
private val LANDMARK_TURNS = setOf(
    TurnType.TURN_ENTER_IC_R, TurnType.TURN_ENTER_IC_L, TurnType.TURN_ENTER_IC_C,
    TurnType.TURN_EXIT_IC_R, TurnType.TURN_EXIT_IC_L, TurnType.TURN_EXIT_IC_C,
    TurnType.TURN_TOLL, TurnType.TURN_RESTAREA, TurnType.TURN_ENTER_SERVICEAREA
)

/** 이만큼 가까운 안내 지점이면 '그 근처에서 일어났다'고 본다 */
internal const val LANDMARK_NEAR_METERS = 300.0

/** 가장 가까운 300m 안쪽 랜드마크 이름. 방향 안내("원주,인천 방면")는 쓰지 않는다 */
internal fun nearbyLandmark(turns: List<TBTInfo>): String? = turns
    .filter { it.distance in 0.0..LANDMARK_NEAR_METERS }
    .sortedBy { it.distance }
    .firstNotNullOfOrNull { landmarkName(it.type, it.description) }

/**
 * "판교IC - 부산 방면" → "판교IC", "서울톨게이트" → "서울톨게이트".
 * IC · 톨게이트 · 휴게소 안내이거나, 이름이 JC로 끝날 때만 (JC는 차로 유지 안내로 온다)
 */
internal fun landmarkName(type: TurnType, description: String): String? {
    val name = description.substringBefore(" - ").removeSuffix("방면").trim()
    if (name.isEmpty() || ',' in name) return null
    return name.takeIf { type in LANDMARK_TURNS || name.endsWith("JC") }
}
