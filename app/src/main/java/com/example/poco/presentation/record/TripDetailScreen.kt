package com.example.poco.presentation.record

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.data.record.MotionRecord
import com.example.poco.data.record.MotionSign
import com.example.poco.data.record.TripEntity
import com.example.poco.data.record.TripEventEntity
import com.example.poco.data.record.TripEventType
import com.example.poco.presentation.design.IconBadge
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.design.MgCard
import com.example.poco.presentation.design.MgIcon
import com.example.poco.presentation.design.MgIcons
import com.example.poco.presentation.design.Pill
import com.example.poco.presentation.design.ScoreRing
import com.example.poco.presentation.design.ValueText
import com.example.poco.presentation.driving.component.clockText
import com.example.poco.presentation.driving.component.koreanDuration

private fun deltaText(d: Int) = when {
    d > 0 -> "+$d"
    d < 0 -> "−${-d}"
    else -> "0"
}

private fun deltaColor(d: Int) = when {
    d < 0 -> Mg.OrangeText
    else -> Mg.Ink
}

/** Figma 465:48424 "이동기록_02". */
@Composable
fun TripDetailRoute(
    tripId: String,
    onBack: () -> Unit,
    onEvents: () -> Unit,
    onMotion: () -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(tripId) { viewModel.load(tripId) }
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val all by viewModel.allTrips.collectAsStateWithLifecycle()
    val interval by viewModel.interval.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val t = trip
    PageScaffold(
        t?.let(::tripTitle) ?: "이동 기록",
        onBack,
        titleExtra = { t?.let { HeaderChip(dateText(it.startedAtEpoch)) } },
        action = {
            if (t != null) {
                HeaderAction("기록 공유", MgIcons.Share) {
                    val text = shareText(t)
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "기록 공유"))
                }
            }
        }
    ) {
        if (t == null) {
            Text("기록을 불러오는 중이에요", style = Mg.Body, color = Mg.Muted)
            return@PageScaffold
        }
        val previous = previousTrip(all, t)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            MgCard(Modifier.width(495.dp).fillMaxHeight(), padding = PaddingValues(horizontal = 37.dp, vertical = 44.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("컴포트 점수", style = Mg.medium(24, 32), color = Mg.Muted)
                    Spacer(Modifier.height(20.dp))
                    ScoreRing((t.finalScore ?: 0) / 100f, 300.dp, 30.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(t.finalScore?.toString() ?: "—", style = Mg.bold(108, 134), color = Mg.Ink)
                            Text(t.finalScore?.let(::pastComfortLabel) ?: "기록 정리 중", style = Mg.semi(24, 32), color = Mg.Ink)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                InfoRow("출발", clockText(t.startedAtEpoch))
                InfoRow("도착", t.endedAtEpoch?.let(::clockText) ?: if (t.status == "INTERRUPTED") "비정상 종료" else "—")
                InfoRow("이동 시간", koreanDuration(tripDurationMs(t)))
                InfoRow("휴식", "${t.restCount ?: 0}회")
                InfoRow("탑승 위치", seatLabel(t), last = !t.simulated)
                if (t.simulated) InfoRow("데이터", "에뮬레이터", last = true)
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                MgCard(Modifier.fillMaxWidth().height(401.dp), padding = PaddingValues(horizontal = 40.dp, vertical = 32.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("컴포트 점수 변화", Modifier.weight(1f), style = Mg.semi(25, 33), color = Mg.Ink)
                        Legend()
                    }
                    ScoreChart(t, events, Modifier.fillMaxWidth().weight(1f).padding(top = 18.dp))
                }
                Spacer(Modifier.height(28.dp))
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    MgCard(Modifier.weight(637f).fillMaxHeight(), padding = PaddingValues(horizontal = 38.dp, vertical = 30.dp)) {
                        ChevronTitle("이벤트 기록", onEvents)
                        Spacer(Modifier.height(20.dp))
                        val items = timelineItems(t, events).filter { it.kind != ItemKind.BONUS || it.type == "REST" }.take(4)
                        if (items.isEmpty()) Text("기록된 이벤트가 없어요", style = Mg.Body, color = Mg.Muted)
                        items.forEachIndexed { i, item ->
                            Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(clockLabel(item.at), Modifier.width(94.dp), style = Mg.Body, color = Mg.Muted)
                                MgIcon(item.icon, Mg.OrangeText, 22.dp)
                                Spacer(Modifier.width(18.dp))
                                Text(item.title, Modifier.weight(1f), style = Mg.semi(22, 30), color = Mg.Ink, maxLines = 1)
                                Text(deltaText(item.delta), style = Mg.bold(24, 33), color = deltaColor(item.delta))
                            }
                            if (i < items.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Mg.Line))
                        }
                    }
                    MotionSummaryCard(t, previous, onMotion, Modifier.weight(432f).fillMaxHeight())
                }
                Spacer(Modifier.height(27.dp))
                val tip = detailTip(t, previous, interval)
                BulbCard(tip.title, tip.body, Modifier.fillMaxWidth().height(123.dp))
            }
        }
    }
}

