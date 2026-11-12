package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.vehicle.api.Window
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.listener.WindowPositionListener
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.WindowDataSource
import com.example.poco.data.pleos.datasource.WindowPositionCapabilities
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WindowDataSourceImpl @Inject constructor(
    private val window: Window,
    private val sdkManager: PleosSdkManager
) : WindowDataSource {
    override suspend fun getWindowPositionCapabilities(): WindowPositionCapabilities {
        sdkManager.ensureVehicle()
        return com.example.poco.data.pleos.awaitPleos { success, error ->
            window.checkWindowPositionCapabilities(
                onComplete = { config, areas -> success(config to areas) },
                onFailed = error
            )
        }
    }

    override suspend fun getCurrentWindowPosition(area: WindowArea): Int? {
        sdkManager.ensureVehicle()
        return com.example.poco.data.pleos.awaitPleos { success, error ->
            window.getCurrentWindowPosition(area, success, error)
        }
    }

    override suspend fun setWindowPosition(area: WindowArea, position: Int): Int? {
        sdkManager.ensureVehicle()
        return com.example.poco.data.pleos.awaitPleos { success, error ->
            window.setWindowPosition(area, position, { _, value -> success(value) }, error)
        }
    }

    override fun observeWindowPosition(area: WindowArea): Flow<Int?> = callbackFlow {
        sdkManager.ensureVehicle()
        val listener =
            object : WindowPositionListener {
                override fun onWindowPositionUpdated(position: Int?) {
                    trySend(position)
                }

                override fun onFailed(e: Exception) {
                    close(e)
                }
            }
        try {
            window.registerWindowPosition(area, listener)
            awaitClose { }
        } finally {
            window.unregisterWindowPosition(area, listener)
        }
    }
}
