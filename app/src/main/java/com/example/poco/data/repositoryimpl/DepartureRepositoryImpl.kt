package com.example.poco.data.repositoryimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.poco.core.util.suspendRunCatching
import com.example.poco.data.repository.DepartureRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DepartureRepositoryImpl @Inject constructor(private val dataStore: DataStore<Preferences>) :
    DepartureRepository {
    override val lastMealAt: Flow<Long?> = dataStore.data.map { it[KEY_LAST_MEAL_AT] }

    override val isVoiceGuideEnabled: Flow<Boolean> =
        dataStore.data.map { it[KEY_VOICE_GUIDE] ?: true }

    override val isRearTempWatchEnabled: Flow<Boolean> =
        dataStore.data.map { it[KEY_REAR_TEMP_WATCH] ?: true }

    override val temperatureAlertRange: Flow<ClosedFloatingPointRange<Float>> =
        dataStore.data.map {
            (it[KEY_TEMP_ALERT_MIN] ?: DEFAULT_MIN)..(it[KEY_TEMP_ALERT_MAX] ?: DEFAULT_MAX)
        }

    override suspend fun recordMeal(atMillis: Long): Result<Unit> =
        edit { it[KEY_LAST_MEAL_AT] = atMillis }

    override suspend fun setVoiceGuideEnabled(enabled: Boolean): Result<Unit> =
        edit { it[KEY_VOICE_GUIDE] = enabled }

    override suspend fun setRearTempWatchEnabled(enabled: Boolean): Result<Unit> =
        edit { it[KEY_REAR_TEMP_WATCH] = enabled }

    override suspend fun setTemperatureAlertRange(
        range: ClosedFloatingPointRange<Float>
    ): Result<Unit> = edit {
        it[KEY_TEMP_ALERT_MIN] = range.start
        it[KEY_TEMP_ALERT_MAX] = range.endInclusive
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) = suspendRunCatching {
        dataStore.edit(block)
        Unit
    }

    private companion object {
        const val DEFAULT_MIN = 18f
        const val DEFAULT_MAX = 26f
        val KEY_LAST_MEAL_AT = longPreferencesKey("last_meal_at")
        val KEY_VOICE_GUIDE = booleanPreferencesKey("trip_voice_guide")
        val KEY_REAR_TEMP_WATCH = booleanPreferencesKey("trip_rear_temp_watch")
        val KEY_TEMP_ALERT_MIN = floatPreferencesKey("temp_alert_min")
        val KEY_TEMP_ALERT_MAX = floatPreferencesKey("temp_alert_max")
    }
}
