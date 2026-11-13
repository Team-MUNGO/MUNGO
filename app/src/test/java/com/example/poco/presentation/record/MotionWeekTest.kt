package com.example.poco.presentation.record

import com.example.poco.data.record.MotionSign
import com.example.poco.data.record.TripEntity
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionWeekTest {
    private val zone = ZoneOffset.UTC

    private fun trip(id: String, date: LocalDate, minutes: Long, signs: String?) = TripEntity(
        id = id,
        petName = "dd",
        petWeightTenths = null,
        petSize = null,
        seat = "REAR_LEFT",
        startedAtEpoch = date.atEpoch(zone) + 9 * 3_600_000L,
        endedAtEpoch = date.atEpoch(zone) + 9 * 3_600_000L + minutes * 60_000,
        status = "COMPLETED",
        startRestIntervalMinutes = 120,
        drivingMs = minutes * 60_000,
        motionSicknessSigns = signs
    )

    @Test
    fun `7일 표 - 이동 수 · 징후가 보인 이동 · 가장 자주 보인 징후`() {
        val end = LocalDate.of(2026, 11, 10)
        val trips = listOf(
            trip("a", LocalDate.of(2026, 11, 5), 65, "YAWN:1"),
            trip("b", LocalDate.of(2026, 11, 8), 25, "FINE"),
            trip("c", LocalDate.of(2026, 11, 9), 80, "YAWN:2"),
            trip("d", end, 154, "YAWN:1,DROOL:1"),
            trip("old", LocalDate.of(2026, 11, 3), 30, "VOMIT:1")
        )
        val week = motionWeek(trips, end, zone)

        assertEquals(LocalDate.of(2026, 11, 4), week.start)
        assertEquals(4, week.tripCount)
        assertEquals(3, week.signTrips)
        assertEquals(MotionSign.YAWN to 3, week.top)
        assertEquals(1, week.perSign[MotionSign.DROOL])
        assertEquals(0, week.perSign[MotionSign.VOMIT])
        val tue = week.days.last()
        assertEquals(154 * 60_000L, tue.drivingMs)
        assertEquals(setOf(MotionSign.YAWN, MotionSign.DROOL), tue.signs)
        val sun = week.days.first { it.date == LocalDate.of(2026, 11, 8) }
        assertTrue(sun.recorded)
        assertTrue(sun.signs.isEmpty())
        assertEquals(0, week.days.first().trips)
    }

    @Test
    fun `기록하지 않은 이동은 칸을 비운다`() {
        val day = LocalDate.of(2026, 11, 10)
        val week = motionWeek(listOf(trip("a", day, 30, null)), day, zone)
        assertFalse(week.days.last().recorded)
        assertEquals(null, week.top)
    }
}
