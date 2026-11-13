package com.example.poco.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import android.car.Car
import android.car.CarInfoManager
import android.content.Context
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.awaitPleos
import com.example.poco.data.repository.DrivingSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Every Pleos SDK connection asks this first; when the car link is off, nothing connects. */
interface VehicleLinkGate {
    val isLinked: Boolean

    object AlwaysLinked : VehicleLinkGate {
        override val isLinked = true
    }
}

class VehicleUnlinkedException : IllegalStateException("차량 연동이 꺼져 있어요")

/** [changedAt]: when the link was last turned off or back on (null = never changed). */
data class VehicleLinkState(val linked: Boolean = true, val changedAt: Long? = null)

/**
 * What the car itself reports. [model] comes from the AAOS car info service; [maker] falls back
 * to the VIN's manufacturer code. Both stay null when the car does not report them (no guessing).
 */
data class VehicleIdentity(val model: String?, val maker: String?, val readAt: Long) {
    val displayName: String? get() = model ?: maker?.let { "$it 차량" }
}

@Singleton
class VehicleLinkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vehicle: Vehicle,
    private val sdk: Provider<PleosSdkManager>,
    private val session: Provider<DrivingSessionRepository>
) : VehicleLinkGate {
    private val prefs = context.getSharedPreferences("vehicle_link", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(
        VehicleLinkState(
            linked = prefs.getBoolean(KEY_LINKED, true),
            changedAt = prefs.getLong(KEY_CHANGED_AT, 0L).takeIf { it > 0L }
        )
    )
    val state: StateFlow<VehicleLinkState> = mutableState.asStateFlow()

    override val isLinked: Boolean get() = mutableState.value.linked

    /**
     * Turns the link off: the running trip is ended (and saved), then every SDK connection
     * (vehicle, TTS, STT, location, navigation) is released. Trips and pet profiles are kept.
     */
    suspend fun unlink() {
        save(false)
        session.get().takeIf { it.state.value.active }?.end()
        sdk.get().releaseAll()
    }

    /** Turns the link back on; connections are made again lazily on next use. */
    fun relink() = save(true)

    private fun save(linked: Boolean) {
        val now = System.currentTimeMillis()
        prefs.edit().putBoolean(KEY_LINKED, linked).putLong(KEY_CHANGED_AT, now).apply()
        mutableState.value = VehicleLinkState(linked, now)
    }

    /** Reads the car model/maker. Only while linked; returns nulls when the car does not report them. */
    suspend fun identity(): VehicleIdentity = withContext(Dispatchers.IO) {
        if (!isLinked) throw VehicleUnlinkedException()
        val aaos = runCatching {
            val car = Car.createCar(context) ?: return@runCatching null
            try {
                val info = car.getCarManager(Car.INFO_SERVICE) as? CarInfoManager
                info?.let { it.model.cleaned() to it.manufacturer.cleaned() }
            } finally {
                car.disconnect()
            }
        }.onFailure { Timber.w(it, "AAOS 차량 정보 조회 실패") }.getOrNull()
        val vinMaker = runCatching {
            sdk.get().ensureVehicle()
            makerFromVin(awaitPleos<String?> { ok, fail -> vehicle.getCarInfo().getVINInfo(ok, fail) })
        }.onFailure { Timber.w(it, "VIN 조회 실패") }.getOrNull()
        VehicleIdentity(
            model = aaos?.first,
            maker = aaos?.second ?: vinMaker,
            readAt = System.currentTimeMillis()
        )
    }

    private companion object {
        const val KEY_LINKED = "linked"
        const val KEY_CHANGED_AT = "changed_at"
    }
}

private fun String?.cleaned() = this?.trim()?.takeIf { it.isNotEmpty() && !it.equals("unknown", true) }

/** World manufacturer identifier (VIN chars 1-3). Unknown codes and invalid VINs give null. */
internal fun makerFromVin(vin: String?): String? {
    val code = vin?.trim()?.uppercase()?.takeIf { it.length == 17 }?.take(3) ?: return null
    return when (code) {
        "KMH" -> "현대"
        "KMT" -> "제네시스"
        "KNA", "KND" -> "기아"
        else -> null
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class VehicleLinkModule {
    @Binds abstract fun bindGate(impl: VehicleLinkRepository): VehicleLinkGate
}
