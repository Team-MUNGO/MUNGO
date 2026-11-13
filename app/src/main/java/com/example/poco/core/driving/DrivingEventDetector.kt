package com.example.poco.core.driving

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.tan

/** Measurement timestamps, not callback arrival spacing, define the detection windows. */
class DrivingEventDetector(
    private val config: ComfortScoreConfig = ComfortScoreConfig(),
    private val anomaly: (String) -> Unit = {}
) {
    private val speeds = ArrayDeque<SpeedSample>()
    private var lastAcceleration: Long? = null
    private var lastBrake: Long? = null
    private var pendingBrakePenalty = 0
    private var previousHeading: Pair<Long, Double>? = null
    private val headingDeltas = ArrayDeque<Pair<Long, Double>>()
    private var curveAngle = 0.0
    private var curveDirection = 0.0
    private var lastCurveMotion: Long? = null
    private var curveSharp = false
    private val curves = ArrayDeque<Long>()
    private var lastSharpTurn: Long? = null
    private var steeringDeg: Double? = null
    private var steeringOverSince: Long? = null
    private var steeringArmed = true

    fun resetSpeed() {
        speeds.clear()
        lastAcceleration = null
        lastBrake = null
        pendingBrakePenalty = 0
    }

    fun onSpeed(sample: SpeedSample): List<DrivingEvent> {
        if (!sample.kmh.isFinite() || sample.at < 0) return emptyList()
        val previous = speeds.lastOrNull()
        if (previous != null && sample.at <= previous.at) return emptyList()
        if (previous != null && sample.at - previous.at > config.maxSampleGapMs) resetSpeed()
        val events = flush(sample.at).toMutableList()
        // A forward/reverse transition is not acceleration through zero.
        if (previous != null && sample.kmh * previous.kmh < 0) resetSpeed()
        speeds.addLast(sample)
        val target = sample.at - config.speedWindowMs
        while (speeds.size > 2 && speeds[1].at <= target) speeds.removeFirst()
        val a = speeds.first()
        if (a.at > target || speeds.size < 2) return events
        val b = speeds[1]
        val ratio = (target - a.at).toDouble() / (b.at - a.at)
        val baseline = abs(a.kmh) + (abs(b.kmh) - abs(a.kmh)) * ratio
        val rate = (abs(sample.kmh) - baseline) * 1000 / config.speedWindowMs
        // Reject intervals containing implausible adjacent changes as well as 1s averages.
        val adjacentRate = previous?.let {
            (abs(sample.kmh) - abs(it.kmh)) * 1000 / (sample.at - it.at)
        } ?: rate
        if (rate > config.accelerationMax ||
            rate < -config.brakingMax ||
            adjacentRate > config.accelerationMax ||
            adjacentRate < -config.brakingMax
        ) {
            anomaly("POCO sensor-policy exclusion: $rate km/h/s (adjacent $adjacentRate)")
            resetSpeed()
            speeds.addLast(sample)
            return events
        }
        if (rate >= config.accelerationMin) {
            if (lastAcceleration == null ||
                sample.at - lastAcceleration!! > config.accelerationMergeMs
            ) {
                events += DrivingEvent.RapidAcceleration
            }
            lastAcceleration = sample.at
        }
        if (-rate >= config.brakingMin) {
            pendingBrakePenalty = maxOf(
                pendingBrakePenalty,
                if (-rate >=
                    config.brakingStrong
                ) {
                    config.brakingStrongPenalty
                } else {
                    config.brakingPenalty
                }
            )
            lastBrake = sample.at
        }
        return events
    }

    /** Braking is settled after its merge window so a stronger sample is one -6 event. */
    fun flush(now: Long): List<DrivingEvent> {
        val events = mutableListOf<DrivingEvent>()
        if (lastBrake != null &&
            now - lastBrake!! > config.brakingMergeMs &&
            pendingBrakePenalty > 0
        ) {
            events += DrivingEvent.RapidBraking(pendingBrakePenalty)
            pendingBrakePenalty = 0
            lastBrake = null
        }
        if (lastCurveMotion != null && now - lastCurveMotion!! >= config.curveEndQuietMs) {
            finishCurve(now)?.let(events::add)
        }
        // Steering may be reported only on change, so a held angle is re-evaluated against fresh speed.
        evaluateSteering(now)?.let(events::add)
        return events
    }

    /**
     * Steering-wheel angle (degrees, either sign) from the vehicle API. Sharp turn when the estimated
     * lateral acceleration stays at or above the POCO threshold for [ComfortScoreConfig.steeringHoldMs]
     * at [ComfortScoreConfig.turnMinSpeed] or faster; re-armed once it falls below the mild level.
     */
    fun onSteering(at: Long, degrees: Double): List<DrivingEvent> {
        if (!degrees.isFinite() || abs(degrees) > 1_080) return emptyList()
        steeringDeg = degrees
        return listOfNotNull(evaluateSteering(at))
    }

    /** Estimated lateral acceleration (m/s²) for [degrees] of steering at [speedKmh]. */
    fun lateralMps2(degrees: Double, speedKmh: Double): Double {
        val v = abs(speedKmh) / 3.6
        val wheel = Math.toRadians(
            abs(degrees) / config.steeringRatio
        ).coerceAtMost(Math.toRadians(45.0))
        return v * v * tan(wheel) / config.wheelbaseM
    }

    private fun evaluateSteering(now: Long): DrivingEvent? {
        val degrees = steeringDeg ?: return null
        val speed = speeds.lastOrNull()
        if (speed == null || abs(speed.kmh) < config.turnMinSpeed) {
            steeringOverSince = null
            return null
        }
        val lateral = lateralMps2(degrees, speed.kmh)
        if (lateral < config.turnRearmLateralMps2) steeringArmed = true
        if (lateral < config.sharpTurnLateralMps2 || !steeringArmed) {
            steeringOverSince = null
            return null
        }
        val since = steeringOverSince ?: now.also { steeringOverSince = it }
        if (now - since < config.steeringHoldMs) return null
        steeringArmed = false
        steeringOverSince = null
        return sharpTurn(now)
    }

    /** Shared by steering and heading detection so one physical turn is never counted twice. */
    private fun sharpTurn(now: Long): DrivingEvent? {
        val last = lastSharpTurn
        if (last != null && now - last < config.sharpTurnDedupMs) return null
        lastSharpTurn = now
        return DrivingEvent.SharpTurn
    }

    /** Only feed validated travel bearings in degrees; production Navi heading is unverified. */
    fun onHeading(at: Long, degrees: Double, speedKmh: Double): List<DrivingEvent> {
        if (!degrees.isFinite() ||
            degrees !in 0.0..360.0 ||
            !speedKmh.isFinite()
        ) {
            return emptyList()
        }
        val previous = previousHeading
        if (previous != null && at <= previous.first) return emptyList()
        previousHeading = at to degrees
        if (previous == null) return emptyList()
        val dt = at - previous.first
        if (dt > config.maxSampleGapMs || abs(speedKmh) < config.turnMinSpeed) {
            clearCurve()
            return emptyList()
        }
        val events = mutableListOf<DrivingEvent>()
        val delta = ((degrees - previous.second + 540) % 360) - 180
        if (abs(delta) * 1000 / dt < config.curveMinRate) return flush(at)
        if (curveDirection != 0.0 && delta.sign != curveDirection) {
            finishCurve(at)?.let(events::add)
        }
        curveDirection = delta.sign
        curveAngle += abs(delta)
        lastCurveMotion = at
        headingDeltas.addLast(at to delta)
        while (headingDeltas.isNotEmpty() &&
            at - headingDeltas.first().first >= config.turnWindowMs
        ) {
            headingDeltas.removeFirst()
        }
        val angle = abs(headingDeltas.sumOf { it.second })
        if (!curveSharp && angle in config.sharpTurnMin..config.sharpTurnMax) {
            curveSharp = true
            sharpTurn(at)?.let(events::add)
        }
        return events
    }

    private fun finishCurve(at: Long): DrivingEvent? {
        val eligible =
            !curveSharp && curveAngle >= config.curveMinAngle && curveAngle < config.sharpTurnMin
        clearCurve()
        if (!eligible) return null
        while (curves.isNotEmpty() &&
            at - curves.first() > config.repeatedTurnWindowMs
        ) {
            curves.removeFirst()
        }
        curves.addLast(at)
        if (curves.size < config.repeatedTurnCount) return null
        repeat(config.repeatedTurnCount) { curves.removeFirst() }
        return DrivingEvent.RepeatedTurns
    }

    private fun clearCurve() {
        headingDeltas.clear()
        curveAngle = 0.0
        curveDirection = 0.0
        lastCurveMotion = null
        curveSharp = false
    }

    fun speedBump(passageConfirmed: Boolean, speedKmh: Double?): DrivingEvent.SpeedBump? {
        if (!passageConfirmed || speedKmh == null || !speedKmh.isFinite()) return null
        return when {
            abs(speedKmh) > config.bumpHeavySpeed -> DrivingEvent.SpeedBump(config.bumpHeavyPenalty)
            abs(speedKmh) > config.bumpLightSpeed -> DrivingEvent.SpeedBump(config.bumpLightPenalty)
            else -> null
        }
    }
}
