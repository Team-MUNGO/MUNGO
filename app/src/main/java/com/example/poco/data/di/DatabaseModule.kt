package com.example.poco.data.di

import android.content.Context
import androidx.room.Room
import com.example.poco.data.local.db.MungoDatabase
import com.example.poco.data.local.db.PetDao
import com.example.poco.data.local.db.TripDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideMungoDatabase(@ApplicationContext context: Context): MungoDatabase =
        Room.databaseBuilder(context, MungoDatabase::class.java, "poco.db").build()

    @Provides
    fun providePetDao(database: MungoDatabase): PetDao = database.petDao()

    @Provides
    fun provideTripDao(database: MungoDatabase): TripDao = database.tripDao()
}
