package com.example.poco.data.assistant

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import com.example.poco.core.driving.NaviWaypoint
import com.example.poco.core.driving.VerifiedPlace
import com.example.poco.core.driving.distanceMeters
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

interface AssistantWaypointSource {
    /** Returns the waypoint as read back from Navi; throws when it cannot be confirmed. */
    suspend fun add(place: VerifiedPlace): NaviWaypoint

    /** Removes the first waypoint; returns only after Navi no longer reports one near [place]. */
    suspend fun remove(place: VerifiedPlace)
}

/**
 * Adds a Google Maps verified place as the FIRST waypoint, keeping the current destination/route.
 * The Google Place ID is NOT a Navi POI ID and is never passed as one: the request carries only the
 * verified coordinates and name. Coordinate-only acceptance is not documented by Pleos, so success is
 * decided solely by a Navi readback within [MATCH_METERS] of the verified coordinates.
 */
class PleosWaypointSource @Inject constructor(@ApplicationContext private val context: Context) :
    AssistantWaypointSource {
    override suspend fun add(place: VerifiedPlace): NaviWaypoint {
        require(place.latitude.isFinite() && place.latitude in -90.0..90.0)
        require(place.longitude.isFinite() && place.longitude in -180.0..180.0)
        val helper = NaviHelper(context)
        val route = CompletableDeferred<Boolean>()
        val points = Channel<List<WaypointInfo>>(Channel.CONFLATED)
        var failure: String? = null
        val listener = object : NaviHelperEventListener {
            override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
                route.complete(routeStateInfo.isRouting)
            }
            override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) {
                points.trySend(waypointInfoList)
            }
            override fun onError(errorCode: NaviErrorCode) {
                failure = "내비게이션이 경유지 요청을 처리하지 못했어요."
                route.complete(false)
                points.close(IllegalStateException(failure))
            }
        }
        try {
            return withTimeout(12_000) {
                helper.addListener(listener)
                suspendCancellableCoroutine<Unit> { continuation ->
                    helper.initialize(onConnected = {
                        if (continuation.isActive) continuation.resume(Unit)
                    }, onFailed = {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException("내비게이션에 연결하지 못했어요.")
                            )
                        }
                    })
                }
                helper.getRouteStateInfo()
                helper.getWaypointInfo()
                check(route.await()) { failure ?: "먼저 내비게이션에서 목적지를 설정해 주세요." }
                check(points.receive().isEmpty()) { "기존 경유지가 있어요. 내비게이션에서 경유지를 관리해 주세요." }
                helper.addWaypoint(
                    RequestWaypointInfo(
                        place.latitude,
                        place.longitude,
                        place.name,
                        WaypointIndex.FIRST,
                        "",
                        place.address.orEmpty(),
                        "",
                        ""
                    )
                )
                // Request return is not success. Poll actual state; no retries of the control command.
                val poller = launch {
                    while (true) {
                        helper.getWaypointInfo()
                        delay(500)
                    }
                }
                try {
                    var confirmed: NaviWaypoint? = null
                    while (confirmed == null) {
                        confirmed = points.receive().firstOrNull {
                            distanceMeters(place.latitude, place.longitude, it.latitude, it.longitude) <=
                                MATCH_METERS
                        }?.let {
                            NaviWaypoint(it.name, it.latitude, it.longitude, it.remainTime, it.remainDistance)
                        }
                    }
                    confirmed
                } finally {
                    poller.cancel()
                }
            }
        } finally {
            helper.removeListener(listener)
            helper.release()
            points.close()
        }
    }

    override suspend fun remove(place: VerifiedPlace) {
        val helper = NaviHelper(context)
        val points = Channel<List<WaypointInfo>>(Channel.CONFLATED)
        val listener = object : NaviHelperEventListener {
            override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) {
                points.trySend(waypointInfoList)
            }
            override fun onError(errorCode: NaviErrorCode) {
                points.close(IllegalStateException("내비게이션이 경유지 취소를 처리하지 못했어요."))
            }
        }
        try {
            withTimeout(12_000) {
                helper.addListener(listener)
                suspendCancellableCoroutine<Unit> { continuation ->
                    helper.initialize(onConnected = {
                        if (continuation.isActive) continuation.resume(Unit)
                    }, onFailed = {
                        if (continuation.isActive) {
                            continuation.resumeWithException(IllegalStateException("내비게이션에 연결하지 못했어요."))
                        }
                    })
                }
                helper.removeWaypoint(WaypointIndex.FIRST)
                // The request is not the result: success only once Navi stops reporting the place.
                val poller = launch {
                    while (true) {
                        helper.getWaypointInfo()
                        delay(500)
                    }
                }
                try {
                    while (points.receive().any {
                            distanceMeters(place.latitude, place.longitude, it.latitude, it.longitude) <= MATCH_METERS
                        }
                    ) Unit
                } finally {
                    poller.cancel()
                }
            }
        } finally {
            helper.removeListener(listener)
            helper.release()
            points.close()
        }
    }

    companion object {
        const val MATCH_METERS = 300.0
    }
}
