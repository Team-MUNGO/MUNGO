package com.example.poco.data.di

import android.content.Context
import androidx.room.Room
import com.example.poco.data.local.db.PetDao
import com.example.poco.data.local.db.PocoDatabase
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
    fun providePocoDatabase(@ApplicationContext context: Context): PocoDatabase =
        Room.databaseBuilder(context, PocoDatabase::class.java, "poco.db").build()

    @Provides
    fun providePetDao(database: PocoDatabase): PetDao = database.petDao()

    @Provides
    fun provideTripDao(database: PocoDatabase): TripDao = database.tripDao()
}
