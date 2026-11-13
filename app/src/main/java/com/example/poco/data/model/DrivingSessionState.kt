package com.example.poco.data.model

import com.example.poco.core.driving.ComfortScoreState
import com.example.poco.core.driving.DrivingGear
import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.Guidance
import com.example.poco.core.driving.Reading
import com.example.poco.core.driving.RestIntervalPolicy
import com.example.poco.core.driving.RestPhase
import com.example.poco.core.driving.SpeedSample
import com.example.poco.core.driving.WindowCloseStatus
import com.example.poco.data.assistant.AssistResponse

data class RestUiState(
    val phase: RestPhase = RestPhase.DRIVING,
    val intervalMinutes: Int = RestIntervalPolicy.DEFAULT_MINUTES,
    val nextRestAtEpoch: Long? = null,
    val remainingMs: Long? = null,
    val restDue: Boolean = false,
    val restStartedAtEpoch: Long? = null,
    val currentRestMs: Long = 0,
    val restCount: Int = 0,
    val totalRestMs: Long = 0,
    val cycle: Int = 1,
    /** Comfort score when the current rest began (shown as "컴포트 81 → 83 회복 중"). */
    val scoreAtRestStart: Int? = null
)

sealed interface WaypointUiState {
    data object None : WaypointUiState
    data class Adding(val name: String) : WaypointUiState

    /** [etaText] only when Navi reported a remaining time whose unit could be verified. */
    data class Added(
        val name: String,
        val etaText: String?,
        val cancelling: Boolean = false,
        val cancelError: String? = null
    ) : WaypointUiState
    data class Failed(val name: String, val message: String) : WaypointUiState
    data class Cancelled(val name: String) : WaypointUiState
    data class Arrived(val name: String) : WaypointUiState
}

/** Place suggestion card (auto rest recommendation or a place answer). Places are Maps-verified. */
data class PlaceProposal(val eventId: String, val response: AssistResponse, val automatic: Boolean)

data class DrivingSessionState(
    val active: Boolean = false,
    val tripId: String? = null,
    val settings: DrivingSettings? = null,
    val startedAtElapsed: Long? = null,
    val startedAtEpoch: Long? = null,
    val elapsedMs: Long = 0,
    val comfort: ComfortScoreState = ComfortScoreState(),
    val temperature: Reading<Float> = Reading.Unknown,
    val speed: Reading<SpeedSample> = Reading.Unknown,
    val gear: Reading<DrivingGear> = Reading.Unknown,
    val windowPercent: Reading<Double> = Reading.Unknown,
    val canCloseWindow: Boolean = false,
    val windowWarning: Boolean = false,
    /** 실내 온도가 위험 기준 이상: "에어컨을 켜 주세요" 팝업 (닫거나 온도가 내려가면 false) */
    val temperatureDanger: Boolean = false,
    /** 주의(더움) 이상이 3분 넘게 이어졌다: 음성 카드에 휴식 제안을 띄워 둔다 (쾌적 · 추움이면 false) */
    val temperatureRestSuggested: Boolean = false,
    val closeStatus: WindowCloseStatus = WindowCloseStatus.IDLE,
    val closeError: String? = null,
    val ttsSpeaking: Boolean = false,
    val ttsError: String? = null,
    val naviStatus: String? = null,
    val bumpApproach: String? = null,
    /** Latest Gemini/guidance text for the always-visible Gleo card. */
    val gleoResponse: String? = null,
    val rest: RestUiState = RestUiState(),
    /** Highest-priority active guidance (one card at a time, stable layout slot). */
    val guidance: Guidance? = null,
    val voiceGuidance: Boolean = true,
    val proposal: PlaceProposal? = null,
    val waypoint: WaypointUiState = WaypointUiState.None,
    val aiRestStatus: String? = null,
    /** Set when a session ends so the summary screen can open the stored record. */
    val lastEndedTripId: String? = null,
    /** Emulator/VHAL simulation,
     labeled on screen and in records (never shown as a real vehicle). */
    val simulatedSource: Boolean = false
)
