package com.example.poco.presentation.driving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.core.driving.DrivingGear
import com.example.poco.core.driving.Guidance
import com.example.poco.core.driving.GuidancePriority
import com.example.poco.core.driving.temperatureStatus
import com.example.poco.core.driving.valueOrNull
import com.example.poco.data.assistant.AssistContext
import com.example.poco.data.assistant.AssistRequest
import com.example.poco.data.assistant.AssistResponse
import com.example.poco.data.assistant.AssistStatus
import com.example.poco.data.assistant.AssistType
import com.example.poco.data.assistant.AssistantLocationSource
import com.example.poco.data.assistant.toVerifiedPlace
import com.example.poco.data.model.DrivingSessionState
import com.example.poco.data.model.PlaceProposal
import com.example.poco.data.repository.AssistantRepository
import com.example.poco.data.repository.DrivingSessionRepository
import com.example.poco.data.repository.SttRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AssistantPhase { IDLE, CONNECTING, LISTENING, THINKING }
data class AssistantUiState(
    val phase: AssistantPhase = AssistantPhase.IDLE,
    val configured: Boolean = false,
    val connected: Boolean = false,
    val anonymousAuth: Boolean = false,
    val consented: Boolean = false,
    val question: String = "",
    val error: String? = null,
    val needsLocationPermission: Boolean = false
)

/**
 * USER_QUESTION flow: Pleos STT -> /assist (Gemini decides intent) -> if a place search is needed the
 * server answers LOCATION_REQUIRED and only then is the location permission/fix requested -> /assist
 * again with the location -> answer card / place proposal through the session's guidance ledger (TTS).
 */
