package com.example.poco.core.driving

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class EtaUnit { SECONDS, MINUTES }

/**
 * NaviHelper's WaypointInfo.remainTime unit is not documented. It is inferred only when the implied
 * average speed is plausible (2..45 m/s) under exactly one hypothesis; the hypotheses differ by 60x so
 * they cannot both pass. Short distances stay unresolved and no time-based guidance is produced.
 */
fun inferEtaUnit(remainTime: Int, remainMeters: Int): EtaUnit? {
    if (remainTime <= 0 || remainMeters < 2_000) return null
    val asSeconds = remainMeters / remainTime.toDouble()
    val asMinutes = remainMeters / (remainTime * 60.0)
    val seconds = asSeconds in 2.0..45.0
    val minutes = asMinutes in 2.0..45.0
    return when {
        seconds && !minutes -> EtaUnit.SECONDS
        minutes && !seconds -> EtaUnit.MINUTES
        else -> null
    }
}

fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = Math.toRadians(1.0)
    val a = sin((lat2 - lat1) * r / 2).let { it * it } +
        cos(lat1 * r) * cos(lat2 * r) * sin((lon2 - lon1) * r / 2).let { it * it }
    return 6_371_000 * 2 * atan2(sqrt(a), sqrt(1 - a))
}

/** A Google Maps verified place: only these coordinates may ever be sent to NaviHelper. */
data class VerifiedPlace(
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?
)

data class NaviWaypoint(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val remainTime: Int,
    val remainMeters: Int
)

sealed interface WaypointSignal {
    data class FiveMinutes(val placeId: String, val name: String) : WaypointSignal
    data class ArrivedAndParked(val placeId: String, val name: String) : WaypointSignal
    data class Cancelled(val placeId: String) : WaypointSignal
}

/**
 * Tracks one confirmed waypoint. ETA comes only from Navi readbacks; 5-minute guidance fires once per
 * waypoint; arrival needs Navi's arrival callback AND confirmed P gear (in either order).
 */
class WaypointTracker(val place: VerifiedPlace, private val matchMeters: Double = 300.0) {
    private var unit: EtaUnit? = null
    var etaSeconds: Int? = null
        private set
    private var fiveMinuteFired = false
    var arrived = false
        private set
    private var arrivalFired = false
    var finished = false
        private set

    fun matches(w: NaviWaypoint) =
        distanceMeters(place.latitude, place.longitude, w.latitude, w.longitude) <= matchMeters

    fun onWaypoints(list: List<NaviWaypoint>): WaypointSignal? {
        if (finished || arrived) return null
        val mine = list.firstOrNull(::matches)
        if (mine == null) {
            finished = true
            return WaypointSignal.Cancelled(place.placeId)
        }
        unit = unit ?: inferEtaUnit(mine.remainTime, mine.remainMeters)
        etaSeconds = when (unit) {
            EtaUnit.SECONDS -> mine.remainTime
            EtaUnit.MINUTES -> mine.remainTime * 60
            null -> null
        }?.takeIf { it >= 0 }
        val eta = etaSeconds
        if (!fiveMinuteFired && eta != null && eta <= 300) {
            fiveMinuteFired = true
            return WaypointSignal.FiveMinutes(place.placeId, place.name)
        }
        return null
    }

    /** NaviHelper onWayPointArrived for the first waypoint (the only one POCO ever adds). */
    fun onArrived(parked: Boolean): WaypointSignal? {
        if (finished) return null
        arrived = true
        return onGear(parked)
    }

    fun onGear(parked: Boolean): WaypointSignal? {
        if (!arrived || !parked || arrivalFired) return null
        arrivalFired = true
        finished = true
        return WaypointSignal.ArrivedAndParked(place.placeId, place.name)
    }

    fun etaMinutesText(): String? = etaSeconds?.let {
        (it / 60.0).roundToInt().coerceAtLeast(1)
    }?.let { "${it}분" }
}

/**
 * Speed-bump passage is confirmed only when a caution point that was reported within [nearMeters]
 * disappears from Navi's list. Each ID is scored at most once; approaches alone never score.
 */
class BumpPassageTracker(private val nearMeters: Int = 30) {
    private val near = mutableSetOf<String>()
    private val passed = mutableSetOf<String>()

    fun update(points: List<Pair<String, Int>>): List<String> {
        val ids = points.map { it.first }.toSet()
        val done = near.filter { it !in ids && passed.add(it) }
        near.removeAll(done.toSet())
        points.filter { it.second in 0..nearMeters && it.first !in passed }.forEach {
            near +=
                it.first
        }
        return done
    }
}
