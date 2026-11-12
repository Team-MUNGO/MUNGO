package com.example.poco.presentation.mypet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.core.common.ToastMessenger
import com.example.poco.core.util.eulReul
import com.example.poco.data.model.PetTrait
import com.example.poco.data.repository.GearRepository
import com.example.poco.data.repository.PetRepository
import com.example.poco.data.repository.TripRepository
import com.example.poco.presentation.petprofile.PetForm
import com.example.poco.presentation.petprofile.toWeightInput
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 마이펫: 왼쪽 반려견 목록 + 오른쪽 프로필 보기 / 수정 / 추가.
 * - 보기에서 케어 기준 칩을 누르면 바로 저장
 * - 수정 · 추가는 "수정"(추가) 버튼을 눌러야 저장, "취소"면 버림
 */
@HiltViewModel
class MyPetViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val tripRepository: TripRepository,
    private val gearRepository: GearRepository,
    private val toastMessenger: ToastMessenger
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPetUiState())
    val uiState = _uiState.asStateFlow()

    /** 수정을 시작할 때의 사진. 취소하면 새로 고른 사진 파일은 지운다 */
    private var photoAtEditStart: String? = null

    init {
        observePets()
        observeTrips()
        observeGear()
    }

    private fun observePets() {
        viewModelScope.launch {
            petRepository.observePets()
                .catch { Timber.e(it, "펫 목록 조회 실패") }
                .collect { pets ->
                    _uiState.update { state ->
                        val selected = state.selectedId?.takeIf { id -> pets.any { it.id == id } }
                            ?: pets.firstOrNull()?.id
                        // 처음 들어왔는데 펫이 없으면 바로 추가 화면
                        val mode = if (!state.isLoaded &&
                            pets.isEmpty()
                        ) {
                            MyPetMode.ADD
                        } else {
                            state.mode
                        }
                        state.copy(pets = pets, selectedId = selected, mode = mode, isLoaded = true)
                    }
                }
        }
    }

    private fun observeTrips() {
        viewModelScope.launch {
            tripRepository.observeTripStats()
                .catch { Timber.e(it, "이동 통계 조회 실패") }
                .collect { stats -> _uiState.update { it.copy(tripStats = stats) } }
        }
    }

    /** 주행이 시작되면(P 아님) 화면을 닫는다. 수정 중이던 건 저장하지 않는다 */
    private fun observeGear() {
        viewModelScope.launch {
            gearRepository.observeIsParked()
                .catch { Timber.w(it, "기어 상태 관찰 실패 - 수정은 허용") }
                .collect { isParked ->
                    _uiState.update {
                        it.copy(isParked = isParked, isDone = it.isDone || !isParked)
                    }
                }
        }
    }

    // region 목록

    fun selectPet(id: Long) {
        if (_uiState.value.isEditing) discardEdit()
        _uiState.update { it.copy(selectedId = id, mode = MyPetMode.VIEW, errorMessage = null) }
    }

    fun startAdd() {
        if (_uiState.value.isEditing) discardEdit()
        photoAtEditStart = null
        _uiState.update { it.copy(mode = MyPetMode.ADD, form = PetForm(), errorMessage = null) }
    }

    // endregion

    // region 보기

    /** 보기 화면에서 케어 기준 칩: 바로 저장 */
    fun toggleCareInView(trait: PetTrait) {
        val state = _uiState.value
        val pet = state.selectedPet ?: return
        if (!state.canEdit) return
        val traits = pet.traits.toggle(trait)
        val form = PetForm.from(pet).copy(traits = traits).withCareApplied()
        viewModelScope.launch {
            petRepository.savePet(form.toPet())
                .onFailure { e ->
                    Timber.e(e, "케어 기준 저장 실패")
                    _uiState.update { it.copy(errorMessage = "저장하지 못했어요. 다시 시도해 주세요.") }
                }
        }
    }

    fun startEdit() {
        val state = _uiState.value
        val pet = state.selectedPet ?: return
        if (!state.canEdit) return
        photoAtEditStart = pet.photoPath
        _uiState.update {
            it.copy(
                mode = MyPetMode.EDIT,
                form = PetForm.from(pet),
                errorMessage = null
            )
        }
    }

    // endregion

    // region 수정 · 추가

    fun updateForm(transform: (PetForm) -> PetForm) =
        _uiState.update { it.copy(form = transform(it.form), errorMessage = null) }

    fun typeAge(text: String) = updateForm { it.copy(ageText = text.filter(Char::isDigit).take(2)) }

    fun typeWeight(text: String) = updateForm { form ->
        text.toWeightInput()?.let { form.copy(weightText = it) } ?: form
    }

    /** 나이 ±1살 (0~30). 비어 있으면 1살부터 */
    fun changeAge(delta: Int) = updateForm { form ->
        val current = form.ageText.toIntOrNull()
        val next = if (current == null) DEFAULT_AGE else (current + delta).coerceIn(0, MAX_AGE)
        form.copy(ageText = next.toString())
    }

    /** 몸무게 ±0.1kg (0.1~99.9). 비어 있으면 5.0kg부터 */
    fun changeWeight(deltaTenths: Int) = updateForm { form ->
        val current = form.weightKg?.let { (it * 10).roundToInt() }
        val next = if (current == null) {
            DEFAULT_WEIGHT_TENTHS
        } else {
            (current + deltaTenths).coerceIn(MIN_WEIGHT_TENTHS, MAX_WEIGHT_TENTHS)
        }
        form.copy(weightText = "%.1f".format(next / 10f))
    }

    fun toggleCare(trait: PetTrait) = updateForm { it.copy(traits = it.traits.toggle(trait)) }

    fun onPhotoPicked(uri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingPhoto = true) }
            petRepository.importPhoto(uri)
                .onSuccess { path ->
                    val old = _uiState.value.form.photoPath
                    // 이번 수정에서 골랐다가 다시 바꾼 사진은 바로 지운다
                    if (old != null && old != photoAtEditStart) petRepository.deletePhoto(old)
                    _uiState.update {
                        it.copy(isImportingPhoto = false, form = it.form.copy(photoPath = path))
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "사진 가져오기 실패")
                    _uiState.update {
                        it.copy(
                            isImportingPhoto = false,
                            errorMessage = "사진을 불러오지 못했어요. 다른 사진을 골라 주세요."
                        )
                    }
                }
        }
    }

    /** "수정" / "추가" */
    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val form = state.form.withDerivedCare(
            state.selectedPet.takeIf {
                state.mode ==
                    MyPetMode.EDIT
            }
        )
            .withCareApplied()
        val isNew = state.mode == MyPetMode.ADD
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            petRepository.savePet(form.toPet())
                .onSuccess { id ->
                    val oldPhoto = photoAtEditStart
                    if (oldPhoto != null &&
                        oldPhoto != form.photoPath
                    ) {
                        petRepository.deletePhoto(oldPhoto)
                    }
                    photoAtEditStart = null
                    _uiState.update {
                        it.copy(isSaving = false, mode = MyPetMode.VIEW, selectedId = id)
                    }
                    val name = form.name.trim()
                    if (isNew) {
                        toastMessenger.show("${name.eulReul} 추가했어요")
                    } else {
                        toastMessenger.show("$name 프로필을 수정했어요", detail = "다음 이동부터 반영돼요")
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "펫 저장 실패")
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = "저장하지 못했어요. 다시 시도해 주세요.")
                    }
                }
        }
    }

    /** "취소" · 뒤로: 바꾼 내용을 버리고 보기로 (펫이 하나도 없으면 추가 화면 그대로) */
    fun cancelEdit() {
        if (_uiState.value.pets.isEmpty()) return
        discardEdit()
        _uiState.update { it.copy(mode = MyPetMode.VIEW, errorMessage = null) }
    }

    private fun discardEdit() {
        val picked = _uiState.value.form.photoPath
        if (picked != null && picked != photoAtEditStart) {
            viewModelScope.launch { petRepository.deletePhoto(picked) }
        }
        photoAtEditStart = null
    }

    /** 수정 중인 펫 삭제 → 남은 첫 번째 펫 보기 (없으면 추가 화면) */
    fun deletePet() {
        val state = _uiState.value
        val pet = state.selectedPet ?: return
        discardEdit()
        viewModelScope.launch {
            petRepository.deletePet(pet)
                .onSuccess {
                    val next = _uiState.value.pets.firstOrNull { it.id != pet.id }
                    _uiState.update {
                        it.copy(
                            selectedId = next?.id,
                            mode = if (next == null) MyPetMode.ADD else MyPetMode.VIEW,
                            form = PetForm()
                        )
                    }
                    toastMessenger.show("${pet.name.eulReul} 삭제했어요")
                }
                .onFailure { e ->
                    Timber.e(e, "펫 삭제 실패")
                    _uiState.update { it.copy(errorMessage = "삭제하지 못했어요. 다시 시도해 주세요.") }
                }
        }
    }

    // endregion

    private fun Set<PetTrait>.toggle(trait: PetTrait) = if (trait in
        this
    ) {
        this - trait
    } else {
        this + trait
    }

    private companion object {
        const val DEFAULT_AGE = 1
        const val MAX_AGE = 30
        const val DEFAULT_WEIGHT_TENTHS = 50
        const val MIN_WEIGHT_TENTHS = 1
        const val MAX_WEIGHT_TENTHS = 999
    }
}
