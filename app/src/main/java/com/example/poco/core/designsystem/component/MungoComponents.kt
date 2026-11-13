package com.example.poco.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.MungoSize
import com.example.poco.core.theme.MungoSpacing
import com.example.poco.core.theme.MungoTheme

/*
 * mungo 디자인 시스템 · Components (피그마 "반려동물 동승 앱 · Components")
 * - 차량 터치 화면이라 Hover 없음. 누르는 동안 state/pressed 오버레이, 비활성은 neutral 면 + disabled 글자
 * - 색만으로 상태를 구분하지 않도록 상태 칩 · 토글 · 선택 타일은 글자나 체크를 함께 쓴다
 */

// region 공통

/** 누르는 동안 [shape] 위에 state/pressed 오버레이를 덮는다 */
@Composable
internal fun Modifier.pressable(
    shape: Shape,
    enabled: Boolean = true,
    onClick: (() -> Unit)?
): Modifier {
    if (onClick == null) return this
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val overlay = MungoTheme.colors.statePressed
    return this
        .clip(shape)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
        .then(if (pressed) Modifier.background(overlay, shape) else Modifier)
}

/** 카드 면. 라이트 = 흰 면 + 옅은 그림자, 다크 = 그림자 대신 surface 한 단계 */
@Composable
fun MungoCard(
    modifier: Modifier = Modifier,
    raised: Boolean = false,
    shape: Shape = RoundedCornerShape(MungoRadius.lg),
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = MungoSpacing.space32,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MungoTheme.colors
    Column(
        modifier = modifier
            .then(
                if (raised && !colors.isDark) {
                    Modifier.shadow(
                        12.dp,
                        shape,
                        ambientColor = colors.shadow,
                        spotColor = colors.shadow
                    )
                } else {
                    Modifier
                }
            )
            .background(if (raised) colors.bgSurfaceRaised else colors.bgSurface, shape)
            .pressable(shape, onClick = onClick)
            .padding(contentPadding),
        content = content
    )
}

// endregion

// region Icon Button (원형 88)

enum class MungoIconButtonStyle {
    /** 회색 원 (다크: 진회색) */
    NEUTRAL,

    /** 망고 원 */
    ACCENT
}

@Composable
fun MungoIconButton(
    icon: MungoIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MungoIconButtonStyle = MungoIconButtonStyle.NEUTRAL,
    size: Dp = MungoSize.iconButton,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    val colors = MungoTheme.colors
    val (container, content) = when {
        !enabled -> colors.bgNeutral to colors.textDisabled
        style == MungoIconButtonStyle.ACCENT -> colors.bgAccent to colors.textOnAccent
        else -> colors.bgNeutral to colors.textPrimary
    }
    Box(
        modifier = modifier
            .size(size)
            .background(container, CircleShape)
            .pressable(CircleShape, enabled, onClick),
        contentAlignment = Alignment.Center
    ) {
        MungoIconView(
            icon,
            tint = content,
            size = size * 0.36f,
            contentDescription = contentDescription
        )
    }
}

// endregion

// region Status Chip (편안해요 · 쉬어갈 때예요 · 주의가 필요해요 · 알림)

enum class MungoStatus {
    /** 편안함 (mint) */
    CALM,

    /** 휴식 필요 (amber) */
    REST,

    /** 주의 (red) */
    ALERT,

    /** 정보 (blue) */
    INFO
}

@Composable
fun MungoStatusChip(text: String, status: MungoStatus, modifier: Modifier = Modifier) {
    val colors = MungoTheme.colors
    val (background, content) = when (status) {
        MungoStatus.CALM -> colors.statusCalmSubtle to colors.statusCalm
        MungoStatus.REST -> colors.statusRestSubtle to colors.statusRest
        MungoStatus.ALERT -> colors.statusAlertSubtle to colors.statusAlert
        MungoStatus.INFO -> colors.statusInfoSubtle to colors.statusInfo
    }
    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .background(background, CircleShape)
            .padding(horizontal = MungoSpacing.space20),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(content, CircleShape)
        )
        Spacer(Modifier.width(MungoSpacing.space8))
        Text(text = text, style = MungoTheme.typography.title2, color = content)
    }
}

