package com.example.poco.core.designsystem

import com.example.poco.data.model.EventStat
import com.example.poco.data.model.ShakeLevel
import com.example.poco.data.model.TimelineBar
import com.example.poco.data.model.TripSummary

/** @Preview 전용 샘플 이동 기록 (시안 수치) */
fun previewTrip(): TripSummary {
    val levels = "CCCBCCCTCCCCBCTCBCCC"
    return TripSummary(
        id = 1,
        startedAtMillis = 1_762_149_600_000L,
        endedAtMillis = 1_762_149_600_000L + 62 * 60_000L,
        destination = "애견카페 봄날",
        distanceKm = 38f,
        comfortScore = 82,
        hardBrake = EventStat(3, -15),
        hardAccel = EventStat(1, -5),
        sharpTurn = EventStat(2, -6),
        calmRecovery = EventStat(4, 8),
        temperatureAlert = EventStat(),
        windowAlertCount = 1,
        calmMinutes = 31,
        timeline = levels.mapIndexed { index, c ->
            when (c) {
                'B' -> TimelineBar(ShakeLevel.BRAKE, 1f)
                'T' -> TimelineBar(ShakeLevel.TURN, 0.6f)
                else -> TimelineBar(ShakeLevel.CALM, listOf(0.3f, 0.32f, 0.4f, 0.26f)[index % 4])
            }
        },
        scoreDelta = 6
    )
}
