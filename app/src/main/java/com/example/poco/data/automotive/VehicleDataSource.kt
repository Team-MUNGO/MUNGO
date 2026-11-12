package com.example.poco.data.automotive

import com.example.poco.core.driving.DrivingGear
import com.example.poco.core.driving.Reading
import com.example.poco.core.driving.SpeedSample
import kotlinx.coroutines.flow.Flow

sealed interface VehicleUpdate {
    data class Speed(val reading: Reading<SpeedSample>) : VehicleUpdate
    data class Gear(val reading: Reading<DrivingGear>) : VehicleUpdate
}

interface VehicleDataSource {
    fun observe(): Flow<VehicleUpdate>
}
