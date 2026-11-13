package com.example.poco.presentation.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.driving.RestIntervalPolicy
import com.example.poco.data.record.MotionRecord
import com.example.poco.data.record.MotionSign
import com.example.poco.domain.motion.InsightType
import com.example.poco.domain.motion.MotionAnalysis
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
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.delay

/** A rest-interval change made from the recommendation, kept for the toast and its undo. */
data class AppliedInterval(val from: Int, val to: Int)

private fun hm(minutes: Int) = "%d:%02d".format(minutes / 60, minutes % 60)

/**
 * Figma 515:347 "이동기록_04_멀미징후": 최근 7일 요약 · 징후 × 날짜 표 · 패턴 제안.
 * 체크는 보호자가 도착 후 고른 값만 센다. 이번 이동이 있는 날(주황 열)의 칸을 누르면 그 이동 기록을 고친다.
 */
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
    val tripDay = t?.startedAtEpoch?.toLocalDate()
    val today = remember { LocalDate.now() }
    // 표의 마지막 날. 처음엔 이번 이동이 있는 날
    var endEpochDay by rememberSaveable(tripDay) {
        mutableLongStateOf((tripDay ?: today).toEpochDay())
    }
    val end = LocalDate.ofEpochDay(endEpochDay)
    val week = motionWeek(all.ifEmpty { listOfNotNull(t) }, end)
    PageScaffold(
        "멀미 징후",
        onBack,
        titleExtra = {
            Text(
                "최근 7일 기준",
                Modifier.background(Mg.IconBg, CircleShape)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                style = Mg.bold(21, 28),
                color = Mg.OrangeText
            )
        },
        action = {
            WeekNav(
                week,
                canNext = end < today,
                onPrev = { endEpochDay -= MOTION_WEEK_DAYS },
                onNext = {
                    endEpochDay = minOf(endEpochDay + MOTION_WEEK_DAYS, today.toEpochDay())
                }
            )
        }
    ) {
        if (t == null) return@PageScaffold
        val record = MotionRecord.decode(t.motionSicknessSigns)
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().height(139.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    SummaryTile(MgIcons.Car, "최근 7일 이동", "${week.tripCount}", "번", null, false)
                    SummaryTile(
                        MgIcons.Yawn,
                        "징후가 보인 이동",
                        "${week.signTrips}",
                        "번",
                        "${week.tripCount}번 중".takeIf { week.tripCount > 0 },
                        week.signTrips > 0
                    )
                    val top = week.top
                    SummaryTile(
                        top?.first?.let(::motionIcon) ?: MgIcons.Yawn,
                        "가장 자주 보인 징후",
                        top?.first?.label ?: "없음",
                        null,
                        top?.let { "${week.tripCount}번 중 ${it.second}번" },
                        top != null
                    )
                }
                Spacer(Modifier.height(22.dp))
                MotionTable(
                    week = week,
                    highlight = tripDay?.takeIf { it in week.start..week.end },
                    editable = true,
                    unrecorded = record?.entered != true,
                    onToggle = { sign ->
                        viewModel.setMotion((record ?: MotionRecord()).toggle(sign))
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                Spacer(Modifier.height(22.dp))
                PatternCard(week, analysis, interval, applied, onOpenInterval)
            }
            if (toast && applied != null) {
                AppliedToast(applied) {
                    toast = false
                    onUndo(applied)
                }
            }
        }
    }
}

@Composable
private fun WeekNav(week: MotionWeek, canNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.height(59.dp).background(Mg.Chip, CircleShape).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onPrev),
            contentAlignment = Alignment.Center
        ) { MgIcon(MgIcons.ChevronLeft, Mg.Ink, 18.dp) }
        Text(
            "${week.start.monthValue}월 ${week.start.dayOfMonth}일 – " +
                "${week.end.monthValue}월 ${week.end.dayOfMonth}일",
            Modifier.padding(horizontal = 12.dp),
            style = Mg.bold(24, 32),
            color = Mg.Ink
        )
        Box(
            Modifier.size(44.dp).clip(CircleShape).clickable(enabled = canNext, onClick = onNext),
            contentAlignment = Alignment.Center
        ) { MgIcon(MgIcons.ChevronRight, if (canNext) Mg.Ink else Mg.BarGrey, 18.dp) }
    }
}

