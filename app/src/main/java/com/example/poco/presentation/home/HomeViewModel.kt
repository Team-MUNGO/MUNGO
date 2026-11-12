package com.example.poco.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.core.common.ToastMessenger
import com.example.poco.data.model.NaviEvent
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.repository.DepartureRepository
import com.example.poco.data.repository.GearRepository
import com.example.poco.data.repository.HvacRepository
import com.example.poco.data.repository.NaviRepository
import com.example.poco.data.repository.PetRepository
import com.example.poco.data.repository.TripRepository
import com.example.poco.data.repository.WindowRepository
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val gearRepository: GearRepository,
    private val hvacRepository: HvacRepository,
    private val windowRepository: WindowRepository,
    private val departureRepository: DepartureRepository,
    private val tripRepository: TripRepository,
    private val naviRepository: NaviRepository,
    private val toastMessenger: ToastMessenger
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observePet()
        observeGear()
        observeRouteStart()
        observeMeal()
        observeTripSettings()
        observeLastTrip()
        observeWeekTrips()
        checkWindows()
    }

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

    /** 오늘의 동승자 = 첫 번째로 등록한 펫. 자리가 바뀌면 그 자리 온도를 다시 읽는다 */
    private fun observePet() {
        viewModelScope.launch {
            petRepository.observePets()
                .map { it.firstOrNull() }
                .catch { Timber.e(it, "펫 조회 실패") }
                .collectLatest { pet ->
                    _uiState.update { it.copy(pet = pet) }
                    pet?.let { loadSeatTemperature(it) }
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
            gearRepository.observeIsParked()
                .catch { Timber.w(it, "기어 상태 관찰 실패") }
                .collect { isParked ->
                    _uiState.update {
                        // 정차(P)에서 기어를 바꾸면 자동으로 주행 화면으로
                        val leftPark = it.isParked == true && !isParked
                        it.copy(
                            isParked = isParked,
                            shouldStartDriving =
                            it.shouldStartDriving || leftPark
                        )
                    }
                }
        }
    }

    /** Pleos 내비에서 길 안내를 시작하면 주행 화면으로 (기어 D와 같은 효과) */
    private fun observeRouteStart() {
        viewModelScope.launch {
            naviRepository.observeEvents()
                .catch { Timber.w(it, "내비 관찰 실패 - 기어로만 주행 시작") }
                .collect { event ->
                    if (event is NaviEvent.RouteStarted) {
                        _uiState.update { it.copy(shouldStartDriving = true) }
                    }
                }
        }
    }

    /** 펫이 없으면 자리를 바꿀 수 없으니 먼저 펫 추가로 안내 */
    fun onSeatChangeWithoutPet() {
        toastMessenger.show("먼저 펫을 추가해 주세요")
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

    fun setRearTempWatchEnabled(enabled: Boolean) {
        viewModelScope.launch { departureRepository.setRearTempWatchEnabled(enabled) }
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
