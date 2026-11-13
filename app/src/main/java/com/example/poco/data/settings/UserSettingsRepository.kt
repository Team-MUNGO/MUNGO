package com.example.poco.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.poco.core.driving.RestIntervalPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface UserSettingsRepository {
    /** User rest interval in minutes; 120 only when nothing has ever been saved. */
    val restIntervalMinutes: Flow<Int>
    val voiceGuidanceEnabled: Flow<Boolean>
    suspend fun setRestIntervalMinutes(minutes: Int)
    suspend fun setVoiceGuidanceEnabled(enabled: Boolean)

    /** 설정 > 알림 '도착 요약': 주행을 마치면 요약 화면을 보여 줄지 */
    val arrivalSummaryEnabled: Flow<Boolean> get() = flowOf(true)
    suspend fun setArrivalSummaryEnabled(enabled: Boolean) {}
}

private val Context.userSettings: DataStore<Preferences> by preferencesDataStore("poco_user_settings")

@Singleton
class DataStoreUserSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : UserSettingsRepository {
    private val intervalKey = intPreferencesKey("rest_interval_minutes")
    private val voiceKey = booleanPreferencesKey("voice_guidance_enabled")

    // Reads never write a default: an existing user value can never be overwritten by 120.
    override val restIntervalMinutes = context.userSettings.data.map {
        it[intervalKey]?.let(RestIntervalPolicy::clamp) ?: RestIntervalPolicy.DEFAULT_MINUTES
    }
    override val voiceGuidanceEnabled = context.userSettings.data.map { it[voiceKey] ?: true }
    private val arrivalKey = booleanPreferencesKey("arrival_summary_enabled")
    override val arrivalSummaryEnabled = context.userSettings.data.map { it[arrivalKey] ?: true }

    override suspend fun setArrivalSummaryEnabled(enabled: Boolean) {
        context.userSettings.edit { it[arrivalKey] = enabled }
    }

    override suspend fun setRestIntervalMinutes(minutes: Int) {
        context.userSettings.edit { it[intervalKey] = RestIntervalPolicy.clamp(minutes) }
    }

    override suspend fun setVoiceGuidanceEnabled(enabled: Boolean) {
        context.userSettings.edit { it[voiceKey] = enabled }
    }
}
