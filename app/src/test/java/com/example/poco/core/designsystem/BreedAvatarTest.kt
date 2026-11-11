package com.example.poco.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BreedAvatarTest {
    @Test
    fun `띄어쓰기 대소문자 별칭이 달라도 품종을 찾는다`() {
        assertEquals(BreedAvatar.GOLDEN_RETRIEVER, BreedAvatar.from("골든 리트리버"))
        assertEquals(BreedAvatar.GOLDEN_RETRIEVER, BreedAvatar.from("골든리트리버"))
        assertEquals(BreedAvatar.GOLDEN_RETRIEVER, BreedAvatar.from("Golden"))
        assertEquals(BreedAvatar.MALTESE, BreedAvatar.from("말티즈"))
        assertEquals(BreedAvatar.TOY_POODLE, BreedAvatar.from("토이푸들"))
        assertEquals(BreedAvatar.POMERANIAN, BreedAvatar.from("포메"))
        assertEquals(BreedAvatar.WELSH_CORGI, BreedAvatar.from("웰시 코기"))
        assertEquals(BreedAvatar.SHIBA_INU, BreedAvatar.from("시바이누"))
        assertEquals(BreedAvatar.BICHON, BreedAvatar.from("비숑 프리제"))
    }

    @Test
    fun `모르는 품종이나 빈칸은 기본 마크`() {
        assertNull(BreedAvatar.from("믹스"))
        assertNull(BreedAvatar.from(""))
        assertNull(BreedAvatar.from(null))
    }
}
