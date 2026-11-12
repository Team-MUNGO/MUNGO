package com.example.poco.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.poco.data.model.EventStat
import com.example.poco.data.model.ShakeLevel
import com.example.poco.data.model.TimelineBar
import com.example.poco.data.model.TripSummary

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val destination: String?,
    val distanceKm: Float,
    val comfortScore: Int,
    val hardBrakeCount: Int,
    val hardBrakePoints: Int,
    val hardAccelCount: Int,
    val hardAccelPoints: Int,
    val sharpTurnCount: Int,
    val sharpTurnPoints: Int,
    val calmRecoveryCount: Int,
    val calmRecoveryPoints: Int,
    val temperatureAlertCount: Int,
    val temperatureAlertPoints: Int,
    val windowAlertCount: Int,
    val calmMinutes: Int,
    /** "CALM:0.3;BRAKE:0.9;..." */
    val timeline: String
)

fun TripEntity.toModel(scoreDelta: Int? = null) = TripSummary(
    id = id,
    startedAtMillis = startedAtMillis,
    endedAtMillis = endedAtMillis,
    destination = destination,
    distanceKm = distanceKm,
    comfortScore = comfortScore,
    hardBrake = EventStat(hardBrakeCount, hardBrakePoints),
    hardAccel = EventStat(hardAccelCount, hardAccelPoints),
    sharpTurn = EventStat(sharpTurnCount, sharpTurnPoints),
    calmRecovery = EventStat(calmRecoveryCount, calmRecoveryPoints),
    temperatureAlert = EventStat(temperatureAlertCount, temperatureAlertPoints),
    windowAlertCount = windowAlertCount,
    calmMinutes = calmMinutes,
    timeline = timeline.split(';').mapNotNull { token ->
        val (level, intensity) = token.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
        TimelineBar(
            level = runCatching { ShakeLevel.valueOf(level) }.getOrDefault(ShakeLevel.CALM),
            intensity = intensity.toFloatOrNull() ?: 0f
        )
    },
    scoreDelta = scoreDelta
)

fun TripSummary.toEntity() = TripEntity(
    id = id,
    startedAtMillis = startedAtMillis,
    endedAtMillis = endedAtMillis,
    destination = destination,
    distanceKm = distanceKm,
    comfortScore = comfortScore,
    hardBrakeCount = hardBrake.count,
    hardBrakePoints = hardBrake.points,
    hardAccelCount = hardAccel.count,
    hardAccelPoints = hardAccel.points,
    sharpTurnCount = sharpTurn.count,
    sharpTurnPoints = sharpTurn.points,
    calmRecoveryCount = calmRecovery.count,
    calmRecoveryPoints = calmRecovery.points,
    temperatureAlertCount = temperatureAlert.count,
    temperatureAlertPoints = temperatureAlert.points,
    windowAlertCount = windowAlertCount,
    calmMinutes = calmMinutes,
    timeline = timeline.joinToString(";") { "${it.level.name}:${it.intensity}" }
)
