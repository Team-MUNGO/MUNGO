package com.example.poco.data.pleos.datasource

import com.example.poco.core.driving.NaviWaypoint
import com.example.poco.core.driving.Reading
import kotlinx.coroutines.flow.Flow

data class BumpApproach(val id: String, val remainingMeters: Int)

sealed interface NaviRouteUpdate {
    /** Actual waypoints reported by the navigation app (polled readback). */
    data class Waypoints(val list: List<NaviWaypoint>) : NaviRouteUpdate
    /** NaviHelper onWayPointArrived; [first] is true for WaypointIndex.FIRST. */
    data class Arrived(val name: String, val first: Boolean) : NaviRouteUpdate
    data object RouteEnded : NaviRouteUpdate
}

interface NaviDataSource {
    fun observeBumps(): Flow<Reading<List<BumpApproach>>>
    fun observeRoute(): Flow<NaviRouteUpdate>
}
