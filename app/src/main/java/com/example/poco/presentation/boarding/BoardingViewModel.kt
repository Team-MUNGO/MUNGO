package com.example.poco.presentation.boarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.Restraint
import com.example.poco.data.repository.GearRepository
import com.example.poco.data.repository.HvacRepository
import com.example.poco.data.repository.PetRepository
import com.example.poco.data.repository.SttRepository
import com.example.poco.data.repository.TtsRepository
import com.example.poco.data.repository.WindowRepository
import com.example.poco.presentation.driving.nearbyWindows
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 탑승 준비 (정차 중에만).
 * 1/3 함께 탈 펫 → 2/3 자리 → 3/3 출발 전 확인 → 출발 준비 끝 → 주행 화면 분할 고르기 → 주행
 * 자리 · 고정 방식은 3/3을 마칠 때 펫 정보에 저장한다 (주행 중 창문 · 흔들림 기준에 쓰임)
 */
@HiltViewModel
class BoardingViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val gearRepository: GearRepository,
    private val sttRepository: SttRepository,
    private val ttsRepository: TtsRepository,
    private val windowRepository: WindowRepository,
    private val hvacRepository: HvacRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BoardingUiState())
    val uiState = _uiState.asStateFlow()

    private var listenJob: Job? = null
    private var isPetsLoaded = false

    init {
        observePets()
        observeGear()
    }

    private fun observePets() {
        viewModelScope.launch {
            petRepository.observePets()
                .catch { Timber.e(it, "펫 목록 조회 실패") }
                .collect { pets ->
                    _uiState.update { state ->
                        // 처음엔 첫 번째 펫을 골라 둔다. 지워진 펫은 선택에서 뺀다
                        val ids = pets.map { it.id }.toSet()
                        val selected = if (!isPetsLoaded) {
                            listOfNotNull(pets.firstOrNull()?.id)
                        } else {
                            state.selectedIds.filter { it in ids }
                        }
                        isPetsLoaded = true
                        state.copy(pets = pets, selectedIds = selected)
                    }
                }
        }
    }

    /** 정차 중에만 진행: 출발하면(P 아님) 바로 주행 화면으로 */
    private fun observeGear() {
        viewModelScope.launch {
            gearRepository.observeIsParked()
                .catch { Timber.w(it, "기어 상태 관찰 실패") }
                .collect { isParked -> if (!isParked) _uiState.update { it.copy(isDone = true) } }
        }
    }

    // region 1/3

    fun togglePet(id: Long) = _uiState.update {
        val selected = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id
        it.copy(selectedIds = selected)
    }

    // endregion

    // region 2/3

    /** 펫 자리를 펫 정보에 있던 자리로 채운다. 겹치면 뒤 펫은 비워 둔다 */
    private fun prepareSeats() = _uiState.update { state ->
        val seats = mutableMapOf<Long, PetSeat>()
        state.selectedPets.forEach { pet ->
            val seat = state.seats[pet.id] ?: pet.seat
            if (seat !in seats.values) seats[pet.id] = seat
        }
        val active = state.selectedIds.firstOrNull { it !in seats } ?: state.selectedIds.first()
        state.copy(seats = seats, activePetId = active)
    }

    /** 자리 누르기: 다른 펫 자리면 그 펫을 고르는 중으로, 빈 자리면 지금 펫을 앉힌다 */
    fun selectSeat(seat: PetSeat) = _uiState.update { state ->
        val owner = state.seats.entries.firstOrNull { it.value == seat }?.key
        val active = state.activePetId ?: return@update state
        when (owner) {
            active -> state
            null -> {
                val seats = state.seats + (active to seat)
                val nextActive = state.selectedIds.firstOrNull { it !in seats } ?: active
                state.copy(seats = seats, activePetId = nextActive)
            }
            else -> state.copy(activePetId = owner)
        }
    }

    fun selectActivePet(id: Long) = _uiState.update { it.copy(activePetId = id) }

    // endregion

    // region 3/3

    fun toggleCheck(item: CheckItem) = _uiState.update { state ->
        // 차량에서 이미 확인한 항목은 그대로
        if (state.autoResult(item) == true) return@update state
        val checks = state.manualChecks
        state.copy(manualChecks = if (item in checks) checks - item else checks + item)
    }

    /** 고른 자리 옆 창문 · 대표 펫 자리 온도를 차량에서 읽는다 */
    private fun loadVehicleChecks() {
        val state = _uiState.value
        val seats = state.selectedIds.mapNotNull { state.seats[it] }
        viewModelScope.launch {
            val ranges = windowRepository.getSupportedWindows().getOrNull()
            val closed = ranges?.let {
                val windows = seats.flatMap { it.nearbyWindows() }.distinct()
                    .filter { it in ranges }
                if (windows.isEmpty()) return@let null
                windows.all { window ->
                    val range = ranges.getValue(window)
                    val position = windowRepository.getWindowPosition(window).getOrNull()
                        ?: return@let null
                    position <= range.min + (range.max - range.min) * CLOSED_RATIO
                }
            }
            _uiState.update { it.copy(isWindowClosed = closed) }
        }
        val mainSeat = seats.firstOrNull() ?: return
        viewModelScope.launch {
            val temperature = hvacRepository.getSeatTemperature(mainSeat)
                .onFailure { Timber.w(it, "자리 온도 읽기 실패") }
                .getOrNull()
            _uiState.update { it.copy(seatTemperature = temperature) }
        }
    }

    /** 3/3을 마치면 자리 · 고정 방식(카시트 · 하네스)을 펫 정보에 저장 */
    private fun saveAndFinishChecks() {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            state.selectedPets.forEach { pet ->
                val seat = state.seats[pet.id] ?: pet.seat
                petRepository.savePet(pet.copy(seat = seat, restraint = Restraint.CRATE_HARNESS))
                    .onFailure { Timber.e(it, "탑승 정보 저장 실패: ${pet.name}") }
            }
            _uiState.update { it.copy(isSaving = false, step = BoardingStep.READY) }
            speakReady()
        }
    }

    // endregion

    // region 출발 준비 끝

    private fun speakReady() {
        val pets = _uiState.value.selectedPets
        if (pets.none { it.voiceGuideEnabled }) return
        viewModelScope.launch {
            ttsRepository.speak(readyMessage(pets.map { it.name }))
                .onFailure { Timber.w(it, "TTS 실패") }
        }
    }

    fun openLayoutDialog() = _uiState.update { it.copy(isLayoutDialogVisible = true) }

    fun closeLayoutDialog() {
        listenJob?.cancel()
        _uiState.update { it.copy(isLayoutDialogVisible = false, isListening = false) }
    }

    /** 분할을 고르면 바로 주행 시작 */
    fun startDriving(layout: DriveLayout) {
        // TODO Pleos 앱 영역 분할을 앱에서 바꿀 수 있으면 여기서 적용 (지금은 고른 값만 기억)
        _uiState.update { it.copy(layout = layout, isLayoutDialogVisible = false, isDone = true) }
    }

    // endregion

    // region 단계 이동

    fun next() {
        val state = _uiState.value
        if (!state.canGoNext) return
        when (state.step) {
            BoardingStep.COMPANIONS -> {
                prepareSeats()
                _uiState.update { it.copy(step = BoardingStep.SEAT) }
            }
            BoardingStep.SEAT -> {
                _uiState.update { it.copy(step = BoardingStep.CHECKLIST) }
                loadVehicleChecks()
            }
            BoardingStep.CHECKLIST -> saveAndFinishChecks()
            BoardingStep.READY -> openLayoutDialog()
        }
        stopListening()
    }

    /** 이전 단계. 1/3에서는 false (화면을 닫아야 함) */
    fun back(): Boolean {
        val state = _uiState.value
        if (state.isLayoutDialogVisible) {
            closeLayoutDialog()
            return true
        }
        val previous = BoardingStep.entries.getOrNull(state.step.ordinal - 1) ?: return false
        stopListening()
        _uiState.update { it.copy(step = previous) }
        return true
    }

    fun consumeDone() = _uiState.update { it.copy(isDone = false) }

    // endregion

    // region 음성

    /**
     * 마이크 한 번 듣기. 단계마다 알아듣는 말이 다르다
     * 1/3 "현댕이랑 같이 탈거야" · 3/3 "다 확인했어" · 분할 "네비랑 같이 보여 줘"
     */
    fun listen() {
        if (_uiState.value.isListening) return
        listenJob = viewModelScope.launch {
            _uiState.update { it.copy(isListening = true, heardText = null) }
            val heard = sttRepository.listenOnce(LISTEN_TIMEOUT_MS)
                .onFailure { Timber.w(it, "STT 실패 - 화면으로 고르기") }
                .getOrNull()
            _uiState.update { it.copy(isListening = false, heardText = heard) }
            onHeard(heard)
        }
    }

    private fun onHeard(heard: String?) {
        val state = _uiState.value
        when {
            state.isLayoutDialogVisible -> parseLayoutReply(heard)?.let(::startDriving)
            state.step == BoardingStep.COMPANIONS -> {
                val matched = matchPetsByVoice(heard, state.pets)
                if (matched.isNotEmpty()) {
                    _uiState.update { s ->
                        s.copy(
                            selectedIds = s.pets.map { it.id }.filter {
                                it in
                                    matched
                            }
                        )
                    }
                }
            }
            state.step == BoardingStep.CHECKLIST && isAllCheckedReply(heard) ->
                _uiState.update { it.copy(manualChecks = CheckItem.entries.toSet()) }
            else -> Unit
        }
    }

    private fun stopListening() {
        listenJob?.cancel()
        _uiState.update { it.copy(isListening = false, heardText = null) }
    }

    // endregion

    override fun onCleared() {
        listenJob?.cancel()
    }

    private companion object {
        const val LISTEN_TIMEOUT_MS = 6_000L

        /** 이만큼 이하로 열려 있으면 닫힘으로 본다 (센서 오차) */
        const val CLOSED_RATIO = 0.02f
    }
}

/** "현댕이 잘 앉았어요. 출발하면 흔들림을 같이 지켜볼게요." */
fun readyMessage(names: List<String>): String =
    "${names.joinToString(", ")} 잘 앉았어요. 출발하면 흔들림을 같이 지켜볼게요."
