package com.example.poco.data.pleos

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInitializeListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInitializeListener
import ai.pleos.playground.vehicle.Vehicle
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Pleos SDK 초기화 담당.
 * 사용자 동의 전에 SDK가 켜지지 않도록, 실제로 사용할 때 한 번만 초기화한다(지연 초기화).
 * DataSource는 SDK API를 호출하기 전에 ensureXxx()를 먼저 호출한다.
 */
@Singleton
class PleosSdkManager @Inject constructor(
    private val vehicle: Vehicle,
    private val textToSpeech: TextToSpeech,
    private val speechToText: SpeechToText,
    private val fusedLocation: FusedLocation,
    private val naviHelper: NaviHelper
) {
    private val vehicleMutex = Mutex()
    private val ttsMutex = Mutex()
    private val sttMutex = Mutex()
    private val locationMutex = Mutex()
    private val naviMutex = Mutex()

    private var isVehicleInitialized = false
    private var isTtsInitialized = false
    private var isSttInitialized = false
    private var isLocationInitialized = false
    private var isNaviInitialized = false

    /** FusedLocation: 지원하지 않는 환경이면 예외 → 위치 기능만 끈다 */
    suspend fun ensureLocation() = locationMutex.withLock {
        if (isLocationInitialized) return@withLock
        if (!fusedLocation.isSupported()) throw PleosInitException("FusedLocation", "not supported")
        fusedLocation.initialize()
        isLocationInitialized = true
        Timber.d("FusedLocation initialized")
    }

    suspend fun ensureNavi() = naviMutex.withLock {
        if (isNaviInitialized) return@withLock
        suspendCancellableCoroutine { continuation ->
            naviHelper.initialize(
                { continuation.resume(Unit) },
                { e ->
                    continuation.resumeWithException(PleosInitException("NaviHelper", e.toString()))
                }
            )
        }
        isNaviInitialized = true
        Timber.d("NaviHelper initialized")
    }

    suspend fun ensureVehicle() = vehicleMutex.withLock {
        if (isVehicleInitialized) return@withLock
        vehicle.initialize()
        isVehicleInitialized = true
        Timber.d("Vehicle initialized")
    }

    suspend fun ensureTts() = ttsMutex.withLock {
        if (isTtsInitialized) return@withLock
        suspendCancellableCoroutine { continuation ->
            textToSpeech.initialize(
                object : TtsInitializeListener {
                    override fun onSuccess() {
                        continuation.resume(Unit)
                    }

                    override fun onFailure(errorType: ai.pleos.playground.tts.constant.ErrorType) {
                        continuation.resumeWithException(
                            PleosInitException("TTS", errorType.toString())
                        )
                    }
                }
            )
        }
        isTtsInitialized = true
        Timber.d("TTS initialized")
    }

    suspend fun ensureStt() = sttMutex.withLock {
        if (isSttInitialized) return@withLock
        suspendCancellableCoroutine { continuation ->
            speechToText.initialize(
                object : SttInitializeListener {
                    override fun onSuccess() {
                        continuation.resume(Unit)
                    }

                    override fun onFailure(errorType: ai.pleos.playground.stt.constant.ErrorType) {
                        continuation.resumeWithException(
                            PleosInitException("STT", errorType.toString())
                        )
                    }
                }
            )
        }
        isSttInitialized = true
        Timber.d("STT initialized")
    }
}

class PleosInitException(sdk: String, reason: String) :
    IllegalStateException("$sdk initialize failed: $reason")
