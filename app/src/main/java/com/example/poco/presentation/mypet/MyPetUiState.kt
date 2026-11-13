package com.example.poco.presentation.mypet

import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetTrait
import com.example.poco.data.model.TripStats
import com.example.poco.data.model.VoiceFrequency
import com.example.poco.domain.driving.MungoStandard
import com.example.poco.domain.driving.PetType
import com.example.poco.presentation.boarding.toCelsius
import com.example.poco.presentation.petprofile.PetForm

/** 오른쪽 영역: 프로필 보기 · 수정 · 새 반려견 추가 */
enum class MyPetMode { VIEW, EDIT, ADD }

data class MyPetUiState(
    val pets: List<Pet> = emptyList(),
    val selectedId: Long? = null,
    val mode: MyPetMode = MyPetMode.VIEW,
    /** 수정 · 추가 중인 값 */
    val form: PetForm = PetForm(),
    /** null = 기어 상태를 아직 모름 → 수정 허용 */
    val isParked: Boolean? = null,
    val tripStats: TripStats = TripStats(),
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val isImportingPhoto: Boolean = false,
    val errorMessage: String? = null,
    /** 주행이 시작돼 화면을 닫아야 함 */
    val isDone: Boolean = false
) {
    val selectedPet: Pet? get() = pets.firstOrNull { it.id == selectedId }

    val isEditing: Boolean get() = mode != MyPetMode.VIEW

    val canEdit: Boolean get() = isParked != false

    val canSave: Boolean get() = form.name.isNotBlank() && !isSaving && canEdit
}

/** 마이펫 케어 기준 칩 (보기 · 수정 공통, 이 순서대로) */
val CARE_TRAITS = listOf(
    PetTrait.SENIOR,
    PetTrait.HEAT_SENSITIVE,
    PetTrait.MOTION_SICK,
    PetTrait.ANXIOUS,
    PetTrait.JOINT_WEAK
)

/** 케어 기준 칩 글자. 단두종이면 더위 칩에 이유를 붙인다 */
fun PetTrait.careLabel(breed: String = ""): String = when (this) {
    PetTrait.SENIOR -> "노령견"
    PetTrait.HEAT_SENSITIVE ->
        if (isBrachycephalic(breed)) "단두종 · 더위에 약해요" else "더위에 약해요"
    PetTrait.MOTION_SICK -> "멀미해요"
    PetTrait.ANXIOUS -> "겁이 많아요"
    PetTrait.JOINT_WEAK -> "관절이 약해요"
    PetTrait.COLD_SENSITIVE -> "추위에 약해요"
    PetTrait.DROOLS -> "침을 흘려요"
    PetTrait.LOVES_RIDES -> "이동을 좋아해요"
}

private fun isBrachycephalic(breed: String): Boolean =
    PetType.BRACHYCEPHALIC in MungoStandard.petTypes(breed, null, null)

/** 정보로 알 수 있는 케어 기준: 8살 이상 → 노령견, 단두종 → 더위에 약해요 */
fun derivedCareTraits(breed: String, weightKg: Float?, ageYears: Int?): Set<PetTrait> = buildSet {
    if ((ageYears ?: 0) >= SENIOR_AGE) add(PetTrait.SENIOR)
    if (PetType.BRACHYCEPHALIC in MungoStandard.petTypes(breed, weightKg, ageYears)) {
        add(PetTrait.HEAT_SENSITIVE)
    }
}

/**
 * 저장할 성향: 고른 것 + 이번에 새로 해당하게 된 것 (예: 나이를 8살로 바꿈).
 * 원래부터 해당했는데 운전자가 끈 건 다시 켜지 않는다
 */
fun PetForm.withDerivedCare(original: Pet?): PetForm {
    val now = derivedCareTraits(breed, weightKg, ageText.toIntOrNull())
    val before = original?.let { derivedCareTraits(it.breed, it.weightKg, it.ageYears) }.orEmpty()
    return copy(traits = traits + (now - before))
}

/** 케어 기준을 주행 기준에 반영: 멀미 · 관절 → 흔들림 민감, 겁 → 음성 안내 차분하게 */
fun PetForm.withCareApplied(): PetForm = copy(
    motionSensitivity = if (PetTrait.MOTION_SICK in traits || PetTrait.JOINT_WEAK in traits) {
        MotionSensitivity.HIGH
    } else {
        MotionSensitivity.NORMAL
    },
    voiceFrequency = if (PetTrait.ANXIOUS in traits) VoiceFrequency.LOW else VoiceFrequency.NORMAL
)

/** 휴식 추천 간격 (표시용). 멀미 · 관절이 약하면 더 자주 */
fun restIntervalLabel(traits: Set<PetTrait>): String =
    if (PetTrait.MOTION_SICK in traits || PetTrait.JOINT_WEAK in traits) "1시간마다" else "2시간마다"

/** 실내 적정 온도 "22~24°C" */
fun comfortRangeLabel(pet: Pet): String {
    val comfort = MungoStandard.temperature(
        MungoStandard.petTypes(pet.breed, pet.weightKg, pet.ageYears)
    ).comfort
    return "${comfort.start.toCelsius()}~${comfort.endInclusive.toCelsius()}°C"
}

/** 왼쪽 목록 "시츄 | 10살" */
fun Pet.listSummary(): String = listOfNotNull(
    breed.takeIf { it.isNotBlank() },
    ageYears?.let { "${it}살" }
).joinToString(" | ")

private const val SENIOR_AGE = 8
