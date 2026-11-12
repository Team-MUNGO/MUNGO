package com.example.poco.domain.driving

import com.example.poco.data.model.ShakeLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ComfortTrackerTest {
    private val start = 0L

    @Test
    fun `급제동은 5점 감점된다`() {
        val tracker = ComfortTracker(start)
        tracker.onSpeed(0, 60f)
        // 1초 만에 60 → 40 km/h = 약 −5.6 m/s²
        val event = tracker.onSpeed(1_000, 40f)

        assertEquals(DrivingEventType.HARD_BRAKE, event?.type)
        assertEquals(95, tracker.score)
    }

    @Test
    fun `완만한 감속은 사건이 아니다`() {
        val tracker = ComfortTracker(start)
        tracker.onSpeed(0, 60f)

        assertNull(tracker.onSpeed(1_000, 55f))
        assertEquals(100, tracker.score)
    }

    @Test
    fun `살짝 불편(1_5~2_5 m·s²)은 감점 없이 횟수만 센다`() {
        val tracker = ComfortTracker(start)
        tracker.onSpeed(0, 30f)
        // 1초에 8 km/h = 2.2 m/s²
        assertNull(tracker.onSpeed(1_000, 38f))

        assertEquals(100, tracker.score)
        assertEquals(1, tracker.mildShakeCount)
    }

    @Test
    fun `멀미 민감도 높음(×0_8)이면 같은 흔들림도 불편으로 감점`() {
        // 2.0 × 0.8 = 기준 2.0 m/s² → 2.2 m/s²는 불편
        val tracker = ComfortTracker(start, thresholdFactor = 0.8f)
        tracker.onSpeed(0, 30f)
        val event = tracker.onSpeed(1_000, 38f)

        assertEquals(DrivingEventType.HARD_ACCEL, event?.type)
        assertEquals(95, tracker.score)
    }

    @Test
    fun `4_0 m·s² 이상은 위험으로 표시한다`() {
        val tracker = ComfortTracker(start)
        tracker.onSpeed(0, 60f)
        // 1초에 20 km/h = 5.6 m/s²
        assertEquals(true, tracker.onSpeed(1_000, 40f)?.isDanger)

        tracker.onSpeed(10_000, 40f)
        // 1초에 10 km/h = 2.8 m/s² → 불편이지만 위험은 아님
        assertEquals(false, tracker.onSpeed(11_000, 30f)?.isDanger)
    }

    @Test
    fun `급회전은 핸들이 돌아오기 전까지 한 번만 센다`() {
        val tracker = ComfortTracker(start)
        tracker.onSpeed(0, 40f)

        assertNotNull(tracker.onSteering(100, 120f))
        assertNull(tracker.onSteering(10_000, 130f))
        tracker.onSteering(11_000, 10f)
        assertNotNull(tracker.onSteering(12_000, -100f))
        assertEquals(2, tracker.count(DrivingEventType.SHARP_TURN))
    }

    @Test
    fun `저속 회전(주차 등)은 급회전이 아니다`() {
        val tracker = ComfortTracker(start)
        tracker.onSpeed(0, 5f)

        assertNull(tracker.onSteering(100, 300f))
    }

    @Test
    fun `5분 평온하면 2점 회복하고 100점을 넘지 않는다`() {
        val tracker = ComfortTracker(start)
        tracker.onSpeed(0, 60f)
        tracker.onSpeed(1_000, 40f) // 95점

        assertEquals(false, tracker.onTick(1_000 + 4 * 60_000L))
        assertEquals(true, tracker.onTick(1_000 + 5 * 60_000L))
        assertEquals(97, tracker.score)
        repeat(10) { tracker.onTick(1_000 + (6 + it * 5) * 60_000L) }
        assertEquals(100, tracker.score)
    }

    @Test
    fun `표정 - 온도 이탈이면 더위, 최근 사건이 있으면 어지러움`() {
        val tracker = ComfortTracker(start)
        assertEquals(PetMood.CALM, tracker.mood(1_000, isTemperatureOff = false))
        assertEquals(PetMood.HOT, tracker.mood(1_000, isTemperatureOff = true))

        tracker.onSpeed(0, 60f)
        tracker.onSpeed(1_000, 40f)
        assertEquals(PetMood.DIZZY, tracker.mood(30_000, isTemperatureOff = false))
        assertEquals(PetMood.HAPPY, tracker.mood(1_000 + 11 * 60_000L, isTemperatureOff = false))
    }

    @Test
    fun `타임라인은 사건이 있던 칸을 표시한다`() {
        val tracker = ComfortTracker(start)
        tracker.onSpeed(0, 60f)
        tracker.onSpeed(1_000, 40f)

        val timeline = tracker.timeline(nowMillis = 20_000, bars = 20)
        assertEquals(20, timeline.size)
        assertEquals(ShakeLevel.BRAKE, timeline[1].level)
        assertEquals(ShakeLevel.CALM, timeline[10].level)
    }

    @Test
    fun `브레이크 신호가 있으면 페달 없이 줄어든 속도는 급제동이 아니다`() {
        val tracker = ComfortTracker(start)
        tracker.onBrakePedal(0, pressed = false)
        tracker.onSpeed(0, 60f)

        // 내리막 엔진브레이크 · 센서 튐 등
        assertNull(tracker.onSpeed(1_000, 40f))

        tracker.onBrakePedal(1_500, pressed = true)
        tracker.onSpeed(2_000, 40f)
        assertEquals(DrivingEventType.HARD_BRAKE, tracker.onSpeed(3_000, 20f)?.type)
    }

    @Test
    fun `진행 방향 변화로 횡가속도가 크면 급회전이다`() {
        val tracker = ComfortTracker(start)
        tracker.onSpeed(0, 36f) // 10 m/s
        tracker.onHeading(0, 350f)

        // 1초에 30° 회전 (350° → 20°, 북쪽 경계 통과) = 10 m/s × 0.52 rad/s ≈ 5.2 m/s²
        val event = tracker.onHeading(1_000, 20f)

        assertEquals(DrivingEventType.SHARP_TURN, event?.type)
    }

    @Test
    fun `완만한 차선 변경은 급회전이 아니다`() {
        val tracker = ComfortTracker(start)
        tracker.onSpeed(0, 36f)
        tracker.onHeading(0, 90f)

        // 1초에 5° = 10 m/s × 0.087 rad/s ≈ 0.9 m/s²
        assertNull(tracker.onHeading(1_000, 95f))
    }

    @Test
    fun `내비가 예고한 구간의 급제동은 감점 절반`() {
        val tracker = ComfortTracker(start)
        tracker.roadContext = RoadContext(expectsBraking = true)
        tracker.onSpeed(0, 60f)
        val event = tracker.onSpeed(1_000, 40f)

        assertEquals(true, event?.isAnticipated)
        // 100 − 2.5 = 97.5 → 98 (반올림), 리포트 감점도 −2로 맞춰진다
        assertEquals(98, tracker.score)
        assertEquals(-2, tracker.stat(DrivingEventType.HARD_BRAKE).points)
    }

    @Test
    fun `예고 구간을 부드럽게 지나가면 가점은 회복 기록에 합쳐진다`() {
        val tracker = ComfortTracker(start)
        tracker.onSpeed(0, 60f)
        tracker.onSpeed(1_000, 40f) // 95
        tracker.onSmoothPass() // 96

        assertEquals(96, tracker.score)
        assertEquals(1, tracker.recoveryStat().count)
        assertEquals(1, tracker.recoveryStat().points)
    }

    @Test
    fun `방향 차이는 북쪽 경계를 넘어도 짧은 쪽으로 계산한다`() {
        assertEquals(20f, ComfortTracker.angleDiff(10f, 350f), 0.001f)
        assertEquals(-20f, ComfortTracker.angleDiff(350f, 10f), 0.001f)
    }

    @Test
    fun `이동 거리는 속도를 적분한다`() {
        val tracker = ComfortTracker(start)
        // 36 km/h로 1초씩 100초 = 1 km
        repeat(101) { tracker.onSpeed(it * 1_000L, 36f) }

        assertEquals(1f, tracker.totalDistanceKm, 0.01f)
    }
}
