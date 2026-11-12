package com.example.poco.presentation.mypet

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.PetPhoto
import com.example.poco.core.designsystem.component.pressable
import com.example.poco.core.designsystem.hasFullPetImage
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.util.toMonthDay
import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.PetTrait
import com.example.poco.data.model.TripStats
import com.example.poco.presentation.common.MungoNavRail
import com.example.poco.presentation.common.RailTab
import com.example.poco.presentation.petprofile.PetDeleteDialog
import com.example.poco.presentation.petprofile.PetForm
import com.example.poco.presentation.petprofile.PetPhotoPickerDialog
import com.example.poco.presentation.petprofile.label

@Composable
fun MyPetRoute(navigateHome: () -> Unit, viewModel: MyPetViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 사진: 큰 선택기에서 '기본 선택기'를 누를 때만 시스템 선택기
    val systemPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.onPhotoPicked(it.toString()) } }
    var showPhotoPicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    if (showPhotoPicker) {
        PetPhotoPickerDialog(
            onPicked = { uri ->
                showPhotoPicker = false
                viewModel.onPhotoPicked(uri)
            },
            onOpenSystemPicker = {
                showPhotoPicker = false
                systemPhotoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = { showPhotoPicker = false }
        )
    }
    if (showDeleteDialog) {
        PetDeleteDialog(
            name = uiState.selectedPet?.name.orEmpty(),
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deletePet()
            }
        )
    }

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) navigateHome()
    }
    // 수정 중 뒤로 = 취소
    BackHandler(enabled = uiState.isEditing && uiState.pets.isNotEmpty()) { viewModel.cancelEdit() }

    MyPetScreen(
        uiState = uiState,
        actions = MyPetActions(
            onHome = navigateHome,
            onSelectPet = viewModel::selectPet,
            onAddPet = viewModel::startAdd,
            onEdit = viewModel::startEdit,
            onToggleCareInView = viewModel::toggleCareInView,
            onCancel = viewModel::cancelEdit,
            onSave = viewModel::save,
            onDelete = { showDeleteDialog = true },
            onPickPhoto = { showPhotoPicker = true },
            onFormChange = viewModel::updateForm,
            onAgeType = viewModel::typeAge,
            onWeightType = viewModel::typeWeight,
            onAgeStep = viewModel::changeAge,
            onWeightStep = viewModel::changeWeight,
            onToggleCare = viewModel::toggleCare
        )
    )
}

/** 화면에서 ViewModel로 보내는 동작 모음 */
internal class MyPetActions(
    val onHome: () -> Unit = {},
    val onSelectPet: (Long) -> Unit = {},
    val onAddPet: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onToggleCareInView: (PetTrait) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onPickPhoto: () -> Unit = {},
    val onFormChange: ((PetForm) -> PetForm) -> Unit = {},
    val onAgeType: (String) -> Unit = {},
    val onWeightType: (String) -> Unit = {},
    val onAgeStep: (Int) -> Unit = {},
    val onWeightStep: (Int) -> Unit = {},
    val onToggleCare: (PetTrait) -> Unit = {}
)

@Composable
private fun MyPetScreen(
    uiState: MyPetUiState,
    actions: MyPetActions,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .safeDrawingPadding()
    ) {
        MungoNavRail(selected = RailTab.MY_PET, onHome = actions.onHome, onMyPet = {})
        VerticalLine()
        PetListPanel(uiState = uiState, actions = actions)
        VerticalLine()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 44.dp, vertical = 40.dp)
        ) {
            when {
                uiState.isEditing -> PetEditor(uiState = uiState, actions = actions)
                uiState.selectedPet != null -> PetDetail(
                    pet = uiState.selectedPet!!,
                    stats = uiState.tripStats,
                    canEdit = uiState.canEdit,
                    actions = actions
                )
            }
        }
    }
}

// region 가운데: 반려견 목록

@Composable
private fun PetListPanel(uiState: MyPetUiState, actions: MyPetActions) {
    val colors = POCOTheme.colors
    Column(
        modifier = Modifier
            .width(LIST_WIDTH)
            .fillMaxHeight()
            .background(colors.bgSurface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "마이펫",
            style = POCOTheme.typography.title1,
            color = colors.textPrimary,
            modifier = Modifier.padding(start = 12.dp, bottom = 24.dp)
        )
        uiState.pets.forEach { pet ->
            PetListCard(
                pet = pet,
                selected = uiState.mode != MyPetMode.ADD && pet.id == uiState.selectedId,
                onClick = { actions.onSelectPet(pet.id) }
            )
        }
        AddPetCard(selected = uiState.mode == MyPetMode.ADD, onClick = actions.onAddPet)
    }
}

