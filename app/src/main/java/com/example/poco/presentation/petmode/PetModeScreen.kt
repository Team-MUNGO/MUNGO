package com.example.poco.presentation.petmode

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.MungoLogo
import com.example.poco.core.designsystem.PetPhoto
import com.example.poco.core.designsystem.component.PocoButton
import com.example.poco.core.designsystem.component.PocoButtonStyle
import com.example.poco.core.designsystem.component.pressable
import com.example.poco.core.theme.MungoLayout
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.POCOTheme
import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import kotlin.math.roundToInt

@Composable
fun PetModeRoute(navigateBack: () -> Unit, viewModel: PetModeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 보호자가 없는 동안 화면이 꺼지지 않게
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) navigateBack()
    }

    PetModeScreen(
        uiState = uiState,
        onSpeakNow = viewModel::speakVoiceMessage,
        onFinish = viewModel::finish
    )
}

@Composable
private fun PetModeScreen(
    uiState: PetModeUiState,
    onSpeakNow: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .safeDrawingPadding()
            .padding(horizontal = 40.dp, vertical = 32.dp)
    ) {
        TopBar(elapsedMinutes = uiState.elapsedMinutes, onFinish = onFinish)
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(MungoLayout.panelGap)
        ) {
            MessageCard(
                uiState = uiState,
                modifier = Modifier
                    .weight(LEFT_WEIGHT)
                    .fillMaxHeight()
            )
            Column(
                modifier = Modifier
                    .weight(RIGHT_WEIGHT)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(MungoLayout.panelGap)
            ) {
                TemperatureCard(uiState)
                VoiceCard(
                    uiState = uiState,
                    onSpeakNow = onSpeakNow,
                    modifier = Modifier.weight(1f)
                )
                BatteryCard(uiState)
            }
        }
    }
}

// region 위

@Composable
private fun TopBar(elapsedMinutes: Int, onFinish: () -> Unit) {
    val colors = POCOTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        MungoLogo(height = 44.dp)
        Spacer(Modifier.width(24.dp))
        Row(
            modifier = Modifier
                .height(64.dp)
                .background(colors.statusCalmSubtle, CircleShape)
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(colors.statusCalm, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "펫모드",
                style = POCOTheme.typography.title2,
                color = colors.statusCalm
            )
        }
        Spacer(Modifier.width(20.dp))
        Text(
            text = "${elapsedMinutes}분째 · 보호자 외출 중",
            style = POCOTheme.typography.title2.copy(fontWeight = FontWeight.Normal),
            color = colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        PocoButton(
            text = "펫모드 끝내기",
            onClick = onFinish,
            style = PocoButtonStyle.SECONDARY,
            modifier = Modifier.width(240.dp)
        )
    }
}

// endregion

// region 왼쪽: 차 밖에서 보는 안내

@Composable
private fun MessageCard(uiState: PetModeUiState, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    val pet = uiState.pet
    Box(
        modifier = modifier
            .background(colors.bgSurface, RoundedCornerShape(MungoRadius.xl))
            .padding(64.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            InfoChip(
                icon = MungoIcon.PAW_FILLED,
                text = "반려견이 차 안에 있어요",
                background = colors.bgSurfaceSunken,
                content = colors.textSecondary
            )
            Spacer(Modifier.height(36.dp))
            Text(
                text = "보호자가 곧\n돌아와요",
                style = POCOTheme.typography.display2.copy(fontSize = 88.sp, lineHeight = 108.sp),
                color = colors.textPrimary
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = messageBody(uiState),
                style = POCOTheme.typography.body1.copy(fontSize = 32.sp, lineHeight = 48.sp),
                color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth(0.62f)
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ComfortChip(uiState)
                when (uiState.isWindowClosed) {
                    true -> InfoChip(
                        icon = MungoIcon.WINDOW,
                        text = "창문 닫힘",
                        background = colors.bgSurfaceSunken,
                        content = colors.textSecondary
                    )
                    false -> InfoChip(
                        icon = MungoIcon.WINDOW,
                        text = "창문 열림",
                        background = colors.statusRestSubtle,
                        content = colors.statusRest
                    )
                    null -> Unit
                }
            }
        }
        if (pet != null) {
            val size = 440.dp
            PetPhoto(
                photoPath = pet.photoPath,
                breed = pet.breed,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size)
                    .then(
                        // 사진은 동그랗게, 품종 캐릭터(배경 없음)는 그대로
                        if (pet.photoPath != null) Modifier.clip(CircleShape) else Modifier
                    )
            )
        }
    }
}

