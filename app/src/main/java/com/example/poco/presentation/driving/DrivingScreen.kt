package com.example.poco.presentation.driving

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.designsystem.MungoBaseIcon
import com.example.poco.core.designsystem.MungoBaseIconView
import com.example.poco.core.designsystem.PetPhoto
import com.example.poco.core.designsystem.ScoreRing
import com.example.poco.core.designsystem.component.AlertCard
import com.example.poco.core.designsystem.component.MungoButton
import com.example.poco.core.designsystem.component.MungoButtonStyle
import com.example.poco.core.designsystem.component.StatTile
import com.example.poco.core.designsystem.component.StatusTone
import com.example.poco.core.designsystem.component.VoiceCard
import com.example.poco.core.theme.MungoBaseLayout
import com.example.poco.core.theme.MungoBaseRadius
import com.example.poco.core.theme.MungoBaseSize
import com.example.poco.core.theme.MungoSpacing
import com.example.poco.core.theme.MungoStroke
import com.example.poco.core.theme.MungoTheme
import com.example.poco.data.model.CarWindow
import com.example.poco.domain.driving.PetMood
import kotlin.math.roundToInt

/**
 * 주행 화면. 기어 D · 내비 길 안내 시작으로 들어오고,
 * 내비 도착 · 기어 P · '주행 종료'로 끝나면 이동 기록을 저장하고 홈으로 돌아간다.
 */
@Composable
fun DrivingRoute(navigateBack: () -> Unit, viewModel: DrivingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.finishedTripId) {
        if (uiState.finishedTripId != null) navigateBack()
    }

    DrivingScreen(
        uiState = uiState,
        onFinish = viewModel::finish,
        onOpenNavigation = viewModel::openNavigation,
        onCloseWindow = viewModel::closeWindow,
        onVentWindow = viewModel::openWindowSlightly,
        onSetTemperature = viewModel::setTargetTemperature,
        onDismissTemperature = viewModel::dismissTemperatureAlert
    )
}

@Composable
private fun DrivingScreen(
    uiState: DrivingUiState,
    onFinish: () -> Unit,
    onOpenNavigation: () -> Unit,
    onCloseWindow: () -> Unit,
    onVentWindow: () -> Unit,
    onSetTemperature: () -> Unit,
    onDismissTemperature: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MungoTheme.colors.backgroundDefault)
            .safeDrawingPadding()
            .padding(
                start = MungoSpacing.space48,
                end = MungoSpacing.space48,
                top = MungoBaseLayout.screenTop,
                bottom = MungoBaseLayout.screenBottom
            )
    ) {
        Header(uiState = uiState, onFinish = onFinish, onOpenNavigation = onOpenNavigation)
        Spacer(Modifier.height(MungoBaseLayout.drivingColumnGap))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(MungoBaseLayout.drivingColumnGap)
        ) {
            ScoreCard(
                uiState = uiState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(MungoBaseLayout.drivingCardGap)
            ) {
                AlertArea(
                    uiState = uiState,
                    onCloseWindow = onCloseWindow,
                    onVentWindow = onVentWindow,
                    onSetTemperature = onSetTemperature,
                    onDismissTemperature = onDismissTemperature
                )
                ListeningBar(isListening = uiState.isListening, heardText = uiState.heardText)
                VoiceCard(
                    message = uiState.voiceMessage,
                    isVoiceOn = uiState.isVoiceEnabled,
                    onTurnOn = {}
                )
                Spacer(Modifier.weight(1f))
                StatsRow(uiState)
            }
        }
    }
}

// region 머리: 목적지 · 주행 종료