@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val assistant: AssistantRepository,
    private val stt: SttRepository,
    private val location: AssistantLocationSource,
    private val session: DrivingSessionRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(snapshotState())
    val state = mutableState.asStateFlow()
    private var work: Job? = null
    private var pendingSearch: Pair<String, String>? = null

    private fun snapshotState(base: AssistantUiState = AssistantUiState()) = base.copy(
        configured = assistant.configured,
        connected = assistant.connected,
        anonymousAuth = assistant.anonymousAuth,
        consented = assistant.consented
    )

    init {
        viewModelScope.launch {
            var wasActive = session.state.value.active
            session.state.collect { vehicle ->
                if (wasActive && !vehicle.active) {
                    work?.cancel()
                    pendingSearch = null
                    mutableState.value = snapshotState()
                } else if (vehicle.windowWarning) {
                    // Window safety outranks every AI interaction.
                    work?.cancel()
                    pendingSearch = null
                    mutableState.update { it.copy(phase = AssistantPhase.IDLE, needsLocationPermission = false) }
                }
                wasActive = vehicle.active
            }
        }
    }

    fun consent() {
        assistant.consented = true
        mutableState.update { snapshotState(it).copy(error = null) }
    }

    fun connect(code: String) {
        if (session.state.value.gear.valueOrNull() != DrivingGear.PARK) {
            mutableState.update { it.copy(error = "P단 정차 상태에서 연결해 주세요.") }
            return
        }
        launchWork(AssistantPhase.CONNECTING) {
            assistant.connect(code.trim())
            mutableState.update { snapshotState(it) }
        }
    }

    fun ask() {
        val vehicle = session.state.value
        if (!vehicle.active || vehicle.windowWarning || vehicle.ttsSpeaking) return
        if (!assistant.configured || !assistant.consented || (!assistant.connected && !assistant.anonymousAuth)) {
            mutableState.update {
                snapshotState(it).copy(
                    error = when {
                        !assistant.configured -> "AI 서버 연결이 아직 설정되지 않았어요."
                        assistant.anonymousAuth -> "정차 후 AI 연결에서 정보 전송에 동의해 주세요."
                        else -> "정차 후 AI 연결과 정보 전송 동의를 설정해 주세요."
                    }
                )
            }
            return
        }
        launchWork(AssistantPhase.LISTENING) {
            val question = stt.recognize { partial -> mutableState.update { it.copy(question = partial) } }
            mutableState.update { it.copy(question = question, phase = AssistantPhase.THINKING) }
            val response = request(question, null, null)
            if (response.status == AssistStatus.LOCATION_REQUIRED) {
                pendingSearch = question to response.searchQuery.orEmpty()
                mutableState.update { it.copy(needsLocationPermission = true) }
            } else {
                present(response)
            }
        }
    }

    fun locationPermission(granted: Boolean) {
        val (question, searchQuery) = pendingSearch ?: return
        pendingSearch = null
        mutableState.update { it.copy(needsLocationPermission = false) }
        if (!granted) {
            mutableState.update { it.copy(error = "위치 권한이 없어 주변 장소를 찾을 수 없어요.") }
            return
        }
        launchWork(AssistantPhase.THINKING) {
            val here = location.current()
            val response = request(question, searchQuery, here)
            present(response)
        }
    }

    private suspend fun request(
        question: String,
        searchQuery: String?,
        here: com.example.poco.data.assistant.AssistantLocation?
    ): AssistResponse {
        val vehicle = session.state.value
        check(vehicle.active && !vehicle.windowWarning)
        return assistant.assist(
            AssistRequest(
                type = AssistType.USER_QUESTION,
                requestId = UUID.randomUUID().toString().take(32),
                question = question,
                searchQuery = searchQuery?.takeIf { it.isNotBlank() },
                context = questionContext(vehicle),
                location = here
            )
        )
    }

    private fun present(response: AssistResponse) {
        if (!session.state.value.active) return
        if (response.places.isNotEmpty()) {
            session.showProposal(PlaceProposal(response.requestId, response, automatic = false))
        } else {
            session.announce(
                Guidance(response.requestId, GuidancePriority.AI_ANSWER, response.displayText, response.ttsText)
            )
        }
    }

    private fun launchWork(phase: AssistantPhase, block: suspend () -> Unit) {
        val previous = work
        previous?.cancel()
        work = viewModelScope.launch {
            previous?.cancelAndJoin()
            mutableState.update {
                it.copy(phase = phase, error = null, question = if (phase == AssistantPhase.LISTENING) "" else it.question)
            }
            try {
                block()
            } catch (_: TimeoutCancellationException) {
                mutableState.update { it.copy(error = "응답 시간이 초과되었어요. 결과를 확인한 후 다시 시도해 주세요.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.update { snapshotState(it).copy(error = e.message ?: "잠시 후 다시 시도해 주세요.") }
            } finally {
                mutableState.update { it.copy(phase = AssistantPhase.IDLE) }
            }
        }
    }

    fun cancel() {
        work?.cancel()
        pendingSearch = null
        mutableState.update { it.copy(phase = AssistantPhase.IDLE, needsLocationPermission = false) }
    }

    /** Only the Maps-verified recommended place can be sent to NaviHelper. */
    fun addWaypoint() {
        val proposal = session.state.value.proposal ?: return
        val place = (proposal.response.recommended ?: proposal.response.places.firstOrNull())
            ?.toVerifiedPlace(proposal.response) ?: return
        val vehicle = session.state.value
        if (!vehicle.active || vehicle.windowWarning) return
        session.addWaypoint(place)
    }

    fun dismissProposal() = session.dismissProposal()

    fun disconnect() {
        cancel()
        assistant.disconnect()
        assistant.consented = false
        mutableState.value = snapshotState()
        session.updateGleoResponse(null)
    }

    companion object {
        /** Small measured context only; location is never part of a general question. */
        fun questionContext(s: DrivingSessionState): AssistContext {
            val temp = s.temperature.valueOrNull()
            return AssistContext(
                comfortScore = s.comfort.score,
                elapsedMinutes = s.elapsedMs / 60_000,
                minutesToRest = s.rest.remainingMs?.let { it / 60_000 },
                restIntervalMinutes = s.rest.intervalMinutes,
                temperatureC = temp,
                minTemperatureC = s.settings?.minTemperatureC,
                maxTemperatureC = s.settings?.maxTemperatureC,
                temperatureStatus = s.settings?.let { temperatureStatus(temp, it).name },
                windowPercent = s.windowPercent.valueOrNull(),
                petSize = s.settings?.petSize?.name,
                petWeightKg = s.settings?.weightTenths?.let { it / 10.0 }
            )
        }
    }
}
