package com.example.poco.presentation.boarding

import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardingUiStateTest {
    private fun pet(id: Long, name: String, breed: String = "", age: Int? = 3, kg: Float? = 12f) =
        Pet(
            id = id,
            name = name,
            breed = breed,
            ageYears = age,
            weightKg = kg,
            seat = PetSeat.REAR_LEFT,
            motionSensitivity = MotionSensitivity.NORMAL
        )

    private val pets = listOf(pet(1, "현댕이"), pet(2, "초코"), pet(3, "보리 콩"))

    @Test
    fun `말에 들어 있는 이름의 펫을 고른다`() {
        assertEquals(setOf(1L), matchPetsByVoice("현댕이랑 같이 탈거야", pets))
        assertEquals(setOf(1L, 2L), matchPetsByVoice("현댕이랑 초코 같이 가", pets))
    }

    @Test
    fun `띄어쓰기는 무시한다`() {
        assertEquals(setOf(3L), matchPetsByVoice("보리콩 탈 거야", pets))
    }

    @Test
    fun `못 알아들으면 빈 결과`() {
        assertEquals(emptySet<Long>(), matchPetsByVoice(null, pets))
        assertEquals(emptySet<Long>(), matchPetsByVoice("오늘 날씨 좋다", pets))
    }

    @Test
    fun `단두종은 더위 이유와 합친 온도 범위를 보여 준다`() {
        // 시츄(단두종 19~23) + 소형(21~25) → 21~23
        val note = temperatureNote(listOf(pet(1, "현댕이", breed = "시츄", age = 5, kg = 7f)))!!
        assertEquals("현댕이는 단두종이라 더위에 약해요", note.title)
        assertEquals("이번 이동은 실내 21~23°C를 기준으로 지켜볼게요", note.body)
    }

    @Test
    fun `아무도 안 고르면 안내 없음`() {
        assertNull(temperatureNote(emptyList()))
    }

    @Test
    fun `다 확인했다는 말을 알아듣는다`() {
        assertEquals(true, isAllCheckedReply("Gleo, 다 확인했어"))
        assertEquals(false, isAllCheckedReply("아직이야"))
        assertEquals(false, isAllCheckedReply(null))
    }

    @Test
    fun `분할 대답`() {
        assertEquals(DriveLayout.THREE, parseLayoutReply("글레오, 네비랑 같이 보여 줘"))
        assertEquals(DriveLayout.TWO, parseLayoutReply("멍고만 크게"))
        assertNull(parseLayoutReply("음"))
    }

    @Test
    fun `필수 항목이 남으면 다음으로 못 간다`() {
        val base = BoardingUiState(
            step = BoardingStep.CHECKLIST,
            pets = listOf(pet(1, "현댕이")),
            selectedIds = listOf(1)
        )
        assertEquals(2, base.requiredLeft)
        assertEquals(false, base.canGoNext)
        val done = base.copy(manualChecks = setOf(CheckItem.CAR_SEAT, CheckItem.HARNESS))
        assertEquals(0, done.requiredLeft)
        assertEquals(true, done.canGoNext)
        // 창문 · 온도는 차량에서 확인되면 저절로 체크
        assertEquals(4, done.copy(isWindowClosed = true, seatTemperature = 21f).checkedCount)
    }
}
