package com.example.poco.data.repository

import com.example.poco.core.driving.PetSeat as SessionSeat
import com.example.poco.core.driving.Reading
import com.example.poco.data.model.PetSeat
import kotlinx.coroutines.flow.Flow

interface HvacRepository {
    /** 반려동물 자리의 설정 온도(℃). 그 자리 공조가 없으면 가까운 존 온도로 대신한다 */
    suspend fun getSeatTemperature(seat: PetSeat): Result<Float?>

    /** 반려동물 자리 온도를 실시간으로. 실내 온도를 못 읽으면 설정 온도로 대신한다 */
    fun observeSeatTemperature(seat: PetSeat): Flow<Float>

    /** 반려동물 자리(또는 가까운 존) 설정 온도 변경 */
    suspend fun setSeatTemperature(seat: PetSeat, celsius: Float): Result<Float?>

    /** 주행 세션용 실내 온도 (Reading으로 미지원/오류를 구분) */
    fun observeInteriorTemperature(seat: SessionSeat): Flow<Reading<Float>>

    /** 공기 청정 켜짐/꺼짐 (미지원 · 오류는 Unavailable) */
    fun observeAirCleaner(): Flow<Reading<Boolean>>

    /** 공기 청정 켜기/끄기. 차가 적용한 값을 돌려준다 */
    suspend fun setAirCleaner(on: Boolean): Result<Boolean?>
}
