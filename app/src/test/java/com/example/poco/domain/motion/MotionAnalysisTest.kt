package com.example.poco.domain.motion

import com.example.poco.data.record.MotionRecord
import com.example.poco.data.record.MotionSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionAnalysisTest {
    private val day = 24 * 60 * 60_000L
    private var seq = 0

    private fun t(minutes: Double, record: MotionRecord?, at: Long = (seq++) * day, id: String = "t$seq") =
        MotionTripInput(id, at, minutes, record)

    private fun signs(vararg pairs: Pair<MotionSign, Int>) = MotionRecord(pairs.toMap())
    private fun yawn(n: Int) = if (n == 0) MotionRecord(fine = true) else signs(MotionSign.YAWN to n)

    private fun analyze(trips: List<MotionTripInput>, interval: Int = 120) =
        MotionAnalysis.of(trips, trips.last(), interval)

    @Test fun ratePerHourUsesDuration() {
        assertEquals(4.0, ratePerHour(2, 30.0)!!, 1e-9) // 이동 A
        assertEquals(2.0, ratePerHour(4, 120.0)!!, 1e-9) // 이동 B
        assertNull(ratePerHour(3, 0.0))
    }

    @Test fun increaseNeedsRatioAndAbsoluteDifference() {
        assertTrue(isMeaningfulIncrease(1.0, 1.5)) // +50%, +0.5
        assertFalse(isMeaningfulIncrease(1.0, 1.4)) // +40% but only +0.4
        assertFalse(isMeaningfulIncrease(2.0, 2.5)) // +0.5 but only +25%
        assertTrue(isMeaningfulIncrease(0.0, 0.5)) // zero baseline: absolute difference only
        assertFalse(isMeaningfulIncrease(0.0, 0.4))
    }

    @Test fun windowExcludesShortUnrecordedAndLaterTrips() {
        val a = t(30.0, yawn(1))
        val tooShort = t(10.0, yawn(2))
        val unrecorded = t(40.0, null)
        val b = t(50.0, yawn(1))
        val c = t(70.0, yawn(1))
        val d = t(45.0, yawn(1))
        val current = t(60.0, yawn(1))
        val later = t(30.0, yawn(5))
        val window = TripAnalyzer.window(listOf(later, a, tooShort, unrecorded, b, c, d, current), current)
        assertEquals(listOf(b.id, c.id, d.id, current.id), window.map { it.id })
        assertFalse(TripAnalyzer.analyze(tooShort).comparable)
        assertNull(TripAnalyzer.analyze(unrecorded).totalSigns) // not entered is never 0 signs
    }

    @Test fun cautionSignWinsAndDoesNotShortenInterval() {
        val trips = listOf(t(30.0, yawn(0)), t(40.0, yawn(0)), t(90.0, yawn(4)), t(120.0, signs(MotionSign.VOMIT to 1, MotionSign.YAWN to 5)))
        val r = analyze(trips)
        assertEquals(InsightType.CAUTION_SIGN, r.insight.type)
        assertEquals(MotionSign.VOMIT, r.insight.sign)
        assertEquals(RestRecommendation.Keep(120, KeepReason.CHECK_PET), r.recommendation)
    }

    @Test fun longTripIncreaseShortensByThirtyMinutes() {
        val trips = listOf(t(30.0, yawn(0)), t(40.0, yawn(1)), t(90.0, yawn(3)), t(120.0, yawn(6)))
        val r = analyze(trips)
        assertEquals(InsightType.LONG_TRIP_INCREASE, r.insight.type)
        assertEquals(0.75, r.insight.baselineRate!!, 1e-9)
        assertEquals(2.5, r.insight.targetRate!!, 1e-9)
        assertEquals(RestRecommendation.Shorten(120, 90), r.recommendation)
        assertEquals(RestRecommendation.Shorten(150, 120), analyze(trips, 150).recommendation) // user's own setting
        assertEquals(RestRecommendation.Keep(30, KeepReason.AT_MINIMUM), analyze(trips, 30).recommendation)
    }

    @Test fun longTripPatternNeedsTwoTripsPerGroup() {
        val trips = listOf(t(30.0, yawn(0)), t(90.0, yawn(3)), t(120.0, yawn(6)))
        assertTrue(analyze(trips).insight.type != InsightType.LONG_TRIP_INCREASE)
    }

    @Test fun recentIncreaseKeepsIntervalAndAsksToObserve() {
        // All short trips, previous avg 1.0/h, now 3.0/h.
        val trips = listOf(t(60.0, yawn(1)), t(60.0, yawn(1)), t(60.0, yawn(1)), t(40.0, yawn(2)))
        val r = analyze(trips)
        assertEquals(InsightType.RECENT_INCREASE, r.insight.type)
        assertEquals(1.0, r.insight.baselineRate!!, 1e-9)
        assertEquals(RestRecommendation.Keep(120, KeepReason.OBSERVE_CLOSELY), r.recommendation)
        assertNull(r.recommendedMinutes)
    }

    @Test fun recentIncreaseFromZeroBaselineUsesAbsoluteDifference() {
        val trips = listOf(t(60.0, yawn(0)), t(60.0, yawn(0)), t(60.0, yawn(0)), t(60.0, yawn(1)))
        assertEquals(InsightType.RECENT_INCREASE, analyze(trips).insight.type)
    }

    @Test fun decreasingTrend() {
        val trips = listOf(t(60.0, yawn(3)), t(60.0, signs(MotionSign.DROOL to 2)), t(60.0, signs(MotionSign.PANT to 1)))
        val r = analyze(trips)
        assertEquals(InsightType.DECREASING, r.insight.type)
        assertEquals(listOf(3.0, 2.0, 1.0), r.insight.trend)
        assertEquals(RestRecommendation.Keep(120, KeepReason.STABLE), r.recommendation)
    }

    @Test fun repeatedSignAcrossDifferentTrips() {
        val trips = listOf(t(60.0, yawn(1)), t(60.0, signs(MotionSign.DROOL to 1)), t(60.0, yawn(1)))
        val r = analyze(trips)
        assertEquals(InsightType.REPEATED_SIGN, r.insight.type)
        assertEquals(MotionSign.YAWN, r.insight.sign)
        assertEquals(2, r.insight.repeatedIn)
        assertEquals(RestRecommendation.Keep(120, KeepReason.OBSERVE_SIGN), r.recommendation)
    }

    @Test fun notEnoughDataOrNoChange() {
        assertEquals(InsightType.NOT_ENOUGH_DATA, analyze(listOf(t(60.0, yawn(1)))).insight.type)
        assertEquals(InsightType.NOT_ENOUGH_DATA, analyze(listOf(t(60.0, yawn(0)), t(60.0, yawn(0)), t(10.0, yawn(0)))).insight.type)
        assertEquals(InsightType.NOT_ENOUGH_DATA, analyze(listOf(t(60.0, yawn(0)), t(60.0, yawn(0)), t(60.0, null))).insight.type)
        val flat = analyze(listOf(t(60.0, yawn(0)), t(60.0, yawn(0)), t(60.0, yawn(0))))
        assertEquals(InsightType.NO_CHANGE, flat.insight.type)
        assertEquals(RestRecommendation.Keep(120, KeepReason.STABLE), flat.recommendation)
    }
}
