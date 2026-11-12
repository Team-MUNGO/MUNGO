package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.vehicle.api.EvBattery
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.BatteryDataSource
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class BatteryDataSourceImpl @Inject constructor(
    private val evBattery: EvBattery,
    private val sdkManager: PleosSdkManager
) : BatteryDataSource {
    override suspend fun getStateOfCharge(): Int? {
        sdkManager.ensureVehicle()
        return suspendCancellableCoroutine { continuation ->
            evBattery.getEvBatteryStateOfCharge(
                { percent -> continuation.resume(percent) },
                { e -> continuation.resumeWithException(e) }
            )
        }
    }

    override fun observeStateOfCharge(): Flow<Int> = callbackFlow {
        sdkManager.ensureVehicle()
        val listener = object : EvBatteryStateOfChargeListener {
            override fun onEvBatteryStateOfChargeUpdated(percent: Int?) {
                percent?.let { trySend(it) }
            }

            override fun onFailed(e: Exception) {
                close(e)
            }
        }
        evBattery.registerEvBatteryStateOfCharge(listener)
        awaitClose { evBattery.unregisterEvBatteryStateOfCharge(listener) }
    }
}
