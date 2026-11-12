package com.example.poco.data.pleos.datasource

interface SttDataSource {
    suspend fun recognize(onPartial: (String) -> Unit): String
}
