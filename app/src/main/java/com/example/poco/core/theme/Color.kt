package com.example.poco.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val primaryMain = Color(0xFF043B78)
val primaryLight = Color(0xFFE8F1FA)
val primaryPressed = Color(0xFF092E55)
val textPrimary = Color(0xFF14293D)
val textSecondary = Color(0xFF587083)
val textOnPrimary = Color(0xFFFFFFFF)
val backgroundDefault = Color(0xFFF4F7FB)
val backgroundCard = Color(0xFFFFFFFF)
val statusSuccess = Color(0xFF2E9E6F)
val statusWarning = Color(0xFFF4A13F)
val statusError = Color(0xFFE35D5D)
val statusInfo = Color(0xFF3E8AE6)
val borderDefault = Color(0xFFDCE6EF)
val logoColor = Color(0xFFA9FF65)
val logoBackground = Color(0xFF121F3D)

@Immutable
data class POCOColors(
    val primaryMain: Color,
    val primaryLight: Color,
    val primaryPressed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnPrimary: Color,
    val backgroundDefault: Color,
    val backgroundCard: Color,
    val statusSuccess: Color,
    val statusWarning: Color,
    val statusError: Color,
    val statusInfo: Color,
    val borderDefault: Color,
    val logoColor: Color,
    val logoBackground: Color
)

val defaultPOCOColors = POCOColors(
    primaryMain = primaryMain,
    primaryLight = primaryLight,
    primaryPressed = primaryPressed,
    textPrimary = textPrimary,
    textSecondary = textSecondary,
    textOnPrimary = textOnPrimary,
    backgroundDefault = backgroundDefault,
    backgroundCard = backgroundCard,
    statusSuccess = statusSuccess,
    statusWarning = statusWarning,
    statusError = statusError,
    statusInfo = statusInfo,
    borderDefault = borderDefault,
    logoColor = logoColor,
    logoBackground = logoBackground
)

val LocalPOCOColorsProvider = staticCompositionLocalOf { defaultPOCOColors }
