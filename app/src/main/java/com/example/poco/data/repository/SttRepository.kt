package com.example.poco.data.repository

interface SttRepository {
    /** [timeoutMs] 안에 한 번 듣는다. 말이 없으면 null */
    suspend fun listenOnce(timeoutMs: Long): Result<String?>
}
