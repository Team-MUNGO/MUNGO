package com.example.poco.data.pleos.datasource

import ai.pleos.playground.vehicle.api.model.area.SeatArea
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
}
