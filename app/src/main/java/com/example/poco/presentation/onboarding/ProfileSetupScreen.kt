package com.example.poco.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.PetPhoto
import com.example.poco.core.designsystem.component.PocoButton
import com.example.poco.core.designsystem.component.PocoButtonSize
import com.example.poco.core.designsystem.component.PocoButtonStyle
import com.example.poco.core.designsystem.component.PocoToggle
import com.example.poco.core.theme.MungoLayout
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.MungoSize
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.theme.PocoSpacing
import com.example.poco.core.util.eunNeun
import com.example.poco.core.util.waGwa
import com.example.poco.data.model.PetSize
import com.example.poco.data.model.PetSpecies
import com.example.poco.data.model.PetTrait
import com.example.poco.domain.driving.PocoStandard
import com.example.poco.presentation.petprofile.PetForm
import com.example.poco.presentation.petprofile.PetPhotoPickerDialog
import com.example.poco.presentation.petprofile.label
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * 온보딩 프로필 만들기 (피그마 온보딩 2 ~ 5페이지)
 * 1/3 기본 정보 → 2/3 차에서 어떤 편인가요? → 3/3 어떤 안내를 받으시겠어요? → 프로필 완성
 * 1~3단계는 왼쪽 패널(단계 · 제목 · 지금까지 입력한 정보 · 이전/완료)이 같고 오른쪽만 바뀐다
 */

@Composable
fun ProfileSetupRoute(
    navigateBack: () -> Unit,
    navigateNext: () -> Unit,
    viewModel: ProfileSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 사진: 앱 안 큰 선택기 → 필요하면 시스템 선택기 (펫 프로필 화면과 같은 방식)
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
        if (uiState.isDone) {
            viewModel.consumeDone()
            navigateNext()
        }
    }

    val goBack = { if (!viewModel.back()) navigateBack() }
    BackHandler(onBack = goBack)

    val actions = ProfileSetupActions(
        onBack = goBack,
        onNext = viewModel::next,
        onFinish = viewModel::finish,
        onNameChange = viewModel::updateName,
        onSpeciesSelect = viewModel::selectSpecies,
        onBreedChange = viewModel::updateBreed,
        onAgeChange = viewModel::changeAge,
        onWeightChange = viewModel::changeWeight,
        onAgeType = viewModel::typeAge,
        onWeightType = viewModel::typeWeight,
        onTraitToggle = viewModel::toggleTrait,
        onVoiceGuideChange = viewModel::setVoiceGuide,
        onTemperatureAlertChange = viewModel::setTemperatureAlert,
        onWindowAlertChange = viewModel::setWindowAlert,
        onRestSuggestionChange = viewModel::setRestSuggestion,
        onPhotoClick = { showPhotoPicker = true }
    )
    ProfileSetupScreen(uiState = uiState, actions = actions)
}

