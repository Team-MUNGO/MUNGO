package com.example.poco.data.di

import com.example.poco.data.pleos.datasource.HvacDataSource
import com.example.poco.data.pleos.datasource.SttDataSource
import com.example.poco.data.pleos.datasource.TtsDataSource
import com.example.poco.data.pleos.datasource.WindowDataSource
import com.example.poco.data.pleos.datasourceimpl.HvacDataSourceImpl
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
}
