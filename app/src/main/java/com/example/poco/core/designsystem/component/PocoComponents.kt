package com.example.poco.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.core.designsystem.PetPhoto
import com.example.poco.core.designsystem.PocoIcon
import com.example.poco.core.designsystem.PocoIconView
import com.example.poco.core.designsystem.hasFullPetImage
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.theme.PocoRadius
import com.example.poco.core.theme.PocoSize
import com.example.poco.core.theme.PocoSpacing
import com.example.poco.core.theme.PocoStroke

/*
 * POCO 기본 컴포넌트 · DS-04 Controls
 * 색은 모두 POCOTheme.colors 토큰 → 라이트/야간 자동 대응
 */

// region Toggle (96×56 · 노브 44)

/** 켜짐: 남색 트랙 + 라임 노브 (야간: 라임 트랙 + 남색 노브) / 꺼짐: 회색 트랙 + 흰 노브. 애니메이션 없음 */
@Composable
fun PocoToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = POCOTheme.colors
    val inset = (PocoSize.toggleHeight - PocoSize.toggleKnob) / 2
    Box(
        modifier = modifier
            .size(PocoSize.toggleWidth, PocoSize.toggleHeight)
            .background(if (checked) colors.accentFill else colors.borderInput, CircleShape)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x = if (checked) PocoSize.toggleWidth - PocoSize.toggleKnob - inset else inset
                )
                .size(PocoSize.toggleKnob)
                .background(if (checked) colors.toggleThumb else Color.White, CircleShape)
        )
    }
}

// endregion

// region Button (높이 76 / 96 · 라운드 22 / 24 · 글자 title/m, 큰 버튼 32)

enum class PocoButtonStyle {
    /** 남색 채움 (야간: 라임) */
    PRIMARY,

    /** 흰 바탕 + border/strong 테두리 */
    SECONDARY
}

enum class PocoButtonSize {
    /** 높이 76 · 라운드 22 */
    NORMAL,

    /** 높이 96 · 라운드 24 · 글자 32 (저장하고 홈으로 등) */
    LARGE
}

@Composable
fun PocoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PocoButtonStyle = PocoButtonStyle.PRIMARY,
    size: PocoButtonSize = PocoButtonSize.NORMAL,
    enabled: Boolean = true,
    icon: PocoIcon? = null
) {
    val colors = POCOTheme.colors
    val isLarge = size == PocoButtonSize.LARGE
    val shape = RoundedCornerShape(if (isLarge) PocoRadius.r24 else PocoRadius.r22)
    val (container, content, border) = when {
        !enabled -> Triple(colors.borderInput, colors.textDisabled, colors.borderInput)
        style == PocoButtonStyle.PRIMARY ->
            Triple(colors.accentFill, colors.onAccentFill, colors.accentFill)
        else -> Triple(colors.backgroundCard, colors.textPrimary, colors.borderStrong)
    }
    val textStyle = POCOTheme.typography.titleM.let {
        if (isLarge) it.copy(fontSize = 32.sp, lineHeight = 44.sp) else it
    }
    Row(
        modifier = modifier
            .height(if (isLarge) PocoSize.buttonHeightLarge else PocoSize.buttonHeight)
            .background(container, shape)
            .border(PocoStroke.control, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = PocoSpacing.space32),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            PocoIconView(it, tint = content, modifier = Modifier.size(PocoSize.icon28))
            Spacer(Modifier.width(PocoSpacing.space8))
        }
        Text(text = text, style = textStyle, color = content, textAlign = TextAlign.Center)
    }
}

// endregion

// region Chip · Tag · Badge (칩 높이 32~40 · 태그 18 Bold · 상태 원 40~44)

