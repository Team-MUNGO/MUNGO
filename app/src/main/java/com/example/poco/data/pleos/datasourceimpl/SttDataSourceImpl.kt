package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.listener.ResultListener
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.SttDataSource
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

class SttDataSourceImpl @Inject constructor(
    private val stt: SpeechToText,
    private val sdk: PleosSdkManager
) : SttDataSource {
    private val mutex = Mutex()
    override suspend fun recognize(onPartial: (String) -> Unit): String = mutex.withLock {
        var listener: ResultListener? = null
        try {
            sdk.ensureStt()
            withTimeout(25_000) {
                suspendCancellableCoroutine { continuation ->
                    val done = AtomicBoolean(false)
                    listener = object : ResultListener {
                        override fun onUpdated(stt: String, completed: Boolean) {
                            if (done.get()) return
                            if (completed &&
                                done.compareAndSet(false, true) &&
                                continuation.isActive
                            ) {
                                if (stt.isBlank()) {
                                    continuation.resumeWithException(
                                        IllegalStateException("질문을 듣지 못했어요.")
                                    )
                                } else {
                                    continuation.resume(stt.take(500))
                                }
                            } else if (!completed) {
                                onPartial(stt.take(500))
                            }
                        }
                        override fun onError(errorType: ErrorType) {
                            if (done.compareAndSet(false, true) &&
                                continuation.isActive
                            ) {
                                continuation.resumeWithException(
                                    IllegalStateException("음성 인식을 시작하지 못했어요. ($errorType)")
                                )
                            }
                        }
                        override fun onReady() = Unit
                        override fun onStartedRecognition() = Unit
                        override fun onEndedRecognition() = Unit
                        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
                        override fun onUpdatedRms(rms: Double) = Unit
                    }
                    continuation.invokeOnCancellation { done.set(true) }
                    stt.addListener(listener!!)
                    stt.setLocale(Locale.KOREA)
                    stt.request()
                }
            }
        } finally {
            listener?.let { runCatching { stt.removeListener(it) } }
            runCatching { stt.stop() }
            withContext(NonCancellable) { sdk.releaseStt() }
        }
    }
}
