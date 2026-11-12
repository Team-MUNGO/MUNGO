package com.example.poco.data.repository

import kotlinx.coroutines.flow.Flow

/** 첫 실행 온보딩(권한 동의) 진행 여부 */
interface OnboardingRepository {
    val isPermissionCompleted: Flow<Boolean>

    suspend fun setPermissionCompleted(): Result<Unit>
}
