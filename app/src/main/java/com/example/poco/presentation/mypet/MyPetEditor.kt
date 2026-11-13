package com.example.poco.presentation.mypet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.component.pressable
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.POCOTheme
import com.example.poco.data.model.PetSize
import com.example.poco.presentation.onboarding.SetupTextField
import com.example.poco.presentation.onboarding.SpeciesDropdown
import com.example.poco.presentation.onboarding.rememberNumberWidth
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* 마이펫 오른쪽: 프로필 수정 / 새 반려견 추가 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PetEditor(uiState: MyPetUiState, actions: MyPetActions) {
    val colors = POCOTheme.colors
    val form = uiState.form
    val isAdd = uiState.mode == MyPetMode.ADD
    Column(modifier = Modifier.fillMaxSize()) {
        // 제목 줄: ‹ 프로필 수정 · 취소 · 수정
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (uiState.pets.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(colors.bgSurface, CircleShape)
                        .pressable(CircleShape, onClick = actions.onCancel),
                    contentAlignment = Alignment.Center
                ) {
                    MungoIconView(
                        MungoIcon.ARROW_LEFT,
                        size = 32.dp,
                        contentDescription = "뒤로"
                    )
                }
                Spacer(Modifier.width(28.dp))
            }
            Text(
                text = if (isAdd) "반려견 추가" else "프로필 수정",
                style = POCOTheme.typography.h3,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (uiState.pets.isNotEmpty()) {
                PillButton(text = "취소", onClick = actions.onCancel)
                Spacer(Modifier.width(16.dp))
            }
            PillButton(
                text = if (isAdd) "추가" else "수정",
                onClick = actions.onSave,
                enabled = uiState.canSave,
                primary = true
            )
        }
        Spacer(Modifier.height(32.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // 사진 · 이름 · 종
            Row(verticalAlignment = Alignment.Top) {
                EditablePhoto(
                    photoPath = form.photoPath,
                    breed = form.breed,
                    isLoading = uiState.isImportingPhoto,
                    onClick = actions.onPickPhoto
                )
                Spacer(Modifier.width(36.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Field("이름", Modifier.weight(NAME_WEIGHT)) {
                        SetupTextField(
                            value = form.name,
                            onValueChange = { v ->
                                actions.onFormChange { it.copy(name = v.take(NAME_MAX)) }
                            },
                            placeholder = "예: 현댕이"
                        )
                    }
                    Field("종", Modifier.weight(1f)) {
                        SpeciesDropdown(
                            selected = form.species,
                            onSelect = { s -> actions.onFormChange { it.copy(species = s) } }
                        )
                    }
                }
            }
            Field("품종") {
                SetupTextField(
                    value = form.breed,
                    onValueChange = { v ->
                        actions.onFormChange { it.copy(breed = v.take(NAME_MAX)) }
                    },
                    placeholder = "예: 시츄",
                    trailingIcon = MungoIcon.SEARCH
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                NumberStepper(
                    label = "나이",
                    text = form.ageText,
                    unit = "살",
                    keyboardType = KeyboardType.Number,
                    onTextChange = actions.onAgeType,
                    onStep = actions.onAgeStep,
                    step = 1,
                    modifier = Modifier.weight(1f)
                )
                NumberStepper(
                    label = "몸무게",
                    text = form.weightText,
                    unit = "kg",
                    keyboardType = KeyboardType.Decimal,
                    onTextChange = actions.onWeightType,
                    onStep = actions.onWeightStep,
                    step = 1,
                    modifier = Modifier.weight(1f)
                )
            }
            Field("몸 크기") {
                SizeSegment(selected = form.size)
            }
            Field("케어 기준 · 해당하는 걸 모두 골라 주세요") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CARE_TRAITS.forEach { trait ->
                        CareChip(
                            text = trait.careLabel(),
                            selected = trait in form.traits,
                            onClick = { actions.onToggleCare(trait) }
                        )
                    }
                }
            }
            uiState.errorMessage?.let {
                Text(text = it, style = POCOTheme.typography.body2, color = colors.statusAlert)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MungoIconView(MungoIcon.INFO, tint = colors.textTertiary, size = 24.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "바뀐 내용은 다음 이동부터 반영돼요.",
                    style = POCOTheme.typography.body3,
                    color = colors.textTertiary,
                    modifier = Modifier.weight(1f)
                )
                if (!isAdd) {
                    Text(
                        text = "${form.name.ifBlank { "이 반려견" }} 삭제",
                        style = POCOTheme.typography.title3,
                        color = colors.statusAlert,
                        modifier = Modifier
                            .clip(RoundedCornerShape(MungoRadius.md))
                            .pressable(
                                RoundedCornerShape(MungoRadius.md),
                                onClick = actions.onDelete
                            )
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditablePhoto(
    photoPath: String?,
    breed: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val colors = POCOTheme.colors
    // pressable은 동그랗게 잘라내므로 사진에만 걸고, 카메라 뱃지는 바깥에 둔다
    Box {
        Box(modifier = Modifier.pressable(CircleShape, enabled = !isLoading, onClick = onClick)) {
            PetAvatar(photoPath, breed, PHOTO_SIZE)
        }
        if (isLoading) {
            CircularProgressIndicator(
                color = colors.bgAccent,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
                .size(56.dp)
                .background(colors.bgSurface, CircleShape)
                .border(2.dp, colors.borderDefault, CircleShape)
                .pressable(CircleShape, enabled = !isLoading, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(
                MungoIcon.CAMERA,
                tint = colors.textAccent,
                size = 28.dp,
                contentDescription = "사진 바꾸기"
            )
        }
    }
}

@Composable
private fun Field(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = POCOTheme.typography.body3,
            color = POCOTheme.colors.textSecondary
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** 큰 숫자(누르면 숫자 키패드) + 단위, 오른쪽 − / + (길게 누르면 계속) */
@Composable
private fun NumberStepper(
    label: String,
    text: String,
    unit: String,
    keyboardType: KeyboardType,
    onTextChange: (String) -> Unit,
    onStep: (Int) -> Unit,
    step: Int,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    val focusManager = LocalFocusManager.current
    val numberStyle = POCOTheme.typography.h1
    // IntrinsicSize.Min은 +/-로 값이 바뀌어도 너비가 갱신되지 않아 숫자가 안 보인다 → 글자 폭을 직접 잰다
    val numberWidth = rememberNumberWidth(text.ifEmpty { "-" }, numberStyle)
    Column(
        modifier = modifier
            .background(colors.bgSurface, RoundedCornerShape(MungoRadius.xl))
            .padding(horizontal = 36.dp, vertical = 28.dp)
    ) {
        Text(text = label, style = POCOTheme.typography.body3, color = colors.textSecondary)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    singleLine = true,
                    textStyle = numberStyle.copy(
                        color = colors.textPrimary,
                        textAlign = TextAlign.Start
                    ),
                    cursorBrush = SolidColor(colors.bgAccent),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    decorationBox = { inner ->
                        Box {
                            if (text.isEmpty()) {
                                Text(text = "-", style = numberStyle, color = colors.textDisabled)
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .width(numberWidth)
                        .semantics { contentDescription = "$label 입력" }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = unit,
                    style = POCOTheme.typography.title2.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            RepeatCircle(MungoIcon.MINUS, "$label 줄이기") { onStep(-step) }
            Spacer(Modifier.width(16.dp))
            RepeatCircle(MungoIcon.PLUS, "$label 늘리기") { onStep(step) }
        }
    }
}

/** 한 번 누르면 1번, 길게 누르면 0.4초 뒤부터 계속 */
@Composable
private fun RepeatCircle(icon: MungoIcon, description: String, onStep: () -> Unit) {
    val colors = POCOTheme.colors
    val currentOnStep by rememberUpdatedState(onStep)
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(if (pressed) colors.statePressed else colors.bgSurfaceSunken)
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
            size = 32.dp,
            contentDescription = description
        )
    }
}

/** 몸 크기: 몸무게로 자동 (누를 수 없음) */
@Composable
private fun SizeSegment(selected: PetSize?) {
    val colors = POCOTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(colors.bgSurfaceSunken, RoundedCornerShape(MungoRadius.lg))
            .padding(6.dp)
    ) {
        PetSize.entries.forEach { size ->
            val isOn = size == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isOn) {
                            Modifier
                                .background(colors.bgSurface, RoundedCornerShape(MungoRadius.md))
                                .border(
                                    3.dp,
                                    colors.borderAccent,
                                    RoundedCornerShape(MungoRadius.md)
                                )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = size.rangeLabel(),
                    style = POCOTheme.typography.body2.copy(
                        fontWeight = if (isOn) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isOn) colors.textPrimary else colors.textSecondary
                )
            }
        }
    }
}

private fun PetSize.rangeLabel(): String = when (this) {
    PetSize.SMALL -> "소형 · 10kg 미만"
    PetSize.MEDIUM -> "중형 · 10~25kg"
    PetSize.LARGE -> "대형 · 25kg 이상"
}

private val PHOTO_SIZE = 180.dp
private const val NAME_WEIGHT = 1.6f
private const val NAME_MAX = 10
private const val REPEAT_START_MS = 400L
private const val REPEAT_INTERVAL_MS = 70L
