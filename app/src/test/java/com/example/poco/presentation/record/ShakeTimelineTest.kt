package com.example.poco.presentation.record

import com.example.poco.data.record.TripEventEntity
import com.example.poco.data.record.TripEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeTimelineTest {
    private val m = 60_000L

    // 분 경계에 맞춘 시각 (시간대와 상관없이 :00초)
    private val base = 1_700_000_000_000L / m * m

    @Test
    fun `1분 안에 끝난 주행은 경과 초로 보여 준다`() {
        assertEquals(listOf("0초", "20초", "40초"), axisLabels(base, base + 40_000L))
    }

    @Test
    fun `거의 바로 끝난 주행은 라벨 하나만`() {
        assertEquals(listOf("0초"), axisLabels(base, base + 500L))
    }

    @Test
    fun `중간 시각이 출발과 같으면 중간 라벨을 뺀다`() {
        val start = base + 10_000L
        val labels = axisLabels(start, start + 70_000L)
        assertEquals(listOf(clockLabel(start), clockLabel(start + 70_000L)), labels)
    }

    @Test
    fun `2분 주행은 시각 세 개`() {
        val labels = axisLabels(base, base + 2 * m)
        assertEquals(3, labels.size)
        assertEquals(3, labels.toSet().size)
    }

    @Test
    fun `같은 순간의 막대는 최소 간격만큼 벌린다`() {
        val out = spreadBars(listOf(100f, 100f, 100f), minGap = 12f, minX = 4f, maxX = 1000f)
        assertEquals(listOf(100f, 112f, 124f), out)
    }

    @Test
    fun `오른쪽 끝에 몰린 막대는 안쪽으로 당긴다`() {
        val out = spreadBars(listOf(996f, 996f, 996f), minGap = 12f, minX = 4f, maxX = 996f)
        assertEquals(listOf(972f, 984f, 996f), out)
    }

    @Test
    fun `칸보다 많으면 균등하게 나눈다`() {
        val out = spreadBars(List(5) { 50f }, minGap = 40f, minX = 0f, maxX = 100f)
        assertEquals(listOf(0f, 25f, 50f, 75f, 100f), out)
    }

    @Test
    fun `떨어진 막대는 그대로 둔다`() {
        val raw = listOf(10f, 200f, 400f, 401f)
        val out = spreadBars(raw, minGap = 12f, minX = 4f, maxX = 1000f)
        assertEquals(listOf(10f, 200f, 400f, 412f), out)
        assertTrue(out.zipWithNext().all { (a, b) -> b - a >= 12f })
    }

    private fun shake(at: Long, peak: String) = TripEventEntity(
        tripId = "t",
        atEpoch = at,
        type = TripEventType.SHAKE_SAMPLE,
        detail = peak
    )

    @Test
    fun `휴식 칸과 감점이 있던 10초는 회색 막대에서 뺀다`() {
        val events = listOf(
            shake(10_000, "0.50"),
            shake(20_000, "2.80"),
            shake(30_000, "0.20"),
            shake(40_000, "oops")
        )
        val out =
            shakeSamples(events, rests = listOf(25_000L to 60_000L), penaltyTimes = listOf(15_000L))
        assertEquals(listOf(10_000L to 0.5f), out)
    }
}
