package com.example.poco.presentation.petprofile

import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.PetSize
import com.example.poco.data.model.Restraint
import com.example.poco.data.model.VoiceFrequency
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class PetProfileUiState(
    val pets: ImmutableList<Pet> = persistentListOf(),
    val form: PetForm = PetForm(),
    /** null = 기어 상태를 아직 모름 (연결 전) → 수정 허용 */
    val isParked: Boolean? = null,
    val isSaving: Boolean = false,
    /** 마지막 변경까지 저장됨 (다시 바꾸면 false) */
    val isSaved: Boolean = false,
    /** 사진을 복사하는 중 */
    val isImportingPhoto: Boolean = false,
    val errorMessage: String? = null,
    val isDone: Boolean = false
) {
    /** 자동 저장 조건: 이름이 있고 정차 중 */
    val canSave: Boolean get() = form.name.isNotBlank() && isParked != false
    val isNewPet: Boolean get() = form.id == 0L
}

/** 편집 중인 입력값. 숫자는 입력 그대로 문자열로 들고 있다가 저장할 때 변환 */
data class PetForm(
    val id: Long = 0,
    val name: String = "",
    val breed: String = "",
    val ageText: String = "",
    val weightText: String = "",
    val seat: PetSeat = PetSeat.REAR_LEFT,
    val motionSensitivity: MotionSensitivity = MotionSensitivity.NORMAL,
    val windowAlertPercent: Int = Pet.DEFAULT_WINDOW_ALERT_PERCENT,
    /** 화면에선 안 바꾸지만 저장할 때 유지 */
    val voiceFrequency: VoiceFrequency = VoiceFrequency.NORMAL,
    /** 앱 내부에 복사된 사진 경로 */
    val photoPath: String? = null,
    val restraint: Restraint = Restraint.NONE
) {
    val weightKg: Float? get() = weightText.toFloatOrNull()?.takeIf { it > 0f }
    val size: PetSize? get() = weightKg?.let(PetSize::fromWeight)

    fun toPet() = Pet(
        id = id,
        name = name.trim(),
        breed = breed.trim(),
        ageYears = ageText.toIntOrNull(),
        weightKg = weightKg,
        seat = seat,
        motionSensitivity = motionSensitivity,
        voiceFrequency = voiceFrequency,
        windowAlertPercent = windowAlertPercent,
        photoPath = photoPath,
        restraint = restraint
    )

    companion object {
        /** 4.0 → "4", 4.2 → "4.2" */
        private fun Float.toWeightText(): String =
            if (this % 1f == 0f) toInt().toString() else toString()

        fun from(pet: Pet) = PetForm(
            id = pet.id,
            name = pet.name,
            breed = pet.breed,
            ageText = pet.ageYears?.toString().orEmpty(),
            weightText = pet.weightKg?.toWeightText().orEmpty(),
            seat = pet.seat,
            motionSensitivity = pet.motionSensitivity,
            windowAlertPercent = pet.windowAlertPercent,
            voiceFrequency = pet.voiceFrequency,
            photoPath = pet.photoPath,
            restraint = pet.restraint
        )
    }
}
