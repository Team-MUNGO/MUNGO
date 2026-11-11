package com.example.poco.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object POCOTheme {
    val colors: POCOColors
        @Composable @ReadOnlyComposable
        get() = LocalPOCOColorsProvider.current
    val typography: POCOTypography
        @Composable @ReadOnlyComposable
        get() = LocalPOCOTypographyProvider.current
}

@Composable
fun ProvidePOCOColorsAndTypography(
    colors: POCOColors,
    typography: POCOTypography,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalPOCOColorsProvider provides colors,
        LocalPOCOTypographyProvider provides typography,
        content = content
    )
}

/**
 * @param darkTheme 기본은 시스템 설정을 따른다 (Pleos 야간 테마 포함)
 */
@Composable
fun POCOTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    ProvidePOCOColorsAndTypography(
        colors = if (darkTheme) darkPOCOColors else defaultPOCOColors,
        typography = defaultPOCOTypography
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
            content = content
        )
    }
}
