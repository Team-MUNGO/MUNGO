package com.example.poco.data.di

import android.content.Context
import android.os.Build
import androidx.room.Room
import com.example.poco.data.assistant.LocationStream
import com.example.poco.data.assistant.PleosLocationStream
import com.example.poco.data.record.TripDao
import com.example.poco.data.record.TripDatabase
import com.example.poco.data.record.TripHistoryRepository
import com.example.poco.data.record.TripRecordSink
import com.example.poco.data.record.TripRecorder
import com.example.poco.data.repositoryimpl.DataOrigin
import com.example.poco.data.settings.DataStoreUserSettingsRepository
import com.example.poco.data.settings.UserSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TripBindings {
    @Binds @Singleton
    abstract fun settings(impl: DataStoreUserSettingsRepository): UserSettingsRepository

    @Binds @Singleton
    abstract fun sink(impl: TripRecorder): TripRecordSink

    @Binds @Singleton
    abstract fun history(impl: TripRecorder): TripHistoryRepository

    @Binds @Singleton
    abstract fun locationStream(impl: PleosLocationStream): LocationStream
}

@Module
@InstallIn(SingletonComponent::class)
object TripModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): TripDatabase =
        Room.databaseBuilder(context, TripDatabase::class.java, "poco_trips.db").build()

    @Provides
    fun dao(db: TripDatabase): TripDao = db.trips()

    /** Pleos Connect emulator (crp_avd) or generic AVD: vehicle values are simulated VHAL data. */
    @Provides @Singleton
    fun origin(): DataOrigin = DataOrigin {
        listOf(Build.PRODUCT, Build.DEVICE, Build.FINGERPRINT).any {
            it.contains("avd", true) || it.contains("emulator", true) || it.contains("generic", true) ||
                it.startsWith("sdk", true)
        }
    }
}
