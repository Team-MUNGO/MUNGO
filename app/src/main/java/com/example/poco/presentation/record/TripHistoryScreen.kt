package com.example.poco.presentation.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.data.record.TripEntity
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.design.MgIcon
import com.example.poco.presentation.design.MgIcons
import com.example.poco.presentation.session.component.durationText
import com.example.poco.presentation.session.component.koreanDuration

/** Figma 465:48292 "이동기록_01". */
@Composable
fun TripHistoryRoute(onOpen: (String) -> Unit, onNavigate: (RailTab) -> Unit, viewModel: TripHistoryViewModel = hiltViewModel(), initialPeriod: Period = Period.MONTH) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    var period by rememberSaveable(initialPeriod) { mutableStateOf(initialPeriod) }
    val now = System.currentTimeMillis()
    RailScaffold(RailTab.HISTORY, onNavigate) {
        Row(Modifier.fillMaxWidth().height(75.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("이동 기록", Modifier.weight(1f), style = Mg.bold(43, 54), color = Mg.Ink)
            Row(Modifier.width(444.dp).fillMaxHeight().background(Mg.Segment, CircleShape).padding(6.dp)) {
                Period.entries.forEach { p ->
                    val on = p == period
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(if (on) Color.White else Color.Transparent)
                            .clickable { period = p },
                        contentAlignment = Alignment.Center
                    ) { Text(p.label, style = if (on) Mg.bold(25, 32) else Mg.regular(25, 32), color = if (on) Mg.Ink else Color(0xFF4E5968)) }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        val list = trips
        if (list == null) {
            Text("기록을 불러오는 중이에요", style = Mg.Body, color = Mg.Muted)
            return@RailScaffold
        }
        val shown = filterTrips(list, period, now)
        val scored = shown.filter { it.finalScore != null }
        Row(Modifier.fillMaxWidth().height(166.dp), horizontalArrangement = Arrangement.spacedBy(21.dp)) {
            StatTile("이동 횟수", "${shown.size}", "회", MgIcons.Route, Modifier.weight(1f).fillMaxHeight(), large = true)
            StatTile(
                "평균 컴포트 점수", if (scored.isEmpty()) "—" else "${scored.map { it.finalScore!! }.average().toInt()}",
                if (scored.isEmpty()) null else "점", MgIcons.Gauge, Modifier.weight(1f).fillMaxHeight(), large = true
            )
            StatTile("총 이동 시간", durationText(shown.sumOf(::tripDurationMs)), null, MgIcons.Clock, Modifier.weight(1f).fillMaxHeight(), large = true)
            StatTile("휴식", "${shown.sumOf { it.restCount ?: 0 }}", "회", MgIcons.Cup, Modifier.weight(1f).fillMaxHeight(), large = true)
        }
        Spacer(Modifier.height(26.dp))
        if (shown.isEmpty()) {
            Text(if (list.isEmpty()) "아직 저장된 이동 기록이 없어요" else "${period.label} 이동 기록이 없어요", style = Mg.Body, color = Mg.Muted)
            return@RailScaffold
        }
        val newest = shown.first().id
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            shown.groupBy { sectionOf(it, now) }.forEach { (section, rows) ->
                item(key = "h-$section") { Text(section, Modifier.padding(start = 5.dp, top = 6.dp), style = Mg.bold(24, 31), color = Mg.Muted) }
                items(rows, key = { it.id }) { trip -> TripRow(trip, trip.id == newest) { onOpen(trip.id) } }
            }
        }
    }
}

@Composable
private fun TripRow(trip: TripEntity, newest: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(134.dp).clip(RoundedCornerShape(Mg.Radius))
            .background(if (newest) Color.White else Mg.Segment).clickable(onClick = onClick).padding(horizontal = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.width(70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dateText(trip.startedAtEpoch, "M월"), style = Mg.regular(21, 27), color = Mg.Muted)
            Text(dateText(trip.startedAtEpoch, "d"), style = Mg.bold(43, 54), color = Mg.Ink)
        }
        Spacer(Modifier.width(32.dp))
        Box(Modifier.width(2.dp).height(70.dp).background(Mg.BarGrey))
        Spacer(Modifier.width(40.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(tripTitle(trip), style = Mg.bold(31, 40), color = Mg.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    koreanDuration(tripDurationMs(trip)),
                    (trip.restCount ?: 0).takeIf { it > 0 }?.let { "휴식 ${it}회" },
                    "비정상 종료".takeIf { trip.status == "INTERRUPTED" },
                    "에뮬레이터 데이터".takeIf { trip.simulated }
                ).joinToString("  "),
                style = Mg.Body, color = Mg.Muted
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(trip.finalScore?.toString() ?: "—", style = Mg.bold(48, 60), color = Mg.Ink)
            if (trip.finalScore != null) Text("점", Modifier.padding(start = 4.dp, bottom = 8.dp), style = Mg.regular(25, 32), color = Mg.Ink)
        }
        Spacer(Modifier.width(36.dp))
        MgIcon(MgIcons.ChevronRight, Mg.Faint, 22.dp)
    }
}
