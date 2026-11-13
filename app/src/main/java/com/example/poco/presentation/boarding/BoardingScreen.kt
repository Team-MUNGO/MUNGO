package com.example.poco.presentation.boarding

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.PetPhoto
import com.example.poco.core.designsystem.component.pressable
import com.example.poco.core.designsystem.hasFullPetImage
import com.example.poco.core.theme.MungoRadius
import com.example.poco.presentation.design.Mg
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.util.irang
import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat

/*
 * 탑승 준비 (mungo 시안 · 1700×1184). 정차 중에만 진행
 * [상단 바: 닫기 · 제목 · 정차 중 안내]
 * [왼쪽: 단계 · 질문 · 요약 · 이전/다음 | 오른쪽: 단계별 내용]
 * 1/3 펫 고르기 · 2/3 자리(BoardingSeatStep) · 3/3 확인(BoardingChecklistStep) · 끝(BoardingReadyStep)
 */

@Composable
fun BoardingRoute(
    navigateBack: () -> Unit,
    navigateToPetProfile: () -> Unit,
    navigateToDriving: () -> Unit,
    viewModel: BoardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) {
            viewModel.consumeDone()
            navigateToDriving()
        }
    }
    // 뒤로: 한 단계씩, 1/3에서는 닫기
    BackHandler { if (!viewModel.back()) navigateBack() }

    BoardingScreen(
        uiState = uiState,
        actions = BoardingActions(
            onClose = navigateBack,
            onBack = { if (!viewModel.back()) navigateBack() },
            onNext = viewModel::next,
            onMic = viewModel::listen,
            onTogglePet = viewModel::togglePet,
            onAddPet = navigateToPetProfile,
            onSelectSeat = viewModel::selectSeat,
            onSelectActivePet = viewModel::selectActivePet,
            onToggleCheck = viewModel::toggleCheck,
            onCloseLayoutDialog = viewModel::closeLayoutDialog,
            onPickLayout = viewModel::startDriving
        )
    )
}

class BoardingActions(
    val onClose: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onMic: () -> Unit = {},
    val onTogglePet: (Long) -> Unit = {},
    val onAddPet: () -> Unit = {},
    val onSelectSeat: (PetSeat) -> Unit = {},
    val onSelectActivePet: (Long) -> Unit = {},
    val onToggleCheck: (CheckItem) -> Unit = {},
    val onCloseLayoutDialog: () -> Unit = {},
    val onPickLayout: (DriveLayout) -> Unit = {}
)

@Composable
private fun BoardingScreen(
    uiState: BoardingUiState,
    actions: BoardingActions,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgSurface)
            .safeDrawingPadding()
    ) {
        TopBar(
            title = if (uiState.step == BoardingStep.READY) "출발 준비" else "탑승 준비",
            onClose = actions.onClose
        )
        HorizontalDivider(color = colors.borderDefault)
        Row(modifier = Modifier.fillMaxSize()) {
            LeftPanel(
                uiState = uiState,
                actions = actions,
                modifier = Modifier
                    .width(LEFT_PANEL_WIDTH)
                    .fillMaxHeight()
            )
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(colors.borderDefault)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(colors.bgCanvas)
            ) {
                when (uiState.step) {
                    BoardingStep.COMPANIONS -> CompanionPicker(uiState, actions)
                    BoardingStep.SEAT -> SeatMap(uiState, actions)
                    BoardingStep.CHECKLIST -> Checklist(uiState, actions)
                    BoardingStep.READY -> ReadyPanel(uiState)
                }
            }
        }
    }
    if (uiState.isLayoutDialogVisible) {
        DriveLayoutDialog(
            isListening = uiState.isListening,
            heardText = uiState.heardText,
            onMic = actions.onMic,
            onDismiss = actions.onCloseLayoutDialog,
            onPick = actions.onPickLayout
        )
    }
}

