package com.example.poco.data.repositoryimpl

import com.example.poco.core.driving.Reading
import com.example.poco.core.util.suspendRunCatching
import com.example.poco.data.model.NaviEvent
import com.example.poco.data.pleos.datasource.NaviDataSource
import com.example.poco.data.repository.NaviRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class NaviRepositoryImpl @Inject constructor(private val naviDataSource: NaviDataSource) :
    NaviRepository {
    override fun observeEvents(): Flow<NaviEvent> = naviDataSource.observeEvents()

    override suspend fun openNavigation(keyword: String?): Result<Unit> = suspendRunCatching {
        naviDataSource.openNavigation(keyword)
    }

    override fun observeBumps() = naviDataSource.observeBumps().catch {
        emit(Reading.Unavailable(it.message ?: "도로 주의 안내 연결 실패"))
    }

    // A Navi outage simply ends route tracking; waypoint guidance then stays silent (no guessed ETA).
    override fun observeRoute() = naviDataSource.observeRoute().catch { }
}
