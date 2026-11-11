package com.example.poco.data.repository

import kotlinx.coroutines.flow.Flow

interface GearRepository {
    /** 기어가 P인지 실시간으로 알려준다 */
    fun observeIsParked(): Flow<Boolean>
}