/** 화면에서 ViewModel로 보내는 동작 모음 */
private class ProfileSetupActions(
    val onBack: () -> Unit,
    val onNext: () -> Unit,
    val onFinish: () -> Unit,
    val onNameChange: (String) -> Unit,
    val onSpeciesSelect: (PetSpecies) -> Unit,
    val onBreedChange: (String) -> Unit,
    val onAgeChange: (Int) -> Unit,
    val onWeightChange: (Int) -> Unit,
    val onAgeType: (String) -> Unit,
    val onWeightType: (String) -> Unit,
    val onTraitToggle: (PetTrait) -> Unit,
    val onVoiceGuideChange: (Boolean) -> Unit,
    val onTemperatureAlertChange: (Boolean) -> Unit,
    val onWindowAlertChange: (Boolean) -> Unit,
    val onRestSuggestionChange: (Boolean) -> Unit,
    val onPhotoClick: () -> Unit
) {
    companion object {
        val Preview = ProfileSetupActions(
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Composable
private fun ProfileSetupScreen(
    uiState: ProfileSetupUiState,
    actions: ProfileSetupActions,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .safeDrawingPadding()
            .padding(MungoLayout.screenPadding)
    ) {
        if (uiState.step == ProfileStep.COMPLETE) {
            CompleteContent(
                uiState = uiState,
                actions = actions,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                StepPanel(
                    uiState = uiState,
                    actions = actions,
                    modifier = Modifier
                        .width(560.dp)
                        .fillMaxHeight()
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (uiState.step) {
                        ProfileStep.BASIC -> BasicInfoContent(uiState.form, actions)
                        ProfileStep.TRAITS -> TraitsContent(uiState.form.traits, actions)
                        ProfileStep.ALERTS -> AlertsContent(uiState, actions)
                        ProfileStep.COMPLETE -> Unit
                    }
                }
            }
        }
    }
}

// region 왼쪽 패널

@Composable
private fun StepPanel(
    uiState: ProfileSetupUiState,
    actions: ProfileSetupActions,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    val form = uiState.form
    val (title, description) = when (uiState.step) {
        ProfileStep.BASIC ->
            "함께 타는\n반려동물을\n소개해 주세요" to "정보를 입력해주시면\n맞춤 안내를 준비할게요."
        ProfileStep.TRAITS ->
            "${form.name.trim().eunNeun} 차에서\n어떤 편인가요?" to
                "해당하는 모습을 골라 주세요.\n고른 모습에 맞춰 지켜볼게요."
        else -> "어떤 안내를\n받으시겠어요?" to "나중에 설정에서 언제든\n바꿀 수 있어요."
    }
    Column(
        modifier = modifier
            .background(colors.bgSurface, RoundedCornerShape(MungoRadius.xl))
            .padding(horizontal = 56.dp, vertical = 48.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "프로필 만들기",
                style = POCOTheme.typography.body2,
                color = colors.textTertiary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${uiState.step.number} / ${ProfileStep.TOTAL}",
                style = POCOTheme.typography.body2,
                color = colors.textTertiary
            )
        }
        Spacer(Modifier.height(PocoSpacing.space16))
        Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space12)) {
            repeat(ProfileStep.TOTAL) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(
                            if (index <
                                uiState.step.number
                            ) {
                                colors.bgAccent
                            } else {
                                colors.borderDefault
                            },
                            CircleShape
                        )
                )
            }
        }
        Spacer(Modifier.height(72.dp))
        Text(
            text = title,
            style = POCOTheme.typography.h2.copy(lineHeight = 72.sp),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(PocoSpacing.space32))
        Text(text = description, style = POCOTheme.typography.body2, color = colors.textSecondary)
        Spacer(Modifier.weight(1f))
        HorizontalDivider(thickness = 2.dp, color = colors.borderDefault)
        Spacer(Modifier.height(PocoSpacing.space40))
        Text(
            text = "지금까지 입력한 정보",
            style = POCOTheme.typography.body3,
            color = colors.textTertiary
        )
        Spacer(Modifier.height(PocoSpacing.space16))
        SummaryRow("이름", form.name.trim())
        SummaryRow("종류", form.species.label())
        SummaryRow("품종", form.breed.trim())
        SummaryRow("나이", form.ageText.toIntOrNull()?.let { "${it}살" }.orEmpty())
        SummaryRow("몸무게", form.weightKg?.let { "${it.toWeightLabel()}kg" }.orEmpty())
        uiState.errorMessage?.let {
            Spacer(Modifier.height(PocoSpacing.space12))
            Text(text = it, style = POCOTheme.typography.body3, color = colors.statusAlert)
        }
        Spacer(Modifier.height(PocoSpacing.space40))
        Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space20)) {
            PocoButton(
                text = "이전",
                onClick = actions.onBack,
                style = PocoButtonStyle.OUTLINE,
                size = PocoButtonSize.LARGE,
                shape = CircleShape,
                modifier = Modifier
                    .width(160.dp)
                    .height(96.dp)
            )
            PocoButton(
                text = "완료",
                onClick = actions.onNext,
                enabled = uiState.canGoNext,
                size = PocoButtonSize.LARGE,
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = POCOTheme.typography.body2,
            color = POCOTheme.colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.ifEmpty { "-" },
            style = POCOTheme.typography.title3.copy(fontWeight = FontWeight.Bold),
            color = with(POCOTheme.colors) { if (value.isEmpty()) textDisabled else textPrimary }
        )
    }
}

// endregion

// region 1/3 기본 정보

