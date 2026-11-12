package com.example.poco.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poco.core.theme.POCOTheme
import com.example.poco.data.model.ShakeLevel
import com.example.poco.data.model.TimelineBar

/** 이벤트 막대 = mango/500 */
val BrakeColor = com.example.poco.core.theme.MungoPalette.mango500

fun ShakeLevel.color(): Color = when (this) {
    ShakeLevel.BRAKE -> BrakeColor
    ShakeLevel.TURN -> com.example.poco.core.theme.MungoPalette.amber400
    ShakeLevel.CALM -> RingTrack
}

/**
 * 흔들림 타임라인. 막대가 높을수록 흔들림이 컸다는 뜻.
 * [labels]는 아래에 균등 배치 (예: 출발 · 중간 · 도착)
 */
@Composable
fun ShakeTimeline(
    bars: List<TimelineBar>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    showLegend: Boolean = false
) {
    val colors = POCOTheme.colors
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bar ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(bar.intensity.coerceIn(0.15f, 1f))
                        .background(
                            if (bar.level ==
                                ShakeLevel.CALM
                            ) {
                                colors.borderDefault
                            } else {
                                bar.level.color()
                            },
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row {
            labels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = POCOTheme.typography.hmiCaption,
                    color = colors.textSecondary,
                    textAlign = when (index) {
                        0 -> TextAlign.Start
                        labels.lastIndex -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (showLegend) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(BrakeColor, "급제동")
                LegendItem(com.example.poco.core.theme.MungoPalette.amber400, "급회전")
                LegendItem(RingTrack, "평온")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = POCOTheme.typography.hmiCaption,
            color = POCOTheme.colors.textSecondary
        )
    }
}
