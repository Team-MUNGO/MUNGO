package com.example.poco.data.repositoryimpl

import ai.pleos.playground.vehicle.constant.control.VehicleGear
import com.example.poco.data.car.datasource.CarPropertyDataSource
import com.example.poco.data.pleos.datasource.GearDataSource
import com.example.poco.data.repository.GearRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import timber.log.Timber

class GearRepositoryImpl @Inject constructor(
    private val carPropertyDataSource: CarPropertyDataSource,
    private val gearDataSource: GearDataSource
) : GearRepository {
    /** Google GEAR_SELECTION 우선 (Pleos 권장), 안 되면 Pleos Gear API로 대체 */
    override fun observeIsParked(): Flow<Boolean> = carPropertyDataSource.observeIsParked()
        .catch { e ->
            Timber.w(e, "GEAR_SELECTION 실패 - Pleos Gear API로 대체")
            emitAll(
                gearDataSource.observeGear()
                    .filter { it != VehicleGear.GEAR_UNKNOWN }
                    .map { it == VehicleGear.GEAR_PARK }
            )
        }
        .distinctUntilChanged()
}