@Composable
private fun BasicInfoContent(form: PetForm, actions: ProfileSetupActions) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PocoSpacing.space24)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space40)) {
            LabeledField("이름", Modifier.weight(1f)) {
                SetupTextField(
                    value = form.name,
                    onValueChange = actions.onNameChange,
                    placeholder = "예: 현댕이"
                )
            }
            LabeledField("종류", Modifier.weight(1f)) {
                SpeciesDropdown(selected = form.species, onSelect = actions.onSpeciesSelect)
            }
        }
        LabeledField("품종") {
            SetupTextField(
                value = form.breed,
                onValueChange = actions.onBreedChange,
                placeholder = "예: 시츄",
                trailingIcon = MungoIcon.SEARCH
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space24)) {
            StepperCard(
                label = "나이",
                text = form.ageText,
                unit = "살",
                keyboardType = KeyboardType.Number,
                onTextChange = actions.onAgeType,
                onMinus = { actions.onAgeChange(-1) },
                onPlus = { actions.onAgeChange(1) },
                modifier = Modifier.weight(1f)
            )
            StepperCard(
                label = "몸무게",
                text = form.weightText,
                unit = "kg",
                keyboardType = KeyboardType.Decimal,
                onTextChange = actions.onWeightType,
                onMinus = { actions.onWeightChange(-1) },
                onPlus = { actions.onWeightChange(1) },
                modifier = Modifier.weight(1f)
            )
        }
        LabeledField("크기") {
            SizeSegment(selected = form.size)
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = POCOTheme.typography.body3.copy(fontWeight = FontWeight.Bold),
            color = POCOTheme.colors.textSecondary
        )
        Spacer(Modifier.height(PocoSpacing.space12))
        content()
    }
}

private val KoreanLocales = LocaleList(Locale("ko-KR"))

/** 흰 입력칸 (높이 88 · 라운드 16). 포커스되면 망고 테두리 */
@Composable
internal fun SetupTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    trailingIcon: MungoIcon? = null
) {
    val colors = POCOTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(MungoRadius.md)
    val textStyle = POCOTheme.typography.title2.copy(fontWeight = FontWeight.Bold)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interaction,
        textStyle = textStyle.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.bgAccent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, hintLocales = KoreanLocales),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MungoSize.input)
                    .background(colors.bgSurface, shape)
                    .then(
                        if (focused) Modifier.border(3.dp, colors.borderAccent, shape) else Modifier
                    )
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(fontWeight = FontWeight.Normal),
                            color = colors.textTertiary
                        )
                    }
                    inner()
                }
                trailingIcon?.let {
                    MungoIconView(it, tint = colors.textSecondary, size = 32.dp)
                }
            }
        }
    )
}

@Composable
internal fun SpeciesDropdown(selected: PetSpecies, onSelect: (PetSpecies) -> Unit) {
    val colors = POCOTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(MungoRadius.md)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MungoSize.input)
                .clip(shape)
                .background(colors.bgSurface)
                .clickable { expanded = true }
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected.label(),
                style = POCOTheme.typography.title2.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            MungoIconView(MungoIcon.CHEVRON_DOWN, tint = colors.textSecondary, size = 32.dp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.bgSurfaceRaised,
            shape = shape
        ) {
            PetSpecies.entries.forEach { species ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = species.label(),
                            style = POCOTheme.typography.title2,
                            color = if (species ==
                                selected
                            ) {
                                colors.textAccent
                            } else {
                                colors.textPrimary
                            }
                        )
                    },
                    onClick = {
                        onSelect(species)
                        expanded = false
                    },
                    modifier = Modifier
                        .widthIn(min = 320.dp)
                        .height(MungoSize.touchMin)
                )
            }
        }
    }
}

/** 나이 · 몸무게: 큰 숫자(누르면 숫자 키패드) + 단위, 아래 − / + (길게 누르면 계속 바뀜) */
@Composable
private fun StepperCard(
    label: String,
    text: String,
    unit: String,
    keyboardType: KeyboardType,
    onTextChange: (String) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    val focusManager = LocalFocusManager.current
    val numberStyle = POCOTheme.typography.display1.copy(fontSize = 112.sp, lineHeight = 112.sp)
    Column(
        modifier = modifier
            .background(colors.bgSurface, RoundedCornerShape(MungoRadius.lg))
            .padding(horizontal = 40.dp, vertical = 36.dp)
    ) {
        Text(text = label, style = POCOTheme.typography.title2, color = colors.textSecondary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = numberStyle.copy(color = colors.textPrimary, textAlign = TextAlign.End),
                cursorBrush = SolidColor(colors.bgAccent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (text.isEmpty()) {
                            Text(text = "-", style = numberStyle, color = colors.textDisabled)
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .alignByBaseline()
                    .width(IntrinsicSize.Min)
                    .widthIn(min = 64.dp)
                    .semantics { contentDescription = "$label 입력" }
            )
            Spacer(Modifier.width(PocoSpacing.space4))
            Text(
                text = unit,
                style = POCOTheme.typography.title1,
                color = colors.textPrimary,
                modifier = Modifier.alignByBaseline()
            )
        }
        Spacer(Modifier.height(PocoSpacing.space16))
        Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space24)) {
            RepeatButton(MungoIcon.MINUS, "$label 줄이기", onMinus)
            RepeatButton(MungoIcon.PLUS, "$label 늘리기", onPlus)
        }
    }
}

