package com.example.poco.data.repository

import com.example.poco.data.model.NaviEvent
import kotlinx.coroutines.flow.Flow

/** NaviHelper SDK: 목적지 · 회전 안내 · 주의 구간 · 길 안내 시작/도착 */
interface NaviRepository {
    fun observeEvents(): Flow<NaviEvent>

    /** Pleos 내비 화면 열기 (목적지 설정용). [keyword]가 있으면 장소 검색 */
    suspend fun openNavigation(keyword: String? = null): Result<Unit>
}
