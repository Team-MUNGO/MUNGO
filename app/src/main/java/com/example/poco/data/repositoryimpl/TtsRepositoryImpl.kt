package com.example.poco.data.repositoryimpl

import com.example.poco.data.pleos.datasource.TtsDataSource
import com.example.poco.data.repository.TtsRepository
import javax.inject.Inject

class TtsRepositoryImpl @Inject constructor(private val source: TtsDataSource) : TtsRepository {
    override suspend fun speak(text: String, onSpeaking: (Boolean) -> Unit) =
        source.speak(text, onSpeaking)
}
