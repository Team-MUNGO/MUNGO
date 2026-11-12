package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.vehicle.api.SteeringWheel
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.SteeringDataSource
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SteeringDataSourceImpl @Inject constructor(
    private val steeringWheel: SteeringWheel,
    private val sdkManager: PleosSdkManager
) : SteeringDataSource {
    override fun observeSteeringAngle(): Flow<Float> = callbackFlow {
        sdkManager.ensureVehicle()
        val listener =
            object : SteeringWheelAngleListener {
                override fun onSteeringWheelAngleUpdated(angle: Float?) {
                    angle?.let { trySend(it) }
                }

                override fun onFailed(e: Exception) {
                    close(e)
                }
            }
        steeringWheel.registerSteeringWheelAngle(listener)
        awaitClose { steeringWheel.unregisterSteeringWheelAngle(listener) }
    }
}
