package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.navi.constants.CautionPointType
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.data.WaypointInfo
import com.example.poco.core.driving.NaviWaypoint
import com.example.poco.data.pleos.datasource.NaviRouteUpdate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ai.pleos.playground.navi.data.CautionPoint
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import com.example.poco.core.driving.Reading
import com.example.poco.data.pleos.datasource.BumpApproach
import com.example.poco.data.pleos.datasource.NaviDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class NaviDataSourceImpl @Inject constructor(@ApplicationContext private val context: Context) :
    NaviDataSource {
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
                            NaviWaypoint(it.name, it.latitude, it.longitude, it.remainTime, it.remainDistance)
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
}