@Composable
private fun RowScope.SummaryTile(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String?,
    note: String?,
    accent: Boolean
) {
    val color = if (accent) Mg.OrangeText else Mg.Ink
    Row(
        Modifier.weight(1f).fillMaxHeight().background(Color.White, RoundedCornerShape(Mg.Radius))
            .padding(horizontal = 29.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, size = 64.dp)
        Spacer(Modifier.width(22.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = Mg.regular(21, 28), color = Mg.Muted)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = Mg.bold(43, 52), color = color, maxLines = 1)
                unit?.let {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        it,
                        Modifier.padding(bottom = 6.dp),
                        style = Mg.bold(24, 32),
                        color = color
                    )
                }
            }
        }
        note?.let {
            Text(
                it,
                Modifier.align(Alignment.Bottom).padding(bottom = 30.dp),
                style = Mg.regular(21, 28),
                color = Mg.Muted
            )
        }
    }
}

private val TABLE_LABEL_W = 245.dp
private val TABLE_TOTAL_W = 130.dp
private val TABLE_ROW_H = 68.dp
private val TableDim = Color(0xFFB0B8C1)
private val TableSub = Color(0xFF4E5968)

@Composable
private fun MotionTable(
    week: MotionWeek,
    highlight: LocalDate?,
    editable: Boolean,
    unrecorded: Boolean,
    onToggle: (MotionSign) -> Unit,
    modifier: Modifier
) {
    MgCard(
        modifier,
        padding = PaddingValues(start = 37.dp, end = 37.dp, top = 30.dp, bottom = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("날짜별로 보면", Modifier.weight(1f), style = Mg.bold(29, 38), color = Mg.Ink)
            CheckMark(true, 24.dp)
            Text(" 보였어요", style = Mg.regular(20, 26), color = TableSub)
            Spacer(Modifier.width(18.dp))
            CheckMark(false, 24.dp)
            Text(" 없었어요", style = Mg.regular(20, 26), color = TableSub)
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().weight(1f)) {
            // 이번 이동이 있는 날 열 바탕
            highlight?.let { day ->
                val index = week.days.indexOfFirst { it.date == day }
                if (index >= 0) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val col = (maxWidth - TABLE_LABEL_W - TABLE_TOTAL_W) / MOTION_WEEK_DAYS
                        Box(
                            Modifier.offset(x = TABLE_LABEL_W + col * index + 8.dp)
                                .width(col - 16.dp).fillMaxHeight()
                                .background(Mg.Peach, RoundedCornerShape(16.dp))
                        )
                    }
                }
            }
            Column(Modifier.fillMaxSize()) {
                TableRow(label = {}, total = {
                    Text("7일 합계", style = Mg.bold(20, 26), color = TableSub)
                }, week = week) { day ->
                    val on = day.date == highlight
                    Text(
                        "${dayName(day.date)} ${day.date.monthValue}/${day.date.dayOfMonth}",
                        style = if (on) Mg.bold(20, 26) else Mg.regular(20, 26),
                        color = if (on) Mg.OrangeText else TableSub
                    )
                }
                TableRow(
                    label = { RowLabel(MgIcons.Car, "이동 시간", true) },
                    total = { Text("${week.tripCount}번", style = Mg.bold(23, 30), color = Mg.Ink) },
                    week = week
                ) { day ->
                    if (day.trips == 0) {
                        Text("–", style = Mg.regular(21, 28), color = Mg.BarGrey)
                    } else {
                        Text(
                            hm((day.drivingMs / 60_000).toInt()),
                            Modifier.background(
                                if (day.date == highlight) Color.White else Mg.Chip,
                                CircleShape
                            ).padding(horizontal = 20.dp, vertical = 6.dp),
                            style = Mg.bold(20, 26),
                            color = Mg.Ink
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(Mg.Line))
                MotionSign.entries.forEach { sign ->
                    val seen = week.perSign[sign] ?: 0
                    TableRow(
                        label = { RowLabel(motionIcon(sign), sign.label, seen > 0) },
                        total = {
                            Text(
                                "$seen / ${week.tripCount}",
                                style = Mg.bold(23, 30),
                                color = if (seen > 0) Mg.OrangeText else TableDim
                            )
                        },
                        week = week
                    ) { day ->
                        val tappable = editable && day.date == highlight
                        if (day.trips > 0 && (day.recorded || tappable)) {
                            Box(
                                Modifier.size(52.dp).clip(CircleShape).then(
                                    if (tappable) {
                                        Modifier.clickable {
                                            onToggle(sign)
                                        }
                                    } else {
                                        Modifier
                                    }
                                ),
                                contentAlignment = Alignment.Center
                            ) { CheckMark(sign in day.signs, 36.dp) }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }
        Text(
            "주행이 끝날 때 체크한 \"보였어요\"만 모아서 세요 · 이동이 ${MOTION_PATTERN_MIN_TRIPS}번 이상일 때 판단해요" +
                if (unrecorded && highlight != null) " · 주황 칸을 눌러 이번 이동을 기록해요" else "",
            style = Mg.regular(20, 26),
            color = Mg.Muted
        )
    }
}

@Composable
private fun TableRow(
    label: @Composable () -> Unit,
    total: @Composable () -> Unit,
    week: MotionWeek,
    cell: @Composable (MotionDay) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(TABLE_ROW_H),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(TABLE_LABEL_W)) { label() }
        week.days.forEach { day ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { cell(day) }
        }
        Box(Modifier.width(TABLE_TOTAL_W), contentAlignment = Alignment.Center) { total() }
    }
}

@Composable
private fun RowLabel(icon: ImageVector, text: String, strong: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MgIcon(icon, if (strong) Mg.Ink else Mg.Faint, 22.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            text,
            style = if (strong) Mg.bold(23, 30) else Mg.regular(23, 30),
            color = if (strong) Mg.Ink else Mg.Faint
        )
    }
}

/** 보였어요 = 주황 원 + 흰 체크, 없었어요 = 회색 테두리 원 */
@Composable
private fun CheckMark(on: Boolean, size: Dp) {
    if (on) {
        Box(
            Modifier.size(size).background(Mg.Orange, CircleShape),
            contentAlignment = Alignment.Center
        ) { MgIcon(MgIcons.Check, Color.White, size * 0.45f) }
    } else {
        Box(Modifier.size(size).border(2.dp, Mg.BarGrey, CircleShape))
    }
}

private fun dayName(date: LocalDate) =
    date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.KOREAN)

