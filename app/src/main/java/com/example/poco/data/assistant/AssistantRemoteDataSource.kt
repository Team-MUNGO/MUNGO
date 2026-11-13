package com.example.poco.data.assistant

interface AssistantRemoteDataSource {
    val configured: Boolean
    val connected: Boolean
    val anonymousAuth: Boolean
    suspend fun connect(invitation: String)
    suspend fun assist(request: AssistRequest): AssistResponse

    /** 주행 기록 → AI "오늘 한 줄" */
    suspend fun summarizeTrip(request: TripSummaryRequest): String =
        throw AssistantException("NOT_CONFIGURED", "AI 서버 연결이 아직 설정되지 않았어요.")
    fun disconnect()
}
