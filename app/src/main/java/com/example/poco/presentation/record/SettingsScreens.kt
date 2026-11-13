package com.example.poco.presentation.record

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.designsystem.MungoLogo
import com.example.poco.data.vehicle.VehicleIdentity
import com.example.poco.data.vehicle.VehicleLinkState
import com.example.poco.presentation.design.DesignCanvas
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.design.MgIcon
import com.example.poco.presentation.design.MgIcons
import com.example.poco.presentation.session.component.intervalText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 설정 left menu (Figma 471:56978 · 470:56154 · 470:56789). "음성 및 알림" is shown as 알림 only. */
enum class SettingsPage(val label: String, val icon: ImageVector) {
    ALERTS("알림", MgIcons.Bell),
    VEHICLE("차량 연동 관리", MgIcons.Car),
    APP_INFO("앱 정보", MgIcons.Info)
}

@Composable
fun SettingsRoute(
    onNavigate: (RailTab) -> Unit,
    onOpenInterval: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.ALERTS) }
    var confirmUnlink by rememberSaveable { mutableStateOf(false) }
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val voice by viewModel.voice.collectAsStateWithLifecycle()
    val link by viewModel.linkState.collectAsStateWithLifecycle()
    val identity by viewModel.identity.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    SettingsScreen(
        page = page,
        onPage = { page = it },
        onNavigate = onNavigate,
        toast = toast,
        dialog = if (confirmUnlink) {
            {
                UnlinkDialog(onCancel = { confirmUnlink = false }, onConfirm = {
                    confirmUnlink = false
                    viewModel.unlink()
                })
            }
        } else {
            null
        }
    ) {
        when (page) {
            SettingsPage.ALERTS -> AlertsPage(
                alerts,
                onRest = viewModel::setRestSuggestion,
                onWindow = viewModel::setWindowAlert,
                onArrival = viewModel::setArrivalSummary,
                onOpenInterval = onOpenInterval
            )
            SettingsPage.VEHICLE -> VehiclePage(
                link,
                identity,
                voice,
                onUnlink = { confirmUnlink = true },
                onRelink = viewModel::relink,
                onVoice = viewModel::setVoice
            )
            SettingsPage.APP_INFO -> AppInfoPage(viewModel.version, linked = link.linked)
        }
    }
}

@Composable
internal fun SettingsScreen(
    page: SettingsPage,
    onPage: (SettingsPage) -> Unit,
    onNavigate: (RailTab) -> Unit,
    toast: String? = null,
    dialog: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    DesignCanvas(RECORD_REF_WIDTH, RECORD_REF_HEIGHT) {
        Row(Modifier.fillMaxSize()) {
            RecordRail(RailTab.SETTINGS, onNavigate)
            // 설정 메뉴: 134 → 551 (Figma "설정 메뉴")
            Column(
                Modifier.width(417.dp).fillMaxHeight().background(Color.White)
                    .border(1.dp, Mg.Line).padding(start = 20.dp, end = 22.dp, top = 52.dp)
            ) {
                Text(
                    "설정",
                    style = Mg.bold(37, 50),
                    color = Mg.Ink,
                    modifier = Modifier.padding(start = 17.dp)
                )
                Spacer(Modifier.height(34.dp))
                SettingsPage.entries.forEach { item ->
                    MenuItem(item, item == page) { onPage(item) }
                    Spacer(Modifier.height(15.dp))
                }
            }
            Column(
                Modifier.weight(
                    1f
                ).fillMaxHeight().padding(start = 43.dp, end = 43.dp, top = 52.dp, bottom = 24.dp)
            ) {
                Text(
                    if (page == SettingsPage.ALERTS) "알림" else page.label,
                    style = Mg.bold(40, 54),
                    color = Mg.Ink
                )
                Spacer(Modifier.height(32.dp))
                content()
            }
        }
        toast?.let { SettingsToast(it, Modifier.align(Alignment.TopCenter).offset(y = 548.dp)) }
        dialog?.let { show ->
            Box(
                Modifier.fillMaxSize().background(Mg.Scrim).clickable(enabled = false) {
                },
                contentAlignment = Alignment.Center
            ) { show() }
        }
    }
}

