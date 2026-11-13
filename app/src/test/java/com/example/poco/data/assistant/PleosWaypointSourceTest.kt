package com.example.poco.data.assistant

import ai.pleos.playground.navi.constants.NaviMode
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.RouteStateInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PleosWaypointSourceTest {
    private fun state(mode: NaviMode, routing: Boolean) =
        RouteStateInfo(true, mode, routing, RouteOption.RECOMMENDED, 0, emptyList(), null)

    /** 내비는 경로 안내 중에도 isRouting=false를 보낸다: 안내 모드로 판단 */
    @Test fun routeGuidingCountsAsActiveRouteEvenWhenIsRoutingIsFalse() {
        assertTrue(state(NaviMode.ROUTE_GUIDING, routing = false).hasActiveRoute())
        assertTrue(state(NaviMode.SIMULATION_GUIDING, routing = false).hasActiveRoute())
        assertTrue(state(NaviMode.SAFE_GUIDING, routing = true).hasActiveRoute())
        assertFalse(state(NaviMode.SAFE_GUIDING, routing = false).hasActiveRoute())
        assertFalse(state(NaviMode.NOT_WORKING, routing = false).hasActiveRoute())
    }
}
