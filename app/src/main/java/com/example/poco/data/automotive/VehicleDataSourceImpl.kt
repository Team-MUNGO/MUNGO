package com.example.poco.data.automotive

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.poco.core.driving.DrivingGear
import com.example.poco.core.driving.Reading
import com.example.poco.core.driving.SpeedSample
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/** Public AAOS APIs. No reflection, vendor binder, GPS substitution or simulated values. */
class VehicleDataSourceImpl @Inject constructor(@ApplicationContext private val context: Context) :
    VehicleDataSource {
    override fun observe(): Flow<VehicleUpdate> = callbackFlow {
        check(Build.VERSION.SDK_INT >= 29) { "차량 API는 Android 10 이상에서 지원합니다" }
        val stopped = AtomicBoolean(false)
        var manager: CarPropertyManager? = null
        var car: Car? = null
        fun unavailable(id: Int, reason: String) {
            when (id) {
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> trySend(
                    VehicleUpdate.Speed(Reading.Unavailable(reason))
                )
                VehiclePropertyIds.GEAR_SELECTION -> trySend(
                    VehicleUpdate.Gear(Reading.Unavailable(reason))
                )
            }
        }
        fun publish(value: CarPropertyValue<*>) {
            if (stopped.get()) return
            if (value.status != CarPropertyValue.STATUS_AVAILABLE) {
                unavailable(value.propertyId, "차량 데이터 사용 불가")
                return
            }
            when (value.propertyId) {
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> {
                    val speed = (value.value as? Float)?.toDouble()
                    if (speed == null ||
                        !speed.isFinite() ||
                        value.timestamp <= 0
                    ) {
                        unavailable(value.propertyId, "유효한 차량 속도 없음")
                    } else {
                        trySend(
                            VehicleUpdate.Speed(
                                Reading.Value(
                                    SpeedSample(
                                        speed * 3.6,
                                        value.timestamp / 1_000_000
                                    )
                                )
                            )
                        )
                    }
                }
                VehiclePropertyIds.GEAR_SELECTION -> {
                    val gear = when (value.value as? Int) {
                        VehicleGear.GEAR_PARK -> DrivingGear.PARK
                        VehicleGear.GEAR_DRIVE -> DrivingGear.DRIVE
                        VehicleGear.GEAR_REVERSE -> DrivingGear.REVERSE
                        VehicleGear.GEAR_NEUTRAL -> DrivingGear.NEUTRAL
                        else -> null
                    }
                    trySend(
                        VehicleUpdate.Gear(
                            gear?.let { Reading.Value(it) } ?: Reading.Unavailable("기어 상태 없음")
                        )
                    )
                }
            }
        }
        val callback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) = publish(value)
            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                unavailable(propertyId, "차량 데이터 수신 실패")
            }
        }
        try {
            car =
                Car.createCar(
                    context,
                    Handler(Looper.getMainLooper()),
                    Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT
                ) {
                        connected,
                        ready
                    ->
                    if (!stopped.get()) {
                        runCatching { manager?.unregisterCallback(callback) }
                        manager = null
                        if (!ready) {
                            unavailable(VehiclePropertyIds.PERF_VEHICLE_SPEED, "차량 서비스 연결 끊김")
                            unavailable(VehiclePropertyIds.GEAR_SELECTION, "차량 서비스 연결 끊김")
                        } else {
                            val properties = runCatching {
                                connected.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
                            }.getOrNull()
                            if (properties == null) {
                                unavailable(VehiclePropertyIds.PERF_VEHICLE_SPEED, "차량 서비스 접근 실패")
                                unavailable(VehiclePropertyIds.GEAR_SELECTION, "차량 서비스 접근 실패")
                                return@createCar
                            }
                            manager = properties
                            listOf(
                                VehiclePropertyIds.PERF_VEHICLE_SPEED,
                                VehiclePropertyIds.GEAR_SELECTION
                            ).forEach { id ->
                                try {
                                    val rate = if (id ==
                                        VehiclePropertyIds.PERF_VEHICLE_SPEED
                                    ) {
                                        CarPropertyManager.SENSOR_RATE_FAST
                                    } else {
                                        CarPropertyManager.SENSOR_RATE_ONCHANGE
                                    }
                                    check(properties.registerCallback(callback, id, rate)) {
                                        "차량 속성 미지원"
                                    }
                                    // Registration delivers initial values; do not race it with a stale get.
                                } catch (e: Exception) {
                                    unavailable(id, e.message ?: "차량 속성 접근 실패")
                                }
                            }
                        }
                    }
                }
            check(car != null) { "차량 서비스 없음" }
            awaitClose { }
        } finally {
            stopped.set(true)
            runCatching { manager?.unregisterCallback(callback) }
            runCatching { car?.disconnect() }
        }
    }.flowOn(Dispatchers.Main.immediate)
}
