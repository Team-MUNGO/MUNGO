package com.example.poco.core.driving

/**
 * Selection bounds for the user rest interval. Default 120 min is the product rule.
 * MIN/MAX/STEP follow Figma Page 3 "휴식 간격" (node 465:48834): 0:30–3:00 scale with
 * "30분 늘리기/줄이기" buttons.
 */
object RestIntervalPolicy {
    const val DEFAULT_MINUTES = 120
    const val MIN_MINUTES = 30
    const val MAX_MINUTES = 180
    const val STEP_MINUTES = 30
    const val RECOMMEND_BEFORE_MS = 20 * 60_000L

    /** Recommended stop length shown on the resting screen (Figma 464:46569 "추천 15분"). */
    const val RECOMMENDED_REST_MINUTES = 15

    fun clamp(minutes: Int): Int {
        val bounded = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
        return MIN_MINUTES + (bounded - MIN_MINUTES) / STEP_MINUTES * STEP_MINUTES
    }
}

enum class RestPhase { DRIVING, RESTING, READY_TO_DEPART }

enum class RestStartSource { USER, WAYPOINT_ARRIVAL }

data class RestSnapshot(
    val intervalMinutes: Int,
    val baseAt: Long,
    val cycle: Int,
    val phase: RestPhase,
    val restStartedAt: Long?,
    val restCount: Int,
    val totalRestMs: Long,
    val lastDepartureAt: Long?
) {
    val nextRestAt: Long get() = baseAt + intervalMinutes * 60_000L
    fun remainingMs(now: Long) = nextRestAt - now
    fun restDue(now: Long) = phase == RestPhase.DRIVING && now >= nextRestAt
}

sealed interface RestEvent {
    data class IntervalChanged(val from: Int, val to: Int, val at: Long) : RestEvent
    data class Started(val at: Long, val source: RestStartSource) : RestEvent
    data class ReadyToDepart(val at: Long) : RestEvent
    /** A rest only completes on an actual departure after at least [ComfortScoreConfig.validRestMs]. */
    data class Completed(val startedAt: Long, val departedAt: Long, val durationMs: Long) : RestEvent
    /** Too-short stop (signal, brief P): no rest is counted and the rest timer is not reset. */
    data class Cancelled(val startedAt: Long, val departedAt: Long) : RestEvent
    data class RecommendationDue(val cycle: Int, val remainingMs: Long) : RestEvent
}

/**
 * nextRestAt = restBase + configured interval. restBase is the actual departure time, then the actual
 * re-departure after a completed rest. Parking alone never starts or completes a rest.
 * Times use the session's monotonic clock; recorders convert to epoch.
 */
class RestPlanner(
    start: Long,
    intervalMinutes: Int,
    private val validRestMs: Long = 180_000
) {
    var snapshot = RestSnapshot(
        RestIntervalPolicy.clamp(intervalMinutes), start, 1, RestPhase.DRIVING,
        null, 0, 0, null
    )
        private set
    private var requestedCycle = 0

    /** Base time is kept; the new interval applies immediately (possibly making the rest overdue). */
    fun changeInterval(minutes: Int, now: Long): RestEvent.IntervalChanged? {
        val next = RestIntervalPolicy.clamp(minutes)
        if (next == snapshot.intervalMinutes) return null
        val event = RestEvent.IntervalChanged(snapshot.intervalMinutes, next, now)
        snapshot = snapshot.copy(intervalMinutes = next)
        return event
    }

    /** Requires a confirmed P gear plus an explicit trigger (user or verified waypoint arrival). */
    fun startRest(now: Long, parked: Boolean, source: RestStartSource): RestEvent.Started? {
        if (!parked || snapshot.phase != RestPhase.DRIVING) return null
        snapshot = snapshot.copy(phase = RestPhase.RESTING, restStartedAt = now)
        return RestEvent.Started(now, source)
    }

    fun readyToDepart(now: Long): RestEvent.ReadyToDepart? {
        if (snapshot.phase != RestPhase.RESTING) return null
        snapshot = snapshot.copy(phase = RestPhase.READY_TO_DEPART)
        return RestEvent.ReadyToDepart(now)
    }

    /** Called on confirmed movement (drive gear with moving speed), never on P->D alone at 0 km/h. */
    fun onDeparture(now: Long): RestEvent? {
        val started = snapshot.restStartedAt ?: return null
        if (snapshot.phase == RestPhase.DRIVING) return null
        val duration = (now - started).coerceAtLeast(0)
        return if (duration >= validRestMs) {
            snapshot = snapshot.copy(
                baseAt = now,
                cycle = snapshot.cycle + 1,
                phase = RestPhase.DRIVING,
                restStartedAt = null,
                restCount = snapshot.restCount + 1,
                totalRestMs = snapshot.totalRestMs + duration,
                lastDepartureAt = now
            )
            RestEvent.Completed(started, now, duration)
        } else {
            snapshot = snapshot.copy(phase = RestPhase.DRIVING, restStartedAt = null)
            RestEvent.Cancelled(started, now)
        }
    }

    /**
     * Fires at most once per rest cycle, at the first tick with <= 20 min left. An interval change that
     * already puts the cycle inside the window fires on the next tick, so the guidance is not lost.
     */
    fun checkRecommendation(now: Long): RestEvent.RecommendationDue? {
        val s = snapshot
        if (s.phase != RestPhase.DRIVING || requestedCycle == s.cycle) return null
        val remaining = s.remainingMs(now)
        if (remaining > RestIntervalPolicy.RECOMMEND_BEFORE_MS) return null
        requestedCycle = s.cycle
        return RestEvent.RecommendationDue(s.cycle, remaining)
    }
}
