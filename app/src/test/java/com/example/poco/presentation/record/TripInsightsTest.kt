package com.example.poco.presentation.record

import com.example.poco.data.record.MotionRecord
import com.example.poco.data.record.MotionSign
import com.example.poco.data.record.TripEntity
import com.example.poco.data.record.TripEventEntity
import com.example.poco.data.record.TripEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripInsightsTest {
    private val m = 60_000L
    private fun trip(id: String, start: Long, minutes: Long, signs: String? = null, score: Int = 88) = TripEntity(
        id = id, petName = "현댕이", petWeightTenths = null, petSize = null, seat = "PASSENGER",
        startedAtEpoch = start, endedAtEpoch = start + minutes * m, status = "COMPLETED",
        startRestIntervalMinutes = 120, finalScore = score, motionSicknessSigns = signs
    )

    private fun e(at: Long, type: String, delta: Int? = null, after: Int? = null, detail: String? = null) =
        TripEventEntity(tripId = "t", atEpoch = at, type = type, scoreDelta = delta, scoreAfter = after, detail = detail)

    @Test fun motionRecordRoundTripAndLegacyValues() {
        val r = MotionRecord().withCount(MotionSign.YAWN, 3).toggle(MotionSign.DROOL)
        assertEquals("YAWN:3,DROOL:1", r.encode())
        assertEquals(r, MotionRecord.decode(r.encode()))
        assertEquals(4, r.total)
        assertEquals("FINE", MotionRecord().markFine().encode())
        assertTrue(MotionRecord.decode("FINE")!!.fine)
        assertNull(MotionRecord.decode(null)) // not entered is never "0 signs"
        assertNull(MotionRecord().encode())
        // Selecting a sign clears "괜찮았어요"; legacy free-text entries are ignored, not guessed.
        assertFalse(MotionRecord(fine = true).toggle(MotionSign.SHAKE).fine)
        assertEquals(0, MotionRecord.decode("하품·침 흘림")!!.total)
    }

    /** Figma 465:48555 rows: bumps merged, rest with its recovery, calm runs, window alert. */
    @Test fun timelineGroupsEventsLikeTheDesign() {
        val t = trip("t", 0, 150)
        val items = timelineItems(
            t,
            listOf(
                e(10 * m, TripEventType.SCORE_PENALTY, -4, 96, "SHARP_TURN"),
                e(20 * m, TripEventType.SCORE_PENALTY, -1, 95, "SPEED_BUMP"),
                e(21 * m, TripEventType.SCORE_PENALTY, -2, 93, "SPEED_BUMP"),
                e(30 * m, TripEventType.WINDOW_WARNING, detail = "50"),
                e(40 * m, TripEventType.REST_STARTED, detail = "WAYPOINT_ARRIVAL"),
                e(43 * m, TripEventType.SCORE_RECOVERY, 1, 94),
                e(46 * m, TripEventType.SCORE_RECOVERY, 1, 95),
                e(56 * m, TripEventType.REST_COMPLETED, detail = "${16 * m}"),
                e(70 * m, TripEventType.SCORE_RECOVERY, 1, 96),
                e(73 * m, TripEventType.SCORE_RECOVERY, 1, 97),
                e(80 * m, TripEventType.SCORE_PENALTY, -5, 92, "RAPID_BRAKING")
            )
        )
        assertEquals(listOf("급회전", "과속방지턱 2회", "창문 50% 열림", "휴식 16분", "평온 유지 3분", "급정거"), items.map { it.title })
        assertEquals(listOf(-4, -3, 0, 2, 2, -5), items.map { it.delta })
        assertEquals(ItemKind.ALERT, items[2].kind)
        assertEquals("경유지 도착 후 휴식", items[3].detail)
    }

    @Test fun recommendationOnlyWhenSignsWereRecorded() {
        assertNull(recommendedInterval(120, null))
        assertNull(recommendedInterval(120, MotionRecord(fine = true)))
        assertEquals(90, recommendedInterval(120, MotionRecord(mapOf(MotionSign.YAWN to 3))))
        assertNull(recommendedInterval(30, MotionRecord(mapOf(MotionSign.YAWN to 1)))) // already at minimum
    }

    /** "1시간 넘게 달린 날 징후가 늘었어요" only when long and short entries both exist. */
    @Test fun patternNeedsLongAndShortEntries() {
        val day = 24 * 60 * m
        val short = trip("a", 0, 25, "FINE", 92)
        val mid = trip("b", day, 80, "YAWN:1", 86)
        val now = trip("c", 2 * day, 154, "YAWN:3,DROOL:1", 88)
        val recent = recentMotionTrips(listOf(now, mid, short), now)
        assertEquals(listOf("a", "b", "c"), recent.map { it.first.id })
        val p = motionPattern(recent)!!
        assertEquals("1시간 넘게 달린 날 징후가 늘었어요.", p.title)
        assertTrue(p.body.startsWith("25분 이동"))
        assertNull(motionPattern(recentMotionTrips(listOf(now, mid), now)))
    }

    @Test fun scoreComparisonUsesThePreviousCompletedTrip() {
        val a = trip("a", 0, 30, score = 86)
        val b = trip("b", 100 * m, 30, score = 88)
        assertEquals(a, previousTrip(listOf(b, a), b))
        assertEquals("지난번보다 2점 올랐어요", scoreDeltaText(b, a))
        assertNull(scoreDeltaText(a, null))
    }

    @Test fun subjectParticleFollowsBatchim() {
        assertEquals("가", subjectParticle("급정거"))
        assertEquals("이", subjectParticle("급회전"))
        assertEquals("가", subjectParticle("과속방지턱 2회"))
        assertEquals("이", subjectParticle("휴식 16분"))
    }
}
