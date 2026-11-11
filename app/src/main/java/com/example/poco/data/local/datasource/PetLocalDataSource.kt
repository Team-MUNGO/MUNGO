package com.example.poco.data.local.datasource

import com.example.poco.data.model.Pet
import kotlinx.coroutines.flow.Flow

interface PetLocalDataSource {
    fun observePets(): Flow<List<Pet>>

    /** @return 저장된 펫 id */
    suspend fun upsertPet(pet: Pet): Long
}
