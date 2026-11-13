package com.example.poco.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object MungoTheme {
    val colors: MungoColors
        @Composable @ReadOnlyComposable
        get() = LocalMungoColorsProvider.current
    val typography: MungoTypography
        @Composable @ReadOnlyComposable
        get() = LocalMungoTypographyProvider.current
}

@Composable
fun ProvideMungoColorsAndTypography(
    colors: MungoColors,
    typography: MungoTypography,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalMungoColorsProvider provides colors,
        LocalMungoTypographyProvider provides typography,
        content = content
    )
}

/**
 * @param darkTheme 기본은 시스템 설정을 따른다 (Pleos 야간 테마 포함)
 */
@Composable
fun MungoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    ProvideMungoColorsAndTypography(
        colors = if (darkTheme) darkMungoColors else defaultMungoColors,
        typography = defaultMungoTypography
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
            content = content
        )
    }
}
