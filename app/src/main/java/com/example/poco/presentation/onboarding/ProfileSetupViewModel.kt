package com.example.poco.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.PetSpecies
import com.example.poco.data.model.PetTrait
import com.example.poco.data.model.VoiceFrequency
import com.example.poco.data.repository.OnboardingRepository
import com.example.poco.data.repository.PetRepository
import com.example.poco.presentation.mypet.withDerivedCare
import com.example.poco.presentation.petprofile.PetForm
import com.example.poco.presentation.petprofile.toWeightInput
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 온보딩 프로필 만들기 (1/3 → 2/3 → 3/3 → 완성).
 * 입력은 화면 안에서만 들고 있다가 완성 화면의 "시작하기"에서 한 번에 저장한다
 * (중간에 그만두면 반쪽짜리 펫이 남지 않게)
 */
@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState = _uiState.asStateFlow()

    private fun updateForm(transform: (PetForm) -> PetForm) {
        _uiState.update { it.copy(form = transform(it.form), errorMessage = null) }
    }

    // region 1/3

    fun updateName(name: String) = updateForm { it.copy(name = name.take(MAX_NAME_LENGTH)) }

    fun selectSpecies(species: PetSpecies) = updateForm { it.copy(species = species) }

    fun updateBreed(breed: String) = updateForm { it.copy(breed = breed.take(MAX_NAME_LENGTH)) }

    /** 나이 ±1살. 처음 누르면 [DEFAULT_AGE]부터 */
    fun changeAge(delta: Int) = updateForm { form ->
        val current = form.ageText.toIntOrNull()
        val next = if (current == null) DEFAULT_AGE else (current + delta).coerceIn(0, MAX_AGE)
        form.copy(ageText = next.toString())
    }

    /** 키패드로 나이 입력: 숫자만 2자리 */
    fun typeAge(text: String) = updateForm { it.copy(ageText = text.filter(Char::isDigit).take(2)) }

    /** 키패드로 몸무게 입력: 숫자 + 소수점 한 자리. 규칙에 안 맞는 글자는 무시 */
    fun typeWeight(text: String) = updateForm { form ->
        text.toWeightInput()?.let { form.copy(weightText = it) } ?: form
    }

    /** 몸무게 ±0.1kg (길게 누르면 반복). 처음 누르면 [DEFAULT_WEIGHT_KG]부터. 크기는 몸무게로 자동 */
    fun changeWeight(deltaTenths: Int) = updateForm { form ->
        val current = form.weightKg?.let { (it * 10).roundToInt() }
        val next = if (current == null) {
            DEFAULT_WEIGHT_TENTHS
        } else {
            (current + deltaTenths).coerceIn(MIN_WEIGHT_TENTHS, MAX_WEIGHT_TENTHS)
        }
        form.copy(weightText = (next / 10f).toWeightLabel())
    }

    // endregion

    // region 2/3 · 3/3

    fun toggleTrait(trait: PetTrait) = updateForm { form ->
        form.copy(traits = if (trait in form.traits) form.traits - trait else form.traits + trait)
    }

    fun setVoiceGuide(enabled: Boolean) = updateForm { it.copy(voiceGuideEnabled = enabled) }

    fun setTemperatureAlert(enabled: Boolean) =
        updateForm { it.copy(temperatureAlertEnabled = enabled) }

    fun setWindowAlert(enabled: Boolean) = updateForm { it.copy(windowAlertEnabled = enabled) }

    fun setRestSuggestion(enabled: Boolean) = updateForm {
        it.copy(restSuggestionEnabled = enabled)
    }

    // endregion

    // region 완성

    fun onPhotoPicked(uri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingPhoto = true) }
            petRepository.importPhoto(uri)
                .onSuccess { path ->
                    val old = _uiState.value.form.photoPath
                    _uiState.update {
                        it.copy(isImportingPhoto = false, form = it.form.copy(photoPath = path))
                    }
                    // 바꾸기 전 사진은 아직 어디에도 저장 안 됐으니 지운다
                    old?.let { petRepository.deletePhoto(it) }
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

    // endregion

    /** 다음 단계. 3/3 → 완성 화면 */
    fun next() {
        val state = _uiState.value
        if (!state.canGoNext) return
        val next = ProfileStep.entries.getOrNull(state.step.ordinal + 1) ?: return
        _uiState.update { it.copy(step = next) }
    }

    /** 이전 단계. 1/3에서는 false (화면 밖으로 나가야 함) */
    fun back(): Boolean {
        val previous =
            ProfileStep.entries.getOrNull(_uiState.value.step.ordinal - 1) ?: return false
        _uiState.update { it.copy(step = previous) }
        return true
    }

    /** 완성 화면 "시작하기": 고른 모습을 기준에 반영해 저장 */
    fun finish() {
        val state = _uiState.value
        if (state.isSaving || state.form.name.isBlank()) return
        val traits = state.form.traits
        // 정보로 알 수 있는 케어 기준(크기별 노령견 · 단두종 더위)도 같이 저장
        val form = state.form.withDerivedCare(original = null).copy(
            motionSensitivity = if (PetTrait.MOTION_SICK in traits) {
                MotionSensitivity.HIGH
            } else {
                MotionSensitivity.NORMAL
            },
            voiceFrequency = if (PetTrait.ANXIOUS in
                traits
            ) {
                VoiceFrequency.LOW
            } else {
                VoiceFrequency.NORMAL
            }
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            petRepository.savePet(form.toPet())
                .onSuccess { id ->
                    // 프로필 완성 → 바로 홈. 다음 실행부터 온보딩을 다시 보이지 않도록 첫 실행 완료로 남긴다
                    // (차량 권한은 홈에 들어가서 시스템 창으로 한 번 요청한다)
                    onboardingRepository.setPermissionCompleted()
                        .onFailure { Timber.e(it, "첫 실행 완료 저장 실패") }
                    // 다시 돌아와 저장해도 같은 펫을 고치도록 id를 남긴다
                    _uiState.update {
                        it.copy(isSaving = false, isDone = true, form = it.form.copy(id = id))
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "프로필 저장 실패")
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = "저장하지 못했어요. 다시 시도해 주세요.")
                    }
                }
        }
    }

    fun consumeDone() = _uiState.update { it.copy(isDone = false) }

    private companion object {
        const val MAX_NAME_LENGTH = 20
        const val DEFAULT_AGE = 1
        const val MAX_AGE = 30
        const val DEFAULT_WEIGHT_TENTHS = 50 // 5.0kg
        const val MIN_WEIGHT_TENTHS = 1
        const val MAX_WEIGHT_TENTHS = 999
    }
}
