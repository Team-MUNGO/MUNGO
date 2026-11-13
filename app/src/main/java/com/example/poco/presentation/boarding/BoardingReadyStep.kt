package com.example.poco.presentation.boarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poco.R
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.ShihTzuIllustration
import com.example.poco.core.designsystem.component.pressable
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.MungoTheme
import com.example.poco.core.util.iGa
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.Restraint
import com.example.poco.presentation.design.DrawableIcon
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.design.ScrimDialog
import com.example.poco.presentation.petprofile.label
import kotlin.math.roundToInt

/* 출발 준비 끝 (요약 + 음성 한마디) → 주행 시작 → 주행 화면 분할 고르기 */

@Composable
internal fun ColumnScope.ReadyLeft(uiState: BoardingUiState) {
    val colors = MungoTheme.colors
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
    val colors = MungoTheme.colors
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
    val colors = MungoTheme.colors
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
    val colors = MungoTheme.colors
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
        // 유저가 등록한 사진 · 품종 대신 mungo 시츄 일러스트를 뒷판 위에 앉힌다 (Figma 488:9017 · 527×618)
        ShihTzuIllustration(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 27.dp, bottom = 38.dp)
                .size(width = 527.dp, height = 618.dp)
        )

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

// region 주행 화면 안내 (Figma 탑승준비_05 · 주행 화면 안내 팝업)

/** 주행 화면은 2분할로 시작한다는 안내. "확인" = 2분할로 출발, 3분할은 주행 화면에서 드래그 */
@Composable
internal fun DriveLayoutDialog(onDismiss: () -> Unit, onConfirm: (hideNextTime: Boolean) -> Unit) {
    val colors = MungoTheme.colors
    var hideNextTime by remember { mutableStateOf(false) }
    ScrimDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .width(1150.dp)
                .background(colors.bgSurface, RoundedCornerShape(43.dp))
                .padding(start = 53.dp, end = 53.dp, top = 37.dp, bottom = 35.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f).padding(top = 11.dp)) {
                    Text(
                        text = "주행 화면은 2분할로 시작해요",
                        style = Mg.bold(40, 48),
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(15.dp))
                    Text(
                        text = "내비를 함께 보고 싶으면 화면을 드래그해서 3분할로 바꿀 수 있어요",
                        style = Mg.regular(25, 30),
                        color = colors.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(colors.bgSurfaceSunken, CircleShape)
                        .pressable(CircleShape, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    MungoIconView(MungoIcon.CLOSE, size = 28.dp, contentDescription = "닫기")
                }
            }
            Spacer(Modifier.height(46.dp))
            Row(verticalAlignment = Alignment.Top) {
                LayoutGuidePreview(DriveLayout.TWO)
                DragHint(modifier = Modifier.weight(1f))
                LayoutGuidePreview(DriveLayout.THREE)
            }
            Spacer(Modifier.height(21.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .background(colors.bgAccentSubtle, CircleShape)
                        .padding(horizontal = 21.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("기본", style = Mg.bold(19, 23), color = colors.textAccent)
                }
                Spacer(Modifier.width(13.dp))
                Text(
                    text = "2분할 · 주행 정보 + 멍고",
                    style = Mg.bold(23, 28),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "3분할 · 주행 정보 + 내비 + 멍고",
                    style = Mg.bold(23, 28),
                    color = colors.textPrimary,
                    modifier = Modifier.width(401.dp)
                )
            }
            Spacer(Modifier.height(91.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.pressable(RoundedCornerShape(MungoRadius.sm)) {
                        hideNextTime = !hideNextTime
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val boxShape = RoundedCornerShape(9.dp)
                    Box(
                        modifier = Modifier
                            .size(35.dp)
                            .then(
                                if (hideNextTime) {
                                    Modifier.background(colors.bgAccent, boxShape)
                                } else {
                                    Modifier.border(3.dp, GUIDE_CHECK_BORDER, boxShape)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hideNextTime) {
                            MungoIconView(MungoIcon.CHECK, tint = Color.White, size = 24.dp)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Text("다시 보지 않기", style = Mg.regular(24, 29), color = READY_SUB)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(width = 294.dp, height = 86.dp)
                        .background(colors.bgAccent, CircleShape)
                        .pressable(CircleShape) { onConfirm(hideNextTime) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("확인", style = Mg.bold(29, 35), color = Color.White)
                }
            }
        }
    }
}

/** 화면 분할 미리보기: 차량 | (내비) | 멍고 */
@Composable
private fun LayoutGuidePreview(layout: DriveLayout) {
    val colors = MungoTheme.colors
    val three = layout == DriveLayout.THREE
    Row(
        modifier = Modifier
            .size(width = 401.dp, height = 262.dp)
            .background(colors.bgSurfaceSunken, RoundedCornerShape(21.dp))
            .padding(13.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        GuidePane(width = 96.dp, background = GUIDE_CAR_PANE) {
            MungoIconView(MungoIcon.CAR, tint = colors.textSecondary, size = 28.dp)
        }
        if (three) {
            GuidePane(
                width = 128.dp,
                background = GUIDE_NAV_PANE,
                border = GUIDE_NAV_BORDER,
                dashed = true
            ) {
                Spacer(Modifier.height(36.dp))
                MungoIconView(MungoIcon.MAP_PIN, tint = GUIDE_NAV_TEXT, size = 28.dp)
                Spacer(Modifier.height(10.dp))
                Text("내비", style = Mg.bold(19, 23), color = GUIDE_NAV_TEXT)
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.size(48.dp).background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    DrawableIcon(R.drawable.ic_mungo_hand, tint = GUIDE_NAV_TEXT, size = 21.dp)
                }
            }
        }
        GuidePane(
            width = if (three) 128.dp else 268.dp,
            background = colors.bgAccentSubtle,
            border = colors.bgAccent
        ) {
            DrawableIcon(R.drawable.ic_mungo_paw_guide, size = 24.dp)
            Spacer(Modifier.height(12.dp))
            Text("멍고", style = Mg.bold(19, 23), color = colors.textAccent)
        }
    }
}

@Composable
private fun GuidePane(
    width: Dp,
    background: Color,
    border: Color? = null,
    dashed: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(background, shape)
            .then(
                when {
                    border == null -> Modifier
                    dashed -> Modifier.dashedBorder(border, 13.dp)
                    else -> Modifier.border(2.7.dp, border, shape)
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (dashed) Arrangement.Top else Arrangement.Center,
        content = content
    )
}

/** 가운데 → 화살표 + 손 + "드래그" */
@Composable
private fun DragHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 88.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DrawableIcon(R.drawable.ic_mungo_arrow_right, size = 31.dp)
        Spacer(Modifier.height(27.dp))
        Box(
            modifier = Modifier.size(70.dp).background(Mg.AlertBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            DrawableIcon(R.drawable.ic_mungo_hand, size = 26.dp)
        }
        Spacer(Modifier.height(14.dp))
        Text("드래그", style = Mg.bold(20, 24), color = MungoTheme.colors.textAccent)
    }
}

private fun Modifier.dashedBorder(color: Color, radius: Dp): Modifier = drawBehind {
    val stroke = 2.7.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))
        )
    )
}

private val GUIDE_CAR_PANE = Color(0xFFD1D6DB)
private val GUIDE_NAV_PANE = Color(0xFFDCE6FB)
private val GUIDE_NAV_BORDER = Color(0xFF2F6FED)
private val GUIDE_NAV_TEXT = Color(0xFF2F5BC4)
private val GUIDE_CHECK_BORDER = Color(0xFFB0B8C1)

// endregion
