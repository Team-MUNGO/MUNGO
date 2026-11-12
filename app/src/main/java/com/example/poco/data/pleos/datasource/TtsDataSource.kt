package com.example.poco.data.pleos.datasource

interface TtsDataSource {
    suspend fun speak(text: String)

    /** 말이 끝날 때까지 기다린다. 말하는 동안 [onSpeaking]이 true. (주행 세션 안내 우선순위용) */
    suspend fun speak(text: String, onSpeaking: (Boolean) -> Unit)
}
