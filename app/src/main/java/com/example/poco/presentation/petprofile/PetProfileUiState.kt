package com.example.poco.presentation.petprofile

import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.PetSize
import com.example.poco.data.model.PetSpecies
import com.example.poco.data.model.PetTrait
import com.example.poco.data.model.Restraint
import com.example.poco.data.model.VoiceFrequency

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
    val restraint: Restraint = Restraint.NONE,
    // 아래는 펫 프로필 화면에선 안 바꾸지만 저장할 때 유지
    val species: PetSpecies = PetSpecies.DOG,
    val traits: Set<PetTrait> = emptySet(),
    val voiceGuideEnabled: Boolean = true,
    val temperatureAlertEnabled: Boolean = true,
    val windowAlertEnabled: Boolean = true,
    val restSuggestionEnabled: Boolean = true
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
        restraint = restraint,
        species = species,
        traits = traits,
        voiceGuideEnabled = voiceGuideEnabled,
        temperatureAlertEnabled = temperatureAlertEnabled,
        windowAlertEnabled = windowAlertEnabled,
        restSuggestionEnabled = restSuggestionEnabled
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
            restraint = pet.restraint,
            species = pet.species,
            traits = pet.traits,
            voiceGuideEnabled = pet.voiceGuideEnabled,
            temperatureAlertEnabled = pet.temperatureAlertEnabled,
            windowAlertEnabled = pet.windowAlertEnabled,
            restSuggestionEnabled = pet.restSuggestionEnabled
        )
    }
}

/** 몸무게: 숫자와 점 하나, 소수점 한 자리까지. 규칙에 안 맞으면 null(입력 무시) */
fun String.toWeightInput(): String? {
    val filtered = filter { it.isDigit() || it == '.' }
    if (filtered.count { it == '.' } > 1) return null
    if (filtered.substringAfter('.', "").length > 1) return null
    return filtered.take(5)
}
