package com.example.poco.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poco.R

/*
 * mungo 로고 (기존 로고를 교체) (원본: design/brand/mungo_logo_sheet@4x.png)
 * - MungoWordmark: 가로형 로고 (발바닥 + mungo) = [MungoLogo]
 * - MungoMark: 앱 아이콘 모양 Symbol (망고 둥근 사각 + 흰 발바닥) = res/drawable-nodpi/mungo_symbol.png
 * - 앱 아이콘: mipmap-anydpi/ic_launcher.xml (망고 바탕 + ic_launcher_mungo_foreground)
 */

/** 가로형 로고 래퍼 → [MungoLogo] */
@Composable
fun MungoWordmark(modifier: Modifier = Modifier, height: Dp = 28.dp) {
    MungoLogo(modifier = modifier, height = height)
}

/** mungo Symbol (앱 아이콘 모양) */
@Composable
fun MungoMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.mungo_symbol),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
