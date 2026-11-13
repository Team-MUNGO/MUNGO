package com.example.poco.presentation.home

import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetTrait
import com.example.poco.domain.driving.MungoStandard
import com.example.poco.domain.driving.PetType

/** 홈 프로필 칩: 보여 줄 칩 + 나머지 개수("+N") */
data class PetChips(val shown: List<String>, val more: Int) {
    val all: Int get() = shown.size + more
}

/**
 * 칩 기준
 * - 자동 · 나이 (항상 1개): 1살 미만 아기 댕 · 1~3살 청춘 댕 · 4살~시니어 전 어른 댕 · 시니어 시니어 댕
 * - 자동 · 품종 (해당될 때만): 단두종 납작코 · 털이 두꺼운 견종 털복숭이
 * - 선택 (온보딩에서 고른 값): [SELECTED_ORDER] 순서가 중요도
 * 홈에는 나이 칩 1개 + 선택 칩 중 가장 중요한 1개, 나머지는 "+N"
 * (고른 칩이 없으면 품종 칩으로 두 번째 자리를 채운다)
 */
fun petChips(pet: Pet): PetChips {
    val age = ageChip(pet)
    val breed = breedChips(pet)
    val selected = SELECTED_ORDER.filter { (trait, _) -> trait in pet.traits }.map { it.second }
    val second = selected.firstOrNull() ?: breed.firstOrNull()
    val shown = listOfNotNull(age, second)
    val total = listOfNotNull(age).size + breed.size + selected.size
    return PetChips(shown, total - shown.size)
}

fun ageChip(pet: Pet): String? {
    val age = pet.ageYears ?: return null
    return when {
        age < 1 -> "아기 댕"
        MungoStandard.isSenior(pet.breed, pet.weightKg, age) -> "시니어 댕"
        age <= 3 -> "청춘 댕"
        else -> "어른 댕"
    }
}

fun breedChips(pet: Pet): List<String> {
    val types = MungoStandard.petTypes(pet.breed, pet.weightKg, pet.ageYears)
    return buildList {
        if (PetType.BRACHYCEPHALIC in types) add("납작코")
        if (PetType.DOUBLE_COAT in types) add("털복숭이")
    }
}

/** 선택 칩 우선순위: 더위 타는 댕 → 멀미쟁이 → 소심댕 → 추위 타는 댕 → 침 뚝뚝 → 드라이브 러버 */
private val SELECTED_ORDER = listOf(
    PetTrait.HEAT_SENSITIVE to "더위 타는 댕",
    PetTrait.MOTION_SICK to "멀미쟁이",
    PetTrait.ANXIOUS to "소심댕",
    PetTrait.COLD_SENSITIVE to "추위 타는 댕",
    PetTrait.DROOLS to "침 뚝뚝",
    PetTrait.LOVES_RIDES to "드라이브 러버"
)
