package com.example.poco.presentation.onboarding

import com.example.poco.data.model.PetSpecies
import com.example.poco.data.model.PetTrait
import com.example.poco.presentation.petprofile.PetForm
import com.example.poco.presentation.petprofile.label

/** 온보딩 프로필 만들기 단계 (1/3 · 2/3 · 3/3 · 완성) */
enum class ProfileStep(val number: Int) {
    /** 1/3 이름 · 종류 · 품종 · 나이 · 몸무게 */
    BASIC(1),

    /** 2/3 차에서 어떤 편인가요? */
    TRAITS(2),

    /** 3/3 어떤 안내를 받으시겠어요? */
    ALERTS(3),

    /** 프로필을 완성했어요 */
    COMPLETE(4);

    companion object {
        const val TOTAL = 3
    }
}

data class ProfileSetupUiState(
    val step: ProfileStep = ProfileStep.BASIC,
    val form: PetForm = PetForm(),
    val isImportingPhoto: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** 저장을 마쳐 다음 화면(권한)으로 가도 된다 */
    val isDone: Boolean = false
) {
    val ageYears: Int? get() = form.ageText.toIntOrNull()
    val weightKg: Float? get() = form.weightKg

    /** 1/3은 이름이 있어야 다음으로 */
    val canGoNext: Boolean get() = step != ProfileStep.BASIC || form.name.isNotBlank()

    /** 더위 · 추위를 고르면 실내 온도 알림을 추천 */
    val isTemperatureRecommended: Boolean
        get() = form.traits.any { it == PetTrait.HEAT_SENSITIVE || it == PetTrait.COLD_SENSITIVE }

    /** 멀미 · 불안 · 침을 고르면 휴식 쉼터 제안을 추천 */
    val isRestRecommended: Boolean
        get() = form.traits.any {
            it == PetTrait.MOTION_SICK || it == PetTrait.ANXIOUS || it == PetTrait.DROOLS
        }
}

/** 몸무게 표시: 7 → "7.0" */
fun Float.toWeightLabel(): String = "%.1f".format(this)

fun PetSpecies.label(): String = when (this) {
    PetSpecies.DOG -> "강아지"
    PetSpecies.CAT -> "고양이"
    PetSpecies.OTHER -> "기타"
}

/** 성향 카드 제목 */
fun PetTrait.title(): String = when (this) {
    PetTrait.MOTION_SICK -> "멀미해요"
    PetTrait.ANXIOUS -> "불안해해요"
    PetTrait.HEAT_SENSITIVE -> "더위를 타요"
    PetTrait.COLD_SENSITIVE -> "추위를 타요"
    PetTrait.DROOLS -> "침을 흘려요"
    PetTrait.LOVES_RIDES -> "이동을 좋아해요"
    PetTrait.SENIOR -> "노령견"
    PetTrait.JOINT_WEAK -> "관절이 약해요"
}

/** 성향 카드 설명 = 고르면 앱이 어떻게 지켜보는지 */
fun PetTrait.description(): String = when (this) {
    PetTrait.MOTION_SICK -> "흔들림을 더 꼼꼼히 봐요"
    PetTrait.ANXIOUS -> "음성 안내를 차분하게 해요"
    PetTrait.HEAT_SENSITIVE -> "온도 기준을 낮게 잡아요"
    PetTrait.COLD_SENSITIVE -> "온도 기준을 높게 잡아요"
    PetTrait.DROOLS -> "멀미 징후로 기록해요"
    PetTrait.LOVES_RIDES -> "평소대로 지켜봐요"
    PetTrait.SENIOR -> "온도 기준을 좁게 잡아요"
    PetTrait.JOINT_WEAK -> "흔들림을 더 꼼꼼히 봐요"
}

/** 완성 화면 칩에 쓰는 짧은 이름 */
fun PetTrait.shortLabel(): String = when (this) {
    PetTrait.MOTION_SICK -> "멀미"
    PetTrait.ANXIOUS -> "불안"
    PetTrait.HEAT_SENSITIVE -> "더위"
    PetTrait.COLD_SENSITIVE -> "추위"
    PetTrait.DROOLS -> "침"
    PetTrait.LOVES_RIDES -> "이동 좋아함"
    PetTrait.SENIOR -> "노령"
    PetTrait.JOINT_WEAK -> "관절"
}

/** "시츄 / 10살 / 7.0kg / 소형" (빈 값은 뺀다) */
fun PetForm.summaryLine(): String = listOfNotNull(
    breed.trim().takeIf { it.isNotEmpty() },
    ageText.toIntOrNull()?.let { "${it}살" },
    weightKg?.let { "${it.toWeightLabel()}kg" },
    size?.label()
).joinToString(" / ")

/** 온보딩 2/3 "차에서 어떤 편인가요?"에서 고르는 모습 (노령 · 관절은 마이펫 케어 기준에서) */
val ONBOARDING_TRAITS = listOf(
    PetTrait.MOTION_SICK,
    PetTrait.ANXIOUS,
    PetTrait.HEAT_SENSITIVE,
    PetTrait.COLD_SENSITIVE,
    PetTrait.DROOLS,
    PetTrait.LOVES_RIDES
)
