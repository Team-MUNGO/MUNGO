package com.example.poco.data.repositoryimpl

import android.content.Context
import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.PetSeat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrivingSettingsRepository @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("driving_settings", Context.MODE_PRIVATE)

    fun load(): DrivingSettings? = runCatching {
        val name = preferences.getString("pet_name", null) ?: return null
        DrivingSettings(
            name,
            PetSeat.valueOf(preferences.getString("seat", null) ?: return null),
            preferences.getFloat("min_c", Float.NaN),
            preferences.getFloat("max_c", Float.NaN),
            preferences.getInt("weight_tenths", 0).takeIf { it > 0 }
        )
    }.getOrNull()

    fun save(settings: DrivingSettings) {
        preferences.edit().putString("pet_name", settings.petName)
            .putString("seat", settings.seat.name)
            .putFloat("min_c", settings.minTemperatureC)
            .putFloat("max_c", settings.maxTemperatureC)
            .apply { settings.weightTenths?.let { putInt("weight_tenths", it) } ?: remove("weight_tenths") }
            .apply()
    }
}
