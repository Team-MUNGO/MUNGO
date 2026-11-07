package com.example.poco.data.repositoryimpl

import com.example.poco.data.pleos.datasource.TtsDataSource
import com.example.poco.domain.repository.TtsRepository
import javax.inject.Inject

class TtsRepositoryImpl @Inject constructor(private val ttsDataSource: TtsDataSource) :
    TtsRepository
