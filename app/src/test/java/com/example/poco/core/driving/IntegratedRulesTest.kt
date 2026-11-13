package com.example.poco.core.driving

import com.example.poco.data.record.TripEventEntity
import com.example.poco.data.record.TripEventType
import com.example.poco.data.record.countEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntegratedRulesTest {
    private class Clock(var time: Long = 0) : DrivingClock {
        override fun now() = time
    }

    @Test fun petSizeBoundariesBelongToLargerClass() {
        assertEquals(PetSize.SMALL, petSizeFor(99))
        assertEquals(PetSize.MEDIUM, petSizeFor(100))
        assertEquals(PetSize.MEDIUM, petSizeFor(249))
        assertEquals(PetSize.LARGE, petSizeFor(250))
        assertEquals(99, parseWeightTenths("9.9"))
        assertEquals(100, parseWeightTenths("10"))
        assertNull(parseWeightTenths("10.25"))
        assertNull(parseWeightTenths("0"))
        assertEquals(PetSize.LARGE, DrivingSettings("a", PetSeat.PASSENGER, 20f, 25f, 250).petSize)
    }

    /** 3.2 examples: 10:00+120=12:00, 10:00+90=11:30, rest 11:20-11:35 then +90 = 13:05. */
    @Test fun restScheduleExamples() {
        val m = 60_000L
        val p = RestPlanner(600 * m, 120)
        assertEquals(720 * m, p.snapshot.nextRestAt)
        p.changeInterval(90, 610 * m)
        assertEquals(690 * m, p.snapshot.nextRestAt)
        assertTrue(p.startRest(680 * m, parked = true, RestStartSource.USER) is RestEvent.Started)
        assertTrue(p.onDeparture(695 * m) is RestEvent.Completed)
        assertEquals(785 * m, p.snapshot.nextRestAt) // 11:35 + 90 = 13:05
        assertEquals(1, p.snapshot.restCount)
        assertEquals(15 * m, p.snapshot.totalRestMs)
        assertNull(p.startRest(700 * m, parked = false, RestStartSource.USER))
    }

    @Test fun intervalPolicyClampsToStep() {
        assertEquals(120, RestIntervalPolicy.clamp(120))
        assertEquals(RestIntervalPolicy.MIN_MINUTES, RestIntervalPolicy.clamp(5))
        assertEquals(RestIntervalPolicy.MAX_MINUTES, RestIntervalPolicy.clamp(1000))
        assertEquals(90, RestIntervalPolicy.clamp(95))
    }

    /** 5.9 scenario using the real detector and calculator: 100,96,92,86,81,86,90,85,88. */
    @Test fun comfortScoreScenarioTable() {
        val clock = Clock()
        val config = ComfortScoreConfig()
        val detector = DrivingEventDetector(config)
        val score = ComfortScoreCalculator(clock, config)
        fun apply(events: List<DrivingEvent>) = events.forEach(score::penalize)
        fun run(ms: Long, resting: Boolean, stable: Boolean) {
            val end = clock.time + ms
            while (clock.time < end) {
                clock.time += 1000
                score.tick(resting, stable)
            }
        }
        // Sharp turn: 90° within 2 s at 30 km/h.
        clock.time = 1_000
        listOf(0.0, 30.0, 60.0, 90.0).forEachIndexed { i, deg ->
            apply(
                detector.onHeading(
                    1_000L + i * 500,
                    deg,
                    30.0
                )
            )
        }
        assertEquals(96, score.state.score)
        // Three bump passages: 35 (-1), 45 (-2), 35 (-1) = -4.
        listOf(35.0, 45.0, 35.0).forEach {
            detector.speedBump(true, it)?.let { e -> score.penalize(e) }
        }
        assertEquals(92, score.state.score)
        // Strong braking: 60 -> 48 km/h in 1 s (12 km/h/s) settles after the 1 s merge window.
        clock.time = 10_000
        apply(detector.onSpeed(SpeedSample(60.0, 10_000)))
        apply(detector.onSpeed(SpeedSample(48.0, 11_000)))
        apply(detector.flush(12_100))
        assertEquals(86, score.state.score)
        // Three mild curves (30° each, below the sharp-turn range) within 60 s -> repeated turns -5.
        var t = 20_000L
        repeat(3) {
            listOf(0.0, 10.0, 20.0, 30.0).forEach { deg ->
                apply(detector.onHeading(t, deg, 30.0))
                t +=
                    1_000
            }
            apply(detector.onHeading(t, 30.0, 30.0))
            t += 1_000
            apply(detector.flush(t))
            t += 5_000
        }
        clock.time = t
        assertEquals(81, score.state.score)
        // Rest begins after the 3-minute post-penalty cooldown and lasts 16 min: +5.
        run(4 * 60_000L, resting = false, stable = false)
        run(16 * 60_000L, resting = true, stable = false)
        assertEquals(86, score.state.score)
        // Completed rest, then calm driving 12 min: +4 (cap 90).
        score.markRestCompleted()
        run(12 * 60_000L + 1_000, resting = false, stable = true)
        assertEquals(90, score.state.score)
        // Regular braking -5: 8 km/h/s.
        val b = clock.time
        apply(detector.onSpeed(SpeedSample(50.0, b)))
        apply(detector.onSpeed(SpeedSample(42.0, b + 1_000)))
        apply(detector.flush(b + 2_100))
        assertEquals(85, score.state.score)
        // 3 min cooldown + 9 min calm driving = +3.
        run(12 * 60_000L + 2_000, resting = false, stable = true)
        assertEquals(88, score.state.score)
        assertEquals(
            listOf(-4, -1, -2, -1, -6, -5, 1, 1, 1, 1, 1, 1, 1, 1, 1, -5, 1, 1, 1),
            score.drainChanges().map { it.delta }
        )
    }

    @Test fun guidanceLedgerPriorityVoiceAndDuplicates() {
        val ledger = GuidanceLedger()
        val ai = Guidance("ai", GuidancePriority.AI_ANSWER, "답")
        val win = Guidance("w1", GuidancePriority.WINDOW_WARNING, "창문")
        assertEquals(GuidanceDecision(true, true, false), ledger.post(ai, voiceEnabled = true))
        ledger.startedSpeaking(ai)
        assertEquals(GuidanceDecision(true, true, true), ledger.post(win, voiceEnabled = true))
        ledger.startedSpeaking(win)
        val proposal = Guidance("p", GuidancePriority.REST_PROPOSAL, "제안")
        assertEquals(
            GuidanceDecision(true, false, false),
            ledger.post(proposal, voiceEnabled = true)
        )
        assertEquals(GuidanceDecision(false, false, false), ledger.post(win, voiceEnabled = true))
        assertEquals("w1", ledger.top()?.id)
        ledger.remove("w1")
        assertEquals("p", ledger.top()?.id)
        assertEquals(
            GuidanceDecision(true, false, false),
            ledger.post(Guidance("x", GuidancePriority.INFO, "i"), voiceEnabled = false)
        )
    }

    @Test fun etaUnitInferenceAndWaypointTracking() {
        assertEquals(EtaUnit.SECONDS, inferEtaUnit(900, 15_000))
        assertEquals(EtaUnit.MINUTES, inferEtaUnit(15, 15_000))
        assertNull(inferEtaUnit(100, 500)) // too short to decide: no ETA guidance
        val place = VerifiedPlace("ChIJvSMioIapZDURijUJ9O6phq8", "덕평", 37.24, 127.39, null)
        val tracker = WaypointTracker(place)
        assertNull(tracker.onWaypoints(listOf(NaviWaypoint("덕평", 37.2405, 127.39, 900, 15_000))))
        assertEquals("15분", tracker.etaMinutesText())
        assertTrue(
            tracker.onWaypoints(
                listOf(NaviWaypoint("덕평", 37.24, 127.39, 290, 4_000))
            ) is WaypointSignal.FiveMinutes
        )
        assertNull(tracker.onWaypoints(listOf(NaviWaypoint("덕평", 37.24, 127.39, 200, 3_000))))
        assertNull(tracker.onGear(parked = true)) // P without arrival
        assertTrue(tracker.onArrived(parked = true) is WaypointSignal.ArrivedAndParked)
        assertNull(tracker.onGear(parked = true))
        val cancelled = WaypointTracker(place)
        assertTrue(
            cancelled.onWaypoints(
                listOf(NaviWaypoint("다른곳", 36.0, 127.0, 10, 10))
            ) is WaypointSignal.Cancelled
        )
    }

    /** Pleos 내비는 경유지 좌표를 0,0으로 돌려준다: 그때는 이름으로 같은 장소인지 본다 */
    @Test fun waypointWithoutCoordinatesMatchesByName() {
        val place = VerifiedPlace("ChIJvSMioIapZDURijUJ9O6phq8", "덕평휴게소 달려라코코", 37.24, 127.39, null)
        val tracker = WaypointTracker(place)
        assertNull(tracker.onWaypoints(listOf(NaviWaypoint("덕평휴게소 달려라코코", 0.0, 0.0, 1980, 43_000))))
        assertFalse(tracker.finished)
        assertTrue(place.matchesWaypoint("덕평휴게소 달려라코코 ", 0.0, 0.0, 300.0))
        assertFalse(place.matchesWaypoint("이천휴게소", 0.0, 0.0, 300.0))
        // 좌표가 있으면 이름이 같아도 거리로 판단
        assertFalse(place.matchesWaypoint("덕평휴게소 달려라코코", 36.0, 127.0, 300.0))
        assertTrue(
            WaypointTracker(place).onWaypoints(
                listOf(NaviWaypoint("이천휴게소", 0.0, 0.0, 10, 10))
            ) is WaypointSignal.Cancelled
        )
    }

    @Test fun bumpPassageOnlyOnceAfterNearApproach() {
        val t = BumpPassageTracker()
        assertTrue(t.update(listOf("a" to 200)).isEmpty())
        assertTrue(t.update(emptyList()).isEmpty()) // never near: not a confirmed passage
        assertTrue(t.update(listOf("b" to 20)).isEmpty())
        assertEquals(listOf("b"), t.update(emptyList()))
        t.update(listOf("b" to 10))
        assertTrue(t.update(emptyList()).isEmpty())
    }

    /** #16: summary/list/detail all derive counts from the same stored events. */
    @Test fun recordCountsComeFromEventsOnly() {
        fun e(type: String, detail: String? = null, after: Int? = null) = TripEventEntity(
            tripId = "t",
            atEpoch = 0,
            type = type,
            detail = detail,
            scoreAfter = after
        )
        val counts = countEvents(
            listOf(
                e(TripEventType.SCORE_PENALTY, "RAPID_BRAKING", 94),
                e(TripEventType.SCORE_PENALTY, "SHARP_TURN", 90),
                e(TripEventType.SCORE_RECOVERY, after = 91),
                e(TripEventType.WINDOW_WARNING, "40"),
                e(TripEventType.TEMPERATURE_OUT_OF_RANGE, "HOT:27.0"),
                e(TripEventType.TEMPERATURE_SAMPLE, "27.0")
            )
        )
        assertEquals(1, counts.rapidBraking)
        assertEquals(1, counts.sharpTurn)
        assertEquals(1, counts.windowWarning)
        assertEquals(1, counts.temperatureAlert)
        assertEquals(90, counts.minScore)
        assertFalse(counts.speedBump > 0)
    }
}