/** "실내는 22°C로 알맞게 지키고 있어요.\n걱정해 주셔서 고마워요." */
private fun messageBody(uiState: PetModeUiState): String {
    val thanks = "걱정해 주셔서 고마워요."
    val t = uiState.temperature ?: uiState.targetTemperature
        ?: return "온도와 창문을 지켜보고 있어요.\n$thanks"
    val celsius = t.roundToInt()
    val line = when (uiState.comfort) {
        TemperatureComfort.HOT -> "실내가 $celsius°C예요. 시원하게 낮추고 있어요."
        TemperatureComfort.COLD -> "실내가 $celsius°C예요. 따뜻하게 높이고 있어요."
        else -> "실내는 $celsius°C로 알맞게 지키고 있어요."
    }
    return "$line\n$thanks"
}

@Composable
private fun ComfortChip(uiState: PetModeUiState) {
    val colors = POCOTheme.colors
    val name = uiState.pet?.name ?: "반려견"
    when (uiState.comfort) {
        TemperatureComfort.OK -> InfoChip(
            icon = MungoIcon.SHIELD_CHECK,
            text = "$name 쾌적",
            background = colors.statusCalmSubtle,
            content = colors.statusCalm
        )
        TemperatureComfort.HOT -> InfoChip(
            icon = MungoIcon.SUN,
            text = "조금 더워요",
            background = colors.statusAlertSubtle,
            content = colors.statusAlert
        )
        TemperatureComfort.COLD -> InfoChip(
            icon = MungoIcon.SNOWFLAKE,
            text = "조금 추워요",
            background = colors.statusInfoSubtle,
            content = colors.statusInfo
        )
        TemperatureComfort.UNKNOWN -> Unit
    }
}

// endregion

// region 오른쪽

@Composable
private fun TemperatureCard(uiState: PetModeUiState) {
    val colors = POCOTheme.colors
    SideCard {
        CardTitle(icon = MungoIcon.THERMOMETER, title = "실내 온도")
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            val t = uiState.temperature ?: uiState.targetTemperature
            Text(
                text = t?.let { "${it.roundToInt()}°C" } ?: "—",
                style = POCOTheme.typography.h1.copy(fontWeight = FontWeight.Normal),
                color = colors.textPrimary
            )
            Spacer(Modifier.width(16.dp))
            val (note, noteColor) = when (uiState.comfort) {
                TemperatureComfort.HOT -> "조금 더워요" to colors.statusAlert
                TemperatureComfort.COLD -> "조금 추워요" to colors.statusInfo
                TemperatureComfort.OK ->
                    (if (uiState.targetTemperature != null) "설정 온도 유지 중" else "알맞은 온도") to
                        colors.statusCalm
                TemperatureComfort.UNKNOWN -> "차량 정보를 못 읽었어요" to colors.textTertiary
            }
            Text(
                text = note,
                style = POCOTheme.typography.title3,
                color = noteColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun VoiceCard(
    uiState: PetModeUiState,
    onSpeakNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    SideCard(modifier = modifier) {
        CardTitle(icon = MungoIcon.SPEAKER, title = "안심 한마디")
        Spacer(Modifier.height(20.dp))
        Text(
            text = "“${uiState.voiceMessage}”",
            style = POCOTheme.typography.h3.copy(fontWeight = FontWeight.Medium),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(28.dp))
        Waveform(isPlaying = uiState.isSpeaking)
        Spacer(Modifier.height(28.dp))
        Text(
            text = if (uiState.isSpeaking) {
                "지금 들려주는 중이에요"
            } else {
                "10분마다 들려줘요 · 다음 재생 ${uiState.minutesToNextVoice}분 뒤"
            },
            style = POCOTheme.typography.body2,
            color = colors.textSecondary
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .clip(CircleShape)
                .background(colors.statusCalmSubtle)
                .pressable(CircleShape, enabled = !uiState.isSpeaking, onClick = onSpeakNow)
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MungoIconView(MungoIcon.PLAY_FILLED, tint = colors.statusCalm, size = 22.dp)
            Spacer(Modifier.width(10.dp))
            Text(text = "지금 들려주기", style = POCOTheme.typography.title3, color = colors.statusCalm)
        }
    }
}

/** 소리 막대. 들려주는 동안 위아래로 움직인다 */
@Composable
private fun Waveform(isPlaying: Boolean) {
    val colors = POCOTheme.colors
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(WAVE_PERIOD_MS), RepeatMode.Reverse),
        label = "phase"
    )
    Row(
        modifier = Modifier.height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WAVE_HEIGHTS.forEachIndexed { i, base ->
            val strong = i % 3 == 1
            val scale = if (isPlaying) 0.5f + 0.5f * (if (i % 2 == 0) phase else 1f - phase) else 1f
            Box(
                Modifier
                    .width(8.dp)
                    .height(64.dp * base * scale)
                    .background(
                        if (strong) colors.textPrimary else colors.borderStrong,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun BatteryCard(uiState: PetModeUiState) {
    val colors = POCOTheme.colors
    val percent = uiState.batteryPercent
    SideCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(
                icon = if (uiState.isBatteryLow) MungoIcon.BATTERY else MungoIcon.SHIELD_CHECK,
                tint = if (uiState.isBatteryLow) colors.statusAlert else colors.textPrimary
            )
            Spacer(Modifier.width(20.dp))
            Column {
                val hours = uiState.batteryHoursLeft?.let { " · 약 ${it.toHoursText()}" }.orEmpty()
                Text(
                    text = percent?.let { "배터리 $it%$hours" } ?: "배터리 —",
                    style = POCOTheme.typography.title1.copy(fontWeight = FontWeight.Medium),
                    color = if (uiState.isBatteryLow) colors.statusAlert else colors.textPrimary
                )
                Text(
                    text = if (uiState.isBatteryLow) {
                        "배터리가 ${PetModeUiState.BATTERY_ALERT_PERCENT}% 아래예요 · 보호자를 기다려요"
                    } else {
                        "${PetModeUiState.BATTERY_ALERT_PERCENT}% 아래로 내려가면 화면에 알려요"
                    },
                    style = POCOTheme.typography.body2,
                    color = colors.textSecondary
                )
            }
        }
    }
}

/** 6.4 → "6시간", 0.5 → "30분" */
private fun Float.toHoursText(): String =
    if (this >= 1f) "${roundToInt()}시간" else "${(this * 60).roundToInt()}분"

// endregion

// region 공통

@Composable
private fun SideCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(POCOTheme.colors.bgSurface, RoundedCornerShape(MungoRadius.xl))
            .padding(horizontal = 40.dp, vertical = 36.dp),
        content = content
    )
}

@Composable
private fun CardTitle(icon: MungoIcon, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconCircle(icon = icon)
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = POCOTheme.typography.title2,
            color = POCOTheme.colors.textSecondary
        )
    }
}

