package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.TtsDataSource
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class TtsDataSourceImpl @Inject constructor(
    private val tts: TextToSpeech,
    private val sdk: PleosSdkManager
) : TtsDataSource {
    private val mutex = Mutex()
    override suspend fun speak(text: String, onSpeaking: (Boolean) -> Unit) = mutex.withLock {
        var listener: EventListener? = null
        try {
            sdk.ensureTts()
            withTimeout(30_000) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val done = AtomicBoolean(false)
                    listener = object : EventListener {
                        override fun onReady() = Unit
                        override fun onStart() {
                            if (!done.get()) onSpeaking(true)
                        }
                        override fun onDone() {
                            if (done.compareAndSet(false, true) &&
                                continuation.isActive
                            ) {
                                continuation.resume(Unit)
                            }
                        }
                        override fun onError(errorType: ErrorType) {
                            if (done.compareAndSet(false, true) && continuation.isActive) {
                                continuation.resumeWithException(
                                    IllegalStateException("음성 안내 실패: $errorType")
                                )
                            }
                        }
                        override fun onStop() {
                            if (done.compareAndSet(false, true) &&
                                continuation.isActive
                            ) {
                                continuation.resume(Unit)
                            }
                        }
                        override fun onUpdatedRms(rms: Double) = Unit
                    }
                    continuation.invokeOnCancellation { done.set(true) }
                    tts.addEventListener(listener!!)
                    tts.setLocale(Locale.KOREA)
                    tts.speak(text, false)
                }
            }
        } finally {
            listener?.let { runCatching { tts.removeEventListener(it) } }
            runCatching { tts.stop() }
            onSpeaking(false)
            withContext(NonCancellable) { sdk.releaseTts() }
        }
    }
}
