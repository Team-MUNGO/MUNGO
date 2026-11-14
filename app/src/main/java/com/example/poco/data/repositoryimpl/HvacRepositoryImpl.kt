package com.example.poco.data.repositoryimpl

import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import com.example.poco.core.debug.DebugTemperature
import com.example.poco.core.driving.PetSeat as SessionSeat
import com.example.poco.core.driving.Reading
import com.example.poco.core.driving.valueOrNull
import com.example.poco.core.util.suspendRunCatching
import com.example.poco.data.model.PetSeat
import com.example.poco.data.pleos.datasource.HvacDataSource
import com.example.poco.data.repository.HvacRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeout
import timber.log.Timber

class HvacRepositoryImpl @Inject constructor(private val hvacDataSource: HvacDataSource) :
    HvacRepository {
    /**
     * 실내 현재 온도를 주행 세션과 같은 방식(차가 지원하는 영역 중 반려견 자리를 포함하는 곳)으로 읽는다.
     * 차가 뒷좌석 칸을 따로 주지 않고 '차 전체' 영역으로만 주는 경우가 있어서, 좌석 칸을 직접 고르면 값이 오지 않는다.
     * 현재 온도를 못 읽는 차만 설정 온도로 대신한다.
     */
    override suspend fun getSeatTemperature(seat: PetSeat): Result<Float?> {
        DebugTemperature.celsius.value?.let { return Result.success(it) }
        val current = suspendRunCatching {
            withTimeout(INTERIOR_READ_TIMEOUT_MS) {
                hvacDataSource.observeInteriorTemperature(seat.toSessionSeat()).first()
            }
        }
        current.getOrNull()?.valueOrNull()?.let { return Result.success(it) }
        Timber.w(current.exceptionOrNull(), "실내 현재 온도 조회 실패 - 설정 온도로 대체")
        return firstWorkingZone(seat) { hvacDataSource.getTemperature(it) }
    }

    override suspend fun setSeatTemperature(seat: PetSeat, celsius: Float): Result<Float?> =
        firstWorkingZone(seat) { hvacDataSource.setTemperature(it, celsius) }

    override fun observeSeatTemperature(seat: PetSeat): Flow<Float> {
        val real = hvacDataSource.observeInteriorTemperature(seat.toSessionSeat())
            .mapNotNull { it.valueOrNull() }
            .catch { e ->
                Timber.w(e, "실내 온도 관찰 실패 - 설정 온도로 대체")
                emitAll(hvacDataSource.observeTemperature(SeatArea(seat.candidateZones().last())))
            }
        return DebugTemperature.overriding(real) { it }
    }

    override fun observeInteriorTemperature(seat: SessionSeat): Flow<Reading<Float>> =
        DebugTemperature.overriding(
            hvacDataSource.observeInteriorTemperature(seat)
                .catch { emit(Reading.Unavailable(it.message ?: "실내 온도 수신 실패")) }
        ) { Reading.Value(it) }

    override fun observeAirCleaner(): Flow<Reading<Boolean>> = hvacDataSource.observeAirCleaner()
        .catch { emit(Reading.Unavailable(it.message ?: "공기 청정 상태 수신 실패")) }

    override suspend fun setAirCleaner(on: Boolean): Result<Boolean?> =
        suspendRunCatching { hvacDataSource.setAirCleaner(on) }

    /** 뒷좌석 독립 공조가 없는 차(듀얼 존)도 있으므로 가까운 존부터 차례로 시도한다 */
    private suspend fun firstWorkingZone(
        seat: PetSeat,
        block: suspend (SeatArea) -> Float?
    ): Result<Float?> = suspendRunCatching {
        var lastError: Throwable? = null
        for (value in seat.candidateZones()) {
            val result = suspendRunCatching { block(SeatArea(value)) }
            result.onSuccess { if (it != null) return@suspendRunCatching it }
            lastError = result.exceptionOrNull() ?: lastError
        }
        lastError?.let { throw it }
        null
    }
}

private const val INTERIOR_READ_TIMEOUT_MS = 10_000L

private fun PetSeat.toSessionSeat(): SessionSeat = SessionSeat.valueOf(name)

private fun PetSeat.candidateZones(): List<SeatValue> = when (this) {
    PetSeat.REAR_LEFT -> listOf(SeatValue.ROW_2_LEFT, SeatValue.ROW_1_LEFT)
    PetSeat.REAR_RIGHT -> listOf(SeatValue.ROW_2_RIGHT, SeatValue.ROW_1_RIGHT)
    PetSeat.PASSENGER -> listOf(SeatValue.ROW_1_RIGHT)
}
