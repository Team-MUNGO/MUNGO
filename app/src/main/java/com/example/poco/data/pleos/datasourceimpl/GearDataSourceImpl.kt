package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.vehicle.api.Gear
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.GearDataSource
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class GearDataSourceImpl @Inject constructor(
    private val gear: Gear,
    private val sdkManager: PleosSdkManager
) : GearDataSource {
    override fun observeGear(): Flow<VehicleGear> = callbackFlow {
        sdkManager.ensureVehicle()
        val listener =
            object : GearSelectionListener {
                override fun onSelected(selected: VehicleGear?) {
                    trySend(selected ?: VehicleGear.GEAR_UNKNOWN)
                }

                override fun onFailed(e: Exception) {
                    close(e)
                }
            }
        gear.registerGearSelection(listener)
        awaitClose { gear.unregisterGearSelection(listener) }
    }
}
