package com.example.poco.data.repositoryimpl

import com.example.poco.core.driving.BumpPassageTracker
import com.example.poco.core.driving.CabinTemperatureStatus
import com.example.poco.core.driving.ComfortScoreCalculator
import com.example.poco.core.driving.ComfortScoreConfig
import com.example.poco.core.driving.DrivingClock
import com.example.poco.core.driving.DrivingEvent
import com.example.poco.core.driving.DrivingEventDetector
import com.example.poco.core.driving.DrivingGear
import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.Guidance
import com.example.poco.core.driving.GuidanceLedger
import com.example.poco.core.driving.GuidancePriority
import com.example.poco.core.driving.Reading
import com.example.poco.core.driving.RestEvent
import com.example.poco.core.driving.RestIntervalPolicy
import com.example.poco.core.driving.RestPhase
import com.example.poco.core.driving.RestPlanner
import com.example.poco.core.driving.RestStartSource
import com.example.poco.core.driving.VerifiedPlace
import com.example.poco.core.driving.WaypointSignal
import com.example.poco.core.driving.WaypointTracker
import com.example.poco.core.driving.WindowCloseStatus
import com.example.poco.core.driving.WindowDangerDetector
import com.example.poco.core.driving.normalizeWindowPosition
import com.example.poco.core.driving.temperatureStatus
import com.example.poco.core.driving.valueOrNull
import com.example.poco.data.assistant.AssistantWaypointSource
import com.example.poco.data.assistant.LocationStream
import com.example.poco.data.automotive.VehicleUpdate
import com.example.poco.data.model.DrivingSessionState
import com.example.poco.data.model.PlaceProposal
import com.example.poco.data.model.RestUiState
import com.example.poco.data.model.WaypointUiState
import com.example.poco.data.model.WindowPositionRange
import com.example.poco.data.pleos.datasource.NaviRouteUpdate
import com.example.poco.data.record.TripEntity
import com.example.poco.data.record.TripEventEntity
import com.example.poco.data.record.TripEventType
import com.example.poco.data.record.TripRecordSink
import com.example.poco.data.repository.DrivingSessionRepository
import com.example.poco.data.repository.VehicleMotionRepository
import com.example.poco.data.repository.HvacRepository
import com.example.poco.data.repository.NaviRepository
import com.example.poco.data.repository.RestRecommendationDue
import com.example.poco.data.repository.TtsRepository
import com.example.poco.data.repository.VehicleRepository
import com.example.poco.data.repository.WindowRepository
import com.example.poco.data.settings.UserSettingsRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber

/** Device origin of vehicle data (emulator VHAL vs. real car), shown and stored with each trip. */
fun interface DataOrigin {
    fun simulated(): Boolean
}

/**
 * One process-owned session shared by the full and split screens. Screens only render this state;
 * timers, scoring, rest scheduling, guidance de-duplication, TTS and trip recording live here once.
 */
