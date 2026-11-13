package com.example.poco.core.driving

import com.example.poco.data.model.CarWindow

/** All time values in the driving engine use a monotonic clock, in milliseconds. */
fun interface DrivingClock {
    fun now(): Long
}

sealed interface Reading<out T> {
    data object Unknown : Reading<Nothing>
    data class Value<T>(val value: T) : Reading<T>
    data class Unavailable(val reason: String) : Reading<Nothing>
}

fun <T> Reading<T>.valueOrNull(): T? = (this as? Reading.Value<T>)?.value

enum class PetSeat(val label: String, val window: CarWindow) {
    PASSENGER("앞좌석 조수석", CarWindow.PASSENGER),
    REAR_LEFT("뒷좌석 왼쪽", CarWindow.REAR_LEFT),
    REAR_RIGHT("뒷좌석 오른쪽", CarWindow.REAR_RIGHT)
}

data class DrivingSettings(
    val petName: String,
    val seat: PetSeat,
    val minTemperatureC: Float,
    val maxTemperatureC: Float,
    /** Body weight in 0.1 kg units; null when the user has not entered it (size then unknown). */
    val weightTenths: Int? = null,
    /** 설정 > 알림 '창문 안전 알림'. 끄면 창문이 열려도 경고·음성 안내를 하지 않는다 */
    val windowAlerts: Boolean = true,
    /** 설정 > 알림 '휴식 쉼터 제안'. 끄면 휴식 20분 전 쉼터 제안을 받지 않는다 */
    val restSuggestions: Boolean = true
) {
    val petSize: PetSize? get() = weightTenths?.let(::petSizeFor)

    init {
        require(petName.isNotBlank())
        require(weightTenths == null || weightTenths in 1..1500)
        require(minTemperatureC.isFinite() && maxTemperatureC.isFinite())
        require(minTemperatureC <= maxTemperatureC)
    }
}

enum class CabinTemperatureStatus { UNKNOWN, COLD, NORMAL, HOT }

fun temperatureStatus(value: Float?, settings: DrivingSettings): CabinTemperatureStatus = when {
    value == null || !value.isFinite() -> CabinTemperatureStatus.UNKNOWN
    value < settings.minTemperatureC -> CabinTemperatureStatus.COLD
    value > settings.maxTemperatureC -> CabinTemperatureStatus.HOT
    else -> CabinTemperatureStatus.NORMAL
}

enum class DrivingGear { PARK, DRIVE, REVERSE, NEUTRAL }

data class SpeedSample(val kmh: Double, val at: Long)

sealed interface DrivingEvent {
    data object RapidAcceleration : DrivingEvent
    data class RapidBraking(val penalty: Int) : DrivingEvent
    data object SharpTurn : DrivingEvent
    data object RepeatedTurns : DrivingEvent
    data class SpeedBump(val penalty: Int) : DrivingEvent
}

/** mungo policy, not a certified safety assessment. */
data class ComfortScoreConfig(
    val accelerationMin: Double = 11.0,
    val accelerationMax: Double = 25.0,
    val accelerationPenalty: Int = 3,
    val accelerationMergeMs: Long = 3_000,
    val brakingMin: Double = 7.5,
    val brakingStrong: Double = 11.0,
    val brakingMax: Double = 40.0,
    val brakingPenalty: Int = 5,
    val brakingStrongPenalty: Int = 6,
    val brakingMergeMs: Long = 1_000,
    val speedWindowMs: Long = 1_000,
    val maxSampleGapMs: Long = 2_000,
    val turnMinSpeed: Double = 15.0,
    val sharpTurnMin: Double = 60.0,
    val sharpTurnMax: Double = 120.0,
    val turnWindowMs: Long = 2_000,
    // Steering-wheel sharp turn: lateral ≈ v² · tan(wheel angle) / wheelbase (bicycle model), the same
    // estimate and MungoStandard lateral thresholds (EVENT 2.0 / MILD 1.5 m/s²) as ComfortTracker.
    val steeringRatio: Double = 14.0,
    val wheelbaseM: Double = 3.0,
    val sharpTurnLateralMps2: Double = 2.0,
    val turnRearmLateralMps2: Double = 1.5,
    val steeringHoldMs: Long = 300,
    /** One turn seen by both steering and GPS heading is counted once. */
    val sharpTurnDedupMs: Long = 4_000,
    val sharpTurnPenalty: Int = 4,
    // Provisional mungo tuning: a turn ends after 1s without >=3°/s movement.
    val curveMinAngle: Double = 15.0,
    val curveMinRate: Double = 3.0,
    val curveEndQuietMs: Long = 1_000,
    val repeatedTurnWindowMs: Long = 60_000,
    val repeatedTurnCount: Int = 3,
    val repeatedTurnPenalty: Int = 5,
    val bumpLightSpeed: Double = 30.0,
    val bumpHeavySpeed: Double = 40.0,
    val bumpLightPenalty: Int = 1,
    val bumpHeavyPenalty: Int = 2,
    val recoveryStepMs: Long = 180_000,
    val recoveryCooldownMs: Long = 180_000,
    val recoveryCap: Int = 90,
    val validRestMs: Long = 180_000,
    val windowWarningPercent: Double = 20.0,
    val closeTimeoutMs: Long = 12_000,
    val sensorStaleMs: Long = 3_000
)

data class ComfortScoreState(
    val score: Int = 100,
    val lastPenaltyAt: Long? = null,
    val hasRested: Boolean = false,
    val accelerationCount: Int = 0,
    val brakingCount: Int = 0,
    val turnCount: Int = 0,
    val repeatedTurnCount: Int = 0,
    val bumpCount: Int = 0
)
