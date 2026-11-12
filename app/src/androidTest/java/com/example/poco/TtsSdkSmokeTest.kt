package com.example.poco

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.vehicle.Vehicle
import androidx.test.platform.app.InstrumentationRegistry
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasourceimpl.TtsDataSourceImpl
import java.io.File
import java.util.Collections
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit TTS probe through the production DataSource, without vehicle reads or writes. */
class TtsSdkSmokeTest {
    @Test fun koreanWarningCompletesPlayback() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("ttsSmoke") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val events = Collections.synchronizedList(mutableListOf<String>())
        // Optional -e ttsMode SERVER|HYBRID to probe non on-device engines.
        val mode = Mode.valueOf(InstrumentationRegistry.getArguments().getString("ttsMode") ?: "ON_DEVICE")
        val tts = TextToSpeech(context, mode)
        val manager = PleosSdkManager(
            Vehicle(context),
            tts,
            SpeechToText(context, ai.pleos.playground.stt.constant.Mode.ON_DEVICE)
        )
        val listener = object : EventListener {
            override fun onReady() {
                events.add("onReady")
            }
            override fun onStart() {
                events.add("onStart")
            }
            override fun onDone() {
                events.add("onDone")
            }
            override fun onStop() {
                events.add("onStop")
            }
            override fun onError(errorType: ErrorType) {
                events.add("onError: $errorType")
            }
            override fun onUpdatedRms(rms: Double) = Unit
        }
        val result = runCatching {
            manager.ensureTts()
            events.add("initialized")
            tts.addEventListener(listener)
            TtsDataSourceImpl(tts, manager).speak(
                "반려견이 있는 쪽 창문이 많이 열려 있어요. 안전을 위해 창문을 닫아주세요."
            ) { events.add("speaking=$it") }
        }
        runCatching { tts.removeEventListener(listener) }
        val report = "Mode=$mode, locale=ko-KR\n" +
            events.toList().joinToString("\n") + "\n" +
            result.fold({ "Result=completed" }, { "Result=failed: $it" })
        File(context.filesDir, "tts-sdk-smoke.txt").writeText(report)
        assertTrue(
            report,
            result.isSuccess && events.contains("onStart") && events.contains("onDone")
        )
    }
}