// endregion

// region Pet Avatar (사진이 없으면 발바닥)

@Composable
fun MungoPetAvatar(
    modifier: Modifier = Modifier,
    size: Dp = MungoSize.avatarLarge,
    photo: (@Composable () -> Unit)? = null
) {
    val colors = MungoTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.bgAccentSubtle),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            photo()
        } else {
            MungoIconView(MungoIcon.PAW_FILLED, tint = colors.textAccent, size = size * 0.42f)
        }
    }
}

// endregion

// region Section Header · Step Indicator · Onboarding Header

@Composable
fun MungoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MungoTheme.typography.title1,
            color = MungoTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                style = MungoTheme.typography.title2,
                color = MungoTheme.colors.textAccent,
                modifier = Modifier
                    .pressable(RoundedCornerShape(MungoRadius.sm), onClick = onAction)
                    .padding(horizontal = MungoSpacing.space12, vertical = MungoSpacing.space8)
            )
        }
    }
}

/** 막대 + "1 / 4" 글자 */
@Composable
fun MungoStepIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    val colors = MungoTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MungoSpacing.space8)
    ) {
        repeat(total) { index ->
            Box(
                Modifier
                    .size(width = 48.dp, height = 8.dp)
                    .background(
                        if (index < current) colors.borderAccent else colors.borderDefault,
                        CircleShape
                    )
            )
        }
        Spacer(Modifier.width(MungoSpacing.space12))
        Text(
            text = "$current / $total",
            style = MungoTheme.typography.body2,
            color = colors.textSecondary
        )
    }
}

/** 온보딩 화면 상단: 뒤로가기 + 단계 + 질문 제목 + 설명 */
@Composable
fun MungoOnboardingHeader(
    step: Int,
    totalSteps: Int,
    title: String,
    description: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MungoIconButton(
                icon = MungoIcon.ARROW_LEFT,
                onClick = onBack,
                size = 76.dp,
                contentDescription = "뒤로"
            )
            Spacer(Modifier.width(MungoSpacing.space24))
            MungoStepIndicator(current = step, total = totalSteps)
        }
        Spacer(Modifier.height(MungoSpacing.space32))
        Text(text = title, style = MungoTheme.typography.h1, color = MungoTheme.colors.textPrimary)
        Spacer(Modifier.height(MungoSpacing.space16))
        Text(
            text = description,
            style = MungoTheme.typography.body1,
            color = MungoTheme.colors.textSecondary
        )
    }
}

// endregion

// region Selection Tile (220×220) · Chip · Check Row · Voice Hint

