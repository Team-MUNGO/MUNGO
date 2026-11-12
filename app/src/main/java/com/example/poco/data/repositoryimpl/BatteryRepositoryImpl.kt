package com.example.poco.data.repositoryimpl

import com.example.poco.core.util.suspendRunCatching
import com.example.poco.data.pleos.datasource.BatteryDataSource
import com.example.poco.data.repository.BatteryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class BatteryRepositoryImpl @Inject constructor(private val batteryDataSource: BatteryDataSource) :
    BatteryRepository {
    override fun observeBatteryPercent(): Flow<Int> = flow {
        suspendRunCatching { batteryDataSource.getStateOfCharge() }
            .onFailure { Timber.w(it, "배터리 잔량 읽기 실패") }
            .getOrNull()
            ?.let { emit(it) }
        batteryDataSource.observeStateOfCharge()
            .catch { Timber.w(it, "배터리 잔량 관찰 실패") }
            .collect { emit(it) }
    }.distinctUntilChanged()
}
