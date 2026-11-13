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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poco.core.designsystem.MungoBaseIcon
import com.example.poco.core.designsystem.MungoBaseIconView
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.PetPhoto
import com.example.poco.core.designsystem.hasFullPetImage
import com.example.poco.core.theme.MungoBaseRadius
import com.example.poco.core.theme.MungoBaseSize
import com.example.poco.core.theme.MungoPalette
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.MungoSize
import com.example.poco.core.theme.MungoSpacing
import com.example.poco.core.theme.MungoStroke
import com.example.poco.core.theme.MungoTheme

/*
 * mungo 기본 컴포넌트 (mungo 디자인 시스템으로 교체)
 * 색은 모두 MungoTheme.colors 토큰 → 라이트/다크 자동 대응. 새 컴포넌트는 MungoComponents.kt
 */

// region Toggle (96×56 · 터치 영역은 줄 전체 88 이상)

/**
 * 켜짐: 망고 트랙 + 노브 안 체크 (테두리 없음 · Figma 온보딩 3/3) / 꺼짐: 진회색 트랙 (다크: 밝은 회색).
 * 색만으로 구분하지 않도록 켜짐에 체크를 넣는다. 애니메이션 없음
 */
@Composable
fun MungoToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MungoTheme.colors
    val knob = MungoBaseSize.toggleKnob
    val inset = (MungoBaseSize.toggleHeight - knob) / 2
    val track = when {
        !enabled && checked -> MungoPalette.mango200
        !enabled -> colors.borderStrong
        checked -> colors.bgAccent
        else -> colors.borderStrong
    }
    Box(
        modifier = modifier
            .size(MungoBaseSize.toggleWidth, MungoBaseSize.toggleHeight)
            .background(track, CircleShape)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = if (checked) MungoBaseSize.toggleWidth - knob - inset else inset)
                .size(knob)
                .background(colors.bgSurface, CircleShape)
        )
    }
}

// endregion

// region Button (L 88 = 주행 중 주요 동작 · M 64 = 정차 화면 · 라운드 16)

enum class MungoButtonStyle {
    /** 망고 채움 + 진회색 글자 (한 화면에 1개) */
    PRIMARY,

    /** 회색 면 (보조) */
    SECONDARY,

    /** 진회색 면 + 흰 글자 (Pleos 저장 버튼 톤. 다크: 밝은 회색 + 검정 글자) */
    NEUTRAL_STRONG,

    /** 흰 면 + 테두리 */
    OUTLINE
}

enum class MungoButtonSize {
    /** M · 높이 64 · 글자 title3(24) */
    NORMAL,

    /** L · 높이 88 · 글자 title2(28) */
    LARGE
}

@Composable
fun MungoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MungoButtonStyle = MungoButtonStyle.PRIMARY,
    size: MungoButtonSize = MungoButtonSize.NORMAL,
    enabled: Boolean = true,
    icon: MungoBaseIcon? = null,
    mungoIcon: MungoIcon? = null,
    shape: Shape = RoundedCornerShape(MungoRadius.md)
) {
    val colors = MungoTheme.colors
    val isLarge = size == MungoButtonSize.LARGE
    val (container, content, border) = when {
        !enabled && style == MungoButtonStyle.OUTLINE ->
            Triple(colors.bgSurface, colors.textDisabled, colors.borderDefault)
        !enabled -> Triple(colors.bgNeutral, colors.textDisabled, null)
        style == MungoButtonStyle.PRIMARY -> Triple(colors.bgAccent, colors.textOnAccent, null)
        style == MungoButtonStyle.SECONDARY -> Triple(colors.bgNeutral, colors.textPrimary, null)
        style == MungoButtonStyle.NEUTRAL_STRONG ->
            Triple(colors.bgNeutralStrong, colors.textOnNeutralStrong, null)
        else -> Triple(colors.bgSurface, colors.textPrimary, colors.borderDefault)
    }
    val textStyle = if (isLarge) MungoTheme.typography.title2 else MungoTheme.typography.title3
    val iconSize = if (isLarge) 32.dp else 28.dp
    Row(
        modifier = modifier
            .height(if (isLarge) MungoSize.buttonLarge else MungoSize.buttonMedium)
            .background(container, shape)
            .then(
                if (border !=
                    null
                ) {
                    Modifier.border(MungoStroke.control, border, shape)
                } else {
                    Modifier
                }
            )
            .pressable(shape, enabled, onClick)
            .padding(horizontal = if (isLarge) MungoSpacing.space32 else MungoSpacing.space24),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            MungoBaseIconView(it, tint = content, modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(MungoSpacing.space12))
        }
        mungoIcon?.let {
            MungoIconView(it, tint = content, size = iconSize)
            Spacer(Modifier.width(MungoSpacing.space12))
        }
        Text(text = text, style = textStyle, color = content, textAlign = TextAlign.Center)
    }
}

