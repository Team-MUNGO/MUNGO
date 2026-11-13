package com.example.poco.presentation.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.core.driving.RestIntervalPolicy
import com.example.poco.data.assistant.toSummaryRequest
import com.example.poco.data.record.MotionRecord
import com.example.poco.data.record.TripEntity
import com.example.poco.data.record.TripEventEntity
import com.example.poco.data.record.TripHistoryRepository
import com.example.poco.data.repository.AssistantRepository
import com.example.poco.data.repository.DrivingSessionRepository
import com.example.poco.data.settings.UserSettingsRepository
import com.example.poco.domain.motion.MotionAnalysis
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class TripHistoryViewModel @Inject constructor(history: TripHistoryRepository) : ViewModel() {
    val trips: StateFlow<List<TripEntity>?> = history.finishedTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** End screen,
detail,
events and motion screens all read the stored record through this ViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val history: TripHistoryRepository,
    settings: UserSettingsRepository,
    private val assistant: AssistantRepository
) : ViewModel() {
    private val id = MutableStateFlow<String?>(null)
    val trip: StateFlow<TripEntity?> = id.flatMapLatest { it?.let(history::trip) ?: emptyFlow() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val events: StateFlow<List<TripEventEntity>> = id.flatMapLatest {
        it?.let(history::events)
            ?: emptyFlow()
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allTrips: StateFlow<List<TripEntity>> = history.finishedTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val interval: StateFlow<Int> = settings.restIntervalMinutes
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            RestIntervalPolicy.DEFAULT_MINUTES
        )

    /**
     * Rule-based motion-sign analysis (insight + rest recommendation from one result). Recomputed
     * whenever the trip, any stored record or the rest interval changes, so edited entries apply at once.
     */
    val motionAnalysis: StateFlow<MotionAnalysis?> = combine(trip, allTrips, interval) {
            t,
            all,
            minutes
        ->
        t?.let { motionAnalysis(all.ifEmpty { listOf(it) }, it, minutes) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** AI 요청에 실패한 기록 (이번 실행 동안은 규칙 문장으로 보여 주고 다시 묻지 않는다) */
    private val aiFailed = MutableStateFlow<Set<String>>(emptySet())
    private val aiRequested = mutableSetOf<String>()

    /**
     * 주행 완료 "오늘 한 줄": 저장된 AI 문장 → (요청 중) 정리 중 안내 → AI를 못 쓰면 규칙 문장.
     * 한 기록에 한 번만 묻고, 받은 문장은 기록에 저장해 다시 열어도 같은 문장을 보여 준다.
     */
    val oneLine: StateFlow<OneLineUi?> = combine(trip, events, aiFailed) { t, ev, failed ->
        when {
            t == null || t.finalScore == null -> null
            t.aiOneLine != null -> OneLineUi(t.aiOneLine, fromAi = true)
            !aiAvailable() || t.id in failed -> OneLineUi(oneLine(t, ev))
            else -> OneLineUi(ONE_LINE_LOADING, loading = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun aiAvailable() = assistant.configured && assistant.consented

    fun load(tripId: String) {
        id.value = tripId
        requestOneLine(tripId)
    }

    private fun requestOneLine(tripId: String) {
        if (!aiAvailable() || !aiRequested.add(tripId)) return
        viewModelScope.launch {
            // 기록이 완료(점수 저장)될 때까지 기다렸다가 한 번만 요청
            val done = history.trip(tripId).filterNotNull().first {
                it.finalScore != null &&
                    it.endedAtEpoch != null
            }
            if (done.aiOneLine != null) return@launch
            val request = done.toSummaryRequest(
                history.events(tripId).first(),
                previousTrip(history.finishedTrips().first(), done)
            ) ?: return@launch
            runCatching { assistant.summarizeTrip(request) }
                .onSuccess { text -> history.setOneLine(tripId, text) }
                .onFailure {
                    Timber.w(it, "오늘 한 줄 AI 실패 - 규칙 문장으로 대신")
                    aiFailed.update { failed -> failed + tripId }
                }
        }
    }

    fun setMotion(record: MotionRecord) {
        val tripId = id.value ?: return
        viewModelScope.launch { history.setMotionSickness(tripId, record) }
    }
}

data class OneLineUi(val text: String, val fromAi: Boolean = false, val loading: Boolean = false)

private const val ONE_LINE_LOADING = "오늘 기록을 정리하고 있어요…"

@HiltViewModel
class RestSettingsViewModel @Inject constructor(
    settings: UserSettingsRepository,
    private val session: DrivingSessionRepository
) : ViewModel() {
    val interval: StateFlow<Int> = settings.restIntervalMinutes
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            RestIntervalPolicy.DEFAULT_MINUTES
        )
    val voice: StateFlow<Boolean> = settings.voiceGuidanceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Persisted; a running session applies it immediately while keeping its rest base time. */
    fun setInterval(minutes: Int) = session.setRestInterval(minutes)
    fun setVoice(enabled: Boolean) = session.setVoiceGuidance(enabled)
}
