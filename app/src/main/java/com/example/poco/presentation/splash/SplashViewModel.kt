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

    /** 갈 곳 (DataStore를 읽으면 정해짐). 인트로 영상이 끝나야 실제로 이동한다 */
    private var pendingDestination: SplashDestination? = null
    private var introFinished = false

    private fun start() {
        viewModelScope.launch {
            val permissionCompleted = onboardingRepository.isPermissionCompleted.first()

            // 권한 동의 전에는 SDK를 켜지 않는다 (PleosSdkManager 지연 초기화 원칙)
            if (permissionCompleted) launch { connectVehicle() }

            pendingDestination = if (permissionCompleted) {
                SplashDestination.HOME
            } else {
                SplashDestination.ONBOARDING
            }
            moveOnIfReady()
        }
    }

    private var fallbackStarted = false

    /**
     * 인트로 화면이 처음 보일 때. 영상이 멈추거나 끝 알림이 안 와도 스플래시에 갇히지 않게
     * 이때부터 [MAX_INTRO_MS] 뒤에는 넘어간다 (앱 프로세스 시작이 느려도 영상은 끝까지 보이도록)
     */
    fun onIntroShown() {
        if (fallbackStarted) return
        fallbackStarted = true
        viewModelScope.launch {
            delay(MAX_INTRO_MS)
            onIntroFinished()
        }
    }

    /** 인트로 영상이 끝났을 때 (재생 실패 · 화면 터치로 건너뛰기 포함) */
    fun onIntroFinished() {
        if (introFinished) return
        introFinished = true
        moveOnIfReady()
    }

    private fun moveOnIfReady() {
        val destination = pendingDestination ?: return
        if (!introFinished) return
        _uiState.update { it.copy(destination = destination) }
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
        /** 인트로 영상(4.6초)보다 넉넉하게 */
        private const val MAX_INTRO_MS = 8_000L
        private const val STATUS_VISIBLE_AFTER_MS = 250L
        private const val CONNECT_TIMEOUT_MS = 2_000L
    }
}
