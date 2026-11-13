package com.example.poco.presentation.boarding

import com.example.poco.core.util.eunNeun
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.domain.driving.PetType
import com.example.poco.domain.driving.PocoStandard
import com.example.poco.domain.driving.TemperatureStandard
import kotlin.math.roundToInt

/** 탑승 준비 단계. 1~3은 '안전동승체크 n / 3', 4는 출발 준비 끝 */
enum class BoardingStep(val number: Int) {
    /** 1/3 오늘은 누구와 함께 이동하나요? */
    COMPANIONS(1),

    /** 2/3 어디에 앉나요? */
    SEAT(2),

    /** 3/3 출발 전에 함께 확인해요 */
    CHECKLIST(3),

    /** 출발 준비가 끝났어요 (+ 주행 화면 분할 고르기) */
    READY(4);

    companion object {
        const val TOTAL = 3
    }
}

/** 출발 전 확인 항목 */
enum class CheckItem(val kind: CheckKind) {
    CAR_SEAT(CheckKind.REQUIRED),
    HARNESS(CheckKind.REQUIRED),
    WINDOW(CheckKind.AUTO),
    TEMPERATURE(CheckKind.AUTO),
    SUPPLIES(CheckKind.OPTIONAL)
}

enum class CheckKind {
    /** 운전자가 꼭 눌러야 함 */
    REQUIRED,

    /** 차량 데이터로 먼저 확인. 못 읽으면 운전자가 누름 */
    AUTO,

    /** 하면 좋음 */
    OPTIONAL
}

/** 주행 화면 분할: 2분할 = 차량 | mungo, 3분할 = 차량 | 내비 | mungo */
enum class DriveLayout {
    TWO,
    THREE
}

data class BoardingUiState(
    val step: BoardingStep = BoardingStep.COMPANIONS,
    val pets: List<Pet> = emptyList(),
    /** 고른 순서를 지킨다 (첫 번째 = 대표 펫) */
    val selectedIds: List<Long> = emptyList(),
    /** 2/3에서 고른 자리 */
    val seats: Map<Long, PetSeat> = emptyMap(),
    /** 2/3에서 지금 자리를 고르는 펫 */
    val activePetId: Long? = null,
    /** 3/3에서 운전자가 직접 누른 항목 */
    val manualChecks: Set<CheckItem> = emptySet(),
    /** 고른 자리 옆 창문이 모두 닫혔는지. 못 읽으면 null */
    val isWindowClosed: Boolean? = null,
    /** 대표 펫 자리 설정 온도. 못 읽으면 null */
    val seatTemperature: Float? = null,
    /** 마이크로 듣는 중 */
    val isListening: Boolean = false,
    /** 마지막으로 알아들은 말 (화면에 그대로 보여 준다) */
    val heardText: String? = null,
    val isLayoutDialogVisible: Boolean = false,
    val layout: DriveLayout = DriveLayout.THREE,
    val isSaving: Boolean = false,
    /** 주행 화면으로 */
    val isDone: Boolean = false
) {
    val selectedPets: List<Pet>
        get() = selectedIds.mapNotNull { id -> pets.firstOrNull { it.id == id } }

    val activePet: Pet? get() = selectedPets.firstOrNull { it.id == activePetId }

    val temperatureStandard: TemperatureStandard
        get() = PocoStandard.temperature(
            selectedPets.flatMap { PocoStandard.petTypes(it.breed, it.weightKg, it.ageYears) }
                .toSet()
        )

    /** 함께 타는 펫들의 온도 안내 (아무도 안 골랐으면 null) */
    val temperatureNote: TemperatureNote? get() = temperatureNote(selectedPets)

    // region 3/3

    /** 차량 데이터로 이미 확인됐는지 (null = 못 읽음) */
    fun autoResult(item: CheckItem): Boolean? = when (item) {
        CheckItem.WINDOW -> isWindowClosed
        CheckItem.TEMPERATURE -> seatTemperature?.let { t ->
            !temperatureStandard.isHot(t) && !temperatureStandard.isCold(t)
        }
        else -> null
    }

    fun isChecked(item: CheckItem): Boolean = autoResult(item) == true || item in manualChecks

    val checkedCount: Int get() = CheckItem.entries.count { isChecked(it) }

    val requiredLeft: Int
        get() = CheckItem.entries.count { it.kind == CheckKind.REQUIRED && !isChecked(it) }

    // endregion

    val canGoNext: Boolean
        get() = when (step) {
            BoardingStep.COMPANIONS -> selectedIds.isNotEmpty()
            BoardingStep.SEAT -> selectedIds.all { it in seats }
            BoardingStep.CHECKLIST -> requiredLeft == 0 && !isSaving
            BoardingStep.READY -> true
        }
}

