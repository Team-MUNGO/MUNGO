package com.example.poco.presentation.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poco.presentation.common.MungoNavRail
import com.example.poco.presentation.common.RailTab as NavRailTab
import com.example.poco.presentation.design.DesignCanvas
import com.example.poco.presentation.design.IconBadge
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.design.MgCard
import com.example.poco.presentation.design.MgIcon
import com.example.poco.presentation.design.MgIcons
import com.example.poco.presentation.design.ValueText

/** Record screens share the 1700 x 1184 Figma frame (465:48292…). */
const val RECORD_REF_WIDTH = 1700f
const val RECORD_REF_HEIGHT = 1184f

enum class RailTab(val label: String, val icon: ImageVector) {
    HOME("홈", MgIcons.Home),
    HISTORY("이동 기록", MgIcons.Calendar),
    PET("마이펫", MgIcons.Paw),
    SETTINGS("설정", MgIcons.Gear)
}

/** Left navigation rail (Figma 465:48292 "rail"): logo, four tabs, Gleo voice button. */
@Composable
fun RailScaffold(
    selected: RailTab,
    onNavigate: (RailTab) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    DesignCanvas(RECORD_REF_WIDTH, RECORD_REF_HEIGHT) {
        Row(Modifier.fillMaxSize()) {
            RecordRail(selected, onNavigate)
            Column(
                Modifier.weight(
                    1f
                ).fillMaxHeight().padding(start = 43.dp, end = 43.dp, top = 48.dp, bottom = 40.dp),
                content = content
            )
        }
    }
}

/**
 * 이동 기록 · 설정의 왼쪽 레일. 홈 · 마이펫과 같은 [MungoNavRail]을 그대로 쓴다 (화면마다 레일 모양이 달라지지 않게).
 * 이동 기록 · 설정 탭은 NavHost가 주는 LocalRailActions로, 홈 · 마이펫은 [onNavigate]로 이동한다
 */
@Composable
fun RecordRail(selected: RailTab, onNavigate: (RailTab) -> Unit) {
    MungoNavRail(
        selected = when (selected) {
            RailTab.HOME -> NavRailTab.HOME
            RailTab.HISTORY -> NavRailTab.RECORDS
            RailTab.PET -> NavRailTab.MY_PET
            RailTab.SETTINGS -> NavRailTab.SETTINGS
        },
        onHome = { onNavigate(RailTab.HOME) },
        onMyPet = { onNavigate(RailTab.PET) }
    )
}

/** White 118 px header with a round back button (Figma 465:48424 "header"). */
@Composable
fun PageScaffold(
    title: String,
    onBack: () -> Unit,
    titleExtra: (@Composable RowScope.() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    DesignCanvas(RECORD_REF_WIDTH, RECORD_REF_HEIGHT) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(
                    118.dp
                ).background(Color.White).padding(horizontal = 43.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(
                        70.dp
                    ).clip(CircleShape).background(Mg.Chip).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { MgIcon(MgIcons.ChevronLeft, Mg.Ink, 28.dp) }
                Spacer(Modifier.width(26.dp))
                Text(title, style = Mg.bold(35, 45), color = Mg.Ink, maxLines = 1)
                Spacer(Modifier.width(30.dp))
                titleExtra?.invoke(this)
                Spacer(Modifier.weight(1f))
                action?.invoke()
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(Mg.Line))
            Column(
                Modifier.weight(
                    1f
                ).fillMaxWidth().padding(start = 43.dp, end = 43.dp, top = 30.dp, bottom = 44.dp),
                content = content
            )
        }
    }
}

@Composable
fun HeaderChip(text: String) {
    Text(
        text,
        Modifier.background(Mg.Chip, CircleShape).padding(horizontal = 21.dp, vertical = 9.dp),
        style = Mg.bold(23, 30),
        color = Mg.Ink,
        maxLines = 1
    )
}

@Composable
fun HeaderAction(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.height(
            64.dp
        ).clip(CircleShape).background(Color.White).border(2.dp, Mg.Line, CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MgIcon(icon, Mg.Ink, 22.dp)
        Text(text, style = Mg.bold(25, 33), color = Mg.Ink)
    }
}

/** Small stat tile (Figma "tile · 급정거" 178 x 128 / list "tile · 이동 횟수" 354 x 166). */
@Composable
fun StatTile(
    label: String,
    value: String,
    unit: String?,
    icon: ImageVector,
    modifier: Modifier,
    highlight: Boolean = false,
    large: Boolean = false,
    /** 화면 분할(좁은 칸)용: 여백 · 숫자를 줄여 라벨이 잘리거나 숫자가 칸 밖으로 넘치지 않게 */
    compact: Boolean = false
) {
    val padding = when {
        large -> PaddingValues(horizontal = 32.dp, vertical = 30.dp)
        compact -> PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        else -> PaddingValues(horizontal = 24.dp, vertical = 20.dp)
    }
    val valueStyle = when {
        large -> Mg.bold(56, 70)
        compact -> Mg.bold(34, 42)
        else -> Mg.bold(42, 53)
    }
    MgCard(modifier, padding = padding) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            MgIcon(
                icon,
                Mg.OrangeText,
                if (large) {
                    24.dp
                } else if (compact) {
                    18.dp
                } else {
                    20.dp
                }
            )
            Text(
                label,
                style = Mg.Body,
                color = Mg.Muted,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.weight(1f))
        // Figma 465:48292 큰 통계 칸: 숫자 B56 · 단위 R28
        ValueText(
            value,
            unit,
            valueStyle,
            if (highlight) Mg.OrangeText else Mg.Ink,
            Mg.Ink,
            if (large) Mg.regular(28, 36) else Mg.Unit
        )
    }
}

@Composable
fun ChevronTitle(title: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().then(
            if (onClick !=
                null
            ) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), style = Mg.bold(28, 36), color = Mg.Ink)
        if (onClick != null) MgIcon(MgIcons.ChevronRight, Mg.Faint, 24.dp)
    }
}

@Composable
fun BulbCard(
    title: String,
    body: String,
    modifier: Modifier,
    color: Color = Mg.Card,
    bodyStyle: TextStyle = Mg.Body,
    bodyColor: Color = Mg.Muted
) {
    Row(
        modifier.background(
            color,
            RoundedCornerShape(Mg.Radius)
        ).padding(horizontal = 40.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        IconBadge(
            MgIcons.Bulb,
            size = 64.dp,
            background = if (color ==
                Mg.Card
            ) {
                Mg.IconBg
            } else {
                Color.White
            }
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Figma 465:48545 · 465:48802: 제목 B27
            Text(title, style = Mg.bold(27, 35), color = Mg.Ink)
            Text(body, style = bodyStyle, color = bodyColor)
        }
    }
}
