package com.example.poco.data.di

import android.os.SystemClock
import com.example.poco.core.driving.ComfortScoreConfig
import com.example.poco.core.driving.DrivingClock
import com.example.poco.data.automotive.VehicleDataSource
import com.example.poco.data.automotive.VehicleDataSourceImpl
import com.example.poco.data.repository.DrivingSessionRepository
import com.example.poco.data.repository.VehicleRepository
import com.example.poco.data.repositoryimpl.DrivingSessionRepositoryImpl
import com.example.poco.data.repositoryimpl.VehicleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DrivingBindings {
    @Binds @Singleton
    abstract fun vehicleSource(impl: VehicleDataSourceImpl): VehicleDataSource

    @Binds @Singleton
    abstract fun vehicleRepository(impl: VehicleRepositoryImpl): VehicleRepository

    // NaviDataSource / NaviRepository are bound in DataSourceModule / RepositoryModule.

    @Binds @Singleton
    abstract fun session(impl: DrivingSessionRepositoryImpl): DrivingSessionRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DrivingModule {
    @Provides @Singleton
    fun clock(): DrivingClock = DrivingClock { SystemClock.elapsedRealtime() }

    @Provides @Singleton
    fun scoreConfig(): ComfortScoreConfig = ComfortScoreConfig()
}
