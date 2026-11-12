package com.example.poco.data.repository

import com.example.poco.data.model.Pet
import kotlinx.coroutines.flow.Flow

interface PetRepository {
    fun observePets(): Flow<List<Pet>>

    suspend fun savePet(pet: Pet): Result<Long>

    /** 펫 삭제. 앱에 복사해 둔 사진 파일도 지운다 */
    suspend fun deletePet(pet: Pet): Result<Unit>

    /** 사진 선택기 URI → 앱 내부 파일로 복사 (줄이고 회전 보정). 저장된 경로를 돌려준다 */
    suspend fun importPhoto(uri: String): Result<String>

    suspend fun deletePhoto(path: String): Result<Unit>
}
