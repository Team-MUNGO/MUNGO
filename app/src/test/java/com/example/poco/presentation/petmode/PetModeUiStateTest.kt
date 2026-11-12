package com.example.poco.presentation.petmode

import com.example.poco.core.util.aYa
import com.example.poco.domain.driving.PocoStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetModeUiStateTest {
    @Test
    fun `부르는 말 - 받침 유무와 받침 뒤 이`() {
        assertEquals("초코야", "초코".aYa)
        assertEquals("콩아", "콩".aYa)
        assertEquals("현댕아", "현댕이".aYa)
        assertEquals("보리야", "보리".aYa)
    }

    @Test
    fun `안심 한마디`() {
        assertEquals("현댕아, 금방 올게", petModeMessage("현댕이"))
        assertEquals("금방 올게", petModeMessage(null))
    }

    @Test
    fun `배터리 - 1퍼센트 이상 줄어야 계산`() {
        assertNull(batteryHoursLeft(72, 0L, 72, 3_600_000L))
        // 1시간에 2% → 72%에서 20%까지 52% → 26시간
        assertEquals(26f, batteryHoursLeft(74, 0L, 72, 3_600_000L)!!, 0.01f)
    }

    @Test
    fun `펫모드 온도는 쾌적 범위 가운데 0점5도 단위`() {
        val standard = PocoStandard.temperature(emptySet())
        val target = petModeTargetTemperature(standard)
        val middle = (standard.comfort.start + standard.comfort.endInclusive) / 2f
        assertEquals(middle, target, 0.25f)
        assertEquals(0f, (target * 2) % 1f, 0.0001f)
    }
}
