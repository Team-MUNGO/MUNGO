package com.example.poco.data.pleos.datasourceimpl

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import com.example.poco.data.model.VehicleLocation
import com.example.poco.data.pleos.PleosSdkManager
import com.example.poco.data.pleos.datasource.LocationDataSource
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationDataSourceImpl @Inject constructor(
    private val fusedLocation: FusedLocation,
    private val sdkManager: PleosSdkManager
) : LocationDataSource {
    override fun observeLocation(): Flow<VehicleLocation> = callbackFlow {
        sdkManager.ensureLocation()
        val listener =
            object : FusedLocationListener {
                // 위치 데이터가 아직 없으면 null이 올 수 있다 (SDK 문서)
                override fun onChanged(location: LocationData?) {
                    if (location == null) return
                    trySend(
                        VehicleLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            headingDeg = location.heading,
                            confidence = location.confidence,
                            timestampMillis = location.timestampMillis
                        )
                    )
                }

                override fun onError(e: Exception) {
                    close(e)
                }
            }
        fusedLocation.registerFusedLocationCallback(listener)
        awaitClose { fusedLocation.unregisterFusedLocationCallback(listener) }
    }
}
