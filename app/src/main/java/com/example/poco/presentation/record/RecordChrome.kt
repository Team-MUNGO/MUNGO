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
import androidx.compose.ui.unit.dp
import com.example.poco.R
import com.example.poco.presentation.design.DesignCanvas
import com.example.poco.presentation.design.DrawableIcon
import com.example.poco.presentation.design.IconBadge
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.design.MgCard
import com.example.poco.presentation.design.MgIcon
import com.example.poco.presentation.design.MgIcons
import com.example.poco.presentation.design.ValueText

/** Record screens share the 1700 x 1184 Figma frame (465:48292…). */
const val RecordRefWidth = 1700f
const val RecordRefHeight = 1184f

enum class RailTab(val label: String, val icon: ImageVector) {
    HOME("홈", MgIcons.Home),
    HISTORY("이동 기록", MgIcons.Calendar),
    PET("마이펫", MgIcons.Paw),
    SETTINGS("설정", MgIcons.Gear)
}

/** Left navigation rail (Figma 465:48292 "rail"): logo, four tabs, Gleo voice button. */
@Composable
fun RailScaffold(selected: RailTab, onNavigate: (RailTab) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    DesignCanvas(RecordRefWidth, RecordRefHeight) {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.width(134.dp).fillMaxHeight().background(Color.White).padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DrawableIcon(R.drawable.driving_logo, size = 59.dp)
                Spacer(Modifier.height(50.dp))
                RailTab.entries.forEach { tab ->
                    val on = tab == selected
                    Column(
                        Modifier.padding(vertical = 10.dp).width(102.dp).clip(RoundedCornerShape(20.dp))
                            .background(if (on) Mg.Ink else Color.Transparent)
                            .clickable { onNavigate(tab) }.padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MgIcon(tab.icon, if (on) Color.White else Mg.Muted, 27.dp)
                        Text(tab.label, style = Mg.medium(20, 27), color = if (on) Color.White else Mg.Muted)
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(91.dp).clip(CircleShape).background(Mg.Chip).clickable { onNavigate(RailTab.HOME) },
                    contentAlignment = Alignment.Center
                ) { MgIcon(MgIcons.Mic, Mg.Ink, 30.dp) }
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 43.dp, end = 43.dp, top = 48.dp, bottom = 40.dp), content = content)
        }
    }
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
    DesignCanvas(RecordRefWidth, RecordRefHeight) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(118.dp).background(Color.White).padding(horizontal = 43.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(70.dp).clip(CircleShape).background(Mg.Chip).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { MgIcon(MgIcons.ChevronLeft, Mg.Ink, 28.dp) }
                Spacer(Modifier.width(26.dp))
                Text(title, style = Mg.bold(32, 42), color = Mg.Ink, maxLines = 1)
                Spacer(Modifier.width(30.dp))
                titleExtra?.invoke(this)
                Spacer(Modifier.weight(1f))
                action?.invoke()
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(Mg.Line))
            Column(Modifier.weight(1f).fillMaxWidth().padding(start = 43.dp, end = 43.dp, top = 30.dp, bottom = 44.dp), content = content)
        }
    }
}

@Composable
fun HeaderChip(text: String) {
    Text(
        text,
        Modifier.background(Mg.Chip, CircleShape).padding(horizontal = 21.dp, vertical = 9.dp),
        style = Mg.semi(20, 27), color = Mg.Ink, maxLines = 1
    )
}

@Composable
fun HeaderAction(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.height(64.dp).clip(CircleShape).background(Color.White).border(2.dp, Mg.Line, CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MgIcon(icon, Mg.Ink, 22.dp)
        Text(text, style = Mg.semi(22, 30), color = Mg.Ink)
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
    large: Boolean = false
) {
    MgCard(modifier, padding = if (large) PaddingValues(horizontal = 32.dp, vertical = 30.dp) else PaddingValues(horizontal = 24.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MgIcon(icon, Mg.OrangeText, if (large) 24.dp else 20.dp)
            Text(label, style = Mg.Body, color = Mg.Muted, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        ValueText(value, unit, if (large) Mg.bold(54, 67) else Mg.bold(42, 53), if (highlight) Mg.OrangeText else Mg.Ink, Mg.Ink)
    }
}

@Composable
fun ChevronTitle(title: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), style = Mg.semi(25, 33), color = Mg.Ink)
        if (onClick != null) MgIcon(MgIcons.ChevronRight, Mg.Faint, 24.dp)
    }
}

@Composable
fun BulbCard(title: String, body: String, modifier: Modifier, color: Color = Mg.Card) {
    Row(
        modifier.background(color, RoundedCornerShape(Mg.Radius)).padding(horizontal = 40.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        IconBadge(MgIcons.Bulb, size = 64.dp, background = if (color == Mg.Card) Mg.IconBg else Color.White)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = Mg.semi(24, 32), color = Mg.Ink)
            Text(body, style = Mg.Body, color = Mg.Muted)
        }
    }
}
