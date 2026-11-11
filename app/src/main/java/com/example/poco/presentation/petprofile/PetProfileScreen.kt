package com.example.poco.presentation.petprofile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.designsystem.PetPhoto
import com.example.poco.core.designsystem.PocoIcon
import com.example.poco.core.designsystem.PocoIconView
import com.example.poco.core.designsystem.component.PetListItem
import com.example.poco.core.designsystem.component.PocoChip
import com.example.poco.core.designsystem.component.PocoInput
import com.example.poco.core.designsystem.component.PocoSegment
import com.example.poco.core.designsystem.component.PocoTag
import com.example.poco.core.designsystem.component.PocoTagStyle
import com.example.poco.core.designsystem.hasFullPetImage
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.theme.PocoLayout
import com.example.poco.core.theme.PocoRadius
import com.example.poco.core.theme.PocoSize
import com.example.poco.core.theme.PocoSpacing
import com.example.poco.core.theme.PocoStroke
import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.PetSize
import com.example.poco.data.model.Restraint
import com.example.poco.domain.driving.PocoStandard
import kotlin.math.roundToInt
import kotlinx.collections.immutable.persistentListOf

@Composable
fun PetProfileRoute(navigateBack: () -> Unit, viewModel: PetProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 시스템 사진 선택기: 큰 선택기에서 '기본 선택기'를 누를 때만 (차량 화면에선 너무 작게 보임)
    val systemPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.onPhotoPicked(it.toString()) } }
    var showPhotoPicker by rememberSaveable { mutableStateOf(false) }
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

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) navigateBack()
    }

    PetProfileScreen(
        uiState = uiState,
        // 뒤로: 아직 저장 안 된 입력(타이핑 중)까지 저장하고 나간다
        onBack = viewModel::leave,
        onPickPhoto = { showPhotoPicker = true },
        onFormChange = viewModel::updateForm,
        onFormSelect = viewModel::selectOption,
        onSelectPet = viewModel::selectPet,
        onAddPet = viewModel::addNewPet
    )
}