// endregion

// region Chip · Tag · Badge (칩 높이 32~40 · 태그 18 Bold · 상태 원 40~44)

/** 테두리 칩. 예) 🛡 처음 한 번만 확인해요 · 뒷좌석 오른쪽 */
@Composable
fun MungoChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: MungoBaseIcon? = null,
    iconTint: Color = MungoTheme.colors.safe,
    background: Color = MungoTheme.colors.backgroundCard
) {
    val colors = MungoTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = MungoBaseSize.chipHeight)
            .background(background, CircleShape)
            .border(MungoStroke.control, colors.borderDefault, CircleShape)
            .padding(horizontal = MungoSpacing.space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            MungoBaseIconView(it, tint = iconTint, modifier = Modifier.size(MungoBaseSize.icon24))
            Spacer(Modifier.width(MungoSpacing.space8))
        }
        Text(text = text, style = MungoTheme.typography.caption, color = colors.textSecondary)
    }
}

/** 망고 옅은 칩 (accent-subtle). 예) 멀미 민감도 보통 · ▲ 지난번보다 18점 올랐어요 */
@Composable
fun MungoSoftChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MungoTheme.typography.caption,
        color = MungoTheme.colors.textPrimary,
        modifier = modifier
            .background(MungoTheme.colors.limeSoft, CircleShape)
            .padding(horizontal = MungoSpacing.space16, vertical = MungoSpacing.space4)
    )
}

/** 진회색 상태 칩 + 민트 점. 예) ● 주행 중 */
@Composable
fun MungoStatusChip(text: String, modifier: Modifier = Modifier) {
    val colors = MungoTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = MungoBaseSize.chipHeight)
            .background(colors.bgNeutralStrong, CircleShape)
            .padding(horizontal = MungoSpacing.space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(12.dp)
                .background(colors.successAccent, CircleShape)
        )
        Spacer(Modifier.width(MungoSpacing.space8))
        Text(text = text, style = MungoTheme.typography.micro, color = colors.textOnNeutralStrong)
    }
}

enum class MungoTagStyle {
    /** 필수 권한 · 중형 (warn) */
    WARN,

    /** 선택 권한 · 소형 (success) */
    SUCCESS,

    /** 대형 (진회색) */
    NAVY,

    /** NEW 뱃지 */
    NEW
}

/** 태그 18 Bold (NEW는 16) */
@Composable
fun MungoTag(text: String, style: MungoTagStyle, modifier: Modifier = Modifier) {
    val colors = MungoTheme.colors
    val (background, content) = when (style) {
        MungoTagStyle.WARN -> colors.statusRestSubtle to colors.statusRest
        MungoTagStyle.SUCCESS -> colors.statusCalmSubtle to colors.statusCalm
        MungoTagStyle.NAVY -> colors.bgNeutralStrong to colors.textOnNeutralStrong
        MungoTagStyle.NEW -> colors.bgAccentSubtle to colors.textAccent
    }
    Text(
        text = text,
        style = if (style ==
            MungoTagStyle.NEW
        ) {
            MungoTheme.typography.nano
        } else {
            MungoTheme.typography.micro
        },
        color = content,
        modifier = modifier
            .background(background, CircleShape)
            .padding(horizontal = MungoSpacing.space12, vertical = MungoSpacing.space4)
    )
}

enum class StatusTone {
    /** 민트 원 + 체크 (편안함) */
    OK,

