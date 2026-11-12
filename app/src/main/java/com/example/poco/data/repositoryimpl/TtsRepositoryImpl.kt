package com.example.poco.data.repositoryimpl

import com.example.poco.core.util.suspendRunCatching
import com.example.poco.data.pleos.datasource.TtsDataSource
import com.example.poco.data.repository.TtsRepository
import javax.inject.Inject

class TtsRepositoryImpl @Inject constructor(private val ttsDataSource: TtsDataSource) :
    TtsRepository {
    override suspend fun speak(text: String): Result<Unit> =
        suspendRunCatching { ttsDataSource.speak(text) }

    override suspend fun speak(text: String, onSpeaking: (Boolean) -> Unit) =
        ttsDataSource.speak(text, onSpeaking)
}
