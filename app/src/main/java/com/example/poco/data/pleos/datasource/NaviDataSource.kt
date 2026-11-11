package com.example.poco.data.pleos.datasource

import com.example.poco.data.model.NaviEvent
import kotlinx.coroutines.flow.Flow

interface NaviDataSource {
    /**
     * NaviHelper SDK 안내 이벤트. 경로 안내 중에만 의미 있는 값이 온다.
     * 회전 안내(TBT)는 주기적으로 다시 요청한다.
     */
    fun observeEvents(): Flow<NaviEvent>

    /** Pleos 내비 화면 열기. [keyword]가 있으면 그 말로 장소 검색, 없으면 내비를 앞으로 */
    suspend fun openNavigation(keyword: String?)
}
