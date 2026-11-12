package com.example.poco.data.pleos.datasource

import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.Reading
import kotlinx.coroutines.flow.Flow

interface HvacDataSource {
    fun observeInteriorTemperature(seat: PetSeat): Flow<Reading<Float>>
}
