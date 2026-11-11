package com.example.poco.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poco.R
import com.example.poco.core.theme.POCOTheme

/*
 * POCO 로고 (원본: design/brand/POCO_LOGO.png · POCO_LOGO_DARK.png)
 * res/drawable-nodpi 의 이미지는 원본에서 바깥 배경을 투명하게 하고 잘라 줄인 것
 * - poco_logo_light/dark: 글자 로고 (가로:세로 ≈ 3.8:1)
 * - poco_puppy_light/dark: 강아지 O만 (정사각형) → 기본 프로필 · 작은 아이콘
 * - ic_launcher_poco_foreground: 앱 아이콘 (흰 바탕 위 강아지)
 */

/** POCO 글자 로고. 라이트 = 남색, 야간 = 라임 */
@Composable
fun PocoWordmark(modifier: Modifier = Modifier, height: Dp = 28.dp) {
    val painter = painterResource(
        if (POCOTheme.colors.isDark) R.drawable.poco_logo_dark else R.drawable.poco_logo_light
    )
    val size = painter.intrinsicSize
    val ratio = if (size.height > 0f) size.width / size.height else LOGO_RATIO
    Image(
        painter = painter,
        contentDescription = "POCO",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(height)
            .aspectRatio(ratio)
    )
}

/** 강아지 얼굴 마크 (앱 아이콘 모양). 라이트 = 남색 원 + 흰 얼굴, 야간 = 라임 원 + 남색 얼굴 */
@Composable
fun PocoMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(
            if (POCOTheme.colors.isDark) R.drawable.poco_puppy_dark else R.drawable.poco_puppy_light
        ),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

private const val LOGO_RATIO = 3.8f
