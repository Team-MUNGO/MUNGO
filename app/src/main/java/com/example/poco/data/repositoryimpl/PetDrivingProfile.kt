package com.example.poco.data.repositoryimpl

import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.PetSeat
import com.example.poco.data.model.Pet
import com.example.poco.data.repository.PetRepository
import com.example.poco.domain.driving.temperatureStandard
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The driving session reads the pet from the shared profile (마이펫 · 탑승 준비), never from a copy.
 * Same choice as the boarding/driving screens: the first registered pet is the one being watched.
 */
@Singleton
class PetDrivingProfile @Inject constructor(private val pets: PetRepository) {
    val profile: Flow<DrivingSettings?> = pets.observePets().map {
        it.firstOrNull()?.toDrivingSettings()
    }

    suspend fun load(): DrivingSettings? = profile.first()
}

/** Comfort range comes from the mungo standard for this pet (breed · weight · age). */
fun Pet.toDrivingSettings(): DrivingSettings? = runCatching {
    val comfort = temperatureStandard.comfort
    DrivingSettings(
        petName = name.trim(),
        seat = PetSeat.valueOf(seat.name),
        minTemperatureC = comfort.start,
        maxTemperatureC = comfort.endInclusive,
        weightTenths = weightKg?.let { (it * 10).roundToInt() }?.takeIf { it in 1..1500 },
        windowAlerts = windowAlertEnabled,
        restSuggestions = restSuggestionEnabled
    )
}.getOrNull()
