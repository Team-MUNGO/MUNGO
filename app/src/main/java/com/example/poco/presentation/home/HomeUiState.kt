package com.example.poco.presentation.home

import com.example.poco.data.model.CarWindow
import com.example.poco.data.model.WindowPositionRange
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

data class HomeUiState(
    val isLoading: Boolean = false,
    /** 차량이 지원하는 창문과 각 창문의 위치 범위 */
    val supportedWindows: ImmutableMap<CarWindow, WindowPositionRange> = persistentMapOf(),
    /** 운전석 창문의 현재 위치 (실시간 갱신) */
    val driverWindowPosition: Int? = null,
    val errorMessage: String? = null
)
