package com.example.poco.data.pleos.datasource

import kotlinx.coroutines.flow.Flow

interface BrakeDataSource {
    /** 브레이크 페달을 밟고 있는지 (Vehicle SDK · Brake) */
    fun observeBrakePedalPressed(): Flow<Boolean>
}