@Composable
private fun PetListCard(pet: Pet, selected: Boolean, onClick: () -> Unit) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(MungoRadius.lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 116.dp)
            .background(if (selected) colors.bgAccentSubtle else colors.bgSurface, shape)
            .border(
                width = if (selected) 3.dp else 2.dp,
                color = if (selected) colors.borderAccent else colors.borderDefault,
                shape = shape
            )
            .pressable(shape, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PetAvatar(pet.photoPath, pet.breed, 76.dp)
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pet.name,
                style = POCOTheme.typography.title2.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pet.listSummary(),
                style = POCOTheme.typography.body3,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddPetCard(selected: Boolean, onClick: () -> Unit) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(MungoRadius.lg)
    val dash = if (selected) colors.borderAccent else colors.borderStrong
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .background(if (selected) colors.bgAccentSubtle else colors.bgSurfaceSunken, shape)
            .dashedBorder(dash, MungoRadius.lg)
            .pressable(shape, onClick = onClick)
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(colors.bgSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(MungoIcon.PLUS, tint = colors.textPrimary, size = 28.dp)
        }
        Spacer(Modifier.width(24.dp))
        Text(
            text = "반려견 추가",
            style = POCOTheme.typography.title2.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
    }
}

// endregion

// region 오른쪽: 프로필 보기

@Composable
private fun PetDetail(pet: Pet, stats: TripStats, canEdit: Boolean, actions: MyPetActions) {
    val colors = POCOTheme.colors
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PetAvatar(pet.photoPath, pet.breed, 136.dp)
            Spacer(Modifier.width(32.dp))
            Text(
                text = pet.name,
                style = POCOTheme.typography.h2,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            PillButton(
                text = "프로필 수정",
                icon = MungoIcon.PENCIL,
                enabled = canEdit,
                onClick = actions.onEdit
            )
        }
        if (!canEdit) {
            Text(
                text = "정차 중에만 수정할 수 있어요",
                style = POCOTheme.typography.body3,
                color = colors.textTertiary,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            )
        }
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            BasicInfoCard(
                pet = pet,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            CareCard(
                pet = pet,
                enabled = canEdit,
                onToggle = actions.onToggleCareInView,
                modifier = Modifier
                    .weight(CARE_CARD_WEIGHT)
                    .fillMaxHeight()
            )
        }
        Spacer(Modifier.height(24.dp))
        StatsRow(stats)
    }
}

