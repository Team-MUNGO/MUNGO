package com.example.poco.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poco.core.designsystem.PocoIcon
import com.example.poco.core.designsystem.PocoIconView
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.theme.PocoRadius
import com.example.poco.core.theme.PocoSize
import com.example.poco.core.theme.PocoSpacing
import com.example.poco.core.theme.PocoStroke

/*
 * POCO 피드백 컴포넌트 · DS-05 Content & Feedback
 * 상태 줄 · 알림 카드 · 음성 카드 · 통계 타일 · 권한 배너 · 토스트
 */

// region Info row (상태 줄: 높이 92 · 라운드 24 · 아이콘 타일 56 · OK / 더움 / 추움)

/**
 * 예) 뒷좌석 온도 / 23°C · 쾌적해요 ✓
 * [tone]에 따라 테두리 · 배경 · 상태 글자색이 바뀐다 (OK 흰 카드 / HOT warn / COLD cold)
 */
@Composable
fun InfoRow(
    icon: PocoIcon,
    label: String,
    value: String,
    status: String,
    tone: StatusTone,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    val (background, border, statusColor, tileBackground, tileTint) = when (tone) {
        StatusTone.HOT -> InfoColors(
            colors.warnBg,
            colors.warnBorder,
            colors.warnText,
            colors.warnBorder.copy(alpha = 0.35f),
            colors.warnText
        )
        StatusTone.COLD -> InfoColors(
            colors.coldBg,
            colors.coldAccent.copy(alpha = 0.5f),
            colors.coldText,
            colors.coldAccent.copy(alpha = 0.18f),
            colors.coldText
        )
        else -> InfoColors(
            colors.backgroundCard,
            colors.borderDefault,
            colors.successText,
            colors.fieldBackground,
            colors.textPrimary
        )
    }
    val shape = RoundedCornerShape(PocoRadius.r24)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PocoSize.infoRowHeight)
            .background(background, shape)
            .border(PocoStroke.card, border, shape)
            .padding(horizontal = PocoSpacing.space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(icon = icon, background = tileBackground, tint = tileTint)
        Spacer(Modifier.width(PocoSpacing.space16))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = POCOTheme.typography.caption, color = colors.textSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$value · ",
                    style = POCOTheme.typography.titleM,
                    color = colors.textPrimary
                )
                Text(text = status, style = POCOTheme.typography.titleM, color = statusColor)
            }
        }
        StatusCircle(tone = tone)
    }
}

private data class InfoColors(
    val background: Color,
    val border: Color,
    val status: Color,
    val tileBackground: Color,
    val tileTint: Color
)

// endregion

// region Alert card (주행 중 알림: 폭 = 오른쪽 열 · 높이 232 · 버튼 6:4 · 오른쪽 열 맨 위 고정)

/**
 * 예) [뒷좌석 온도 알림] 뒷좌석이 27도예요 / 현댕이에게 조금 더워요 · 알맞은 온도 20~24°C
 *     [22°C로 낮추기] [괜찮아요]
 */
@Composable
fun AlertCard(
    icon: PocoIcon,
    eyebrow: String,
    title: String,
    description: String,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.HOT
) {
    val colors = POCOTheme.colors
    val (background, border, accent, eyebrowColor) = when (tone) {
        StatusTone.COLD -> listOf(
            colors.coldBg,
            colors.coldAccent.copy(alpha = 0.5f),
            colors.coldAccent,
            colors.coldText
        )
        else -> listOf(colors.warnBg, colors.warnBorder, colors.tangerine, colors.warnText)
    }
    val shape = RoundedCornerShape(PocoRadius.r24)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PocoSize.alertCardHeight)
            .background(background, shape)
            .border(PocoStroke.control, border, shape)
            .padding(PocoSpacing.space24),
        verticalArrangement = Arrangement.spacedBy(PocoSpacing.space16)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(PocoSize.iconTile)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PocoIconView(icon, tint = Color.White, modifier = Modifier.size(PocoSize.icon28))
            }
            Spacer(Modifier.width(PocoSpacing.space16))
            Column {
                Text(text = eyebrow, style = POCOTheme.typography.micro, color = eyebrowColor)
                Text(
                    text = title,
                    style = POCOTheme.typography.headingM,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = POCOTheme.typography.caption,
                    color = colors.textSecondary
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space16)) {
            PocoButton(text = primaryText, onClick = onPrimary, modifier = Modifier.weight(6f))
            PocoButton(
                text = secondaryText,
                onClick = onSecondary,
                style = PocoButtonStyle.SECONDARY,
                modifier = Modifier.weight(4f)
            )
        }
    }
}

