package com.example.poco.data.repository

import com.example.poco.core.driving.Reading
import com.example.poco.data.pleos.datasource.BumpApproach
import com.example.poco.data.pleos.datasource.NaviRouteUpdate
import kotlinx.coroutines.flow.Flow

interface NaviRepository {
    fun observeBumps(): Flow<Reading<List<BumpApproach>>>
    fun observeRoute(): Flow<NaviRouteUpdate>
}