@Composable
private fun Header(uiState: DrivingUiState, onFinish: () -> Unit, onOpenNavigation: () -> Unit) {
    val colors = MungoTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MungoBaseLayout.drivingHeaderHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "주행 중 · ${uiState.elapsedMinutes}분째",
                style = MungoTheme.typography.caption,
                color = colors.textSecondary
            )
            val destination = uiState.destinationName
            if (destination != null) {
                Text(
                    text = destination,
                    style = MungoTheme.typography.headingM,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                remainLine(uiState.remainDistanceM, uiState.remainTimeSec)?.let {
                    Text(
                        text = it,
                        style = MungoTheme.typography.bodyM,
                        color = colors.textSecondary
                    )
                }
            } else {
                Text(
                    text = "${uiState.petName} · 함께 이동 중",
                    style = MungoTheme.typography.headingM,
                    color = colors.textPrimary
                )
                Text(
                    text = "목적지를 정하면 도착할 때 자동으로 기록을 정리해요",
                    style = MungoTheme.typography.bodyM,
                    color = colors.textSecondary
                )
            }
        }
        if (uiState.destinationName == null) {
            MungoButton(
                text = "목적지 설정",
                onClick = onOpenNavigation,
                icon = MungoBaseIcon.TURN_RIGHT,
                style = MungoButtonStyle.SECONDARY
            )
            Spacer(Modifier.width(MungoSpacing.space16))
        }
        MungoButton(text = "주행 종료", onClick = onFinish)
    }
}

