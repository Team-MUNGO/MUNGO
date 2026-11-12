package com.example.poco.data.pleos.datasource

import com.example.poco.core.driving.NaviWaypoint
import com.example.poco.core.driving.Reading
import com.example.poco.data.model.NaviEvent
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
    /**
     * NaviHelper SDK 안내 이벤트. 경로 안내 중에만 의미 있는 값이 온다.
     * 회전 안내(TBT)는 주기적으로 다시 요청한다.
     */
    fun observeEvents(): Flow<NaviEvent>

    /** Pleos 내비 화면 열기. [keyword]가 있으면 그 말로 장소 검색, 없으면 내비를 앞으로 */
    suspend fun openNavigation(keyword: String?)

    /** 주행 세션용: 앞쪽 과속방지턱 (통과 시점 감점에 사용) */
    fun observeBumps(): Flow<Reading<List<BumpApproach>>>

    /** 주행 세션용: 경유지 · 경유지 도착 · 경로 종료 */
    fun observeRoute(): Flow<NaviRouteUpdate>
}
