package com.example.poco.data.repository

import com.example.poco.data.model.CarWindow
import com.example.poco.data.model.WindowPositionRange
import kotlinx.coroutines.flow.Flow

interface WindowRepository {
    suspend fun getSupportedWindows(): Result<Map<CarWindow, WindowPositionRange>>

    suspend fun getWindowPosition(window: CarWindow): Result<Int?>

    suspend fun setWindowPosition(window: CarWindow, position: Int): Result<Int?>

    fun observeWindowPosition(window: CarWindow): Flow<Int?>
}
