package com.example.poco.data.di

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.HVAC
import ai.pleos.playground.vehicle.api.Window
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Pleos SDK 객체 제공 모듈.
 * 객체를 앱 전체에서 하나만 사용하도록 싱글톤으로 제공
 * initialize()는 PocoApplication에서 호출
 */
@Module
@InstallIn(SingletonComponent::class)
object PleosModule {
    @Provides
    @Singleton
    fun provideVehicle(@ApplicationContext context: Context): Vehicle = Vehicle(context = context)

    @Provides
    @Singleton
    fun provideWindow(vehicle: Vehicle): Window = vehicle.getWindow()

    @Provides
    @Singleton
    fun provideHvac(vehicle: Vehicle): HVAC = vehicle.getHVAC()

    @Provides
    @Singleton
    fun provideTts(@ApplicationContext context: Context): TextToSpeech =
        TextToSpeech(context = context, mode = Mode.ON_DEVICE)

    @Provides
    @Singleton
    fun provideStt(@ApplicationContext context: Context): SpeechToText =
        SpeechToText(context = context, mode = ai.pleos.playground.stt.constant.Mode.ON_DEVICE)
}
