package com.example.poco.data.pleos.datasource

import ai.pleos.playground.vehicle.api.model.area.SeatArea
import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.Reading
import kotlinx.coroutines.flow.Flow

interface HvacDataSource {
    /** 해당 좌석의 설정 온도(℃) */
    suspend fun getTemperature(area: SeatArea): Float?

    /** 해당 좌석 설정 온도 변경(℃). 적용된 값을 돌려준다 */
    suspend fun setTemperature(area: SeatArea, celsius: Float): Float?

    /** 해당 좌석의 현재 실내 온도(℃) */
    fun observeCurrentTemperature(area: SeatArea): Flow<Float>

    /** 해당 좌석의 설정 온도(℃) */
    fun observeTemperature(area: SeatArea): Flow<Float>

    /** 주행 세션용: 반려견 자리 실내 온도. 읽을 수 없으면 Unavailable (추정값을 만들지 않는다). */
    fun observeInteriorTemperature(seat: PetSeat): Flow<Reading<Float>>

    /** 공기 청정 켜짐/꺼짐. 차가 지원하지 않거나 읽을 수 없으면 Unavailable */
    fun observeAirCleaner(): Flow<Reading<Boolean>>

    /** 공기 청정 켜기/끄기. 차가 알려 준 적용 값을 돌려준다 */
    suspend fun setAirCleaner(on: Boolean): Boolean?
}
