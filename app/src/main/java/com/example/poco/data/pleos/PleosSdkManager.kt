package com.example.poco.data.pleos

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInitializeListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInitializeListener
import ai.pleos.playground.vehicle.Vehicle
import javax.inject.Inject
import javax.inject.Singleton
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
    private val speechToText: SpeechToText
) {
    private val vehicleMutex = Mutex()
    private val ttsMutex = Mutex()
    private val sttMutex = Mutex()

    private var isVehicleInitialized = false
    private var isTtsInitialized = false
    private var isSttInitialized = false

    suspend fun ensureVehicle() = vehicleMutex.withLock {
        if (isVehicleInitialized) return@withLock
        vehicle.initialize()
        isVehicleInitialized = true
        Timber.d("Vehicle initialized")
    }

    suspend fun ensureTts() = ttsMutex.withLock {
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
    suspend fun releaseStt() = sttMutex.withLock {
        speechToText.release()
        isSttInitialized = false
    }
}

class PleosInitException(sdk: String, reason: String) :
    IllegalStateException("$sdk initialize failed: $reason")
