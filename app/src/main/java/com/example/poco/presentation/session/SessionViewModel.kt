package com.example.poco.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.core.driving.DrivingSettings
import com.example.poco.data.repository.DrivingSessionRepository
import com.example.poco.data.repositoryimpl.DrivingSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val session: DrivingSessionRepository,
    private val settings: DrivingSettingsRepository
) : ViewModel() {
    val uiState = session.state.map(::SessionUiState)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SessionUiState(session.state.value)
        )
    val savedSettings get() = settings.load()

    fun start(value: DrivingSettings) {
        settings.save(value)
        session.start(value)
    }
    fun end() = session.end()
    fun dismissWarning() = session.dismissWindowWarning()
    fun closeWindow() = session.closeWindow()
    fun startRest() = session.startRest()
    fun readyToDepart() = session.readyToDepart()
    fun cancelWaypoint() = session.cancelWaypoint()
    fun setRestInterval(minutes: Int) = session.setRestInterval(minutes)
    fun setVoiceGuidance(enabled: Boolean) = session.setVoiceGuidance(enabled)
}
