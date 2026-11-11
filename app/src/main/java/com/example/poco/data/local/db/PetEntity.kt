package com.example.poco.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.Restraint
import com.example.poco.data.model.VoiceFrequency

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val breed: String,
    val ageYears: Int?,
    val weightKg: Float?,
    /** PetSeat 이름. 예전 값(CARRIER 등)이 남아 있어도 깨지지 않게 문자열로 저장 */
    val seat: String,
    val motionSensitivity: MotionSensitivity,
    val voiceFrequency: VoiceFrequency,
    @ColumnInfo(defaultValue = "${Pet.DEFAULT_WINDOW_ALERT_PERCENT}")
    val windowAlertPercent: Int,
    val photoPath: String? = null,
    /** Restraint 이름 */
    @ColumnInfo(defaultValue = "NONE")
    val restraint: String = Restraint.NONE.name
)

fun PetEntity.toModel() = Pet(
    id = id,
    name = name,
    breed = breed,
    ageYears = ageYears,
    weightKg = weightKg,
    seat = PetSeat.entries.find { it.name == seat } ?: PetSeat.REAR_RIGHT,
    motionSensitivity = motionSensitivity,
    voiceFrequency = voiceFrequency,
    windowAlertPercent = windowAlertPercent,
    photoPath = photoPath,
    restraint = Restraint.entries.find { it.name == restraint } ?: Restraint.NONE
)

fun Pet.toEntity() = PetEntity(
    id = id,
    name = name,
    breed = breed,
    ageYears = ageYears,
    weightKg = weightKg,
    seat = seat.name,
    motionSensitivity = motionSensitivity,
    voiceFrequency = voiceFrequency,
    windowAlertPercent = windowAlertPercent,
    photoPath = photoPath,
    restraint = restraint.name
)
