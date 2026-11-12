package com.example.poco.data.record

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

object TripEventType {
    const val SCORE_PENALTY = "SCORE_PENALTY" // detail = DrivingEvent reason
    const val SCORE_RECOVERY = "SCORE_RECOVERY"
    const val INTERVAL_CHANGED = "INTERVAL_CHANGED" // detail = "from->to"
    const val REST_STARTED = "REST_STARTED" // detail = USER | WAYPOINT_ARRIVAL
    const val REST_COMPLETED = "REST_COMPLETED" // detail = duration ms
    const val REST_CANCELLED = "REST_CANCELLED"
    const val DEPARTED = "DEPARTED"
    const val WINDOW_WARNING = "WINDOW_WARNING" // detail = percent
    const val TEMPERATURE_OUT_OF_RANGE = "TEMPERATURE_OUT_OF_RANGE" // detail = "HOT:27.5"
    const val TEMPERATURE_SAMPLE = "TEMPERATURE_SAMPLE" // detail = measured cabin °C
    const val AI_REST_REQUEST = "AI_REST_REQUEST" // dedupeKey = rest-cycle-N
    const val WAYPOINT_ADDED = "WAYPOINT_ADDED" // detail = Google place ID
    const val WAYPOINT_FAILED = "WAYPOINT_FAILED"
    const val WAYPOINT_CANCELLED = "WAYPOINT_CANCELLED"
    const val WAYPOINT_ARRIVED = "WAYPOINT_ARRIVED" // detail = Navi-reported name
    const val SENSOR_ANOMALY = "SENSOR_ANOMALY"
}

data class TripCounts(
    val rapidAcceleration: Int,
    val rapidBraking: Int,
    val sharpTurn: Int,
    val repeatedTurns: Int,
    val speedBump: Int,
    val windowWarning: Int,
    val temperatureAlert: Int,
    val minScore: Int
)

/** Counts come from the event log only, so the end screen, list and detail always agree. */
fun countEvents(events: List<TripEventEntity>, initialScore: Int = 100): TripCounts {
    fun penalties(reason: String) =
        events.count { it.type == TripEventType.SCORE_PENALTY && it.detail == reason }
    return TripCounts(
        penalties("RAPID_ACCELERATION"),
        penalties("RAPID_BRAKING"),
        penalties("SHARP_TURN"),
        penalties("REPEATED_TURNS"),
        penalties("SPEED_BUMP"),
        events.count { it.type == TripEventType.WINDOW_WARNING },
        events.count { it.type == TripEventType.TEMPERATURE_OUT_OF_RANGE },
        (events.mapNotNull { it.scoreAfter } + initialScore).min()
    )
}

interface TripRecordSink {
    fun begin(trip: TripEntity)
    fun event(event: TripEventEntity)
    /** Returns true only for the first completion of an in-progress trip. */
    suspend fun complete(
        tripId: String,
        endedAt: Long,
        finalScore: Int,
        drivingMs: Long,
        restMs: Long,
        restCount: Int
    ): Boolean
}

interface TripHistoryRepository {
    fun finishedTrips(): Flow<List<TripEntity>>
    fun trip(id: String): Flow<TripEntity?>
    fun events(id: String): Flow<List<TripEventEntity>>
    suspend fun setMotionSickness(id: String, record: MotionRecord)
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TripRecorder @Inject constructor(private val dao: TripDao) : TripRecordSink, TripHistoryRepository {
    // One writer, FIFO: events are persisted in order and completion runs after queued events.
    private val writer = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + writer)

    override fun begin(trip: TripEntity) {
        scope.launch {
            // A trip left IN_PROGRESS by a killed process is labeled, not silently completed.
            dao.markInterrupted(trip.id)
            dao.insertTrip(trip)
        }
    }

    override fun event(event: TripEventEntity) {
        scope.launch { runCatching { dao.insertEvent(event) }.onFailure { Timber.w(it, "trip event") } }
    }

    override suspend fun complete(
        tripId: String,
        endedAt: Long,
        finalScore: Int,
        drivingMs: Long,
        restMs: Long,
        restCount: Int
    ): Boolean = withContext(writer) {
        val counts = countEvents(dao.events(tripId))
        dao.complete(
            tripId, endedAt, finalScore, minOf(counts.minScore, finalScore), drivingMs, restMs, restCount,
            counts.rapidAcceleration, counts.rapidBraking, counts.sharpTurn, counts.repeatedTurns,
            counts.speedBump, counts.windowWarning, counts.temperatureAlert
        ) == 1
    }

    override fun finishedTrips() = dao.observeFinishedTrips()
    override fun trip(id: String) = dao.observeTrip(id)
    override fun events(id: String) = dao.observeEvents(id)
    override suspend fun setMotionSickness(id: String, record: MotionRecord) =
        withContext(writer) { dao.setMotionSickness(id, record.encode()) }
}