private fun shareText(t: TripEntity) = buildString {
    appendLine("mungo 이동 기록 · ${tripTitle(t)}")
    appendLine(dateText(t.startedAtEpoch))
    appendLine("컴포트 ${t.finalScore ?: "—"}점 · 이동 ${koreanDuration(tripDurationMs(t))} · 휴식 ${t.restCount ?: 0}회")
    append("급정거 ${t.rapidBrakingCount ?: 0} · 급회전 ${turnCount(t)} · 창문 알림 ${t.windowWarningCount ?: 0}")
}

@Composable
private fun InfoRow(label: String, value: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().height(82.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = Mg.Body, color = Mg.Muted)
        Text(value, style = Mg.semi(22, 29), color = Mg.Ink)
    }
    if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Mg.Line))
}

@Composable
private fun Legend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(14.dp).background(Mg.Orange, CircleShape))
        Text("급정거", style = Mg.Caption, color = Mg.Muted)
        Spacer(Modifier.width(14.dp))
        Box(Modifier.size(14.dp).border(3.dp, Mg.OrangeText, CircleShape))
        Text("창문 알림", style = Mg.Caption, color = Mg.Muted)
        Spacer(Modifier.width(14.dp))
        Box(Modifier.size(14.dp).background(Mg.Track, RoundedCornerShape(3.dp)))
        Text("휴식", style = Mg.Caption, color = Mg.Muted)
    }
}

