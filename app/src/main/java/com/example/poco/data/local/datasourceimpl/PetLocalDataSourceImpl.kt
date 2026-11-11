package com.example.poco.data.local.datasourceimpl

import com.example.poco.data.local.datasource.PetLocalDataSource
import com.example.poco.data.local.db.PetDao
import com.example.poco.data.local.db.toEntity
import com.example.poco.data.local.db.toModel
import com.example.poco.data.model.Pet
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PetLocalDataSourceImpl @Inject constructor(private val petDao: PetDao) : PetLocalDataSource {
    override fun observePets(): Flow<List<Pet>> =
        petDao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun upsertPet(pet: Pet): Long {
        val rowId = petDao.upsert(pet.toEntity())
        // Upsert가 update로 처리되면 -1을 돌려준다
        return if (rowId == -1L) pet.id else rowId
    }
}
