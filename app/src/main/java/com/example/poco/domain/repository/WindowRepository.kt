package com.example.poco.domain.repository

import com.example.poco.domain.model.CarWindow
import com.example.poco.domain.model.WindowPositionRange
import kotlinx.coroutines.flow.Flow

interface WindowRepository {
    suspend fun getSupportedWindows(): Result<Map<CarWindow, WindowPositionRange>>

    suspend fun getWindowPosition(window: CarWindow): Result<Int?>

    suspend fun setWindowPosition(window: CarWindow, position: Int): Result<Int?>

    fun observeWindowPosition(window: CarWindow): Flow<Int?>
}
