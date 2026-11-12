package com.example.poco.data.repository

import kotlinx.coroutines.flow.Flow

interface BatteryRepository {
    /** 배터리 잔량(%). 지금 값부터 바뀔 때마다. 못 읽으면 아무것도 안 보낸다 */
    fun observeBatteryPercent(): Flow<Int>
}
