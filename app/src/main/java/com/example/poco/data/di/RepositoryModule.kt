package com.example.poco.data.di

import com.example.poco.data.repository.DepartureRepository
import com.example.poco.data.repository.GearRepository
import com.example.poco.data.repository.HvacRepository
import com.example.poco.data.repository.NaviRepository
import com.example.poco.data.repository.OnboardingRepository
import com.example.poco.data.repository.PetRepository
import com.example.poco.data.repository.SttRepository
import com.example.poco.data.repository.TripRepository
import com.example.poco.data.repository.TtsRepository
import com.example.poco.data.repository.VehicleMotionRepository
import com.example.poco.data.repository.WindowRepository
import com.example.poco.data.repositoryimpl.DepartureRepositoryImpl
import com.example.poco.data.repositoryimpl.GearRepositoryImpl
import com.example.poco.data.repositoryimpl.HvacRepositoryImpl
import com.example.poco.data.repositoryimpl.NaviRepositoryImpl
import com.example.poco.data.repositoryimpl.OnboardingRepositoryImpl
import com.example.poco.data.repositoryimpl.PetRepositoryImpl
import com.example.poco.data.repositoryimpl.SttRepositoryImpl
import com.example.poco.data.repositoryimpl.TripRepositoryImpl
import com.example.poco.data.repositoryimpl.TtsRepositoryImpl
import com.example.poco.data.repositoryimpl.VehicleMotionRepositoryImpl
import com.example.poco.data.repositoryimpl.WindowRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindWindowRepository(impl: WindowRepositoryImpl): WindowRepository

    @Binds
    @Singleton
    abstract fun bindHvacRepository(impl: HvacRepositoryImpl): HvacRepository

    @Binds
    @Singleton
    abstract fun bindTtsRepository(impl: TtsRepositoryImpl): TtsRepository

    @Binds
    @Singleton
    abstract fun bindSttRepository(impl: SttRepositoryImpl): SttRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindPetRepository(impl: PetRepositoryImpl): PetRepository

    @Binds
    @Singleton
    abstract fun bindGearRepository(impl: GearRepositoryImpl): GearRepository

    @Binds
    @Singleton
    abstract fun bindDepartureRepository(impl: DepartureRepositoryImpl): DepartureRepository

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: TripRepositoryImpl): TripRepository

    @Binds
    @Singleton
    abstract fun bindVehicleMotionRepository(
        impl: VehicleMotionRepositoryImpl
    ): VehicleMotionRepository

    @Binds
    @Singleton
    abstract fun bindNaviRepository(impl: NaviRepositoryImpl): NaviRepository
}
