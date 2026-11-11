package com.example.poco.domain.driving

import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.Restraint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PocoStandardTest {
    @Test
    fun `중대형 보통 털은 기본 유형 - 26도 이상 더움, 17도 이하 추움`() {
        val types = PocoStandard.petTypes("비글", weightKg = 12f, ageYears = 3)
        val standard = PocoStandard.temperature(types)

        assertEquals(setOf(PetType.BASIC), types)
        assertEquals(26f, standard.hotAt)
        assertEquals(17f, standard.coldAt)
    }

    @Test
    fun `10kg 미만이면 소형 단모`() {
        assertEquals(
            setOf(PetType.SMALL_SHORT_COAT),
            PocoStandard.petTypes("몰티즈", weightKg = 4f, ageYears = 3)
        )
    }

    @Test
    fun `단두종 + 노견은 더 엄격한 값 - 더움 24도, 추움 19도`() {
        val types = PocoStandard.petTypes("퍼그", weightKg = 12f, ageYears = 9)
        val standard = PocoStandard.temperature(types)

        assertEquals(setOf(PetType.BRACHYCEPHALIC, PetType.SENIOR_PUPPY), types)
        assertEquals(24f, standard.hotAt)
        assertEquals(19f, standard.coldAt)
        assertTrue(standard.comfort.start <= standard.comfort.endInclusive)
    }

    @Test
    fun `포메라니안은 이중모 + 소형 - 더위는 이중모, 추위는 소형 기준`() {
        val standard =
            PocoStandard.temperature(PocoStandard.petTypes("포메라니안", 3f, ageYears = 2))

        assertEquals(24f, standard.hotAt)
        assertEquals(18f, standard.coldAt)
    }

    @Test
    fun `1살 미만은 퍼피`() {
        assertTrue(PetType.SENIOR_PUPPY in PocoStandard.petTypes("진돗개", 15f, ageYears = 0))
    }

    @Test
    fun `29_5도 이상은 모든 유형 위험`() {
        val standard = PocoStandard.temperature(setOf(PetType.DOUBLE_COAT))
        assertTrue(standard.isDanger(29.5f))
    }

    @Test
    fun `흔들림 단계 - 앞뒤 1_5 · 2_5 · 4_0, 좌우 1_5 · 2_0 · 4_0`() {
        assertEquals(ShakeGrade.CALM, PocoStandard.longitudinalGrade(1.4f))
        assertEquals(ShakeGrade.MILD, PocoStandard.longitudinalGrade(-2.0f))
        assertEquals(ShakeGrade.EVENT, PocoStandard.longitudinalGrade(3.0f))
        assertEquals(ShakeGrade.DANGER, PocoStandard.longitudinalGrade(-4.0f))
        assertEquals(ShakeGrade.EVENT, PocoStandard.lateralGrade(2.1f))
    }

    @Test
    fun `보정 배율 - 높음 0_8 × 소형 0_9 × 하네스 1_2`() {
        val pet = Pet(
            name = "현댕이",
            breed = "몰티즈",
            ageYears = 3,
            weightKg = 4f,
            seat = PetSeat.REAR_LEFT,
            motionSensitivity = MotionSensitivity.HIGH,
            restraint = Restraint.CRATE_HARNESS
        )
        assertEquals(0.864f, pet.shakeThresholdFactor, 0.001f)
    }
}
