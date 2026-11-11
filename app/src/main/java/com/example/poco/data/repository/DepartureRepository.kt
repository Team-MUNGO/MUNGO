package com.example.poco.data.repository

import kotlinx.coroutines.flow.Flow

/** 출발 준비 · 이동 설정: 마지막 식사 시각, 음성 안내, 온도 지켜보기와 기준 */
interface DepartureRepository {
    /** 마지막 식사 시각 (epoch millis). 기록이 없으면 null */
    val lastMealAt: Flow<Long?>
    val isVoiceGuideEnabled: Flow<Boolean>
    val isRearTempWatchEnabled: Flow<Boolean>

    /** 이 범위를 벗어나면(경계 포함) 온도 알림. 기본 18~26°C */
    val temperatureAlertRange: Flow<ClosedFloatingPointRange<Float>>

    suspend fun recordMeal(atMillis: Long): Result<Unit>

    suspend fun setVoiceGuideEnabled(enabled: Boolean): Result<Unit>

    suspend fun setRearTempWatchEnabled(enabled: Boolean): Result<Unit>

    suspend fun setTemperatureAlertRange(range: ClosedFloatingPointRange<Float>): Result<Unit>
}