@Composable
private fun TopBar(title: String, onClose: () -> Unit) {
    val colors = POCOTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .padding(horizontal = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(colors.bgSurfaceSunken, CircleShape)
                .pressable(CircleShape, onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(MungoIcon.CLOSE, size = 32.dp, contentDescription = "닫기")
        }
        Spacer(Modifier.width(24.dp))
        Text(text = title, style = Mg.semi(35, 45), color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        MungoIconView(MungoIcon.LOCK, tint = colors.textTertiary, size = 26.dp)
        Spacer(Modifier.width(10.dp))
        Text(text = "정차 중에만 진행해요", style = POCOTheme.typography.body2, color = colors.textTertiary)
    }
}

// region 왼쪽 패널

@Composable
private fun LeftPanel(
    uiState: BoardingUiState,
    actions: BoardingActions,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(start = 56.dp, end = 56.dp, top = 40.dp, bottom = 44.dp)) {
        if (uiState.step.number <= BoardingStep.TOTAL) {
            StepHeader(uiState.step)
            Spacer(Modifier.height(56.dp))
        } else {
            Spacer(Modifier.height(59.dp)) // Figma 470:54958: 제목 y 216
        }
        Column(modifier = Modifier.weight(1f)) {
            when (uiState.step) {
                BoardingStep.COMPANIONS -> CompanionsLeft(uiState, actions)
                BoardingStep.SEAT -> SeatLeft(uiState, actions)
                BoardingStep.CHECKLIST -> ChecklistLeft(uiState, actions)
                BoardingStep.READY -> ReadyLeft(uiState)
            }
        }
        Spacer(Modifier.height(24.dp))
        BottomButtons(
            showBack = uiState.step != BoardingStep.COMPANIONS,
            nextText = when (uiState.step) {
                BoardingStep.CHECKLIST -> "준비 완료"
                BoardingStep.READY -> "주행 시작"
                else -> "다음"
            },
            enabled = uiState.canGoNext,
            onBack = actions.onBack,
            onNext = actions.onNext
        )
    }
}

@Composable
private fun StepHeader(step: BoardingStep) {
    val colors = POCOTheme.colors
    Row {
        // Figma 470:54513: M24 #9CA7B4 · 막대 높이 8
        Text(text = "안전동승체크", style = Mg.medium(24, 32), color = Color(0xFF9CA7B4))
        Spacer(Modifier.weight(1f))
        Text(
            text = "${step.number} / ${BoardingStep.TOTAL}",
            style = Mg.medium(24, 32),
            color = Color(0xFF9CA7B4)
        )
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(BoardingStep.TOTAL) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(
                        if (index < step.number) colors.bgAccent else colors.borderDefault,
                        CircleShape
                    )
            )
        }
    }
}

/** 질문 제목 (+ 설명) */
@Composable
internal fun StepTitle(title: String, description: String? = null) {
    val colors = POCOTheme.colors
    // Figma 470:54519: B60 · 줄 간격 78 / 설명 R26 · 줄 간격 37
    Text(text = title, style = Mg.bold(60, 78), color = colors.textPrimary)
    description?.let {
        Spacer(Modifier.height(28.dp))
        Text(text = it, style = Mg.regular(26, 37), color = colors.textSecondary)
    }
}

/** 구분선 + 작은 제목 */
@Composable
internal fun SectionLabel(text: String, topGap: Dp = 41.dp) {
    val colors = POCOTheme.colors
    HorizontalDivider(color = colors.borderDefault)
    Spacer(Modifier.height(topGap))
    Text(text = text, style = Mg.medium(24, 32), color = Color(0xFF9CA7B4))
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun BottomButtons(
    showBack: Boolean,
    nextText: String,
    enabled: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val colors = POCOTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(BUTTON_HEIGHT)
                    .border(2.dp, colors.borderDefault, CircleShape)
                    .pressable(CircleShape, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "이전", style = POCOTheme.typography.title1, color = colors.textSecondary)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(BUTTON_HEIGHT)
                .background(if (enabled) colors.bgAccent else colors.bgNeutral, CircleShape)
                .pressable(CircleShape, enabled = enabled, onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = nextText,
                style = POCOTheme.typography.title1,
                color = if (enabled) colors.textOnAccent else colors.textDisabled
            )
        }
    }
}