/** 한 번 누르면 1번, 길게 누르면 0.4초 뒤부터 계속 */
@Composable
private fun RowScope.RepeatButton(icon: MungoIcon, description: String, onStep: () -> Unit) {
    val colors = POCOTheme.colors
    val currentOnStep by rememberUpdatedState(onStep)
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(MungoSize.buttonLarge)
            .clip(CircleShape)
            .background(if (pressed) colors.statePressed else colors.bgSurface)
            .border(2.dp, colors.borderDefault, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        coroutineScope {
                            val repeat = launch {
                                currentOnStep()
                                delay(REPEAT_START_MS)
                                while (true) {
                                    currentOnStep()
                                    delay(REPEAT_INTERVAL_MS)
                                }
                            }
                            tryAwaitRelease()
                            repeat.cancel()
                        }
                        pressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        MungoIconView(
            icon,
            tint = colors.textPrimary,
            size = 36.dp,
            contentDescription = description
        )
    }
}

private const val REPEAT_START_MS = 400L
private const val REPEAT_INTERVAL_MS = 70L

/** 크기: 몸무게로 자동 (소형 10kg 미만 · 중형 25kg 미만 · 대형). 누를 수 없음 */
@Composable
private fun SizeSegment(selected: PetSize?) {
    val colors = POCOTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MungoSize.segment)
            .background(colors.bgSurface, RoundedCornerShape(MungoRadius.md)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PetSize.entries.forEach { size ->
            Text(
                text = size.label(),
                style = POCOTheme.typography.title2.copy(
                    fontWeight = if (size == selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (size == selected) colors.textAccent else colors.textDisabled,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// endregion

// region 2/3 차에서 어떤 편인가요?

private fun PetTrait.icon(): MungoIcon = when (this) {
    PetTrait.MOTION_SICK -> MungoIcon.SPIRAL
    PetTrait.ANXIOUS -> MungoIcon.ACTIVITY
    PetTrait.HEAT_SENSITIVE -> MungoIcon.SUN
    PetTrait.COLD_SENSITIVE -> MungoIcon.SNOWFLAKE
    PetTrait.DROOLS -> MungoIcon.DROPLET
    PetTrait.LOVES_RIDES -> MungoIcon.HEART
    PetTrait.SENIOR -> MungoIcon.CLOCK
    PetTrait.JOINT_WEAK -> MungoIcon.ACTIVITY
}

@Composable
private fun TraitsContent(selected: Set<PetTrait>, actions: ProfileSetupActions) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PocoSpacing.space20)
    ) {
        ONBOARDING_TRAITS.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space20)) {
                row.forEach { trait ->
                    TraitCard(
                        trait = trait,
                        selected = trait in selected,
                        onClick = { actions.onTraitToggle(trait) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(Modifier.height(PocoSpacing.space12))
        InfoBar("고른 모습은 마이펫 > 케어 기준에서 언제든 바꿀 수 있어요.")
    }
}

@Composable
private fun TraitCard(
    trait: PetTrait,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(MungoRadius.lg)
    Box(
        modifier = modifier
            .height(300.dp)
            .clip(shape)
            .background(if (selected) colors.bgAccentSubtle else colors.bgSurface)
            .then(if (selected) Modifier.border(3.dp, colors.bgAccent, shape) else Modifier)
            .clickable(onClick = onClick)
    ) {
        CheckSquare(
            checked = selected,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(PocoSpacing.space24)
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        if (selected) colors.bgSurface else colors.bgAccentSubtle,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                MungoIconView(trait.icon(), tint = colors.textAccent, size = 36.dp)
            }
            Spacer(Modifier.height(PocoSpacing.space24))
            Text(
                text = trait.title(),
                style = POCOTheme.typography.title1,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(PocoSpacing.space8))
            Text(
                text = trait.description(),
                style = POCOTheme.typography.body3,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun CheckSquare(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(MungoRadius.sm)
    Box(
        modifier = modifier
            .size(40.dp)
            .background(if (checked) colors.bgAccent else colors.bgSurface, shape)
            .then(if (checked) Modifier else Modifier.border(2.dp, colors.borderStrong, shape)),
        contentAlignment = Alignment.Center
    ) {
        if (checked) MungoIconView(MungoIcon.CHECK, tint = colors.textOnAccent, size = 28.dp)
    }
}

/** 흰 알약 안내 바: (i) + 한 줄 */
@Composable
private fun InfoBar(text: String) {
    val colors = POCOTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .background(colors.bgSurface, CircleShape)
            .padding(horizontal = PocoSpacing.space32),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MungoIconView(MungoIcon.INFO, tint = colors.textSecondary, size = 26.dp)
        Spacer(Modifier.width(PocoSpacing.space12))
        Text(text = text, style = POCOTheme.typography.body2, color = colors.textSecondary)
    }
}

// endregion

// region 3/3 어떤 안내를 받으시겠어요?

@Composable
private fun AlertsContent(uiState: ProfileSetupUiState, actions: ProfileSetupActions) {
    val form = uiState.form
    val comfort = remember(form.breed, form.weightKg, form.ageText) {
        PocoStandard.temperature(
            PocoStandard.petTypes(form.breed, form.weightKg, form.ageText.toIntOrNull())
        ).comfort
    }
    val name = form.name.trim()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PocoSpacing.space20)
    ) {
        AlertRow(
            icon = MungoIcon.SPEAKER,
            title = "글레오 음성 안내",
            description = "급제동 · 급커브가 있으면 음성으로 알려 드려요.",
            checked = form.voiceGuideEnabled,
            onCheckedChange = actions.onVoiceGuideChange
        )
        AlertRow(
            icon = MungoIcon.THERMOMETER,
            title = "실내 온도 알림",
            description = "차 내 온도가 ${comfort.start.roundToInt()}~" +
                "${comfort.endInclusive.roundToInt()}°C를 벗어나면 알려 드려요.",
            checked = form.temperatureAlertEnabled,
            onCheckedChange = actions.onTemperatureAlertChange,
            recommended = uiState.isTemperatureRecommended
        )
        AlertRow(
            icon = MungoIcon.WINDOW,
            title = "창문 안전 알림",
            description = "창문이 ${form.windowAlertPercent}% 넘게 열렸을 때 알려 드려요.",
            checked = form.windowAlertEnabled,
            onCheckedChange = actions.onWindowAlertChange
        )
        AlertRow(
            icon = MungoIcon.COFFEE,
            title = "휴식 쉼터 제안",
            description = "${name.ifEmpty { "반려동물" }.waGwa} 쉴 타이밍을 알려 드려요.",
            checked = form.restSuggestionEnabled,
            onCheckedChange = actions.onRestSuggestionChange,
            recommended = uiState.isRestRecommended
        )
        Spacer(Modifier.height(PocoSpacing.space12))
        InfoBar("앞에서 선택한 모습에 맞춰 설정을 추천드려요.")
    }
}

@Composable
private fun AlertRow(
    icon: MungoIcon,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    recommended: Boolean = false
) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(MungoRadius.lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .clip(shape)
            .background(colors.bgSurface)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(colors.bgAccentSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(icon, tint = colors.textAccent, size = 32.dp)
        }
        Spacer(Modifier.width(PocoSpacing.space24))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = POCOTheme.typography.title1, color = colors.textPrimary)
                if (recommended) {
                    Spacer(Modifier.width(PocoSpacing.space12))
                    Text(
                        text = "추천",
                        style = POCOTheme.typography.body3.copy(fontWeight = FontWeight.Bold),
                        color = colors.textAccent,
                        modifier = Modifier
                            .background(colors.bgAccentSubtle, CircleShape)
                            .padding(
                                horizontal = PocoSpacing.space16,
                                vertical = PocoSpacing.space4
                            )
                    )
                }
            }
            Spacer(Modifier.height(PocoSpacing.space4))
            Text(
                text = description,
                style = POCOTheme.typography.body2,
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.width(PocoSpacing.space24))
        PocoToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// endregion

// region 완성

@Composable
private fun CompleteContent(
    uiState: ProfileSetupUiState,
    actions: ProfileSetupActions,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    val form = uiState.form
    val name = form.name.trim()
    Column(
        modifier = modifier.width(860.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .background(colors.bgAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(MungoIcon.CHECK, tint = colors.textOnAccent, size = 64.dp)
        }
        Spacer(Modifier.height(PocoSpacing.space40))
        Text(
            text = "${name}의 프로필을 완성했어요",
            style = POCOTheme.typography.h2,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(PocoSpacing.space16))
        Text(
            text = "입력하신 정보가 맞는지 확인해주세요.",
            style = POCOTheme.typography.body1,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(PocoSpacing.space48))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(248.dp)
                .background(colors.bgSurface, RoundedCornerShape(MungoRadius.xl))
                .padding(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhotoAvatar(
                photoPath = form.photoPath,
                breed = form.breed,
                isLoading = uiState.isImportingPhoto,
                onClick = actions.onPhotoClick
            )
            Spacer(Modifier.width(PocoSpacing.space48))
            Text(
                text = name,
                style = POCOTheme.typography.h3,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = form.summaryLine(),
                    style = POCOTheme.typography.body1,
                    color = colors.textSecondary
                )
                if (form.traits.isNotEmpty()) {
                    Spacer(Modifier.height(PocoSpacing.space12))
                    Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space8)) {
                        form.traits.sortedBy { it.ordinal }.forEach { trait ->
                            Text(
                                text = trait.shortLabel(),
                                style = POCOTheme.typography.title3.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.textAccent,
                                modifier = Modifier
                                    .background(colors.bgAccentSubtle, CircleShape)
                                    .padding(
                                        horizontal = PocoSpacing.space20,
                                        vertical = PocoSpacing.space8
                                    )
                            )
                        }
                    }
                }
            }
        }
        uiState.errorMessage?.let {
            Spacer(Modifier.height(PocoSpacing.space16))
            Text(text = it, style = POCOTheme.typography.body2, color = colors.statusAlert)
        }
        Spacer(Modifier.height(PocoSpacing.space48))
        Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space40)) {
            PocoButton(
                text = "이전",
                onClick = actions.onBack,
                style = PocoButtonStyle.OUTLINE,
                size = PocoButtonSize.LARGE,
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
            )
            PocoButton(
                text = if (uiState.isSaving) "저장 중…" else "시작하기",
                onClick = actions.onFinish,
                enabled = !uiState.isSaving,
                size = PocoButtonSize.LARGE,
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
            )
        }
    }
}

