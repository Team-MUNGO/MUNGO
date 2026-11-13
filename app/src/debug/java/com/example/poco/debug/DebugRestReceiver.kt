package com.example.poco.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.poco.data.assistant.RestRecommendationCoordinator
import com.example.poco.data.repository.DrivingSessionRepository
import com.example.poco.data.repository.RestRecommendationDue
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 디버그 빌드 전용: 휴식 20분 전 자동 제안을 지금 바로 한 번 보낸다 (에뮬레이터에서 2시간을 기다리지 않게).
 * 실제와 같은 경로(코디네이터 → 서버 /assist → 제안 카드)를 탄다.
 * `adb shell am broadcast --user 10 -p com.example.poco -a com.example.poco.DEBUG_REST`
 */
class DebugRestReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun session(): DrivingSessionRepository
        fun coordinator(): RestRecommendationCoordinator
    }

    override fun onReceive(context: Context, intent: Intent) {
        val deps = EntryPointAccessors.fromApplication(context, Deps::class.java)
        val state = deps.session().state.value
        val tripId = state.tripId
        if (!state.active || tripId == null) {
            Timber.w("DEBUG_REST 주행 중이 아니라 보내지 않음")
            return
        }
        val now = System.currentTimeMillis()
        val due = RestRecommendationDue(
            tripId = tripId,
            cycle = DEBUG_CYCLE,
            eventId = "rest-debug-${now % 1_000_000_000}",
            remainingMs = 20 * 60_000L
        )
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            try {
                deps.coordinator().handle(due)
                val after = deps.session().state.value
                Timber.i(
                    "DEBUG_REST 끝: %s 제안=%s 상태=%s 쉼터제안설정=%s",
                    due.eventId,
                    after.proposal != null,
                    after.aiRestStatus,
                    after.settings?.restSuggestions
                )
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val DEBUG_CYCLE = 99
    }
}
