package com.example.poco.data.car.datasourceimpl

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import com.example.poco.data.car.datasource.CarPropertyDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapNotNull

class CarPropertyDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CarPropertyDataSource {
    override fun observeSpeedMps(): Flow<Float> =
        observe(VehiclePropertyIds.PERF_VEHICLE_SPEED, SAMPLE_RATE_HZ).mapNotNull { it as? Float }

    override fun observeSteeringAngle(): Flow<Float> =
        observe(VehiclePropertyIds.PERF_STEERING_ANGLE, SAMPLE_RATE_HZ).mapNotNull { it as? Float }

    // Pleos Gear API가 deprecated → Google GEAR_SELECTION 속성 사용 (Pleos 권장)
    override fun observeIsParked(): Flow<Boolean> =
        observe(VehiclePropertyIds.GEAR_SELECTION, ON_CHANGE)
            .mapNotNull { it as? Int }
            .mapNotNull { gear ->
                when (gear) {
                    VehicleGear.GEAR_UNKNOWN -> null
                    VehicleGear.GEAR_PARK -> true
                    else -> false
                }
            }

    private fun observe(propertyId: Int, rateHz: Float): Flow<Any?> = callbackFlow {
        val car = Car.createCar(context)
            ?: throw IllegalStateException("Car 서비스에 연결할 수 없어요")
        val manager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        val callback =
            object : CarPropertyManager.CarPropertyEventCallback {
                override fun onChangeEvent(value: CarPropertyValue<*>) {
                    trySend(value.value)
                }

                override fun onErrorEvent(propId: Int, areaId: Int) {
                    close(IllegalStateException("차량 속성 오류: $propId"))
                }
            }

        @Suppress("DEPRECATION")
        val registered = manager.registerCallback(callback, propertyId, rateHz)
        if (!registered) {
            car.disconnect()
            throw IllegalStateException("차량 속성 구독 실패: $propertyId")
        }
        awaitClose {
            @Suppress("DEPRECATION")
            manager.unregisterCallback(callback)
            car.disconnect()
        }
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 5f
        const val ON_CHANGE = 0f
    }
}
