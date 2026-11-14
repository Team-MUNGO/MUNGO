package com.example.poco.data.repositoryimpl

import com.example.poco.core.driving.ComfortScoreConfig
import com.example.poco.core.driving.DrivingClock
import com.example.poco.core.driving.DrivingGear
import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.NaviWaypoint
import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.Reading
import com.example.poco.core.driving.SpeedSample
import com.example.poco.core.driving.VerifiedPlace
import com.example.poco.core.driving.WindowCloseStatus
import com.example.poco.core.driving.valueOrNull
import com.example.poco.data.assistant.AssistantWaypointSource
import com.example.poco.data.assistant.LocationFix
import com.example.poco.data.assistant.LocationStream
import com.example.poco.data.automotive.VehicleUpdate
import com.example.poco.data.model.CarWindow
import com.example.poco.data.model.WaypointUiState
import com.example.poco.data.model.WindowPositionRange
import com.example.poco.data.pleos.datasource.BumpApproach
import com.example.poco.data.pleos.datasource.NaviRouteUpdate
import com.example.poco.data.repository.HvacRepository
import com.example.poco.data.repository.NaviRepository
import com.example.poco.data.repository.TtsRepository
import com.example.poco.data.repository.VehicleRepository
import com.example.poco.data.repository.WindowRepository
import com.example.poco.data.settings.UserSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalCoroutinesApi::class)
class DrivingSessionRepositoryTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val clock = DrivingClock { scheduler.currentTime }
    private val positions = MutableStateFlow<Int?>(0)
    private val updates = MutableSharedFlow<VehicleUpdate>(extraBufferCapacity = 8)
    private val bumps = MutableStateFlow<Reading<List<BumpApproach>>>(Reading.Value(emptyList()))
    private var spoken = 0
    private var ttsFails = false
    private var commandFails = false
    private var physicallyClose = false
    private var readSupported = true
    private var commandCalls = 0
    private val spokenTexts = mutableListOf<String>()
    private var hangTts = false
    private val interval = MutableStateFlow(120)
    private val voice = MutableStateFlow(true)
    private val locationFixes = MutableSharedFlow<LocationFix>(extraBufferCapacity = 8)
    private val route = MutableSharedFlow<NaviRouteUpdate>(extraBufferCapacity = 8)
    private val places =
        MutableSharedFlow<com.example.poco.data.pleos.datasource.NaviPlaceHint>(
            extraBufferCapacity = 8
        )
    private var waypointCalls = 0
    private var waypointFailure: String? = null
    private var removeCalls = 0
    private var removeFailure: String? = null
    private val recorder = FakeRecorder()
    private val settings = DrivingSettings("현댕이", PetSeat.REAR_RIGHT, 20f, 25f)
    private lateinit var session: DrivingSessionRepositoryImpl
    private val steering = kotlinx.coroutines.flow.MutableSharedFlow<Float>(extraBufferCapacity = 8)

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        session = DrivingSessionRepositoryImpl(
            object : VehicleRepository {
                override fun observe() = updates
            },
            object : HvacRepository {
                override fun observeInteriorTemperature(seat: PetSeat) =
                    flowOf<Reading<Float>>(Reading.Unavailable("온도 없음"))
                override suspend fun getSeatTemperature(seat: com.example.poco.data.model.PetSeat) =
                    Result.success<Float?>(null)
                override fun observeSeatTemperature(seat: com.example.poco.data.model.PetSeat) =
                    kotlinx.coroutines.flow.emptyFlow<Float>()
                override suspend fun setSeatTemperature(
                    seat: com.example.poco.data.model.PetSeat,
                    celsius: Float
                ) = Result.success<Float?>(null)
                override fun observeAirCleaner() =
                    kotlinx.coroutines.flow.emptyFlow<Reading<Boolean>>()
                override suspend fun setAirCleaner(on: Boolean) = Result.success<Boolean?>(on)
            },
            object : WindowRepository {
                override suspend fun getSupportedWindows() = Result.success(
                    mapOf(
                        CarWindow.REAR_RIGHT to WindowPositionRange(0, 200, readSupported, true)
                    )
                )
                override suspend fun getWindowPosition(window: CarWindow) =
                    Result.success(positions.value)
                override suspend fun setWindowPosition(
                    window: CarWindow,
                    position: Int
                ): Result<Int?> {
                    commandCalls++
                    assertEquals(CarWindow.REAR_RIGHT, window)
                    assertEquals(0, position)
                    if (commandFails) return Result.failure(IllegalStateException("제어 미지원"))
                    if (physicallyClose) positions.value = 0
                    return Result.success(0) // Intentionally claims zero without physical movement.
                }
                override fun observeWindowPosition(window: CarWindow) = positions
            },
            object : NaviRepository {
                override fun observeEvents() =
                    kotlinx.coroutines.flow.emptyFlow<com.example.poco.data.model.NaviEvent>()
                override suspend fun openNavigation(keyword: String?) = Result.success(Unit)
                override fun observeBumps() = bumps
                override fun observeRoute() = route
                override fun observePlace() = places
            },
            object : TtsRepository {
                override suspend fun speak(text: String) = Result.success(Unit)
                override suspend fun speak(text: String, onSpeaking: (Boolean) -> Unit) {
                    spoken++
                    spokenTexts += text
                    if (ttsFails) error("TTS unavailable")
                    if (hangTts) {
                        onSpeaking(true)
                        try {
                            kotlinx.coroutines.awaitCancellation()
                        } finally {
                            onSpeaking(false)
                        }
                    }
                }
            },
            clock,
            ComfortScoreConfig(closeTimeoutMs = 1000),
            object : UserSettingsRepository {
                override val restIntervalMinutes = interval
                override val voiceGuidanceEnabled = voice
                override suspend fun setRestIntervalMinutes(minutes: Int) {
                    interval.value = minutes
                }
                override suspend fun setVoiceGuidanceEnabled(enabled: Boolean) {
                    voice.value = enabled
                }
            },
            recorder,
            object : AssistantWaypointSource {
                override suspend fun add(place: VerifiedPlace): NaviWaypoint {
                    waypointCalls++
                    waypointFailure?.let { throw IllegalStateException(it) }
                    return NaviWaypoint(
                        place.name,
                        place.latitude + 0.0005,
                        place.longitude,
                        900,
                        15_000
                    )
                }
                override suspend fun remove(place: VerifiedPlace) {
                    removeCalls++
                    removeFailure?.let { throw IllegalStateException(it) }
                }
            },
            object : LocationStream {
                override fun observe() = locationFixes
            },
            DataOrigin { true },
            object : com.example.poco.data.repository.VehicleMotionRepository {
                override fun observeSpeedKmh() = kotlinx.coroutines.flow.emptyFlow<Float>()
                override fun observeSteeringAngle() = steering
                override fun observeBrakePedalPressed() =
                    kotlinx.coroutines.flow.emptyFlow<Boolean>()
                override fun observeLocation() =
                    kotlinx.coroutines.flow.emptyFlow<com.example.poco.data.model.VehicleLocation>()
            }
        )
    }

    @After fun tearDown() {
        session.end()
        scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test fun windowRepeatedCallbacksSpeakOnceAndNewEpisodeSpeaksAgain() {
        positions.value = 50
        session.start(settings)
        scheduler.runCurrent()
        assertTrue(session.state.value.windowWarning)
        assertEquals(1, spoken)
        positions.value = 60
        scheduler.runCurrent()
        assertEquals(1, spoken)
        session.dismissWindowWarning()
        positions.value = 70
        scheduler.runCurrent()
        assertFalse(session.state.value.windowWarning)
        positions.value = 0
        scheduler.runCurrent()
        positions.value = 80
        scheduler.runCurrent()
        assertEquals(2, spoken)
        assertEquals(100, session.state.value.comfort.score)
    }

    @Test fun exactly20PercentDoesNotWarn() {
        positions.value = 40
        session.start(settings)
        scheduler.runCurrent()
        assertFalse(session.state.value.windowWarning)
        assertEquals(0, spoken)
    }

    @Test fun commandAcknowledgementWithoutMovementTimesOut() {
        positions.value = 100
        session.start(settings)
        scheduler.runCurrent()
        session.closeWindow()
        scheduler.runCurrent()
        assertEquals(WindowCloseStatus.VERIFYING, session.state.value.closeStatus)
        scheduler.advanceTimeBy(1001)
        scheduler.runCurrent()
        assertTrue(session.state.value.windowWarning)
        assertEquals(WindowCloseStatus.FAILED, session.state.value.closeStatus)
        assertEquals(50.0, session.state.value.windowPercent.valueOrNull()!!, 0.001)
    }

    @Test fun failedWindowCommandNeverShowsSuccess() {
        commandFails = true
        positions.value = 100
        session.start(settings)
        scheduler.runCurrent()
        session.closeWindow()
        scheduler.runCurrent()
        assertEquals(WindowCloseStatus.FAILED, session.state.value.closeStatus)
        assertTrue(session.state.value.windowWarning)
        assertNotNull(session.state.value.closeError)
    }

    @Test fun confirmedPhysicalCloseDismissesWarning() {
        physicallyClose = true
        positions.value = 100
        session.start(settings)
        scheduler.runCurrent()
        session.closeWindow()
        scheduler.runCurrent()
        assertFalse(session.state.value.windowWarning)
        assertEquals(WindowCloseStatus.IDLE, session.state.value.closeStatus)
    }

    @Test fun duplicateCloseTapSendsOneCommand() {
        positions.value = 100
        session.start(settings)
        scheduler.runCurrent()
        session.closeWindow()
        session.closeWindow()
        scheduler.runCurrent()
        assertEquals(1, commandCalls)
    }

    @Test fun missingTemperatureAndWindowDataStayUnknownOrUnavailable() {
        positions.value = null
        session.start(settings)
        scheduler.runCurrent()
        assertTrue(session.state.value.temperature is Reading.Unavailable)
        assertTrue(session.state.value.windowPercent is Reading.Unavailable)
        assertFalse(session.state.value.windowWarning)
    }

    @Test fun unsupportedWindowIsNotExposedAsClosed() {
        readSupported = false
        session.start(settings)
        scheduler.runCurrent()
        assertNull(session.state.value.windowPercent.valueOrNull())
        assertFalse(session.state.value.canCloseWindow)
    }

    @Test fun sessionAndTimerSurviveSecondStartAndParkDoesNotEndIt() {
        session.start(settings)
        scheduler.runCurrent()
        val started = session.state.value.startedAtElapsed
        scheduler.advanceTimeBy(10000)
        scheduler.runCurrent()
        session.start(settings)
        updates.tryEmit(VehicleUpdate.Gear(Reading.Value(DrivingGear.PARK)))
        scheduler.runCurrent()
        assertEquals(started, session.state.value.startedAtElapsed)
        assertEquals(10000, session.state.value.elapsedMs)
        assertTrue(session.state.value.active)
    }

    @Test fun endClearsResponseAndStopsTimerAndNewSessionStartsAt100() {
        session.start(settings)
        scheduler.runCurrent()
        session.updateGleoResponse("첫 응답")
        session.updateGleoResponse("새 응답")
        assertEquals("새 응답", session.state.value.gleoResponse)
        session.end()
        scheduler.advanceTimeBy(10000)
        scheduler.runCurrent()
        assertEquals(0, session.state.value.elapsedMs)
        assertNull(session.state.value.gleoResponse)
        session.start(settings)
        scheduler.runCurrent()
        assertEquals(100, session.state.value.comfort.score)
    }

    @Test fun bumpApproachHasNoPenaltyEvenAbove40() {
        session.start(settings)
        scheduler.runCurrent()
        updates.tryEmit(VehicleUpdate.Speed(Reading.Value(SpeedSample(60.0, 0))))
        bumps.value = Reading.Value(listOf(BumpApproach("one", 0)))
        scheduler.runCurrent()
        assertNotNull(session.state.value.bumpApproach)
        assertEquals(100, session.state.value.comfort.score)
    }

    @Test fun ttsFailureNeverSuppressesWindowWarning() {
        ttsFails = true
        positions.value = 100
        session.start(settings)
        scheduler.runCurrent()
        assertTrue(session.state.value.windowWarning)
        assertNotNull(session.state.value.ttsError)
    }

    @Test fun actualSpeedInputFeedsTheScoreEngine() {
        session.start(settings)
        scheduler.runCurrent()
        updates.tryEmit(VehicleUpdate.Speed(Reading.Value(SpeedSample(0.0, 0))))
        scheduler.runCurrent()
        scheduler.advanceTimeBy(1000)
        updates.tryEmit(VehicleUpdate.Speed(Reading.Value(SpeedSample(11.0, 1000))))
        scheduler.runCurrent()
        assertEquals(97, session.state.value.comfort.score)
    }

    /** Vehicle API steering at 40 km/h: 120° held 0.3s (~4.4 m/s² lateral) is one sharp turn. */
    @Test fun steeringAngleFeedsSharpTurn() {
        session.start(settings)
        scheduler.runCurrent()
        speed(40.0)
        steering.tryEmit(120f)
        scheduler.runCurrent()
        advance(300)
        speed(40.0)
        advance(300)
        speed(40.0)
        assertEquals(1, session.state.value.comfort.turnCount)
        assertEquals(96, session.state.value.comfort.score)
    }

    // ---------------- rest / AI trigger / guidance / waypoint / record (mock SDKs) ----------------

    private fun advance(ms: Long) {
        scheduler.advanceTimeBy(ms)
        scheduler.runCurrent()
    }
    private fun gear(g: DrivingGear) {
        updates.tryEmit(VehicleUpdate.Gear(Reading.Value(g)))
        scheduler.runCurrent()
    }
    private fun speed(kmh: Double) {
        updates.tryEmit(VehicleUpdate.Speed(Reading.Value(SpeedSample(kmh, scheduler.currentTime))))
        scheduler.runCurrent()
    }
    private val dues = mutableListOf<com.example.poco.data.repository.RestRecommendationDue>()
    private fun startCollecting() {
        kotlinx.coroutines.CoroutineScope(dispatcher).launch {
            session.restRecommendations.collect {
                dues +=
                    it
            }
        }
        scheduler.runCurrent()
    }
    private val place = VerifiedPlace("ChIJvSMioIapZDURijUJ9O6phq8", "덕평휴게소", 37.24, 127.39, null)

    /** #1/#3: default 120 min; interval change keeps the base time and re-computes immediately. */
    @Test fun restIntervalDefaultAndLiveChangeKeepsBase() {
        session.start(settings)
        scheduler.runCurrent()
        assertEquals(120, session.state.value.rest.intervalMinutes)
        assertEquals(120 * 60_000L, session.state.value.rest.remainingMs)
        advance(30 * 60_000L)
        interval.value = 90
        advance(250)
        assertEquals(90, session.state.value.rest.intervalMinutes)
        assertEquals(60 * 60_000L - 250, session.state.value.rest.remainingMs)
        interval.value = 30
        advance(250)
        assertTrue(session.state.value.rest.restDue)
        assertEquals(
            listOf("120->90", "90->30"),
            recorder.events.filter {
                it.type ==
                    "INTERVAL_CHANGED"
            }.map { it.detail }
        )
        assertEquals(120, recorder.trips.single().startRestIntervalMinutes)
    }

    /** #23/#24/#25: one AI request per cycle at <= 20 min; a change already inside 20 min still fires once. */
    @Test fun restRecommendationOncePerCycleIncludingIntervalChange() {
        startCollecting()
        session.start(settings)
        scheduler.runCurrent()
        advance(99 * 60_000L)
        assertTrue(dues.isEmpty())
        advance(60_000L)
        assertEquals(1, dues.size)
        advance(30 * 60_000L)
        interval.value = 150
        advance(250)
        assertEquals(1, dues.size)
        assertEquals(1, recorder.events.count { it.type == "AI_REST_REQUEST" })
        // New cycle after an actual rest (P + start + >= 3 min + real departure).
        gear(DrivingGear.PARK)
        session.startRest()
        advance(16 * 60_000L)
        session.readyToDepart()
        gear(DrivingGear.DRIVE)
        speed(20.0)
        assertEquals(2, session.state.value.rest.cycle)
        interval.value = 30
        advance(9 * 60_000L)
        assertEquals(1, dues.size)
        advance(2 * 60_000L)
        assertEquals(2, dues.size)
        assertEquals(2, dues.map { it.eventId }.distinct().size)
    }

    /** #4 and 3.2: P alone is not a rest; short stops do not reset; real re-departure resets base. */
    @Test fun restRequiresParkAndTriggerAndValidDuration() {
        session.start(settings)
        scheduler.runCurrent()
        gear(DrivingGear.DRIVE)
        session.startRest()
        assertEquals(
            com.example.poco.core.driving.RestPhase.DRIVING,
            session.state.value.rest.phase
        )
        gear(DrivingGear.PARK)
        advance(10 * 60_000L)
        assertEquals(
            com.example.poco.core.driving.RestPhase.DRIVING,
            session.state.value.rest.phase
        )
        session.startRest()
        advance(60_000L)
        gear(DrivingGear.DRIVE)
        speed(30.0)
        assertEquals(0, session.state.value.rest.restCount)
        assertEquals(1, recorder.events.count { it.type == "REST_CANCELLED" })
        gear(DrivingGear.PARK)
        session.startRest()
        advance(15 * 60_000L)
        gear(DrivingGear.DRIVE)
        advance(250)
        assertEquals(0, session.state.value.rest.restCount) // D at 0 km/h is not a departure
        speed(10.0)
        val departed = scheduler.currentTime
        assertEquals(1, session.state.value.rest.restCount)
        advance(250)
        assertEquals(
            120 * 60_000L - (scheduler.currentTime - departed),
            session.state.value.rest.remainingMs
        )
    }

    /** #8: score recovers only during a confirmed rest, never in plain P. */
    @Test fun recoveryOnlyDuringConfirmedRest() {
        session.start(settings)
        scheduler.runCurrent()
        gear(DrivingGear.DRIVE)
        // Four separated +20 km/h/s steps (each > 3 s apart, samples every 1 s): 4 x -3 = 88.
        var kmh = 0.0
        speed(kmh)
        repeat(4) {
            advance(1000)
            kmh += 20
            speed(kmh)
            repeat(4) {
                advance(1000)
                speed(kmh)
            }
        }
        assertEquals(88, session.state.value.comfort.score)
        gear(DrivingGear.PARK)
        advance(10 * 60_000L)
        assertEquals(88, session.state.value.comfort.score) // parked, but no confirmed rest
        session.startRest()
        advance(6 * 60_000L + 1_000)
        assertEquals(90, session.state.value.comfort.score) // +1 per 3 min, capped at 90
        advance(30 * 60_000L)
        assertEquals(90, session.state.value.comfort.score)
        assertEquals(2, recorder.events.count { it.type == "SCORE_RECOVERY" })
        assertEquals(
            4,
            recorder.events.count {
                it.type == "SCORE_PENALTY" &&
                    it.detail == "RAPID_ACCELERATION"
            }
        )
    }

    /** #29/#12: voice OFF shows but never speaks (and never replays later); duplicate IDs are ignored. */
    @Test fun voiceOffAndDuplicateGuidance() {
        session.start(settings)
        scheduler.runCurrent()
        voice.value = false
        scheduler.runCurrent()
        session.announce(
            com.example.poco.core.driving.Guidance(
                "a1",
                com.example.poco.core.driving.GuidancePriority.AI_ANSWER,
                "답변"
            )
        )
        scheduler.runCurrent()
        assertEquals("답변", session.state.value.guidance?.displayText)
        assertEquals(0, spoken)
        voice.value = true
        scheduler.runCurrent()
        session.announce(
            com.example.poco.core.driving.Guidance(
                "a1",
                com.example.poco.core.driving.GuidancePriority.AI_ANSWER,
                "답변"
            )
        )
        scheduler.runCurrent()
        assertEquals(0, spoken)
    }

    /** #29: window warning outranks and interrupts an AI answer that is being spoken. */
    @Test fun windowWarningInterruptsAiSpeech() {
        hangTts = true
        session.start(settings)
        scheduler.runCurrent()
        session.announce(
            com.example.poco.core.driving.Guidance(
                "a1",
                com.example.poco.core.driving.GuidancePriority.AI_ANSWER,
                "답변"
            )
        )
        scheduler.runCurrent()
        assertTrue(session.state.value.ttsSpeaking)
        positions.value = 100
        scheduler.runCurrent()
        assertEquals(2, spoken)
        assertEquals(
            com.example.poco.core.driving.GuidancePriority.WINDOW_WARNING,
            session.state.value.guidance?.priority
        )
    }

    /** #27/#28/#11: verified add -> success TTS; 5 min once; arrival needs Navi arrival AND P. */
    @Test fun waypointSuccessFiveMinutesAndArrivalTts() {
        session.start(settings)
        scheduler.runCurrent()
        gear(DrivingGear.DRIVE)
        session.addWaypoint(place)
        scheduler.runCurrent()
        assertEquals(1, waypointCalls)
        assertEquals(
            WaypointUiState.Added("덕평휴게소", "15분"),
            session.state.value.waypoint
        )
        assertEquals("15분 뒤 덕평휴게소에 들를게요.", spokenTexts.last())
        val wp = NaviWaypoint("덕평", place.latitude, place.longitude, 280, 4_000)
        route.tryEmit(NaviRouteUpdate.Waypoints(listOf(wp)))
        route.tryEmit(
            NaviRouteUpdate.Waypoints(listOf(wp.copy(remainTime = 250, remainMeters = 3_500)))
        )
        scheduler.runCurrent()
        assertEquals(1, spokenTexts.count { it.startsWith("5분 뒤") })
        gear(DrivingGear.PARK) // P before arrival: no arrival guidance
        assertEquals(0, spokenTexts.count { it.startsWith("도착했어요") })
        route.tryEmit(NaviRouteUpdate.Arrived("덕평", true))
        scheduler.runCurrent()
        assertEquals(1, spokenTexts.count { it.startsWith("도착했어요") })
        assertEquals(
            com.example.poco.core.driving.RestPhase.RESTING,
            session.state.value.rest.phase
        )
        gear(DrivingGear.PARK)
        assertEquals(1, spokenTexts.count { it.startsWith("도착했어요") })
    }

    @Test fun waypointFailureIsShownWithoutTtsAndCancellationClearsPending() {
        session.start(settings)
        scheduler.runCurrent()
        waypointFailure = "먼저 내비게이션에서 목적지를 설정해 주세요."
        session.addWaypoint(place)
        scheduler.runCurrent()
        assertTrue(
            session.state.value.waypoint is WaypointUiState.Failed
        )
        assertEquals(0, spoken)
        waypointFailure = null
        session.addWaypoint(place)
        scheduler.runCurrent()
        route.tryEmit(NaviRouteUpdate.Waypoints(emptyList()))
        scheduler.runCurrent()
        assertTrue(
            session.state.value.waypoint is WaypointUiState.Cancelled
        )
        assertEquals(1, recorder.events.count { it.type == "WAYPOINT_CANCELLED" })
    }

    /** Figma "경유지 취소": success only after Navi readback; failure keeps the waypoint with an error. */
    @Test fun userCancelWaypointConfirmedByNaviOrShowsError() {
        session.start(settings)
        scheduler.runCurrent()
        session.addWaypoint(place)
        scheduler.runCurrent()
        removeFailure = "내비게이션이 경유지 취소를 처리하지 못했어요."
        session.cancelWaypoint()
        scheduler.runCurrent()
        val failed = session.state.value.waypoint as WaypointUiState.Added
        assertEquals(removeFailure, failed.cancelError)
        assertEquals(0, recorder.events.count { it.type == "WAYPOINT_CANCELLED" })
        removeFailure = null
        session.cancelWaypoint()
        scheduler.runCurrent()
        assertEquals(2, removeCalls)
        assertEquals(
            WaypointUiState.Cancelled("덕평휴게소"),
            session.state.value.waypoint
        )
        assertEquals(1, recorder.events.count { it.type == "WAYPOINT_CANCELLED" })
        session.cancelWaypoint() // nothing left to cancel
        scheduler.runCurrent()
        assertEquals(2, removeCalls)
    }

    /** Resting screen "컴포트 81 → 83": the score at rest start is kept only while resting. */
    @Test fun restStartScoreIsExposedWhileResting() {
        session.start(settings)
        scheduler.runCurrent()
        gear(DrivingGear.PARK)
        session.startRest()
        scheduler.runCurrent()
        assertEquals(session.state.value.comfort.score, session.state.value.rest.scoreAtRestStart)
    }

    /** 5.4: passage is scored once at fresh speed; approach alone never scores. */
    @Test fun bumpPassageScoredOnce() {
        session.start(settings)
        scheduler.runCurrent()
        speed(45.0)
        bumps.value = Reading.Value(listOf(BumpApproach("b1", 20)))
        scheduler.runCurrent()
        assertEquals(100, session.state.value.comfort.score)
        bumps.value = Reading.Value(emptyList())
        scheduler.runCurrent()
        assertEquals(98, session.state.value.comfort.score)
        bumps.value = Reading.Value(listOf(BumpApproach("b1", 5)))
        scheduler.runCurrent()
        bumps.value = Reading.Value(emptyList())
        scheduler.runCurrent()
        assertEquals(98, session.state.value.comfort.score)
    }

    /** #13: end stores exactly one completion with frozen values; repeated end is a no-op. */
    @Test fun endCompletesRecordOnce() {
        session.start(settings)
        scheduler.runCurrent()
        positions.value = 100
        scheduler.runCurrent()
        advance(60_000)
        val id = session.state.value.tripId!!
        session.end()
        session.end()
        scheduler.runCurrent()
        Thread.sleep(200)
        assertEquals(1, recorder.completions.size)
        assertEquals(id, session.state.value.lastEndedTripId)
        assertEquals(1, recorder.events.count { it.type == "WINDOW_WARNING" })
        assertTrue(recorder.trips.single().simulated)
    }

    @Test fun eventKeepsNaviPlaceNameLandmarkFirstThenAddress() {
        session.start(settings)
        scheduler.runCurrent()
        val epoch = recorder.trips.single().startedAtEpoch
        places.tryEmit(
            com.example.poco.data.pleos.datasource.NaviPlaceHint("판교IC", "수정구 시흥동", epoch)
        )
        scheduler.runCurrent()
        positions.value = 100
        scheduler.runCurrent()
        assertEquals("판교IC", recorder.events.last { it.type == "WINDOW_WARNING" }.placeName)
        // 랜드마크는 20초가 지나면 쓰지 않고 주소로
        positions.value = 0
        scheduler.runCurrent()
        advance(25_000)
        positions.value = 100
        scheduler.runCurrent()
        assertEquals("수정구 시흥동", recorder.events.last { it.type == "WINDOW_WARNING" }.placeName)
        // 1분이 넘은 이름은 붙이지 않는다
        positions.value = 0
        scheduler.runCurrent()
        advance(40_000)
        positions.value = 100
        scheduler.runCurrent()
        assertNull(recorder.events.last { it.type == "WINDOW_WARNING" }.placeName)
    }

    @Test fun eventCapturesFreshLocationWithoutHeadingAndDropsStaleFixes() {
        session.start(settings)
        scheduler.runCurrent()
        val epoch = recorder.trips.single().startedAtEpoch
        locationFixes.tryEmit(LocationFix(37.0, 127.0, null, epoch))
        scheduler.runCurrent()
        positions.value = 100
        scheduler.runCurrent()
        val event = recorder.events.last { it.type == "WINDOW_WARNING" }
        assertEquals(37.0, event.latitude!!, 0.0)
        assertEquals(127.0, event.longitude!!, 0.0)
        assertEquals(epoch, event.locationAtEpoch)
        assertEquals("REAR_RIGHT", event.seat)
        assertEquals(50.0, event.windowPercent!!, 0.0)
        positions.value = 0
        scheduler.runCurrent()
        advance(31_000)
        positions.value = 100
        scheduler.runCurrent()
        assertNull(recorder.events.last { it.type == "WINDOW_WARNING" }.latitude)
    }

    class FakeRecorder : com.example.poco.data.record.TripRecordSink {
        val trips = java.util.Collections.synchronizedList(
            mutableListOf<com.example.poco.data.record.TripEntity>()
        )
        val events = java.util.Collections.synchronizedList(
            mutableListOf<com.example.poco.data.record.TripEventEntity>()
        )
        val completions = java.util.Collections.synchronizedList(mutableListOf<String>())
        override fun begin(trip: com.example.poco.data.record.TripEntity) {
            trips += trip
        }
        override fun event(event: com.example.poco.data.record.TripEventEntity) {
            if (event.dedupeKey == null ||
                events.none { it.dedupeKey == event.dedupeKey }
            ) {
                events += event
            }
        }
        override suspend fun complete(
            tripId: String,
            endedAt: Long,
            finalScore: Int,
            drivingMs: Long,
            restMs: Long,
            restCount: Int
        ): Boolean {
            if (tripId in completions) return false
            completions += tripId
            return true
        }
    }
}
