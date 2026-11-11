package com.example.poco.presentation.petprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.core.common.ToastMessenger
import com.example.poco.core.util.eulReul
import com.example.poco.data.repository.GearRepository
import com.example.poco.data.repository.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * 펫 프로필은 따로 저장 버튼 없이 바꾸는 즉시 저장한다.
 * - 버튼(자리 · 민감도 · 고정 방식 · 창문 기준) · 사진: 바로 저장
 * - 글자 입력(이름 · 종류 · 나이 · 몸무게): 입력이 멈추고 0.6초 뒤 저장
 * - 새 펫은 이름을 넣는 순간 목록에 추가된다
 */
@HiltViewModel
class PetProfileViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val gearRepository: GearRepository,
    private val toastMessenger: ToastMessenger
) : ViewModel() {
    private val _uiState = MutableStateFlow(PetProfileUiState())
    val uiState = _uiState.asStateFlow()

    private var isFormLoaded = false

    /** 저장은 한 번에 하나씩 (새 펫이 두 번 추가되지 않게) */
    private val saveMutex = Mutex()
    private var saveJob: Job? = null

    /** 다른 펫을 고르거나 새 펫을 시작하면 바뀜 → 이전 폼의 저장 결과가 새 폼에 섞이지 않게 */
    private var draftToken = 0L

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
                        // 처음 한 번만 첫 번째 펫으로 폼을 채운다 (편집 중 덮어쓰기 방지)
                        val form = if (!isFormLoaded && pets.isNotEmpty()) {
                            PetForm.from(pets.first())
                        } else {
                            state.form
                        }
                        isFormLoaded = true
                        state.copy(pets = pets.toImmutableList(), form = form)
                    }
                }
        }
    }

    /** 기어 P에서만 수정 가능. D 등으로 바뀌면 자동으로 홈으로 돌아간다 */
    private fun observeGear() {
        viewModelScope.launch {
            gearRepository.observeIsParked()
                .catch { Timber.w(it, "기어 상태 관찰 실패 - 수정은 허용") }
                .collect { isParked ->
                    if (!isParked) flushThen { _uiState.update { it.copy(isDone = true) } }
                    _uiState.update { it.copy(isParked = isParked) }
                }
        }
    }

    /** 글자 입력: 입력이 멈추면 저장 */
    fun updateForm(transform: (PetForm) -> PetForm) {
        applyChange(transform)
        scheduleSave(TEXT_SAVE_DELAY_MS)
    }

    /** 버튼 선택: 바로 저장 */
    fun selectOption(transform: (PetForm) -> PetForm) {
        applyChange(transform)
        scheduleSave(0L)
    }

    private fun applyChange(transform: (PetForm) -> PetForm) {
        _uiState.update {
            val form = transform(it.form)
            it.copy(form = form, errorMessage = null, isSaved = it.isSaved && form == it.form)
        }
    }

    /** 사진 선택기에서 고른 사진 → 앱 내부로 복사 후 바로 저장 */
    fun onPhotoPicked(uri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingPhoto = true) }
            petRepository.importPhoto(uri)
                .onSuccess { path ->
                    _uiState.update { it.copy(isImportingPhoto = false) }
                    selectOption { it.copy(photoPath = path) }
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

    fun selectPet(id: Long) {
        val pet = _uiState.value.pets.find { it.id == id } ?: return
        flushThen { startDraft(PetForm.from(pet), isSaved = true) }
    }

    fun addNewPet() {
        flushThen { startDraft(PetForm(), isSaved = false) }
    }

    /** 뒤로: 입력 중이던 내용까지 저장하고 나간다 */
    fun leave() {
        flushThen { _uiState.update { it.copy(isDone = true) } }
    }

    private fun startDraft(form: PetForm, isSaved: Boolean) {
        draftToken++
        _uiState.update { it.copy(form = form, errorMessage = null, isSaved = isSaved) }
    }

    private fun scheduleSave(delayMs: Long) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(delayMs)
            saveCurrentForm()
        }
    }

    /** 기다리던 저장이 있으면 먼저 끝내고 [action] */
    private fun flushThen(action: () -> Unit) {
        val pending = saveJob?.takeIf { it.isActive }
        pending?.cancel()
        saveJob = null
        viewModelScope.launch {
            if (pending != null) saveCurrentForm()
            action()
        }
    }

    private suspend fun saveCurrentForm() = saveMutex.withLock {
        val state = _uiState.value
        if (!state.canSave) return@withLock
        val form = state.form
        val token = draftToken
        _uiState.update { it.copy(isSaving = true) }
        petRepository.savePet(form.toPet())
            .onSuccess { id ->
                val isNew = form.id == 0L
                _uiState.update { s ->
                    // 새 펫: 받은 id를 폼에 넣어 다음 저장부터는 같은 펫을 고친다
                    val newForm = if (isNew && token == draftToken && s.form.id == 0L) {
                        s.form.copy(id = id)
                    } else {
                        s.form
                    }
                    s.copy(form = newForm, isSaving = false, isSaved = s.form == form)
                }
                if (isNew) toastMessenger.show("${form.name.trim().eulReul} 펫으로 추가했어요")
            }
            .onFailure { e ->
                Timber.e(e, "펫 저장 실패")
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "저장하지 못했어요. 다시 시도해 주세요.")
                }
            }
    }

    private companion object {
        const val TEXT_SAVE_DELAY_MS = 600L
    }
}
