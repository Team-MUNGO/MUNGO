package com.example.poco.data.assistant

import com.example.poco.data.record.TripEntity
import com.example.poco.data.record.TripEventEntity
import com.example.poco.data.record.TripEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripSummaryMapperTest {
    private val m = 60_000L
    private val trip = TripEntity(
        id = "trip-1", petName = "현댕이", petWeightTenths = 50, petSize = "SMALL", seat = "PASSENGER",
        startedAtEpoch = 0, endedAtEpoch = 154 * m, status = "COMPLETED", startRestIntervalMinutes = 120,
        finalScore = 88, minScore = 80, restCount = 1, restMs = 16 * m, rapidBrakingCount = 1, sharpTurnCount = 1,
        repeatedTurnCount = 2, speedBumpCount = 2, motionSicknessSigns = "YAWN:3,DROOL:1"
    )
    private fun e(min: Long, type: String, delta: Int? = null, detail: String? = null) =
        TripEventEntity(tripId = "trip-1", atEpoch = min * m, type = type, scoreDelta = delta, detail = detail)

    @Test fun mapsRecordWithoutLocationOrClockTimes() {
        val request = trip.toSummaryRequest(
            listOf(
                e(10, TripEventType.SCORE_PENALTY, -4, "SHARP_TURN"),
                e(30, TripEventType.TEMPERATURE_SAMPLE, detail = "23.0"),
                e(60, TripEventType.REST_STARTED),
                e(100, TripEventType.SCORE_PENALTY, -7, "RAPID_BRAKING")
            ),
            previous = trip.copy(id = "old", finalScore = 86)
        )!!
        val t = request.trip
        assertEquals(154, t.durationMinutes)
        assertEquals(86, t.previousScore)
        assertEquals(3, t.counts.sharpTurn) // 급회전 + 연속 커브
        assertEquals(listOf("SHARP_TURN", "REST", "RAPID_BRAKING"), t.events.map { it.kind })
        assertEquals(listOf(10, 60, 100), t.events.map { it.minute })
        assertEquals(setOf("YAWN", "DROOL"), t.motionSigns.toSet())
    }

    @Test fun noPreviousTripMeansNoComparison() {
        assertNull(trip.toSummaryRequest(emptyList(), null)!!.trip.previousScore)
    }

    @Test fun unfinishedTripIsNotSent() {
        assertNull(trip.copy(finalScore = null).toSummaryRequest(emptyList(), null))
    }

    @Test fun trimsToFortyEventsKeepingBiggestDrops() {
        val many = (1..60L).map { e(it, TripEventType.SCORE_PENALTY, if (it == 59L) -9 else -1, "SPEED_BUMP") }
        val events = trip.toSummaryRequest(many, null)!!.trip.events
        assertEquals(40, events.size)
        assertEquals(true, events.any { it.scoreDelta == -9 })
        assertEquals(events.sortedBy { it.minute }, events)
    }
}