/** 테두리 칩. 예) 🛡 처음 한 번만 확인해요 · 뒷좌석 오른쪽 */
@Composable
fun PocoChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: PocoIcon? = null,
    iconTint: Color = POCOTheme.colors.safe,
    background: Color = POCOTheme.colors.backgroundCard
) {
    val colors = POCOTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = PocoSize.chipHeight)
            .background(background, CircleShape)
            .border(PocoStroke.control, colors.borderDefault, CircleShape)
            .padding(horizontal = PocoSpacing.space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            PocoIconView(it, tint = iconTint, modifier = Modifier.size(PocoSize.icon24))
            Spacer(Modifier.width(PocoSpacing.space8))
        }
        Text(text = text, style = POCOTheme.typography.caption, color = colors.textSecondary)
    }
}

/** lime-soft 칩. 예) 멀미 민감도 보통 · ▲ 지난번보다 18점 올랐어요 */
@Composable
fun PocoSoftChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = POCOTheme.typography.caption,
        color = POCOTheme.colors.textPrimary,
        modifier = modifier
            .background(POCOTheme.colors.limeSoft, CircleShape)
            .padding(horizontal = PocoSpacing.space16, vertical = PocoSpacing.space4)
    )
}

/** 남색 상태 칩 + 초록 점. 예) ● 주행 중 */
@Composable
fun PocoStatusChip(text: String, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = PocoSize.chipHeight)
            .background(colors.logoBackground, CircleShape)
            .padding(horizontal = PocoSpacing.space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(12.dp)
                .background(colors.successAccent, CircleShape)
        )
        Spacer(Modifier.width(PocoSpacing.space8))
        Text(text = text, style = POCOTheme.typography.micro, color = Color.White)
    }
}

enum class PocoTagStyle {
    /** 필수 권한 · 중형 (warn) */
    WARN,

    /** 선택 권한 · 소형 (success) */
    SUCCESS,

    /** 대형 (네이비) */
    NAVY,

    /** NEW 뱃지 */
    NEW
}

/** 태그 18 Bold (NEW는 16) */
@Composable
fun PocoTag(text: String, style: PocoTagStyle, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    val (background, content) = when (style) {
        PocoTagStyle.WARN -> colors.warnBg to colors.warnText
        PocoTagStyle.SUCCESS -> colors.limeSoft to colors.successText
        PocoTagStyle.NAVY -> colors.logoBackground to Color.White
        PocoTagStyle.NEW -> colors.limeSoft to colors.textPrimary
    }
    Text(
        text = text,
        style = if (style ==
            PocoTagStyle.NEW
        ) {
            POCOTheme.typography.nano
        } else {
            POCOTheme.typography.micro
        },
        color = content,
        modifier = modifier
            .background(background, CircleShape)
            .padding(horizontal = PocoSpacing.space12, vertical = PocoSpacing.space4)
    )
}

enum class StatusTone {
    /** 라임 원 + 체크 */
    OK,

    /** 탠저린 원 + ! (더움 · 경고) */
    HOT,

    /** 파랑 원 + ! (추움) */
    COLD,

    /** 회색 점 (확인 중) */
    UNKNOWN
}

/** 상태 원 40~44 */
@Composable
fun StatusCircle(
    tone: StatusTone,
    modifier: Modifier = Modifier,
    size: Dp = PocoSize.statusCircle
) {
    val colors = POCOTheme.colors
    val background = when (tone) {
        StatusTone.OK -> colors.logoColor
        StatusTone.HOT -> colors.tangerine
        StatusTone.COLD -> colors.coldAccent
        StatusTone.UNKNOWN -> colors.fieldBackground
    }
    Box(
        modifier = modifier
            .size(size)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (tone) {
            StatusTone.OK ->
                PocoIconView(
                    PocoIcon.CHECK,
                    tint = colors.logoBackground,
                    modifier = Modifier.size(
                        size * 0.5f
                    )
                )
            StatusTone.HOT, StatusTone.COLD ->
                Text(text = "!", style = POCOTheme.typography.titleM, color = Color.White)
            StatusTone.UNKNOWN ->
                Text(text = "·", style = POCOTheme.typography.titleM, color = colors.textSecondary)
        }
    }
}