// endregion

// region Voice card (높이 116 · 켜짐 = 라임 스피커 / 꺼짐 = 음성 켜기 버튼)

/** 켜짐: POCO 음성 안내 + 말한 문장 그대로 / 꺼짐: 음성 안내 꺼짐 + [음성 켜기] */
@Composable
fun VoiceCard(
    message: String?,
    isVoiceOn: Boolean,
    onTurnOn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(PocoRadius.r24)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PocoSize.voiceCardHeight)
            .background(colors.backgroundCard, shape)
            .border(PocoStroke.card, colors.borderDefault, shape)
            .padding(horizontal = PocoSpacing.space24, vertical = PocoSpacing.space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isVoiceOn) {
            Box(
                modifier = Modifier
                    .size(PocoSize.iconTile)
                    .background(colors.logoColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PocoIconView(
                    PocoIcon.SPEAKER,
                    tint = colors.logoBackground,
                    modifier = Modifier.size(PocoSize.icon28)
                )
            }
            Spacer(Modifier.width(PocoSpacing.space16))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "POCO 음성 안내",
                    style = POCOTheme.typography.caption,
                    color = colors.textSecondary
                )
                Text(
                    text = message?.let { "“$it”" } ?: "필요할 때 말로 알려드릴게요",
                    style = POCOTheme.typography.titleM,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(PocoSize.iconTile)
                    .background(colors.fieldBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PocoIconView(
                    PocoIcon.SPEAKER_OFF,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(PocoSize.icon28)
                )
            }
            Spacer(Modifier.width(PocoSpacing.space16))
            Text(
                text = "음성 안내 꺼짐",
                style = POCOTheme.typography.titleM,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            PocoButton(text = "음성 켜기", onClick = onTurnOn, icon = PocoIcon.SPEAKER)
        }
    }
}

// endregion

// region Stat tile (통계 타일: 라운드 20)

/** 예) ● 급제동 / 3회 */
@Composable
fun StatTile(label: String, value: String, dotColor: Color, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    Column(
        modifier = modifier
            .background(colors.fieldBackground, RoundedCornerShape(PocoRadius.r20))
            .padding(horizontal = PocoSpacing.space16, vertical = PocoSpacing.space12)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(Modifier.width(PocoSpacing.space8))
            Text(text = label, style = POCOTheme.typography.caption, color = colors.textSecondary)
        }
        Spacer(Modifier.height(PocoSpacing.space4))
        Text(text = value, style = POCOTheme.typography.titleM, color = colors.textPrimary)
    }
}

// endregion

// region Permission banner (960 × 200~232 · 위쪽 가운데 오버레이)

/** 예) [필수 권한] 창문 정보는 꼭 필요해요 / 이 항목을 켜야 POCO를 시작할 수 있어요. [확인] */
@Composable
fun PermissionBanner(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "필수 권한"
) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(PocoRadius.r28)
    Row(
        modifier = modifier
            .widthIn(max = 960.dp)
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .background(colors.backgroundCard, shape)
            .border(PocoStroke.card, colors.borderDefault, shape)
            .padding(horizontal = PocoSpacing.space32, vertical = PocoSpacing.space24),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(PocoSize.iconTileLarge)
                .background(colors.tangerine, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            PocoIconView(
                PocoIcon.WARNING,
                tint = colors.logoBackground,
                modifier = Modifier.size(PocoSize.icon32)
            )
        }
        Spacer(Modifier.width(PocoSpacing.space24))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PocoSpacing.space8)
        ) {
            PocoTag(text = tag, style = PocoTagStyle.WARN)
            Text(text = title, style = POCOTheme.typography.headingM, color = colors.textPrimary)
            Text(
                text = description,
                style = POCOTheme.typography.bodyM,
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.width(PocoSpacing.space24))
        PocoButton(text = "확인", onClick = onConfirm)
    }
}

// endregion

// region Toast (네이비 pill)

/** 예) ✓ 보리를 동승자로 등록했어요 */
@Composable
fun PocoToast(text: String, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = PocoSize.buttonHeight)
            .background(colors.logoBackground, CircleShape)
            .padding(start = PocoSpacing.space16, end = PocoSpacing.space32),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusCircle(tone = StatusTone.OK)
        Spacer(Modifier.width(PocoSpacing.space16))
        Text(text = text, style = POCOTheme.typography.titleM, color = Color.White)
    }
}

// endregion
