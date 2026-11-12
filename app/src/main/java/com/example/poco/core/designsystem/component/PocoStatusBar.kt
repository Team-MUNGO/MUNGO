package com.example.poco.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.poco.core.designsystem.PocoWordmark
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.theme.PocoLayout
import com.example.poco.core.util.toClock
import kotlinx.coroutines.delay

/**
 * 앱 상단 바: 왼쪽 시각 · 기어 상태, 가운데 POCO 로고.
 * (LTE·배터리는 차량 시스템 UI가 보여주므로 앱에서는 그리지 않음)
 */
@Composable
fun PocoStatusBar(
    gearLabel: String?,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            now = System.currentTimeMillis()
        }
    }
    val colors = POCOTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PocoLayout.statusBarHeight)
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = now.toClock(),
                    style = POCOTheme.typography.titleM,
                    color = colors.textPrimary
                )
                gearLabel?.let {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = it,
                        style = POCOTheme.typography.bodyM,
                        color = colors.textSecondary
                    )
                }
            }
            PocoWordmark(modifier = Modifier.align(Alignment.Center), height = 36.dp)
            Box(modifier = Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
        HorizontalDivider(color = colors.borderDefault)
    }
}
