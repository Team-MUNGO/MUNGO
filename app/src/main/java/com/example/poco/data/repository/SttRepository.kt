package com.example.poco.data.repository

interface SttRepository {
    suspend fun recognize(onPartial: (String) -> Unit): String
}
