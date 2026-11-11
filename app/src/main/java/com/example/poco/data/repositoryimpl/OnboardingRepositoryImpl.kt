package com.example.poco.data.repositoryimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.poco.core.util.suspendRunCatching
import com.example.poco.data.repository.OnboardingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OnboardingRepositoryImpl @Inject constructor(private val dataStore: DataStore<Preferences>) :
    OnboardingRepository {
    override val isPermissionCompleted: Flow<Boolean> =
        dataStore.data.map { it[KEY_PERMISSION_COMPLETED] ?: false }

    override suspend fun setPermissionCompleted(): Result<Unit> = suspendRunCatching {
        dataStore.edit { it[KEY_PERMISSION_COMPLETED] = true }
        Unit
    }

    private companion object {
        val KEY_PERMISSION_COMPLETED = booleanPreferencesKey("permission_completed")
    }
}
