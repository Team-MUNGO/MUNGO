package com.example.poco

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.example.poco.data.assistant.RestRecommendationCoordinator
import com.example.poco.data.record.LegacyTripImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class PocoApplication : Application() {
    @Inject lateinit var restRecommendations: RestRecommendationCoordinator
    @Inject lateinit var legacyTrips: LegacyTripImporter

    override fun onCreate() {
        super.onCreate()
        // 디버그 빌드에서만 로그 출력
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        // App-scoped so the automatic rest recommendation never depends on which screen is shown.
        restRecommendations.start()
        // 이전 주행 화면(poco.db)에 남은 이동 기록을 이동 기록 저장소로 한 번 옮긴다 (원본은 그대로 둔다)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { legacyTrips.importOnce() }
    }
}
