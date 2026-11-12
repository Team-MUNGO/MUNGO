package com.example.poco.data.local.datasource

import com.example.poco.data.model.TripSummary
import kotlinx.coroutines.flow.Flow

interface TripLocalDataSource {
    /** 최신순 */
    fun observeRecentTrips(limit: Int): Flow<List<TripSummary>>

    suspend fun getTrip(id: Long): TripSummary?

    /** [beforeMillis]보다 먼저 시작한 이동 중 가장 최근 것 */
    suspend fun getPreviousTrip(beforeMillis: Long): TripSummary?

    suspend fun insertTrip(trip: TripSummary): Long
}
