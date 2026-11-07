package com.example.poco.data.repositoryimpl

import com.example.poco.data.pleos.datasource.HvacDataSource
import com.example.poco.data.repository.HvacRepository
import javax.inject.Inject

class HvacRepositoryImpl @Inject constructor(private val hvacDataSource: HvacDataSource) :
    HvacRepository
