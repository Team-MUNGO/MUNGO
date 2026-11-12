package com.example.poco.data.pleos.datasource

interface TtsDataSource {
    suspend fun speak(text: String)
}
