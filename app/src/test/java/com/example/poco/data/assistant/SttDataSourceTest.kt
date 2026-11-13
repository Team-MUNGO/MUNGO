package com.example.poco.data.assistant

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasourceimpl.SttDataSourceImpl
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SttDataSourceTest {
    private val stt = mockk<SpeechToText>(relaxed = true)
    private val sdk = mockk<PleosSdkManager>(relaxed = true)
    private val listener = slot<ResultListener>()
    private val source = SttDataSourceImpl(stt, sdk)
    init {
        every { stt.addListener(capture(listener)) } returns Unit
    }

    @Test fun duplicateFinalCallbacksReturnOnceAndRelease() = runTest {
        val partials = mutableListOf<String>()
        val result = async { source.recognize(partials::add) }
        runCurrent()
        listener.captured.onUpdated("중간", false)
        listener.captured.onUpdated("최종 질문", true)
        listener.captured.onUpdated("중복", true)
        listener.captured.onUpdated("늦은 중간", false)
        assertEquals("최종 질문", result.await())
        assertEquals(listOf("중간"), partials)
        verify(exactly = 1) { stt.request() }
        verify(exactly = 1) { stt.removeListener(listener.captured) }
        verify(exactly = 1) { stt.stop() }
        coVerify(exactly = 1) { sdk.releaseStt() }
    }

    @Test fun cancellationStopsRecognitionAndIgnoresLateCallbacks() = runTest {
        val partials = mutableListOf<String>()
        val result = async { source.recognize(partials::add) }
        runCurrent()
        result.cancelAndJoin()
        listener.captured.onUpdated("늦은 결과", true)
        listener.captured.onUpdated("늦은 중간", false)
        assertTrue(partials.isEmpty())
        verify {
            stt.removeListener(listener.captured)
            stt.stop()
        }
        coVerify { sdk.releaseStt() }
    }
}
