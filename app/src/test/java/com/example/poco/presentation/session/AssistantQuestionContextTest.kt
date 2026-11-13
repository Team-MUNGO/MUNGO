package com.example.poco.presentation.session

import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.PetSeat
import com.example.poco.data.model.DrivingSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssistantQuestionContextTest {
    private val state = DrivingSessionState(
        active = true,
        settings = DrivingSettings(" 현댕이 ", PetSeat.REAR_LEFT, 20f, 25f, 70)
    )

    @Test fun `pet name is sent trimmed`() {
        assertEquals("현댕이", AssistantViewModel.questionContext(state).petName)
    }

    @Test fun `route remaining time counts down from when navi reported it`() {
        val route = RouteSnapshot("서울역", remainSec = 40 * 60, receivedAt = 1_000_000L)
        val ctx = AssistantViewModel.questionContext(state, route, now = 1_000_000L + 3 * 60_000L)
        assertEquals("서울역", ctx.routeDestination)
        assertEquals(37L, ctx.routeRemainingMinutes)
    }

    @Test fun `stale or missing route is not sent`() {
        assertNull(AssistantViewModel.questionContext(state).routeRemainingMinutes)
        val old = RouteSnapshot("서울역", 40 * 60, receivedAt = 0L)
        val ctx = AssistantViewModel.questionContext(state, old, now = 6 * 60_000L)
        assertNull(ctx.routeDestination)
        assertNull(ctx.routeRemainingMinutes)
    }
}
