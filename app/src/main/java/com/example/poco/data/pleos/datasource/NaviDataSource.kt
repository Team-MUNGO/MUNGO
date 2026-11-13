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

/**
 * 내비가 알려 주는 지금 위치 이름.
 * [landmark] = 300m 안쪽 IC · JC · 톨게이트 · 휴게소 이름 (길 안내 중일 때만), [address] = 구 · 동 이름
 */
data class NaviPlaceHint(val landmark: String?, val address: String?, val atEpoch: Long) {
    /** 꿀팁 · 기록에 쓸 이름: 랜드마크가 있으면 그것, 없으면 주소 */
    val name: String? get() = landmark ?: address
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

    /** 주행 세션용: 이벤트 기록에 붙일 지금 위치 이름 (주기적으로 내비에 묻는다) */
    fun observePlace(): Flow<NaviPlaceHint> = kotlinx.coroutines.flow.emptyFlow()
}
