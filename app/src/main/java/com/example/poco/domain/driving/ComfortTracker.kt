package com.example.poco.domain.driving

import com.example.poco.data.model.EventStat
import com.example.poco.data.model.ShakeLevel
import com.example.poco.data.model.TimelineBar
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.tan

enum class DrivingEventType {
    HARD_BRAKE,
    HARD_ACCEL,
    SHARP_TURN
}

/**
 * @param points 실제로 깎인 점수 (양수)
 * @param isAnticipated 내비가 미리 알려준 구간(과속방지턱 · 급커브 등)에서 생긴 사건 → 감점 절반
 * @param isDanger 4.0m/s² 이상 (mungo 기준점 '위험') → 정차 후 확인 안내
 */
data class DrivingEvent(
    val type: DrivingEventType,
    val atMillis: Long,
    val points: Float,
    val isAnticipated: Boolean = false,
    val isDanger: Boolean = false
)

/** 반려견 표정 4가지 */
enum class PetMood {
    CALM,
    HAPPY,
    DIZZY,
    HOT
}

/** 내비(NaviHelper)가 알려준 지금 도로 상황 */
data class RoadContext(
    /** 과속방지턱 · 어린이보호구역 · 내리막 등이 가까움 → 감속이 자연스러움 */
    val expectsBraking: Boolean = false,
    /** 급커브 · 유턴 · 회전교차로 · IC가 가까움 → 회전이 자연스러움 */
    val expectsTurning: Boolean = false
)

/**
 * 주행 중 펫 컴포트 점수 계산기 (안드로이드 의존성 없는 순수 로직 → 단위 테스트 가능).
 *
 * 입력 (Pleos SDK)
 * - 속도 (Android 표준 PERF_VEHICLE_SPEED) → 급제동 · 급가속
 * - 브레이크 페달 (Vehicle SDK · Brake) → 페달을 밟지 않은 감속(내리막 · 센서 튐)은 급제동에서 제외
 * - 조향각 (Vehicle SDK · SteeringWheel) → 급회전
 * - 진행 방향 (FusedLocation SDK) → 횡가속도(속도 × 회전 속도)로 급회전, 차종과 무관하게 판단
 * - 도로 상황 (NaviHelper SDK) → 예고된 구간의 사건은 감점 절반, 예고 후 부드럽게 지나가면 가점
 *
 * 판단 기준 = mungo 기준점 ② (앞뒤 1.5 / 2.5 / 4.0, 좌우 1.5 / 2.0 / 4.0 m/s²)
 * × [thresholdFactor] (mungo 기준점 ③: 멀미 민감도 × 크기 × 고정 방식)
 *
 * 점수
 * - 100점에서 시작
 * - 편안: 기록만 · 살짝 불편: 감점 없음(횟수만 셈)
 * - 불편 · 위험: 급제동 · 급가속 −5, 급회전 −3 / 온도 이탈 −3
 * - 사건 없이 5분 달리면 평온 회복 +2, 예고 구간 부드럽게 통과 +1 (최대 100)
 */
class ComfortTracker(private val startedAtMillis: Long, private val thresholdFactor: Float = 1f) {
    private val events = mutableListOf<DrivingEvent>()
    private var rawScore = MAX_SCORE.toFloat()
    private var recoveryCount = 0
    private var smoothPassCount = 0
    private var temperatureAlertCount = 0

    /** 살짝 불편(감점 없음) 횟수 - 리포트의 '조심할 곳' 후보 */
    var mildShakeCount = 0
        private set
    private val lastMildAt = mutableMapOf<Boolean, Long>()
    private var calmSinceMillis = startedAtMillis

    private var speedReference: Pair<Long, Float>? = null
    private var lastSpeedSample: Pair<Long, Float>? = null
    private var distanceKm = 0f
    private val lastEventAt = mutableMapOf<DrivingEventType, Long>()

    private var steeringArmed = true
    private var lateralArmed = true
    private var headingReference: Pair<Long, Float>? = null

    private var isBrakeSignalKnown = false
    private var lastBrakePressedAt: Long? = null

    var roadContext = RoadContext()

    val score: Int get() = rawScore.roundToInt().coerceIn(0, MAX_SCORE)
    val currentSpeedKmh: Float? get() = lastSpeedSample?.second