@Composable
private fun MenuItem(item: SettingsPage, selected: Boolean, onClick: () -> Unit) {
    val fg = if (selected) Color.White else Mg.Ink
    Row(
        Modifier.fillMaxWidth().height(86.dp).clip(RoundedCornerShape(20.dp))
            .background(if (selected) Mg.Ink else Color.Transparent)
            .clickable(onClick = onClick).padding(start = 31.dp, end = 26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MgIcon(item.icon, fg, 30.dp)
        Spacer(Modifier.width(18.dp))
        Text(
            item.label,
            style = if (selected) Mg.bold(27, 36) else Mg.regular(27, 36),
            color = fg,
            modifier = Modifier.weight(1f)
        )
        MgIcon(MgIcons.ChevronRight, if (selected) Color.White else Mg.Faint, 26.dp)
    }
}

// region 알림 (Figma 471:56978 "알림" section only)

@Composable
internal fun AlertsPage(
    alerts: AlertSettings,
    onRest: (Boolean) -> Unit,
    onWindow: (Boolean) -> Unit,
    onArrival: (Boolean) -> Unit,
    onOpenInterval: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        ToggleRow(
            MgIcons.Cup,
            "휴식 쉼터 제안",
            "배변 주기에 맞춰 쉴 곳을 알려 줘요 · ${intervalText(alerts.intervalMinutes)} 간격",
            checked = alerts.restSuggestion,
            enabled = alerts.hasPet,
            onChange = onRest,
            onClickBody = onOpenInterval
        )
        ToggleRow(
            MgIcons.Window,
            "창문 안전 알림",
            "기준보다 많이 열리면 알려 줘요 · 기준 ${alerts.windowAlertPercent}%",
            checked = alerts.windowAlert,
            enabled = alerts.hasPet,
            onChange = onWindow
        )
        ToggleRow(
            MgIcons.Flag,
            "도착 요약",
            "도착하면 점수와 다음 운전 팁을 보여 줘요",
            checked = alerts.arrivalSummary,
            onChange = onArrival
        )
    }
    if (!alerts.hasPet) {
        Spacer(Modifier.height(20.dp))
        Note("반려견을 등록하면 휴식 · 창문 알림을 바꿀 수 있어요")
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    body: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    onClickBody: (() -> Unit)? = null,
    height: Dp = 123.dp,
    titleSize: Int = 28,
    bodySize: Int = 23
) {
    RowCard(height) {
        Row(
            Modifier.weight(1f).then(
                if (onClickBody !=
                    null
                ) {
                    Modifier.clickable(onClick = onClickBody)
                } else {
                    Modifier
                }
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Badge(
                icon,
                background = if (enabled) Mg.IconBg else Mg.Chip,
                tint = if (enabled) Mg.OrangeText else Mg.Muted
            )
            Spacer(Modifier.width(23.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = Mg.bold(titleSize, (titleSize * 1.3f).toInt()), color = Mg.Ink)
                Text(
                    body,
                    style = Mg.regular(bodySize, (bodySize * 1.35f).toInt()),
                    color = Mg.Muted
                )
            }
        }
        SettingsToggle(checked, onChange, enabled)
    }
}

@Composable
private fun RowCard(height: Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(
            height
        ).clip(RoundedCornerShape(Mg.Radius)).background(Color.White)
            .padding(start = 29.dp, end = 37.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun Badge(
    icon: ImageVector,
    size: Dp = 63.dp,
    background: Color = Mg.IconBg,
    tint: Color = Mg.OrangeText
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center
    ) {
        MgIcon(icon, tint, size * 0.45f)
    }
}

/** Figma toggle: 86 x 48, orange when on, no border or check mark. */
@Composable
private fun SettingsToggle(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Box(
        Modifier.size(86.dp, 48.dp).clip(CircleShape)
            .background(if (checked && enabled) Mg.Orange else Mg.BarGrey)
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(5.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = Mg.bold(23, 31),
        color = Mg.Muted,
        modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
private fun Note(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        MgIcon(MgIcons.Info, Mg.Muted, 24.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, style = Mg.regular(23, 31), color = Mg.Muted)
    }
}

// endregion

// region 차량 연동 관리 (Figma 470:56154 연결됨 · 470:56498 해지됨)

private data class Feature(val icon: ImageVector, val title: String, val body: String)

private val requiredFeatures = listOf(
    Feature(MgIcons.Gauge, "주행 데이터", "속도 · 브레이크 · 조향으로 흔들림 감지 · 점수 계산"),
    Feature(MgIcons.Window, "창문", "열림 확인 · 닫기는 알림에서 직접 누를 때만 동작해요"),
    Feature(MgIcons.Thermo, "실내 온도 · 공조", "온도 확인 · 기준을 넘으면 알림"),
    Feature(MgIcons.Route, "경로 · 경유지", "반려견 쉼터를 경유지로 넣어요")
)

@Composable
internal fun VehiclePage(
    link: VehicleLinkState,
    identity: VehicleIdentity?,
    voice: Boolean,
    onUnlink: () -> Unit,
    onRelink: () -> Unit,
    onVoice: (Boolean) -> Unit
) {
    val linked = link.linked
    // 연결된 차량
    Row(
        Modifier.fillMaxWidth().height(
            170.dp
        ).clip(RoundedCornerShape(Mg.Radius)).background(Color.White)
            .padding(start = 32.dp, end = 37.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(
                186.dp,
                116.dp
            ).clip(RoundedCornerShape(20.dp)).background(if (linked) Mg.IconBg else Mg.Chip),
            contentAlignment = Alignment.Center
        ) {
            Image(
                rememberVectorPainter(carIllustration(if (linked) Mg.Orange else Mg.BarGrey)),
                contentDescription = null,
                modifier = Modifier.size(158.dp, 75.dp)
            )
        }
        Spacer(Modifier.width(36.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                identity?.displayName ?: if (linked) "연결된 차량" else "차량",
                style = Mg.bold(35, 46),
                color = Mg.Ink
            )
            Text(
                when {
                    !linked ->
                        "연동 해지됨" +
                            (link.changedAt?.let { " · ${unlinkedAtLabel(it)}" } ?: "")
                    identity == null -> "mungo 연결됨 · 차량 정보를 읽는 중"
                    identity.displayName == null -> "mungo 연결됨 · 차종 정보를 받지 못했어요"
                    else -> "mungo 연결됨 · ${syncLabel(identity.readAt)}"
                },
                style = Mg.regular(24, 32),
                color = Mg.Muted
            )
        }
        if (linked) {
            Chip("연결됨", Mg.OrangeText, Mg.IconBg, MgIcons.Check)
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.heightIn(
                    min = 53.dp
                ).clip(CircleShape).border(2.dp, Mg.Line, CircleShape).background(Color.White)
                    .clickable(onClick = onUnlink).padding(horizontal = 28.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text("해지", style = Mg.bold(24, 32), color = Mg.Ink) }
        } else {
            Chip("연결 안 됨", Mg.Ink, Mg.Chip)
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.heightIn(min = 53.dp).clip(CircleShape).background(Mg.Orange)
                    .clickable(onClick = onRelink).padding(horizontal = 28.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text("다시 연결", style = Mg.bold(24, 32), color = Color.White) }
        }
    }
    SectionLabel(
        if (linked) {
            "필수 · 멍고의 핵심 기능이라 따로 끌 수 없어요"
        } else {
            "필수 · 다시 연결하면 바로 사용할 수 있어요 · 이동 기록과 프로필은 그대로 있어요"
        }
    )
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        requiredFeatures.forEach { f ->
            RowCard(110.dp) {
                Badge(
                    f.icon,
                    size = 60.dp,
                    background = if (linked) Mg.IconBg else Mg.Chip,
                    tint = if (linked) Mg.OrangeText else Mg.Muted
                )
                Spacer(Modifier.width(23.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(f.title, style = Mg.bold(27, 36), color = Mg.Ink)
                    Text(f.body, style = Mg.regular(21, 28), color = Mg.Muted)
                }
                if (linked) {
                    Chip("사용 중", Color(0xFF4E5968), Mg.Chip, MgIcons.Check, small = true)
                } else {
                    Chip("멈춤", Mg.Faint, Mg.Chip, small = true)
                }
            }
        }
    }
    SectionLabel("선택")
    ToggleRow(
        MgIcons.Speaker, "음성 안내",
        if (linked) "멍고가 상태와 제안을 말로 알려 줘요" else "연동을 다시 켜면 함께 켜져요",
        checked = linked && voice, enabled = linked, onChange = onVoice,
        height = 110.dp, titleSize = 27, bodySize = 21
    )
}

@Composable
private fun Chip(
    text: String,
    color: Color,
    background: Color,
    icon: ImageVector? = null,
    small: Boolean = false
) {
    Row(
        Modifier.clip(CircleShape).background(background)
            .padding(
                horizontal = if (small) 20.dp else 24.dp,
                vertical = if (small) 8.dp else 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            MgIcon(it, color, if (small) 20.dp else 24.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = if (small) Mg.bold(21, 28) else Mg.bold(24, 32), color = color)
    }
}

/** 차량 연동 해지 확인 (Figma 470:56472) */
@Composable
private fun UnlinkDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Column(
        Modifier.width(
            857.dp
        ).clip(
            RoundedCornerShape(33.dp)
        ).background(Color.White).padding(horizontal = 53.dp, vertical = 48.dp)
    ) {
        Badge(MgIcons.Car, size = 73.dp)
        Spacer(Modifier.height(28.dp))
        Text("차량 연동을 해지할까요?", style = Mg.bold(35, 46), color = Mg.Ink)
        Spacer(Modifier.height(10.dp))
        Text("멍고가 차량 데이터를 더 이상 읽지 않아요.", style = Mg.regular(23, 31), color = Mg.Muted)
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Bullet(MgIcons.Close, Mg.OrangeText, "주행 케어 · 창문 알림 · 휴식 제안이 멈춰요", strong = true)
            Bullet(MgIcons.Check, Mg.Muted, "이동 기록과 반려견 프로필은 그대로 남아요")
            Bullet(MgIcons.Check, Mg.Muted, "다시 연결하면 바로 이어서 쓸 수 있어요")
        }
        Spacer(Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                Modifier.weight(
                    1f
                ).height(
                    103.dp
                ).clip(
                    CircleShape
                ).border(2.dp, Mg.Line, CircleShape).clickable(onClick = onCancel),
                contentAlignment = Alignment.Center
            ) { Text("취소", style = Mg.bold(27, 36), color = Mg.Ink) }
            Box(
                Modifier.weight(
                    1f
                ).height(
                    103.dp
                ).clip(CircleShape).background(Mg.Orange).clickable(onClick = onConfirm),
                contentAlignment = Alignment.Center
            ) { Text("해지하기", style = Mg.bold(27, 36), color = Color.White) }
        }
    }
}

@Composable
private fun Bullet(icon: ImageVector, tint: Color, text: String, strong: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MgIcon(icon, tint, 26.dp)
        Spacer(Modifier.width(16.dp))
        Text(text, style = if (strong) Mg.bold(23, 31) else Mg.regular(23, 31), color = Mg.Ink)
    }
}

@Composable
private fun SettingsToast(text: String, modifier: Modifier) {
    Row(
        modifier.height(
            86.dp
        ).clip(CircleShape).background(Mg.Ink).padding(start = 30.dp, end = 36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Mg.Orange),
            contentAlignment = Alignment.Center
        ) {
            MgIcon(MgIcons.Check, Color.White, 24.dp)
        }
        Spacer(Modifier.width(16.dp))
        Text(text, style = Mg.bold(25, 34), color = Color.White)
    }
}

/** Figma "차량 일러스트" (157.82 x 74.91). [body] is orange while linked, grey after unlinking. */
@Composable
private fun carIllustration(body: Color): ImageVector = remember(body) {
    val glass = Color(0xFFFFF7EE)
    val tyre = Color(0xFF191F28)
    val hub = Color(0xFFC9CED4)
    ImageVector.Builder("car", 158.dp, 75.dp, 157.82f, 74.91f).apply {
        addPath(
            pathData = addPathNodes(
                "M0 56.18V40.13C1.78002 32.9967 7.13002 28.5367 16.05 26.75L40.12 24.08L58.85 5.35C62.4167 1.78333 67.7667 0 74.9 0H115.02C122.153 0 127.503 2.67667 131.07 8.03L144.45 24.08C151.583 25.86 156.04 30.3167 157.82 37.45V56.18C157.82 59.7467 156.037 61.53 152.47 61.53H5.35002C1.78335 61.53 0 59.7467 0 56.18Z"
            ),
            fill = SolidColor(body)
        )
        addPath(
            pathData = addPathNodes(
                "M61.52 22.74L70.88 9.36C72.6667 7.58 75.3434 6.69 78.91 6.69H96.3V22.74H61.52Z"
            ),
            fill = SolidColor(glass)
        )
        addPath(
            pathData = addPathNodes(
                "M102.99 6.69H119.04C122.607 6.69 125.28 8.02667 127.06 10.7L135.09 22.74H102.99V6.69Z"
            ),
            fill = SolidColor(glass)
        )
        addPath(
            pathData = addPathNodes(
                "M10.69 33.44H5.35002C3.8699 33.44 2.67002 34.6399 2.67002 36.12V37.45C2.67002 38.9301 3.8699 40.13 5.35002 40.13H10.69C12.1701 40.13 13.37 38.9301 13.37 37.45V36.12C13.37 34.6399 12.1701 33.44 10.69 33.44Z"
            ),
            fill = SolidColor(glass)
        )
        addPath(
            pathData = addPathNodes(
                "M34.77 74.91A13.38 13.38 0 1 0 34.77 48.15A13.38 13.38 0 1 0 34.77 74.91Z"
            ),
            fill = SolidColor(tyre)
        )
        addPath(
            pathData = addPathNodes(
                "M34.77 66.88A5.35 5.35 0 1 0 34.77 56.18A5.35 5.35 0 1 0 34.77 66.88Z"
            ),
            fill = SolidColor(hub)
        )
        addPath(
            pathData = addPathNodes(
                "M125.72 74.91A13.38 13.38 0 1 0 125.72 48.15A13.38 13.38 0 1 0 125.72 74.91Z"
            ),
            fill = SolidColor(tyre)
        )
        addPath(
            pathData = addPathNodes(
                "M125.72 66.88A5.35 5.35 0 1 0 125.72 56.18A5.35 5.35 0 1 0 125.72 66.88Z"
            ),
            fill = SolidColor(hub)
        )
    }.build()
}

private fun unlinkedAtLabel(epoch: Long): String {
    val then = Calendar.getInstance().apply { timeInMillis = epoch }
    val now = Calendar.getInstance()
    val sameDay =
        then.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            then.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    val time = SimpleDateFormat("a h:mm", Locale.KOREAN).format(then.time)
    return if (sameDay) {
        "오늘 $time"
    } else {
        SimpleDateFormat("M월 d일 ", Locale.KOREAN).format(then.time) +
            time
    }
}

private fun syncLabel(readAt: Long): String {
    val minutes = (System.currentTimeMillis() - readAt) / 60_000
    return if (minutes < 1) "방금 동기화" else "${minutes}분 전 동기화"
}

// endregion

// region 앱 정보 (Figma 470:56789) — UI only; rows do not navigate yet

private data class SdkFeature(val icon: ImageVector, val title: String, val sdk: String)

private val sdkFeatures = listOf(
    SdkFeature(MgIcons.Gauge, "속도 · 가속 · 기어", "Vehicle SDK"),
    SdkFeature(MgIcons.Window, "창문 · 실내 온도", "Vehicle SDK"),
    SdkFeature(MgIcons.Brake, "급정거 · 급커브", "ADAS SDK"),
    SdkFeature(MgIcons.Mic, "mungo 음성 안내", "Gleo AI SDK"),
    SdkFeature(MgIcons.Route, "경유지 추가", "NaviHelper SDK")
)

@Composable
internal fun ColumnScope.AppInfoPage(version: String, linked: Boolean) {
    Row(
        Modifier.fillMaxWidth().height(
            170.dp
        ).clip(RoundedCornerShape(Mg.Radius)).background(Color.White)
            .padding(start = 44.dp, end = 43.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MungoLogo(height = 73.dp)
        Spacer(Modifier.weight(1f))
        Text("버전 $version", style = Mg.bold(32, 42), color = Mg.Ink)
        Spacer(Modifier.width(18.dp))
        Chip("최신", Mg.Ink, Mg.Chip, small = true)
    }
    Spacer(Modifier.height(28.dp))
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(27.dp)) {
        InfoCard(Modifier.weight(1f).fillMaxHeight()) {
            Text("멍고가 쓰는 차량 기능", style = Mg.bold(28, 37), color = Mg.Ink)
            Spacer(Modifier.height(6.dp))
            Text("Pleos SDK로 연결돼 있어요", style = Mg.regular(23, 31), color = Mg.Muted)
            Spacer(Modifier.height(20.dp))
            sdkFeatures.forEachIndexed { index, f ->
                if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Mg.Line))
                Row(
                    Modifier.fillMaxWidth().height(122.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Badge(f.icon, size = 61.dp)
                    Spacer(Modifier.width(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(f.title, style = Mg.bold(25, 34), color = Mg.Ink)
                        Text(f.sdk, style = Mg.regular(20, 27), color = Mg.Muted)
                    }
                    if (linked) {
                        Chip("연결", Mg.OrangeText, Mg.IconBg, MgIcons.Check, small = true)
                    } else {
                        Chip("멈춤", Mg.Faint, Mg.Chip, small = true)
                    }
                }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            InfoCard(Modifier.fillMaxWidth()) {
                Text("멀미징후 기록", style = Mg.bold(28, 37), color = Mg.Ink)
                Spacer(Modifier.height(8.dp))
                LinkRow("기록 보관 기간", "90일")
                Divider()
                LinkRow("기록 내보내기", "휴대폰으로")
                Divider()
                LinkRow("모든 기록 지우기", null)
            }
            InfoCard(Modifier.fillMaxWidth().weight(1f)) {
                Text("기타", style = Mg.bold(28, 37), color = Mg.Ink)
                Spacer(Modifier.height(8.dp))
                LinkRow("도움말", null)
                Divider()
                LinkRow("문의하기", null)
                Divider()
                LinkRow("이용약관 · 개인정보 처리방침", null)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "© 2026 Team 멍고 · 현대자동차그룹 차량용 앱 개발 공모전 출품작",
        style = Mg.regular(19, 25),
        color = Color(0xFFB0B8C1),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InfoCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(
            RoundedCornerShape(Mg.Radius)
        ).background(Color.White).padding(horizontal = 36.dp, vertical = 36.dp),
        content = content
    )
}

/** Display only for now (기능 미구현): value and chevron as in the design, no navigation. */
@Composable
private fun LinkRow(label: String, value: String?) {
    Row(Modifier.fillMaxWidth().height(86.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Mg.regular(25, 34), color = Mg.Ink, modifier = Modifier.weight(1f))
        value?.let {
            Text(it, style = Mg.bold(24, 32), color = Color(0xFF4E5968))
            Spacer(Modifier.width(10.dp))
        }
        MgIcon(MgIcons.ChevronRight, Mg.Faint, 24.dp)
    }
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Mg.Line))

// endregion
