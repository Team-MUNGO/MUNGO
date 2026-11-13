package com.example.poco.core.driving

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeSamplerTest {
    @Test fun `10초마다 그 칸의 가장 큰 가속도를 남긴다`() {
        val s = ShakeSampler(startedAt = 0)
        s.add(1_000, 0.4)
        s.add(5_000, -1.8)
        s.add(9_000, 0.9)
        assertTrue(s.flush(9_999).isEmpty())
        assertEquals(listOf(ShakeSampler.Sample(10_000, 1.8)), s.flush(10_000))
        s.add(12_000, 0.3)
        assertEquals(listOf(ShakeSampler.Sample(20_000, 0.3)), s.flush(20_250))
    }

    @Test fun `값이 없던 칸은 건너뛴다`() {
        val s = ShakeSampler(startedAt = 0)
        s.add(1_000, 1.0)
        assertEquals(listOf(ShakeSampler.Sample(10_000, 1.0)), s.flush(45_000))
        // 40초 칸부터 다시 모은다
        s.add(41_000, 2.0)
        assertEquals(listOf(ShakeSampler.Sample(50_000, 2.0)), s.flush(50_000))
    }

    @Test fun `주행이 끝나면 채우다 만 칸도 남긴다`() {
        val s = ShakeSampler(startedAt = 0)
        s.add(2_000, 0.5)
        s.flush(10_000)
        s.add(13_000, 0.7)
        assertEquals(listOf(ShakeSampler.Sample(16_000, 0.7)), s.finish(16_000))
    }

    @Test fun `감지기는 계산한 앞뒤 가속도를 넘겨준다`() {
        val seen = mutableListOf<Double>()
        val d = DrivingEventDetector(motion = { _, mps2 -> seen += mps2 })
        d.onSpeed(SpeedSample(0.0, 0))
        d.onSpeed(SpeedSample(3.6, 1_000))
        assertEquals(1.0, seen.single(), 1e-6)
    }
}
