package com.example.poco.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 디자인 시스템 선 아이콘 (DS-04: 24 그리드 · 선 2.2 · 끝 둥글게).
 * 아이콘 라이브러리가 없어 직접 그림 → TODO: 피그마에서 SVG로 받으면 vector drawable로 교체
 */
enum class MungoBaseIcon {
    CAR,
    WINDOW,
    THERMOMETER,
    SPEAKER,
    SPEAKER_OFF,
    SHIELD,
    LOCK,
    CAMERA,
    CHEVRON_LEFT,
    CHEVRON_RIGHT,
    PLUS,
    PHONE,
    PARKING,
    WARNING,
    PLAY,
    CHECK,
    CLOCK,
    HEART_PULSE,
    TURN_RIGHT,
    SEAT,
    SETTINGS
}

@Composable
fun MungoBaseIconView(icon: MungoBaseIcon, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val w = (s * 0.09f).coerceAtLeast(1.5.dp.toPx())
        val stroke = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val draw = IconDraw(this, s, w, stroke, tint)
        when (icon) {
            MungoBaseIcon.CAR -> draw.car()
            MungoBaseIcon.WINDOW -> draw.window()
            MungoBaseIcon.THERMOMETER -> draw.thermometer()
            MungoBaseIcon.SPEAKER -> draw.speaker()
            MungoBaseIcon.SPEAKER_OFF -> draw.speakerOff()
            MungoBaseIcon.SHIELD -> draw.shield()
            MungoBaseIcon.LOCK -> draw.lock()
            MungoBaseIcon.CAMERA -> draw.camera()
            MungoBaseIcon.CHEVRON_LEFT -> draw.polyline(0.62f to 0.2f, 0.34f to 0.5f, 0.62f to 0.8f)
            MungoBaseIcon.CHEVRON_RIGHT -> draw.polyline(
                0.38f to 0.2f,
                0.66f to 0.5f,
                0.38f to 0.8f
            )
            MungoBaseIcon.PLUS -> draw.plus()
            MungoBaseIcon.PHONE -> draw.phone()
            MungoBaseIcon.PARKING -> draw.parking()
            MungoBaseIcon.WARNING -> draw.warning()
            MungoBaseIcon.PLAY -> draw.play()
            MungoBaseIcon.CHECK -> draw.polyline(0.2f to 0.52f, 0.42f to 0.72f, 0.8f to 0.3f)
            MungoBaseIcon.CLOCK -> draw.clock()
            MungoBaseIcon.HEART_PULSE -> draw.heartPulse()
            MungoBaseIcon.TURN_RIGHT -> draw.turnRight()
            MungoBaseIcon.SEAT -> draw.seat()
            MungoBaseIcon.SETTINGS -> draw.settings()
        }
    }
}

