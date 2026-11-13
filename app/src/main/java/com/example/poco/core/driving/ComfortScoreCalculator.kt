package com.example.poco.core.driving

/** One score change for the trip timeline. [reason] is a DrivingEvent name or "RECOVERY". */
data class ScoreChange(val at: Long, val delta: Int, val scoreAfter: Int, val reason: String)

fun DrivingEvent.reason(): String = when (this) {
    DrivingEvent.RapidAcceleration -> "RAPID_ACCELERATION"
    is DrivingEvent.RapidBraking -> "RAPID_BRAKING"
    DrivingEvent.SharpTurn -> "SHARP_TURN"
    DrivingEvent.RepeatedTurns -> "REPEATED_TURNS"
    is DrivingEvent.SpeedBump -> "SPEED_BUMP"
}

/**
 * Stateful per-session engine; call serially. No Android or SDK dependencies.
 * Recovery (+1 per 3 min, capped at 90, starting score 100 kept) only while
 *  - resting: a rest confirmed by [RestPlanner] with P gear confirmed, or
 *  - calm driving after a completed rest ([markRestCompleted]).
 * No recovery during the 3 minutes after any penalty. Parking alone is not a rest.
 */
class ComfortScoreCalculator(
    private val clock: DrivingClock,
    private val config: ComfortScoreConfig = ComfortScoreConfig()
) {
    var state = ComfortScoreState()
        private set
    private var lastTick = clock.now()
    private var recoveryMs = 0L
    private var previousEligible = false
    private val pending = mutableListOf<ScoreChange>()

    /** Score changes since the last call, in order (for the persistent timeline). */
    fun drainChanges(): List<ScoreChange> = pending.toList().also { pending.clear() }

    fun markRestCompleted() {
        state = state.copy(hasRested = true)
    }

    fun penalize(event: DrivingEvent) {
        val amount = when (event) {
            DrivingEvent.RapidAcceleration -> config.accelerationPenalty
            is DrivingEvent.RapidBraking -> event.penalty
            DrivingEvent.SharpTurn -> config.sharpTurnPenalty
            DrivingEvent.RepeatedTurns -> config.repeatedTurnPenalty
            is DrivingEvent.SpeedBump -> event.penalty
        }
        require(amount > 0)
        val before = state.score
        state = state.copy(
            score = (state.score - amount).coerceAtLeast(0),
            lastPenaltyAt = clock.now(),
            accelerationCount =
            state.accelerationCount + if (event == DrivingEvent.RapidAcceleration) 1 else 0,
            brakingCount = state.brakingCount + if (event is DrivingEvent.RapidBraking) 1 else 0,
            turnCount = state.turnCount + if (event == DrivingEvent.SharpTurn) 1 else 0,
            repeatedTurnCount =
            state.repeatedTurnCount + if (event == DrivingEvent.RepeatedTurns) 1 else 0,
            bumpCount = state.bumpCount + if (event is DrivingEvent.SpeedBump) 1 else 0
        )
        pending += ScoreChange(clock.now(), state.score - before, state.score, event.reason())
        recoveryMs = 0
        previousEligible = false
    }

    /** Unknown stability must be passed as false; unknown gear must not count as resting. */
    fun tick(resting: Boolean, stableDriving: Boolean) {
        val now = clock.now()
        val from = lastTick
        lastTick = now
        val eligible = resting || (state.hasRested && stableDriving)
        if (!eligible || !previousEligible || state.score >= config.recoveryCap) {
            recoveryMs = 0
        } else {
            val cooldownEnd = state.lastPenaltyAt?.plus(config.recoveryCooldownMs) ?: Long.MIN_VALUE
            recoveryMs += (now - maxOf(from, cooldownEnd)).coerceAtLeast(0)
            val steps = recoveryMs / config.recoveryStepMs
            if (steps > 0) {
                val before = state.score
                state = state.copy(
                    score = minOf(config.recoveryCap, state.score + steps.coerceAtMost(100).toInt())
                )
                recoveryMs %= config.recoveryStepMs
                if (state.score != before) {
                    pending += ScoreChange(now, state.score - before, state.score, "RECOVERY")
                }
            }
        }
        previousEligible = eligible
    }
}