/** "남은 12.3km · 약 18분" */
private fun remainLine(distanceM: Int?, timeSec: Int?): String? {
    val parts = buildList {
        distanceM?.let {
            add(if (it >= 1000) "남은 ${"%.1f".format(it / 1000f)}km" else "남은 ${it}m")
        }
        timeSec?.let { add("약 ${(it / 60f).roundToInt().coerceAtLeast(1)}분") }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

// endregion

// region 왼쪽: 컴포트 점수 · 펫 상태

@Composable
private fun ScoreCard(uiState: DrivingUiState, modifier: Modifier = Modifier) {
    val colors = MungoTheme.colors
    Column(
        modifier = modifier
            .background(colors.backgroundCard, RoundedCornerShape(MungoBaseRadius.r28))
            .padding(MungoSpacing.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ScoreRing(
            score = uiState.score,
            label = "컴포트 점수",
            strokeWidth = MungoSpacing.space24,
            scoreStyle = MungoTheme.typography.displayL,
            progressColor = when {
                uiState.score >= 90 -> colors.toggleThumb
                uiState.score >= 70 -> colors.tangerine
                else -> colors.statusError
            },
            modifier = Modifier.size(MungoBaseSize.scoreRingLarge)
        )
        Spacer(Modifier.height(MungoSpacing.space32))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PetPhoto(
                photoPath = uiState.petPhotoPath,
                breed = uiState.petBreed,
                modifier = Modifier.size(MungoBaseSize.iconTileLarge + MungoSpacing.space16)
            )
            Spacer(Modifier.width(MungoSpacing.space20))
            Column {
                Text(
                    text = uiState.petName,
                    style = MungoTheme.typography.titleM,
                    color = colors.textPrimary
                )
                Text(
                    text = uiState.mood.description(),
                    style = MungoTheme.typography.bodyL,
                    color = colors.textSecondary
                )
            }
        }
    }
}

private fun PetMood.description(): String = when (this) {
    PetMood.CALM -> "편안하게 타고 있어요"
    PetMood.HAPPY -> "기분 좋게 가고 있어요"
    PetMood.DIZZY -> "조금 어지러워해요"
    PetMood.HOT -> "자리가 덥거나 추워요"
}

// endregion

// region 오른쪽: 알림 · 음성 대답 · 통계

@Composable
private fun AlertArea(
    uiState: DrivingUiState,
    onCloseWindow: () -> Unit,
    onVentWindow: () -> Unit,
    onSetTemperature: () -> Unit,
    onDismissTemperature: () -> Unit
) {
    when (val alert = uiState.alert) {
        is DrivingAlert.Window -> AlertCard(
            icon = MungoBaseIcon.WINDOW,
            eyebrow = "안전 알림 · ${alert.speedKmh}km/h",
            title = "${alert.window.label()} 창문이 ${alert.openPercent}% 열려 있어요",
            description = "“창문 닫아” 또는 “괜찮아”라고 말해도 돼요",
            primaryText = "창문 닫기",
            onPrimary = onCloseWindow,
            secondaryText = "조금만 열기",
            onSecondary = onVentWindow,
            tone = StatusTone.HOT
        )

        is DrivingAlert.Temperature -> AlertCard(
            icon = MungoBaseIcon.THERMOMETER,
            eyebrow = (if (alert.isDanger) "위험 · " else "온도 알림 · ") + "${alert.typeLabel} 쾌적 " +
                "${alert.comfortRange.start.roundToInt()}~${alert.comfortRange.endInclusive.roundToInt()}°C",
            title = "${uiState.petName} 자리 ${alert.celsius.roundToInt()}°C" +
                if (alert.isHot) " · 더워요" else " · 추워요",
            description = "“맞춰 줘” 또는 “괜찮아”라고 말해도 돼요",
            primaryText = "${alert.targetCelsius.roundToInt()}°C로 맞추기",
            onPrimary = onSetTemperature,
            secondaryText = "괜찮아요",
            onSecondary = onDismissTemperature,
            tone = if (alert.isHot) StatusTone.HOT else StatusTone.COLD
        )

        null -> Unit
    }
}

/** 알림 질문 뒤 STT로 듣는 중 · 알아들은 말 */
@Composable
private fun ListeningBar(isListening: Boolean, heardText: String?) {
    if (!isListening && heardText.isNullOrBlank()) return
    val colors = MungoTheme.colors
    val shape = RoundedCornerShape(MungoBaseRadius.r24)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MungoBaseSize.touchMin)
            .background(if (isListening) colors.limeSoft else colors.backgroundCard, shape)
            .border(MungoStroke.card, colors.borderDefault, shape)
            .padding(horizontal = MungoSpacing.space24, vertical = MungoSpacing.space12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(MungoBaseSize.iconTile)
                .background(colors.accentFill, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MungoBaseIconView(
                MungoBaseIcon.SPEAKER,
                tint = colors.onAccentFill,
                modifier = Modifier.size(MungoBaseSize.icon28)
            )
        }
        Spacer(Modifier.width(MungoSpacing.space16))
        Text(
            text = if (isListening) {
                "듣고 있어요 · “괜찮아” “창문 닫아”처럼 말해 보세요"
            } else {
                "“$heardText” 라고 들었어요"
            },
            style = MungoTheme.typography.titleM,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatsRow(uiState: DrivingUiState) {
    val colors = MungoTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(MungoSpacing.space16)) {
        StatTile(
            label = "급제동",
            value = "${uiState.hardBrakeCount}회",
            dotColor = colors.tangerine,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "급회전",
            value = "${uiState.sharpTurnCount}회",
            dotColor = colors.coldAccent,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "편안한 시간",
            value = "${uiState.calmMinutes}분",
            dotColor = colors.toggleThumb,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "자리 온도",
            value = uiState.seatTemperature?.let { "${it.roundToInt()}°C" } ?: "-",
            dotColor = if (uiState.isTemperatureOff) colors.tangerine else colors.successAccent,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun CarWindow.label(): String = when (this) {
    CarWindow.REAR_LEFT -> "뒷좌석 왼쪽"
    CarWindow.REAR_RIGHT -> "뒷좌석 오른쪽"
    CarWindow.PASSENGER -> "조수석"
    else -> "옆"
}

// endregion

@Preview(widthDp = 1700, heightDp = 1100)
@Composable
private fun DrivingScreenPreview() {
    MungoTheme(darkTheme = false) {
        DrivingScreen(
            uiState = DrivingUiState(
                petName = "현댕이",
                score = 92,
                voiceMessage = "현댕이 옆 창문이 열려 있어요. 닫을까요?",
                isListening = true,
                destinationName = "서울숲 공원",
                remainDistanceM = 12300,
                remainTimeSec = 1080,
                seatTemperature = 23f,
                alert = DrivingAlert.Window(CarWindow.REAR_RIGHT, 40, 52)
            ),
            onFinish = {},
            onOpenNavigation = {},
            onCloseWindow = {},
            onVentWindow = {},
            onSetTemperature = {},
            onDismissTemperature = {}
        )
    }
}
