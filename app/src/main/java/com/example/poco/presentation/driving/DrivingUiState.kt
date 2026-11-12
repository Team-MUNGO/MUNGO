package com.example.poco.presentation.driving

import com.example.poco.data.model.DrivingSessionState

/** The snapshot is session-owned, so navigation and recomposition do not restart the timer. */
data class DrivingUiState(val session: DrivingSessionState = DrivingSessionState())
