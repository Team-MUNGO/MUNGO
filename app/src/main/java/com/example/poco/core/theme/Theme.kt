package com.example.poco.core.theme

import androidx.compose.material3.MaterialTheme
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

@Composable
fun POCOTheme(content: @Composable () -> Unit) {
    ProvidePOCOColorsAndTypography(
        colors = defaultPOCOColors,
        typography = defaultPOCOTypography
    ) {
        MaterialTheme(
            content = content
        )
    }
}
