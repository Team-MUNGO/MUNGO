package com.example.poco.data.record

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * A trip is written while driving (events appended as they happen) and finalized once on end.
 * Summary columns are frozen at completion: later setting changes never recompute history.
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val petName: String,
    val petWeightTenths: Int?,
    val petSize: String?,
    val seat: String,
    val startedAtEpoch: Long,
    val endedAtEpoch: Long? = null,
    /** IN_PROGRESS, COMPLETED, INTERRUPTED (process died before a normal end). */
    val status: String = "IN_PROGRESS",
    val startRestIntervalMinutes: Int,
    val initialScore: Int = 100,
    val finalScore: Int? = null,
    val minScore: Int? = null,
    val drivingMs: Long? = null,
    val restMs: Long? = null,
    val restCount: Int? = null,
    val rapidAccelerationCount: Int? = null,
    val rapidBrakingCount: Int? = null,
    val sharpTurnCount: Int? = null,
    val repeatedTurnCount: Int? = null,
    val speedBumpCount: Int? = null,
    val windowWarningCount: Int? = null,
    val temperatureAlertCount: Int? = null,
    /** No verified odometer/route-distance source yet: null means "not measurable", never 0. */
    val distanceMeters: Long? = null,
    val origin: String? = null,
    val destination: String? = null,
    /** User-entered motion-sickness checklist (comma separated); not a diagnosis. */
    val motionSicknessSigns: String? = null,
    /** True when any value came from debug simulation; such trips are labeled, never mixed silently. */
    val simulated: Boolean = false
)

@Entity(
    tableName = "trip_events",
    indices = [Index("tripId"), Index(value = ["tripId", "dedupeKey"], unique = true)]
)
data class TripEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: String,
    val atEpoch: Long,
    val type: String,
    val scoreDelta: Int? = null,
    val scoreAfter: Int? = null,
    val detail: String? = null,
    /** Optional idempotency key (e.g. AI rest request per cycle). Unique per trip. */
    val dedupeKey: String? = null
)

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: TripEventEntity): Long

    @Query("SELECT * FROM trip_events WHERE tripId = :tripId ORDER BY atEpoch, id")
    suspend fun events(tripId: String): List<TripEventEntity>

    @Query("SELECT * FROM trip_events WHERE tripId = :tripId ORDER BY atEpoch, id")
    fun observeEvents(tripId: String): Flow<List<TripEventEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun trip(id: String): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeTrip(id: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE status != 'IN_PROGRESS' ORDER BY startedAtEpoch DESC")
    fun observeFinishedTrips(): Flow<List<TripEntity>>

    /** Idempotent: a second end event cannot overwrite or duplicate a completed trip. */
    @Query(
        """UPDATE trips SET status = 'COMPLETED', endedAtEpoch = :endedAt, finalScore = :finalScore,
        minScore = :minScore, drivingMs = :drivingMs, restMs = :restMs, restCount = :restCount,
        rapidAccelerationCount = :accel, rapidBrakingCount = :brake, sharpTurnCount = :sharp,
        repeatedTurnCount = :repeated, speedBumpCount = :bumps, windowWarningCount = :windows,
        temperatureAlertCount = :temps WHERE id = :id AND status = 'IN_PROGRESS'"""
    )
    suspend fun complete(
        id: String,
        endedAt: Long,
        finalScore: Int,
        minScore: Int,
        drivingMs: Long,
        restMs: Long,
        restCount: Int,
        accel: Int,
        brake: Int,
        sharp: Int,
        repeated: Int,
        bumps: Int,
        windows: Int,
        temps: Int
    ): Int

    @Query("UPDATE trips SET status = 'INTERRUPTED' WHERE status = 'IN_PROGRESS' AND id != :keep")
    suspend fun markInterrupted(keep: String): Int

    @Query("UPDATE trips SET motionSicknessSigns = :signs WHERE id = :id")
    suspend fun setMotionSickness(id: String, signs: String?)
}

@Database(entities = [TripEntity::class, TripEventEntity::class], version = 1, exportSchema = true)
abstract class TripDatabase : RoomDatabase() {
    abstract fun trips(): TripDao
}
