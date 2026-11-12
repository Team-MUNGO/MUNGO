package com.example.poco.data.pleos.datasource

interface SttDataSource {
    /**
     * 한 번 듣고 최종 인식 문장을 돌려준다 (SpeechToText SDK · ON_DEVICE · 한국어).
     * 말이 없거나 인식에 실패하면 null.
     */
    suspend fun listenOnce(): String?
}
