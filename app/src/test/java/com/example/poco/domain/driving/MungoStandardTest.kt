package com.example.poco.domain.driving

import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.Restraint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MungoStandardTest {
    @Test
    fun `일반 반려견 - 쾌적 20~25도, 26도부터 주의, 28도부터 위험, 20도 미만 추움`() {
        val types = MungoStandard.petTypes("비글", weightKg = 12f, ageYears = 3)
        val standard = MungoStandard.temperature(types)

        assertEquals(setOf(PetType.BASIC), types)
        assertEquals(TemperatureGroup.GENERAL, standard.group)
        assertEquals(20f..25f, standard.comfort)
        assertFalse(standard.isHot(25f))
        assertTrue(standard.isHot(26f))
        assertFalse(standard.isDanger(27f))
        assertTrue(standard.isDanger(28f))
        assertTrue(standard.isCold(19.5f))
        assertFalse(standard.isCold(20f))
    }

    @Test
    fun `위험군 - 쾌적 20~24도, 25도부터 주의, 27도부터 위험`() {
        val standard = MungoStandard.temperature(MungoStandard.petTypes("시츄", 7f, ageYears = 3))

        assertEquals(TemperatureGroup.RISK, standard.group)
        assertEquals(20f..24f, standard.comfort)
        assertFalse(standard.isHot(24f))
        assertTrue(standard.isHot(25f))
        assertFalse(standard.isDanger(26f))
        assertTrue(standard.isDanger(27f))
    }

    @Test
    fun `이중모 · 소형은 위험군이 아니라 일반 기준`() {
        val standard =
            MungoStandard.temperature(MungoStandard.petTypes("포메라니안", 3f, ageYears = 2))
        assertEquals(TemperatureGroup.GENERAL, standard.group)
    }

    @Test
    fun `10kg 미만이면 소형 단모`() {
        assertEquals(
            setOf(PetType.SMALL_SHORT_COAT),
            MungoStandard.petTypes("몰티즈", weightKg = 4f, ageYears = 3)
        )
    }

    @Test
    fun `노령견 시작 나이 - 소형 10살 · 중대형 8살 · 초대형 6살`() {
        assertFalse(MungoStandard.isSenior("시츄", 7f, ageYears = 9))
        assertTrue(MungoStandard.isSenior("시츄", 7f, ageYears = 10))
        assertFalse(MungoStandard.isSenior("진돗개", 18f, ageYears = 7))
        assertTrue(MungoStandard.isSenior("진돗개", 18f, ageYears = 8))
        assertTrue(MungoStandard.isSenior("골든리트리버", 40f, ageYears = 8))
        assertFalse(MungoStandard.isSenior("그레이트데인", 55f, ageYears = 5))
        assertTrue(MungoStandard.isSenior("그레이트데인", 55f, ageYears = 6))
    }

    @Test
    fun `몸무게를 모르면 견종으로 크기 추정`() {
        assertEquals(10, MungoStandard.seniorAge("몰티즈", null))
        assertEquals(8, MungoStandard.seniorAge("진돗개", null))
    }

    @Test
    fun `노령견 · 1살 미만은 위험군`() {
        assertTrue(PetType.SENIOR_PUPPY in MungoStandard.petTypes("진돗개", 15f, ageYears = 0))
        assertTrue(PetType.SENIOR_PUPPY in MungoStandard.petTypes("진돗개", 15f, ageYears = 8))
        assertFalse(PetType.SENIOR_PUPPY in MungoStandard.petTypes("몰티즈", 4f, ageYears = 8))
    }

    @Test
    fun `흔들림 단계 - 앞뒤 1_5 · 2_5 · 4_0, 좌우 1_5 · 2_0 · 4_0`() {
        assertEquals(ShakeGrade.CALM, MungoStandard.longitudinalGrade(1.4f))
        assertEquals(ShakeGrade.MILD, MungoStandard.longitudinalGrade(-2.0f))
        assertEquals(ShakeGrade.EVENT, MungoStandard.longitudinalGrade(3.0f))
        assertEquals(ShakeGrade.DANGER, MungoStandard.longitudinalGrade(-4.0f))
        assertEquals(ShakeGrade.EVENT, MungoStandard.lateralGrade(2.1f))
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
