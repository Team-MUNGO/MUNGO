package com.example.poco.data.pleos.datasource

import kotlinx.coroutines.flow.Flow

interface BatteryDataSource {
    /** 전기차 배터리 잔량(%) */
    suspend fun getStateOfCharge(): Int?

    /** 전기차 배터리 잔량(%) 변화 */
    fun observeStateOfCharge(): Flow<Int>
}
