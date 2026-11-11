package com.example.poco.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startedAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: Long): TripEntity?

    @Query(
        "SELECT * FROM trips WHERE startedAtMillis < :before ORDER BY startedAtMillis DESC LIMIT 1"
    )
    suspend fun getPreviousBefore(before: Long): TripEntity?

    @Insert
    suspend fun insert(trip: TripEntity): Long
}
