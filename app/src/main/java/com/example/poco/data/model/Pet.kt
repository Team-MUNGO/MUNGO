package com.example.poco.data.model

/** 등록된 반려견 */
data class Pet(
    val id: Long = 0,
    val name: String,
    val breed: String,
    val ageYears: Int?,
    val weightKg: Float?,
    val seat: PetSeat,
    val motionSensitivity: MotionSensitivity,
    /** 음성 안내 빈도 - 설정 화면에서 바꿈 */
    val voiceFrequency: VoiceFrequency = VoiceFrequency.NORMAL,
    /** 이만큼(%) 열리면 창문 알림 (30km/h 이상 주행 중) */
    val windowAlertPercent: Int = DEFAULT_WINDOW_ALERT_PERCENT,
    /** 앱 내부 저장소에 복사해 둔 사진 파일 경로. 없으면 기본 캐릭터 */
    val photoPath: String? = null,
    /** 고정 방식: 이동장 · 하네스면 같은 흔들림에도 덜 쏠림 */
    val restraint: Restraint = Restraint.NONE
) {
    val size: PetSize? get() = weightKg?.let(PetSize::fromWeight)

    companion object {
        const val DEFAULT_WINDOW_ALERT_PERCENT = 20
        val WINDOW_ALERT_OPTIONS = listOf(10, 20, 30, 50)
    }
}

/** 탑승 자리 - 어느 창문이 "반려견 옆"인지 정하는 기준 */
enum class PetSeat {
    REAR_LEFT,
    REAR_RIGHT,
    PASSENGER
}

/** 멀미 민감도 = 흔들림 기준점 배율 (POCO 기준점 ③). 높음이면 더 작은 흔들림부터 알린다 */
enum class MotionSensitivity(val thresholdFactor: Float) {
    LOW(1.2f),
    NORMAL(1.0f),
    HIGH(0.8f)
}

/** 고정 방식 (POCO 기준점 ③): 몸이 고정돼 있으면 같은 흔들림에도 덜 쏠린다 */
enum class Restraint(val thresholdFactor: Float) {
    /** 좌석에 그냥 앉거나 안고 탐 */
    NONE(1.0f),

    /** 이동장 · 카시트 · 하네스로 고정 */
    CRATE_HARNESS(1.2f)
}

/** 음성 안내 빈도 */
enum class VoiceFrequency {
    LOW,
    NORMAL,
    HIGH
}

/**
 * 몸무게로 자동 분류되는 크기.
 * 소형 10kg 미만 · 중형 10~25kg 미만 · 대형 25kg 이상.
 * 온도 · 흔들림 기준은 domain/driving/PocoStandard (POCO 기준점)에서 정한다
 */
enum class PetSize {
    SMALL,
    MEDIUM,
    LARGE;

    companion object {
        const val SMALL_MAX_KG = 10f
        const val MEDIUM_MAX_KG = 25f

        fun fromWeight(weightKg: Float): PetSize = when {
            weightKg < SMALL_MAX_KG -> SMALL
            weightKg < MEDIUM_MAX_KG -> MEDIUM
            else -> LARGE
        }
    }
}
