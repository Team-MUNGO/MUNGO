package com.example.poco.data.repositoryimpl

import com.example.poco.core.util.suspendRunCatching
import com.example.poco.data.pleos.datasource.SttDataSource
import com.example.poco.data.repository.SttRepository
import javax.inject.Inject
import kotlinx.coroutines.withTimeoutOrNull

class SttRepositoryImpl @Inject constructor(private val sttDataSource: SttDataSource) :
    SttRepository {
    override suspend fun listenOnce(timeoutMs: Long): Result<String?> = suspendRunCatching {
        withTimeoutOrNull(timeoutMs) { sttDataSource.listenOnce() }
    }

    override suspend fun recognize(onPartial: (String) -> Unit): String =
        sttDataSource.recognize(onPartial)
}