    /** 속도 샘플 → 급제동/급가속 감지. 감지되면 사건을 돌려준다 */
    fun onSpeed(nowMillis: Long, speedKmh: Float): DrivingEvent? {
        lastSpeedSample?.let { (t, v) ->
            val hours = (nowMillis - t) / 3_600_000f
            if (hours in 0f..MAX_INTEGRATION_HOURS) distanceKm += (v + speedKmh) / 2f * hours
        }
        lastSpeedSample = nowMillis to speedKmh

        val reference = speedReference
        if (reference == null) {
            speedReference = nowMillis to speedKmh
            return null
        }
        val dtSec = (nowMillis - reference.first) / 1000f
        if (dtSec < ACCEL_WINDOW_SEC) return null
        speedReference = nowMillis to speedKmh
        if (dtSec > MAX_ACCEL_WINDOW_SEC) return null

        val accelMps2 = (speedKmh - reference.second) / KMH_PER_MPS / dtSec
        val grade = PocoStandard.longitudinalGrade(accelMps2, thresholdFactor)
        val isBrake = accelMps2 < 0
        // 페달 없이 줄어든 속도(내리막 · 센서 튐)는 급제동으로 보지 않는다
        if (isBrake && !isBrakingNow(nowMillis)) return null
        return when (grade) {
            ShakeGrade.CALM -> null
            ShakeGrade.MILD -> onMild(nowMillis, lateral = false)
            ShakeGrade.EVENT, ShakeGrade.DANGER -> record(
                if (isBrake) DrivingEventType.HARD_BRAKE else DrivingEventType.HARD_ACCEL,
                nowMillis,
                anticipated = roadContext.expectsBraking,
                isDanger = grade == ShakeGrade.DANGER
            )
        }
    }

    /** 브레이크 페달 상태 (Vehicle SDK). 한 번이라도 들어오면 급제동은 페달을 밟았을 때만 센다 */
    fun onBrakePedal(nowMillis: Long, pressed: Boolean) {
        isBrakeSignalKnown = true
        if (pressed) lastBrakePressedAt = nowMillis
    }

    private fun isBrakingNow(nowMillis: Long): Boolean {
        if (!isBrakeSignalKnown) return true
        val pressedAt = lastBrakePressedAt ?: return false
        return nowMillis - pressedAt <= BRAKE_MATCH_WINDOW_MS
    }

    /**
     * 조향각 → 횡가속도 ≈ 속도² × tan(바퀴 각도) / 축간거리 (자전거 모델).
     * 한 번 감지하면 흔들림이 편안 단계로 돌아올 때까지 다시 세지 않는다
     */
    fun onSteering(nowMillis: Long, angleDeg: Float): DrivingEvent? {
        val speedMps = (currentSpeedKmh ?: return null) / KMH_PER_MPS
        val wheelRad = Math.toRadians((abs(angleDeg) / STEERING_RATIO).toDouble())
        val lateralMps2 = (speedMps * speedMps * tan(wheelRad) / WHEELBASE_M).toFloat()
        val grade = PocoStandard.lateralGrade(lateralMps2, thresholdFactor)
        if (grade == ShakeGrade.CALM) steeringArmed = true
        if (!steeringArmed || isTooSlowForTurn()) return null
        return onLateral(nowMillis, grade)?.also { steeringArmed = false }
    }

    /** 좌우 단계 → 살짝 불편이면 횟수만, 불편 · 위험이면 급회전 */
    private fun onLateral(nowMillis: Long, grade: ShakeGrade): DrivingEvent? = when (grade) {
        ShakeGrade.CALM -> null
        ShakeGrade.MILD -> onMild(nowMillis, lateral = true)
        ShakeGrade.EVENT, ShakeGrade.DANGER -> record(
            DrivingEventType.SHARP_TURN,
            nowMillis,
            anticipated = roadContext.expectsTurning,
            isDanger = grade == ShakeGrade.DANGER
        )
    }

    /** 살짝 불편: 감점 없이 횟수만 (같은 방향은 쿨다운) */
    private fun onMild(nowMillis: Long, lateral: Boolean): DrivingEvent? {
        val last = lastMildAt[lateral]
        if (last == null || nowMillis - last >= EVENT_COOLDOWN_MS) {
            lastMildAt[lateral] = nowMillis
            mildShakeCount++
        }
        return null
    }

