package com.example.poco.presentation.boarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.poco.R
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.PetPhoto
import com.example.poco.core.designsystem.component.pressable
import com.example.poco.core.designsystem.hasFullPetImage
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.util.iGa
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.Restraint
import com.example.poco.presentation.design.DrawableIcon
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.petprofile.label
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/* 출발 준비 끝 (요약 + 음성 한마디) → 주행 시작 → 주행 화면 분할 고르기 */

@Composable
internal fun ColumnScope.ReadyLeft(uiState: BoardingUiState) {
    val colors = POCOTheme.colors
    val pets = uiState.selectedPets
    val name = pets.firstOrNull()?.name ?: "반려견"
    val onPassenger = uiState.selectedIds.any { uiState.seats[it] == PetSeat.PASSENGER }
    StepTitle(
        title = "출발 준비가\n끝났어요",
        description = if (onPassenger) {
            "조수석에 에어백이 꺼져 있는지\n한 번 더 확인해 주세요."
        } else {
            "${name.iGa} 잘 앉았는지\n한 번 더 확인해 주세요."
        }
    )
    Spacer(Modifier.height(115.dp)) // Figma 470:54958: 구분선 y 577 · 라벨 y 657
    SectionLabel("지금까지 입력한 정보", topGap = 80.dp)
    val seatText = if (pets.size == 1) {
        uiState.seats[pets.first().id]?.label() ?: "—"
    } else {
        pets.joinToString(" · ") { "${it.name} ${uiState.seats[it.id]?.label() ?: "—"}" }
    }
    val windowText = when {
        uiState.isWindowClosed == true -> "잠김"
        uiState.isWindowClosed == false -> "열림"
        CheckItem.WINDOW in uiState.manualChecks -> "직접 확인"
        else -> "—"
    }
    Column(verticalArrangement = Arrangement.spacedBy(23.dp)) {
        // 줄 간격 59
        SummaryRow("자리", seatText)
        SummaryRow("고정", "카시트 · 하네스")
        SummaryRow("창문 잠금", windowText)
        SummaryRow("실내 온도", uiState.seatTemperature?.let { "${it.roundToInt()}°C" } ?: "—")
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    val colors = POCOTheme.colors
    Row {
        Text(
            text = label,
            style = Mg.medium(26, 36),
            color = colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = Mg.bold(25, 36),
            color = colors.textPrimary
        )
    }
}

/** 오른쪽: 펫 그림 + 방금 말한 한마디 */
@Composable
internal fun ReadyPanel(uiState: BoardingUiState) {
    val colors = POCOTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ReadyCard(uiState, Modifier.fillMaxWidth().weight(1f))
        // Figma 488:9008 음성 안내 카드: 망고 원(75) + 진한 스피커, 'mungo' 라벨 아래 안내 문장
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 212.dp)
                .background(colors.bgSurface, RoundedCornerShape(35.dp))
                .padding(start = 54.dp, end = 40.dp, top = 40.dp, bottom = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(75.dp)
                    .background(colors.bgAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                DrawableIcon(R.drawable.driving_speaker, tint = Mg.Ink, size = 30.dp)
            }
            Spacer(Modifier.width(21.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "mungo", style = Mg.regular(23, 27), color = colors.textSecondary)
                Text(
                    text = "“${readyMessage(uiState.selectedPets.map { it.name })}”",
                    style = Mg.semi(35, 42),
                    color = colors.textPrimary
                )
            }
        }
    }
}

/**
 * Figma 488:8997 출발 준비 완료 카드: 왼쪽 위 제목 · 요약, 왼쪽 아래 D 기어 안내,
 * 오른쪽은 망고 도형(뒷판 · 바닥) 위에 반려견 그림
 */
