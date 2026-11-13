package com.example.poco.data.repositoryimpl

import com.example.poco.core.util.suspendRunCatching
import com.example.poco.data.pleos.datasource.TtsDataSource
import com.example.poco.data.repository.TtsRepository
import com.example.poco.data.settings.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * 설정 > 음성 및 알림 > '멍고 음성 안내'가 꺼져 있으면 아무 말도 하지 않는다.
 * 모든 화면(탑승 준비 · 주행 · 어시스턴트)이 이 저장소를 거치므로 여기 한 곳에서 막는다.
 */
class TtsRepositoryImpl @Inject constructor(
    private val ttsDataSource: TtsDataSource,
    private val settings: UserSettingsRepository
) : TtsRepository {
    override suspend fun speak(text: String): Result<Unit> = suspendRunCatching {
        if (voiceEnabled()) ttsDataSource.speak(text)
    }

    override suspend fun speak(text: String, onSpeaking: (Boolean) -> Unit) {
        if (voiceEnabled()) ttsDataSource.speak(text, onSpeaking)
    }

    private suspend fun voiceEnabled(): Boolean = settings.voiceGuidanceEnabled.first()
}