/** Step line of stored scores (y 60–100), rest bands, braking dots and window rings. */
@Composable
private fun ScoreChart(trip: TripEntity, events: List<TripEventEntity>, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val axis = Mg.medium(19, 25).copy(color = Mg.Muted)
    val start = trip.startedAtEpoch
    val end = trip.endedAtEpoch ?: (events.maxOfOrNull { it.atEpoch } ?: start)
    val points = buildList {
        add(start to trip.initialScore)
        events.filter { it.scoreAfter != null }.sortedBy { it.atEpoch }.forEach { add(it.atEpoch to it.scoreAfter!!) }
        trip.finalScore?.let { add(end to it) }
    }
    val bands = restBands(events)
    Canvas(modifier) {
        val left = 50.dp.toPx()
        val bottomAxis = 40.dp.toPx()
        val w = size.width - left
        val h = size.height - bottomAxis
        val span = (end - start).coerceAtLeast(60_000L).toFloat()
        fun x(t: Long) = left + ((t - start) / span).coerceIn(0f, 1f) * w
        fun y(s: Int) = h * (1f - ((s.coerceIn(60, 100) - 60) / 40f))
        listOf(100, 80, 60).forEach { s ->
            drawLine(Mg.Line, Offset(left, y(s)), Offset(size.width, y(s)), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            val text = measurer.measure("$s", axis)
            drawText(text, topLeft = Offset(0f, y(s) - text.size.height / 2))
        }
        if (points.size >= 2) {
            val fill = Path()
            points.forEachIndexed { i, (t, s) ->
                if (i == 0) {
                    fill.moveTo(x(t), h); fill.lineTo(x(t), y(s))
                } else {
                    fill.lineTo(x(t), y(s))
                }
            }
            fill.lineTo(x(points.last().first), h)
            fill.close()
            drawPath(fill, Mg.Peach)
        }
        bands.forEach { (a, b) ->
            val bx = x(a)
            val bw = (x(b) - bx).coerceAtLeast(30.dp.toPx())
            drawRoundRect(Mg.Track.copy(alpha = 0.85f), Offset(bx, 0f), Size(bw, h), CornerRadius(8.dp.toPx()))
            val text = measurer.measure("휴식", axis.copy(color = Mg.Ink))
            drawText(text, topLeft = Offset(bx + (bw - text.size.width) / 2, 10.dp.toPx()))
        }
        if (points.size >= 2) {
            val line = Path()
            points.forEachIndexed { i, (t, s) -> if (i == 0) line.moveTo(x(t), y(s)) else line.lineTo(x(t), y(s)) }
            drawPath(line, Mg.Orange, style = Stroke(4.dp.toPx()))
        }
        fun scoreAt(t: Long) = points.lastOrNull { it.first <= t }?.second ?: trip.initialScore
        events.filter { it.type == TripEventType.SCORE_PENALTY && it.detail == "RAPID_BRAKING" }.forEach {
            drawCircle(Mg.Orange, 8.dp.toPx(), Offset(x(it.atEpoch), y(it.scoreAfter ?: scoreAt(it.atEpoch))))
        }
        events.filter { it.type == TripEventType.WINDOW_WARNING }.forEach {
            val c = Offset(x(it.atEpoch), y(scoreAt(it.atEpoch)))
            drawCircle(Color.White, 8.dp.toPx(), c)
            drawCircle(Mg.OrangeText, 8.dp.toPx(), c, style = Stroke(3.dp.toPx()))
        }
        trip.finalScore?.let { drawCircle(Mg.Ink, 8.dp.toPx(), Offset(x(end), y(it))) }
        val ticks = listOf(start, start + (end - start) / 3, start + (end - start) * 2 / 3, end)
        ticks.forEachIndexed { i, t ->
            val text = measurer.measure(clockLabel(t), axis)
            val tx = (x(t) - text.size.width / 2).coerceIn(left, size.width - text.size.width)
            drawText(text, topLeft = Offset(if (i == 0) left else tx, h + 12.dp.toPx()))
        }
    }
}

@Composable
private fun MotionSummaryCard(trip: TripEntity, previous: TripEntity?, onOpen: () -> Unit, modifier: Modifier) {
    val now = MotionRecord.decode(trip.motionSicknessSigns)
    val before = previous?.let { MotionRecord.decode(it.motionSicknessSigns) }
    MgCard(modifier, padding = PaddingValues(horizontal = 27.dp, vertical = 30.dp)) {
        ChevronTitle("멀미 징후", onOpen)
        Spacer(Modifier.height(24.dp))
        when {
            now == null -> {
                Text("도착 후 기록하지 않았어요", style = Mg.Body, color = Mg.Muted)
                Spacer(Modifier.height(10.dp))
                Pill("지금 기록하기", background = Mg.Peach, color = Mg.OrangeText, onClick = onOpen)
            }
            now.fine || now.total == 0 -> Text("이번 이동에선 징후가 없었어요", style = Mg.Body, color = Mg.Muted)
            else -> {
                MotionSign.entries.filter { now.count(it) > 0 }.sortedByDescending { now.count(it) }.take(2).forEach { sign ->
                    Row(
                        Modifier.fillMaxWidth().height(128.dp).padding(bottom = 16.dp).background(Mg.Yellow, RoundedCornerShape(20.dp)).padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        IconBadge(motionIcon(sign), size = 56.dp, background = Color.White)
                        Column(Modifier.weight(1f)) {
                            Text(sign.label, style = Mg.semi(23, 30), color = Mg.Ink)
                            Text(
                                before?.let { b -> if (b.count(sign) > 0) "지난번 ${b.count(sign)}회" else "지난번 없음" } ?: "지난 기록 없음",
                                style = Mg.Caption, color = Mg.Muted
                            )
                        }
                        Text("${now.count(sign)}회", style = Mg.bold(38, 48), color = Mg.Ink)
                    }
                }
            }
        }
    }
}

fun motionIcon(sign: MotionSign) = when (sign) {
    MotionSign.YAWN -> MgIcons.Yawn
    MotionSign.DROOL -> MgIcons.Drop
    MotionSign.PANT -> MgIcons.Pant
    MotionSign.SHAKE -> MgIcons.Shake
    MotionSign.VOMIT -> MgIcons.Vomit
    MotionSign.RESTLESS -> MgIcons.Restless
}

/** Figma 465:48555 "이동기록_03_이벤트기록". */
@Composable
fun TripEventsRoute(tripId: String, onBack: () -> Unit, viewModel: TripDetailViewModel = hiltViewModel()) {
    LaunchedEffect(tripId) { viewModel.load(tripId) }
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAt by rememberSaveable { mutableStateOf<Long?>(null) }
    val t = trip
    PageScaffold(
        "이벤트 기록", onBack,
        action = { t?.let { HeaderChip("${dateText(it.startedAtEpoch)} · ${tripTitle(it)}") } }
    ) {
        if (t == null) return@PageScaffold
        val all = timelineItems(t, events)
        val shown = all.filter { filter == null || it.kind.name == filter }
        val selected = shown.firstOrNull { it.at == selectedAt } ?: shown.filter { it.kind == ItemKind.PENALTY }.minByOrNull { it.delta } ?: shown.firstOrNull()
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(27.dp)) {
            MgCard(Modifier.weight(936f).fillMaxHeight(), padding = PaddingValues(horizontal = 32.dp, vertical = 27.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    FilterChip("전체 ${all.size}", filter == null) { filter = null }
                    FilterChip("감점 ${all.count { it.kind == ItemKind.PENALTY }}", filter == "PENALTY") { filter = "PENALTY" }
                    FilterChip("가점 ${all.count { it.kind == ItemKind.BONUS }}", filter == "BONUS") { filter = "BONUS" }
                    FilterChip("알림 ${all.count { it.kind == ItemKind.ALERT }}", filter == "ALERT") { filter = "ALERT" }
                    Spacer(Modifier.weight(1f))
                    Text("시간순", style = Mg.medium(20, 27), color = Mg.Muted)
                }
                Spacer(Modifier.height(24.dp))
                if (shown.isEmpty()) Text("해당하는 이벤트가 없어요", style = Mg.Body, color = Mg.Muted)
                LazyColumn(Modifier.weight(1f)) {
                    items(shown, key = { "${it.at}-${it.type}" }) { item ->
                        val on = item == selected
                        Row(
                            Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(20.dp))
                                .background(if (on) Mg.Yellow else Color.Transparent)
                                .then(if (on) Modifier.border(2.dp, Mg.WarnBorder, RoundedCornerShape(20.dp)) else Modifier)
                                .clickable { selectedAt = item.at }.padding(horizontal = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(clockLabel(item.at), Modifier.width(92.dp), style = Mg.Body, color = Mg.Muted)
                            IconBadge(item.icon, size = 48.dp, background = if (on) Color.White else Mg.IconBg)
                            Spacer(Modifier.width(24.dp))
                            Text(item.title, Modifier.width(192.dp), style = Mg.semi(24, 32), color = Mg.Ink, maxLines = 1)
                            Text(item.detail, Modifier.weight(1f), style = Mg.medium(20, 26), color = Mg.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(deltaText(item.delta), style = Mg.bold(26, 35), color = deltaColor(item.delta))
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Mg.Line))
                    }
                }
            }
            MgCard(Modifier.weight(651f).fillMaxHeight(), padding = PaddingValues(horizontal = 37.dp, vertical = 37.dp)) {
                if (selected == null) {
                    Text("이벤트를 고르면 자세히 보여 드려요", style = Mg.Body, color = Mg.Muted)
                    return@MgCard
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(selected.title.substringBefore(" "), background = Mg.Peach, color = Mg.OrangeText, height = 51.dp, leading = { MgIcon(selected.icon, Mg.OrangeText, 18.dp) })
                    Spacer(Modifier.weight(1f))
                    Text("점수 ${t.initialScore} → ${t.finalScore ?: "—"}", style = Mg.medium(24, 32), color = Mg.Faint)
                }
                Spacer(Modifier.height(30.dp))
                Text("${clockText(selected.at)}에\n${selected.title}${if (selected.kind == ItemKind.ALERT) " 알림이 있었어요" else subjectParticle(selected.title) + " 기록됐어요"}", style = Mg.bold(31, 47), color = Mg.Ink)
                Spacer(Modifier.height(8.dp))
                ValueText(deltaText(selected.delta), "점", Mg.bold(70, 84), deltaColor(selected.delta), Mg.Ink)
                Spacer(Modifier.height(20.dp))
                Text(selected.detail, style = Mg.Body, color = Mg.Muted)
                Spacer(Modifier.height(30.dp))
                Text("그때 ${t.petName}는?", style = Mg.medium(20, 27), color = Mg.Muted)
                Spacer(Modifier.height(14.dp))
                Row { Text("자리", Modifier.width(97.dp), style = Mg.Body, color = Mg.Muted); Text(seatLabel(t), style = Mg.semi(22, 29), color = Mg.Ink) }
                Spacer(Modifier.height(16.dp))
                Row { Text("실내", Modifier.width(97.dp), style = Mg.Body, color = Mg.Muted); Text(temperatureAt(events, selected.at) ?: "측정 기록 없음", style = Mg.semi(22, 29), color = Mg.Ink) }
                Spacer(Modifier.weight(1f))
                Column(Modifier.fillMaxWidth().background(Mg.Yellow, RoundedCornerShape(20.dp)).padding(horizontal = 32.dp, vertical = 26.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MgIcon(MgIcons.Bulb, Mg.OrangeText, 22.dp)
                        Text("${t.petName}의 꿀 TIP", style = Mg.semi(22, 29), color = Mg.OrangeText)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(tipFor(selected.type), style = Mg.semi(23, 30), color = Mg.Ink)
                }
            }
        }
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(59.dp).clip(CircleShape).background(if (selected) Mg.Ink else Mg.Chip).clickable(onClick = onClick).padding(horizontal = 25.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, style = Mg.semi(22, 29), color = if (selected) Color.White else Mg.Ink) }
}

/** 이/가 by the final syllable's batchim; digits and Latin fall back to "이(가)"-free "이". */
internal fun subjectParticle(word: String): String {
    val c = word.trimEnd().lastOrNull() ?: return "이"
    if (c !in '가'..'힣') return "이"
    return if ((c - '가') % 28 == 0) "가" else "이"
}
