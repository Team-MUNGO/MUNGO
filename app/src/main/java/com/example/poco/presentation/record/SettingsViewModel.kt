package com.example.poco.presentation.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.BuildConfig
import com.example.poco.core.driving.RestIntervalPolicy
import com.example.poco.data.model.Pet
import com.example.poco.data.repository.DrivingSessionRepository
import com.example.poco.data.repository.PetRepository
import com.example.poco.data.settings.UserSettingsRepository
import com.example.poco.data.vehicle.VehicleIdentity
import com.example.poco.data.vehicle.VehicleLinkRepository
import com.example.poco.data.vehicle.VehicleLinkState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/** 알림 toggles. Rest/window come from the pet profiles (온보딩 3/3에서 고른 값),
arrival summary is app-wide. */
data class AlertSettings(
    val restSuggestion: Boolean = true,
    val windowAlert: Boolean = true,
    val windowAlertPercent: Int = 20,
    val arrivalSummary: Boolean = true,
    val intervalMinutes: Int = RestIntervalPolicy.DEFAULT_MINUTES,
    /** No pet registered yet: the pet-based toggles have nothing to change. */
    val hasPet: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val pets: PetRepository,
    private val settings: UserSettingsRepository,
    private val session: DrivingSessionRepository,
    private val link: VehicleLinkRepository
) : ViewModel() {
    private val petList = pets.observePets().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    private val _alerts = MutableStateFlow(AlertSettings())
    val alerts: StateFlow<AlertSettings> = _alerts.asStateFlow()

    val voice: StateFlow<Boolean> = settings.voiceGuidanceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val linkState: StateFlow<VehicleLinkState> = link.state

    private val _identity = MutableStateFlow<VehicleIdentity?>(null)
    val identity: StateFlow<VehicleIdentity?> = _identity.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val version: String = BuildConfig.VERSION_NAME

    init {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                pets.observePets(),
                settings.arrivalSummaryEnabled,
                settings.restIntervalMinutes
            ) { list, arrival, interval ->
                val first = list.firstOrNull()
                AlertSettings(
                    restSuggestion = first?.restSuggestionEnabled ?: true,
                    windowAlert = first?.windowAlertEnabled ?: true,
                    windowAlertPercent = first?.windowAlertPercent ?: 20,
                    arrivalSummary = arrival,
                    intervalMinutes = interval,
                    hasPet = first != null
                )
            }.collect { _alerts.value = it }
        }
        viewModelScope.launch {
            link.state.map { it.linked }.collect {
                if (it) {
                    loadIdentity()
                } else {
                    _identity.value =
                        null
                }
            }
        }
    }

    /** Same switch for every registered pet, so the app-wide setting matches what driving uses. */
    fun setRestSuggestion(enabled: Boolean) =
        updatePets { it.copy(restSuggestionEnabled = enabled) }
    fun setWindowAlert(enabled: Boolean) = updatePets { it.copy(windowAlertEnabled = enabled) }
    fun setArrivalSummary(enabled: Boolean) {
        viewModelScope.launch { settings.setArrivalSummaryEnabled(enabled) }
    }
    fun setVoice(enabled: Boolean) = session.setVoiceGuidance(enabled)

    private fun updatePets(change: (Pet) -> Pet) {
        viewModelScope.launch {
            petList.value.ifEmpty { pets.observePets().first() }.forEach { pet ->
                pets.savePet(change(pet)).onFailure { Timber.e(it, "알림 설정 저장 실패") }
            }
        }
    }

    private fun loadIdentity() {
        viewModelScope.launch {
            _identity.value =
                runCatching {
                    link.identity()
                }.onFailure { Timber.w(it, "차량 정보 조회 실패") }.getOrNull()
        }
    }

    fun unlink() {
        viewModelScope.launch {
            link.unlink()
            showToast("차량 연동을 해지했어요")
        }
    }

    fun relink() {
        link.relink()
        showToast("차량을 다시 연결했어요")
    }

    private fun showToast(text: String) {
        viewModelScope.launch {
            _toast.value = text
            delay(2_000)
            if (_toast.value == text) _toast.value = null
        }
    }
}
