package com.example.poco.data.local.datasourceimpl

import com.example.poco.data.local.datasource.TripLocalDataSource
import com.example.poco.data.local.db.TripDao
import com.example.poco.data.local.db.toEntity
import com.example.poco.data.local.db.toModel
import com.example.poco.data.model.TripSummary
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TripLocalDataSourceImpl @Inject constructor(private val tripDao: TripDao) :
    TripLocalDataSource {
    override fun observeRecentTrips(limit: Int): Flow<List<TripSummary>> =
        tripDao.observeRecent(limit).map { entities -> entities.map { it.toModel() } }

    override suspend fun getTrip(id: Long): TripSummary? = tripDao.getById(id)?.toModel()

    override suspend fun getPreviousTrip(beforeMillis: Long): TripSummary? =
        tripDao.getPreviousBefore(beforeMillis)?.toModel()

    override suspend fun insertTrip(trip: TripSummary): Long = tripDao.insert(trip.toEntity())
}
