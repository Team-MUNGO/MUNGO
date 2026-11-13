package com.example.poco.presentation.home

import com.example.poco.core.driving.RestIntervalPolicy
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
    /** 이번 주(월~일) 요일별 이동 횟수. 7칸 */
    val weekTripCounts: List<Int> = List(7) { 0 },
    val isVoiceGuideEnabled: Boolean = true,
    val isRearTempWatchEnabled: Boolean = true,
    /** 기어 P → D 전환 감지 → 주행 화면으로 자동 이동 */
    val shouldStartDriving: Boolean = false,
    /** 설정 > 차량 연동 관리에서 해지하면 false: 차량 값은 비우고 '다시 연결하기'를 보여 준다 */
    val isVehicleLinked: Boolean = true,
    /** 차가 알려 준 차종 (예: 그랜저). 연동 중에 한 번 읽어 두고, 해지 후에도 이름만 보여 준다 */
    val vehicleName: String? = null,
    /** 설정한 휴식 간격 (분) - 연동 해지 홈의 '휴식 추천 간격' 카드 */
    val restIntervalMinutes: Int = RestIntervalPolicy.DEFAULT_MINUTES
) {
    val mealStatus: CheckStatus
        get() = when {
            minutesSinceMeal == null -> CheckStatus.UNKNOWN
            minutesSinceMeal < MEAL_REST_MINUTES -> CheckStatus.WARNING
            else -> CheckStatus.OK
        }

    val weekTripTotal: Int get() = weekTripCounts.sum()

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
