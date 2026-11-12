package com.example.poco.presentation.petmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.data.model.Pet
import com.example.poco.data.repository.BatteryRepository
import com.example.poco.data.repository.GearRepository
import com.example.poco.data.repository.HvacRepository
import com.example.poco.data.repository.PetRepository
import com.example.poco.data.repository.TtsRepository
import com.example.poco.data.repository.WindowRepository
import com.example.poco.presentation.driving.nearbyWindows
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 펫모드 (정차 중, 보호자 외출).
 * - 켤 때 대표 펫(첫 번째)에 맞는 온도로 공조를 맞춘다
 * - 실내 온도 · 창문 · 배터리를 지켜 보여준다
 * - 10분마다 안심 한마디("현댕아, 금방 올게")를 들려준다
 * - 기어를 P에서 바꾸면(출발) 끝난다
 */
@HiltViewModel
class PetModeViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val hvacRepository: HvacRepository,
    private val windowRepository: WindowRepository,
    private val batteryRepository: BatteryRepository,
    private val ttsRepository: TtsRepository,
    private val gearRepository: GearRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        System.currentTimeMillis().let { now ->
            PetModeUiState(
                startedAtMillis = now,
                nowMillis = now,
                nextVoiceAtMillis = now + PetModeUiState.VOICE_INTERVAL_MS
            )
        }
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val pet = petRepository.observePets()
                .catch { Timber.e(it, "펫 목록 조회 실패") }
                .map { it.firstOrNull() }
                .filterNotNull()
                .first()
            _uiState.update { it.copy(pet = pet) }
            setComfortTemperature(pet)
            observeTemperature(pet)
            watchWindows(pet)
        }
        observeBattery()
        observeGear()
        tick()
    }

    private fun setComfortTemperature(pet: Pet) {
        viewModelScope.launch {
            val target = petModeTargetTemperature(_uiState.value.standard)
            hvacRepository.setSeatTemperature(pet.seat, target)
                .onSuccess { applied ->
                    _uiState.update {
                        it.copy(
                            targetTemperature =
                            applied ?: target
                        )
                    }
                }
                .onFailure { Timber.w(it, "펫모드 온도 맞추기 실패") }
        }
    }

    private fun observeTemperature(pet: Pet) {
        viewModelScope.launch {
            hvacRepository.observeSeatTemperature(pet.seat)
                .catch { Timber.w(it, "실내 온도 관찰 실패") }
                .collect { t -> _uiState.update { it.copy(temperature = t) } }
        }
    }

    /** 창문은 바뀔 일이 드물어서 30초마다 확인 */
    private fun watchWindows(pet: Pet) {
        viewModelScope.launch {
            val ranges = windowRepository.getSupportedWindows().getOrNull() ?: return@launch
            val windows = pet.seat.nearbyWindows().filter { it in ranges }
            if (windows.isEmpty()) return@launch
            while (true) {
                val closed = windows.all { window ->
                    val range = ranges.getValue(window)
                    val position = windowRepository.getWindowPosition(window).getOrNull()
                        ?: return@all false
                    position <= range.min + (range.max - range.min) * CLOSED_RATIO
                }
                _uiState.update { it.copy(isWindowClosed = closed) }
                delay(WINDOW_CHECK_MS)
            }
        }
    }

    private fun observeBattery() {
        viewModelScope.launch {
            var start: Pair<Int, Long>? = null
            batteryRepository.observeBatteryPercent().collect { percent ->
                val now = System.currentTimeMillis()
                val first = start ?: (percent to now).also { start = it }
                val hours = batteryHoursLeft(first.first, first.second, percent, now)
                _uiState.update { it.copy(batteryPercent = percent, batteryHoursLeft = hours) }
            }
        }
    }

    private fun observeGear() {
        viewModelScope.launch {
            gearRepository.observeIsParked()
                .catch { Timber.w(it, "기어 상태 관찰 실패") }
                .collect { isParked -> if (!isParked) finish() }
        }
    }

    /** 경과 시간 갱신 + 안심 한마디 차례면 들려준다 */
    private fun tick() {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _uiState.update { it.copy(nowMillis = now) }
                if (now >= _uiState.value.nextVoiceAtMillis) speakVoiceMessage()
                delay(TICK_MS)
            }
        }
    }

    /** 안심 한마디 (지금 바로 듣기 버튼도 이걸 부른다) */
    fun speakVoiceMessage() {
        val state = _uiState.value
        if (state.isSpeaking) return
        _uiState.update {
            it.copy(
                isSpeaking = true,
                nextVoiceAtMillis = System.currentTimeMillis() + PetModeUiState.VOICE_INTERVAL_MS
            )
        }
        viewModelScope.launch {
            ttsRepository.speak(state.voiceMessage).onFailure { Timber.w(it, "TTS 실패") }
            _uiState.update { it.copy(isSpeaking = false) }
        }
    }

    fun finish() = _uiState.update { it.copy(isDone = true) }

    private companion object {
        const val TICK_MS = 10_000L
        const val WINDOW_CHECK_MS = 30_000L

        /** 이만큼 이하로 열려 있으면 닫힘으로 본다 (센서 오차) */
        const val CLOSED_RATIO = 0.02f
    }
}