    /**
     * 진행 방향 (FusedLocation) → 횡가속도 = 속도 × 회전 속도.
     * 조향각보다 실제로 몸이 쏠리는 정도에 가깝다.
     */
    fun onHeading(nowMillis: Long, headingDeg: Float): DrivingEvent? {
        val reference = headingReference
        headingReference = nowMillis to headingDeg
        if (reference == null) return null
        val dtSec = (nowMillis - reference.first) / 1000f
        if (dtSec < MIN_HEADING_WINDOW_SEC || dtSec > MAX_ACCEL_WINDOW_SEC) return null

        val yawRateRad =
            Math.toRadians(angleDiff(headingDeg, reference.second).toDouble()).toFloat() / dtSec
        val speedMps = (currentSpeedKmh ?: return null) / KMH_PER_MPS
        val lateralMps2 = abs(speedMps * yawRateRad)

        val grade = PocoStandard.lateralGrade(lateralMps2, thresholdFactor)
        if (grade == ShakeGrade.CALM) lateralArmed = true
        if (!lateralArmed || isTooSlowForTurn()) return null
        return onLateral(nowMillis, grade)?.also { lateralArmed = false }
    }

    private fun isTooSlowForTurn(): Boolean {
        val speed = currentSpeedKmh
        return speed != null && speed < TURN_MIN_SPEED_KMH
    }

    /** 내비가 예고한 구간을 사건 없이 지나감 → 가점 */
    fun onSmoothPass() {
        smoothPassCount++
        rawScore = (rawScore + SMOOTH_PASS_BONUS).coerceAtMost(MAX_SCORE.toFloat())
    }

    /** 온도 알림이 새로 떴을 때 한 번 */
    fun onTemperatureAlert() {
        temperatureAlertCount++
        rawScore -= TEMPERATURE_PENALTY
    }

    /** 주기적으로 호출. 평온 회복이 일어나면 true */
    fun onTick(nowMillis: Long): Boolean {
        if (nowMillis - calmSinceMillis < CALM_RECOVERY_MS) return false
        calmSinceMillis = nowMillis
        recoveryCount++
        rawScore = (rawScore + RECOVERY_BONUS).coerceAtMost(MAX_SCORE.toFloat())
        return true
    }

    private fun record(
        type: DrivingEventType,
        nowMillis: Long,
        anticipated: Boolean,
        isDanger: Boolean = false
    ): DrivingEvent? {
        val last = lastEventAt[type]
        if (last != null && nowMillis - last < EVENT_COOLDOWN_MS) return null
        lastEventAt[type] = nowMillis
        calmSinceMillis = nowMillis
        val points = type.penalty * if (anticipated) ANTICIPATED_FACTOR else 1f
        rawScore -= points
        return DrivingEvent(type, nowMillis, points, anticipated, isDanger).also { events += it }
    }

    /** 마지막 사건이 [sinceMillis] 이후에 있었는지 (예고 구간 통과 판정용) */
    fun hasEventSince(sinceMillis: Long): Boolean = events.any { it.atMillis >= sinceMillis }

    fun count(type: DrivingEventType): Int = events.count { it.type == type }

    fun stat(type: DrivingEventType): EventStat {
        val matching = events.filter { it.type == type }
        // 점수(100 − 감점)와 같은 방향으로 반올림되도록 음수 상태에서 반올림
        val points = (-matching.sumOf { it.points.toDouble() }).roundToInt()
        return EventStat(matching.size, points)
    }

    /** 평온 회복 + 예고 구간 부드러운 통과 (모두 가점) */
    fun recoveryStat() = EventStat(
        recoveryCount + smoothPassCount,
        recoveryCount * RECOVERY_BONUS + smoothPassCount * SMOOTH_PASS_BONUS
    )

    fun temperatureStat() = EventStat(
        temperatureAlertCount,
        -(temperatureAlertCount * TEMPERATURE_PENALTY).roundToInt()
    )

    val totalDistanceKm: Float get() = distanceKm

    fun recentEvents(nowMillis: Long, windowMillis: Long = MOOD_WINDOW_MS): List<DrivingEvent> =
        events.filter { nowMillis - it.atMillis <= windowMillis }