/** 마이크 + 예시 문장 (듣는 중 · 알아들은 말로 바뀜) */
@Composable
internal fun VoiceRow(
    isListening: Boolean,
    heardText: String?,
    example: String,
    onMic: () -> Unit,
    micSize: Dp = 64.dp
) {
    val colors = POCOTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(micSize)
                .background(
                    if (isListening) colors.bgAccent else colors.bgNeutralStrong,
                    CircleShape
                )
                .pressable(CircleShape, onClick = onMic),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(
                MungoIcon.MIC,
                tint = if (isListening) colors.textOnAccent else colors.textOnNeutralStrong,
                size = micSize * 0.44f,
                contentDescription = "말로 답하기"
            )
        }
        Spacer(Modifier.width(20.dp))
        Text(
            text = when {
                isListening -> "듣고 있어요…"
                heardText != null -> "“ $heardText ”"
                else -> "“ $example ”"
            },
            style = Mg.regular(25, 32),
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// endregion

// region 1/3 펫 고르기

@Composable
private fun ColumnScope.CompanionsLeft(uiState: BoardingUiState, actions: BoardingActions) {
    StepTitle("오늘은 누구와\n함께 이동하나요?")
    Spacer(Modifier.height(108.dp))
    VoiceRow(
        isListening = uiState.isListening,
        heardText = uiState.heardText,
        example = uiState.pets.firstOrNull()?.name?.let { "${it.irang} 같이 탈거야" } ?: "같이 탈거야",
        onMic = actions.onMic
    )
    Spacer(Modifier.height(45.dp))
    SectionLabel("함께 타는 친구")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.selectedPets.isEmpty()) {
            Text(
                text = "함께 탈 반려견을 골라 주세요",
                style = POCOTheme.typography.body2,
                color = POCOTheme.colors.textSecondary
            )
        }
        uiState.selectedPets.forEach { CompanionRow(it) }
    }
}

@Composable
private fun CompanionRow(pet: Pet) {
    val colors = POCOTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .background(colors.bgSurfaceSunken, RoundedCornerShape(MungoRadius.lg))
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PetCircle(pet = pet, size = 72.dp)
        Spacer(Modifier.width(18.dp))
        Text(
            text = pet.name,
            style = Mg.semi(25, 32),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompanionPicker(uiState: BoardingUiState, actions: BoardingActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // 펫 카드 + 빈 칸은 '반려견 추가'. 펫이 3마리 이상이면 한 줄씩 늘어난다
        val slots = maxOf(SLOTS_PER_ROW, uiState.pets.size + 1)
            .let { (it + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW * SLOTS_PER_ROW }
        val cards: List<Pet?> = uiState.pets + List(slots - uiState.pets.size) { null }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = CARD_HEIGHT * 1.5f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CARD_GAP)
        ) {
            cards.chunked(SLOTS_PER_ROW).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(CARD_GAP)) {
                    row.forEach { pet ->
                        if (pet != null) {
                            PetCard(
                                pet = pet,
                                selected = pet.id in uiState.selectedIds,
                                onClick = { actions.onTogglePet(pet.id) }
                            )
                        } else {
                            AddPetCard(onClick = actions.onAddPet)
                        }
                    }
                }
            }
        }
        // Keep the note's measured space when selection is cleared, so the centered pet grid
        // stays at the same position. Hidden content must not remain in accessibility output.
        val note = uiState.temperatureNote
        val reservedNote = note ?: temperatureNote(uiState.pets)
        reservedNote?.let {
            Spacer(Modifier.height(48.dp))
            Box(
                Modifier.alpha(if (note != null) 1f else 0f)
                    .then(if (note == null) Modifier.clearAndSetSemantics {} else Modifier)
            ) {
                TemperatureBar(it)
            }
        }
    }
}

