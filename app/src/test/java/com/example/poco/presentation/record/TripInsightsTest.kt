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

    /** Insight and recommendation come from one analysis; the window maps back to stored trips. */
    @Test fun motionAnalysisDrivesInsightAndRecommendation() {
        val day = 24 * 60 * m
        val trips = listOf(
            trip("a", 0, 30, "FINE"),
            trip("b", day, 40, "YAWN:1"),
            trip("c", 2 * day, 90, "YAWN:3"),
            trip("d", 3 * day, 120, "YAWN:4,DROOL:2")
        )
        val now = trips.last()
        val analysis = motionAnalysis(trips, now, 120)
        assertEquals(listOf("a", "b", "c", "d"), windowTrips(trips, now, analysis).map { it.first.id })
        assertEquals("긴 이동에서 불편 징후가 더 자주 관찰됐어요.", insightTip(analysis.insight).title)
        assertEquals(90, analysis.recommendedMinutes)
        // A plain count increase on the detail screen never shortens the interval by itself.
        val calm = motionAnalysis(listOf(trips[0], trips[1]), trips[1], 120)
        assertNull(calm.recommendedMinutes)
        assertEquals("이동 중 반려견 상태를 조금 더 살펴 주세요.", detailTip(trips[1], trips[0], 120, calm).body)
    }

    @Test fun cautionAndRepeatedSignTextUseParticles() {
        val now = trip("v", 0, 30, "VOMIT:1")
        val tip = insightTip(motionAnalysis(listOf(now), now, 120).insight)
        assertEquals("이번 이동에서 구토가 관찰됐어요. 반려견의 상태를 확인해 주세요.", tip.title)
        assertEquals("시간당 1.5회", rateText(1.5))
        assertEquals("시간당 2회", rateText(2.0))
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
    @Test fun restBandsUseRecordedTimesAndRecoverCompletedDuration() {
        assertEquals(listOf(40 * m to 56 * m), restBands(listOf(
            e(56 * m, TripEventType.REST_COMPLETED, detail = "${16 * m}"),
            e(40 * m, TripEventType.REST_STARTED)
        )))
        assertEquals(listOf(40 * m to 56 * m), restBands(listOf(
            e(56 * m, TripEventType.REST_COMPLETED, detail = "${16 * m}")
        )))
        assertTrue(restBands(listOf(e(56 * m, TripEventType.REST_COMPLETED, detail = "invalid"))).isEmpty())
        assertTrue(restBands(listOf(e(56 * m, TripEventType.REST_CANCELLED))).isEmpty())
    }

    @Test fun eventDetailUsesEventSnapshotAndPreservesGroupedBeforeScore() {
        val first = e(10 * m, TripEventType.SCORE_PENALTY, -2, 98, "SPEED_BUMP").copy(
            scoreBefore = 100, latitude = 37.0, longitude = 127.0,
            temperatureC = 23f, windowPercent = 0.0, seat = "PASSENGER", guidanceText = "안내 기록"
        )
        val second = e(11 * m, TripEventType.SCORE_PENALTY, -3, 95, "SPEED_BUMP").copy(scoreBefore = 98)
        val item = timelineItems(trip("t", 0, 60), listOf(first, second)).single()
        assertEquals(100, item.scoreBefore)
        assertEquals(95, item.scoreAfter)
        assertEquals(-5, item.delta)
        assertEquals(first, item.snapshot)
        assertEquals("23°C / 창문 닫힘", eventCabinText(item, emptyList()))
        assertEquals("37.00000, 127.00000", eventLocationText(item))
        assertNull(eventLocationText(item.copy(snapshot = null)))
    }

}
