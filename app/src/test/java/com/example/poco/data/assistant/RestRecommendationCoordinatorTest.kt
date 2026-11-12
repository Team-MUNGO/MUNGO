package com.example.poco.data.assistant

import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.Guidance
import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.VerifiedPlace
import com.example.poco.data.model.DrivingSessionState
import com.example.poco.data.model.PlaceProposal
import com.example.poco.data.repository.AssistantRepository
import com.example.poco.data.repository.DrivingSessionRepository
import com.example.poco.data.repository.RestRecommendationDue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mock test of the automatic REST_RECOMMENDATION path (#23, #26). Real Gemini/Maps are not called. */
class RestRecommendationCoordinatorTest {
    private val state = MutableStateFlow(
        DrivingSessionState(active = true, tripId = "trip-1", settings = DrivingSettings("멍고", PetSeat.PASSENGER, 20f, 25f, 300))
    )
    private val requests = mutableListOf<AssistRequest>()
    private val proposals = mutableListOf<PlaceProposal>()
    private var status: String? = null
    private val place = AssistPlace("ChIJvSMioIapZDURijUJ9O6phq8", "덕평휴게소", 37.24, 127.39, null, null, true)
    private val assistant = object : AssistantRepository {
        override val configured = true
        override val connected = true
        override val anonymousAuth = true
        override var consented = false
        override suspend fun connect(invitation: String) = Unit
        override suspend fun assist(request: AssistRequest): AssistResponse {
            requests += request
            return AssistResponse(request.requestId, request.eventId, request.type, AssistStatus.OK, "덕평휴게소에서 쉬어요.", "덕평휴게소에서 쉬어요.", listOf(place), place.placeId, null, AssistSources("gemini", "google_maps"))
        }
        override fun disconnect() = Unit
    }
    private val session = object : DrivingSessionRepository {
        override val state = this@RestRecommendationCoordinatorTest.state
        override val restRecommendations = emptyFlow<RestRecommendationDue>()
        override fun start(settings: DrivingSettings) = Unit
        override fun end() = Unit
        override fun dismissWindowWarning() = Unit
        override fun closeWindow() = Unit
        override fun updateGleoResponse(text: String?) = Unit
        override fun startRest() = Unit
        override fun readyToDepart() = Unit
        override fun setRestInterval(minutes: Int) = Unit
        override fun setVoiceGuidance(enabled: Boolean) = Unit
        override fun announce(guidance: Guidance) = Unit
        override fun dismissGuidance(id: String) = Unit
        override fun showProposal(proposal: PlaceProposal) {
            proposals += proposal
        }
        override fun dismissProposal() = Unit
        override fun setAiRestStatus(text: String?) {
            status = text
        }
        override fun addWaypoint(place: VerifiedPlace) = Unit
        override fun cancelWaypoint() = Unit
    }
    private val coordinator = RestRecommendationCoordinator(session, assistant, object : AssistantLocationSource {
        override suspend fun current() = AssistantLocation(37.2, 127.3, 500)
    })
    private val due = RestRecommendationDue("trip-1", 1, "rest-trip-1-1", 19 * 60_000L)

    @Test fun withoutConsentNothingIsSent() = runBlocking {
        coordinator.handle(due)
        assertTrue(requests.isEmpty())
        assertTrue(status!!.contains("동의"))
    }

    @Test fun sendsMinimalContextWithLocationAndShowsAutomaticProposal() = runBlocking {
        assistant.consented = true
        coordinator.handle(due)
        val r = requests.single()
        assertEquals(AssistType.REST_RECOMMENDATION, r.type)
        assertEquals("rest-trip-1-1", r.eventId)
        assertNull(r.question)
        assertEquals("LARGE", r.context.petSize)
        assertEquals(19L, r.context.minutesToRest)
        assertNull(r.context.temperatureC) // only the status, and only when measured
        assertEquals(37.2, r.location!!.latitude, 0.0)
        assertTrue(proposals.single().automatic)
    }

    @Test fun staleTripIsIgnored() = runBlocking {
        assistant.consented = true
        coordinator.handle(due.copy(tripId = "old-trip"))
        assertTrue(requests.isEmpty())
    }
}
