package com.example.poco.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.data.repository.OnboardingRepository
import com.example.poco.data.repository.WindowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val windowRepository: WindowRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState = _uiState.asStateFlow()

    init {
        start()
    }

    private fun start() {
        viewModelScope.launch {
            val permissionCompleted = onboardingRepository.isPermissionCompleted.first()

            // 권한 동의 전에는 SDK를 켜지 않는다 (PleosSdkManager 지연 초기화 원칙)
            if (permissionCompleted) launch { connectVehicle() }

            delay(SPLASH_DURATION_MS)
            _uiState.update {
                it.copy(
                    destination = if (permissionCompleted) {
                        SplashDestination.HOME
                    } else {
                        SplashDestination.PERMISSION
                    }
                )
            }
        }
    }

    private suspend fun connectVehicle() = coroutineScope {
        // Pleos 즉시성 원칙: 0.25초 안에 끝나지 않으면 연결 중 상태를 보여준다
        val showStatusJob = launch {
            delay(STATUS_VISIBLE_AFTER_MS)
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTING) }
        }
        val result = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            windowRepository.getSupportedWindows()
        }
        showStatusJob.cancel()
        if (result?.isSuccess != true) Timber.w("차량 연결 지연: $result")
        _uiState.update {
            it.copy(
                connectionStatus = if (result?.isSuccess == true) {
                    ConnectionStatus.CONNECTED
                } else {
                    ConnectionStatus.DELAYED
                }
            )
        }
    }

    companion object {
        const val SPLASH_DURATION_MS = 2_500L
        private const val STATUS_VISIBLE_AFTER_MS = 250L
        private const val CONNECT_TIMEOUT_MS = 2_000L
    }
}
