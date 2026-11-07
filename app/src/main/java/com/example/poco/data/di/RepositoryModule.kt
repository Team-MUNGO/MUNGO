package com.example.poco.data.di

import com.example.poco.data.repository.HvacRepository
import com.example.poco.data.repository.SttRepository
import com.example.poco.data.repository.TtsRepository
import com.example.poco.data.repository.WindowRepository
import com.example.poco.data.repositoryimpl.HvacRepositoryImpl
import com.example.poco.data.repositoryimpl.SttRepositoryImpl
import com.example.poco.data.repositoryimpl.TtsRepositoryImpl
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
}
