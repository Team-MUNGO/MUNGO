package com.example.poco.data.di

import com.example.poco.data.assistant.AssistantLocationSource
import com.example.poco.data.assistant.AssistantRemoteDataSource
import com.example.poco.data.assistant.AssistantRemoteDataSourceImpl
import com.example.poco.data.assistant.AssistantWaypointSource
import com.example.poco.data.assistant.PleosLocationSource
import com.example.poco.data.assistant.PleosWaypointSource
import com.example.poco.data.repository.AssistantRepository
import com.example.poco.data.repositoryimpl.AssistantRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantModule {
    @Binds @Singleton
    abstract fun remote(impl: AssistantRemoteDataSourceImpl): AssistantRemoteDataSource

    @Binds @Singleton
    abstract fun repository(impl: AssistantRepositoryImpl): AssistantRepository

    @Binds abstract fun waypoint(impl: PleosWaypointSource): AssistantWaypointSource

    @Binds abstract fun location(impl: PleosLocationSource): AssistantLocationSource
}