@Composable
private fun ReadyCard(uiState: BoardingUiState, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    val pets = uiState.selectedPets
    val names = pets.joinToString(", ") { it.name }.ifBlank { "반려견" }
    val dark = colors.isDark
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(37.dp))
            .background(if (dark) colors.bgAccentSubtle else Mg.AlertBg)
    ) {
        // 뒷판 (#FFE2BF) · 바닥 (#FFC98A)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 94.dp, bottom = 99.dp)
                .size(width = 308.dp, height = 334.dp)
                .background(if (dark) colors.bgNeutral else READY_BACK, RoundedCornerShape(64.dp))
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 53.dp, bottom = 56.dp)
                .size(width = 388.dp, height = 94.dp)
                .background(
                    if (dark) colors.bgNeutralStrong.copy(alpha = 0.2f) else READY_FLOOR,
                    RoundedCornerShape(40.dp)
                )
        )
        // 사진 · 품종 그림은 둥글게 잘린 그림이라 뒷판 위에 원으로 앉힌다 (그림이 없으면 작은 기본 마크)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 68.dp, bottom = 76.dp),
            horizontalArrangement = Arrangement.spacedBy((-60).dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val shown = pets.take(MAX_PETS_SHOWN)
            shown.forEach { pet ->
                val full = hasFullPetImage(pet.photoPath, pet.breed)
                PetPhoto(
                    photoPath = pet.photoPath,
                    breed = pet.breed,
                    modifier = when {
                        !full -> Modifier.padding(end = 60.dp, bottom = 70.dp).size(240.dp)
                        shown.size == 1 -> Modifier.size(360.dp)
                        else -> Modifier.size(240.dp)
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 54.dp, top = 45.dp, bottom = 48.dp)
        ) {
            Text(
                text = "$names 준비 끝\n이제 출발해요",
                style = Mg.bold(62, 80),
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 560.dp)
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = readySummary(uiState),
                style = Mg.regular(27, 32),
                color = if (dark) colors.textSecondary else READY_SUB,
                modifier = Modifier.padding(start = 9.dp)
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .background(colors.bgAccent, RoundedCornerShape(19.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "D", style = Mg.bold(37, 44), color = Mg.Ink)
                }
                Spacer(Modifier.width(21.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "기어를 D로 바꾸면",
                        style = Mg.regular(24, 29),
                        color = if (dark) colors.textSecondary else READY_SUB
                    )
                    Text(
                        text = "주행 케어가 바로 시작돼요",
                        style = Mg.bold(24, 29),
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}

/** "조수석 | 이동장 | 실내 24°C" (여러 마리면 자리는 '여러 자리') */
private fun readySummary(uiState: BoardingUiState): String {
    val pets = uiState.selectedPets
    val seats = pets.mapNotNull { uiState.seats[it.id] }.distinct()
    val seat = when {
        seats.size == 1 -> seats.first().label()
        seats.size > 1 -> "여러 자리"
        else -> null
    }
    val restraint = "이동장".takeIf { pets.any { it.restraint == Restraint.CRATE_HARNESS } }
    val temp = uiState.seatTemperature?.let { "실내 ${it.roundToInt()}°C" }
    return listOfNotNull(seat, restraint, temp).joinToString(" | ")
}

private val READY_BACK = Color(0xFFFFE2BF)
private val READY_FLOOR = Color(0xFFFFC98A)
private val READY_SUB = Color(0xFF4E5968)

// region 주행 화면 분할

@Composable
internal fun DriveLayoutDialog(
    isListening: Boolean,
    heardText: String?,
    onMic: () -> Unit,
    onDismiss: () -> Unit,
    onPick: (DriveLayout) -> Unit
) {
    val colors = POCOTheme.colors
    // 고르지 않으면 추천(3분할)으로 출발. 말하는 중에는 기다린다
    var secondsLeft by remember { mutableIntStateOf(AUTO_START_SECONDS) }
    LaunchedEffect(isListening) {
        while (!isListening && secondsLeft > 0) {
            delay(1_000L)
            secondsLeft--
        }
        if (secondsLeft == 0) onPick(DriveLayout.THREE)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(1150.dp)
                .background(colors.bgSurface, RoundedCornerShape(MungoRadius.xl))
                .padding(48.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "주행 화면을 어떻게 볼까요?",
                        style = Mg.bold(40, 52),
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "운전 중에는 네비게이션을 함께 보는 걸 추천해요",
                        style = Mg.regular(25, 34),
                        color = colors.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(colors.bgSurfaceSunken, CircleShape)
                        .pressable(CircleShape, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    MungoIconView(MungoIcon.CLOSE, size = 32.dp, contentDescription = "닫기")
                }
            }
            Spacer(Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                LayoutOption(DriveLayout.TWO, selected = false, onClick = {
                    onPick(DriveLayout.TWO)
                })
                LayoutOption(
                    DriveLayout.THREE,
                    selected = true,
                    onClick = { onPick(DriveLayout.THREE) }
                )
            }
            Spacer(Modifier.height(36.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MungoIconView(MungoIcon.INFO, tint = colors.textTertiary, size = 24.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "고르지 않으면 3분할로 출발해요 (${secondsLeft}초)",
                    style = Mg.regular(24, 32),
                    color = Color(0xFF4E5968),
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .background(colors.bgSurfaceSunken, CircleShape)
                        .padding(start = 10.dp, end = 28.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VoiceRow(
                        isListening = isListening,
                        heardText = heardText,
                        example = "mungo, 네비랑 같이 보여 줘",
                        onMic = onMic,
                        micSize = 56.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.LayoutOption(layout: DriveLayout, selected: Boolean, onClick: () -> Unit) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(MungoRadius.xl)
    Column(
        modifier = Modifier
            .weight(1f)
            .border(
                if (selected) 3.dp else 2.dp,
                if (selected) colors.borderAccent else colors.borderDefault,
                shape
            )
            .pressable(shape, onClick = onClick)
            .padding(28.dp)
    ) {
        // 미리보기: 차량 | (내비) | mungo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(colors.bgSurfaceSunken, RoundedCornerShape(MungoRadius.md))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PreviewPane(weight = 0.27f, background = colors.bgNeutral) {
                MungoIconView(MungoIcon.CAR, tint = colors.textTertiary, size = 32.dp)
            }
            if (layout == DriveLayout.THREE) {
                PreviewPane(weight = 0.4f, background = colors.statusCalmSubtle) {
                    MungoIconView(MungoIcon.MAP_PIN, tint = colors.statusCalm, size = 32.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Navigation",
                        style = Mg.medium(19, 24),
                        color = Color(0xFF08705D)
                    )
                }
            }
            PreviewPane(
                weight = if (layout == DriveLayout.THREE) 0.33f else 0.73f,
                background = colors.bgAccentSubtle,
                outlined = true
            ) {
                MungoIconView(MungoIcon.PAW_FILLED, tint = colors.bgAccent, size = 32.dp)
                Spacer(Modifier.height(8.dp))
                Text("Mungo", style = Mg.medium(19, 24), color = colors.textAccent)
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (layout == DriveLayout.THREE) "3분할" else "2분할",
                style = Mg.bold(35, 46),
                color = colors.textPrimary
            )
            if (layout == DriveLayout.THREE) {
                Spacer(Modifier.width(14.dp))
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .background(colors.bgAccentSubtle, CircleShape)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "추천",
                        style = Mg.bold(24, 32),
                        color = colors.textAccent
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (selected) {
                CheckBadge(size = 52.dp)
            } else {
                Box(
                    Modifier
                        .size(52.dp)
                        .border(3.dp, colors.borderStrong, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun RowScope.PreviewPane(
    weight: Float,
    background: Color,
    outlined: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(MungoRadius.sm)
    Column(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .background(background, shape)
            .then(if (outlined) Modifier.border(2.dp, colors.borderAccent, shape) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

// endregion

private const val MAX_PETS_SHOWN = 3
private const val AUTO_START_SECONDS = 10
