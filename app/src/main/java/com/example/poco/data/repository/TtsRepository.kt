package com.example.poco.data.repository

interface TtsRepository {
    suspend fun speak(text: String): Result<Unit>

    /** 말이 끝날 때까지 기다린다. 말하는 동안 [onSpeaking]이 true. */
    suspend fun speak(text: String, onSpeaking: (Boolean) -> Unit)
}
