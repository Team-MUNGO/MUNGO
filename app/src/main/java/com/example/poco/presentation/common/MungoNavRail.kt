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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.MungoPaw
import com.example.poco.core.designsystem.component.pressable
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.POCOTheme

/** 왼쪽 메뉴 레일 탭 */
enum class RailTab { HOME, RECORDS, MY_PET, SETTINGS }

/**
 * 홈 · 마이펫 등이 같이 쓰는 왼쪽 메뉴 레일.
 * 이동 기록 · 설정 · 마이크는 아직 화면이 없어 누를 수 없다
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
        Spacer(Modifier.height(32.dp))
        RailItem(
            icon = MungoIcon.HOUSE,
            label = "홈",
            selected = selected == RailTab.HOME,
            onClick = onHome.takeIf { selected != RailTab.HOME }
        )
        RailItem(icon = MungoIcon.CALENDAR, label = "이동 기록", selected = selected == RailTab.RECORDS)
        RailItem(
            icon = MungoIcon.PAW,
            label = "마이펫",
            selected = selected == RailTab.MY_PET,
            onClick = onMyPet.takeIf { selected != RailTab.MY_PET }
        )
        RailItem(icon = MungoIcon.SETTINGS, label = "설정", selected = selected == RailTab.SETTINGS)
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
            .padding(vertical = 6.dp)
            .size(width = 104.dp, height = 112.dp)
            .background(if (selected) colors.bgNeutralStrong else Color.Transparent, shape)
            .pressable(shape = shape, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MungoIconView(icon, tint = content, size = 36.dp)
        Spacer(Modifier.height(8.dp))
        Text(text = label, style = POCOTheme.typography.body3, color = content, maxLines = 1)
    }
}

val RAIL_WIDTH = 136.dp
