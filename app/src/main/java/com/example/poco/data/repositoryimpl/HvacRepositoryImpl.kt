package com.example.poco.data.repositoryimpl

import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.Reading
import com.example.poco.data.pleos.datasource.HvacDataSource
import com.example.poco.data.repository.HvacRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.catch

class HvacRepositoryImpl @Inject constructor(private val source: HvacDataSource) : HvacRepository {
    override fun observeInteriorTemperature(seat: PetSeat) = source.observeInteriorTemperature(seat)
        .catch { emit(Reading.Unavailable(it.message ?: "실내 온도 수신 실패")) }
}
