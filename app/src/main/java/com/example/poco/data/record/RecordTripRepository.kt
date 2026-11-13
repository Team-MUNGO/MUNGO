package com.example.poco.data.record

import com.example.poco.data.model.EventStat
import com.example.poco.data.model.TripStats
import com.example.poco.data.model.TripSummary
import com.example.poco.data.repository.TripRepository
import com.example.poco.core.util.suspendRunCatching
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Single trip store: the home card, the pet stats and the record screens all read the trip
 * database written by the driving session (poco_trips.db).
 *
 * [TripSummary.id] is the trip's start time (record ids are strings). Values the session does not
 * record (per-event points, shake timeline, distance) are left empty instead of being estimated.
 */
@Singleton
class RecordTripRepository @Inject constructor(
    private val dao: TripDao,
    private val legacy: LegacyTripImporter
) : TripRepository {
    // Old poco.db trips are copied before the first read, so the home card never misses them.
    private val scored: Flow<List<TripEntity>> = flow {
        legacy.importOnce()
        emitAll(dao.observeFinishedTrips())
    }.map { list -> list.filter { it.finalScore != null } }

    override fun observeLastTrip(): Flow<TripSummary?> = scored.map { trips ->
        val last = trips.getOrNull(0) ?: return@map null
        last.toSummary(trips.getOrNull(1)?.let { last.finalScore!! - it.finalScore!! })
    }

    override fun observeTripStartsSince(sinceMillis: Long): Flow<List<Long>> =
        scored.map { trips -> trips.map { it.startedAtEpoch }.filter { it >= sinceMillis } }

    override fun observeTripStats(): Flow<TripStats> = scored.map { trips ->
        TripStats(
            count = trips.size,
            averageScore = trips.takeIf { it.isNotEmpty() }?.let { l -> l.sumOf { it.finalScore!! } / l.size },
            lastStartedAtMillis = trips.maxOfOrNull { it.startedAtEpoch }
        )
    }

    override suspend fun getTrip(id: Long): Result<TripSummary?> = suspendRunCatching {
        val trips = scored.first()
        val index = trips.indexOfFirst { it.startedAtEpoch == id }
        if (index < 0) return@suspendRunCatching null
        val trip = trips[index]
        trip.toSummary(trips.getOrNull(index + 1)?.let { trip.finalScore!! - it.finalScore!! })
    }

    override suspend fun getLatestScore(): Result<Int?> = suspendRunCatching {
        scored.first().firstOrNull()?.finalScore
    }

    /** Kept for the old driving screen: stores the summary as a completed record. */
    override suspend fun saveTrip(trip: TripSummary): Result<Long> = suspendRunCatching {
        dao.insertTrip(trip.toRecord("summary-${trip.startedAtMillis}", petName = "반려견", seat = "UNKNOWN"))
        trip.startedAtMillis
    }
}

private fun TripEntity.toSummary(scoreDelta: Int?) = TripSummary(
    id = startedAtEpoch,
    startedAtMillis = startedAtEpoch,
    endedAtMillis = endedAtEpoch ?: startedAtEpoch,
    destination = destination,
    distanceKm = distanceMeters?.let { it / 1000f } ?: 0f,
    comfortScore = finalScore ?: initialScore,
    hardBrake = EventStat(rapidBrakingCount ?: 0),
    hardAccel = EventStat(rapidAccelerationCount ?: 0),
    sharpTurn = EventStat((sharpTurnCount ?: 0) + (repeatedTurnCount ?: 0)),
    calmRecovery = EventStat(),
    temperatureAlert = EventStat(temperatureAlertCount ?: 0),
    windowAlertCount = windowWarningCount ?: 0,
    calmMinutes = 0,
    timeline = emptyList(),
    scoreDelta = scoreDelta
)

/** A summary-only trip (old driving screen or the old poco.db table) as a completed record. */
internal fun TripSummary.toRecord(id: String, petName: String, seat: String) = TripEntity(
    id = id,
    petName = petName,
    petWeightTenths = null,
    petSize = null,
    seat = seat,
    startedAtEpoch = startedAtMillis,
    endedAtEpoch = endedAtMillis,
    status = "COMPLETED",
    startRestIntervalMinutes = 0,
    finalScore = comfortScore,
    drivingMs = (endedAtMillis - startedAtMillis).coerceAtLeast(0),
    rapidAccelerationCount = hardAccel.count,
    rapidBrakingCount = hardBrake.count,
    sharpTurnCount = sharpTurn.count,
    windowWarningCount = windowAlertCount,
    temperatureAlertCount = temperatureAlert.count,
    distanceMeters = distanceKm.takeIf { it > 0f }?.let { (it * 1000).toLong() },
    destination = destination
)