// endregion

// region PermissionCard

/** 라임 아이콘 타일 + 제목/설명 + 토글. [tag]에 필수/선택 태그 */
@Composable
fun PermissionCard(
    icon: PocoIcon,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tag: (@Composable () -> Unit)? = null
) {
    val colors = POCOTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.backgroundCard, RoundedCornerShape(PocoRadius.r24))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = PocoSpacing.space24, vertical = PocoSpacing.space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon = icon, background = colors.logoColor)
        Spacer(Modifier.width(PocoSpacing.space20))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = POCOTheme.typography.titleM, color = colors.textPrimary)
                tag?.let {
                    Spacer(Modifier.width(PocoSpacing.space8))
                    it()
                }
            }
            Text(
                text = description,
                style = POCOTheme.typography.bodyM,
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.width(PocoSpacing.space16))
        PocoToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 아이콘 타일 56 · 라운드 16 (64는 라운드 18) */
@Composable
fun IconBox(
    icon: PocoIcon,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = PocoSize.iconTile,
    tint: Color = POCOTheme.colors.logoBackground
) {
    val radius = if (size >= PocoSize.iconTileLarge) PocoRadius.r18 else PocoRadius.r16
    Box(
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(radius)),
        contentAlignment = Alignment.Center
    ) {
        PocoIconView(icon, tint = tint, modifier = Modifier.size(size / 2))
    }
}

// endregion

// region Segment (높이 80 · 라운드 18 · 선택 = 네이비)

@Composable
fun PocoSegment(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(PocoRadius.r18)
    val content = if (selected) colors.onAccentFill else colors.textPrimary
    Column(
        modifier = modifier
            .height(PocoSize.segmentHeight)
            .background(if (selected) colors.accentFill else colors.fieldBackground, shape)
            .border(
                PocoStroke.control,
                if (selected) colors.accentFill else colors.borderDefault,
                shape
            )
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, style = POCOTheme.typography.bodyL, color = content)
        subtitle?.let {
            Text(
                text = it,
                style = POCOTheme.typography.nano.copy(fontWeight = FontWeight.Normal),
                color = content.copy(alpha = 0.75f)
            )
        }
    }
}

// endregion

// region Input (높이 76 · 라운드 16 · 포커스 테두리 3 + 링 6)

private val KoreanLocales = LocaleList(Locale("ko-KR"))

/**
 * 라벨 + 입력칸 + 도움말.
 * - 비어 있으면 "예: …" [placeholder]를 회색(text/secondary) Regular로, 입력하면 네이비 Medium
 * - [unit]: 단위(살, kg). [inlineUnit]이면 값 바로 뒤(4.2kg · 예시도 "56kg"처럼 단위 포함), 아니면 오른쪽 끝
 * - [trailing]: 오른쪽 끝 내용 (몸무게 크기 뱃지 등)
 * - 포커스되면 흰 바탕 + 남색 테두리 3 + lime-soft 링 6
 * - 글자 칸은 키보드가 한국어로 먼저 열리도록 요청(hintLocales). 키보드에 한국어가 설치돼 있어야 한다
 */
