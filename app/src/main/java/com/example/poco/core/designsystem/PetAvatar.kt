package com.example.poco.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.poco.domain.driving.PetMood

private val EarColor = Color(0xFFC9733A)
private val FaceColor = Color(0xFFF2AE6B)
private val MuzzleColor = Color(0xFFFFF1DE)
private val FeatureColor = Color(0xFF3A2A20)
private val BlushColor = Color(0xFFF38B8B)
private val SweatColor = Color(0xFF7FB8E6)

/**
 * 반려동물(초코) 캐릭터 임시 일러스트. 표정 4가지: 평온 · 웃음 · 어지러움 · 더위
 * TODO: 피그마 캐릭터 에셋을 받으면 표정별 Image(painterResource(...))로 교체
 */
@Composable
fun PetAvatar(modifier: Modifier = Modifier, mood: PetMood = PetMood.CALM) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val c = center
        val earSize = Size(s * 0.24f, s * 0.48f)

        drawOval(EarColor, topLeft = Offset(c.x - s * 0.47f, c.y - s * 0.30f), size = earSize)
        drawOval(EarColor, topLeft = Offset(c.x + s * 0.23f, c.y - s * 0.30f), size = earSize)
        drawCircle(FaceColor, radius = s * 0.36f, center = c)
        drawOval(
            MuzzleColor,
            topLeft = Offset(c.x - s * 0.18f, c.y + s * 0.01f),
            size = Size(s * 0.36f, s * 0.26f)
        )

        val leftEye = Offset(c.x - s * 0.13f, c.y - s * 0.07f)
        val rightEye = Offset(c.x + s * 0.13f, c.y - s * 0.07f)
        when (mood) {
            PetMood.CALM -> {
                drawCircle(FeatureColor, radius = s * 0.04f, center = leftEye)
                drawCircle(FeatureColor, radius = s * 0.04f, center = rightEye)
            }

            PetMood.HAPPY -> {
                happyEye(leftEye, s)
                happyEye(rightEye, s)
            }

            PetMood.DIZZY -> {
                dizzyEye(leftEye, s)
                dizzyEye(rightEye, s)
            }

            PetMood.HOT -> {
                // 반쯤 감긴 눈
                drawLine(
                    FeatureColor,
                    leftEye.minus(Offset(s * 0.05f, 0f)),
                    leftEye.plus(
                        Offset(
                            s * 0.05f,
                            0f
                        )
                    ),
                    s * 0.03f,
                    StrokeCap.Round
                )
                drawLine(
                    FeatureColor,
                    rightEye.minus(Offset(s * 0.05f, 0f)),
                    rightEye.plus(
                        Offset(
                            s * 0.05f,
                            0f
                        )
                    ),
                    s * 0.03f,
                    StrokeCap.Round
                )
            }
        }

        drawOval(
            FeatureColor,
            topLeft = Offset(c.x - s * 0.05f, c.y + s * 0.05f),
            size = Size(s * 0.10f, s * 0.07f)
        )
        if (mood == PetMood.HAPPY) {
            drawArc(
                FeatureColor,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(c.x - s * 0.08f, c.y + s * 0.06f),
                size = Size(s * 0.16f, s * 0.12f),
                style = Stroke(width = s * 0.025f, cap = StrokeCap.Round)
            )
        }

        val blushAlpha = if (mood == PetMood.HOT) 0.9f else 0.55f
        drawCircle(
            BlushColor.copy(alpha = blushAlpha),
            radius = s * 0.045f,
            center = Offset(
                c.x - s * 0.22f,
                c.y + s * 0.08f
            )
        )
        drawCircle(
            BlushColor.copy(alpha = blushAlpha),
            radius = s * 0.045f,
            center = Offset(
                c.x + s * 0.22f,
                c.y + s * 0.08f
            )
        )

        if (mood == PetMood.HOT) sweatDrop(Offset(c.x + s * 0.30f, c.y - s * 0.26f), s)
    }
}

private fun DrawScope.happyEye(center: Offset, s: Float) {
    drawArc(
        FeatureColor,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - s * 0.05f, center.y - s * 0.03f),
        size = Size(s * 0.10f, s * 0.08f),
        style = Stroke(width = s * 0.025f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.dizzyEye(center: Offset, s: Float) {
    drawCircle(
        FeatureColor,
        radius = s * 0.05f,
        center = center,
        style = Stroke(width = s * 0.018f)
    )
    drawCircle(
        FeatureColor,
        radius = s * 0.022f,
        center = center,
        style = Stroke(width = s * 0.018f)
    )
}

private fun DrawScope.sweatDrop(top: Offset, s: Float) {
    val path = Path().apply {
        moveTo(top.x, top.y)
        quadraticTo(top.x + s * 0.06f, top.y + s * 0.09f, top.x, top.y + s * 0.11f)
        quadraticTo(top.x - s * 0.06f, top.y + s * 0.09f, top.x, top.y)
        close()
    }
    drawPath(path, SweatColor)
}
