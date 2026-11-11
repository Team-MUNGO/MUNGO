package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.tts.TextToSpeech
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.TtsDataSource
import javax.inject.Inject

class TtsDataSourceImpl @Inject constructor(
    private val textToSpeech: TextToSpeech,
    private val sdkManager: PleosSdkManager
) : TtsDataSource {
    override suspend fun speak(text: String) {
        sdkManager.ensureTts()
        textToSpeech.speak(text)
    }
}
