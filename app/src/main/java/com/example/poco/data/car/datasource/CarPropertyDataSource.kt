package com.example.poco.data.car.datasource

import kotlinx.coroutines.flow.Flow

/** Android Automotive 표준 차량 속성 (Pleos SDK에 없거나 deprecated된 값) */
interface CarPropertyDataSource {
    /** 차량 속도 (m/s) */
    fun observeSpeedMps(): Flow<Float>

    /** 조향각 (도). Pleos 조향 API를 못 쓸 때 대체용 */
    fun observeSteeringAngle(): Flow<Float>

    /** 기어가 P인지 (GEAR_SELECTION). 알 수 없는 값은 건너뛴다 */
    fun observeIsParked(): Flow<Boolean>
}
