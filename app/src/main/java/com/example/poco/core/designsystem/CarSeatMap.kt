package com.example.poco.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poco.core.theme.MungoStroke
import com.example.poco.core.theme.MungoTheme
import com.example.poco.data.model.PetSeat

/**
 * 위에서 본 차 (탑승 위치). 너비:높이 = 2:3, 위쪽이 차 앞.
 * - [petSeat]: 반려견 자리 = lime-soft 면 + success/accent 테두리 + 얼굴, 옆 창문 = 초록 막대(알림 기준 창문)
 * - [previousSeat]: 자리 바꾸기 중 지금 자리 = 점선 + "지금"
 * - [showLabels]: 운전석 글자 표시 (모달처럼 크게 그릴 때)
 */
@Composable
fun CarSeatMap(
    petSeat: PetSeat?,
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    previousSeat: PetSeat? = null,
    showLabels: Boolean = false,
    photoPath: String? = null,
    breed: String? = null
) {
    val colors = MungoTheme.colors
    val height = width * 1.5f
    val seatW = 0.30f
    val seatH = 0.22f
    val leftX = 0.15f
    val rightX = 0.55f
    val frontY = 0.30f
    val rearY = 0.58f
    val driver = leftX to frontY

    fun seatOrigin(seat: PetSeat): Pair<Float, Float> = when (seat) {
        PetSeat.REAR_LEFT -> leftX to rearY
        PetSeat.REAR_RIGHT -> rightX to rearY
        PetSeat.PASSENGER -> rightX to frontY
    }

    val petOrigin = petSeat?.let(::seatOrigin)
    val previousOrigin = previousSeat?.takeIf { it != petSeat }?.let(::seatOrigin)

    Box(modifier = modifier.size(width, height)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val line = MungoStroke.card.toPx()
            val corner = CornerRadius(w * 0.06f)

            // 차체 · 앞/뒤 유리 · 차 앞 표시(▲)
            drawRoundRect(
                color = colors.borderInput,
                topLeft = Offset(w * 0.06f, h * 0.04f),
                size = Size(w * 0.88f, h * 0.92f),
                cornerRadius = CornerRadius(w * 0.3f),
                style = Stroke(line * 1.5f)
            )
            drawLine(
                colors.borderInput,
                Offset(w * 0.2f, h * 0.22f),
                Offset(w * 0.8f, h * 0.22f),
                line
            )
            drawLine(
                colors.borderInput,
                Offset(w * 0.2f, h * 0.86f),
                Offset(w * 0.8f, h * 0.86f),
                line
            )

            // 창문 자리 (옅은 막대)
            listOf(frontY, rearY).forEach { y ->
                listOf(w * 0.06f, w * 0.94f).forEach { x ->
                    drawLine(
                        colors.borderDefault,
                        Offset(x, h * (y + 0.02f)),
                        Offset(x, h * (y + seatH * 0.85f)),
                        3.dp.toPx(),
                        StrokeCap.Round
                    )
                }
            }

            // 좌석 4개
            listOf(
                leftX to frontY,
                rightX to frontY,
                leftX to rearY,
                rightX to rearY
            ).forEach { origin ->
                val topLeft = Offset(w * origin.first, h * origin.second)
                val seatSize = Size(w * seatW, h * seatH * 0.9f)
                val isPet = origin == petOrigin
                val isPrevious = origin == previousOrigin
                drawRoundRect(
                    color = when {
                        isPet -> colors.limeSoft
                        isPrevious -> colors.backgroundCard
                        else -> colors.fieldBackground
                    },
                    topLeft = topLeft,
                    size = seatSize,
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = when {
                        isPet -> colors.successAccent
                        isPrevious -> colors.borderStrong
                        else -> colors.borderInput
                    },
                    topLeft = topLeft,
                    size = seatSize,
                    cornerRadius = corner,
                    style = Stroke(
                        width = if (isPet) MungoStroke.control.toPx() else line,
                        pathEffect = if (isPrevious) {
                            PathEffect.dashPathEffect(
                                floatArrayOf(
                                    MungoStroke.DASH_ON * density,
                                    MungoStroke.DASH_OFF * density
                                )
                            )
                        } else {
                            null
                        }
                    )
                )
            }

            // 운전석 핸들
            drawCircle(
                color = colors.textSecondary,
                radius = w * 0.06f,
                center = Offset(w * (driver.first + seatW * 0.3f), h * (frontY - 0.03f)),
                style = Stroke(line * 1.5f)
            )

            // 반려견 옆 창문 = 알림 기준 창문 (초록 막대)
            petOrigin?.let { (x, y) ->
                val windowX = if (x == leftX) w * 0.06f else w * 0.94f
                drawLine(
                    color = colors.successAccent,
                    start = Offset(windowX, h * (y + 0.02f)),
                    end = Offset(windowX, h * (y + seatH * 0.85f)),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        val labelWidth = width * seatW
        if (showLabels) {
            SeatLabel("운전석", driver, labelWidth, width, height, seatH)
            previousOrigin?.let { SeatLabel("지금", it, labelWidth, width, height, seatH) }
        }

        // 반려견 얼굴 (사진 → 품종 아바타 → 기본 마크)
        petOrigin?.let { (x, y) ->
            val face = width * 0.22f
            PetPhoto(
                photoPath = photoPath,
                breed = breed,
                modifier = Modifier
                    .offset(
                        x = width * (x + seatW / 2) - face / 2,
                        y = height * (y + seatH * 0.45f) - face / 2
                    )
                    .size(face)
            )
        }
    }
}

@Composable
private fun SeatLabel(
    text: String,
    origin: Pair<Float, Float>,
    labelWidth: Dp,
    width: Dp,
    height: Dp,
    seatH: Float
) {
    Text(
        text = text,
        style = MungoTheme.typography.nano,
        color = MungoTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .offset(x = width * origin.first, y = height * (origin.second + seatH * 0.45f) - 11.dp)
            .width(labelWidth)
    )
}
