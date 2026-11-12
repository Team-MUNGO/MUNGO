package com.example.poco.data.repository

import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.Reading
import kotlinx.coroutines.flow.Flow

interface HvacRepository {
    fun observeInteriorTemperature(seat: PetSeat): Flow<Reading<Float>>
}