@Composable
fun MungoSelectionTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: MungoIcon = MungoIcon.PAW_FILLED,
    size: Dp = MungoSize.selectionTile
) {
    val colors = MungoTheme.colors
    val shape = RoundedCornerShape(MungoRadius.lg)
    Box(
        modifier = modifier
            .size(size)
            .background(if (selected) colors.bgAccentSubtle else colors.bgSurface, shape)
            .border(
                width = if (selected) 3.dp else 1.5.dp,
                color = if (selected) colors.borderAccent else colors.borderDefault,
                shape = shape
            )
            .pressable(shape, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MungoIconView(
                icon,
                tint = if (selected) colors.textPrimary else colors.textSecondary,
                size = 56.dp
            )
            Spacer(Modifier.height(MungoSpacing.space16))
            Text(
                text = label,
                style = MungoTheme.typography.body1,
                color = if (selected) colors.textAccent else colors.textPrimary
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(MungoSpacing.space16)
                    .size(40.dp)
                    .background(colors.bgAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MungoIconView(MungoIcon.CHECK, tint = colors.textOnAccent, size = 24.dp)
            }
        }
    }
}

enum class MungoChipSize {
    /** 64 · 눌러서 고르는 칩 */
    MEDIUM,

    /** 40 · 보기만 하는 태그 */
    SMALL
}

@Composable
fun MungoChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: MungoChipSize = MungoChipSize.MEDIUM,
    onClick: (() -> Unit)? = null
) {
    val colors = MungoTheme.colors
    val isMedium = size == MungoChipSize.MEDIUM
    Row(
        modifier = modifier
            .height(if (isMedium) MungoSize.chipMedium else MungoSize.chipSmall)
            .background(if (selected) colors.bgAccent else colors.bgNeutral, CircleShape)
            .pressable(CircleShape, onClick = onClick)
            .padding(horizontal = if (isMedium) MungoSpacing.space24 else MungoSpacing.space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val content = if (selected) colors.textOnAccent else colors.textSecondary
        MungoIconView(MungoIcon.CHECK, tint = content, size = if (isMedium) 24.dp else 20.dp)
        Spacer(Modifier.width(MungoSpacing.space8))
        Text(
            text = text,
            style = if (isMedium) MungoTheme.typography.title2 else MungoTheme.typography.body3,
            color = content
        )
    }
}

/** 체크리스트 한 줄. 줄 전체가 터치 영역(높이 100 이상). 체크하면 제목이 한 단계 흐려진다 */
@Composable
fun MungoCheckRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    val colors = MungoTheme.colors
    val shape = RoundedCornerShape(MungoRadius.lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MungoSize.checkRow)
            .background(colors.bgSurface, shape)
            .pressable(shape) { onCheckedChange(!checked) }
            .padding(horizontal = MungoSpacing.space24, vertical = MungoSpacing.space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val boxShape = RoundedCornerShape(MungoRadius.sm)
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (checked) colors.bgAccent else Color.Transparent, boxShape)
                .border(2.dp, if (checked) colors.borderAccent else colors.borderStrong, boxShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) MungoIconView(MungoIcon.CHECK, tint = colors.textOnAccent, size = 26.dp)
        }
        Spacer(Modifier.width(MungoSpacing.space24))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MungoTheme.typography.title2,
                color = if (checked) colors.textSecondary else colors.textPrimary
            )
            description?.let {
                Text(text = it, style = MungoTheme.typography.body2, color = colors.textTertiary)
            }
        }
    }
}

/** "말로 답해도 돼요" 안내 바. 마이크 버튼 + 예시 문장 */
@Composable
fun MungoVoiceHint(
    example: String,
    modifier: Modifier = Modifier,
    title: String = "말로 답해도 돼요",
    onMicClick: () -> Unit = {}
) {
    val colors = MungoTheme.colors
    Row(
        modifier = modifier
            .background(colors.bgAccentSubtle, CircleShape)
            .padding(MungoSpacing.space12)
            .padding(end = MungoSpacing.space32),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MungoIconButton(
            icon = MungoIcon.MIC,
            onClick = onMicClick,
            style = MungoIconButtonStyle.ACCENT,
            size = 76.dp,
            contentDescription = "음성으로 답하기"
        )
        Spacer(Modifier.width(MungoSpacing.space20))
        Column {
            Text(text = title, style = MungoTheme.typography.title2, color = colors.textAccent)
            Text(
                text = "\"$example\"",
                style = MungoTheme.typography.body1,
                color = colors.textSecondary
            )
        }
    }
}

// endregion

// region Info Tile · List Item

/** 정차 중 홈의 요약 타일. 숫자는 크게, 설명은 짧게 */
@Composable
fun MungoInfoTile(icon: MungoIcon, label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MungoTheme.colors
    MungoCard(modifier = modifier, contentPadding = MungoSpacing.space28) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MungoIconView(icon, tint = colors.textSecondary, size = 24.dp)
            Spacer(Modifier.width(MungoSpacing.space8))
            Text(text = label, style = MungoTheme.typography.body1, color = colors.textSecondary)
        }
        Spacer(Modifier.height(MungoSpacing.space16))
        Text(text = value, style = MungoTheme.typography.h2, color = colors.textPrimary)
    }
}

