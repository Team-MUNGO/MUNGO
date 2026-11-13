package com.example.poco.data.repositoryimpl

import com.example.poco.core.util.suspendRunCatching
import com.example.poco.data.pleos.datasource.SttDataSource
import com.example.poco.data.repository.SttRepository
import com.example.poco.data.settings.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 설정 > 음성 및 알림 > '멍고 음성 안내'가 꺼져 있으면 마이크를 열지 않는다.
 * - [listenOnce]: 아무 말도 못 들은 것(null)으로 돌려 화면 버튼으로 고르게 한다
 * - [recognize]: 어시스턴트가 안내 문구를 띄우도록 [VoiceGuidanceOffException]을 던진다
 */
class SttRepositoryImpl @Inject constructor(
    private val sttDataSource: SttDataSource,
    private val settings: UserSettingsRepository
) : SttRepository {
    override suspend fun listenOnce(timeoutMs: Long): Result<String?> = suspendRunCatching {
        if (!voiceEnabled()) return@suspendRunCatching null
        withTimeoutOrNull(timeoutMs) { sttDataSource.listenOnce() }
    }

    override suspend fun recognize(onPartial: (String) -> Unit): String {
        if (!voiceEnabled()) throw VoiceGuidanceOffException()
        return sttDataSource.recognize(onPartial)
    }

    private suspend fun voiceEnabled(): Boolean = settings.voiceGuidanceEnabled.first()
}

class VoiceGuidanceOffException : IllegalStateException("음성 안내가 꺼져 있어요. 설정 > 음성 및 알림에서 켜 주세요.")