@Composable
private fun BasicInfoCard(pet: Pet, modifier: Modifier = Modifier) {
    WhiteCard(modifier = modifier) {
        CardTitle("기본 정보")
        Spacer(Modifier.height(16.dp))
        val rows = listOf(
            "이름" to pet.name,
            "품종" to pet.breed.ifBlank { "—" },
            "나이" to (pet.ageYears?.let { "${it}살" } ?: "—"),
            "몸무게" to (pet.weightKg?.let { "%.1fkg".format(it) } ?: "—"),
            "크기" to (pet.size?.label() ?: "—")
        )
        rows.forEachIndexed { i, (label, value) ->
            if (i > 0) HorizontalDivider(color = POCOTheme.colors.borderDefault)
            InfoRow(label = label, value = value)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CareCard(
    pet: Pet,
    enabled: Boolean,
    onToggle: (PetTrait) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    WhiteCard(modifier = modifier) {
        CardTitle("케어 기준")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "해당하는 걸 모두 골라 주세요",
            style = POCOTheme.typography.body3,
            color = colors.textTertiary
        )
        Spacer(Modifier.height(24.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CARE_TRAITS.forEach { trait ->
                CareChip(
                    text = trait.careLabel(pet.breed),
                    selected = trait in pet.traits,
                    onClick = if (enabled) ({ onToggle(trait) }) else null
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = colors.borderDefault)
        Spacer(Modifier.height(12.dp))
        InfoRow("휴식 추천 간격", restIntervalLabel(pet.traits))
        InfoRow("실내 적정 온도", comfortRangeLabel(pet))
        InfoRow("창문 알림 기준", "${pet.windowAlertPercent}% 넘게 열릴 때")
    }
}

@Composable
private fun StatsRow(stats: TripStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        StatCard("함께한 이동", stats.count.toString(), "회", Modifier.weight(1f))
        StatCard(
            "평균 컴포트 점수",
            stats.averageScore?.toString() ?: "—",
            if (stats.averageScore != null) "점" else null,
            Modifier.weight(1f)
        )
        StatCard(
            "마지막 이동",
            stats.lastStartedAtMillis?.toMonthDay() ?: "—",
            null,
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String?, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    WhiteCard(modifier = modifier, vertical = 32.dp) {
        Text(text = label, style = POCOTheme.typography.body2, color = colors.textSecondary)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, style = POCOTheme.typography.h2, color = colors.textPrimary)
            unit?.let {
                Text(
                    text = it,
                    style = POCOTheme.typography.body1,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
            }
        }
    }
}

// endregion

// region 공통

@Composable
internal fun PetAvatar(photoPath: String?, breed: String?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(POCOTheme.colors.bgAccentSubtle, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        PetPhoto(
            photoPath = photoPath,
            breed = breed,
            modifier = Modifier.size(if (hasFullPetImage(photoPath, breed)) size else size * 0.6f)
        )
    }
}

@Composable
internal fun WhiteCard(
    modifier: Modifier = Modifier,
    vertical: Dp = 40.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(POCOTheme.colors.bgSurface, RoundedCornerShape(MungoRadius.xl))
            .padding(horizontal = 36.dp, vertical = vertical),
        content = content
    )
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text = text,
        style = POCOTheme.typography.title2.copy(fontWeight = FontWeight.Bold),
        color = POCOTheme.colors.textPrimary
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = POCOTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = POCOTheme.typography.body2,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = POCOTheme.typography.title3.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
    }
}

/** 케어 기준 칩: 고르면 망고 + ✓ */
@Composable
internal fun CareChip(text: String, selected: Boolean, onClick: (() -> Unit)?) {
    val colors = POCOTheme.colors
    Row(
        modifier = Modifier
            .height(64.dp)
            .background(if (selected) colors.bgAccent else colors.bgSurface, CircleShape)
            .then(
                if (selected) Modifier else Modifier.border(2.dp, colors.borderDefault, CircleShape)
            )
            .pressable(CircleShape, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            MungoIconView(MungoIcon.CHECK, tint = colors.textOnAccent, size = 22.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = POCOTheme.typography.title3.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) colors.textOnAccent else colors.textPrimary
        )
    }
}

/** 흰 바탕 · 테두리 pill 버튼 (프로필 수정 · 취소) */
@Composable
internal fun PillButton(
    text: String,
    onClick: () -> Unit,
    icon: MungoIcon? = null,
    enabled: Boolean = true,
    primary: Boolean = false
) {
    val colors = POCOTheme.colors
    val content = when {
        !enabled -> colors.textDisabled
        primary -> colors.textOnAccent
        else -> colors.textPrimary
    }
    Row(
        modifier = Modifier
            .height(80.dp)
            .background(if (primary && enabled) colors.bgAccent else colors.bgSurface, CircleShape)
            .then(
                if (primary && enabled) {
                    Modifier
                } else {
                    Modifier.border(2.dp, colors.borderDefault, CircleShape)
                }
            )
            .pressable(CircleShape, enabled = enabled, onClick = onClick)
            .padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            MungoIconView(it, tint = content, size = 28.dp)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = POCOTheme.typography.title3.copy(fontWeight = FontWeight.Bold),
            color = content
        )
    }
}

@Composable
private fun VerticalLine() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(POCOTheme.colors.borderDefault)
    )
}

private fun Modifier.dashedBorder(color: Color, radius: Dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx()))
        )
    )
}

// endregion

private val LIST_WIDTH = 420.dp
private const val CARE_CARD_WEIGHT = 1.1f

internal val previewPets = listOf(
    Pet(
        id = 1,
        name = "현댕이",
        breed = "시츄",
        ageYears = 10,
        weightKg = 7f,
        seat = PetSeat.REAR_RIGHT,
        motionSensitivity = MotionSensitivity.NORMAL,
        traits = setOf(PetTrait.SENIOR, PetTrait.HEAT_SENSITIVE)
    ),
    Pet(
        id = 2,
        name = "보리",
        breed = "토이 푸들",
        ageYears = 9,
        weightKg = 3.8f,
        seat = PetSeat.REAR_RIGHT,
        motionSensitivity = MotionSensitivity.NORMAL
    )
)

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun MyPetDetailPreview() {
    POCOTheme {
        MyPetScreen(
            uiState = MyPetUiState(
                pets = previewPets,
                selectedId = 1,
                tripStats = TripStats(23, 86, 0L),
                isLoaded = true
            ),
            actions = MyPetActions()
        )
    }
}

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun MyPetEditPreview() {
    POCOTheme {
        MyPetScreen(
            uiState = MyPetUiState(
                pets = previewPets,
                selectedId = 1,
                mode = MyPetMode.EDIT,
                form = PetForm.from(previewPets[0]),
                isLoaded = true
            ),
            actions = MyPetActions()
        )
    }
}
