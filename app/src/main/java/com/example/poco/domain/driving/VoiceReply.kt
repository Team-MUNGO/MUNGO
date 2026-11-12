package com.example.poco.domain.driving

/** 주행 중 알림에 대한 운전자의 말 대답 */
enum class VoiceReply {
    /** "응", "낮춰 줘", "22도로 맞춰" → 알림이 제안한 동작 (온도 맞추기 · 창문 닫기) */
    ACCEPT,

    /** "5센티만", "살짝 열어 둬", "환기" → 창문 5cm만 열기 */
    VENT,

    /** "괜찮아", "됐어", "아니" → 알림 닫기 */
    DECLINE,

    /** 알아듣지 못함 → 아무것도 하지 않음 (화면 버튼은 그대로) */
    UNKNOWN;

    companion object {
        private val VENT_WORDS = listOf("5센티", "오센티", "5cm", "조금만", "살짝", "환기", "열어둬", "열어놔")
        private val DECLINE_WORDS =
            listOf("괜찮", "됐어", "됐다", "아니", "놔둬", "놔두", "그냥둬", "그대로", "싫어", "취소")
        private val ACCEPT_WORDS = listOf(
            "닫아", "닫자", "낮춰", "낮추", "내려", "올려", "높여", "맞춰", "22도", "이십이도",
            "그래", "좋아", "해줘", "부탁"
        )

        /** 한 글자 대답은 다른 말 속에 섞이기 쉬워서("어때") 대답 전체가 이것일 때만 수락 */
        private val SHORT_ACCEPT = setOf("응", "어", "네", "예", "웅", "넵", "네네", "응응", "그래요", "네에")

        /**
         * 띄어쓰기를 무시하고 단어로 판단한다.
         * "아니 괜찮아"처럼 거절이 먼저 오거나, "5센티만 열어"처럼 구체적인 말이 있으면 그쪽을 먼저 본다.
         */
        fun parse(text: String?): VoiceReply {
            val normalized = text?.replace(
                " ",
                ""
            )?.lowercase()?.trimEnd('.', '!', '?', '~').orEmpty()
            if (normalized.isEmpty()) return UNKNOWN
            return when {
                VENT_WORDS.any { normalized.contains(it) } -> VENT
                DECLINE_WORDS.any { normalized.contains(it) } -> DECLINE
                normalized in SHORT_ACCEPT -> ACCEPT
                ACCEPT_WORDS.any { normalized.contains(it) } -> ACCEPT
                else -> UNKNOWN
            }
        }
    }
}
