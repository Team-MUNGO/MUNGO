package com.example.poco.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pets ORDER BY id")
    fun observeAll(): Flow<List<PetEntity>>

    @Upsert
    suspend fun upsert(pet: PetEntity): Long
}
