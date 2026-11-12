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
 * 예전 POCO 로고 자리 → mungo 로고로 교체 (원본: design/brand/mungo_logo_sheet@4x.png)
 * - PocoWordmark: 가로형 로고 (발바닥 + mungo) = [MungoLogo]
 * - PocoMark: 앱 아이콘 모양 Symbol (망고 둥근 사각 + 흰 발바닥) = res/drawable-nodpi/mungo_symbol.png
 * - 앱 아이콘: mipmap-anydpi/ic_launcher.xml (망고 바탕 + ic_launcher_mungo_foreground)
 */

/** 가로형 로고. 이름은 호환용으로 남겨 둠 → [MungoLogo] */
@Composable
fun PocoWordmark(modifier: Modifier = Modifier, height: Dp = 28.dp) {
    MungoLogo(modifier = modifier, height = height)
}

/** mungo Symbol (앱 아이콘 모양) */
@Composable
fun PocoMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.mungo_symbol),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
