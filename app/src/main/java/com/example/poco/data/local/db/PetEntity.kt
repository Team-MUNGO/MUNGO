package com.example.poco.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.PetSpecies
import com.example.poco.data.model.PetTrait
import com.example.poco.data.model.Restraint
import com.example.poco.data.model.VoiceFrequency
import java.time.Year

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val breed: String,
    /** ageRecordedYear 해에 입력한 나이 */
    val ageYears: Int?,
    /** 나이를 입력한 해. 지금 나이 = ageYears + (올해 - 이 해). 예전 데이터는 2026년 입력으로 본다 */
    @ColumnInfo(defaultValue = "$AGE_BASE_YEAR")
    val ageRecordedYear: Int = AGE_BASE_YEAR,
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
    val restraint: String = Restraint.NONE.name,
    /** PetSpecies 이름 */
    @ColumnInfo(defaultValue = "DOG")
    val species: String = PetSpecies.DOG.name,
    /** PetTrait 이름들을 쉼표로 이은 값 (예: "MOTION_SICK,HEAT_SENSITIVE") */
    @ColumnInfo(defaultValue = "")
    val traits: String = "",
    @ColumnInfo(defaultValue = "1")
    val voiceGuideEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "1")
    val temperatureAlertEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "1")
    val windowAlertEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "1")
    val restSuggestionEnabled: Boolean = true
)

/** 앱을 만든 해. 6→7 마이그레이션 전 데이터의 나이 입력 해 */
private const val AGE_BASE_YEAR = 2026

/** 해가 바뀐 만큼 나이를 더한다 (생일은 모르니 1월 1일에 한 살) */
private fun ageNow(ageYears: Int?, recordedYear: Int, thisYear: Int): Int? =
    ageYears?.let { it + (thisYear - recordedYear).coerceAtLeast(0) }

fun PetEntity.toModel(thisYear: Int = Year.now().value) = Pet(
    id = id,
    name = name,
    breed = breed,
    ageYears = ageNow(ageYears, ageRecordedYear, thisYear),
    weightKg = weightKg,
    seat = PetSeat.entries.find { it.name == seat } ?: PetSeat.REAR_RIGHT,
    motionSensitivity = motionSensitivity,
    voiceFrequency = voiceFrequency,
    windowAlertPercent = windowAlertPercent,
    photoPath = photoPath,
    restraint = Restraint.entries.find { it.name == restraint } ?: Restraint.NONE,
    species = PetSpecies.entries.find { it.name == species } ?: PetSpecies.DOG,
    traits = traits.split(',').mapNotNull { name ->
        PetTrait.entries.find { it.name == name }
    }.toSet(),
    voiceGuideEnabled = voiceGuideEnabled,
    temperatureAlertEnabled = temperatureAlertEnabled,
    windowAlertEnabled = windowAlertEnabled,
    restSuggestionEnabled = restSuggestionEnabled
)

fun Pet.toEntity(thisYear: Int = Year.now().value) = PetEntity(
    id = id,
    name = name,
    breed = breed,
    ageYears = ageYears,
    ageRecordedYear = thisYear,
    weightKg = weightKg,
    seat = seat.name,
    motionSensitivity = motionSensitivity,
    voiceFrequency = voiceFrequency,
    windowAlertPercent = windowAlertPercent,
    photoPath = photoPath,
    restraint = restraint.name,
    species = species.name,
    traits = traits.sortedBy { it.ordinal }.joinToString(",") { it.name },
    voiceGuideEnabled = voiceGuideEnabled,
    temperatureAlertEnabled = temperatureAlertEnabled,
    windowAlertEnabled = windowAlertEnabled,
    restSuggestionEnabled = restSuggestionEnabled
)
