package com.example.poco.presentation.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.RestIntervalPolicy
import com.example.poco.data.record.MotionRecord
import com.example.poco.data.record.TripEntity
import com.example.poco.data.record.TripEventEntity
import com.example.poco.data.record.TripHistoryRepository
import com.example.poco.data.repository.DrivingSessionRepository
import com.example.poco.data.settings.UserSettingsRepository
import com.example.poco.domain.motion.MotionAnalysis
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TripHistoryViewModel @Inject constructor(history: TripHistoryRepository) : ViewModel() {
    val trips: StateFlow<List<TripEntity>?> = history.finishedTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** End screen, detail, events and motion screens all read the stored record through this ViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val history: TripHistoryRepository,
    settings: UserSettingsRepository
) : ViewModel() {
    private val id = MutableStateFlow<String?>(null)
    val trip: StateFlow<TripEntity?> = id.flatMapLatest { it?.let(history::trip) ?: emptyFlow() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val events: StateFlow<List<TripEventEntity>> = id.flatMapLatest { it?.let(history::events) ?: emptyFlow() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allTrips: StateFlow<List<TripEntity>> = history.finishedTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val interval: StateFlow<Int> = settings.restIntervalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RestIntervalPolicy.DEFAULT_MINUTES)

    /**
     * Rule-based motion-sign analysis (insight + rest recommendation from one result). Recomputed
     * whenever the trip, any stored record or the rest interval changes, so edited entries apply at once.
     */
    val motionAnalysis: StateFlow<MotionAnalysis?> = combine(trip, allTrips, interval) { t, all, minutes ->
        t?.let { motionAnalysis(all.ifEmpty { listOf(it) }, it, minutes) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun load(tripId: String) {
        id.value = tripId
    }

    fun setMotion(record: MotionRecord) {
        val tripId = id.value ?: return
        viewModelScope.launch { history.setMotionSickness(tripId, record) }
    }
}

@HiltViewModel
class RestSettingsViewModel @Inject constructor(
    settings: UserSettingsRepository,
    private val session: DrivingSessionRepository
) : ViewModel() {
    val interval: StateFlow<Int> = settings.restIntervalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RestIntervalPolicy.DEFAULT_MINUTES)
    val voice: StateFlow<Boolean> = settings.voiceGuidanceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Persisted; a running session applies it immediately while keeping its rest base time. */
    fun setInterval(minutes: Int) = session.setRestInterval(minutes)
    fun setVoice(enabled: Boolean) = session.setVoiceGuidance(enabled)
}
