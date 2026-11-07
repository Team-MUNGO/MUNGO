package com.example.poco.data.pleos.datasource

import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import kotlinx.coroutines.flow.Flow

/**
 * Pleos Window API를 코루틴/Flow 형태로 감싼 DataSource.
 * SDK 타입(WindowArea, AreaBounds)은 data 레이어 안에서만 사용한다.
 */
interface WindowDataSource {
    /** 지원하는 창문 영역과 각 영역의 위치 범위(min~max) */
    suspend fun getWindowPositionCapabilities(): Map<WindowArea, AreaBounds<Int>>

    /** 현재 창문 위치 */
    suspend fun getCurrentWindowPosition(area: WindowArea): Int?

    /** 창문 위치 설정. 성공 시 설정된 위치 반환 */
    suspend fun setWindowPosition(area: WindowArea, position: Int): Int?

    /** 창문 위치 변경 감지. collect 시 리스너 등록, 취소 시 해제 */
    fun observeWindowPosition(area: WindowArea): Flow<Int?>
}
