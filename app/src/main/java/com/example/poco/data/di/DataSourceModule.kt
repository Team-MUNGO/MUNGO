package com.example.poco.data.di

import com.example.poco.data.car.datasource.CarPropertyDataSource
import com.example.poco.data.car.datasourceimpl.CarPropertyDataSourceImpl
import com.example.poco.data.local.datasource.PetLocalDataSource
import com.example.poco.data.local.datasource.PetPhotoDataSource
import com.example.poco.data.local.datasource.TripLocalDataSource
import com.example.poco.data.local.datasourceimpl.PetLocalDataSourceImpl
import com.example.poco.data.local.datasourceimpl.PetPhotoDataSourceImpl
import com.example.poco.data.local.datasourceimpl.TripLocalDataSourceImpl
import com.example.poco.data.pleos.datasource.BrakeDataSource
import com.example.poco.data.pleos.datasource.GearDataSource
import com.example.poco.data.pleos.datasource.HvacDataSource
import com.example.poco.data.pleos.datasource.LocationDataSource
import com.example.poco.data.pleos.datasource.NaviDataSource
import com.example.poco.data.pleos.datasource.SteeringDataSource
import com.example.poco.data.pleos.datasource.SttDataSource
import com.example.poco.data.pleos.datasource.TtsDataSource
import com.example.poco.data.pleos.datasource.WindowDataSource
import com.example.poco.data.pleos.datasourceimpl.BrakeDataSourceImpl
import com.example.poco.data.pleos.datasourceimpl.GearDataSourceImpl
import com.example.poco.data.pleos.datasourceimpl.HvacDataSourceImpl
import com.example.poco.data.pleos.datasourceimpl.LocationDataSourceImpl
import com.example.poco.data.pleos.datasourceimpl.NaviDataSourceImpl
import com.example.poco.data.pleos.datasourceimpl.SteeringDataSourceImpl
import com.example.poco.data.pleos.datasourceimpl.SttDataSourceImpl
import com.example.poco.data.pleos.datasourceimpl.TtsDataSourceImpl
import com.example.poco.data.pleos.datasourceimpl.WindowDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    @Singleton
    abstract fun bindWindowDataSource(impl: WindowDataSourceImpl): WindowDataSource

    @Binds
    @Singleton
    abstract fun bindHvacDataSource(impl: HvacDataSourceImpl): HvacDataSource

    @Binds
    @Singleton
    abstract fun bindTtsDataSource(impl: TtsDataSourceImpl): TtsDataSource

    @Binds
    @Singleton
    abstract fun bindSttDataSource(impl: SttDataSourceImpl): SttDataSource

    @Binds
    @Singleton
    abstract fun bindGearDataSource(impl: GearDataSourceImpl): GearDataSource

    @Binds
    @Singleton
    abstract fun bindPetLocalDataSource(impl: PetLocalDataSourceImpl): PetLocalDataSource

    @Binds
    @Singleton
    abstract fun bindTripLocalDataSource(impl: TripLocalDataSourceImpl): TripLocalDataSource

    @Binds
    @Singleton
    abstract fun bindSteeringDataSource(impl: SteeringDataSourceImpl): SteeringDataSource

    @Binds
    @Singleton
    abstract fun bindCarPropertyDataSource(impl: CarPropertyDataSourceImpl): CarPropertyDataSource

    @Binds
    @Singleton
    abstract fun bindBrakeDataSource(impl: BrakeDataSourceImpl): BrakeDataSource

    @Binds
    @Singleton
    abstract fun bindLocationDataSource(impl: LocationDataSourceImpl): LocationDataSource

    @Binds
    @Singleton
    abstract fun bindNaviDataSource(impl: NaviDataSourceImpl): NaviDataSource

    @Binds
    @Singleton
    abstract fun bindPetPhotoDataSource(impl: PetPhotoDataSourceImpl): PetPhotoDataSource
}
