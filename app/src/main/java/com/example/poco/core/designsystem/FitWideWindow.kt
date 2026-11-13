package com.example.poco.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 화면 분할(2분할 · 3분할)로 앱 창이 [minWidth]보다 좁아지면 화면 전체를 같은 비율로 줄여 그린다.
 * 가로 화면 기준으로 만든 홈 · 온보딩 · 마이펫 화면이 좁은 창에서 찌그러지거나 글자가 세로로 쌓이지 않게 한다.
 * 창이 충분히 넓으면 아무것도 바꾸지 않는다.
 */
@Composable
fun FitWideWindow(minWidth: Dp = 1600.dp, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= minWidth || maxWidth <= 0.dp) {
            content()
        } else {
            val density = LocalDensity.current
            val scale = (maxWidth / minWidth).coerceIn(0.3f, 1f)
            CompositionLocalProvider(LocalDensity provides Density(density.density * scale, density.fontScale)) {
                Box(Modifier.fillMaxSize()) { content() }
            }
        }
    }
}
