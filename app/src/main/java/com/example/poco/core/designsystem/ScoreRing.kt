package com.example.poco.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poco.core.theme.MungoTheme

val RingTrack = Color(0xFFE6E6E6) // neutral/200
private val RingStart = Color(0xFFFFB257) // mango/300
private val RingEnd = Color(0xFFFF8400) // mango/500

/**
 * 컴포트 점수 링. 애니메이션 없음 (주행 중 시선 분산 방지)
 * @param progressColor 지정하면 단색(예: 망고), 없으면 주황 그라데이션
 */
@Composable
fun ScoreRing(
    score: Int,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    scoreStyle: TextStyle = MungoTheme.typography.hmiTitle,
    label: String? = null,
    progressColor: Color? = null
) {
    val colors = MungoTheme.colors
    val track = colors.borderDefault
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // 글자는 링 안쪽 원에 맞춘다: 숫자는 안쪽 지름의 34%, 라벨은 9%까지 (큰 글자 스타일이어도 링과 겹치지 않게)
        // 화면 분할로 칸이 아주 좁아지면 링 두께보다 작아질 수 있다: 음수 글자 크기로 앱이 죽지 않게 0 이상으로
        val inner = (minOf(maxWidth, maxHeight) - strokeWidth * 2).coerceAtLeast(1.dp)
        val scoreSize =
            with(density) { minOf(scoreStyle.fontSize.toPx(), (inner * 0.34f).toPx()).toSp() }
        val labelStyle = MungoTheme.typography.caption
        val labelSize =
            with(density) { minOf(labelStyle.fontSize.toPx(), (inner * 0.09f).toPx()).toSp() }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(size.minDimension - stroke, size.minDimension - stroke)
            val topLeft = Offset(
                (size.width - arcSize.width) / 2,
                (size.height - arcSize.height) / 2
            )
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            rotate(-90f) {
                drawArc(
                    brush = progressColor?.let { SolidColor(it) }
                        ?: Brush.sweepGradient(listOf(RingStart, RingEnd, RingStart)),
                    startAngle = 0f,
                    sweepAngle = 360f * score.coerceIn(0, 100) / 100f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = scoreStyle.copy(fontSize = scoreSize, lineHeight = scoreSize),
                color = colors.textPrimary
            )
            label?.let {
                Text(
                    text = it,
                    style = labelStyle.copy(fontSize = labelSize, lineHeight = labelSize * 1.3f),
                    color = colors.textSecondary
                )
            }
        }
    }
}
