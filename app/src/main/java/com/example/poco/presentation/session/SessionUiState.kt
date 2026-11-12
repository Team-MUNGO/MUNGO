package com.example.poco.presentation.session

import com.example.poco.data.model.DrivingSessionState

/** The snapshot is session-owned, so navigation and recomposition do not restart the timer. */
data class SessionUiState(val session: DrivingSessionState = DrivingSessionState())
