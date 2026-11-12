package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.listener.ResultListener
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.SttDataSource
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

class SttDataSourceImpl @Inject constructor(
    private val speechToText: SpeechToText,
    private val sdkManager: PleosSdkManager
) : SttDataSource {
    override suspend fun listenOnce(): String? {
        sdkManager.ensureStt()
        return suspendCancellableCoroutine { continuation ->
            var lastText: String? = null
            val listener =
                object : ResultListener {
                    override fun onUpdated(text: String, isFinal: Boolean) {
                        if (text.isNotBlank()) lastText = text
                        if (isFinal && continuation.isActive) continuation.resume(lastText)
                    }

                    override fun onEndedRecognition() {
                        if (continuation.isActive) continuation.resume(lastText)
                    }

                    override fun onError(errorType: ErrorType) {
                        Timber.w("STT 오류: $errorType")
                        if (continuation.isActive) continuation.resume(null)
                    }

                    override fun onUpdatedEpdData(epdOn: Long, epdOff: Long) = Unit

                    override fun onStartedRecognition() = Unit

                    override fun onReady() = Unit

                    override fun onUpdatedRms(rms: Double) = Unit
                }
            speechToText.addListener(listener)
            speechToText.setLocale(Locale.KOREA)
            speechToText.request()
            continuation.invokeOnCancellation {
                runCatching { speechToText.stop() }
                speechToText.removeListener(listener)
            }
        }.also { runCatching { speechToText.stop() } }
    }
}
