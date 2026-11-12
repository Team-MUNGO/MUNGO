package com.example.poco.presentation.driving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.core.util.eulReul
import com.example.poco.core.util.iGa
import com.example.poco.data.model.CarWindow
import com.example.poco.data.model.CautionKind
import com.example.poco.data.model.NaviEvent
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.TripSummary
import com.example.poco.data.model.TurnKind
import com.example.poco.data.model.UpcomingCaution
import com.example.poco.data.model.UpcomingTurn
import com.example.poco.data.model.VoiceFrequency
import com.example.poco.data.model.WindowPositionRange
import com.example.poco.data.repository.DepartureRepository
import com.example.poco.data.repository.GearRepository
import com.example.poco.data.repository.HvacRepository
import com.example.poco.data.repository.NaviRepository
import com.example.poco.data.repository.PetRepository
import com.example.poco.data.repository.SttRepository
import com.example.poco.data.repository.TripRepository
import com.example.poco.data.repository.TtsRepository
import com.example.poco.data.repository.VehicleMotionRepository
import com.example.poco.data.repository.WindowRepository
import com.example.poco.domain.driving.ComfortTracker
import com.example.poco.domain.driving.DrivingEvent
import com.example.poco.domain.driving.DrivingEventType
import com.example.poco.domain.driving.PetType
import com.example.poco.domain.driving.PocoStandard
import com.example.poco.domain.driving.RoadContext
import com.example.poco.domain.driving.VoiceReply
import com.example.poco.domain.driving.shakeThresholdFactor
import com.example.poco.domain.driving.temperatureStandard
import com.example.poco.presentation.petprofile.label
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class DrivingViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val departureRepository: DepartureRepository,
    private val vehicleMotionRepository: VehicleMotionRepository,
    private val hvacRepository: HvacRepository,
    private val windowRepository: WindowRepository,
    private val gearRepository: GearRepository,
    private val ttsRepository: TtsRepository,
    private val tripRepository: TripRepository,
    private val naviRepository: NaviRepository,
    private val sttRepository: SttRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DrivingUiState())
    val uiState = _uiState.asStateFlow()

    private val startedAt = System.currentTimeMillis()
    private var tracker = ComfortTracker(startedAt)
    private var pet: Pet? = null
    private var voiceFrequency = VoiceFrequency.NORMAL

    /** POCO 기준점 ①: 견종 유형별 온도 기준 (펫이 없으면 기본 유형) */
    private var temperatureStandard = PocoStandard.temperature(setOf(PetType.BASIC))

    private val lastSpokenAt = mutableMapOf<VoiceKey, Long>()
    private var lastRoutineSpokenAt = 0L

    private var windowRanges: Map<CarWindow, WindowPositionRange> = emptyMap()
    private val windowOpenPercent = mutableMapOf<CarWindow, Int>()
    private var windowAlertCount = 0
    private var isWindowAlertActive = false
    private var isWindowDismissed = false

    private var isTemperatureEpisode = false
    private var isTemperatureDismissed = false
    private var isTemperatureDanger = false

    private var isFinishing = false

    /** NaviHelper 목적지 (도착 리포트 경로 칩) */
    private var destinationName: String? = null

    /** 알림 질문 뒤 운전자 대답 듣기 (새 알림이 오거나 버튼을 누르면 취소) */
    private var listenJob: Job? = null

    /** 미리 안내한 구간: 안내 시각 → 지나가면 부드럽게 통과했는지 판정 */
    private var pendingAnticipation: Long? = null

    init {
        viewModelScope.launch {
            loadSettings()
            say(VoiceKey.START, "출발했어요. ${petName().eulReul} 지켜볼게요.")
            launch { observeSpeed() }
            launch { observeSteering() }
            launch { observeBrake() }
            launch { observeHeading() }
            launch { observeNavi() }
            launch { observeGear() }
            launch { observeWindows() }
            if (departureRepository.isRearTempWatchEnabled.first()) launch { observeTemperature() }
            launch { tick() }
        }
    }

    private suspend fun loadSettings() {
        pet = petRepository.observePets().first().firstOrNull()
        pet?.let {
            tracker = ComfortTracker(startedAt, it.shakeThresholdFactor)
            temperatureStandard = it.temperatureStandard
            voiceFrequency = it.voiceFrequency
        }
        val voiceEnabled = departureRepository.isVoiceGuideEnabled.first()
        _uiState.update {
            it.copy(
                petName = petName(),
                petSeat = pet?.seat ?: PetSeat.REAR_RIGHT,
                petPhotoPath = pet?.photoPath,
                petBreed = pet?.breed,
                isVoiceEnabled = voiceEnabled
            )
        }
    }

    private fun petName() = pet?.name ?: "반려견"

    /** 반려견 프로필의 창문 알림 기준 (기본 20%) */
    private fun windowAlertPercent() = pet?.windowAlertPercent ?: Pet.DEFAULT_WINDOW_ALERT_PERCENT

    // region 차량 신호

    private suspend fun observeSpeed() {
        vehicleMotionRepository.observeSpeedKmh()
            .catch { Timber.w(it, "속도 관찰 실패") }
            .collect { speed ->
                tracker.onSpeed(System.currentTimeMillis(), speed)?.let(::onDrivingEvent)
                evaluateAlerts()
            }
    }

    private suspend fun observeSteering() {
        vehicleMotionRepository.observeSteeringAngle()
            .catch { Timber.w(it, "조향각 관찰 실패") }
            .collect { angle ->
                tracker.onSteering(System.currentTimeMillis(), angle)?.let(::onDrivingEvent)
            }
    }

    /** Vehicle SDK 브레이크 페달 → 페달 없이 줄어든 속도(내리막 등)는 급제동에서 제외 */
    private suspend fun observeBrake() {
        vehicleMotionRepository.observeBrakePedalPressed()
            .catch { Timber.w(it, "브레이크 페달 관찰 실패 - 속도만으로 판단") }
            .collect { pressed -> tracker.onBrakePedal(System.currentTimeMillis(), pressed) }
    }

    /** FusedLocation SDK 진행 방향 → 횡가속도로 급회전 판단 */
    private suspend fun observeHeading() {
        vehicleMotionRepository.observeLocation()
            .catch { Timber.w(it, "위치 관찰 실패 - 조향각만으로 판단") }
            .collect { location ->
                tracker.onHeading(
                    location.timestampMillis,
                    location.headingDeg
                )?.let(::onDrivingEvent)
            }
    }

    /**
     * NaviHelper SDK: 다가오는 급커브 · 과속방지턱을 미리 말해주고(코칭),
     * 그 구간의 사건은 감점 절반, 사건 없이 지나가면 가점. 목적지에 도착하면 주행 끝.
     */
    private suspend fun observeNavi() {
        naviRepository.observeEvents()
            .catch { Timber.w(it, "내비 안내 관찰 실패 - 경로 안내 없이 진행") }
            .collect { event ->
                when (event) {
                    is NaviEvent.UpcomingTurns -> onUpcomingTurns(event.turns)
                    is NaviEvent.UpcomingCautions -> onUpcomingCautions(event.cautions)
                    is NaviEvent.Destination -> {
                        destinationName = event.name.ifBlank { destinationName }
                        _uiState.update {
                            it.copy(
                                destinationName = destinationName,
                                remainDistanceM = event.remainDistanceM,
                                remainTimeSec = event.remainTimeSec
                            )
                        }
                    }

                    is NaviEvent.RouteStarted -> {
                        destinationName = event.name.ifBlank { null }
                        _uiState.update {
                            it.copy(
                                destinationName = destinationName,
                                remainDistanceM = event.distanceM,
                                remainTimeSec = event.durationSec
                            )
                        }
                        say(VoiceKey.ROUTE, "길 안내를 시작했어요. 도착하면 이동 기록을 정리해 드릴게요.")
                    }

                    // 도착 근처에서 안내가 끝나면 도착으로 본다. 멀리서 끝나면 목적지만 지운다
                    NaviEvent.RouteEnded -> {
                        val remain = _uiState.value.remainDistanceM
                        if (remain != null &&
                            remain <= ARRIVAL_DISTANCE_M
                        ) {
                            finish()
                        } else {
                            clearDestination()
                        }
                    }

                    NaviEvent.RouteCancelled -> clearDestination()
                    is NaviEvent.Arrived -> {
                        destinationName = event.name.ifBlank { destinationName }
                        finish()
                    }
                    is NaviEvent.SpeedLimit -> Unit
                }
            }
    }

    private fun clearDestination() {
        destinationName = null
        _uiState.update {
            it.copy(destinationName = null, remainDistanceM = null, remainTimeSec = null)
        }
    }

    /** 목적지가 없을 때: Pleos 내비 화면을 열어 목적지를 고르게 한다 */
    fun openNavigation() {
        viewModelScope.launch {
            naviRepository.openNavigation().onFailure { Timber.w(it, "내비 화면 열기 실패") }
        }
    }

    private fun onUpcomingTurns(turns: List<UpcomingTurn>) {
        val sharp = turns.filter { it.kind == TurnKind.SHARP }.minByOrNull { it.distanceM }
        val expectsTurning = sharp != null && sharp.distanceM <= CONTEXT_DISTANCE_M
        updateRoadContext(tracker.roadContext.copy(expectsTurning = expectsTurning))
        if (sharp != null && sharp.distanceM in 1..COACH_DISTANCE_M) {
            sayRoutine(
                VoiceKey.COACH_TURN,
                "${sharp.distanceM.roundTo50()}m 앞 급커브예요. 미리 천천히 가면 ${petName().iGa} 편해요."
            )
        }
    }

    private fun onUpcomingCautions(cautions: List<UpcomingCaution>) {
        val nearest = cautions
            .filter { it.kind != CautionKind.OTHER }
            .minByOrNull { it.distanceM }
        val expectsBraking = nearest != null && nearest.distanceM <= CONTEXT_DISTANCE_M
        updateRoadContext(tracker.roadContext.copy(expectsBraking = expectsBraking))
        if (nearest != null && nearest.distanceM in 1..COACH_DISTANCE_M) {
            val place = when (nearest.kind) {
                CautionKind.SPEED_BUMP -> "과속방지턱이"
                CautionKind.SCHOOL_ZONE -> "어린이보호구역이"
                CautionKind.DOWNHILL -> "내리막이"
                CautionKind.NARROW_ROAD -> "좁은 길이"
                CautionKind.SLIPPERY -> "미끄러운 길이"
                CautionKind.OTHER -> return
            }
            sayRoutine(VoiceKey.COACH_CAUTION, "곧 $place 있어요. 천천히 지나가면 ${petName().iGa} 편해요.")
        }
    }

    /** 예고 구간에 들어오면 시각을 기억하고, 빠져나갈 때 그 사이 사건이 없으면 가점 */
    private fun updateRoadContext(context: RoadContext) {
        val wasAnticipating =
            tracker.roadContext.expectsBraking || tracker.roadContext.expectsTurning
        val isAnticipating = context.expectsBraking || context.expectsTurning
        val now = System.currentTimeMillis()
        if (!wasAnticipating && isAnticipating) pendingAnticipation = now
        if (wasAnticipating && !isAnticipating) {
            pendingAnticipation?.let { enteredAt ->
                if (!tracker.hasEventSince(enteredAt)) tracker.onSmoothPass()
            }
            pendingAnticipation = null
        }
        tracker.roadContext = context
    }

    /** P → D → P 가 되면 주행 끝 (기어 P 전환 시 도착 리포트 자동 표시) */
    private suspend fun observeGear() {
        var hasLeftPark = false
        gearRepository.observeIsParked()
            .catch { Timber.w(it, "기어 관찰 실패 - 뒤로가기로 종료") }
            .collect { isParked ->
                if (!isParked) {
                    hasLeftPark = true
                } else if (hasLeftPark) {
                    finish()
                }
            }
    }

    private suspend fun observeTemperature() {
        val seat = pet?.seat ?: PetSeat.REAR_RIGHT
        hvacRepository.observeSeatTemperature(seat)
            .catch { Timber.w(it, "좌석 온도 관찰 실패") }
            .collect { temperature ->
                _uiState.update { it.copy(seatTemperature = temperature) }
                evaluateAlerts()
            }
    }

    private suspend fun observeWindows() {
        windowRanges = windowRepository.getSupportedWindows()
            .onFailure { Timber.w(it, "창문 범위 조회 실패") }
            .getOrNull() ?: return
        val targets = (pet?.seat ?: PetSeat.REAR_RIGHT).nearbyWindows().filter {
            it in windowRanges
        }
        targets.forEach { window ->
            viewModelScope.launch {
                windowRepository.observeWindowPosition(window)
                    .catch { Timber.w(it, "창문 위치 관찰 실패: $window") }
                    .collect { position ->
                        val range = windowRanges[window] ?: return@collect
                        windowOpenPercent[window] = position?.let { range.percentOf(it) } ?: 0
                        evaluateAlerts()
                    }
            }
        }
    }

    private suspend fun tick() {
        while (viewModelScope.isActive) {
            val now = System.currentTimeMillis()
            if (tracker.onTick(now)) {
                sayRoutine(VoiceKey.RECOVER, "${petName().iGa} 다시 편안해졌어요.")
            }
            refreshScoreState(now)
            delay(TICK_MS)
        }
    }

    // endregion

    private fun onDrivingEvent(event: DrivingEvent) {
        val name = petName()
        if (event.isDanger) {
            // POCO 기준점 '위험' (4.0m/s² 이상): 빈도 설정과 상관없이 바로 말한다
            say(VoiceKey.DANGER, "많이 흔들렸어요. ${name.iGa} 괜찮은지 정차 후 확인해 주세요.")
        } else {
            onComfortEvent(event, name)
        }
        _uiState.update { it.copy(lastEventType = event.type) }
        refreshScoreState(event.atMillis)
    }

    private fun onComfortEvent(event: DrivingEvent, name: String) {
        when (event.type) {
            DrivingEventType.HARD_BRAKE ->
                sayRoutine(VoiceKey.BRAKE, "급제동이 있었어요. 앞차와 거리를 조금 더 두면 ${name.iGa} 편해요.")

            DrivingEventType.HARD_ACCEL ->
                sayRoutine(VoiceKey.ACCEL, "출발이 조금 빨랐어요. 천천히 밟으면 ${name.iGa} 편해요.")

            DrivingEventType.SHARP_TURN ->
                sayRoutine(VoiceKey.TURN, "커브가 이어져요. 조금 천천히 가면 ${name.iGa} 편해요.")
        }
    }

    private fun refreshScoreState(now: Long) {
        _uiState.update {
            it.copy(
                score = tracker.score,
                mood = tracker.mood(now, it.isTemperatureOff),
                hardBrakeCount = tracker.count(DrivingEventType.HARD_BRAKE),
                sharpTurnCount = tracker.count(DrivingEventType.SHARP_TURN),
                calmMinutes = tracker.calmMinutes(now),
                elapsedMinutes = ((now - startedAt) / 60_000L).toInt()
            )
        }
    }

    // region 알림 (창문이 온도보다 먼저)

    private fun evaluateAlerts() {
        val speed = tracker.currentSpeedKmh ?: 0f
        val openWindow = windowOpenPercent.maxByOrNull { it.value }
        val windowDanger = openWindow != null &&
            openWindow.value >= windowAlertPercent() &&
            speed >= WINDOW_ALERT_SPEED_KMH
        if (openWindow == null || openWindow.value < windowAlertPercent()) isWindowDismissed = false
        if (windowDanger && !isWindowAlertActive && !isWindowDismissed) {
            windowAlertCount++
            ask(VoiceKey.ALERT_WINDOW, "${petName()} 옆 창문이 열려 있어요. 닫을까요?")
        }
        isWindowAlertActive = windowDanger && !isWindowDismissed

        val temperature = _uiState.value.seatTemperature
        val standard = temperatureStandard
        // POCO 기준점 ①: 더움 알림(이상) · 추움 알림(이하). 경계값도 알림 대상
        val isHot = temperature != null && standard.isHot(temperature)
        val isOff = temperature != null && (isHot || standard.isCold(temperature))
        val isDanger = temperature != null && standard.isDanger(temperature)
        if (isOff && !isTemperatureEpisode) {
            isTemperatureEpisode = true
            tracker.onTemperatureAlert()
            ask(
                VoiceKey.ALERT_TEMPERATURE,
                "${seatLabel().iGa} ${temperature!!.roundToInt()}도예요. " +
                    "${TARGET_CELSIUS.roundToInt()}도로 ${if (isHot) "낮출까요" else "높일까요"}?"
            )
        }
        // 29.5°C 이상은 모든 유형 즉시 위험 경고 (닫아 둔 알림도 다시 띄움)
        if (isDanger && !isTemperatureDanger) {
            isTemperatureDanger = true
            isTemperatureDismissed = false
            say(
                VoiceKey.TEMPERATURE_DANGER,
                "${seatLabel().iGa} ${temperature!!.roundToInt()}도로 위험해요. 바로 에어컨을 켜 주세요."
            )
        }
        if (!isDanger) isTemperatureDanger = false
        if (!isOff) {
            isTemperatureEpisode = false
            isTemperatureDismissed = false
        }

        val alert = when {
            isWindowAlertActive && openWindow != null ->
                DrivingAlert.Window(openWindow.key, openWindow.value, speed.roundToInt())

            isOff && !isTemperatureDismissed -> DrivingAlert.Temperature(
                celsius = temperature!!,
                isHot = isHot,
                isDanger = isDanger,
                comfortRange = standard.comfort,
                typeLabel = standard.label,
                targetCelsius = TARGET_CELSIUS
            )

            else -> null
        }
        _uiState.update { it.copy(alert = alert, isTemperatureOff = isOff) }
    }

    fun closeWindow() = adjustWindow(toPercent = 0)

    /** "괜찮아" - 창문은 그대로 두고 이번 알림만 닫는다 */
    fun dismissWindowAlert() {
        stopListening()
        isWindowDismissed = true
        evaluateAlerts()
    }

    /** 5cm만 열기 - 환기는 허용 */
    fun openWindowSlightly() = adjustWindow(toPercent = VENT_PERCENT)

    private fun adjustWindow(toPercent: Int) {
        val alert = _uiState.value.alert as? DrivingAlert.Window ?: return
        val range = windowRanges[alert.window] ?: return
        stopListening()
        isWindowDismissed = true
        evaluateAlerts()
        viewModelScope.launch {
            val position = range.min + (range.max - range.min) * toPercent / 100
            windowRepository.setWindowPosition(alert.window, position)
                .onFailure { Timber.e(it, "창문 제어 실패") }
        }
    }

    fun setTargetTemperature() {
        val seat = pet?.seat ?: PetSeat.REAR_RIGHT
        stopListening()
        isTemperatureDismissed = true
        evaluateAlerts()
        viewModelScope.launch {
            hvacRepository.setSeatTemperature(seat, TARGET_CELSIUS)
                .onFailure { Timber.e(it, "공조 제어 실패") }
        }
    }

    fun dismissTemperatureAlert() {
        stopListening()
        isTemperatureDismissed = true
        evaluateAlerts()
    }

    // endregion

    // region 음성 (같은 멘트 3분에 1번 · 빈도 설정 반영)

    /** 알림·출발 안내: 빈도와 상관없이 말한다 (같은 멘트 3분 제한만). 실제로 말했으면 true */
    private fun say(key: VoiceKey, text: String): Boolean {
        val now = System.currentTimeMillis()
        val last = lastSpokenAt[key]
        if (last != null && now - last < SAME_LINE_INTERVAL_MS) return false
        lastSpokenAt[key] = now
        speakNow(text)
        return _uiState.value.isVoiceEnabled
    }

    /** 중복 제한 없이 바로 말한다 (대답에 대한 확인 멘트) */
    private fun speakNow(text: String) {
        _uiState.update { it.copy(voiceMessage = text) }
        if (_uiState.value.isVoiceEnabled) {
            viewModelScope.launch {
                ttsRepository.speak(text).onFailure { Timber.w(it, "TTS 실패") }
            }
        }
    }

    /**
     * 알림 질문 → STT로 대답 듣기.
     * "괜찮아" = 닫기, "창문 닫아" · "응" = 제안대로, "5센티만" = 조금 열기. 못 알아들으면 화면 버튼 그대로.
     */
    private fun ask(key: VoiceKey, question: String) {
        if (!say(key, question)) return
        listenJob?.cancel()
        listenJob = viewModelScope.launch {
            // TTS가 질문을 끝낼 때까지 기다린다 (내 목소리를 대답으로 듣지 않게)
            delay(SPEECH_BASE_MS + question.length * SPEECH_PER_CHAR_MS)
            _uiState.update { it.copy(isListening = true, heardText = null) }
            val heard = sttRepository.listenOnce(LISTEN_TIMEOUT_MS)
                .onFailure { Timber.w(it, "STT 실패 - 화면 버튼으로") }
                .getOrNull()
            _uiState.update { it.copy(isListening = false, heardText = heard) }
            onVoiceReply(VoiceReply.parse(heard))
        }
    }

    private fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        _uiState.update { it.copy(isListening = false) }
    }

    private fun onVoiceReply(reply: VoiceReply) {
        when (val alert = _uiState.value.alert) {
            is DrivingAlert.Window -> when (reply) {
                VoiceReply.ACCEPT -> {
                    closeWindow()
                    speakNow("창문을 닫을게요.")
                }
                VoiceReply.VENT -> {
                    openWindowSlightly()
                    speakNow("환기되게 조금만 열어 둘게요.")
                }
                VoiceReply.DECLINE -> {
                    dismissWindowAlert()
                    speakNow("알겠어요. 그대로 둘게요.")
                }
                VoiceReply.UNKNOWN -> Unit
            }

            is DrivingAlert.Temperature -> when (reply) {
                VoiceReply.ACCEPT -> {
                    setTargetTemperature()
                    speakNow("${alert.targetCelsius.roundToInt()}도로 맞출게요.")
                }
                VoiceReply.DECLINE -> {
                    dismissTemperatureAlert()
                    speakNow("알겠어요. 지금 온도로 둘게요.")
                }
                VoiceReply.VENT, VoiceReply.UNKNOWN -> Unit
            }

            null -> Unit
        }
    }

    /** 주행 사건 안내: 적게 = 말하지 않음, 보통 = 3분에 1번까지, 자주 = 모든 이벤트 */
    private fun sayRoutine(key: VoiceKey, text: String) {
        val now = System.currentTimeMillis()
        val allowed = when (voiceFrequency) {
            VoiceFrequency.LOW -> false
            VoiceFrequency.NORMAL -> now - lastRoutineSpokenAt >= SAME_LINE_INTERVAL_MS
            VoiceFrequency.HIGH -> true
        }
        if (!allowed) return
        lastRoutineSpokenAt = now
        say(key, text)
    }

    // endregion

    /** 주행 끝: 이동 기록 저장 후 도착 리포트로 */
    fun finish() {
        if (isFinishing) return
        isFinishing = true
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val trip = TripSummary(
                startedAtMillis = startedAt,
                endedAtMillis = now,
                destination = destinationName,
                distanceKm = tracker.totalDistanceKm,
                comfortScore = tracker.score,
                hardBrake = tracker.stat(DrivingEventType.HARD_BRAKE),
                hardAccel = tracker.stat(DrivingEventType.HARD_ACCEL),
                sharpTurn = tracker.stat(DrivingEventType.SHARP_TURN),
                calmRecovery = tracker.recoveryStat(),
                temperatureAlert = tracker.temperatureStat(),
                windowAlertCount = windowAlertCount,
                calmMinutes = tracker.calmMinutes(now),
                timeline = tracker.timeline(now)
            )
            tripRepository.saveTrip(trip)
                .onSuccess { id -> _uiState.update { it.copy(finishedTripId = id) } }
                .onFailure {
                    Timber.e(it, "이동 기록 저장 실패")
                    isFinishing = false
                }
        }
    }

    private fun seatLabel(): String = when (pet?.seat) {
        PetSeat.PASSENGER -> "조수석"
        else -> "뒷좌석"
    }

    private enum class VoiceKey {
        START,
        BRAKE,
        ACCEL,
        TURN,
        RECOVER,
        COACH_TURN,
        COACH_CAUTION,
        ALERT_WINDOW,
        ALERT_TEMPERATURE,
        TEMPERATURE_DANGER,
        DANGER,
        ROUTE
    }

    companion object {
        const val TARGET_CELSIUS = PocoStandard.SUGGEST_CELSIUS
        private const val WINDOW_ALERT_SPEED_KMH = 30f
        private const val VENT_PERCENT = 10
        private const val SAME_LINE_INTERVAL_MS = 3 * 60_000L
        private const val TICK_MS = 5_000L

        /** 이만큼 남기고 길 안내가 끝나면 도착으로 본다 */
        private const val ARRIVAL_DISTANCE_M = 300

        /** 한국어 TTS 말하기 시간 어림: 0.8초 + 글자당 0.16초 */
        private const val SPEECH_BASE_MS = 800L
        private const val SPEECH_PER_CHAR_MS = 160L
        private const val LISTEN_TIMEOUT_MS = 7_000L

        /** 이 거리 안의 예고 구간에서 생긴 사건은 감점 절반 */
        private const val CONTEXT_DISTANCE_M = 150

        /** 이 거리 안에 들어오면 미리 음성 코칭 */
        private const val COACH_DISTANCE_M = 300
    }
}

private fun WindowPositionRange.percentOf(position: Int): Int {
    val span = (max - min).coerceAtLeast(1)
    return ((position - min) * 100 / span).coerceIn(0, 100)
}

/** 반려동물 자리 옆 창문 */
fun PetSeat.nearbyWindows(): List<CarWindow> = when (this) {
    PetSeat.REAR_LEFT -> listOf(CarWindow.REAR_LEFT)
    PetSeat.REAR_RIGHT -> listOf(CarWindow.REAR_RIGHT)
    PetSeat.PASSENGER -> listOf(CarWindow.PASSENGER)
}

/** 287m → 300m 처럼 안내하기 좋은 단위로 */
private fun Int.roundTo50(): Int = ((this + 25) / 50 * 50).coerceAtLeast(50)
