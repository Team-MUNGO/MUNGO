package com.example.poco.data.assistant

import com.example.poco.core.driving.temperatureStatus
import com.example.poco.core.driving.valueOrNull
import com.example.poco.data.model.DrivingSessionState
import com.example.poco.data.model.PlaceProposal
import com.example.poco.data.repository.AssistantRepository
import com.example.poco.data.repository.DrivingSessionRepository
import com.example.poco.data.repository.RestRecommendationDue
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process-level owner of the automatic REST_RECOMMENDATION request. The session emits one due
 * signal per rest cycle (de-duplicated by cycle and persisted with a unique key in the trip), so
 * screen switches, recomposition or ViewModel re-creation can never call Gemini/Maps twice.
 */
@Singleton
class RestRecommendationCoordinator @Inject constructor(
    private val session: DrivingSessionRepository,
    private val assistant: AssistantRepository,
    private val location: AssistantLocationSource
) {
    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch { session.restRecommendations.collect { due -> launch { handle(due) } } }
    }

    internal suspend fun handle(due: RestRecommendationDue) {
        val snapshot = session.state.value
        if (!snapshot.active || snapshot.tripId != due.tripId) return
        if (!assistant.configured || !assistant.consented) {
            session.setAiRestStatus("휴식 20분 전이에요. AI 맞춤 제안은 정보 전송 동의 후 사용할 수 있어요.")
            return
        }
        session.setAiRestStatus("주변 휴식 장소를 찾고 있어요.")
        try {
            val here = location.current()
            val request = AssistRequest(
                type = AssistType.REST_RECOMMENDATION,
                requestId = UUID.randomUUID().toString().take(32),
                eventId = due.eventId,
                context = restContext(snapshot, due),
                location = here
            )
            val response = assistant.assist(request)
            if (session.state.value.tripId != due.tripId) return
            when (response.status) {
                AssistStatus.OK -> session.showProposal(PlaceProposal(due.eventId, response, automatic = true))
                else -> session.setAiRestStatus(response.displayText)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // No retry within the same cycle: failure is shown, the waypoint button stays disabled.
            session.setAiRestStatus(e.message ?: "맞춤 휴식 제안을 받지 못했어요.")
        }
    }

    companion object {
        /** Minimal data for a rest recommendation: pet size, rest timing, score, temperature status. */
        fun restContext(s: DrivingSessionState, due: RestRecommendationDue) = AssistContext(
            comfortScore = s.comfort.score,
            minutesToRest = due.remainingMs / 60_000,
            restIntervalMinutes = s.rest.intervalMinutes,
            temperatureStatus = s.settings?.let { temperatureStatus(s.temperature.valueOrNull(), it).name },
            petSize = s.settings?.petSize?.name
        )
    }
}