    /** 최근 2분 사건으로 표정 결정. 온도 이탈 중이면 더위가 우선 */
    fun mood(nowMillis: Long, isTemperatureOff: Boolean): PetMood = when {
        isTemperatureOff -> PetMood.HOT
        recentEvents(nowMillis).isNotEmpty() -> PetMood.DIZZY
        nowMillis - calmSinceMillis >= HAPPY_AFTER_CALM_MS -> PetMood.HAPPY
        else -> PetMood.CALM
    }

    /** 사건이 없었던 '분'의 수 */
    fun calmMinutes(nowMillis: Long): Int {
        val totalMinutes = ((nowMillis - startedAtMillis) / 60_000L).toInt()
        val eventMinutes = events.map {
            ((it.atMillis - startedAtMillis) / 60_000L).toInt()
        }.toSet()
        return (totalMinutes - eventMinutes.count { it < totalMinutes }).coerceAtLeast(0)
    }

    /** 출발~지금을 [bars]칸으로 나눈 흔들림 타임라인 */
    fun timeline(nowMillis: Long, bars: Int = TIMELINE_BARS): List<TimelineBar> {
        val duration = (nowMillis - startedAtMillis).coerceAtLeast(1L)
        val buckets = List(bars) { mutableListOf<DrivingEventType>() }
        events.forEach { event ->
            val index = ((event.atMillis - startedAtMillis) * bars / duration).toInt()
            buckets[index.coerceIn(0, bars - 1)] += event.type
        }
        return buckets.mapIndexed { index, types ->
            when {
                types.isEmpty() ->
                    TimelineBar(ShakeLevel.CALM, CALM_INTENSITY[index % CALM_INTENSITY.size])

                types.any { it != DrivingEventType.SHARP_TURN } ->
                    TimelineBar(ShakeLevel.BRAKE, (0.7f + 0.15f * types.size).coerceAtMost(1f))

                else ->
                    TimelineBar(ShakeLevel.TURN, (0.5f + 0.15f * types.size).coerceAtMost(0.85f))
            }
        }
    }

    companion object {
        const val MAX_SCORE = 100

        /** 조향비 (핸들 각도 ÷ 바퀴 각도). 승용차 평균 */
        const val STEERING_RATIO = 14f

        /** 축간거리 (아이오닉 5 기준 3.0m) */
        const val WHEELBASE_M = 3.0f
        const val TURN_MIN_SPEED_KMH = 15f
        const val TEMPERATURE_PENALTY = 3f
        const val RECOVERY_BONUS = 2
        const val SMOOTH_PASS_BONUS = 1
        const val ANTICIPATED_FACTOR = 0.5f
        const val CALM_RECOVERY_MS = 5 * 60_000L
        const val HAPPY_AFTER_CALM_MS = 10 * 60_000L
        const val MOOD_WINDOW_MS = 2 * 60_000L
        const val EVENT_COOLDOWN_MS = 4_000L
        const val BRAKE_MATCH_WINDOW_MS = 2_000L
        const val TIMELINE_BARS = 20
        private const val ACCEL_WINDOW_SEC = 1f
        private const val MIN_HEADING_WINDOW_SEC = 0.3f
        private const val MAX_ACCEL_WINDOW_SEC = 3f
        private const val MAX_INTEGRATION_HOURS = 5f / 3600f
        private const val KMH_PER_MPS = 3.6f

        /** 평온 구간도 완전히 평평하지 않게 (시안의 잔잔한 막대) */
        private val CALM_INTENSITY = listOf(0.3f, 0.32f, 0.4f, 0.3f, 0.26f, 0.34f)

        /** 359° → 1° 같은 경계를 넘는 방향 차이를 −180..180으로 */
        fun angleDiff(to: Float, from: Float): Float {
            var diff = (to - from) % 360f
            if (diff > 180f) diff -= 360f
            if (diff < -180f) diff += 360f
            return diff
        }
    }
}

val DrivingEventType.penalty: Int
    get() = when (this) {
        DrivingEventType.HARD_BRAKE -> 5
        DrivingEventType.HARD_ACCEL -> 5
        DrivingEventType.SHARP_TURN -> 3
    }
