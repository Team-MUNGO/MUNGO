package com.example.poco.data.assistant

import com.example.poco.data.record.MotionRecord
import com.example.poco.data.record.TripEntity
import com.example.poco.data.record.TripEventEntity
import com.example.poco.data.record.TripEventType

private const val MAX_EVENTS = 40

/** Builds the /summary payload from the stored record. Returns null while the trip is not finished. */
fun TripEntity.toSummaryRequest(
    events: List<TripEventEntity>,
    previous: TripEntity?
): TripSummaryRequest? {
    val score = finalScore ?: return null
    val end = endedAtEpoch ?: return null
    val minute = { at: Long -> ((at - startedAtEpoch) / 60_000L).toInt().coerceIn(0, 2880) }
    val mapped = events.mapNotNull { e ->
        val kind = when (e.type) {
            TripEventType.SCORE_PENALTY -> when (e.detail) {
                "RAPID_BRAKING" -> "RAPID_BRAKING"
                "RAPID_ACCELERATION" -> "RAPID_ACCELERATION"
                "SHARP_TURN" -> "SHARP_TURN"
                "REPEATED_TURNS" -> "REPEATED_TURN"
                "SPEED_BUMP" -> "SPEED_BUMP"
                else -> null
            }
            TripEventType.SCORE_RECOVERY -> "RECOVERY"
            TripEventType.REST_STARTED -> "REST"
            TripEventType.WINDOW_WARNING -> "WINDOW_WARNING"
            TripEventType.TEMPERATURE_OUT_OF_RANGE -> "TEMPERATURE_ALERT"
            else -> null
        } ?: return@mapNotNull null
        TripSummaryEvent(minute(e.atEpoch), kind, e.scoreDelta?.coerceIn(-100, 100))
    }
    // Keep the biggest drops and every rest first when trimming, then restore time order.
    val kept = mapped.sortedWith(
        compareBy({
            it.kind != "REST"
        }, { it.scoreDelta ?: 0 })
    ).take(MAX_EVENTS)
        .sortedBy { it.minute }
    val record = MotionRecord.decode(motionSicknessSigns)
    val signs = buildList {
        if (record?.fine == true) add("FINE")
        record?.counts?.filterValues { it > 0 }?.keys?.forEach { add(it.name) }
    }
    return TripSummaryRequest(
        requestId = id.filter {
            it.isLetterOrDigit() || it == '-' || it == '_'
        }.take(64).ifEmpty { "trip" },
        trip = TripSummaryInput(
            petName = petName.trim().take(20).ifEmpty { "반려견" },
            durationMinutes = minute(end),
            finalScore = score.coerceIn(0, 100),
            minScore = minScore?.coerceIn(0, 100),
            previousScore = previous?.finalScore?.coerceIn(0, 100),
            restCount = (restCount ?: 0).coerceIn(0, 100),
            restMinutes = ((restMs ?: 0L) / 60_000L).toInt().coerceIn(0, 2880),
            counts = TripSummaryCounts(
                rapidBraking = rapidBrakingCount ?: 0,
                sharpTurn = (sharpTurnCount ?: 0) + (repeatedTurnCount ?: 0),
                speedBump = speedBumpCount ?: 0,
                rapidAcceleration = rapidAccelerationCount ?: 0,
                windowWarning = windowWarningCount ?: 0,
                temperatureAlert = temperatureAlertCount ?: 0
            ),
            events = kept,
            motionSigns = signs
        )
    )
}
