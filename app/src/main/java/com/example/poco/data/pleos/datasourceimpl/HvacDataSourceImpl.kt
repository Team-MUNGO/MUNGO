package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.vehicle.api.HVAC
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import ai.pleos.playground.vehicle.listener.HvacTemperatureListener
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.HvacDataSource
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class HvacDataSourceImpl @Inject constructor(
    private val hvac: HVAC,
    private val sdkManager: PleosSdkManager
) : HvacDataSource {
    override suspend fun getTemperature(area: SeatArea): Float? {
        sdkManager.ensureVehicle()
        return suspendCancellableCoroutine { continuation ->
            hvac.getHvacTemperature(
                area,
                TemperatureUnit.CELSIUS,
                { temperature -> continuation.resume(temperature) },
                { e -> continuation.resumeWithException(e) }
            )
        }
    }

    override suspend fun setTemperature(area: SeatArea, celsius: Float): Float? {
        sdkManager.ensureVehicle()
        return suspendCancellableCoroutine { continuation ->
            hvac.setHvacTemperature(
                area,
                celsius,
                TemperatureUnit.CELSIUS,
                { _, applied -> continuation.resume(applied) },
                { e -> continuation.resumeWithException(e) }
            )
        }
    }

    override fun observeCurrentTemperature(area: SeatArea): Flow<Float> = callbackFlow {
        sdkManager.ensureVehicle()
        val listener = temperatureListener { trySend(it) }
        hvac.registerHvacTemperatureCurrent(area, listener)
        awaitClose { hvac.unregisterHvacTemperatureCurrent(area, listener) }
    }

    override fun observeTemperature(area: SeatArea): Flow<Float> = callbackFlow {
        sdkManager.ensureVehicle()
        val listener = temperatureListener { trySend(it) }
        hvac.registerHvacTemperature(area, listener)
        awaitClose { hvac.unregisterHvacTemperature(area, listener) }
    }

    private fun ProducerScope<Float>.temperatureListener(onValue: (Float) -> Unit) =
        object : HvacTemperatureListener {
            override fun onHvacTemperatureUpdated(temperature: Float?) {
                temperature?.let(onValue)
            }

            override fun onFailed(e: Exception) {
                close(e)
            }
        }
}
