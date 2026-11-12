package com.example.poco.data.assistant

interface AssistantRemoteDataSource {
    val configured: Boolean
    val connected: Boolean
    val anonymousAuth: Boolean
    suspend fun connect(invitation: String)
    suspend fun assist(request: AssistRequest): AssistResponse
    fun disconnect()
}
