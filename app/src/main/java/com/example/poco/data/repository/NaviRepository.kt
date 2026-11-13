package com.example.poco.data.repository

import com.example.poco.core.driving.Reading
import com.example.poco.data.model.NaviEvent
import com.example.poco.data.pleos.datasource.BumpApproach
import com.example.poco.data.pleos.datasource.NaviRouteUpdate
import kotlinx.coroutines.flow.Flow

/** NaviHelper SDK: 목적지 · 회전 안내 · 주의 구간 · 길 안내 시작/도착 */
interface NaviRepository {
    fun observeEvents(): Flow<NaviEvent>

    /** Pleos 내비 화면 열기 (목적지 설정용). [keyword]가 있으면 장소 검색 */
    suspend fun openNavigation(keyword: String? = null): Result<Unit>

    /** 주행 세션용 과속방지턱 접근 */
    fun observeBumps(): Flow<Reading<List<BumpApproach>>>

    /** 주행 세션용 경유지 · 도착 */
    fun observeRoute(): Flow<NaviRouteUpdate>
}
