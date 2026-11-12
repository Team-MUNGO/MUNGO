package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.vehicle.api.Brake
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.BrakeDataSource
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BrakeDataSourceImpl @Inject constructor(
    private val brake: Brake,
    private val sdkManager: PleosSdkManager
) : BrakeDataSource {
    override fun observeBrakePedalPressed(): Flow<Boolean> = callbackFlow {
        sdkManager.ensureVehicle()
        val listener =
            object : BrakePedalStatusListener {
                override fun onBrakePedalStatusUpdated(pressed: Boolean?) {
                    pressed?.let { trySend(it) }
                }

                override fun onFailed(e: Exception) {
                    close(e)
                }
            }
        brake.registerBrakePedalStatus(listener)
        awaitClose { brake.unregisterBrakePedalStatus(listener) }
    }
}
