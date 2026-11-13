package com.example.poco.data.repository

import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.Guidance
import com.example.poco.core.driving.VerifiedPlace
import com.example.poco.data.model.DrivingSessionState
import com.example.poco.data.model.PlaceProposal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Emitted once per rest cycle when <= 20 minutes remain (app-side timer; server keeps none). */
data class RestRecommendationDue(
    val tripId: String,
    val cycle: Int,
    val eventId: String,
    val remainingMs: Long
)

interface DrivingSessionRepository {
    val state: StateFlow<DrivingSessionState>
    val restRecommendations: Flow<RestRecommendationDue>
    fun start(settings: DrivingSettings)
    fun end()
    fun dismissWindowWarning()

    /** 온도 위험 팝업 닫기 (온도가 다시 위험해지면 또 뜬다) */
    fun dismissTemperatureDanger() {}
    fun closeWindow()
    fun updateGleoResponse(text: String?)
    fun startRest()
    fun readyToDepart()
    fun setRestInterval(minutes: Int)
    fun setVoiceGuidance(enabled: Boolean)
    fun announce(guidance: Guidance)
    fun dismissGuidance(id: String)
    fun showProposal(proposal: PlaceProposal)
    fun dismissProposal()
    fun setAiRestStatus(text: String?)
    fun addWaypoint(place: VerifiedPlace)

    /** Removes the confirmed waypoint through Navi; success only after Navi no longer reports it. */
    fun cancelWaypoint()
}
