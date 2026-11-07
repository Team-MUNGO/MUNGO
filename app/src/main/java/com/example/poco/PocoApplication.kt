package com.example.poco

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class PocoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 디버그 빌드에서만 로그 출력
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