/** 장소 · 기록 목록 한 줄 (높이 112) */
@Composable
fun MungoListItem(
    icon: MungoIcon,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = MungoTheme.colors
    val shape = RoundedCornerShape(MungoRadius.md)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MungoSize.listItem)
            .background(colors.bgSurface, shape)
            .pressable(shape, onClick = onClick)
            .padding(horizontal = MungoSpacing.space24),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(MungoSize.avatarMedium)
                .background(colors.bgAccentSubtle, RoundedCornerShape(MungoRadius.md)),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(icon, tint = colors.textAccent, size = 28.dp)
        }
        Spacer(Modifier.width(MungoSpacing.space20))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MungoTheme.typography.title2,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MungoTheme.typography.body2,
                color = colors.textSecondary,
                maxLines = 1
            )
        }
        trailingText?.let {
            Spacer(Modifier.width(MungoSpacing.space16))
            Text(text = it, style = MungoTheme.typography.title2, color = colors.textSecondary)
        }
        if (onClick != null) {
            Spacer(Modifier.width(MungoSpacing.space12))
            MungoIconView(MungoIcon.CHEVRON_RIGHT, tint = colors.textSecondary, size = 28.dp)
        }
    }
}

// endregion

// region Drive Status Card · Suggestion Card (주행 중 분할 칸, 폭 640 가정)

@Composable
fun MungoDriveStatusCard(
    petName: String,
    status: MungoStatus,
    statusText: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    footerIcon: MungoIcon? = null,
    footerText: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = MungoTheme.colors
    MungoCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MungoPetAvatar(size = 56.dp)
            Spacer(Modifier.width(MungoSpacing.space16))
            Text(
                text = petName,
                style = MungoTheme.typography.h3,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            MungoStatusChip(text = statusText, status = status)
        }
        Spacer(Modifier.height(MungoSpacing.space32))
        Text(text = label, style = MungoTheme.typography.body1, color = colors.textSecondary)
        Text(
            text = value,
            style = MungoTheme.typography.display2.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        if (footerText != null) {
            Spacer(Modifier.height(MungoSpacing.space16))
            Row(verticalAlignment = Alignment.CenterVertically) {
                footerIcon?.let {
                    MungoIconView(it, tint = colors.textSecondary, size = 28.dp)
                    Spacer(Modifier.width(MungoSpacing.space8))
                }
                Text(
                    text = footerText,
                    style = MungoTheme.typography.body1,
                    color = colors.textSecondary
                )
            }
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(MungoSpacing.space24))
            MungoButton(
                text = actionText,
                onClick = onAction,
                size = MungoButtonSize.LARGE,
                mungoIcon = MungoIcon.REST_AREA,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** 휴게소 경유 제안. "이유 한 줄 + 카운트다운 버튼" (Pleos 내비 카드 패턴) */
@Composable
fun MungoSuggestionCard(
    title: String,
    reason: String,
    placeName: String,
    placeDetail: String,
    countdownSeconds: Int,
    onAccept: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MungoTheme.colors
    MungoCard(modifier = modifier, raised = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(colors.bgNeutral, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MungoIconView(MungoIcon.REST_AREA, tint = colors.textPrimary, size = 32.dp)
            }
            Spacer(Modifier.width(MungoSpacing.space20))
            Column {
                Text(text = title, style = MungoTheme.typography.h3, color = colors.textPrimary)
                Text(
                    text = reason,
                    style = MungoTheme.typography.body1,
                    color = colors.textSecondary
                )
            }
        }
        Spacer(Modifier.height(MungoSpacing.space24))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgSurfaceSunken, RoundedCornerShape(MungoRadius.md))
                .padding(MungoSpacing.space24),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MungoIconView(MungoIcon.MAP_PIN, tint = colors.textAccent, size = 28.dp)
            Spacer(Modifier.width(MungoSpacing.space16))
            Column {
                Text(
                    text = placeName,
                    style = MungoTheme.typography.title1,
                    color = colors.textPrimary
                )
                Text(
                    text = placeDetail,
                    style = MungoTheme.typography.body2,
                    color = colors.textSecondary
                )
            }
        }
        Spacer(Modifier.height(MungoSpacing.space24))
        Row(horizontalArrangement = Arrangement.spacedBy(MungoSpacing.space16)) {
            MungoButton(
                text = "경유하기 ($countdownSeconds)",
                onClick = onAccept,
                size = MungoButtonSize.LARGE,
                mungoIcon = MungoIcon.REST_AREA,
                modifier = Modifier.weight(1.4f)
            )
            MungoButton(
                text = "나중에",
                onClick = onLater,
                style = MungoButtonStyle.SECONDARY,
                size = MungoButtonSize.LARGE,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// endregion
