package com.example.poco.data.pleos.datasource

import com.example.poco.data.model.VehicleLocation
import kotlinx.coroutines.flow.Flow

interface LocationDataSource {
    /** FusedLocation SDK 위치 업데이트 */
    fun observeLocation(): Flow<VehicleLocation>
}