    /** 빨강 원 + ! (더움 · 경고) */
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
    size: Dp = MungoBaseSize.statusCircle
) {
    val colors = MungoTheme.colors
    val background = when (tone) {
        StatusTone.OK -> colors.statusCalmSubtle
        StatusTone.HOT -> colors.statusAlertSubtle
        StatusTone.COLD -> colors.statusInfoSubtle
        StatusTone.UNKNOWN -> colors.bgNeutral
    }
    Box(
        modifier = modifier
            .size(size)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (tone) {
            StatusTone.OK ->
                MungoIconView(MungoIcon.CHECK, tint = colors.statusCalm, size = size * 0.55f)
            StatusTone.HOT ->
                Text(text = "!", style = MungoTheme.typography.titleM, color = colors.statusAlert)
            StatusTone.COLD ->
                Text(text = "!", style = MungoTheme.typography.titleM, color = colors.statusInfo)
            StatusTone.UNKNOWN ->
                Text(text = "·", style = MungoTheme.typography.titleM, color = colors.textSecondary)
        }
    }
}

// endregion

// region PermissionCard

/** 망고 아이콘 타일 + 제목/설명 + 토글. [tag]에 필수/선택 태그 */
@Composable
fun PermissionCard(
    icon: MungoBaseIcon,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tag: (@Composable () -> Unit)? = null
) {
    val colors = MungoTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface, RoundedCornerShape(MungoRadius.lg))
            .pressable(RoundedCornerShape(MungoRadius.lg)) { onCheckedChange(!checked) }
            .padding(horizontal = MungoSpacing.space24, vertical = MungoSpacing.space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon = icon, background = colors.bgAccentSubtle, tint = colors.bgAccent)
        Spacer(Modifier.width(MungoSpacing.space20))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MungoTheme.typography.titleM, color = colors.textPrimary)
                tag?.let {
                    Spacer(Modifier.width(MungoSpacing.space8))
                    it()
                }
            }
            Text(
                text = description,
                style = MungoTheme.typography.bodyM,
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.width(MungoSpacing.space16))
        MungoToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 아이콘 타일 56 · 라운드 16 (64는 라운드 18) */
@Composable
fun IconBox(
    icon: MungoBaseIcon,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = MungoBaseSize.iconTile,
    tint: Color = MungoTheme.colors.textPrimary
) {
    val radius = if (size >= MungoBaseSize.iconTileLarge) MungoRadius.lg else MungoRadius.md
    Box(
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(radius)),
        contentAlignment = Alignment.Center
    ) {
        MungoBaseIconView(icon, tint = tint, modifier = Modifier.size(size / 2))
    }
}

// endregion

// region Segment (높이 88 · 회색 홈 안에서 선택 칸만 흰 면 + 테두리)

/**
 * 세그먼트 한 칸. 여러 칸을 Row로 나란히 둔다.
 * 선택: 흰 면(다크: surface) + border/strong + text/primary / 비선택: 회색 면 + text/secondary
 */
@Composable
fun MungoSegment(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    val colors = MungoTheme.colors
    val shape = RoundedCornerShape(MungoRadius.md)
    val content = when {
        !enabled -> colors.textDisabled
        selected -> colors.textPrimary
        else -> colors.textSecondary
    }
    Column(
        modifier = modifier
            .height(MungoSize.segment)
            .background(if (selected) colors.bgSurface else colors.bgNeutral, shape)
            .border(
                MungoStroke.control,
                if (selected) colors.borderStrong else colors.bgNeutral,
                shape
            )
            .pressable(shape, enabled, onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MungoTheme.typography.title2.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = content
        )
        subtitle?.let {
            Text(text = it, style = MungoTheme.typography.caption1, color = content)
        }
    }
}

// endregion

// region Input (높이 88 · 라운드 16 · 포커스 = 망고 테두리 3 · 오류 = 빨강 테두리 + 빨강 안내)

private val KoreanLocales = LocaleList(Locale("ko-KR"))

/**
 * 라벨 + 입력칸 + 도움말.
 * - 비어 있으면 "예: …" [placeholder]를 회색(text/secondary) Regular로, 입력하면 네이비 Medium
 * - [unit]: 단위(살, kg). [inlineUnit]이면 값 바로 뒤(4.2kg · 예시도 "56kg"처럼 단위 포함), 아니면 오른쪽 끝
 * - [trailing]: 오른쪽 끝 내용 (몸무게 크기 뱃지 등)
 * - 포커스되면 border/accent 3 · [error]가 있으면 빨강 테두리 + 도움말 대신 빨강 안내
 * - [onMicClick]이 있으면 오른쪽 끝에 마이크 (차량에서는 음성 입력을 먼저 권한다)
 * - 글자 칸은 키보드가 한국어로 먼저 열리도록 요청(hintLocales). 키보드에 한국어가 설치돼 있어야 한다
 */
