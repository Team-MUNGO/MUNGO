package com.example.poco.presentation.permission

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class PermissionViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState = _uiState.asStateFlow()
}
