package com.example.poco.presentation.petmode

import com.example.poco.core.util.aYa
import com.example.poco.data.model.Pet
import com.example.poco.domain.driving.PocoStandard
import com.example.poco.domain.driving.TemperatureStandard
import kotlin.math.roundToInt

/**
 * 펫모드: 보호자가 잠깐 내린 사이 차 안의 반려견을 지킨다 (정차 중).
 * 화면은 차 밖에서 들여다보는 사람과 반려견에게 보여 주는 용도
 */
data class PetModeUiState(
    val pet: Pet? = null,
    val startedAtMillis: Long = 0L,
    val nowMillis: Long = 0L,
    /** 실내 온도 (못 읽으면 설정 온도, 둘 다 없으면 null) */
    val temperature: Float? = null,
    /** 펫모드를 켤 때 맞춰 둔 설정 온도. 못 바꿨으면 null */
    val targetTemperature: Float? = null,
    /** 반려견 옆 창문이 닫혔는지. 못 읽으면 null */
    val isWindowClosed: Boolean? = null,
    val batteryPercent: Int? = null,
    /** 경고(20%)까지 남은 시간. 아직 계산 못 했으면 null */
    val batteryHoursLeft: Float? = null,
    /** 다음 안심 한마디를 들려줄 시각 */
    val nextVoiceAtMillis: Long = 0L,
    val isSpeaking: Boolean = false,
    /** 펫모드 끝 → 홈으로 */
    val isDone: Boolean = false
) {
    val elapsedMinutes: Int
        get() = ((nowMillis - startedAtMillis) / MINUTE_MS).toInt().coerceAtLeast(0)

    val minutesToNextVoice: Int
        get() {
            val leftMs = (nextVoiceAtMillis - nowMillis).coerceAtLeast(0)
            return ((leftMs + MINUTE_MS - 1) / MINUTE_MS).toInt()
        }

    val standard: TemperatureStandard
        get() = PocoStandard.temperature(
            pet?.let { PocoStandard.petTypes(it.breed, it.weightKg, it.ageYears) }.orEmpty()
        )

    val comfort: TemperatureComfort
        get() = when {
            temperature == null -> TemperatureComfort.UNKNOWN
            standard.isHot(temperature) -> TemperatureComfort.HOT
            standard.isCold(temperature) -> TemperatureComfort.COLD
            else -> TemperatureComfort.OK
        }

    val voiceMessage: String get() = petModeMessage(pet?.name)

    val isBatteryLow: Boolean get() = (batteryPercent ?: 100) <= BATTERY_ALERT_PERCENT

    companion object {
        const val MINUTE_MS = 60_000L

        /** 이만큼 아래로 떨어지면 경고 */
        const val BATTERY_ALERT_PERCENT = 20

        /** 안심 한마디 간격 */
        const val VOICE_INTERVAL_MS = 10 * MINUTE_MS
    }
}

enum class TemperatureComfort { OK, HOT, COLD, UNKNOWN }

/** "현댕아, 금방 올게" */
fun petModeMessage(name: String?): String =
    if (name.isNullOrBlank()) "금방 올게" else "${name.trim().aYa}, 금방 올게"

/** 펫모드를 켤 때 맞출 온도: 쾌적 범위 가운데 (0.5도 단위) */
fun petModeTargetTemperature(standard: TemperatureStandard): Float {
    val middle = (standard.comfort.start + standard.comfort.endInclusive) / 2f
    return (middle * 2).roundToInt() / 2f
}

/**
 * 배터리가 [floorPercent]까지 내려가는 데 남은 시간(시간).
 * 펫모드를 켠 뒤 1% 이상 줄어야 계산한다 (그 전엔 null)
 */
fun batteryHoursLeft(
    startPercent: Int,
    startMillis: Long,
    nowPercent: Int,
    nowMillis: Long,
    floorPercent: Int = PetModeUiState.BATTERY_ALERT_PERCENT
): Float? {
    val used = startPercent - nowPercent
    val hours = (nowMillis - startMillis) / 3_600_000f
    if (used < 1 || hours <= 0f) return null
    val left = (nowPercent - floorPercent).coerceAtLeast(0)
    return left / (used / hours)
}
