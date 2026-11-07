package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.vehicle.api.Window
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.listener.WindowPositionListener
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.WindowDataSource
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class WindowDataSourceImpl @Inject constructor(
    private val window: Window,
    private val sdkManager: PleosSdkManager
) : WindowDataSource {
    override suspend fun getWindowPositionCapabilities(): Map<WindowArea, AreaBounds<Int>> {
        sdkManager.ensureVehicle()
        return suspendCancellableCoroutine { continuation ->
            window.checkWindowPositionCapabilities(
                onComplete = { _, areas -> continuation.resume(areas) },
                onFailed = { e -> continuation.resumeWithException(e) }
            )
        }
    }

    override suspend fun getCurrentWindowPosition(area: WindowArea): Int? {
        sdkManager.ensureVehicle()
        return suspendCancellableCoroutine { continuation ->
            window.getCurrentWindowPosition(
                area = area,
                onComplete = { position -> continuation.resume(position) },
                onFailed = { e -> continuation.resumeWithException(e) }
            )
        }
    }

    override suspend fun setWindowPosition(area: WindowArea, position: Int): Int? {
        sdkManager.ensureVehicle()
        return suspendCancellableCoroutine { continuation ->
            window.setWindowPosition(
                area = area,
                value = position,
                onComplete = { _, result -> continuation.resume(result) },
                onFailed = { e -> continuation.resumeWithException(e) }
            )
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
        window.registerWindowPosition(area, listener)
        awaitClose { window.unregisterWindowPosition(area, listener) }
    }
}