@Composable
private fun RowScope.PetCard(pet: Pet, selected: Boolean, onClick: () -> Unit) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(MungoRadius.xl)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(CARD_HEIGHT)
            .background(if (selected) colors.bgAccentSubtle else colors.bgSurface, shape)
            .border(3.dp, if (selected) colors.borderAccent else colors.borderDefault, shape)
            .pressable(shape, onClick = onClick)
    ) {
        if (selected) CheckBadge(Modifier.align(Alignment.TopEnd).padding(20.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PetCircle(pet = pet, size = 188.dp)
            Spacer(Modifier.height(48.dp))
            Text(
                text = pet.name,
                style = POCOTheme.typography.h3,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RowScope.AddPetCard(onClick: () -> Unit) {
    val colors = POCOTheme.colors
    val dash = colors.borderStrong
    val shape = RoundedCornerShape(MungoRadius.xl)
    Column(
        modifier = Modifier
            .weight(1f)
            .height(CARD_HEIGHT)
            .drawBehind {
                drawRoundRect(
                    color = dash,
                    cornerRadius = CornerRadius(MungoRadius.xl.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                    )
                )
            }
            .pressable(shape, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .background(colors.bgSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(MungoIcon.PLUS, size = 36.dp)
        }
        Spacer(Modifier.height(36.dp))
        Text(text = "반려견 추가", style = Mg.bold(24, 32), color = colors.textPrimary)
    }
}

@Composable
private fun TemperatureBar(note: TemperatureNote) {
    val colors = POCOTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSurface, RoundedCornerShape(MungoRadius.lg))
            .padding(horizontal = 32.dp, vertical = 30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(colors.bgAccentSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(MungoIcon.THERMOMETER, tint = colors.textAccent, size = 34.dp)
        }
        Spacer(Modifier.width(28.dp))
        Column {
            Text(text = note.title, style = Mg.bold(25, 35), color = colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(text = note.body, style = POCOTheme.typography.body2, color = colors.textSecondary)
        }
    }
}

// endregion

// region 공통

/** 망고 원 + 체크 (선택됨 표시) */
@Composable
internal fun CheckBadge(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val colors = POCOTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .background(colors.bgAccent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        MungoIconView(MungoIcon.CHECK, tint = colors.textOnAccent, size = size * 0.54f)
    }
}

/** 동그란 펫 사진 (사진 · 품종 그림이 없으면 발바닥) */
@Composable
internal fun PetCircle(pet: Pet, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(POCOTheme.colors.bgSurfaceSunken, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        PetPhoto(
            photoPath = pet.photoPath,
            breed = pet.breed,
            modifier = Modifier.size(
                if (hasFullPetImage(pet.photoPath, pet.breed)) size else size * 0.7f
            )
        )
    }
}

private val LEFT_PANEL_WIDTH = 564.dp
private val BUTTON_HEIGHT = 104.dp
private val CARD_HEIGHT = 470.dp
private val CARD_GAP = 28.dp
private const val SLOTS_PER_ROW = 3

// endregion

internal fun previewPet(id: Long, name: String, seat: PetSeat = PetSeat.PASSENGER) = Pet(
    id = id,
    name = name,
    breed = "시츄",
    ageYears = 10,
    weightKg = 7f,
    seat = seat,
    motionSensitivity = MotionSensitivity.NORMAL
)

private fun previewState(step: BoardingStep) = BoardingUiState(
    step = step,
    pets = listOf(previewPet(1, "현댕이")),
    selectedIds = listOf(1),
    seats = mapOf(1L to PetSeat.PASSENGER),
    activePetId = 1,
    manualChecks = setOf(CheckItem.CAR_SEAT),
    isWindowClosed = true,
    seatTemperature = 22f
)

@Preview(widthDp = 1700, heightDp = 1184, name = "1/3 펫")
@Composable
internal fun CompanionsPreview() {
    POCOTheme(darkTheme = false) {
        BoardingScreen(previewState(BoardingStep.COMPANIONS), BoardingActions())
    }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "2/3 자리")
@Composable
internal fun SeatPreview() {
    POCOTheme(darkTheme = false) {
        BoardingScreen(previewState(BoardingStep.SEAT), BoardingActions())
    }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "3/3 확인")
@Composable
internal fun ChecklistPreview() {
    POCOTheme(darkTheme = false) {
        BoardingScreen(previewState(BoardingStep.CHECKLIST), BoardingActions())
    }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "출발 준비 + 분할")
@Composable
private fun ReadyPreview() {
    POCOTheme(darkTheme = false) {
        BoardingScreen(
            previewState(BoardingStep.READY).copy(isLayoutDialogVisible = true),
            BoardingActions()
        )
    }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "다크 모드")
@Composable
private fun DarkPreview() {
    POCOTheme(darkTheme = true) {
        BoardingScreen(previewState(BoardingStep.SEAT), BoardingActions())
    }
}
