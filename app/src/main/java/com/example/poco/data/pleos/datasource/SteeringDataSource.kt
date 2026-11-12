package com.example.poco.data.pleos.datasource

import kotlinx.coroutines.flow.Flow

interface SteeringDataSource {
    /** 핸들 조향각 (도) */
    fun observeSteeringAngle(): Flow<Float>
}