@Composable
private fun PetProfileScreen(
    uiState: PetProfileUiState,
    onBack: () -> Unit,
    onPickPhoto: () -> Unit,
    onFormChange: ((PetForm) -> PetForm) -> Unit,
    onFormSelect: ((PetForm) -> PetForm) -> Unit,
    onSelectPet: (Long) -> Unit,
    onAddPet: () -> Unit,
    modifier: Modifier = Modifier
) {
    // DS-03 정차 화면: 좌우 96 · 위 32(상태 바 아래) · 아래 48 · 열 520 | 32 | 1176
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(POCOTheme.colors.backgroundDefault)
            .safeDrawingPadding()
            .padding(
                start = PocoLayout.screenHorizontal,
                end = PocoLayout.screenHorizontal,
                top = PocoLayout.screenTop,
                bottom = PocoLayout.screenBottom
            )
    ) {
        Header(uiState = uiState, onBack = onBack)
        Spacer(Modifier.height(PocoSpacing.space28))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(PocoLayout.profileColumnGap)
        ) {
            PetListCard(
                uiState = uiState,
                onPickPhoto = onPickPhoto,
                onSelectPet = onSelectPet,
                onAddPet = onAddPet,
                modifier = Modifier
                    .weight(PocoLayout.PROFILE_COLUMN_LEFT)
                    .fillMaxHeight()
            )
            PetFormCard(
                uiState = uiState,
                editable = uiState.isParked != false,
                onFormChange = onFormChange,
                onFormSelect = onFormSelect,
                modifier = Modifier
                    .weight(PocoLayout.PROFILE_COLUMN_RIGHT)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun Header(uiState: PetProfileUiState, onBack: () -> Unit) {
    val colors = POCOTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 뒤로: 남색으로 채운 버튼 (다크 모드에선 라임) - 회색 테두리보다 눈에 잘 띄게
        Row(
            modifier = Modifier
                .heightIn(min = PocoSize.touchMin)
                .clip(CircleShape)
                .background(colors.accentFill, CircleShape)
                .clickable(onClick = onBack)
                .padding(start = PocoSpacing.space20, end = PocoSpacing.space28),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PocoIconView(
                PocoIcon.CHEVRON_LEFT,
                tint = colors.onAccentFill,
                modifier = Modifier.size(PocoSize.icon24)
            )
            Spacer(Modifier.width(PocoSpacing.space8))
            Text(text = "뒤로", style = POCOTheme.typography.titleM, color = colors.onAccentFill)
        }
        Spacer(Modifier.width(PocoSpacing.space24))
        Text(text = "펫 프로필", style = POCOTheme.typography.headingXl, color = colors.textPrimary)
        Spacer(Modifier.width(PocoSpacing.space24))
        PocoChip(
            text = if (uiState.isParked == false) "주행 중에는 수정할 수 없어요" else "정차 중에만 수정할 수 있어요",
            icon = PocoIcon.SHIELD,
            background = Color.Transparent
        )
        Spacer(Modifier.weight(1f))
        SaveStatus(uiState)
    }
}

@Composable
private fun PetListCard(
    uiState: PetProfileUiState,
    onPickPhoto: () -> Unit,
    onSelectPet: (Long) -> Unit,
    onAddPet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    val canEditPhoto = uiState.isParked != false && !uiState.isImportingPhoto
    val avatar = 200.dp
    Column(
        modifier = modifier
            .background(colors.backgroundCard, RoundedCornerShape(PocoRadius.r28))
            .verticalScroll(rememberScrollState())
            .padding(PocoSpacing.space40),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.clickable(enabled = canEditPhoto, onClick = onPickPhoto)
        ) {
            Box(
                modifier = Modifier
                    .size(avatar)
                    .background(colors.fieldBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // 사진 → 품종 아바타 → 기본 캐릭터
                PetPhoto(
                    photoPath = uiState.form.photoPath,
                    breed = uiState.form.breed,
                    modifier = Modifier.size(
                        if (hasFullPetImage(uiState.form.photoPath, uiState.form.breed)) {
                            avatar
                        } else {
                            avatar * 0.75f
                        }
                    )
                )
                if (uiState.isImportingPhoto) {
                    CircularProgressIndicator(
                        color = colors.accentFill,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(PocoSize.iconTile)
                    .background(colors.accentFill, CircleShape)
                    .border(PocoStroke.focus, colors.backgroundCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PocoIconView(
                    PocoIcon.CAMERA,
                    tint = colors.onAccentFill,
                    modifier = Modifier.size(PocoSize.icon28)
                )
            }
        }
        Spacer(Modifier.height(PocoSpacing.space20))
        Text(
            text = uiState.form.name.ifBlank { "새 펫" },
            style = POCOTheme.typography.headingXl,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(PocoSpacing.space20))
        HorizontalDivider(color = colors.borderDefault, thickness = PocoStroke.card)
        Spacer(Modifier.height(PocoSpacing.space20))
        Text(
            text = "등록된 펫",
            style = POCOTheme.typography.bodyM,
            color = colors.textSecondary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PocoSpacing.space12))
        uiState.pets.forEach { pet ->
            PetListItem(
                name = pet.name,
                summary = pet.summary(),
                photoPath = pet.photoPath,
                breed = pet.breed,
                selected = pet.id == uiState.form.id,
                onClick = { onSelectPet(pet.id) }
            )
            Spacer(Modifier.height(PocoSpacing.space12))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PocoSize.touchMin)
                .dashedBorder(colors.borderStrong)
                .clickable(onClick = onAddPet),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PocoIconView(
                PocoIcon.PLUS,
                tint = colors.textSecondary,
                modifier = Modifier.size(PocoSize.icon24)
            )
            Spacer(Modifier.width(PocoSpacing.space8))
            Text(text = "펫 추가하기", style = POCOTheme.typography.bodyL, color = colors.textSecondary)
        }
    }
}

@Composable
private fun PetFormCard(
    uiState: PetProfileUiState,
    editable: Boolean,
    onFormChange: ((PetForm) -> PetForm) -> Unit,
    onFormSelect: ((PetForm) -> PetForm) -> Unit,
    modifier: Modifier = Modifier
) {
    val form = uiState.form
    val colors = POCOTheme.colors
    Column(
        modifier = modifier
            .background(colors.backgroundCard, RoundedCornerShape(PocoRadius.r28))
            .verticalScroll(rememberScrollState())
            .padding(PocoSpacing.space32),
        verticalArrangement = Arrangement.spacedBy(PocoSpacing.space24)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space16)) {
            PocoInput(
                label = "이름",
                value = form.name,
                placeholder = "현댕이",
                enabled = editable,
                onValueChange = { v -> onFormChange { it.copy(name = v.toPetName()) } },
                modifier = Modifier.weight(1f)
            )
            PocoInput(
                label = "종류",
                value = form.breed,
                placeholder = "골든 리트리버",
                enabled = editable,
                onValueChange = { v -> onFormChange { it.copy(breed = v.take(BREED_MAX)) } },
                modifier = Modifier.weight(1f)
            )
            PocoInput(
                label = "나이",
                value = form.ageText,
                placeholder = "3살",
                unit = "살",
                inlineUnit = true,
                keyboardType = KeyboardType.Number,
                enabled = editable,
                onValueChange = { v ->
                    onFormChange { it.copy(ageText = v.filter(Char::isDigit).take(2)) }
                },
                modifier = Modifier.weight(AGE_FIELD_WEIGHT)
            )
            PocoInput(
                label = "몸무게",
                value = form.weightText,
                placeholder = "56kg",
                unit = "kg",
                inlineUnit = true,
                // 입력 전엔 예시 크기(대형)를 회색으로, 입력하면 실제 크기 뱃지
                trailing = { form.size?.let { SizeBadge(it) } ?: SizePlaceholder() },
                keyboardType = KeyboardType.Decimal,
                enabled = editable,
                onValueChange = { v ->
                    v.toWeightInput()?.let { w -> onFormChange { it.copy(weightText = w) } }
                },
                modifier = Modifier.weight(WEIGHT_FIELD_WEIGHT)
            )
        }
        TemperatureStandardLine(form)

        Column {
            SectionTitle(title = "탑승 자리", caption = "창문 알림 기준이 돼요")
            Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space12)) {
                PetSeat.entries.forEach { seat ->
                    PocoSegment(
                        title = seat.label(),
                        selected = form.seat == seat,
                        enabled = editable,
                        onClick = { onFormSelect { it.copy(seat = seat) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(PocoSpacing.space8))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PocoIconView(
                    PocoIcon.WARNING,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(PocoSize.icon24)
                )
                Spacer(Modifier.width(PocoSpacing.space8))
                Text(
                    text = "안고 탐을 고르면 출발 전에 자리로 옮기도록 안내해요 · 도로교통법 제39조 제5항",
                    style = POCOTheme.typography.caption,
                    color = colors.textSecondary
                )
            }
        }

        Column {
            SectionTitle(title = "멀미 민감도", caption = "높을수록 더 작은 흔들림부터 알려요")
            Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space12)) {
                MotionSensitivity.entries.forEach { level ->
                    PocoSegment(
                        title = level.label(),
                        subtitle = level.description(),
                        selected = form.motionSensitivity == level,
                        enabled = editable,
                        onClick = { onFormSelect { it.copy(motionSensitivity = level) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Column {
            SectionTitle(title = "고정 방식", caption = "몸이 고정돼 있으면 같은 흔들림에도 덜 쏠려요")
            Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space12)) {
                Restraint.entries.forEach { restraint ->
                    PocoSegment(
                        title = restraint.label(),
                        subtitle = restraint.description(),
                        selected = form.restraint == restraint,
                        enabled = editable,
                        onClick = { onFormSelect { it.copy(restraint = restraint) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Column {
            SectionTitle(title = "창문 알림 기준", caption = "이만큼 열리면 알려드려요 · 30km/h 이상 주행 중")
            Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space12)) {
                Pet.WINDOW_ALERT_OPTIONS.forEach { percent ->
                    PocoSegment(
                        title = "$percent%",
                        subtitle = if (percent == Pet.DEFAULT_WINDOW_ALERT_PERCENT) "기본값" else null,
                        selected = form.windowAlertPercent == percent,
                        enabled = editable,
                        onClick = { onFormSelect { it.copy(windowAlertPercent = percent) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        uiState.errorMessage?.let {
            Text(text = it, style = POCOTheme.typography.bodyM, color = colors.statusError)
        }
    }
}

/** 자동 저장 상태 (펫 추가하기 버튼 대신) */
@Composable
private fun SaveStatus(uiState: PetProfileUiState) {
    val colors = POCOTheme.colors
    val (text, done) = when {
        uiState.isParked == false -> "주행 중에는 저장되지 않아요" to false
        uiState.form.name.isBlank() -> "이름을 넣으면 바로 저장돼요" to false
        uiState.isSaving -> "저장하는 중…" to false
        uiState.isSaved -> "저장했어요" to true
        else -> "바꾸면 바로 저장돼요" to false
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (done) {
            PocoIconView(
                PocoIcon.CHECK,
                tint = colors.safe,
                modifier = Modifier.size(PocoSize.icon24)
            )
            Spacer(Modifier.width(PocoSpacing.space8))
        }
        Text(
            text = text,
            style = POCOTheme.typography.bodyL,
            color = if (done) colors.textPrimary else colors.textSecondary
        )
    }
}

/** POCO 기준점 ①: 종류 · 몸무게 · 나이로 정한 온도 기준을 바로 보여준다 */
@Composable
private fun TemperatureStandardLine(form: PetForm) {
    val colors = POCOTheme.colors
    val standard = PocoStandard.temperature(
        PocoStandard.petTypes(form.breed, form.weightKg, form.ageText.toIntOrNull())
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        PocoIconView(
            PocoIcon.THERMOMETER,
            tint = colors.textSecondary,
            modifier = Modifier.size(PocoSize.icon24)
        )
        Spacer(Modifier.width(PocoSpacing.space8))
        Text(
            text = "온도 기준 · ${standard.label} · 쾌적 " +
                "${standard.comfort.start.roundToInt()}~" +
                "${standard.comfort.endInclusive.roundToInt()}°C · " +
                "${standard.hotAt.roundToInt()}°C 이상 더움 · ${standard.coldAt.roundToInt()}°C 이하 추움",
            style = POCOTheme.typography.caption,
            color = colors.textSecondary
        )
    }
}

/** 몸무게를 넣기 전 예시 크기 */
@Composable
private fun SizePlaceholder() {
    Text(text = "대형", style = POCOTheme.typography.caption, color = POCOTheme.colors.textSecondary)
}

/** 몸무게 크기 뱃지: 소형 lime-soft · 중형 warn · 대형 네이비 */
@Composable
private fun SizeBadge(size: PetSize) {
    PocoTag(
        text = size.label(),
        style = when (size) {
            PetSize.SMALL -> PocoTagStyle.SUCCESS
            PetSize.MEDIUM -> PocoTagStyle.WARN
            PetSize.LARGE -> PocoTagStyle.NAVY
        }
    )
}

@Composable
private fun SectionTitle(title: String, caption: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = POCOTheme.typography.titleM,
            color = POCOTheme.colors.textPrimary
        )
        Spacer(Modifier.width(PocoSpacing.space12))
        Text(
            text = caption,
            style = POCOTheme.typography.caption,
            color = POCOTheme.colors.textSecondary
        )
    }
    Spacer(Modifier.height(PocoSpacing.space12))
}

private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(PocoRadius.r20.toPx()),
        style = Stroke(
            width = PocoStroke.dashed.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(PocoStroke.DASH_ON * density, PocoStroke.DASH_OFF * density)
            )
        )
    )
}

private const val NAME_MAX = 10
private const val BREED_MAX = 20

/** 나이(2자리 + 살)는 좁게, 몸무게(숫자 + kg + 크기 뱃지)는 넓게 */
private const val AGE_FIELD_WEIGHT = 0.75f
private const val WEIGHT_FIELD_WEIGHT = 1.3f

/** 이름: 한글·영문(+공백) 최대 10자. 한글 조합 중인 자모도 글자로 허용 */
private fun String.toPetName(): String = filter { it.isLetter() || it == ' ' }.take(NAME_MAX)

/** 몸무게: 숫자와 점 하나, 소수점 한 자리까지. 규칙에 안 맞으면 null(입력 무시) */
private fun String.toWeightInput(): String? {
    val filtered = filter { it.isDigit() || it == '.' }
    if (filtered.count { it == '.' } > 1) return null
    if (filtered.substringAfter('.', "").length > 1) return null
    return filtered.take(5)
}

private fun previewPet(weight: Float) = Pet(
    id = 1,
    name = "현댕이",
    breed = "몰티즈",
    ageYears = 3,
    weightKg = weight,
    seat = PetSeat.REAR_LEFT,
    motionSensitivity = MotionSensitivity.NORMAL
)

@Preview(widthDp = 1920, heightDp = 992)
@Composable
private fun PetProfileScreenPreview() {
    val pet = previewPet(4.2f)
    POCOTheme(darkTheme = false) {
        PetProfileScreen(
            uiState = PetProfileUiState(
                pets = persistentListOf(pet),
                form = PetForm.from(pet),
                isParked = true
            ),
            onBack = {},
            onPickPhoto = {},
            onFormChange = {},
            onFormSelect = {},
            onSelectPet = {},
            onAddPet = {}
        )
    }
}

@Preview(widthDp = 1920, heightDp = 992, name = "새 펫 (예시 placeholder)")
@Composable
private fun PetProfileEmptyPreview() {
    POCOTheme(darkTheme = false) {
        PetProfileScreen(
            uiState = PetProfileUiState(),
            onBack = {},
            onPickPhoto = {},
            onFormChange = {},
            onFormSelect = {},
            onSelectPet = {},
            onAddPet = {}
        )
    }
}

@Preview(widthDp = 1920, heightDp = 992, name = "다크 모드 · 대형")
@Composable
private fun PetProfileDarkPreview() {
    val pet = previewPet(28f)
    POCOTheme(darkTheme = true) {
        PetProfileScreen(
            uiState = PetProfileUiState(
                pets = persistentListOf(pet),
                form = PetForm.from(pet),
                isParked = true
            ),
            onBack = {},
            onPickPhoto = {},
            onFormChange = {},
            onFormSelect = {},
            onSelectPet = {},
            onAddPet = {}
        )
    }
}
