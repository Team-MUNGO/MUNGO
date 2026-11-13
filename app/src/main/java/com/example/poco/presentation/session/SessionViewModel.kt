package com.example.poco.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.core.driving.DrivingSettings
import com.example.poco.data.repository.DrivingSessionRepository
import com.example.poco.data.repositoryimpl.PetDrivingProfile
import com.example.poco.data.settings.UserSettingsRepository
import com.example.poco.data.vehicle.VehicleLinkGate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Pet profile state for starting a session: still loading, missing (go to 마이펫), or ready. */
sealed interface SessionProfile {
    data object Loading : SessionProfile
    data object Missing : SessionProfile
    data class Ready(val settings: DrivingSettings) : SessionProfile
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val session: DrivingSessionRepository,
    private val pets: PetDrivingProfile,
    settings: UserSettingsRepository,
    private val link: VehicleLinkGate
) : ViewModel() {
    /** 설정 > 알림 '도착 요약'. 끄면 주행을 마치고 바로 홈으로 */
    val arrivalSummary: StateFlow<Boolean> = settings.arrivalSummaryEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val uiState = session.state.map(::SessionUiState)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SessionUiState(session.state.value)
        )

    /** The watched pet comes from 마이펫 (first registered pet), like the boarding screen. */
    val profile: StateFlow<SessionProfile> = pets.profile
        .map { it?.let(SessionProfile::Ready) ?: SessionProfile.Missing }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionProfile.Loading)

    /** Starts with the current pet profile. No-op while a session is already running. */
    fun startWithProfile() {
        if (session.state.value.active) return
        // 차량 연동을 해지했으면 차량 데이터를 읽지 않으므로 주행 세션도 시작하지 않는다
        if (!link.isLinked) return
        viewModelScope.launch { pets.load()?.let(session::start) }
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
