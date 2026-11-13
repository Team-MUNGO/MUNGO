package com.example.poco.data.repository

import com.example.poco.data.assistant.AssistResponse
import com.example.poco.data.assistant.TripSummaryRequest
import com.example.poco.data.assistant.AssistRequest

interface AssistantRepository {
    val configured: Boolean
    val connected: Boolean
    val anonymousAuth: Boolean
    suspend fun connect(invitation: String)
    suspend fun assist(request: AssistRequest): AssistResponse

    /** 주행 완료 "오늘 한 줄" (AI). 실패하면 예외 */
    suspend fun summarizeTrip(request: TripSummaryRequest): String =
        throw UnsupportedOperationException("summary not supported")

    /** 정보 전송 동의. 개발 단계에선 기본 true (구현 참고). 연결 해제 시 이번 실행 동안만 false */
    var consented: Boolean
    fun disconnect()
}
