package com.example.poco.core.driving

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Steering-wheel sharp turn: bicycle-model lateral ≥ 2.0 m/s² held 0.3s at ≥ 15 km/h. */
class SteeringTurnTest {
    private fun DrivingEventDetector.drive(
        kmh: Double,
        from: Long,
        to: Long,
        step: Long = 100
    ): List<DrivingEvent> {
        val out = mutableListOf<DrivingEvent>()
        var t = from
        while (t <= to) {
            out += onSpeed(SpeedSample(kmh, t))
            t += step
        }
        return out
    }

    private fun turns(events: List<DrivingEvent>) = events.count { it == DrivingEvent.SharpTurn }

    @Test fun lateralEstimateMatchesBicycleModel() {
        val d = DrivingEventDetector()
        // 40 km/h, 120° wheel → 8.57° road wheel: 11.11² × tan(8.57°) / 3.0 ≈ 6.2 m/s²
        assertEquals(6.2, d.lateralMps2(120.0, 40.0), 0.1)
        assertEquals(d.lateralMps2(-90.0, 30.0), d.lateralMps2(90.0, 30.0), 1e-9)
    }

    @Test fun heldSharpSteeringAtSpeedIsOneTurn() {
        val d = DrivingEventDetector()
        val events = d.drive(40.0, 0, 1_000).toMutableList()
        events += d.onSteering(1_000, 120.0)
        events += d.drive(40.0, 1_100, 3_000)
        assertEquals(1, turns(events))
    }

    @Test fun briefSpikeShorterThanHoldIsIgnored() {
        val d = DrivingEventDetector()
        val events = d.drive(40.0, 0, 1_000).toMutableList()
        events += d.onSteering(1_000, 120.0)
        events += d.drive(40.0, 1_100, 1_200)
        events += d.onSteering(1_250, 0.0)
        events += d.drive(40.0, 1_300, 3_000)
        assertEquals(0, turns(events))
    }

    @Test fun gentleSteeringOrLowSpeedIsNotSharp() {
        val d = DrivingEventDetector()
        // 40 km/h with 20° steering ≈ 1.0 m/s²: lane change, not a sharp turn.
        var events =
            d.drive(40.0, 0, 1_000) + d.onSteering(1_000, 20.0) + d.drive(40.0, 1_100, 3_000)
        assertEquals(0, turns(events))
        // Parking-lot full lock at 10 km/h is below the 15 km/h turn speed.
        val slow = DrivingEventDetector()
        events =
            slow.drive(10.0, 0, 1_000) + slow.onSteering(1_000, 400.0) +
            slow.drive(10.0, 1_100, 3_000)
        assertEquals(0, turns(events))
    }

    @Test fun rearmsOnlyAfterSteeringStraightens() {
        val d = DrivingEventDetector()
        val events = d.drive(40.0, 0, 500).toMutableList()
        events += d.onSteering(500, 120.0)
        events += d.drive(40.0, 600, 6_000) // held through one long corner: still 1
        assertEquals(1, turns(events))
        events += d.onSteering(6_000, 0.0)
        events += d.drive(40.0, 6_100, 7_000)
        events += d.onSteering(7_000, -120.0)
        events += d.drive(40.0, 7_100, 8_000)
        assertEquals(2, turns(events))
    }

    @Test fun steeringAndHeadingForTheSameTurnCountOnce() {
        val d = DrivingEventDetector()
        val events = d.drive(40.0, 0, 1_000).toMutableList()
        events += d.onSteering(1_000, 120.0)
        events += d.drive(40.0, 1_100, 1_400)
        // GPS heading then reports the same 90° turn within the dedup window.
        events += d.onHeading(1_500, 0.0, 40.0)
        events += d.onHeading(2_000, 45.0, 40.0)
        events += d.onHeading(2_500, 90.0, 40.0)
        assertEquals(1, turns(events))
        assertTrue(d.lateralMps2(120.0, 40.0) > 2.0)
    }
}
