package com.example.poco.data.repository

import com.example.poco.data.model.TripSummary
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    /** 가장 최근 이동. scoreDelta는 그 직전 이동과 비교한 값 */
    fun observeLastTrip(): Flow<TripSummary?>

    /** scoreDelta를 채워서 돌려준다 */
    suspend fun getTrip(id: Long): Result<TripSummary?>

    /** 직전 이동 점수 (점수 변화 계산용). 없으면 null */
    suspend fun getLatestScore(): Result<Int?>

    suspend fun saveTrip(trip: TripSummary): Result<Long>
}