private class IconDraw(
    val scope: DrawScope,
    val s: Float,
    val w: Float,
    val stroke: Stroke,
    val tint: Color
) {
    fun p(x: Float, y: Float) = Offset(x * s, y * s)
    fun sz(x: Float, y: Float) = Size(x * s, y * s)
    val r = CornerRadius(s * 0.12f)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        scope.drawLine(tint, p(x1, y1), p(x2, y2), w, StrokeCap.Round)

    fun polyline(vararg points: Pair<Float, Float>, closed: Boolean = false) {
        val path = Path().apply {
            points.forEachIndexed { i, (x, y) ->
                if (i ==
                    0
                ) {
                    moveTo(x * s, y * s)
                } else {
                    lineTo(x * s, y * s)
                }
            }
            if (closed) close()
        }
        scope.drawPath(path, tint, style = stroke)
    }

    fun car() {
        polyline(
            0.12f to 0.64f,
            0.12f to 0.48f,
            0.24f to 0.30f,
            0.76f to 0.30f,
            0.88f to 0.48f,
            0.88f to 0.64f,
            closed = true
        )
        line(0.12f, 0.48f, 0.88f, 0.48f)
        line(0.24f, 0.64f, 0.24f, 0.74f)
        line(0.76f, 0.64f, 0.76f, 0.74f)
    }

    fun window() {
        scope.drawRoundRect(tint, p(0.16f, 0.2f), sz(0.68f, 0.6f), r, style = stroke)
        line(0.16f, 0.5f, 0.84f, 0.5f)
        line(0.5f, 0.2f, 0.5f, 0.8f)
    }

    fun thermometer() {
        scope.drawRoundRect(
            tint,
            p(0.42f, 0.1f),
            sz(0.16f, 0.54f),
            CornerRadius(s * 0.08f),
            style = stroke
        )
        scope.drawCircle(tint, radius = s * 0.13f, center = p(0.5f, 0.76f))
    }

    fun speaker() {
        polyline(
            0.12f to 0.40f,
            0.28f to 0.40f,
            0.48f to 0.22f,
            0.48f to 0.78f,
            0.28f to 0.60f,
            0.12f to 0.60f,
            closed = true
        )
        scope.drawArc(tint, -45f, 90f, false, p(0.48f, 0.36f), sz(0.18f, 0.28f), style = stroke)
        scope.drawArc(tint, -50f, 100f, false, p(0.50f, 0.24f), sz(0.34f, 0.52f), style = stroke)
    }

    fun shield() {
        val path = Path().apply {
            moveTo(0.5f * s, 0.1f * s)
            lineTo(0.82f * s, 0.22f * s)
            quadraticTo(0.82f * s, 0.70f * s, 0.5f * s, 0.9f * s)
            quadraticTo(0.18f * s, 0.70f * s, 0.18f * s, 0.22f * s)
            close()
        }
        scope.drawPath(path, tint, style = stroke)
        polyline(0.36f to 0.50f, 0.46f to 0.60f, 0.64f to 0.40f)
    }

    fun lock() {
        scope.drawRoundRect(tint, p(0.22f, 0.44f), sz(0.56f, 0.44f), r, style = stroke)
        scope.drawArc(tint, 180f, 180f, false, p(0.32f, 0.14f), sz(0.36f, 0.5f), style = stroke)
    }

    fun camera() {
        scope.drawRoundRect(tint, p(0.1f, 0.3f), sz(0.8f, 0.52f), r, style = stroke)
        polyline(0.34f to 0.3f, 0.40f to 0.18f, 0.60f to 0.18f, 0.66f to 0.3f)
        scope.drawCircle(tint, radius = s * 0.14f, center = p(0.5f, 0.56f), style = stroke)
    }

    fun plus() {
        line(0.5f, 0.18f, 0.5f, 0.82f)
        line(0.18f, 0.5f, 0.82f, 0.5f)
    }

    fun phone() {
        scope.drawRoundRect(tint, p(0.3f, 0.1f), sz(0.4f, 0.8f), r, style = stroke)
        line(0.46f, 0.78f, 0.54f, 0.78f)
    }

    fun parking() {
        scope.drawRoundRect(tint, p(0.16f, 0.16f), sz(0.68f, 0.68f), r, style = stroke)
        line(0.40f, 0.30f, 0.40f, 0.70f)
        scope.drawArc(tint, -90f, 180f, false, p(0.30f, 0.30f), sz(0.32f, 0.24f), style = stroke)
        line(0.40f, 0.30f, 0.46f, 0.30f)
        line(0.40f, 0.54f, 0.46f, 0.54f)
    }

    fun warning() {
        polyline(0.5f to 0.14f, 0.88f to 0.82f, 0.12f to 0.82f, closed = true)
        line(0.5f, 0.38f, 0.5f, 0.58f)
        scope.drawCircle(tint, radius = w * 0.7f, center = p(0.5f, 0.70f))
    }

    fun play() {
        val path = Path().apply {
            moveTo(0.3f * s, 0.2f * s)
            lineTo(0.8f * s, 0.5f * s)
            lineTo(0.3f * s, 0.8f * s)
            close()
        }
        scope.drawPath(path, tint)
    }

    fun clock() {
        scope.drawCircle(tint, radius = s * 0.38f, center = p(0.5f, 0.5f), style = stroke)
        polyline(0.5f to 0.28f, 0.5f to 0.5f, 0.66f to 0.6f)
    }

    fun heartPulse() {
        val heart = Path().apply {
            moveTo(0.5f * s, 0.84f * s)
            cubicTo(0.1f * s, 0.56f * s, 0.06f * s, 0.24f * s, 0.30f * s, 0.20f * s)
            cubicTo(0.42f * s, 0.18f * s, 0.48f * s, 0.26f * s, 0.5f * s, 0.32f * s)
            cubicTo(0.52f * s, 0.26f * s, 0.58f * s, 0.18f * s, 0.70f * s, 0.20f * s)
            cubicTo(0.94f * s, 0.24f * s, 0.90f * s, 0.56f * s, 0.5f * s, 0.84f * s)
        }
        scope.drawPath(heart, tint, style = stroke)
        polyline(
            0.2f to 0.5f,
            0.38f to 0.5f,
            0.46f to 0.38f,
            0.56f to 0.62f,
            0.62f to 0.5f,
            0.8f to 0.5f
        )
    }

    fun turnRight() {
        val path = Path().apply {
            moveTo(0.24f * s, 0.84f * s)
            lineTo(0.24f * s, 0.56f * s)
            quadraticTo(0.24f * s, 0.36f * s, 0.46f * s, 0.36f * s)
            lineTo(0.78f * s, 0.36f * s)
        }
        scope.drawPath(path, tint, style = stroke)
        polyline(0.62f to 0.20f, 0.78f to 0.36f, 0.62f to 0.52f)
    }

    fun seat() {
        polyline(0.30f to 0.16f, 0.30f to 0.62f, 0.76f to 0.62f)
        scope.drawRoundRect(tint, p(0.2f, 0.12f), sz(0.2f, 0.56f), r, style = stroke)
        line(0.40f, 0.62f, 0.40f, 0.86f)
        line(0.70f, 0.62f, 0.70f, 0.86f)
    }

    fun settings() {
        // 톱니 8개 + 바깥 원 + 가운데 구멍
        repeat(8) { i ->
            val angle = Math.toRadians(i * 45.0)
            val inner = 0.30f
            val outer = 0.42f
            line(
                0.5f + (inner * kotlin.math.cos(angle)).toFloat(),
                0.5f + (inner * kotlin.math.sin(angle)).toFloat(),
                0.5f + (outer * kotlin.math.cos(angle)).toFloat(),
                0.5f + (outer * kotlin.math.sin(angle)).toFloat()
            )
        }
        scope.drawCircle(tint, radius = s * 0.28f, center = p(0.5f, 0.5f), style = stroke)
        scope.drawCircle(tint, radius = s * 0.11f, center = p(0.5f, 0.5f), style = stroke)
    }

    fun speakerOff() {
        polyline(
            0.12f to 0.40f,
            0.28f to 0.40f,
            0.48f to 0.22f,
            0.48f to 0.78f,
            0.28f to 0.60f,
            0.12f to 0.60f,
            closed = true
        )
        line(0.62f, 0.38f, 0.86f, 0.62f)
        line(0.86f, 0.38f, 0.62f, 0.62f)
    }
}