/** 어두운 패턴 · 제안 카드: 규칙 분석 결과로만 말하고, 바꾸는 건 휴식 간격 화면에서 보호자가 한다 */
@Composable
private fun PatternCard(
    week: MotionWeek,
    analysis: MotionAnalysis?,
    interval: Int,
    applied: AppliedInterval?,
    onOpenInterval: (Int) -> Unit
) {
    val tip = analysis?.insight?.let(::insightTip)
    val recommended = if (applied == null) analysis?.recommendedMinutes else null
    val title = when {
        applied != null -> "다음 이동부터 ${intervalText(applied.to)}마다 쉬어 가요"
        tip != null -> tip.title
        week.tripCount < MOTION_PATTERN_MIN_TRIPS ->
            "이동이 ${MOTION_PATTERN_MIN_TRIPS}번 이상 모이면 패턴을 알려 드릴게요"
        week.signTrips == 0 -> "이번 주는 징후 없이 잘 다녀왔어요"
        else -> "아직 눈에 띄는 패턴은 없어요"
    }
    val body = when {
        applied != null -> "휴식 간격을 ${intervalText(
            applied.from
        )} → ${intervalText(applied.to)}으로 바꿨어요"
        recommended != null ->
            "다음 이동부터 휴식 간격을 ${intervalText(interval)} → ${intervalText(recommended)}으로 줄여 볼까요?"
        tip != null -> tip.body
        week.tripCount < MOTION_PATTERN_MIN_TRIPS -> "주행이 끝날 때 보인 모습을 체크해 주세요"
        else -> "지금처럼 ${intervalText(interval)}마다 쉬어 가요"
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 144.dp)
            .background(Mg.Ink, RoundedCornerShape(Mg.Radius))
            .padding(horizontal = 37.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(MgIcons.Bulb, size = 70.dp, background = Mg.Orange, tint = Color.White)
        Spacer(Modifier.width(26.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = Mg.bold(29, 38), color = Color.White)
            Text(body, style = Mg.regular(21, 28), color = Mg.OnDarkMuted)
        }
        if (recommended != null) {
            Spacer(Modifier.width(24.dp))
            PrimaryButton(
                "휴식 간격 바꿔 보기",
                { onOpenInterval(recommended) },
                Modifier.width(320.dp).height(82.dp),
                style = Mg.bold(25, 32)
            )
        }
    }
}

@Composable
private fun BoxScope.AppliedToast(applied: AppliedInterval, onUndo: () -> Unit) {
    Row(
        Modifier.align(Alignment.Center).offset(y = (-40).dp).height(91.dp)
            .background(Mg.Ink, CircleShape).padding(horizontal = 40.dp),
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
            Modifier.clickable(onClick = onUndo),
            style = Mg.bold(25, 32),
            color = Mg.Orange
        )
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