@Composable
fun MungoInput(
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
    enabled: Boolean = true,
    error: String? = null,
    onMicClick: (() -> Unit)? = null
) {
    val colors = MungoTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(MungoBaseRadius.r16)
    val valueStyle = MungoTheme.typography.body1
    val placeholderStyle = valueStyle
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MungoTheme.typography.body2,
            color = if (enabled) colors.textSecondary else colors.textDisabled
        )
        Spacer(Modifier.height(MungoSpacing.space8))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            interactionSource = interaction,
            textStyle = valueStyle.copy(
                color = if (enabled) colors.textPrimary else colors.textDisabled
            ),
            cursorBrush = SolidColor(colors.bgAccent),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Next,
                hintLocales = if (keyboardType == KeyboardType.Text) KoreanLocales else null
            ),
            decorationBox = { innerTextField ->
                val borderColor = when {
                    error != null -> colors.statusAlert
                    focused -> colors.borderAccent
                    else -> colors.borderDefault
                }
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MungoSize.input)
                            .background(
                                if (enabled) colors.bgSurface else colors.bgSurfaceSunken,
                                shape
                            )
                            .border(
                                if (focused ||
                                    error != null
                                ) {
                                    MungoStroke.focus
                                } else {
                                    MungoStroke.control
                                },
                                borderColor,
                                shape
                            )
                            .padding(horizontal = MungoSpacing.space24),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = placeholderStyle,
                                    color = colors.textTertiary
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
                            Spacer(Modifier.width(MungoSpacing.space12))
                            Text(
                                text = unit,
                                style = placeholderStyle,
                                color = colors.textSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        trailing?.let {
                            Spacer(Modifier.width(MungoSpacing.space12))
                            it()
                        }
                        onMicClick?.let { onMic ->
                            // 음성 안내가 꺼져 있으면 마이크만 회색 · 누를 수 없음
                            val micEnabled = enabled &&
                                com.example.poco.core.designsystem.LocalVoiceEnabled.current
                            Spacer(Modifier.width(MungoSpacing.space12))
                            Box(
                                modifier = Modifier
                                    .size(MungoSize.touchMin)
                                    .pressable(CircleShape, micEnabled, onMic),
                                contentAlignment = Alignment.Center
                            ) {
                                MungoIconView(
                                    MungoIcon.MIC,
                                    tint = if (micEnabled) {
                                        colors.textPrimary
                                    } else {
                                        colors.textDisabled
                                    },
                                    size = 32.dp,
                                    contentDescription = "음성으로 입력"
                                )
                            }
                        }
                    }
                }
            }
        )
        (error ?: helper)?.let {
            Spacer(Modifier.height(MungoSpacing.space8))
            Text(
                text = it,
                style = MungoTheme.typography.body2,
                color = if (error != null) colors.statusAlert else colors.textSecondary
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
    val colors = MungoTheme.colors
    val shape = RoundedCornerShape(MungoBaseRadius.r20)
    val avatar = 56.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(if (selected) colors.backgroundCard else colors.fieldBackground, shape)
            .border(
                width = MungoStroke.control,
                color = if (selected) colors.accentFill else colors.borderDefault,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = MungoSpacing.space16),
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
        Spacer(Modifier.width(MungoSpacing.space16))
        Text(text = name, style = MungoTheme.typography.titleM, color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        Text(text = summary, style = MungoTheme.typography.caption, color = colors.textSecondary)
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
    icon: MungoBaseIcon,
    title: String,
    description: String,
    state: CheckState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = MungoTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = MungoSpacing.space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon = icon, background = colors.fieldBackground, tint = colors.textPrimary)
        Spacer(Modifier.width(MungoSpacing.space16))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MungoTheme.typography.bodyL, color = colors.textPrimary)
            Text(
                text = description,
                style = MungoTheme.typography.caption,
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
