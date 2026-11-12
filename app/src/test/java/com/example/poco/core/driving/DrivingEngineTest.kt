package com.example.poco.core.driving

import com.example.poco.data.model.WindowPositionRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivingEngineTest {
    private class Clock(var time: Long = 0) : DrivingClock {
        override fun now() = time
    }

    @Test fun startsAt100() {
        assertEquals(100, ComfortScoreCalculator(Clock()).state.score)
    }

    @Test fun accelerationFromStopCosts3() {
        val detector = DrivingEventDetector()
        val events =
            detector.onSpeed(SpeedSample(0.0, 0)) + detector.onSpeed(SpeedSample(11.0, 1000))
        val score = ComfortScoreCalculator(Clock())
        events.forEach(score::penalize)
        assertEquals(97, score.state.score)
    }

    @Test fun accelerationWithin3SecondsIsMerged() {
        val detector = DrivingEventDetector()
        val events = (0..4).flatMap { detector.onSpeed(SpeedSample(it * 11.0, it * 1000L)) }
        assertEquals(listOf(DrivingEvent.RapidAcceleration), events)
    }

    @Test fun accelerationAfterQuietIntervalIsNew() {
        val d = DrivingEventDetector()
        d.onSpeed(SpeedSample(0.0, 0))
        d.onSpeed(SpeedSample(11.0, 1000))
        for (i in 2..5) d.onSpeed(SpeedSample(11.0, i * 1000L))
        assertEquals(listOf(DrivingEvent.RapidAcceleration), d.onSpeed(SpeedSample(22.0, 6000)))
    }

    @Test fun brakingIntensityBoundaries() {
        for ((rate, expected) in listOf(7.5 to 5, 10.99 to 5, 11.0 to 6, 40.0 to 6)) {
            val d = DrivingEventDetector()
            d.onSpeed(SpeedSample(60.0, 0))
            d.onSpeed(SpeedSample(60.0 - rate, 1000))
            assertEquals(listOf(DrivingEvent.RapidBraking(expected)), d.flush(2001))
            assertTrue(d.flush(3000).isEmpty())
        }
    }

    @Test fun strongerBrakeWithinMergeWindowIsOneEvent() {
        val d = DrivingEventDetector()
        d.onSpeed(SpeedSample(60.0, 0))
        d.onSpeed(SpeedSample(52.0, 1000))
        d.onSpeed(SpeedSample(40.0, 2000))
        assertEquals(listOf(DrivingEvent.RapidBraking(6)), d.flush(3001))
    }

    @Test fun sensorAnomaliesAreLoggedNotPenalized() {
        val messages = mutableListOf<String>()
        for (end in listOf(100.0, 0.0)) {
            val d = DrivingEventDetector(anomaly = messages::add)
            d.onSpeed(SpeedSample(50.0, 0))
            assertTrue(d.onSpeed(SpeedSample(end, 1000)).isEmpty())
        }
        assertEquals(2, messages.size)
    }

    @Test fun duplicateOrOutOfOrderSpeedDoesNotScore() {
        val d = DrivingEventDetector()
        d.onSpeed(SpeedSample(0.0, 1000))
        assertTrue(d.onSpeed(SpeedSample(20.0, 1000)).isEmpty())
        assertTrue(d.onSpeed(SpeedSample(20.0, 0)).isEmpty())
    }

    @Test fun scoreNeverFallsBelowZero() {
        val score = ComfortScoreCalculator(Clock())
        repeat(100) { score.penalize(DrivingEvent.RapidAcceleration) }
        assertEquals(0, score.state.score)
    }

    @Test fun recoveryHasCooldownAndCapAndNoDuplicateTick() {
        val clock = Clock()
        val score = ComfortScoreCalculator(clock)
        repeat(5) { score.penalize(DrivingEvent.RapidAcceleration) }
        score.tick(true, false)
        clock.time = 179999
        score.tick(true, false)
        assertEquals(85, score.state.score)
        clock.time = 180000
        score.tick(true, false)
        assertEquals(85, score.state.score)
        clock.time = 360000
        score.tick(true, false)
        assertEquals(86, score.state.score)
        score.tick(true, false)
        assertEquals(86, score.state.score)
        clock.time = 3600000
        score.tick(true, false)
        assertEquals(90, score.state.score)
        score.penalize(DrivingEvent.RapidAcceleration)
        assertEquals(87, score.state.score)
    }

    @Test fun stableMovingRecoveryRequiresValidRestAndContinuousStability() {
        val clock = Clock()
        val score = ComfortScoreCalculator(clock)
        repeat(5) { score.penalize(DrivingEvent.RapidAcceleration) }
        score.tick(false, true)
        clock.time = 600000
        score.tick(false, true)
        assertEquals(85, score.state.score)
        score.tick(true, false)
        clock.time += 180000
        score.tick(true, false)
        // Parking/resting alone does not complete a rest; only RestPlanner's departure does.
        assertFalse(score.state.hasRested)
        score.markRestCompleted()
        assertTrue(score.state.hasRested)
        score.tick(false, true)
        clock.time += 90000
        score.tick(false, true)
        score.tick(false, false)
        clock.time += 90000
        score.tick(false, true)
        val before = score.state.score
        clock.time += 179999
        score.tick(false, true)
        assertEquals(before, score.state.score)
        clock.time++
        score.tick(false, true)
        assertEquals(before + 1, score.state.score)
    }

    @Test fun unknownStabilityNeverRecoversDuringDriving() {
        val clock = Clock()
        val score = ComfortScoreCalculator(clock)
        repeat(5) { score.penalize(DrivingEvent.RapidAcceleration) }
        score.tick(true, false)
        clock.time = 180000
        score.tick(true, false)
        score.tick(false, false)
        clock.time += 1800000
        score.tick(false, false)
        assertEquals(85, score.state.score)
    }

    @Test fun windowThresholdAndDangerEpisodes() {
        val d = WindowDangerDetector()
        assertFalse(d.update(null))
        assertFalse(d.update(20.0))
        assertTrue(d.update(20.01))
        assertFalse(d.update(30.0))
        assertFalse(d.update(null))
        assertFalse(d.update(35.0))
        assertFalse(d.update(0.0))
        assertTrue(d.update(35.0))
    }

    @Test fun initialOpenWindowAlerts() {
        assertTrue(WindowDangerDetector().update(25.0))
    }

    @Test fun windowRangeIsNormalizedAndUnsupportedValuesAreUnknown() {
        assertEquals(25.0, normalizeWindowPosition(50, WindowPositionRange(0, 200))!!, 0.001)
        assertNull(normalizeWindowPosition(null, WindowPositionRange(0, 200)))
        assertNull(normalizeWindowPosition(5, WindowPositionRange(-10, 200)))
        assertNull(normalizeWindowPosition(300, WindowPositionRange(0, 200)))
    }

    @Test fun temperatureUsesInclusiveUserLimitsAndHandlesMissingValue() {
        val settings = DrivingSettings("강아지", PetSeat.PASSENGER, 19f, 25f)
        assertEquals(CabinTemperatureStatus.UNKNOWN, temperatureStatus(null, settings))
        assertEquals(CabinTemperatureStatus.COLD, temperatureStatus(18f, settings))
        assertEquals(CabinTemperatureStatus.NORMAL, temperatureStatus(19f, settings))
        assertEquals(CabinTemperatureStatus.NORMAL, temperatureStatus(25f, settings))
        assertEquals(CabinTemperatureStatus.HOT, temperatureStatus(26f, settings))
    }

    @Test fun cautionWithoutConfirmedPassageNeverCostsPoints() {
        val d = DrivingEventDetector()
        assertNull(d.speedBump(false, 60.0))
        assertNull(d.speedBump(true, null))
        assertNull(d.speedBump(true, 30.0))
        assertEquals(DrivingEvent.SpeedBump(1), d.speedBump(true, 40.0))
        assertEquals(DrivingEvent.SpeedBump(2), d.speedBump(true, 40.01))
    }

    @Test fun sharpTurnUsesWraparoundAndDoesNotRepeatWithinOneCurve() {
        val d = DrivingEventDetector()
        val events = listOf(350.0, 10.0, 30.0, 50.0, 70.0).flatMapIndexed { i, heading ->
            d.onHeading(
                i * 400L,
                heading,
                30.0
            )
        }
        assertEquals(listOf(DrivingEvent.SharpTurn), events)
        assertTrue(d.flush(5000).isEmpty())
    }

    @Test fun threeIndependentCurvesAreConsumedAndSharpCurveIsExcluded() {
        val d = DrivingEventDetector()
        val events = mutableListOf<DrivingEvent>()
        var now = 0L
        var heading = 0.0
        d.onHeading(now, heading, 30.0)
        repeat(3) {
            now += 400
            heading += 20
            events += d.onHeading(now, heading, 30.0)
            now += 1600
            events += d.flush(now)
            events += d.onHeading(now, heading, 30.0)
        }
        assertEquals(listOf(DrivingEvent.RepeatedTurns), events)
        assertTrue(d.flush(now + 1000).isEmpty())
    }
}
