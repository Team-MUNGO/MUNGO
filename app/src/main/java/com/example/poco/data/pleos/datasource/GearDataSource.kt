package com.example.poco.data.pleos.datasource

import ai.pleos.playground.vehicle.constant.control.VehicleGear
import kotlinx.coroutines.flow.Flow

interface GearDataSource {
    fun observeGear(): Flow<VehicleGear>
}
