package com.example.poco.presentation.petprofile

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PetProfileRoute(viewModel: PetProfileViewModel = hiltViewModel()) {
    PetProfileScreen()
}

@Composable
private fun PetProfileScreen() {
}