/** 사진(없으면 품종 아바타 · 발바닥) + 오른쪽 아래 카메라 뱃지. 누르면 사진 고르기 */
@Composable
private fun PhotoAvatar(
    photoPath: String?,
    breed: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    size: Dp = 156.dp
) {
    val colors = POCOTheme.colors
    Box(
        modifier = Modifier
            .size(size)
            .clickable(enabled = !isLoading, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(colors.bgAccentSubtle, CircleShape)
        ) {
            PetPhoto(photoPath = photoPath, breed = breed, modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
                .size(52.dp)
                .background(colors.bgSurface, CircleShape)
                .border(2.dp, colors.borderDefault, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(
                MungoIcon.CAMERA,
                tint = colors.textAccent,
                size = 26.dp,
                contentDescription = "사진 바꾸기"
            )
        }
    }
}

// endregion

// region Preview

private val previewForm = PetForm(
    name = "현댕이",
    breed = "시츄",
    ageText = "10",
    weightText = "7.0",
    traits = setOf(PetTrait.MOTION_SICK, PetTrait.HEAT_SENSITIVE)
)

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun BasicPreview() {
    POCOTheme(darkTheme = false) {
        ProfileSetupScreen(ProfileSetupUiState(form = previewForm), ProfileSetupActions.Preview)
    }
}

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun TraitsPreview() {
    POCOTheme(darkTheme = false) {
        ProfileSetupScreen(
            ProfileSetupUiState(step = ProfileStep.TRAITS, form = previewForm),
            ProfileSetupActions.Preview
        )
    }
}

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun AlertsPreview() {
    POCOTheme(darkTheme = false) {
        ProfileSetupScreen(
            ProfileSetupUiState(step = ProfileStep.ALERTS, form = previewForm),
            ProfileSetupActions.Preview
        )
    }
}

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun CompletePreview() {
    POCOTheme(darkTheme = false) {
        ProfileSetupScreen(
            ProfileSetupUiState(step = ProfileStep.COMPLETE, form = previewForm),
            ProfileSetupActions.Preview
        )
    }
}

// endregion