@Composable
fun PocoInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    helper: String? = null,
    unit: String? = null,
    inlineUnit: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    val colors = POCOTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(PocoRadius.r16)
    val ringShape = RoundedCornerShape(PocoRadius.r16 + PocoStroke.focusRing)
    val valueStyle = POCOTheme.typography.bodyL
    val placeholderStyle = valueStyle.copy(fontWeight = FontWeight.Normal)
    Column(modifier = modifier) {
        Text(text = label, style = POCOTheme.typography.bodyM, color = colors.textSecondary)
        Spacer(Modifier.height(PocoSpacing.space8))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            interactionSource = interaction,
            textStyle = valueStyle.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accentFill),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Next,
                hintLocales = if (keyboardType == KeyboardType.Text) KoreanLocales else null
            ),
            decorationBox = { innerTextField ->
                // 포커스 링(6)은 칸 바깥에 그려 높이(76)가 변하지 않게 한다
                Box(
                    modifier = Modifier
                        .then(
                            if (focused) {
                                Modifier.border(PocoStroke.focusRing, colors.limeSoft, ringShape)
                            } else {
                                Modifier
                            }
                        )
                        .padding(PocoStroke.focusRing)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PocoSize.inputHeight)
                            .background(
                                if (focused) colors.backgroundCard else colors.fieldBackground,
                                shape
                            )
                            .then(
                                if (focused) {
                                    Modifier.border(
                                        PocoStroke.focus,
                                        colors.accentFill,
                                        shape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = PocoSpacing.space20),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = placeholderStyle,
                                    color = colors.textSecondary
                                )
                            }
                            // 단위(살 · kg)는 숫자 바로 뒤에 붙이고, 칸이 좁아도 줄바꿈하지 않는다
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .width(IntrinsicSize.Min)
                                ) {
                                    innerTextField()
                                }
                                if (inlineUnit && unit != null && value.isNotEmpty()) {
                                    Text(
                                        text = unit,
                                        style = valueStyle,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        if (unit != null && !inlineUnit) {
                            Spacer(Modifier.width(PocoSpacing.space12))
                            Text(
                                text = unit,
                                style = placeholderStyle,
                                color = colors.textSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        trailing?.let {
                            Spacer(Modifier.width(PocoSpacing.space12))
                            it()
                        }
                    }
                }
            }
        )
        helper?.let {
            Text(
                text = it,
                style = POCOTheme.typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = PocoStroke.focusRing)
            )
        }
    }
}

// endregion

// region PetListItem (동승자 목록 84)

/** 반려견 목록 한 줄: 얼굴 · 이름 · "몰티즈 · 소형". 선택되면 남색 테두리 */
@Composable
fun PetListItem(
    name: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    photoPath: String? = null,
    breed: String? = null
) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(PocoRadius.r20)
    val avatar = 56.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(if (selected) colors.backgroundCard else colors.fieldBackground, shape)
            .border(
                width = PocoStroke.control,
                color = if (selected) colors.accentFill else colors.borderDefault,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = PocoSpacing.space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(avatar)
                .background(colors.fieldBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            PetPhoto(
                photoPath = photoPath,
                breed = breed,
                modifier = Modifier.size(
                    if (hasFullPetImage(photoPath, breed)) {
                        avatar
                    } else {
                        40.dp
                    }
                )
            )
        }
        Spacer(Modifier.width(PocoSpacing.space16))
        Text(text = name, style = POCOTheme.typography.titleM, color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        Text(text = summary, style = POCOTheme.typography.caption, color = colors.textSecondary)
    }
}

// endregion

// region CheckRow (체크 줄)

enum class CheckState {
    OK,
    WARNING,
    UNKNOWN
}

/** 출발 체크 한 줄: 아이콘 타일 · 제목/설명 · 상태 원 */
@Composable
fun CheckRow(
    icon: PocoIcon,
    title: String,
    description: String,
    state: CheckState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = POCOTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = PocoSpacing.space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon = icon, background = colors.fieldBackground, tint = colors.textPrimary)
        Spacer(Modifier.width(PocoSpacing.space16))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = POCOTheme.typography.bodyL, color = colors.textPrimary)
            Text(
                text = description,
                style = POCOTheme.typography.caption,
                color = colors.textSecondary
            )
        }
        StatusCircle(
            tone = when (state) {
                CheckState.OK -> StatusTone.OK
                CheckState.WARNING -> StatusTone.HOT
                CheckState.UNKNOWN -> StatusTone.UNKNOWN
            }
        )
    }
}

// endregion
