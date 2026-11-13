package com.example.poco.data.pleos.datasource

interface SttDataSource {
    /**
     * 한 번 듣고 최종 인식 문장을 돌려준다 (SpeechToText SDK · ON_DEVICE · 한국어).
     * 말이 없거나 인식에 실패하면 null.
     */
    suspend fun listenOnce(): String?

    /** AI 어시스턴트 질문용: 부분 인식 결과를 [onPartial]로 알리고, 실패하면 예외를 던진다. */
    suspend fun recognize(onPartial: (String) -> Unit): String
}
