package com.example.poco.presentation.session

import com.example.poco.core.driving.DrivingGear
import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.Guidance
import com.example.poco.core.driving.GuidancePriority
import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.Reading
import com.example.poco.core.driving.VerifiedPlace
import com.example.poco.data.assistant.AssistPlace
import com.example.poco.data.assistant.AssistRequest
import com.example.poco.data.assistant.AssistResponse
import com.example.poco.data.assistant.AssistSources
import com.example.poco.data.assistant.AssistStatus
import com.example.poco.data.assistant.AssistType
import com.example.poco.data.assistant.AssistantLocation
import com.example.poco.data.assistant.AssistantLocationSource
import com.example.poco.data.model.DrivingSessionState
import com.example.poco.data.model.NaviEvent
import com.example.poco.data.model.PlaceProposal
import com.example.poco.data.pleos.datasource.BumpApproach
import com.example.poco.data.pleos.datasource.NaviRouteUpdate
import com.example.poco.data.repository.AssistantRepository
import com.example.poco.data.repository.DrivingSessionRepository
import com.example.poco.data.repository.NaviRepository
import com.example.poco.data.repository.RestRecommendationDue
import com.example.poco.data.repository.SttRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Mock-based: the real Gemini/Maps behaviour is covered by server tests and live checks. */
@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val vehicle = MutableStateFlow(
        DrivingSessionState(
            active = true,
            gear = Reading.Value(DrivingGear.PARK),
            settings = DrivingSettings("멍고", PetSeat.REAR_LEFT, 20f, 25f, 123)
        )
    )
    private var sttCalls = 0
    private var locations = 0
    private val requests = mutableListOf<AssistRequest>()
    private val announced = mutableListOf<Guidance>()
    private val proposals = mutableListOf<PlaceProposal>()
    private val waypoints = mutableListOf<VerifiedPlace>()
    private var reply: (AssistRequest) -> AssistResponse = { answer(it) }
    private lateinit var vm: AssistantViewModel

    private fun answer(r: AssistRequest) = AssistResponse(
        r.requestId,
        r.eventId,
        r.type,
        AssistStatus.OK,
        "현재 온도는 확인할 수 없어요.",
        "현재 온도는 확인할 수 없어요."
    )

    private val place =
        AssistPlace("ChIJvSMioIapZDURijUJ9O6phq8", "덕평휴게소", 37.24, 127.39, null, null, true)

    private val remote = object : AssistantRepository {
        override var configured = true
        override var connected = true
        override var anonymousAuth = false
        override var consented = false
        override suspend fun connect(invitation: String) {
            connected = true
        }
        override suspend fun assist(request: AssistRequest): AssistResponse {
            requests += request
            return reply(request)
        }
        override fun disconnect() {
            connected = false
        }
    }

    private val session = object : DrivingSessionRepository {
        override val state = vehicle
        override val restRecommendations = emptyFlow<RestRecommendationDue>()
        override fun start(settings: DrivingSettings) = Unit
        override fun end() {
            vehicle.value = DrivingSessionState()
        }
        override fun dismissWindowWarning() = Unit
        override fun closeWindow() = Unit
        override fun updateGleoResponse(text: String?) = Unit
        override fun startRest() = Unit
        override fun readyToDepart() = Unit
        override fun setRestInterval(minutes: Int) = Unit
        override fun setVoiceGuidance(enabled: Boolean) = Unit
        override fun announce(guidance: Guidance) {
            announced += guidance
        }
        override fun dismissGuidance(id: String) = Unit
        override fun showProposal(proposal: PlaceProposal) {
            proposals += proposal
            vehicle.value = vehicle.value.copy(proposal = proposal)
        }
        override fun dismissProposal() {
            vehicle.value = vehicle.value.copy(proposal = null)
        }
        override fun setAiRestStatus(text: String?) = Unit
        override fun addWaypoint(place: VerifiedPlace) {
            waypoints += place
        }
        override fun cancelWaypoint() = Unit
    }

    private val navi = kotlinx.coroutines.flow.MutableSharedFlow<NaviEvent>()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = AssistantViewModel(
            remote,
            object : SttRepository {
                override suspend fun listenOnce(timeoutMs: Long) = Result.success<String?>(null)
                override suspend fun recognize(onPartial: (String) -> Unit): String {
                    sttCalls++
                    onPartial("중간")
                    return "근처에 강아지랑 쉴 데 있어?"
                }
            },
            object : AssistantLocationSource {
                override suspend fun current(): AssistantLocation {
                    locations++
                    return AssistantLocation(37.0, 127.0, 0)
                }
            },
            session,
            object : NaviRepository {
                override fun observeEvents() = navi
                override suspend fun openNavigation(keyword: String?) = Result.success(Unit)
                override fun observeBumps() = emptyFlow<Reading<List<BumpApproach>>>()
                override fun observeRoute() = emptyFlow<NaviRouteUpdate>()
            },
            com.example.poco.data.assistant.RestRecommendationCoordinator(
                session,
                remote,
                object : AssistantLocationSource {
                    override suspend fun current() = AssistantLocation(37.0, 127.0, 0)
                }
            )
        )
        scheduler.runCurrent()
    }

    @After fun teardown() {
        vm.cancel()
        scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test fun noConsentOrServerDoesNotRecordOrTransmit() {
        vm.ask()
        scheduler.runCurrent()
        assertEquals(0, sttCalls)
        vm.consent()
        remote.configured = false
        vm.ask()
        scheduler.runCurrent()
        assertEquals(0, sttCalls)
        assertTrue(requests.isEmpty())
    }

    @Test fun anonymousAuthAsksWithoutCodeOnlyAfterConsent() {
        remote.connected = false
        remote.anonymousAuth = true
        vm.ask()
        scheduler.runCurrent()
        assertEquals(0, sttCalls)
        vm.consent()
        vm.ask()
        scheduler.runCurrent()
        assertEquals(1, requests.size)
    }

    /** #18/#20: a general answer goes to the ledger once as AI_ANSWER; no location is ever sent. */
    @Test fun generalAnswerIsAnnouncedWithoutLocation() {
        vm.consent()
        vm.ask()
        scheduler.runCurrent()
        assertEquals(1, requests.size)
        val r = requests.single()
        assertEquals(AssistType.USER_QUESTION, r.type)
        assertNull(r.location)
        assertEquals(0, locations)
        assertEquals("MEDIUM", r.context.petSize)
        assertEquals(GuidancePriority.AI_ANSWER, announced.single().priority)
        assertEquals(r.requestId, announced.single().id)
    }

    /** #19: intent comes from the server (Gemini); the app only reacts to LOCATION_REQUIRED. */
    @Test fun placeIntentRequestsLocationThenSendsOnlyThenAndShowsVerifiedPlaces() {
        reply = { r ->
            if (r.location == null) {
                AssistResponse(
                    r.requestId,
                    null,
                    r.type,
                    AssistStatus.LOCATION_REQUIRED,
                    "위치가 필요해요.",
                    "위치가 필요해요.",
                    searchQuery = "반려견 동반 휴게소"
                )
            } else {
                AssistResponse(
                    r.requestId, null, r.type, AssistStatus.OK, "덕평휴게소가 좋아요.", "덕평휴게소가 좋아요.",
                    listOf(
                        place
                    ),
                    place.placeId, "반려견 동반 휴게소", AssistSources("gemini", "google_maps")
                )
            }
        }
        vm.consent()
        vm.ask()
        scheduler.runCurrent()
        assertTrue(vm.state.value.needsLocationPermission)
        assertEquals(0, locations)
        vm.locationPermission(false)
        scheduler.runCurrent()
        assertEquals(1, requests.size)
        vm.ask()
        scheduler.runCurrent()
        vm.locationPermission(true)
        scheduler.runCurrent()
        assertEquals(1, locations)
        assertEquals(3, requests.size)
        assertEquals("반려견 동반 휴게소", requests.last().searchQuery)
        assertNotNull(requests.last().location)
        assertEquals(place.placeId, proposals.single().response.recommendedPlaceId)
        assertFalse(proposals.single().automatic)
    }

    /** #27: only Maps-verified coordinates reach the waypoint call. */
    @Test fun addWaypointUsesVerifiedPlaceOnly() {
        val unverified =
            AssistResponse(
                "a", null, AssistType.USER_QUESTION, AssistStatus.OK, "t", "t",
                listOf(
                    place.copy(canAddWaypoint = false)
                ),
                place.placeId, null, AssistSources("gemini", "google_maps")
            )
        vehicle.value = vehicle.value.copy(proposal = PlaceProposal("a", unverified, false))
        vm.addWaypoint()
        assertTrue(waypoints.isEmpty())
        val verified = unverified.copy(places = listOf(place))
        vehicle.value = vehicle.value.copy(proposal = PlaceProposal("a", verified, false))
        vm.addWaypoint()
        assertEquals(VerifiedPlace(place.placeId, "덕평휴게소", 37.24, 127.39, null), waypoints.single())
        vehicle.value = vehicle.value.copy(windowWarning = true)
        vm.addWaypoint()
        assertEquals(1, waypoints.size)
    }

    @Test fun windowWarningPreventsNewRecognition() {
        vm.consent()
        vehicle.value = vehicle.value.copy(windowWarning = true)
        scheduler.runCurrent()
        vm.ask()
        scheduler.runCurrent()
        assertEquals(0, sttCalls)
    }

    @Test fun movingCannotConnect() {
        remote.connected = false
        vehicle.value = vehicle.value.copy(gear = Reading.Value(DrivingGear.DRIVE))
        vm.connect("fixture")
        scheduler.runCurrent()
        assertFalse(remote.connected)
    }
}