@Singleton
class DrivingSessionRepositoryImpl @Inject constructor(
    private val vehicle: VehicleRepository,
    private val hvac: HvacRepository,
    private val windows: WindowRepository,
    private val navi: NaviRepository,
    private val tts: TtsRepository,
    private val clock: DrivingClock,
    private val config: ComfortScoreConfig,
    private val userSettings: UserSettingsRepository,
    private val recorder: TripRecordSink,
    private val waypoints: AssistantWaypointSource,
    private val location: LocationStream,
    private val origin: DataOrigin,
    private val motion: VehicleMotionRepository
) : DrivingSessionRepository {
    private val mutableState = MutableStateFlow(DrivingSessionState())
    override val state = mutableState.asStateFlow()
    private val recommendations = MutableSharedFlow<RestRecommendationDue>(extraBufferCapacity = 4)
    override val restRecommendations = recommendations.asSharedFlow()
    private var sessionScope: CoroutineScope? = null
    private val finishScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var generation = 0L
    private var closeJob: Job? = null
    private var voiceJob: Job? = null
    private var waypointJob: Job? = null
    private var lastBumpUpdate = 0L
    private var windowRange: WindowPositionRange? = null
    private var windowRevision = 0L
    private var windowEpisode = 0
    private var warningDetector = WindowDangerDetector(config.windowWarningPercent)
    private lateinit var detector: DrivingEventDetector
    private lateinit var calculator: ComfortScoreCalculator
    private var planner: RestPlanner? = null
    private var ledger = GuidanceLedger()
    private var bumpTracker = BumpPassageTracker()
    private var waypointTracker: WaypointTracker? = null
    private var lastTemperatureStatus = CabinTemperatureStatus.UNKNOWN
    private var lastTemperatureSample = Long.MIN_VALUE
    private var started = 0L
    private var startedEpoch = 0L
    private var restStartScore: Int? = null
    private var latestLocation: com.example.poco.data.assistant.LocationFix? = null

    private fun epoch(mono: Long) = startedEpoch + (mono - started)

    private fun record(type: String, delta: Int? = null, after: Int? = null, detail: String? = null, key: String? = null, at: Long = clock.now()) {
        val snapshot = state.value
        val trip = snapshot.tripId ?: return
        val eventEpoch = epoch(at)
        val fix = latestLocation?.takeIf {
            eventEpoch - it.epochMs in 0..30_000 && it.latitude.isFinite() && it.longitude.isFinite() &&
                it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0
        }
        recorder.event(TripEventEntity(
            tripId = trip, atEpoch = eventEpoch, type = type, scoreDelta = delta,
            scoreAfter = after, detail = detail, dedupeKey = key,
            scoreBefore = if (after != null && delta != null) after - delta else null,
            latitude = fix?.latitude, longitude = fix?.longitude, locationAtEpoch = fix?.epochMs,
            temperatureC = snapshot.temperature.valueOrNull(), windowPercent = snapshot.windowPercent.valueOrNull(),
            seat = snapshot.settings?.seat?.name,
            guidanceText = snapshot.guidance?.displayText ?: snapshot.gleoResponse
        ))
    }

    override fun start(settings: DrivingSettings) {
        if (state.value.active) return
        val token = ++generation
        latestLocation = null
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        sessionScope = scope
        detector = DrivingEventDetector(config) { reason ->
            Timber.w(reason)
            record(TripEventType.SENSOR_ANOMALY, detail = reason.take(120))
        }
        calculator = ComfortScoreCalculator(clock, config)
        warningDetector = WindowDangerDetector(config.windowWarningPercent)
        ledger = GuidanceLedger()
        bumpTracker = BumpPassageTracker()
        waypointTracker = null
        windowRange = null
        windowRevision = 0
        windowEpisode = 0
        lastTemperatureStatus = CabinTemperatureStatus.UNKNOWN
        lastTemperatureSample = Long.MIN_VALUE
        started = clock.now()
        startedEpoch = System.currentTimeMillis()
        val tripId = UUID.randomUUID().toString()
        val simulated = origin.simulated()
        // The persisted interval is applied as soon as DataStore emits; default until then.
        planner = RestPlanner(started, RestIntervalPolicy.DEFAULT_MINUTES, config.validRestMs)
        mutableState.value = DrivingSessionState(
            active = true,
            tripId = tripId,
            settings = settings,
            startedAtElapsed = started,
            startedAtEpoch = startedEpoch,
            voiceGuidance = state.value.voiceGuidance,
            simulatedSource = simulated
        )
        var tripBegun = false
        fun current() = token == generation
        scope.launch {
            userSettings.restIntervalMinutes.catch { }.collect { minutes ->
                if (!current()) return@collect
                val p = planner ?: return@collect
                if (!tripBegun) {
                    // The first emitted value is the interval in force at departure (stored with the trip).
                    tripBegun = true
                    p.changeInterval(minutes, started)
                    recorder.begin(
                        TripEntity(
                            id = tripId, petName = settings.petName, petWeightTenths = settings.weightTenths,
                            petSize = settings.petSize?.name, seat = settings.seat.name,
                            startedAtEpoch = startedEpoch, startRestIntervalMinutes = p.snapshot.intervalMinutes,
                            simulated = simulated
                        )
                    )
                } else {
                    p.changeInterval(minutes, clock.now())?.let {
                        record(TripEventType.INTERVAL_CHANGED, detail = "${it.from}->${it.to}")
                    }
                }
                publishRest()
            }
        }
        scope.launch {
            userSettings.voiceGuidanceEnabled.catch { }.collect { enabled ->
                if (current()) mutableState.update { it.copy(voiceGuidance = enabled) }
            }
        }
        scope.launch {
            vehicle.observe().collect { update ->
                if (!current()) return@collect
                when (update) {
                    is VehicleUpdate.Speed -> onSpeed(update)
                    is VehicleUpdate.Gear -> {
                        tickScore()
                        mutableState.update { it.copy(gear = update.reading) }
                        tickScore()
                        val parked = update.reading.valueOrNull() == DrivingGear.PARK
                        waypointTracker?.onGear(parked)?.let(::onWaypointSignal)
                    }
                }
                mutableState.update { it.copy(comfort = calculator.state) }
            }
        }
        scope.launch {
            hvac.observeInteriorTemperature(settings.seat).collect { value ->
                if (!current()) return@collect
                mutableState.update { it.copy(temperature = value) }
                onTemperature(value.valueOrNull(), settings)
            }
        }
        scope.launch {
            navi.observeBumps().collect { value ->
                lastBumpUpdate = clock.now()
                if (!current()) return@collect
                mutableState.update { previous ->
                    previous.copy(
                        naviStatus = (value as? Reading.Unavailable)?.reason,
                        bumpApproach = value.valueOrNull()?.minByOrNull { it.remainingMeters }
                            ?.let { "${it.remainingMeters}m 앞 과속방지턱 · 천천히 지나가세요" }
                    )
                }
                val points = value.valueOrNull() ?: return@collect
                // Passage = a point reported within 30 m disappears. Scored once, with fresh speed only.
                bumpTracker.update(points.map { it.id to it.remainingMeters }).forEach { _ ->
                    val sample = state.value.speed.valueOrNull()
                    val fresh = sample != null && clock.now() - sample.at <= config.sensorStaleMs
                    detector.speedBump(fresh, sample?.kmh)?.let { applyEvents(listOf(it)) }
                }
            }
        }
        scope.launch {
            navi.observeRoute().collect { update -> if (current()) onRoute(update) }
        }
        scope.launch {
            location.observe().catch { }.collect { fix ->
                if (!current()) return@collect
                latestLocation = fix
                val heading = fix.headingDegrees ?: return@collect
                val sample = state.value.speed.valueOrNull() ?: return@collect
                if (clock.now() - sample.at > config.sensorStaleMs) return@collect
                applyEvents(detector.onHeading(clock.now(), heading, sample.kmh))
            }
        }
        scope.launch {
            // Vehicle API steering angle (Pleos SteeringWheel, falling back to PERF_STEERING_ANGLE).
            // Optional: without it, sharp turns still come from the GPS heading below.
            motion.observeSteeringAngle().catch { Timber.w(it, "조향각 수신 실패") }.collect { angle ->
                if (!current()) return@collect
                applyEvents(detector.onSteering(clock.now(), angle.toDouble()))
            }
        }
        scope.launch { observeWindow(settings, ::current) }
        scope.launch {
            while (isActive) {
                delay(250)
                if (!current()) break
                applyEvents(detector.flush(clock.now()))
                tickScore()
                if (clock.now() - started > 8_000) {
                    mutableState.update {
                        it.copy(
                            speed = if (it.speed == Reading.Unknown) Reading.Unavailable("속도 수신 없음") else it.speed,
                            gear = if (it.gear == Reading.Unknown) Reading.Unavailable("기어 수신 없음") else it.gear
                        )
                    }
                }
                val speed = state.value.speed.valueOrNull()
                if (speed != null && clock.now() - speed.at > config.sensorStaleMs) {
                    detector.resetSpeed()
                    mutableState.update { it.copy(speed = Reading.Unavailable("차량 속도 갱신 지연")) }
                }
                if (state.value.bumpApproach != null && clock.now() - lastBumpUpdate > 10_000) {
                    mutableState.update { it.copy(bumpApproach = null) }
                }
                planner?.checkRecommendation(clock.now())?.let { due ->
                    val eventId = "rest-${tripId.take(8)}-${due.cycle}"
                    record(TripEventType.AI_REST_REQUEST, detail = "cycle ${due.cycle}", key = eventId)
                    recommendations.tryEmit(RestRecommendationDue(tripId, due.cycle, eventId, due.remainingMs))
                }
                publishRest()
                mutableState.update {
                    it.copy(elapsedMs = (clock.now() - started).coerceAtLeast(0), comfort = calculator.state)
                }
            }
        }
    }

    private fun onSpeed(update: VehicleUpdate.Speed) {
        val sample = update.reading.valueOrNull()
        if (sample != null && sample.at <= clock.now() && clock.now() - sample.at <= config.sensorStaleMs) {
            applyEvents(detector.onSpeed(sample))
            mutableState.update { it.copy(speed = update.reading) }
            // Actual re-departure: moving in a non-P gear. P->D at 0 km/h is not a departure.
            val gear = state.value.gear.valueOrNull()
            if (abs(sample.kmh) >= 5 && gear != null && gear != DrivingGear.PARK) {
                planner?.onDeparture(clock.now())?.let(::onRestEvent)
            }
        } else {
            detector.resetSpeed()
            mutableState.update {
                it.copy(speed = if (sample != null) Reading.Unavailable("차량 속도 갱신 지연") else update.reading)
            }
        }
    }

    private fun parked() = state.value.gear.valueOrNull() == DrivingGear.PARK

    private fun tickScore() {
        val rest = planner?.snapshot
        val resting = rest != null && rest.phase != RestPhase.DRIVING && parked()
        val speed = state.value.speed.valueOrNull()
        val stable = state.value.gear.valueOrNull() == DrivingGear.DRIVE && speed != null &&
            clock.now() - speed.at <= config.sensorStaleMs && abs(speed.kmh) >= 5
        calculator.tick(resting, stable)
        drainScore()
    }

    private fun drainScore() {
        calculator.drainChanges().forEach { change ->
            record(
                if (change.reason == "RECOVERY") TripEventType.SCORE_RECOVERY else TripEventType.SCORE_PENALTY,
                change.delta, change.scoreAfter, change.reason, at = change.at
            )
        }
    }

    private fun applyEvents(events: List<DrivingEvent>) {
        events.forEach(calculator::penalize)
        drainScore()
        mutableState.update { it.copy(comfort = calculator.state) }
    }

    private fun onTemperature(value: Float?, settings: DrivingSettings) {
        val status = temperatureStatus(value, settings)
        val now = clock.now()
        if (value != null && now - lastTemperatureSample >= 60_000) {
            lastTemperatureSample = now
            record(TripEventType.TEMPERATURE_SAMPLE, detail = "%.1f".format(java.util.Locale.ROOT, value))
        }
        val outside = status == CabinTemperatureStatus.HOT || status == CabinTemperatureStatus.COLD
        if (outside && status != lastTemperatureStatus) {
            record(TripEventType.TEMPERATURE_OUT_OF_RANGE, detail = "${status.name}:%.1f".format(java.util.Locale.ROOT, value))
        }
        // UNKNOWN (no measurement) neither ends nor starts an episode: no value is invented.
        if (status != CabinTemperatureStatus.UNKNOWN) lastTemperatureStatus = status
    }

    private fun publishRest() {
        val p = planner ?: return
        val s = p.snapshot
        val now = clock.now()
        mutableState.update {
            it.copy(
                rest = RestUiState(
                    phase = s.phase,
                    intervalMinutes = s.intervalMinutes,
                    nextRestAtEpoch = epoch(s.nextRestAt),
                    remainingMs = s.remainingMs(now),
                    restDue = s.restDue(now),
                    restStartedAtEpoch = s.restStartedAt?.let(::epoch),
                    currentRestMs = s.restStartedAt?.let { start -> now - start } ?: 0,
                    restCount = s.restCount,
                    totalRestMs = s.totalRestMs,
                    cycle = s.cycle,
                    scoreAtRestStart = if (s.phase != RestPhase.DRIVING) restStartScore else null
                )
            )
        }
    }

    private fun onRestEvent(event: RestEvent) {
        when (event) {
            is RestEvent.Started -> {
                restStartScore = calculator.state.score
                record(TripEventType.REST_STARTED, detail = event.source.name, at = event.at)
            }
            is RestEvent.Completed -> {
                calculator.markRestCompleted()
                record(TripEventType.REST_COMPLETED, detail = event.durationMs.toString(), at = event.departedAt)
                record(TripEventType.DEPARTED, at = event.departedAt)
                // Rest-proposal guidance belongs to the finished cycle.
                ledger.removeWhere { it.priority == GuidancePriority.REST_PROPOSAL }
                mutableState.update { it.copy(proposal = null, aiRestStatus = null) }
                publishGuidance()
            }
            is RestEvent.Cancelled -> record(TripEventType.REST_CANCELLED, at = event.departedAt)
            else -> Unit
        }
        publishRest()
    }

    override fun startRest() {
        if (!state.value.active) return
        planner?.startRest(clock.now(), parked(), RestStartSource.USER)?.let(::onRestEvent)
    }

    override fun readyToDepart() {
        planner?.readyToDepart(clock.now())
        publishRest()
    }

    override fun setRestInterval(minutes: Int) {
        // Persist only; the DataStore flow applies it to the running session exactly once.
        finishScope.launch { userSettings.setRestIntervalMinutes(minutes) }
    }

    override fun setVoiceGuidance(enabled: Boolean) {
        if (!enabled) voiceJob?.cancel()
        mutableState.update { it.copy(voiceGuidance = enabled) }
        finishScope.launch { userSettings.setVoiceGuidanceEnabled(enabled) }
    }

    // ---- Guidance (one ledger for screen + TTS) ----

    override fun announce(guidance: Guidance) {
        val scope = sessionScope ?: return
        val decision = ledger.post(guidance, state.value.voiceGuidance)
        if (!decision.accepted) return
        if (guidance.priority == GuidancePriority.AI_ANSWER) {
            mutableState.update { it.copy(gleoResponse = guidance.displayText) }
        }
        publishGuidance()
        if (!decision.speak) return
        if (decision.interruptCurrent) voiceJob?.cancel()
        val token = generation
        mutableState.update { it.copy(ttsError = null) }
        voiceJob = scope.launch {
            ledger.startedSpeaking(guidance)
            try {
                tts.speak(guidance.ttsText.orEmpty()) { speaking ->
                    if (token == generation) mutableState.update { it.copy(ttsSpeaking = speaking) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (token == generation) {
                    mutableState.update { it.copy(ttsError = e.message ?: "음성 안내 실패", ttsSpeaking = false) }
                }
            } finally {
                ledger.finishedSpeaking(guidance.id)
                if (token == generation) mutableState.update { it.copy(ttsSpeaking = false) }
            }
        }
    }

    override fun dismissGuidance(id: String) {
        ledger.remove(id)
        publishGuidance()
    }

    private fun publishGuidance() {
        mutableState.update { it.copy(guidance = ledger.top()) }
    }

    override fun showProposal(proposal: PlaceProposal) {
        if (!state.value.active) return
        mutableState.update { it.copy(proposal = proposal, aiRestStatus = null) }
        val text = proposal.response.displayText
        announce(
            Guidance(
                proposal.eventId,
                if (proposal.automatic) GuidancePriority.REST_PROPOSAL else GuidancePriority.AI_ANSWER,
                text,
                proposal.response.ttsText
            )
        )
        if (!proposal.automatic) mutableState.update { it.copy(gleoResponse = text) }
    }

    override fun dismissProposal() {
        state.value.proposal?.let { ledger.remove(it.eventId) }
        mutableState.update { it.copy(proposal = null) }
        publishGuidance()
    }

    override fun setAiRestStatus(text: String?) {
        if (state.value.active) mutableState.update { it.copy(aiRestStatus = text) }
    }

    // ---- Waypoint ----

    override fun addWaypoint(place: VerifiedPlace) {
        val scope = sessionScope ?: return
        if (waypointJob?.isActive == true || waypointTracker?.finished == false) return
        val token = generation
        mutableState.update { it.copy(waypoint = WaypointUiState.Adding(place.name)) }
        waypointJob = scope.launch {
            try {
                val confirmed = waypoints.add(place)
                if (token != generation) return@launch
                val tracker = WaypointTracker(place)
                waypointTracker = tracker
                tracker.onWaypoints(listOf(confirmed))
                record(TripEventType.WAYPOINT_ADDED, detail = place.placeId)
                val eta = tracker.etaMinutesText()
                state.value.proposal?.let { ledger.remove(it.eventId) }
                mutableState.update { it.copy(waypoint = WaypointUiState.Added(place.name, eta), proposal = null) }
                val text = if (eta != null) "$eta 뒤 ${place.name}에 들를게요." else "${place.name}을 경유지로 추가했어요."
                announce(Guidance("wp-added-${place.placeId}", GuidancePriority.REST_PROPOSAL, text))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (token != generation) return@launch
                val message = if (e is TimeoutCancellationException) {
                    "경유지 등록을 확인하지 못했어요. 내비게이션에서 확인해 주세요."
                } else {
                    e.message ?: "경유지를 추가하지 못했어요."
                }
                record(TripEventType.WAYPOINT_FAILED, detail = message.take(120))
                // Failure is shown on screen only; success-only TTS policy.
                mutableState.update { it.copy(waypoint = WaypointUiState.Failed(place.name, message)) }
            }
        }
    }

    override fun cancelWaypoint() {
        val scope = sessionScope ?: return
        val tracker = waypointTracker ?: return
        val added = state.value.waypoint as? WaypointUiState.Added ?: return
        if (added.cancelling || tracker.finished) return
        val token = generation
        mutableState.update { it.copy(waypoint = added.copy(cancelling = true, cancelError = null)) }
        scope.launch {
            try {
                waypoints.remove(tracker.place)
                if (token != generation || waypointTracker !== tracker) return@launch
                // Same path as a Navi-side cancellation: record once, clear pending guidance.
                tracker.onWaypoints(emptyList())?.let(::onWaypointSignal)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (token != generation) return@launch
                val message = if (e is TimeoutCancellationException) {
                    "경유지 취소를 확인하지 못했어요. 내비게이션에서 확인해 주세요."
                } else {
                    e.message ?: "경유지를 취소하지 못했어요."
                }
                mutableState.update {
                    val w = it.waypoint
                    if (w is WaypointUiState.Added) it.copy(waypoint = w.copy(cancelling = false, cancelError = message)) else it
                }
            }
        }
    }

    private fun onRoute(update: NaviRouteUpdate) {
        val tracker = waypointTracker ?: return
        when (update) {
            is NaviRouteUpdate.Waypoints -> {
                tracker.onWaypoints(update.list)?.let(::onWaypointSignal)
                if (!tracker.finished) {
                    mutableState.update {
                        val w = it.waypoint
                        if (w is WaypointUiState.Added) it.copy(waypoint = w.copy(etaText = tracker.etaMinutesText())) else it
                    }
                }
            }
            is NaviRouteUpdate.Arrived -> if (update.first) {
                record(TripEventType.WAYPOINT_ARRIVED, detail = update.name.take(100))
                mutableState.update { it.copy(waypoint = WaypointUiState.Arrived(tracker.place.name)) }
                tracker.onArrived(parked())?.let(::onWaypointSignal)
            }
            NaviRouteUpdate.RouteEnded -> if (!tracker.arrived) {
                tracker.onWaypoints(emptyList())?.let(::onWaypointSignal)
            }
        }
    }

    private fun onWaypointSignal(signal: WaypointSignal) {
        when (signal) {
            is WaypointSignal.FiveMinutes -> announce(
                Guidance("wp-5min-${signal.placeId}", GuidancePriority.WAYPOINT_FIVE_MINUTES, "5분 뒤 휴게소예요. 물을 준비해 주세요.")
            )
            is WaypointSignal.ArrivedAndParked -> {
                ledger.removeWhere { it.id.startsWith("wp-5min-") || it.id.startsWith("wp-added-") }
                announce(Guidance("wp-arrived-${signal.placeId}", GuidancePriority.WAYPOINT_ARRIVAL, "도착했어요. 물부터 챙겨 주세요."))
                // Verified arrival + confirmed P: a real rest starts (not P alone).
                planner?.startRest(clock.now(), parked(), RestStartSource.WAYPOINT_ARRIVAL)?.let(::onRestEvent)
            }
            is WaypointSignal.Cancelled -> {
                record(TripEventType.WAYPOINT_CANCELLED, detail = signal.placeId)
                ledger.removeWhere { it.id.startsWith("wp-") }
                publishGuidance()
                waypointTracker = null
                mutableState.update {
                    val name = (it.waypoint as? WaypointUiState.Added)?.name ?: return@update it.copy(waypoint = WaypointUiState.None)
                    it.copy(waypoint = WaypointUiState.Cancelled(name))
                }
            }
        }
    }

    // ---- Window safety (unchanged rules; warning now goes through the shared guidance ledger) ----

    private suspend fun observeWindow(settings: DrivingSettings, current: () -> Boolean) {
        val scope = sessionScope ?: return
        try {
            val range = windows.getSupportedWindows().getOrThrow()[settings.seat.window]
                ?: error("탑승 위치 창문 미지원")
            check(range.canRead && range.min == 0 && range.max > 0) { "창문 위치 범위 또는 읽기 미지원" }
            windowRange = range
            mutableState.update { it.copy(canCloseWindow = range.canWrite) }
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                windows.observeWindowPosition(settings.seat.window)
                    .catch { error -> if (current()) windowUnavailable(error.message ?: "창문 관찰 실패") }
                    .collect { raw ->
                        if (current()) {
                            windowRevision++
                            acceptWindow(raw, range)
                        }
                    }
            }
            val revision = windowRevision
            val initial = windows.getWindowPosition(settings.seat.window).getOrThrow()
            if (current() && revision == windowRevision) acceptWindow(initial, range)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (current()) windowUnavailable(e.message ?: "창문 상태 조회 실패")
        }
    }

    private fun windowUnavailable(reason: String) {
        mutableState.update { it.copy(windowPercent = Reading.Unavailable(reason), canCloseWindow = false) }
    }

    private fun windowGuidanceId() = "window-$windowEpisode"

    private fun acceptWindow(raw: Int?, range: WindowPositionRange) {
        val percent = normalizeWindowPosition(raw, range)
        if (percent == null) {
            windowUnavailable("창문 위치 데이터 없음")
            return
        }
        // 설정 > 알림 '창문 안전 알림'을 끄면 열림 경고 · 음성 안내를 하지 않는다 (위치 값은 계속 보여 준다)
        val entered = warningDetector.update(percent) && mutableState.value.settings?.windowAlerts != false
        mutableState.update {
            val closing = it.closeStatus == WindowCloseStatus.REQUESTING || it.closeStatus == WindowCloseStatus.VERIFYING
            it.copy(
                windowPercent = Reading.Value(percent),
                canCloseWindow = range.canWrite,
                windowWarning = when {
                    percent == 0.0 -> false
                    entered -> true
                    percent <= config.windowWarningPercent && !closing -> false
                    else -> it.windowWarning
                },
                closeStatus = if (percent == 0.0) WindowCloseStatus.IDLE else it.closeStatus,
                closeError = if (percent == 0.0) null else it.closeError
            )
        }
        if (percent <= config.windowWarningPercent) {
            if (ledger.speaking?.priority == GuidancePriority.WINDOW_WARNING) voiceJob?.cancel()
            ledger.remove(windowGuidanceId())
            publishGuidance()
        }
        if (entered) {
            windowEpisode++
            record(TripEventType.WINDOW_WARNING, detail = percent.roundToInt().toString())
            announce(
                Guidance(
                    windowGuidanceId(),
                    GuidancePriority.WINDOW_WARNING,
                    "반려견이 있는 쪽 창문이 ${percent.roundToInt()}% 열려 있어요.",
                    "반려견이 있는 쪽 창문이 많이 열려 있어요. 안전을 위해 창문을 닫아주세요."
                )
            )
        }
    }

    override fun closeWindow() {
        val scope = sessionScope ?: return
        if (closeJob?.isActive == true) return
        val snapshot = state.value
        val settings = snapshot.settings ?: return
        val range = windowRange ?: return
        if (!snapshot.canCloseWindow) return
        val token = generation
        closeJob = scope.launch {
            mutableState.update { it.copy(closeStatus = WindowCloseStatus.REQUESTING, closeError = null) }
            try {
                withTimeout(config.closeTimeoutMs) {
                    windows.setWindowPosition(settings.seat.window, 0).getOrThrow()
                    ensureActive()
                    mutableState.update { it.copy(closeStatus = WindowCloseStatus.VERIFYING) }
                    // Only fresh readbacks/SDK listener values can confirm completion, never the set result.
                    while (true) {
                        val revision = windowRevision
                        val raw = windows.getWindowPosition(settings.seat.window).getOrThrow()
                        if (token != generation) return@withTimeout
                        if (revision == windowRevision) acceptWindow(raw, range)
                        if (state.value.windowPercent.valueOrNull() == 0.0) break
                        delay(350)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                if (token == generation) closeFailed("창문 닫힘을 확인하지 못했어요. 차량 스위치를 확인해 주세요.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (token == generation) closeFailed("창문을 닫지 못했어요. 다시 시도하거나 차량 스위치를 이용해 주세요.")
            }
        }
    }

    private fun closeFailed(message: String) {
        if (state.value.windowPercent.valueOrNull() == 0.0) return
        mutableState.update { it.copy(windowWarning = true, closeStatus = WindowCloseStatus.FAILED, closeError = message) }
    }

    override fun dismissWindowWarning() {
        if (state.value.closeStatus == WindowCloseStatus.REQUESTING || state.value.closeStatus == WindowCloseStatus.VERIFYING) return
        mutableState.update { it.copy(windowWarning = false) }
        ledger.remove(windowGuidanceId())
        publishGuidance()
    }

    override fun updateGleoResponse(text: String?) {
        if (state.value.active) mutableState.update { it.copy(gleoResponse = text?.takeIf(String::isNotBlank)) }
    }

    override fun end() {
        val snapshot = state.value
        val tripId = snapshot.tripId
        val p = planner
        if (snapshot.active && tripId != null && p != null) {
            val now = clock.now()
            val s = p.snapshot
            // A rest still in progress at the end counts toward rest time but not as a completed rest.
            val openRest = s.restStartedAt?.let { now - it } ?: 0
            val restMs = s.totalRestMs + openRest
            val elapsed = (now - started).coerceAtLeast(0)
            val final = calculator.state.score
            drainScore()
            finishScope.launch {
                runCatching {
                    recorder.complete(tripId, epoch(now), final, (elapsed - restMs).coerceAtLeast(0), restMs, s.restCount)
                }.onFailure { Timber.w(it, "trip completion") }
            }
        }
        generation++
        latestLocation = null
        sessionScope?.cancel()
        sessionScope = null
        closeJob = null
        voiceJob = null
        waypointJob = null
        waypointTracker = null
        windowRange = null
        planner = null
        mutableState.value = DrivingSessionState(
            voiceGuidance = snapshot.voiceGuidance,
            lastEndedTripId = tripId ?: snapshot.lastEndedTripId
        )
    }
}
