package com.example.poco.data.repositoryimpl

import com.example.poco.core.driving.Reading
import com.example.poco.data.automotive.VehicleDataSource
import com.example.poco.data.automotive.VehicleUpdate
import com.example.poco.data.repository.VehicleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.catch

class VehicleRepositoryImpl @Inject constructor(private val source: VehicleDataSource) :
    VehicleRepository {
    override fun observe() = source.observe().catch {
        emit(VehicleUpdate.Speed(Reading.Unavailable(it.message ?: "차량 속도 연결 실패")))
        emit(VehicleUpdate.Gear(Reading.Unavailable(it.message ?: "기어 연결 실패")))
    }
}
