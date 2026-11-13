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
import com.example.poco.data.vehicle.VehicleLinkGate
import com.example.poco.data.vehicle.VehicleUnlinkedException
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
    private val naviHelper: NaviHelper,
    /** 설정 > 차량 연동 관리에서 해지하면 어떤 SDK도 새로 연결하지 않는다 */
    private val link: VehicleLinkGate = VehicleLinkGate.AlwaysLinked
) {
    private fun requireLinked() {
        if (!link.isLinked) throw VehicleUnlinkedException()
    }

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
        requireLinked()
        if (isLocationInitialized) return@withLock
        if (!fusedLocation.isSupported()) throw PleosInitException("FusedLocation", "not supported")
        fusedLocation.initialize()
        isLocationInitialized = true
        Timber.d("FusedLocation initialized")
    }

    suspend fun ensureNavi() = naviMutex.withLock {
        requireLinked()
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
        requireLinked()
        if (isVehicleInitialized) return@withLock
        vehicle.initialize()
        isVehicleInitialized = true
        Timber.d("Vehicle initialized")
    }

    suspend fun ensureTts() = ttsMutex.withLock {
        requireLinked()
        if (isTtsInitialized) return@withLock
        awaitPleos<Unit> { success, error ->
            textToSpeech.initialize(object : TtsInitializeListener {
                override fun onSuccess() = success(Unit)
                override fun onFailure(errorType: ai.pleos.playground.tts.constant.ErrorType) =
                    error(PleosInitException("TTS", errorType.toString()))
            })
        }
        isTtsInitialized = true
        Timber.d("TTS initialized")
    }

    suspend fun releaseTts() = ttsMutex.withLock {
        textToSpeech.release()
        isTtsInitialized = false
    }

    suspend fun ensureStt() = sttMutex.withLock {
        requireLinked()
        if (isSttInitialized) return@withLock
        awaitPleos<Unit> { success, error ->
            speechToText.initialize(object : SttInitializeListener {
                override fun onSuccess() = success(Unit)
                override fun onFailure(errorType: ai.pleos.playground.stt.constant.ErrorType) =
                    error(PleosInitException("STT", errorType.toString()))
            })
        }
        isSttInitialized = true
        Timber.d("STT initialized")
    }
    /** 차량 연동 해지: 연결된 SDK를 모두 끊는다 (차량 · 음성 출력 · 음성 인식 · 위치 · 내비). 하나가 실패해도 나머지는 계속 끊는다 */
    suspend fun releaseAll() {
        vehicleMutex.withLock {
            if (isVehicleInitialized) runCatching { vehicle.release() }.onFailure { Timber.w(it, "Vehicle release 실패") }
            isVehicleInitialized = false
        }
        ttsMutex.withLock {
            if (isTtsInitialized) runCatching { textToSpeech.release() }.onFailure { Timber.w(it, "TTS release 실패") }
            isTtsInitialized = false
        }
        sttMutex.withLock {
            if (isSttInitialized) runCatching { speechToText.release() }.onFailure { Timber.w(it, "STT release 실패") }
            isSttInitialized = false
        }
        locationMutex.withLock {
            if (isLocationInitialized) runCatching { fusedLocation.release() }.onFailure { Timber.w(it, "위치 release 실패") }
            isLocationInitialized = false
        }
        naviMutex.withLock {
            if (isNaviInitialized) runCatching { naviHelper.release() }.onFailure { Timber.w(it, "NaviHelper release 실패") }
            isNaviInitialized = false
        }
        Timber.d("차량 연동 해지: SDK 연결을 모두 끊었어요")
    }

    suspend fun releaseStt() = sttMutex.withLock {
        speechToText.release()
        isSttInitialized = false
    }
}

class PleosInitException(sdk: String, reason: String) :
    IllegalStateException("$sdk initialize failed: $reason")
