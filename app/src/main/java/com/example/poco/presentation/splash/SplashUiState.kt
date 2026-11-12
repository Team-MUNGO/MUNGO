package com.example.poco.presentation.splash

data class SplashUiState(
    val petName: String = "초코",
    val connectionStatus: ConnectionStatus = ConnectionStatus.PREPARING,
    val destination: SplashDestination? = null
)

enum class ConnectionStatus {
    /** 첫 실행 (권한 동의 전) - SDK에 연결하지 않음 */
    PREPARING,
    CONNECTING,
    CONNECTED,
    DELAYED
}

enum class SplashDestination {
    /** 첫 실행: 온보딩 소개 → 권한 → 펫 프로필 */
    ONBOARDING,
    HOME
}
