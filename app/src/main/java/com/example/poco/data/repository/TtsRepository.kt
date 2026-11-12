package com.example.poco.data.repository

interface TtsRepository {
    suspend fun speak(text: String): Result<Unit>
}
