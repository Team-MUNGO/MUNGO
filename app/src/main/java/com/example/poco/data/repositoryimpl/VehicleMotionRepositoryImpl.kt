package com.example.poco.data.repositoryimpl

import com.example.poco.data.car.datasource.CarPropertyDataSource
import com.example.poco.data.model.VehicleLocation
import com.example.poco.data.pleos.datasource.BrakeDataSource
import com.example.poco.data.pleos.datasource.LocationDataSource
import com.example.poco.data.pleos.datasource.SteeringDataSource
import com.example.poco.data.repository.VehicleMotionRepository
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import timber.log.Timber

class VehicleMotionRepositoryImpl @Inject constructor(
    private val carPropertyDataSource: CarPropertyDataSource,
    private val steeringDataSource: SteeringDataSource,
    private val brakeDataSource: BrakeDataSource,
    private val locationDataSource: LocationDataSource
) : VehicleMotionRepository {
    // Pleos Vehicle SDK에는 속도 API가 없어 Android Automotive 표준 속성을 사용
    override fun observeSpeedKmh(): Flow<Float> =
        carPropertyDataSource.observeSpeedMps().map { abs(it) * MPS_TO_KMH }

    override fun observeSteeringAngle(): Flow<Float> = steeringDataSource.observeSteeringAngle()
        .catch { e ->
            Timber.w(e, "Pleos 조향각 실패 - 표준 차량 속성으로 대체")
            emitAll(carPropertyDataSource.observeSteeringAngle())
        }

    override fun observeBrakePedalPressed(): Flow<Boolean> =
        brakeDataSource.observeBrakePedalPressed()

    override fun observeLocation(): Flow<VehicleLocation> = locationDataSource.observeLocation()

    private companion object {
        const val MPS_TO_KMH = 3.6f
    }
}
