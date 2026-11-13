package com.example.poco.data.repository

import com.example.poco.data.automotive.VehicleUpdate
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun observe(): Flow<VehicleUpdate>
}
