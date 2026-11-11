package com.example.poco.data.repositoryimpl

import com.example.poco.core.util.suspendRunCatching
import com.example.poco.data.model.NaviEvent
import com.example.poco.data.pleos.datasource.NaviDataSource
import com.example.poco.data.repository.NaviRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class NaviRepositoryImpl @Inject constructor(private val naviDataSource: NaviDataSource) :
    NaviRepository {
    override fun observeEvents(): Flow<NaviEvent> = naviDataSource.observeEvents()

    override suspend fun openNavigation(keyword: String?): Result<Unit> = suspendRunCatching {
        naviDataSource.openNavigation(keyword)
    }
}
