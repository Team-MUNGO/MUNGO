package com.example.poco.data.repository

interface SttRepository {
    /** [timeoutMs] 안에 한 번 듣는다. 말이 없으면 null */
    suspend fun listenOnce(timeoutMs: Long): Result<String?>

    /** AI 어시스턴트 질문용: 부분 인식 결과를 [onPartial]로 알린다. */
    suspend fun recognize(onPartial: (String) -> Unit): String
}
