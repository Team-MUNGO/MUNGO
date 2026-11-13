package com.example.poco.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.MungoPaw
import com.example.poco.core.designsystem.component.pressable
import com.example.poco.presentation.design.Mg
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.POCOTheme

/** 왼쪽 메뉴 레일 탭 */
enum class RailTab { HOME, RECORDS, MY_PET, SETTINGS }

/**
 * 이동 기록 · 설정 탭 이동. 화면마다 콜백을 넘기지 않도록 NavHost에서 한 번 제공한다.
 * 제공하지 않으면(프리뷰 등) 두 탭은 누를 수 없다.
 */
data class RailActions(val onRecords: (() -> Unit)? = null, val onSettings: (() -> Unit)? = null)

val LocalRailActions = staticCompositionLocalOf { RailActions() }

/**
 * 홈 · 마이펫 등이 같이 쓰는 왼쪽 메뉴 레일.
 * 이동 기록 · 설정은 [LocalRailActions]로 이동한다. 마이크는 아직 누를 수 없다
 */
@Composable
fun MungoNavRail(
    selected: RailTab,
    onHome: () -> Unit,
    onMyPet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    Column(
        modifier = modifier
            .width(RAIL_WIDTH)
            .fillMaxHeight()
            .background(colors.bgSurface)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MungoPaw(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(28.dp))
        RailItem(
            icon = MungoIcon.HOUSE,
            label = "홈",
            selected = selected == RailTab.HOME,
            onClick = onHome.takeIf { selected != RailTab.HOME }
        )
        val railActions = LocalRailActions.current
        RailItem(
            icon = MungoIcon.CALENDAR,
            label = "이동 기록",
            selected = selected == RailTab.RECORDS,
            onClick = railActions.onRecords.takeIf { selected != RailTab.RECORDS }
        )
        RailItem(
            icon = MungoIcon.PAW,
            label = "마이펫",
            selected = selected == RailTab.MY_PET,
            onClick = onMyPet.takeIf { selected != RailTab.MY_PET }
        )
        RailItem(
            icon = MungoIcon.SETTINGS,
            label = "설정",
            selected = selected == RailTab.SETTINGS,
            onClick = railActions.onSettings.takeIf { selected != RailTab.SETTINGS }
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(colors.bgSurfaceSunken, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(MungoIcon.MIC, size = 36.dp, contentDescription = "음성으로 말하기")
        }
    }
}

@Composable
private fun RailItem(
    icon: MungoIcon,
    label: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(MungoRadius.lg)
    val content = if (selected) colors.textOnNeutralStrong else colors.textSecondary
    Column(
        modifier = Modifier
            .padding(vertical = 10.dp) // Figma 470:55153 메뉴 간격 132
            .size(width = 104.dp, height = 112.dp)
            .background(if (selected) colors.bgNeutralStrong else Color.Transparent, shape)
            .pressable(shape = shape, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MungoIconView(icon, tint = content, size = 36.dp)
        Spacer(Modifier.height(8.dp))
        Text(text = label, style = if (selected) Mg.semi(23, 30) else Mg.regular(23, 30), color = content, maxLines = 1)
    }
}

val RAIL_WIDTH = 136.dp
