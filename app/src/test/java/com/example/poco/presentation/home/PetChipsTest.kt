package com.example.poco.presentation.home

import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.PetTrait
import org.junit.Assert.assertEquals
import org.junit.Test

class PetChipsTest {
    private fun pet(breed: String, age: Int?, kg: Float?, traits: Set<PetTrait> = emptySet()) = Pet(
        name = "현댕이",
        breed = breed,
        ageYears = age,
        weightKg = kg,
        seat = PetSeat.REAR_LEFT,
        motionSensitivity = MotionSensitivity.NORMAL,
        traits = traits
    )

    @Test
    fun `가이드 예시 - 시츄 10살 7kg 멀미 + 더위`() {
        val chips = petChips(
            pet("시츄", 10, 7f, setOf(PetTrait.MOTION_SICK, PetTrait.HEAT_SENSITIVE))
        )
        assertEquals(listOf("시니어 댕", "더위 타는 댕"), chips.shown)
        assertEquals(2, chips.more)
    }

    @Test
    fun `나이 칩 - 아기 · 청춘 · 어른 · 시니어 (크기별 시니어 나이)`() {
        assertEquals("아기 댕", ageChip(pet("비글", 0, 8f)))
        assertEquals("청춘 댕", ageChip(pet("비글", 3, 12f)))
        assertEquals("어른 댕", ageChip(pet("비글", 4, 12f)))
        assertEquals("어른 댕", ageChip(pet("몰티즈", 9, 4f)))
        assertEquals("시니어 댕", ageChip(pet("몰티즈", 10, 4f)))
        assertEquals("시니어 댕", ageChip(pet("진돗개", 8, 18f)))
        assertEquals("시니어 댕", ageChip(pet("그레이트데인", 6, 55f)))
    }

    @Test
    fun `품종 칩 - 납작코 · 털복숭이`() {
        assertEquals(listOf("납작코"), breedChips(pet("퍼그", 3, 8f)))
        assertEquals(listOf("털복숭이"), breedChips(pet("포메라니안", 3, 3f)))
        assertEquals(emptyList<String>(), breedChips(pet("비글", 3, 12f)))
    }

    @Test
    fun `선택 칩 우선순위 - 멀미쟁이가 소심댕보다 먼저`() {
        val chips = petChips(pet("비글", 3, 12f, setOf(PetTrait.ANXIOUS, PetTrait.MOTION_SICK)))
        assertEquals(listOf("청춘 댕", "멀미쟁이"), chips.shown)
        assertEquals(1, chips.more)
    }
}
