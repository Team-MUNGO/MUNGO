package com.example.poco.data.repository

import com.example.poco.data.assistant.AssistResponse
import com.example.poco.data.assistant.AssistRequest

interface AssistantRepository {
    val configured: Boolean
    val connected: Boolean
    val anonymousAuth: Boolean
    suspend fun connect(invitation: String)
    suspend fun assist(request: AssistRequest): AssistResponse

    /** Data-transfer consent; process memory only (asked again after restart). */
    var consented: Boolean
    fun disconnect()
}
