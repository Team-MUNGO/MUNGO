package com.example.poco

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.example.poco.data.assistant.RestRecommendationCoordinator
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class PocoApplication : Application() {
    @Inject lateinit var restRecommendations: RestRecommendationCoordinator

    override fun onCreate() {
        super.onCreate()
        // 디버그 빌드에서만 로그 출력
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        // App-scoped so the automatic rest recommendation never depends on which screen is shown.
        restRecommendations.start()
    }
}
