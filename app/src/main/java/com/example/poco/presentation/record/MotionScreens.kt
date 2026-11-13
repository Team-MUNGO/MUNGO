package com.example.poco.presentation.record

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.driving.RestIntervalPolicy
import com.example.poco.data.record.MotionRecord
import com.example.poco.data.record.MotionSign
import com.example.poco.data.record.TripEntity
import com.example.poco.domain.motion.InsightType
import com.example.poco.domain.motion.RestRecommendation
import com.example.poco.presentation.design.IconBadge
import com.example.poco.presentation.design.InfoNote
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.design.MgCard
import com.example.poco.presentation.design.MgIcon
import com.example.poco.presentation.design.MgIcons
import com.example.poco.presentation.design.PrimaryButton
import com.example.poco.presentation.design.SecondaryButton
import com.example.poco.presentation.session.component.intervalText
import com.example.poco.presentation.session.component.koreanDuration
import kotlinx.coroutines.delay

/** A rest-interval change made from the recommendation, kept for the toast and its undo. */
data class AppliedInterval(val from: Int, val to: Int)

private fun hm(minutes: Int) = "%d:%02d".format(minutes / 60, minutes % 60)

/** Figma 465:48703 / 465:48921 "멀미 징후". Counts are guardian-entered, never inferred. */
@Composable
fun MotionSicknessRoute(
    tripId: String,
    onBack: () -> Unit,
    onOpenInterval: (Int) -> Unit,
    applied: AppliedInterval?,
    onUndo: (AppliedInterval) -> Unit,
    onToastShown: () -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(tripId) { viewModel.load(tripId) }
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val all by viewModel.allTrips.collectAsStateWithLifecycle()
    val interval by viewModel.interval.collectAsStateWithLifecycle()
    val analysis by viewModel.motionAnalysis.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var toast by rememberSaveable { mutableStateOf(applied != null) }
    LaunchedEffect(applied) {
        if (applied != null) {
            toast = true
            delay(4_000)
            toast = false
            onToastShown()
        }
    }
    val t = trip
    PageScaffold(
        "멀미 징후",
        onBack,
        titleExtra = { if (applied != null) HeaderChip("보호자 기록") },
        action = {
            HeaderAction(
                if (editing) "수정 완료" else "기록 수정",
                if (editing) MgIcons.Check else MgIcons.Pencil
            ) {
                editing =
                    !editing
            }
        }
    ) {
        if (t == null) return@PageScaffold
        val record = MotionRecord.decode(t.motionSicknessSigns)
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                MgCard(
                    Modifier.width(696.dp).fillMaxHeight(),
                    padding = PaddingValues(start = 37.dp, end = 15.dp, top = 34.dp, bottom = 28.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(end = 20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("이번 이동에서 본 모습", style = Mg.bold(28, 36), color = Mg.Ink)
                            Text(
                                "${dateText(t.startedAtEpoch, "M월 d일")} · 도착 후 기록",
                                style = Mg.regular(23, 30),
                                color = Mg.Muted
                            )
                        }
                        if (applied == null) HeaderChip("보호자 기록")
                    }
                    Spacer(Modifier.height(28.dp))
                    Column(
                        Modifier.weight(1f).padding(end = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MotionSign.entries.forEach { sign ->
                            val n = record?.count(sign) ?: 0
                            Row(
                                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp))
                                    .background(if (n > 0) Mg.Yellow else Color.White)
                                    .then(
                                        if (n >
                                            0
                                        ) {
                                            Modifier
                                        } else {
                                            Modifier.border(
                                                2.dp,
                                                Mg.Line,
                                                RoundedCornerShape(20.dp)
                                            )
                                        }
                                    )
                                    .padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                IconBadge(
                                    motionIcon(sign),
                                    size = 56.dp,
                                    background = if (n >
                                        0
                                    ) {
                                        Color.White
                                    } else {
                                        Mg.Chip
                                    },
                                    tint = if (n >
                                        0
                                    ) {
                                        Mg.OrangeText
                                    } else {
                                        Mg.Muted
                                    }
                                )
                                Text(
                                    sign.label,
                                    Modifier.weight(1f),
                                    style = if (n >
                                        0
                                    ) {
                                        Mg.bold(28, 36)
                                    } else {
                                        Mg.regular(28, 36)
                                    },
                                    color = if (n >
                                        0
                                    ) {
                                        Mg.Ink
                                    } else {
                                        Mg.Muted
                                    }
                                )
                                if (editing) {
                                    StepButton(MgIcons.Minus, n > 0) {
                                        viewModel.setMotion(
                                            (
                                                record
                                                    ?: MotionRecord()
                                                ).withCount(sign, n - 1)
                                        )
                                    }
                                }
                                Text(
                                    "${n}회",
                                    style = Mg.bold(31, 39),
                                    color = if (n >
                                        0
                                    ) {
                                        Mg.OrangeText
                                    } else {
                                        Mg.Muted
                                    }
                                )
                                if (editing) {
                                    StepButton(MgIcons.Plus, true) {
                                        viewModel.setMotion(
                                            (
                                                record
                                                    ?: MotionRecord()
                                                ).withCount(sign, n + 1)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    InfoNote(
                        if (record ==
                            null
                        ) {
                            "아직 기록하지 않았어요 · 기록 수정으로 입력할 수 있어요"
                        } else {
                            "걱정되면 수의사와 상담해 주세요"
                        },
                        style = Mg.regular(21, 28)
                    )
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    val recent = windowTrips(all, t, analysis)
                    MgCard(
                        Modifier.fillMaxWidth().height(492.dp),
                        padding = PaddingValues(horizontal = 38.dp, vertical = 32.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "최근 ${recent.size}번 이동 비교",
                                Modifier.weight(1f),
                                style = Mg.bold(28, 36),
                                color = Mg.Ink
                            )
                            Box(
                                Modifier.size(
                                    16.dp
                                ).background(Mg.BarGrey, RoundedCornerShape(3.dp))
                            )
                            Text(" 지난 이동", style = Mg.regular(21, 28), color = Mg.Muted)
                            Spacer(Modifier.width(20.dp))
                            Box(
                                Modifier.size(16.dp).background(Mg.Orange, RoundedCornerShape(3.dp))
                            )
                            Text(" 이번 이동", style = Mg.regular(21, 28), color = Mg.Muted)
                        }
                        MotionBars(
                            recent,
                            t,
                            applied != null,
                            Modifier.fillMaxWidth().weight(1f).padding(top = 20.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    val insight = analysis?.insight
                    val tip = insight?.let(::insightTip)
                    BulbCard(
                        tip?.title ?: "이동 기록을 더 모으면 변화를 비교할 수 있어요.",
                        tip?.body ?: "15분 이상 이동하고 징후를 기록하면 비교에 쓰여요.",
                        Modifier.fillMaxWidth().heightIn(min = 115.dp),
                        color = Mg.Yellow,
                        bodyStyle = Mg.regular(23, 30),
                        bodyColor = Color(0xFF4E5968)
                    )
                    Spacer(Modifier.height(24.dp))
                    // Insight and recommendation come from the same analysis result; nothing is applied
                    // until the guardian accepts it on the interval screen.
                    val recommendation = analysis?.recommendation
                    val recommended = if (applied == null) analysis?.recommendedMinutes else null
                    // Keep: wraps its content with equal top/bottom padding instead of filling the column.
                    val keep = applied == null && recommended == null
                    MgCard(
                        if (keep) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().weight(1f),
                        padding = PaddingValues(horizontal = 27.dp, vertical = 28.dp)
                    ) {
                        ChevronTitle(
                            when {
                                applied != null -> "다음 이동부터 이렇게 바뀌어요"
                                recommended != null -> "다음 이동에 이렇게 바꿔 볼까요?"
                                else -> "다음 이동도 지금 간격을 유지해요"
                            },
                            recommended?.let { r -> { onOpenInterval(r) } }
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            Modifier.fillMaxWidth().height(
                                150.dp
                            ).background(
                                Mg.Yellow,
                                RoundedCornerShape(20.dp)
                            ).padding(horizontal = 30.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            IconBadge(MgIcons.Cup, size = 66.dp, background = Color.White)
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    if (applied == null &&
                                        recommended == null
                                    ) {
                                        "휴식 간격"
                                    } else {
                                        "휴식 추천 간격"
                                    },
                                    style = Mg.bold(28, 36),
                                    color = Mg.Ink
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    val from = applied?.from ?: interval
                                    val to = applied?.to ?: recommended
                                    // Figma 465:48812: 지금 R25 · 추천 B25 망고
                                    Text(
                                        intervalText(from),
                                        style = Mg.regular(25, 32),
                                        color = Color(0xFF4E5968)
                                    )
                                    if (to != null) {
                                        MgIcon(MgIcons.ArrowRight, Mg.OrangeText, 18.dp)
                                        Text(
                                            intervalText(to),
                                            style = Mg.bold(25, 32),
                                            color = Mg.OrangeText
                                        )
                                    } else {
                                        Text(
                                            "· 지금 간격 유지",
                                            style = Mg.regular(25, 32),
                                            color = Color(0xFF4E5968)
                                        )
                                    }
                                }
                            }
                            when {
                                applied != null -> Row(
                                    Modifier.height(
                                        70.dp
                                    ).background(
                                        Mg.Orange,
                                        CircleShape
                                    ).padding(horizontal = 30.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MgIcon(MgIcons.Check, Color.White, 18.dp)
                                    Text("적용됨", style = Mg.bold(25, 32), color = Color.White)
                                }
                                recommended != null -> Switch(
                                    false,
                                    { if (it) onOpenInterval(recommended) },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = Mg.Orange,
                                        uncheckedTrackColor = Mg.BarGrey,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedBorderColor = Color.Transparent
                                    )
                                )
                            }
                        }
                        if (applied == null && recommendation is RestRecommendation.Shorten) {
                            Spacer(Modifier.height(18.dp))
                            InfoNote(
                                "스위치를 켜면 상세 페이지에서 휴식 간격을 조절할 수 있어요. 직접 적용하기 전엔 바뀌지 않아요.",
                                style = Mg.regular(21, 28)
                            )
                        }
                    }
                }
            }
            if (toast && applied != null) {
                Row(
                    Modifier.align(
                        Alignment.Center
                    ).offset(
                        y = (-40).dp
                    ).height(91.dp).background(Mg.Ink, CircleShape).padding(horizontal = 40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        Modifier.size(32.dp).background(Mg.Orange, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        MgIcon(MgIcons.Check, Color.White, 16.dp)
                    }
                    Text(
                        "휴식 간격을 ${intervalText(applied.to)}으로 바꿨어요",
                        style = Mg.regular(25, 32),
                        color = Color.White
                    )
                    Box(Modifier.width(2.dp).height(30.dp).background(Mg.Muted))
                    Text(
                        "되돌리기",
                        Modifier.clickable {
                            toast = false
                            onUndo(applied)
                        },
                        style = Mg.bold(25, 32),
                        color = Mg.Orange
                    )
                }
            }
        }
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(if (enabled) Color.White else Mg.Chip)
            .border(2.dp, Mg.Line, CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { MgIcon(icon, if (enabled) Mg.Ink else Mg.Faint, 22.dp) }
}

@Composable
private fun MotionBars(
    recent: List<Pair<TripEntity, MotionRecord?>>,
    current: TripEntity,
    withDuration: Boolean,
    modifier: Modifier
) {
    val measurer = rememberTextMeasurer()
    // Figma 465:48770: 눈금 R20 · 개수 B20 · 날짜 R23(이번 B23) · 점수 R19
    val axis = Mg.regular(20, 26).copy(color = Mg.Muted)
    val countStyle = Mg.bold(20, 26)
    val subStyle = Mg.regular(19, 25)
    val dateStyle = Mg.regular(23, 30)
    val maxCount = (recent.maxOfOrNull { it.second?.total ?: 0 } ?: 0).coerceAtLeast(4)
    Canvas(modifier) {
        val left = 40.dp.toPx()
        val labelsH = 70.dp.toPx()
        val topPad = 34.dp.toPx()
        val h = size.height - labelsH - topPad
        val w = size.width - left
        for (i in 0..4) {
            val v = maxCount * i / 4
            val y = topPad + h * (1f - i / 4f)
            drawLine(
                Mg.Line,
                Offset(left, y),
                Offset(size.width, y),
                2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
            val text = measurer.measure("$v", axis)
            drawText(text, topLeft = Offset(0f, y - text.size.height / 2))
        }
        val slot = w / recent.size.coerceAtLeast(1)
        val barW = (slot * 0.42f).coerceAtMost(76.dp.toPx())
        recent.forEachIndexed { i, (trip, record) ->
            val cx = left + slot * (i + 0.5f)
            val isNow = trip.id == current.id
            val n = record?.total
            val bh = if (n == null) 0f else (n / maxCount.toFloat()) * h
            val color = if (isNow) Mg.Orange else Mg.BarGrey
            drawRoundRect(
                color,
                Offset(cx - barW / 2, topPad + h - bh.coerceAtLeast(4f)),
                Size(barW, bh.coerceAtLeast(4f)),
                CornerRadius(8.dp.toPx())
            )
            val countText = measurer.measure(
                n?.let {
                    "${it}개"
                } ?: "기록 없음",
                countStyle.copy(color = if (isNow) Mg.OrangeText else Color(0xFF4E5968))
            )
            drawText(
                countText,
                topLeft = Offset(
                    cx - countText.size.width / 2,
                    topPad + h - bh - countText.size.height - 6.dp.toPx()
                )
            )
            val date = measurer.measure(
                dateText(trip.startedAtEpoch, "M/d"),
                (
                    if (isNow) {
                        dateStyle.copy(
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        dateStyle
                    }
                    ).copy(color = if (isNow) Mg.Ink else Mg.Muted)
            )
            drawText(date, topLeft = Offset(cx - date.size.width / 2, topPad + h + 12.dp.toPx()))
            val sub =
                (if (withDuration) "${koreanDuration(tripDurationMs(trip))} · " else "") +
                    (trip.finalScore?.let { "${it}점" } ?: "")
            val subText = measurer.measure(
                sub,
                subStyle.copy(color = if (isNow) Mg.Orange else Mg.Muted)
            )
            drawText(
                subText,
                topLeft = Offset(
                    cx - subText.size.width / 2,
                    topPad + h + 44.dp.toPx()
                )
            )
        }
    }
}

/** Figma 465:48834 "휴식 간격": 0:30–3:00 in 30-minute steps, recommendation marker, 취소 / 적용하기. */
@Composable
fun RestIntervalRoute(
    recommended: Int?,
    tripId: String?,
    onCancel: () -> Unit,
    onApplied: (AppliedInterval) -> Unit,
    settings: RestSettingsViewModel = hiltViewModel(),
    detail: TripDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(tripId) { tripId?.let(detail::load) }
    val current by settings.interval.collectAsStateWithLifecycle()
    val trip by detail.trip.collectAsStateWithLifecycle()
    val all by detail.allTrips.collectAsStateWithLifecycle()
    val analysis by detail.motionAnalysis.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    val value = if (selected < 0) recommended ?: current else selected
    PageScaffold("휴식 간격", onCancel) {
        MgCard(
            Modifier.fillMaxWidth().height(201.dp),
            padding = PaddingValues(horizontal = 23.dp, vertical = 22.dp)
        ) {
            IntervalSlider(value, current, recommended) { selected = it }
        }
        Spacer(Modifier.height(26.dp))
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            MgCard(
                Modifier.weight(803f).fillMaxHeight(),
                padding = PaddingValues(horizontal = 37.dp, vertical = 40.dp)
            ) {
                Text("왜 바뀌나요?", style = Mg.bold(28, 36), color = Mg.OrangeText)
                Spacer(Modifier.height(24.dp))
                val t = trip
                val recent = if (t !=
                    null
                ) {
                    windowTrips(all, t, analysis).takeLast(3)
                } else {
                    emptyList()
                }
                val insight = analysis?.insight
                when {
                    recommended != null && insight?.type == InsightType.LONG_TRIP_INCREASE -> {
                        Text(insightTip(insight).title, style = Mg.bold(27, 36), color = Mg.Ink)
                        Text("그 전에 한 번 쉬어 가 보세요.", style = Mg.regular(27, 36), color = Mg.Ink)
                    }
                    else -> {
                        Text(
                            "${intervalText(value)}마다 쉬어 가도록 바꿀 수 있어요.",
                            style = Mg.bold(27, 36),
                            color = Mg.Ink
                        )
                        Text(
                            "주행 중에 바꾸면 다음 휴식 시간도 바로 다시 계산돼요.",
                            style = Mg.regular(23, 32),
                            color = Mg.Muted
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                val maxSigns = (recent.maxOfOrNull { it.second?.total ?: 0 } ?: 0).coerceAtLeast(1)
                recent.forEach { (trip, record) ->
                    val isNow = trip.id == t?.id
                    Row(
                        Modifier.fillMaxWidth().height(93.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Figma 465:48868: 날짜 R24 · 시간 R24 #4E5968 · 개수 S24 (이번 이동은 S24 진하게)
                        Text(
                            dateText(trip.startedAtEpoch, "M/d"),
                            Modifier.width(97.dp),
                            style = if (isNow) Mg.semi(24, 32) else Mg.Body,
                            color = if (isNow) Mg.Ink else Mg.Muted
                        )
                        Text(
                            koreanDuration(tripDurationMs(trip)),
                            Modifier.width(150.dp),
                            style = if (isNow) Mg.semi(24, 32) else Mg.Body,
                            color = if (isNow) Mg.Ink else Color(0xFF4E5968)
                        )
                        BoxWithConstraints(
                            Modifier.weight(1f).height(16.dp).background(Mg.Chip, CircleShape)
                        ) {
                            val n = record?.total ?: 0
                            if (n >
                                0
                            ) {
                                Box(
                                    Modifier.width(
                                        maxWidth * (n / maxSigns.toFloat())
                                    ).fillMaxHeight().background(
                                        if (isNow) Mg.Orange else Mg.BarGrey,
                                        CircleShape
                                    )
                                )
                            }
                        }
                        Text(
                            record?.let {
                                "${it.total}개"
                            } ?: "—",
                            Modifier.width(80.dp).padding(start = 24.dp),
                            style = Mg.semi(24, 32),
                            color = if (isNow) Mg.OrangeText else Mg.Muted
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (recent.isNotEmpty()) InfoNote("징후 수는 보호자가 도착 후 기록한 내용이에요 · 비교는 시간당 발생률 기준이에요")
            }
            MgCard(
                Modifier.weight(785f).fillMaxHeight(),
                padding = PaddingValues(horizontal = 38.dp, vertical = 40.dp)
            ) {
                Text("바뀌는 곳", style = Mg.bold(28, 36), color = Mg.Ink)
                Spacer(Modifier.height(20.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
                    ChangeRow(MgIcons.Heart, "마이펫", "휴식 추천 간격이 ${intervalText(value)}이 돼요.")
                    Box(Modifier.fillMaxWidth().height(2.dp).background(Mg.Line))
                    ChangeRow(MgIcons.Sliders, "휴식 쉼터 제안", "${intervalText(value)}마다 쉴 곳을 알려 드려요.")
                    Box(Modifier.fillMaxWidth().height(2.dp).background(Mg.Line))
                    ChangeRow(MgIcons.Cup, "주행 중에 다음 휴식까지", "${intervalText(value)}부터 거꾸로 세요.")
                }
            }
        }
        Spacer(Modifier.height(27.dp))
        Row(
            Modifier.fillMaxWidth().height(102.dp),
            horizontalArrangement = Arrangement.spacedBy(21.dp)
        ) {
            SecondaryButton(
                "취소",
                onCancel,
                Modifier.weight(348f).fillMaxHeight(),
                style = Mg.bold(31, 40)
            )
            PrimaryButton(
                "적용하기",
                {
                    settings.setInterval(value)
                    onApplied(AppliedInterval(current, value))
                },
                Modifier.weight(1245f).fillMaxHeight(),
                enabled = value != current,
                style = Mg.bold(33, 42)
            )
        }
    }
}

@Composable
private fun ChangeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        IconBadge(icon, size = 64.dp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = Mg.bold(27, 35), color = Mg.Ink)
            Text(body, style = Mg.regular(23, 30), color = Color(0xFF4E5968))
        }
    }
}

@Composable
private fun IntervalSlider(value: Int, current: Int, recommended: Int?, onChange: (Int) -> Unit) {
    val min = RestIntervalPolicy.MIN_MINUTES
    val max = RestIntervalPolicy.MAX_MINUTES
    val step = RestIntervalPolicy.STEP_MINUTES
    val ticks = (min..max step step).toList()
    Row(Modifier.fillMaxWidth().height(110.dp), verticalAlignment = Alignment.Bottom) {
        SliderArrow(MgIcons.ChevronLeft, value > min) { onChange(value - step) }
        BoxWithConstraints(Modifier.weight(1f).height(110.dp).padding(horizontal = 12.dp)) {
            val fraction = (value - min) / (max - min).toFloat()
            val trackW = maxWidth
            recommended?.let { r ->
                val rx = trackW * ((r - min) / (max - min).toFloat())
                Text(
                    "추천",
                    Modifier.offset(
                        x = (rx - 31.dp).coerceAtLeast(0.dp)
                    ).background(
                        Mg.Orange,
                        CircleShape
                    ).padding(horizontal = 12.dp, vertical = 2.dp),
                    style = Mg.bold(23, 30),
                    color = Color.White
                )
            }
            Box(
                Modifier.align(
                    Alignment.BottomStart
                ).fillMaxWidth().height(60.dp).background(Color.White, RoundedCornerShape(16.dp))
            )
            Row(
                Modifier.align(
                    Alignment.BottomStart
                ).width(trackW * fraction.coerceAtLeast(0.12f)).height(60.dp)
                    .background(
                        Color(0xFFFFC27F),
                        RoundedCornerShape(16.dp)
                    ).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MgIcon(MgIcons.Cup, Mg.Ink, 22.dp)
                Text(hm(value), style = Mg.bold(29, 38), color = Mg.Ink)
            }
        }
        SliderArrow(MgIcons.ChevronRight, value < max) { onChange(value + step) }
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth().padding(horizontal = 80.dp)) {
        ticks.forEach { m ->
            Text(
                hm(m) + if (m == current) " · 지금" else "",
                Modifier.weight(1f).clickable { onChange(m) },
                style = if (m == value || m == current) Mg.bold(20, 26) else Mg.regular(20, 26),
                color = when (m) {
                    value -> Mg.OrangeText
                    current -> Color(0xFF4E5968)
                    else -> Mg.Muted
                }
            )
        }
    }
}

@Composable
private fun SliderArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(60.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { MgIcon(icon, if (enabled) Mg.Ink else Mg.BarGrey, 26.dp) }
}
