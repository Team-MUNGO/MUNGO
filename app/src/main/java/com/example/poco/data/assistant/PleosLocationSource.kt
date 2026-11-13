package com.example.poco.data.assistant

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

interface AssistantLocationSource {
    suspend fun current(): AssistantLocation

    /** Pleos 위치 권한이 허용돼 있는지. 꺼져 있으면 [current]는 SecurityException으로 실패한다 */
    fun hasPermission(): Boolean = true
}

const val FUSED_LOCATION_PERMISSION = "pleos.car.permission.FUSED_LOCATION"

/** On-demand public Pleos location only. Unknown timestamp semantics fail closed. */
class PleosLocationSource @Inject constructor(@ApplicationContext private val context: Context) :
    AssistantLocationSource {
    override fun hasPermission() =
        context.checkSelfPermission(FUSED_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED

    override suspend fun current(): AssistantLocation {
        val sdk = FusedLocation(context)
        check(sdk.isSupported()) { "이 차량에서 위치 정보를 사용할 수 없어요." }
        var listener: FusedLocationListener? = null
        try {
            sdk.initialize()
            return withTimeout(10_000) {
                suspendCancellableCoroutine { continuation ->
                    val done = AtomicBoolean(false)
                    listener = object : FusedLocationListener {
                        override fun onChanged(location: LocationData?) {
                            if (done.get()) return
                            val value = location?.let {
                                freshLocation(
                                    it.latitude,
                                    it.longitude,
                                    it.timestampMillis,
                                    System.currentTimeMillis()
                                )
                            }
                            if (value != null &&
                                done.compareAndSet(false, true) &&
                                continuation.isActive
                            ) {
                                continuation.resume(value)
                            }
                        }
                        override fun onError(exception: Exception) {
                            if (done.compareAndSet(false, true) &&
                                continuation.isActive
                            ) {
                                continuation.resumeWithException(
                                    IllegalStateException("현재 위치를 확인하지 못했어요.")
                                )
                            }
                        }
                    }
                    continuation.invokeOnCancellation { done.set(true) }
                    sdk.registerFusedLocationCallback(listener!!)
                    sdk.getLastKnownLocation(listener!!)
                }
            }
        } finally {
            listener?.let { runCatching { sdk.unregisterFusedLocationCallback(it) } }
            sdk.release()
        }
    }
}

internal fun freshLocation(
    latitude: Double,
    longitude: Double,
    timestamp: Long,
    now: Long
): AssistantLocation? {
    if (!latitude.isFinite() ||
        latitude !in -90.0..90.0 ||
        !longitude.isFinite() ||
        longitude !in -180.0..180.0
    ) {
        return null
    }
    // SDK source does not specify the timestamp clock. Accept recent Unix milliseconds only;
    // elapsed/unknown/stale values cannot silently become a current location.
    if (timestamp > now || timestamp < now - 30_000) return null
    return AssistantLocation(latitude, longitude, now - timestamp)
}

/** Continuous location fix used for the GPS-bearing turn detector and the trip's latest position. */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val headingDegrees: Double?,
    val epochMs: Long
)

interface LocationStream {
    fun observe(): kotlinx.coroutines.flow.Flow<LocationFix>
}

/**
 * Pleos FusedLocation stream. Only fixes passing [freshLocation] are emitted; heading semantics
 * (travel bearing vs. device) are not documented, so it is used only together with fresh vehicle speed
 * by the turn detector and is a device-verification item.
 */
class PleosLocationStream @Inject constructor(@ApplicationContext private val context: Context) :
    LocationStream {
    override fun observe() = kotlinx.coroutines.flow.callbackFlow {
        val sdk = FusedLocation(context)
        if (!sdk.isSupported()) {
            close()
            return@callbackFlow
        }
        val listener = object : FusedLocationListener {
            override fun onChanged(location: LocationData?) {
                val now = System.currentTimeMillis()
                location ?: return
                freshLocation(location.latitude, location.longitude, location.timestampMillis, now)
                    ?: return
                val heading = location.heading.toDouble().takeIf {
                    it.isFinite() && it in 0.0..360.0
                }
                trySend(
                    LocationFix(
                        location.latitude,
                        location.longitude,
                        heading,
                        location.timestampMillis
                    )
                )
            }
            override fun onError(exception: Exception) = Unit
        }
        sdk.initialize()
        sdk.registerFusedLocationCallback(listener)
        awaitClose {
            runCatching { sdk.unregisterFusedLocationCallback(listener) }
            sdk.release()
        }
    }
}
