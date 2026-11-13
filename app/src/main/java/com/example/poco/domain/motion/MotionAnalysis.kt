package com.example.poco.domain.motion

import com.example.poco.data.record.MotionRecord
import com.example.poco.data.record.MotionSign

/**
 * 이동 기록 분석 정책값. 의학적 위험 기준이 아니라 앱의 초기 분석 정책이며, 조정은 여기서만 한다.
 */
object MotionAnalysisPolicy {
    /** 현재 이동을 포함해 비교하는 최대 이동 수. */
    const val WINDOW_SIZE = 4

    /** 이보다 짧은 이동은 비교 분석에서 제외한다. */
    const val MIN_ANALYZABLE_MINUTES = 15.0

    /** 이 시간을 넘으면 장시간 이동으로 본다. */
    const val LONG_TRIP_MINUTES = 60.0

    /** 증가로 판단하는 최소 증가율 (30%). */
    const val INCREASE_RATIO = 0.30

    /** 증가로 판단하는 최소 시간당 발생률 차이. 기준 평균이 0이면 이 차이만으로 판단한다. */
    const val MIN_RATE_DIFF_PER_HOUR = 0.5

    /** 장시간·단시간 그룹 비교에 필요한 그룹별 최소 이동 수. */
    const val MIN_TRIPS_PER_GROUP = 2

    /** 최근 징후 증가 비교에 쓰는 이전 이동 수. */
    const val PREVIOUS_TRIPS_FOR_INCREASE = 3

    /** 감소 추세·반복 징후를 볼 최근 이동 수. */
    const val RECENT_TREND_TRIPS = 3

    /** 반복 징후로 보는 서로 다른 이동 수. */
    const val REPEAT_MIN_TRIPS = 2

    /** 휴식 간격 단축 단위와 최소 간격 (분). */
    const val REST_STEP_MINUTES = 30
    const val REST_MIN_MINUTES = 30

    /** 휴식 간격 조정 대신 상태 확인을 안내하는 주의 징후. */
    val CAUTION_SIGNS: Set<MotionSign> = setOf(MotionSign.VOMIT)
}

/** 분석 입력: 저장된 이동 한 건. [record]가 null이면 보호자가 아직 기록하지 않은 이동이다. */
data class MotionTripInput(
    val id: String,
    val startedAtEpoch: Long,
    val durationMinutes: Double,
    val record: MotionRecord?
)

/** 분석 창에 들어간 이동 한 건과 계산값. */
data class AnalyzedTrip(
    val input: MotionTripInput,
    /** 총 징후 횟수. 기록이 없으면 null (0회로 취급하지 않는다). */
    val totalSigns: Int?,
    /** 시간당 징후 발생률. 기록이 없거나 이동 시간이 0이면 null. */
    val ratePerHour: Double?,
    /** 기록이 있고 15분 이상 이동해 비교 분석에 쓸 수 있는지. */
    val comparable: Boolean
) {
    val id: String get() = input.id
    val isLong: Boolean get() = input.durationMinutes > MotionAnalysisPolicy.LONG_TRIP_MINUTES
}

/** 시간당 징후 발생률 = (총 징후 횟수 ÷ 이동 시간(분)) × 60. 이동 시간이 0 이하이면 계산하지 않는다. */
fun ratePerHour(totalSigns: Int, durationMinutes: Double): Double? =
    if (durationMinutes <= 0.0) null else totalSigns / durationMinutes * 60.0

/**
 * 비교 기준(이전 평균) 대비 증가 여부. 30% 이상 증가하면서 시간당 0.5회 이상 늘어야 한다.
 * 기준이 0이면 증가율을 계산하지 않고 절대 발생률 차이만으로 판단한다.
 */
fun isMeaningfulIncrease(baseline: Double, value: Double): Boolean {
    val diff = value - baseline
    if (diff < MotionAnalysisPolicy.MIN_RATE_DIFF_PER_HOUR) return false
    if (baseline <= 0.0) return true
    return diff / baseline >= MotionAnalysisPolicy.INCREASE_RATIO
}

/** 이동 기록을 분석 창으로 추리고 징후 발생률을 계산한다. 점수 계산과는 독립된 로직이다. */
object TripAnalyzer {
    fun analyze(input: MotionTripInput): AnalyzedTrip {
        val total = input.record?.total
        val rate = total?.let { ratePerHour(it, input.durationMinutes) }
        val comparable = input.record != null &&
            rate != null &&
            input.durationMinutes >= MotionAnalysisPolicy.MIN_ANALYZABLE_MINUTES
        return AnalyzedTrip(input, total, rate, comparable)
    }

    /**
     * 현재 이동 + 그 이전의 비교 가능한 이동 최대 3회, 오래된 순. 선택한 이동 이후의 기록은 포함하지 않는다.
     * 현재 이동은 비교 조건을 만족하지 않아도 화면 표시를 위해 항상 마지막에 둔다.
     */
    fun window(all: List<MotionTripInput>, current: MotionTripInput): List<AnalyzedTrip> {
        val earlier = all.asSequence()
            .filter { it.id != current.id && it.startedAtEpoch < current.startedAtEpoch }
            .map(::analyze)
            .filter { it.comparable }
            .sortedByDescending { it.input.startedAtEpoch }
            .take(MotionAnalysisPolicy.WINDOW_SIZE - 1)
            .toList()
            .reversed()
        return earlier + analyze(current)
    }
}

