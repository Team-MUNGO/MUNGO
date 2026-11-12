package com.example.poco.data.repository

import com.example.poco.data.model.VehicleLocation
import kotlinx.coroutines.flow.Flow

/** 주행 중 흔들림 판단에 쓰는 차량 움직임 */
interface VehicleMotionRepository {
    /** 차량 속도 (km/h) */
    fun observeSpeedKmh(): Flow<Float>

    /** 핸들 조향각 (도) */
    fun observeSteeringAngle(): Flow<Float>

    /** 브레이크 페달을 밟고 있는지 */
    fun observeBrakePedalPressed(): Flow<Boolean>

    /** 위치 · 진행 방향 (회전 시 횡가속도 계산) */
    fun observeLocation(): Flow<VehicleLocation>
}
