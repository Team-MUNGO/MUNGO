package com.example.poco.presentation.home

import com.example.poco.data.model.Pet
import com.example.poco.data.model.TripSummary
import com.example.poco.domain.driving.PetType
import com.example.poco.domain.driving.PocoStandard
import com.example.poco.domain.driving.temperatureStandard

data class HomeUiState(
    /** 오늘의 동승자 */
    val pet: Pet? = null,
    /** null = 기어 상태 모름 */
    val isParked: Boolean? = null,
    /** 마지막 식사 후 지난 분. 기록이 없으면 null */
    val minutesSinceMeal: Long? = null,
    /** 반려동물 자리 설정 온도. 못 읽으면 null */
    val seatTemperature: Float? = null,
    /** 모든 창문이 닫혔는지. 못 읽으면 null */
    val isAllWindowsClosed: Boolean? = null,
    val lastTrip: TripSummary? = null,
    val isVoiceGuideEnabled: Boolean = true,
    val isRearTempWatchEnabled: Boolean = true,
    /** 기어 P → D 전환 감지 → 주행 화면으로 자동 이동 */
    val shouldStartDriving: Boolean = false
) {
    val mealStatus: CheckStatus
        get() = when {
            minutesSinceMeal == null -> CheckStatus.UNKNOWN
            minutesSinceMeal < MEAL_REST_MINUTES -> CheckStatus.WARNING
            else -> CheckStatus.OK
        }

    val temperatureStatus: CheckStatus
        get() {
            val temperature = seatTemperature ?: return CheckStatus.UNKNOWN
            // POCO 기준점 ①: 견종 유형별 더움 · 추움 기준 (펫이 없으면 기본 유형)
            val standard = pet?.temperatureStandard
                ?: PocoStandard.temperature(setOf(PetType.BASIC))
            val isOff = standard.isHot(temperature) || standard.isCold(temperature)
            return if (isOff) CheckStatus.WARNING else CheckStatus.OK
        }

    val windowStatus: CheckStatus
        get() = when (isAllWindowsClosed) {
            null -> CheckStatus.UNKNOWN
            true -> CheckStatus.OK
            false -> CheckStatus.WARNING
        }

    companion object {
        /** 전문가 권고: 이동 전 1~2시간 공복 */
        const val MEAL_REST_MINUTES = 60L
    }
}

enum class CheckStatus {
    OK,
    WARNING,
    UNKNOWN
}
