package com.example.poco.presentation.permission

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PermissionRoute(viewModel: PermissionViewModel = hiltViewModel()) {
    PermissionScreen()
}

@Composable
private fun PermissionScreen() {
}