data class TemperatureNote(val title: String, val body: String)

/**
 * 함께 타는 펫 기준 온도 안내.
 * 제목은 가장 예민한 펫 한 마리의 이유, 범위는 모두를 합친 가장 엄격한 기준 (mungo 기준점 ①)
 */
fun temperatureNote(pets: List<Pet>): TemperatureNote? {
    if (pets.isEmpty()) return null
    val typesByPet = pets.associateWith {
        PocoStandard.petTypes(it.breed, it.weightKg, it.ageYears)
    }
    val standard = PocoStandard.temperature(typesByPet.values.flatten().toSet())
    val (pet, reason) = typesByPet.entries
        .mapNotNull { (pet, types) -> reasonFor(pet, types)?.let { pet to it } }
        .firstOrNull()
        ?: (pets.first() to null)
    val title = if (reason != null) {
        "${pet.name.eunNeun} $reason"
    } else {
        "${pet.name.eunNeun} 평소 온도면 괜찮아요"
    }
    val comfort = standard.comfort
    val range = "${comfort.start.toCelsius()}~${comfort.endInclusive.toCelsius()}°C"
    return TemperatureNote(title = title, body = "이번 이동은 실내 ${range}를 기준으로 지켜볼게요")
}

/** 온도에 예민한 이유. 더위가 더 위험해서 더위 이유를 먼저 */
private fun reasonFor(pet: Pet, types: Set<PetType>): String? = when {
    PetType.BRACHYCEPHALIC in types -> "단두종이라 더위에 약해요"
    PetType.DOUBLE_COAT in types -> "털이 두꺼워 더위에 약해요"
    PetType.CAT in types -> "고양이라 온도에 예민해요"
    PetType.SENIOR_PUPPY in types ->
        if ((pet.ageYears ?: 0) < 1) "어려서 더위·추위에 약해요" else "나이가 많아 더위·추위에 약해요"
    PetType.SMALL_SHORT_COAT in types -> "몸이 작아 추위에 약해요"
    else -> null
}

/** 22.0 → "22", 21.5 → "21.5" */
fun Float.toCelsius(): String = if (this % 1f ==
    0f
) {
    roundToInt().toString()
} else {
    "%.1f".format(this)
}

/** "현댕이랑 초코 같이 탈거야" → 이름이 들어 있는 펫들. 띄어쓰기는 무시 */
fun matchPetsByVoice(heard: String?, pets: List<Pet>): Set<Long> {
    val text = heard?.replace(" ", "") ?: return emptySet()
    return pets.filter { it.name.isNotBlank() && text.contains(it.name.replace(" ", "")) }
        .map { it.id }
        .toSet()
}

/** "Gleo, 다 확인했어" → true */
fun isAllCheckedReply(heard: String?): Boolean {
    val text = heard?.replace(" ", "") ?: return false
    return ALL_CHECKED_WORDS.any { text.contains(it) }
}

/** "네비랑 같이 보여 줘" → 3분할, "멍고만" → 2분할, 못 알아들으면 null */
fun parseLayoutReply(heard: String?): DriveLayout? {
    val text = heard?.replace(" ", "") ?: return null
    return when {
        THREE_WORDS.any { text.contains(it) } -> DriveLayout.THREE
        TWO_WORDS.any { text.contains(it) } -> DriveLayout.TWO
        else -> null
    }
}

private val ALL_CHECKED_WORDS = listOf("다확인", "확인했", "다했어", "다챙겼", "체크했")
private val THREE_WORDS = listOf("3분할", "삼분할", "세개", "네비", "내비", "지도", "길안내")
private val TWO_WORDS = listOf("2분할", "이분할", "두개", "멍고만", "mungo만", "크게", "혼자")
