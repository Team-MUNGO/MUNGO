package com.example.poco.data.repositoryimpl

import com.example.poco.core.util.suspendRunCatching
import com.example.poco.data.local.datasource.TripLocalDataSource
import com.example.poco.data.model.TripStats
import com.example.poco.data.model.TripSummary
import com.example.poco.data.repository.TripRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TripRepositoryImpl @Inject constructor(private val tripLocalDataSource: TripLocalDataSource) :
    TripRepository {
    override fun observeLastTrip(): Flow<TripSummary?> =
        tripLocalDataSource.observeRecentTrips(limit = 2).map { trips ->
            val last = trips.getOrNull(0) ?: return@map null
            last.copy(scoreDelta = trips.getOrNull(1)?.let { last.comfortScore - it.comfortScore })
        }

    override fun observeTripStartsSince(sinceMillis: Long): Flow<List<Long>> =
        tripLocalDataSource.observeRecentTrips(limit = MAX_WEEK_TRIPS).map { trips ->
            trips.map { it.startedAtMillis }.filter { it >= sinceMillis }
        }

    override fun observeTripStats(): Flow<TripStats> =
        tripLocalDataSource.observeRecentTrips(limit = MAX_STATS_TRIPS).map { trips ->
            TripStats(
                count = trips.size,
                averageScore = trips.takeIf { it.isNotEmpty() }
                    ?.let { list -> list.sumOf { it.comfortScore } / list.size },
                lastStartedAtMillis = trips.maxOfOrNull { it.startedAtMillis }
            )
        }

    override suspend fun getTrip(id: Long): Result<TripSummary?> = suspendRunCatching {
        val trip = tripLocalDataSource.getTrip(id) ?: return@suspendRunCatching null
        val previous = tripLocalDataSource.getPreviousTrip(trip.startedAtMillis)
        trip.copy(scoreDelta = previous?.let { trip.comfortScore - it.comfortScore })
    }

    override suspend fun getLatestScore(): Result<Int?> = suspendRunCatching {
        tripLocalDataSource.observeRecentTrips(limit = 1).first().firstOrNull()?.comfortScore
    }

    override suspend fun saveTrip(trip: TripSummary): Result<Long> =
        suspendRunCatching { tripLocalDataSource.insertTrip(trip) }

    private companion object {
        /** 일주일 동안 이보다 많이 다닐 일은 없다고 본다 */
        const val MAX_WEEK_TRIPS = 200

        /** 통계는 최근 이만큼까지 */
        const val MAX_STATS_TRIPS = 1_000
    }
}
