package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.TBTInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 에뮬레이터 내비가 실제로 준 회전 안내 문구로 확인 */
class NaviLandmarkTest {
    @Test fun icTollAndJunctionNamesAreKept() {
        assertEquals("판교IC", landmarkName(TurnType.TURN_ENTER_IC_R, "판교IC - 부산 방면"))
        assertEquals("서울톨게이트", landmarkName(TurnType.TURN_TOLL, "서울톨게이트"))
        assertEquals("신갈JC", landmarkName(TurnType.TURN_KEEP_R, "신갈JC"))
    }

    @Test fun directionsAreNotPlaces() {
        assertNull(landmarkName(TurnType.TURN_KEEP_R, "원주,인천 방면"))
        assertNull(landmarkName(TurnType.TURN_RIGHT, "판교IC 방면"))
        assertNull(landmarkName(TurnType.TURN_LEFT, "기업지원허브 방면"))
    }

    @Test fun onlyTheNearestLandmarkWithin300m() {
        val turns = listOf(
            TBTInfo(TurnType.TURN_LEFT, 80.0, "기업지원허브 방면"),
            TBTInfo(TurnType.TURN_ENTER_IC_R, 250.0, "판교IC - 부산 방면"),
            TBTInfo(TurnType.TURN_TOLL, 280.0, "서울톨게이트")
        )
        assertEquals("판교IC", nearbyLandmark(turns))
        assertNull(nearbyLandmark(listOf(TBTInfo(TurnType.TURN_ENTER_IC_R, 1700.0, "판교IC"))))
    }
}
