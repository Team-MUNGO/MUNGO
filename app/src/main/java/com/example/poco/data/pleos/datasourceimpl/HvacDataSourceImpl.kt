package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.vehicle.api.HVAC
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.listener.HvacTemperatureListener
import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.Reading
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.awaitPleos
import com.example.poco.data.pleos.datasource.HvacDataSource
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class HvacDataSourceImpl @Inject constructor(
    private val hvac: HVAC,
    private val sdk: PleosSdkManager
) : HvacDataSource {
    override fun observeInteriorTemperature(seat: PetSeat): Flow<Reading<Float>> = callbackFlow {
        sdk.ensureVehicle()
        val capability = awaitPleos<Pair<CapabilityConfig, List<SeatArea>>> { success, error ->
            hvac.checkHvacTemperatureCurrentCapabilities({ config, areas ->
                success(config to areas)
            }, error)
        }
        check(
            capability.first.accessType == AccessType.READ ||
                capability.first.accessType == AccessType.READ_WRITE
        ) { "실내 온도 읽기 미지원" }
        val seatValue = when (seat) {
            PetSeat.PASSENGER -> SeatValue.ROW_1_RIGHT
            PetSeat.REAR_LEFT -> SeatValue.ROW_2_LEFT
            PetSeat.REAR_RIGHT -> SeatValue.ROW_2_RIGHT
        }
        val area =
            capability.second.firstOrNull { seatValue in it.values } ?: error("탑승 위치의 실내 온도 센서 미지원")
        val received = AtomicBoolean(false)
        fun publish(value: Float?) {
            trySend(
                if (value != null &&
                    value.isFinite()
                ) {
                    Reading.Value(value)
                } else {
                    Reading.Unavailable("실내 온도 데이터 없음")
                }
            )
        }
        val listener = object : HvacTemperatureListener {
            override fun onHvacTemperatureUpdated(temperature: Float?) {
                received.set(true)
                publish(temperature)
            }
            override fun onFailed(e: Exception) {
                close(e)
            }
        }
        try {
            hvac.registerHvacTemperatureCurrent(area, listener)
            val initial =
                awaitPleos<Float?> { success, error ->
                    hvac.getHvacTemperatureCurrent(area, success, error)
                }
            if (!received.get()) publish(initial)
            awaitClose { }
        } finally {
            hvac.unregisterHvacTemperatureCurrent(area, listener)
        }
    }
}
