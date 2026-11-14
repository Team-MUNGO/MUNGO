package com.example.poco.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.core.common.ToastMessenger
import com.example.poco.core.driving.Reading
import com.example.poco.core.driving.valueOrNull
import com.example.poco.data.model.NaviEvent
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.repository.DepartureRepository
import com.example.poco.data.repository.DrivingSessionRepository
import com.example.poco.data.repository.GearRepository
import com.example.poco.data.repository.HvacRepository
import com.example.poco.data.repository.NaviRepository
import com.example.poco.data.repository.PetRepository
import com.example.poco.data.repository.TripRepository
import com.example.poco.data.repository.WindowRepository
import com.example.poco.data.settings.UserSettingsRepository
import com.example.poco.data.vehicle.VehicleLinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val gearRepository: GearRepository,
    private val hvacRepository: HvacRepository,
    private val windowRepository: WindowRepository,
    private val departureRepository: DepartureRepository,
    private val tripRepository: TripRepository,
    private val naviRepository: NaviRepository,
    private val toastMessenger: ToastMessenger,
    private val vehicleLink: VehicleLinkRepository,
    private val userSettings: UserSettingsRepository,
    private val drivingSession: DrivingSessionRepository
) : ViewModel() {
    private val linked = vehicleLink.state.map { it.linked }.distinctUntilChanged()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeLink()
        observePet()
        observeGear()
        observeRouteStart()
        observeMeal()
        observeTripSettings()
        observeLastTrip()
        observeWeekTrips()
        observeRestInterval()
        observeAirCleaner()
        checkWindows()
    }

    /** 공기 청정 켜짐/꺼짐을 차에서 계속 읽는다. 연동을 해지하면 '–'로 비운다 */
    private fun observeAirCleaner() {
        viewModelScope.launch {
            linked.flatMapLatest { if (it) hvacRepository.observeAirCleaner() else flowOf(null) }
                .catch { Timber.w(it, "공기 청정 관찰 실패") }
                .collect { reading ->
                    if (reading is Reading.Unavailable) Timber.w("공기 청정: ${reading.reason}")
                    _uiState.update { it.copy(isAirCleanerOn = reading?.valueOrNull()) }
                }
        }
    }

    /** 공기 청정 칸을 누르면 켜고 끈다. 화면 값은 차가 알려 준 상태로만 바꾼다 */
    fun toggleAirCleaner() {
        val current = _uiState.value
        val on = current.isAirCleanerOn ?: return
        if (!current.isVehicleLinked || current.isAirCleanerChanging) return
        _uiState.update { it.copy(isAirCleanerChanging = true) }
        viewModelScope.launch {
            hvacRepository.setAirCleaner(!on)
                .onSuccess { applied ->
                    applied?.let { value -> _uiState.update { it.copy(isAirCleanerOn = value) } }
                }
                .onFailure {
                    Timber.w(it, "공기 청정 변경 실패")
                    toastMessenger.show("공기 청정을 바꾸지 못했어요. 차량에서 직접 확인해 주세요")
                }
            _uiState.update { it.copy(isAirCleanerChanging = false) }
        }
    }

    private fun observeRestInterval() {
        viewModelScope.launch {
            userSettings.restIntervalMinutes
                .catch { Timber.w(it, "휴식 간격 조회 실패") }
                .collect { minutes -> _uiState.update { it.copy(restIntervalMinutes = minutes) } }
        }
    }

    /** 차종 이름은 연동 중에만 읽을 수 있다. 해지 후 홈에서도 '그랜저 · 연동이 꺼져 있어요'로 쓰려고 기억해 둔다 */
    private fun loadVehicleName() {
        if (!vehicleLink.isLinked) return
        viewModelScope.launch {
            runCatching { vehicleLink.identity().displayName }
                .onFailure { Timber.w(it, "차종 조회 실패") }
                .getOrNull()
                ?.let { name -> _uiState.update { it.copy(vehicleName = name) } }
        }
    }

    /** 연동을 해지하면 차량 값을 비우고, 다시 연결하면 온도 · 창문을 다시 읽는다 */
    private fun observeLink() {
        viewModelScope.launch {
            var wasLinked = vehicleLink.isLinked
            linked.collect { isLinked ->
                _uiState.update {
                    if (isLinked) {
                        it.copy(isVehicleLinked = true)
                    } else {
                        it.copy(
                            isVehicleLinked = false,
                            isParked = null,
                            seatTemperature = null,
                            isAllWindowsClosed = null,
                            isAirCleanerOn = null,
                            shouldStartDriving = false
                        )
                    }
                }
                if (isLinked && !wasLinked) {
                    _uiState.value.pet?.let { loadSeatTemperature(it) }
                    checkWindows()
                }
                if (isLinked && _uiState.value.vehicleName == null) loadVehicleName()
                wasLinked = isLinked
            }
        }
    }

    /** 차량 연동 다시 켜기 (홈 '다시 연결하기') */
    fun relinkVehicle() = vehicleLink.relink()

    /** 이번 주 월요일 0시부터 요일별 이동 횟수 */
    private fun observeWeekTrips() {
        val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val zone = ZoneId.systemDefault()
        val since = monday.atStartOfDay(zone).toInstant().toEpochMilli()
        viewModelScope.launch {
            tripRepository.observeTripStartsSince(since)
                .catch { Timber.e(it, "이번 주 이동 조회 실패") }
                .collect { starts ->
                    val counts = IntArray(DAYS_IN_WEEK)
                    starts.forEach { millis ->
                        val day = Instant.ofEpochMilli(millis).atZone(zone).dayOfWeek
                        counts[day.value - 1]++
                    }
                    _uiState.update { it.copy(weekTripCounts = counts.toList()) }
                }
        }
    }

    private fun observeLastTrip() {
        viewModelScope.launch {
            tripRepository.observeLastTrip()
                .catch { Timber.e(it, "지난 이동 조회 실패") }
                .collect { trip -> _uiState.update { it.copy(lastTrip = trip) } }
        }
    }

    /** 오늘의 동승자 = 첫 번째로 등록한 펫. 그 자리 온도를 읽고, 이후 바뀌는 값도 따라간다 */
    private fun observePet() {
        viewModelScope.launch {
            petRepository.observePets()
                .map { it.firstOrNull() }
                .catch { Timber.e(it, "펫 조회 실패") }
                .collectLatest { pet ->
                    _uiState.update { it.copy(pet = pet) }
                    if (pet == null) return@collectLatest
                    if (vehicleLink.isLinked) loadSeatTemperature(pet)
                    // 홈에 있는 동안 실내 온도가 바뀌면 칸도 바로 바뀌게 계속 지켜본다
                    hvacRepository.observeSeatTemperature(pet.seat)
                        .catch { Timber.w(it, "좌석 온도 관찰 실패") }
                        .collect { temperature ->
                            if (vehicleLink.isLinked) {
                                _uiState.update { it.copy(seatTemperature = temperature) }
                            }
                        }
                }
        }
    }

    private suspend fun loadSeatTemperature(pet: Pet) {
        hvacRepository.getSeatTemperature(pet.seat)
            .onSuccess { temperature -> _uiState.update { it.copy(seatTemperature = temperature) } }
            .onFailure { Timber.w(it, "좌석 온도 조회 실패") }
    }

    private fun observeGear() {
        viewModelScope.launch {
            linked.flatMapLatest { if (it) gearRepository.observeIsParked() else emptyFlow() }
                .catch { Timber.w(it, "기어 상태 관찰 실패") }
                .collect { isParked ->
                    _uiState.update {
                        // 정차(P)에서 기어를 바꾸면 자동으로 주행 화면으로
                        // 홈이 보일 때 바뀐 기어만 주행 시작으로 본다 (주행 · 요약 화면 뒤에 남아 있을 때 바뀐 건 무시)
                        val leftPark = screenVisible && it.isParked == true && !isParked
                        it.copy(
                            isParked = isParked,
                            shouldStartDriving =
                            it.shouldStartDriving || leftPark
                        )
                    }
                }
        }
    }

    /** 차량 내비에서 길 안내를 시작하면 주행 화면으로 (기어 D와 같은 효과) */
    private fun observeRouteStart() {
        viewModelScope.launch {
            linked.flatMapLatest { if (it) naviRepository.observeEvents() else emptyFlow() }
                .catch { Timber.w(it, "내비 관찰 실패 - 기어로만 주행 시작") }
                .collect { event ->
                    if (event is NaviEvent.RouteStarted && screenVisible) {
                        _uiState.update { it.copy(shouldStartDriving = true) }
                    }
                }
        }
    }

    /** 펫이 없으면 자리를 바꿀 수 없으니 먼저 펫 추가로 안내 */
    fun onSeatChangeWithoutPet() {
        toastMessenger.show("먼저 펫을 추가해 주세요")
    }

    @Volatile
    private var screenVisible = false

    /** 홈 화면이 보이는지 (RESUMED). 다른 화면 뒤에 있을 때 쌓인 주행 시작 신호는 버린다 */
    fun onScreenVisible(visible: Boolean) {
        screenVisible = visible
        if (!visible) {
            _uiState.update { it.copy(shouldStartDriving = false) }
        } else if (drivingSession.state.value.active) {
            // 주행 중에 내비(경유지 추가 등)를 보고 앱을 다시 열면 홈이 아니라 주행 화면으로 돌아간다
            _uiState.update { it.copy(shouldStartDriving = true) }
        }
    }

    fun onDrivingStarted() {
        _uiState.update { it.copy(shouldStartDriving = false) }
    }

    private fun observeMeal() {
        viewModelScope.launch {
            combine(departureRepository.lastMealAt, minuteTicker()) { lastMealAt, now ->
                lastMealAt?.let { ((now - it) / 60_000L).coerceAtLeast(0L) }
            }.distinctUntilChanged()
                .catch { Timber.e(it, "식사 기록 조회 실패") }
                .collect { minutes -> _uiState.update { it.copy(minutesSinceMeal = minutes) } }
        }
    }

    private fun observeTripSettings() {
        viewModelScope.launch {
            combine(
                departureRepository.isVoiceGuideEnabled,
                departureRepository.isRearTempWatchEnabled
            ) { voice, temp -> voice to temp }
                .catch { Timber.e(it, "이동 설정 조회 실패") }
                .collect { (voice, temp) ->
                    _uiState.update {
                        it.copy(isVoiceGuideEnabled = voice, isRearTempWatchEnabled = temp)
                    }
                }
        }
    }

    /** 지원하는 모든 창문이 최소 위치(닫힘)인지 확인 */
    private fun checkWindows() {
        if (!vehicleLink.isLinked) return
        viewModelScope.launch {
            val windows = windowRepository.getSupportedWindows()
                .onFailure { Timber.w(it, "창문 정보 조회 실패") }
                .getOrNull() ?: return@launch
            val allClosed = windows.all { (window, range) ->
                val position = windowRepository.getWindowPosition(window).getOrNull()
                    ?: return@launch
                position <= range.min
            }
            _uiState.update { it.copy(isAllWindowsClosed = allClosed) }
        }
    }

    /** 자리 바꾸기 모달 → 펫 프로필의 탑승 자리도 같이 저장 */
    fun changeSeat(seat: PetSeat) {
        val pet = _uiState.value.pet ?: return
        viewModelScope.launch {
            petRepository.savePet(pet.copy(seat = seat))
                .onSuccess { toastMessenger.show(seatChangedMessage(pet.name, seat)) }
                .onFailure { Timber.e(it, "자리 바꾸기 실패") }
        }
    }

    fun recordMealNow() {
        viewModelScope.launch {
            departureRepository.recordMeal(System.currentTimeMillis())
                .onFailure { Timber.e(it, "식사 기록 실패") }
        }
    }

    fun setVoiceGuideEnabled(enabled: Boolean) {
        viewModelScope.launch { departureRepository.setVoiceGuideEnabled(enabled) }
    }

    private fun minuteTicker(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000L)
        }
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}
