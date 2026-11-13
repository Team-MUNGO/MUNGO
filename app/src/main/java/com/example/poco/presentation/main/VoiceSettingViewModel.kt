package com.example.poco.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.data.settings.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** 앱 전체 마이크 버튼이 보는 '멍고 음성 안내' 설정 ([com.example.poco.core.designsystem.LocalVoiceEnabled]) */
@HiltViewModel
class VoiceSettingViewModel @Inject constructor(settings: UserSettingsRepository) : ViewModel() {
    val enabled: StateFlow<Boolean> = settings.voiceGuidanceEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
}
