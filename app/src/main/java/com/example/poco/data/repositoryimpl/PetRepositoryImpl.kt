package com.example.poco.data.repositoryimpl

import com.example.poco.core.util.suspendRunCatching
import com.example.poco.data.local.datasource.PetLocalDataSource
import com.example.poco.data.local.datasource.PetPhotoDataSource
import com.example.poco.data.model.Pet
import com.example.poco.data.repository.PetRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class PetRepositoryImpl @Inject constructor(
    private val petLocalDataSource: PetLocalDataSource,
    private val petPhotoDataSource: PetPhotoDataSource
) : PetRepository {
    override fun observePets(): Flow<List<Pet>> = petLocalDataSource.observePets()

    override suspend fun savePet(pet: Pet): Result<Long> =
        suspendRunCatching { petLocalDataSource.upsertPet(pet) }

    override suspend fun importPhoto(uri: String): Result<String> =
        suspendRunCatching { petPhotoDataSource.importPhoto(uri) }

    override suspend fun deletePhoto(path: String): Result<Unit> =
        suspendRunCatching { petPhotoDataSource.deletePhoto(path) }
}