enum class InsightType(val priority: Int) {
    CAUTION_SIGN(1),
    LONG_TRIP_INCREASE(2),
    RECENT_INCREASE(3),
    DECREASING(4),
    REPEATED_SIGN(5),
    NOT_ENOUGH_DATA(6),
    NO_CHANGE(6)
}

/** 우선순위가 가장 높은 인사이트 하나와 판단 근거 수치. 문구는 화면에서 만든다. */
data class MotionInsight(
    val type: InsightType,
    /** CAUTION_SIGN · REPEATED_SIGN 대상 징후. */
    val sign: MotionSign? = null,
    /** 비교 기준 시간당 발생률 (짧은 이동 평균 / 이전 3회 평균). */
    val baselineRate: Double? = null,
    /** 비교 대상 시간당 발생률 (긴 이동 평균 / 이번 이동). */
    val targetRate: Double? = null,
    /** DECREASING: 최근 3회 발생률, 오래된 순. */
    val trend: List<Double> = emptyList(),
    /** REPEATED_SIGN: 해당 징후가 나온 이동 수 / 살펴본 이동 수. */
    val repeatedIn: Int = 0,
    val repeatedOf: Int = 0
)

/** 분석 조건에 따라 인사이트 유형과 우선순위를 정한다. */
object InsightRuleEngine {
    fun evaluate(window: List<AnalyzedTrip>): MotionInsight {
        val current = window.lastOrNull() ?: return MotionInsight(InsightType.NOT_ENOUGH_DATA)
        cautionSign(current)?.let { return it }
        val comparable = window.filter { it.comparable }
        longTripIncrease(comparable)?.let { return it }
        if (!current.comparable) return MotionInsight(InsightType.NOT_ENOUGH_DATA)
        recentIncrease(comparable)?.let { return it }
        decreasing(comparable)?.let { return it }
        repeatedSign(comparable)?.let { return it }
        return if (comparable.size <
            MotionAnalysisPolicy.RECENT_TREND_TRIPS
        ) {
            MotionInsight(InsightType.NOT_ENOUGH_DATA)
        } else {
            MotionInsight(InsightType.NO_CHANGE)
        }
    }

    /** 우선순위 1: 이번 이동에서 구토 등 주의 징후. 이동 시간과 관계없이 확인을 안내한다. */
    private fun cautionSign(current: AnalyzedTrip): MotionInsight? {
        val record = current.input.record ?: return null
        val sign =
            MotionSign.entries.firstOrNull {
                it in MotionAnalysisPolicy.CAUTION_SIGNS &&
                    record.count(it) > 0
            }
                ?: return null
        return MotionInsight(InsightType.CAUTION_SIGN, sign = sign)
    }

    /** 우선순위 2: 60분 초과·이하 그룹이 각각 2회 이상이고 긴 이동 평균 발생률이 의미 있게 높을 때. */
    private fun longTripIncrease(comparable: List<AnalyzedTrip>): MotionInsight? {
        val (long, short) = comparable.partition { it.isLong }
        if (long.size < MotionAnalysisPolicy.MIN_TRIPS_PER_GROUP ||
            short.size < MotionAnalysisPolicy.MIN_TRIPS_PER_GROUP
        ) {
            return null
        }
        val longAvg = long.mapNotNull { it.ratePerHour }.average()
        val shortAvg = short.mapNotNull { it.ratePerHour }.average()
        if (!isMeaningfulIncrease(shortAvg, longAvg)) return null
        return MotionInsight(
            InsightType.LONG_TRIP_INCREASE,
            baselineRate = shortAvg,
            targetRate = longAvg
        )
    }

    /** 우선순위 3: 이전 3회 평균 대비 이번 이동 발생률이 의미 있게 늘었을 때. */
    private fun recentIncrease(comparable: List<AnalyzedTrip>): MotionInsight? {
        val previous = comparable.dropLast(1)
        if (previous.size < MotionAnalysisPolicy.PREVIOUS_TRIPS_FOR_INCREASE) return null
        val baseline = previous.takeLast(
            MotionAnalysisPolicy.PREVIOUS_TRIPS_FOR_INCREASE
        ).mapNotNull {
            it.ratePerHour
        }.average()
        val now = comparable.last().ratePerHour ?: return null
        if (!isMeaningfulIncrease(baseline, now)) return null
        return MotionInsight(InsightType.RECENT_INCREASE, baselineRate = baseline, targetRate = now)
    }

