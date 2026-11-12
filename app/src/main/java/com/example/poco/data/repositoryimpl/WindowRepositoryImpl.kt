package com.example.poco.data.repositoryimpl

import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.WindowValue
import com.example.poco.core.util.suspendRunCatching
import com.example.poco.data.model.CarWindow
import com.example.poco.data.model.WindowPositionRange
import com.example.poco.data.pleos.datasource.WindowDataSource
import com.example.poco.data.repository.WindowRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class WindowRepositoryImpl @Inject constructor(private val windowDataSource: WindowDataSource) :
    WindowRepository {
    override suspend fun getSupportedWindows(): Result<Map<CarWindow, WindowPositionRange>> =
        suspendRunCatching {
            val (config, areas) = windowDataSource.getWindowPositionCapabilities()
            areas.mapNotNull { (area, bounds) ->
                area.toCarWindow()?.let {
                    it to
                        WindowPositionRange(
                            bounds.minValue,
                            bounds.maxValue,
                            canRead =
                            config.accessType == AccessType.READ ||
                                config.accessType == AccessType.READ_WRITE,
                            canWrite =
                            config.accessType == AccessType.WRITE ||
                                config.accessType == AccessType.READ_WRITE
                        )
                }
            }.toMap()
        }

    override suspend fun getWindowPosition(window: CarWindow): Result<Int?> =
        suspendRunCatching { windowDataSource.getCurrentWindowPosition(window.toWindowArea()) }

    override suspend fun setWindowPosition(window: CarWindow, position: Int): Result<Int?> =
        suspendRunCatching {
            windowDataSource.setWindowPosition(window.toWindowArea(), position)
        }

    override fun observeWindowPosition(window: CarWindow): Flow<Int?> =
        windowDataSource.observeWindowPosition(window.toWindowArea())
}

private fun CarWindow.toWindowArea(): WindowArea = WindowArea(
    when (this) {
        CarWindow.DRIVER -> WindowValue.ROW_1_LEFT
        CarWindow.PASSENGER -> WindowValue.ROW_1_RIGHT
        CarWindow.REAR_LEFT -> WindowValue.ROW_2_LEFT
        CarWindow.REAR_RIGHT -> WindowValue.ROW_2_RIGHT
    }
)

private fun WindowArea.toCarWindow(): CarWindow? = when (values.singleOrNull()) {
    WindowValue.ROW_1_LEFT -> CarWindow.DRIVER
    WindowValue.ROW_1_RIGHT -> CarWindow.PASSENGER
    WindowValue.ROW_2_LEFT -> CarWindow.REAR_LEFT
    WindowValue.ROW_2_RIGHT -> CarWindow.REAR_RIGHT
    else -> null
}