@Composable
private fun IconCircle(
    icon: MungoIcon,
    tint: Color = POCOTheme.colors.textPrimary,
    size: Dp = 52.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(POCOTheme.colors.bgSurfaceSunken, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        MungoIconView(icon, tint = tint, size = size * 0.5f)
    }
}

@Composable
private fun InfoChip(icon: MungoIcon, text: String, background: Color, content: Color) {
    Row(
        modifier = Modifier
            .height(64.dp)
            .background(background, CircleShape)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MungoIconView(icon, tint = content, size = 24.dp)
        Spacer(Modifier.width(10.dp))
        Text(text = text, style = POCOTheme.typography.title3, color = content)
    }
}

// endregion

private const val LEFT_WEIGHT = 0.63f
private const val RIGHT_WEIGHT = 0.37f
private const val WAVE_PERIOD_MS = 420
private val WAVE_HEIGHTS = listOf(
    0.3f, 0.55f, 0.35f, 0.8f, 0.4f, 0.7f, 1f, 0.45f, 0.3f, 0.75f,
    0.4f, 0.3f, 0.9f, 0.5f, 0.3f, 0.6f, 0.85f, 0.35f
)

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun PetModeScreenPreview() {
    POCOTheme {
        PetModeScreen(
            uiState = PetModeUiState(
                pet = Pet(
                    id = 1,
                    name = "현댕이",
                    breed = "시츄",
                    ageYears = 10,
                    weightKg = 7f,
                    seat = PetSeat.REAR_RIGHT,
                    motionSensitivity = MotionSensitivity.NORMAL
                ),
                startedAtMillis = 0L,
                nowMillis = 41 * PetModeUiState.MINUTE_MS,
                temperature = 22f,
                targetTemperature = 22f,
                isWindowClosed = true,
                batteryPercent = 72,
                batteryHoursLeft = 6f,
                nextVoiceAtMillis = 44 * PetModeUiState.MINUTE_MS
            ),
            onSpeakNow = {},
            onFinish = {}
        )
    }
}
