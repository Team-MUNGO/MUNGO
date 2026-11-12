package com.example.poco.data.repositoryimpl

import com.example.poco.data.assistant.AssistantRemoteDataSource
import com.example.poco.data.assistant.AssistRequest
import com.example.poco.data.repository.AssistantRepository
import javax.inject.Inject

class AssistantRepositoryImpl @Inject constructor(private val source: AssistantRemoteDataSource) :
    AssistantRepository {
    override val configured get() = source.configured
    override val connected get() = source.connected
    override val anonymousAuth get() = source.anonymousAuth
    override suspend fun connect(invitation: String) = source.connect(invitation)
    override suspend fun assist(request: AssistRequest) = source.assist(request)
    @Volatile override var consented = false
    override fun disconnect() = source.disconnect()
}
