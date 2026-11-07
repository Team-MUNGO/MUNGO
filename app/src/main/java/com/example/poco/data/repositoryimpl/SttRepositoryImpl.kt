package com.example.poco.data.repositoryimpl

import com.example.poco.data.pleos.datasource.SttDataSource
import com.example.poco.data.repository.SttRepository
import javax.inject.Inject

class SttRepositoryImpl @Inject constructor(private val sttDataSource: SttDataSource) :
    SttRepository