    /** 우선순위 4: 최근 3회 발생률이 연속으로 감소. */
    private fun decreasing(comparable: List<AnalyzedTrip>): MotionInsight? {
        val recent = comparable.takeLast(MotionAnalysisPolicy.RECENT_TREND_TRIPS).mapNotNull {
            it.ratePerHour
        }
        if (recent.size < MotionAnalysisPolicy.RECENT_TREND_TRIPS) return null
        if (!recent.zipWithNext().all { (a, b) -> b < a }) return null
        return MotionInsight(InsightType.DECREASING, trend = recent)
    }

    /** 우선순위 5: 최근 3회 중 같은 징후가 서로 다른 2회 이상의 이동에서 기록됨. */
    private fun repeatedSign(comparable: List<AnalyzedTrip>): MotionInsight? {
        val recent = comparable.takeLast(MotionAnalysisPolicy.RECENT_TREND_TRIPS).mapNotNull {
            it.input.record
        }
        val best = MotionSign.entries
            .map { sign ->
                Triple(sign, recent.count { it.count(sign) > 0 }, recent.sumOf { it.count(sign) })
            }
            .filter { it.second >= MotionAnalysisPolicy.REPEAT_MIN_TRIPS }
            .sortedWith(
                compareByDescending<Triple<MotionSign, Int, Int>> {
                    it.second
                }.thenByDescending { it.third }
            )
            .firstOrNull() ?: return null
        return MotionInsight(
            InsightType.REPEATED_SIGN,
            sign = best.first,
            repeatedIn = best.second,
            repeatedOf = recent.size
        )
    }
}

/** 휴식 간격을 유지할 때 화면에서 함께 안내할 내용. */
enum class KeepReason {
    /** 주의 징후: 상태 확인 · 필요시 수의사 상담. */
    CHECK_PET,

    /** 최근 징후 증가: 이동 중 상태를 더 주의 깊게 관찰. */
    OBSERVE_CLOSELY,

    /** 특정 징후 반복: 해당 징후가 반복되는지 관찰. */
    OBSERVE_SIGN,

    /** 징후 감소 · 변화 없음. */
    STABLE,

    /** 비교할 기록 부족. */
    NOT_ENOUGH_DATA,

    /** 단축 조건은 충족했지만 이미 최소 간격(30분). */
    AT_MINIMUM
}

sealed interface RestRecommendation {
    val currentMinutes: Int

    /** 보호자가 수락해야만 설정에 반영된다. 자동 적용하지 않는다. */
    data class Shorten(override val currentMinutes: Int, val recommendedMinutes: Int) :
        RestRecommendation
    data class Keep(override val currentMinutes: Int, val reason: KeepReason) : RestRecommendation
}

/** 인사이트와 현재 휴식 간격(사용자 설정값)으로 다음 이동 제안을 계산한다. */
object RecommendationEngine {
    /** 추천 휴식 간격 = max(현재 휴식 간격 - 30분, 30분). */
    fun shortened(currentMinutes: Int): Int = maxOf(
        currentMinutes - MotionAnalysisPolicy.REST_STEP_MINUTES,
        MotionAnalysisPolicy.REST_MIN_MINUTES
    )

    fun recommend(insight: MotionInsight, currentMinutes: Int): RestRecommendation =
        when (insight.type) {
            InsightType.LONG_TRIP_INCREASE -> {
                val next = shortened(currentMinutes)
                if (next < currentMinutes) {
                    RestRecommendation.Shorten(currentMinutes, next)
                } else {
                    RestRecommendation.Keep(currentMinutes, KeepReason.AT_MINIMUM)
                }
            }
            InsightType.CAUTION_SIGN -> RestRecommendation.Keep(
                currentMinutes,
                KeepReason.CHECK_PET
            )
            InsightType.RECENT_INCREASE -> RestRecommendation.Keep(
                currentMinutes,
                KeepReason.OBSERVE_CLOSELY
            )
            InsightType.REPEATED_SIGN -> RestRecommendation.Keep(
                currentMinutes,
                KeepReason.OBSERVE_SIGN
            )
            InsightType.DECREASING, InsightType.NO_CHANGE -> RestRecommendation.Keep(
                currentMinutes,
                KeepReason.STABLE
            )
            InsightType.NOT_ENOUGH_DATA -> RestRecommendation.Keep(
                currentMinutes,
                KeepReason.NOT_ENOUGH_DATA
            )
        }
}

/** 인사이트와 추천이 같은 분석 결과에서 나오도록 묶어서 전달한다. */
data class MotionAnalysis(
    val window: List<AnalyzedTrip>,
    val insight: MotionInsight,
    val recommendation: RestRecommendation
) {
    val recommendedMinutes: Int?
        get() = (recommendation as? RestRecommendation.Shorten)?.recommendedMinutes

    companion object {
        fun of(
            all: List<MotionTripInput>,
            current: MotionTripInput,
            restIntervalMinutes: Int
        ): MotionAnalysis {
            val window = TripAnalyzer.window(all, current)
            val insight = InsightRuleEngine.evaluate(window)
            return MotionAnalysis(
                window,
                insight,
                RecommendationEngine.recommend(insight, restIntervalMinutes)
            )
        }
    }
}
