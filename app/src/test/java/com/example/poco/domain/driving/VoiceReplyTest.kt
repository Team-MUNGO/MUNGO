package com.example.poco.domain.driving

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceReplyTest {
    @Test
    fun `괜찮아 류는 알림 닫기`() {
        assertEquals(VoiceReply.DECLINE, VoiceReply.parse("괜찮아"))
        assertEquals(VoiceReply.DECLINE, VoiceReply.parse("아니 괜찮아요"))
        assertEquals(VoiceReply.DECLINE, VoiceReply.parse("그냥 둬"))
        assertEquals(VoiceReply.DECLINE, VoiceReply.parse("됐어"))
    }

    @Test
    fun `창문 닫아 · 온도 낮춰 · 응은 제안 수락`() {
        assertEquals(VoiceReply.ACCEPT, VoiceReply.parse("창문 닫아"))
        assertEquals(VoiceReply.ACCEPT, VoiceReply.parse("닫아 줘"))
        assertEquals(VoiceReply.ACCEPT, VoiceReply.parse("응"))
        assertEquals(VoiceReply.ACCEPT, VoiceReply.parse("22도로 맞춰 줘"))
        assertEquals(VoiceReply.ACCEPT, VoiceReply.parse("온도 낮춰"))
    }

    @Test
    fun `5센티만 · 살짝 · 환기는 조금 열기`() {
        assertEquals(VoiceReply.VENT, VoiceReply.parse("5센티만 열어 둬"))
        assertEquals(VoiceReply.VENT, VoiceReply.parse("살짝만 열어"))
        assertEquals(VoiceReply.VENT, VoiceReply.parse("환기할래"))
    }

    @Test
    fun `알아듣지 못하면 아무것도 안 한다`() {
        assertEquals(VoiceReply.UNKNOWN, VoiceReply.parse(null))
        assertEquals(VoiceReply.UNKNOWN, VoiceReply.parse(""))
        assertEquals(VoiceReply.UNKNOWN, VoiceReply.parse("오늘 날씨 어때"))
    }
}
