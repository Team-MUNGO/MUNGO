package com.example.poco

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.poco.data.record.*
import com.example.poco.presentation.record.timelineItems
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TripPersistenceTest {
    @Test fun completionReadsAllPreviouslySubmittedEvents() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, TripDatabase::class.java).build()
        try {
            val recorder = TripRecorder(db.trips())
            val start = 1_700_000_000_000L
            recorder.begin(TripEntity("persist-test", "현댕이", null, null, "PASSENGER", start,
                startRestIntervalMinutes = 120))
            repeat(50) { i ->
                recorder.event(TripEventEntity(tripId = "persist-test", atEpoch = start + i * 1000,
                    type = TripEventType.SCORE_PENALTY, scoreDelta = -1, scoreAfter = 99 - i, detail = "RAPID_BRAKING",
                    scoreBefore = 100 - i, latitude = 37.0, longitude = 127.0, locationAtEpoch = start + i * 1000,
                    temperatureC = 23f, windowPercent = 0.0, seat = "PASSENGER", guidanceText = "천천히 가요"))
            }
            recorder.event(TripEventEntity(tripId = "persist-test", atEpoch = start + 60_000, type = TripEventType.REST_STARTED))
            recorder.event(TripEventEntity(tripId = "persist-test", atEpoch = start + 1_020_000,
                type = TripEventType.REST_COMPLETED, detail = "960000"))
            recorder.event(TripEventEntity(tripId = "persist-test", atEpoch = start + 1_030_000,
                type = TripEventType.WINDOW_WARNING, detail = "50"))
            assertTrue(recorder.complete("persist-test", start + 1_200_000, 50, 240_000, 960_000, 1))
            val trip = recorder.trip("persist-test").first()!!
            val events = recorder.events("persist-test").first()
            assertEquals(53, events.size)
            val saved = events.first()
            assertEquals(100, saved.scoreBefore)
            assertEquals(99, saved.scoreAfter)
            assertEquals(37.0, saved.latitude!!, 0.0)
            assertEquals(127.0, saved.longitude!!, 0.0)
            assertEquals(23f, saved.temperatureC!!, 0f)
            assertEquals(0.0, saved.windowPercent!!, 0.0)
            assertEquals("PASSENGER", saved.seat)
            assertEquals("천천히 가요", saved.guidanceText)
            assertEquals(50, trip.rapidBrakingCount)
            assertEquals(1, trip.windowWarningCount)
            assertEquals(1, trip.restCount)
            assertEquals("COMPLETED", trip.status)
            assertTrue(timelineItems(trip, events).any { it.title == "휴식 16분" })
            assertFalse(recorder.complete("persist-test", start + 1_200_000, 0, 0, 0, 0))
        } finally { db.close() }
    }
}
