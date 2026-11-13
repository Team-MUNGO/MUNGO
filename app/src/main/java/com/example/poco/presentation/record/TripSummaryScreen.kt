package com.example.poco.presentation.record

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.R
import com.example.poco.data.record.MotionRecord
import com.example.poco.data.record.MotionSign
import com.example.poco.data.record.TripEntity
import com.example.poco.data.record.TripEventEntity
import com.example.poco.data.record.TripEventType
import com.example.poco.presentation.design.DesignCanvas
import com.example.poco.presentation.design.DrawableIcon
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.design.MgCard
import com.example.poco.presentation.design.MgIcon
import com.example.poco.presentation.design.MgIcons
import com.example.poco.presentation.design.Pill
import com.example.poco.presentation.design.ScoreRing
import com.example.poco.presentation.design.SecondaryButton
import com.example.poco.presentation.session.SplitLayoutMaxWidth
import com.example.poco.presentation.session.component.clockText
import com.example.poco.presentation.session.component.koreanDuration

/** Trip end screen (Figma 464:46724 full / 464:48080 split). Reads only the stored record. */
@Composable
fun TripSummaryRoute(
    tripId: String,
    onDetail: () -> Unit,
    onDone: () -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(tripId) { viewModel.load(tripId) }
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val all by viewModel.allTrips.collectAsStateWithLifecycle()
    val line by viewModel.oneLine.collectAsStateWithLifecycle()
    BoxWithConstraints(Modifier.fillMaxSize().background(Mg.Bg)) {
        val split = maxWidth < SplitLayoutMaxWidth
        DesignCanvas(if (split) 618f else 1220f, 1028f) {
            Column(
                Modifier.fillMaxSize().padding(
                    horizontal = if (split) 20.dp else 24.dp,
                    vertical = 24.dp
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().height(60.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawableIcon(R.drawable.driving_paw, size = 20.dp)
                    Spacer(Modifier.width(9.dp))
                    Text("mungo", style = Mg.bold(28, 35), color = Mg.Ink)
                    Spacer(Modifier.width(16.dp))
                    // Figma 488:10786: 전체 화면에선 "도착" 칩이 왼쪽 카드 맨 위로 옮겨 갔다
                    if (split) {
                        Pill(
                            "도착",
                            background = Mg.Peach,
                            color = Mg.OrangeText,
                            height = 40.dp,
                            leading = {
                                MgIcon(MgIcons.Flag, Mg.OrangeText, 16.dp)
                            }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(
                            60.dp
                        ).clip(CircleShape).background(Color.White).clickable(onClick = onDone),
                        contentAlignment = Alignment.Center
                    ) {
                        MgIcon(MgIcons.Close, Mg.Ink, 24.dp)
                    }
                }
                Spacer(Modifier.height(22.dp))
                val t = trip
                if (t == null || t.finalScore == null) {
                    Text(
                        if (t ==
                            null
                        ) {
                            "기록을 불러오는 중이에요"
                        } else {
                            "기록을 정리하는 중이에요"
                        },
                        style = Mg.Body,
                        color = Mg.Muted
                    )
                } else {
                    val previous = previousTrip(all, t)
                    val oneLine = line ?: OneLineUi(oneLine(t, events))
                    if (split) {
                        SplitSummary(
                            t,
                            oneLine,
                            previous,
                            viewModel::setMotion,
                            onDetail
                        )
                    } else {
                        FullSummary(
                            t,
                            events,
                            oneLine,
                            previous,
                            viewModel::setMotion,
                            onDetail,
                            onDone
                        )
                    }
                }
            }
        }
    }
}

/** 출발지 → 도착지 (기록에 없으면 날짜 · 출발 시각) */
private fun routeLine(trip: TripEntity) = when {
    trip.origin != null && trip.destination != null -> "${trip.origin} → ${trip.destination}"
    trip.destination != null -> "→ ${trip.destination}"
    else -> "${dateText(trip.startedAtEpoch, "M월 d일")} · ${clockText(trip.startedAtEpoch)} 출발"
}

/**
 * Figma 488:10786: 왼쪽 "오늘 요약"(400) — 도착 칩 · 경로 · 시간 | 휴식 · 점수 링(194) · 지난번 비교 · 오늘 한 줄.
 * 오른쪽(752) — 횟수 타일 128 · 타임라인 330 · 이상징후 171 · 이동기록 보기 100 · 주행 완료 100.
 */
@Composable
private fun FullSummary(
    trip: TripEntity,
    events: List<TripEventEntity>,
    line: OneLineUi,
    previous: TripEntity?,
    onMotion: (MotionRecord) -> Unit,
    onDetail: () -> Unit,
    onDone: () -> Unit
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        MgCard(
            Modifier.width(400.dp).fillMaxHeight(),
            padding = PaddingValues(start = 20.dp, end = 20.dp, top = 57.dp, bottom = 20.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Pill(
                    "도착",
                    height = 52.dp,
                    horizontalPadding = 34.dp,
                    background = Mg.StatusChip,
                    color = Mg.OrangeText,
                    style = Mg.semi(24, 29)
                )
                Spacer(Modifier.height(28.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MgIcon(MgIcons.Flag, Mg.OrangeText, 16.dp)
                    Text(
                        routeLine(trip),
                        style = Mg.semi(24, 29),
                        color = Mg.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    "${koreanDuration(tripDurationMs(trip))} | 휴식 ${trip.restCount ?: 0}번",
                    style = Mg.regular(24, 29),
                    color = Mg.Ink
                )
                if (trip.simulated || trip.status == "INTERRUPTED") {
                    Text(
                        if (trip.simulated) "에뮬레이터 데이터로 기록됐어요" else "비정상 종료된 기록이에요",
                        style = Mg.Caption,
                        color = Mg.OrangeText
                    )
                }
                // 링이 Figma(194)보다 커진 만큼 위아래 간격을 똑같이 나눠 가진다
                Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
                // 100점이 링 안쪽에 들어가는 크기 기준
                ScoreRing(
                    (trip.finalScore ?: 0) / 100f,
                    236.dp,
                    24.dp,
                    description = "컴포트 점수 ${trip.finalScore}"
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${trip.finalScore}", style = Mg.bold(80, 97), color = Mg.Ink)
                        Text(
                            "점",
                            Modifier.padding(start = 2.dp, bottom = 18.dp),
                            style = Mg.medium(24, 29),
                            color = Color(0xFF7C899D)
                        )
                    }
                }
                Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
                // 지난번 기록이 있을 때만 보이지만, 자리는 항상 잡아 둔다 (첫 기록에도 링 · 한 줄 카드 위치가 같게)
                Box(Modifier.height(DELTA_PILL_HEIGHT), contentAlignment = Alignment.Center) {
                    scoreDeltaText(trip, previous)?.let {
                        Pill(it, style = Mg.semi(24, 29), leading = {
                            MgIcon(MgIcons.Gauge, Mg.Ink, 18.dp)
                        })
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            OneLineCard(
                "${trip.petName}의 오늘 한 줄",
                line,
                Modifier.fillMaxWidth().height(312.dp),
                compact = false
            )
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(
                Modifier.fillMaxWidth().height(128.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SummaryStatTile(
                    "급정거",
                    "${trip.rapidBrakingCount ?: 0}",
                    "회",
                    MgIcons.Brake,
                    Modifier.weight(1f).fillMaxHeight(),
                    highlight =
                    (trip.rapidBrakingCount ?: 0) > 0
                )
                SummaryStatTile(
                    "급회전",
                    "${turnCount(trip)}",
                    "회",
                    MgIcons.Turn,
                    Modifier.weight(1f).fillMaxHeight()
                )
                SummaryStatTile(
                    "휴식",
                    "${trip.restCount ?: 0}",
                    "회",
                    MgIcons.Cup,
                    Modifier.weight(1f).fillMaxHeight()
                )
                SummaryStatTile(
                    "창문 알림",
                    "${trip.windowWarningCount ?: 0}",
                    "회",
                    MgIcons.Window,
                    Modifier.weight(1f).fillMaxHeight()
                )
            }
            Spacer(Modifier.height(16.dp))
            MgCard(
                Modifier.fillMaxWidth().height(330.dp),
                padding = PaddingValues(horizontal = 30.dp, vertical = 24.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("흔들림 타임라인", Modifier.weight(1f), style = Mg.semi(24, 29), color = Mg.Ink)
                    Text("막대가 높을수록 많이 흔들렸어요", style = Mg.Body, color = Mg.Muted)
                }
                ShakeTimeline(trip, events, Modifier.fillMaxWidth().weight(1f).padding(top = 10.dp))
            }
            Spacer(Modifier.height(19.dp))
            MotionChipsCard(
                trip,
                onMotion,
                compact = false,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Spacer(Modifier.height(19.dp))
            SecondaryButton(
                "이동기록 보기",
                onDetail,
                Modifier.fillMaxWidth().height(100.dp),
                icon = MgIcons.Doc,
                style = Mg.bold(30, 36)
            )
            Spacer(Modifier.height(15.dp))
            // Figma 488:11007: 남색 "주행 완료" (닫기와 같은 동작)
            Box(
                Modifier.fillMaxWidth().height(
                    100.dp
                ).clip(CircleShape).background(Mg.Ink).clickable(onClick = onDone),
                contentAlignment = Alignment.Center
            ) { Text("주행 완료", style = Mg.semi(30, 36), color = Color.White) }
        }
    }
}

@Composable
private fun SplitSummary(
    trip: TripEntity,
    line: OneLineUi,
    previous: TripEntity?,
    onMotion: (MotionRecord) -> Unit,
    onDetail: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        MgCard(
            Modifier.fillMaxWidth().height(188.dp),
            padding = PaddingValues(horizontal = 30.dp, vertical = 24.dp)
        ) {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                // 링 안쪽(지름 112)에 숫자 · 라벨이 다 들어가게: 라벨이 링 테두리에 겹치지 않도록 작게
                ScoreRing((trip.finalScore ?: 0) / 100f, 140.dp, 14.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${trip.finalScore}", style = Mg.bold(42, 46), color = Mg.Ink)
                        Text("컴포트", style = Mg.Caption, color = Mg.Muted)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("오늘 ${trip.petName}는", style = Mg.Body, color = Mg.Muted)
                    Text(
                        pastComfortLabel(trip.finalScore ?: 0),
                        style = Mg.bold(34, 46),
                        color = Mg.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOfNotNull(
                            koreanDuration(tripDurationMs(trip)),
                            scoreDeltaText(trip, previous)
                        ).joinToString(" · "),
                        style = Mg.regular(22, 30),
                        color = Mg.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().height(112.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryStatTile(
                "급정거",
                "${trip.rapidBrakingCount ?: 0}",
                "회",
                MgIcons.Brake,
                Modifier.weight(1f).fillMaxHeight(),
                highlight =
                (trip.rapidBrakingCount ?: 0) > 0,
                compact = true
            )
            SummaryStatTile(
                "급회전",
                "${turnCount(trip)}",
                "회",
                MgIcons.Turn,
                Modifier.weight(1f).fillMaxHeight(),
                compact = true
            )
            SummaryStatTile(
                "휴식",
                "${trip.restCount ?: 0}",
                "회",
                MgIcons.Cup,
                Modifier.weight(1f).fillMaxHeight(),
                compact = true
            )
            SummaryStatTile(
                "창문",
                "${trip.windowWarningCount ?: 0}",
                "회",
                MgIcons.Window,
                Modifier.weight(1f).fillMaxHeight(),
                compact = true
            )
        }
        Spacer(Modifier.height(12.dp))
        // 높이를 고정하지 않는다: 칩이 두 줄로 넘어가도 아래 안내 문구가 잘리지 않게
        MotionChipsCard(trip, onMotion, compact = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        // 높이를 고정하지 않고 내용만큼 (위아래 25). 남는 자리는 버튼 위로
        OneLineCard("${trip.petName}의 오늘 한 줄", line, Modifier.fillMaxWidth(), compact = true)
        Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
        SecondaryButton(
            "리포트 자세히 보기",
            onDetail,
            Modifier.fillMaxWidth().height(80.dp),
            icon = MgIcons.Doc
        )
    }
}

/** Completion-only Figma tiles: 24px label, 44/34px count, baseline-aligned 24px unit. */
@Composable
private fun SummaryStatTile(
    label: String,
    value: String,
    unit: String?,
    icon: ImageVector,
    modifier: Modifier,
    highlight: Boolean = false,
    compact: Boolean = false
) {
    val foreground = if (highlight) Mg.OrangeText else Mg.Ink
    MgCard(
        modifier,
        padding = PaddingValues(
            horizontal = if (compact) 16.dp else 24.dp,
            vertical = if (compact) 14.dp else 20.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MgIcon(icon, Mg.OrangeText, if (compact) 16.dp else 20.dp)
            Text(
                label,
                style = Mg.regular(24, 32),
                color = Mg.Muted,
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                value,
                Modifier.alignByBaseline(),
                style = Mg.bold(if (compact) 34 else 44, if (compact) 43 else 55),
                color = foreground
            )
            unit?.let {
                Text(it, Modifier.alignByBaseline(), style = Mg.regular(24, 32), color = foreground)
            }
        }
    }
}

/** Figma 488:10831 / 488:13290: AI가 오늘 기록을 보고 쓴 한 줄 (받는 중엔 정리 중 안내, 못 받으면 규칙 문장) */
@Composable
private fun OneLineCard(title: String, line: OneLineUi, modifier: Modifier, compact: Boolean) {
    if (!compact) {
        Column(
            modifier.background(
                Mg.Ink,
                RoundedCornerShape(Mg.Radius)
            ).padding(start = 30.dp, end = 24.dp, top = 28.dp, bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MgIcon(MgIcons.Bulb, Mg.Orange, 20.dp)
                Text(title, style = Mg.medium(24, 29), color = Mg.Orange)
            }
            Spacer(Modifier.height(25.dp))
            Text(
                // 한국어는 어절 단위로 줄바꿈 (낱글자에서 끊기지 않게)
                line.text,
                Modifier.padding(start = 24.dp),
                style = Mg.semi(30, 40).copy(lineBreak = LineBreak.Heading),
                color = if (line.loading) Mg.OnDarkMuted else Color.White,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
        return
    }
    Column(
        modifier.background(
            Mg.Ink,
            RoundedCornerShape(Mg.Radius)
        ).padding(horizontal = 28.dp, vertical = 25.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MgIcon(MgIcons.Bulb, Mg.Orange, 20.dp)
            Text(title, style = Mg.BodyStrong, color = Mg.Orange)
        }
        // 한 줄 내용이 다 보이도록 줄 수를 넉넉히 (서버 응답은 70자 이내), 어절 단위로 줄바꿈
        Text(
            line.text,
            style = Mg.semi(22, 32).copy(lineBreak = LineBreak.Heading),
            color = if (line.loading) Mg.OnDarkMuted else Color.White,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** "지난번보다 2점 올랐어요" 알약 자리 (없어도 높이는 유지) */
private val DELTA_PILL_HEIGHT = 44.dp

private val SummarySigns =
    listOf(MotionSign.YAWN, MotionSign.DROOL, MotionSign.PANT, MotionSign.SHAKE, MotionSign.VOMIT)

/** "이런 모습이 있었나요?" — guardian observation, stored with the trip, never a diagnosis. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MotionChipsCard(
    trip: TripEntity,
    onMotion: (MotionRecord) -> Unit,
    compact: Boolean,
    modifier: Modifier
) {
    val record = MotionRecord.decode(trip.motionSicknessSigns) ?: MotionRecord()
    MgCard(
        modifier,
        padding = if (compact) {
            PaddingValues(
                horizontal = 26.dp,
                vertical = 22.dp
            )
        } else {
            PaddingValues(start = 30.dp, end = 30.dp, top = 25.dp, bottom = 26.dp)
        }
    ) {
        if (compact) {
            Text("${trip.petName}에게 이런 모습이 있었나요?", style = Mg.BodyStrong, color = Mg.Ink)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MgIcon(MgIcons.Check, Mg.OrangeText, 16.dp)
                Text("고른 모습은 다음 안내에 반영해요", style = Mg.Body, color = Mg.OrangeText)
            }
        } else {
            // Figma 488:10925: "이상징후 기록" · 고르면 오른쪽에 "저장됨"
            Row(
                Modifier.fillMaxWidth().height(29.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("이상징후 기록", Modifier.weight(1f), style = Mg.semi(24, 29), color = Mg.Ink)
                if (record.entered) {
                    MgIcon(MgIcons.Check, Mg.OrangeText, 16.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("저장됨", style = Mg.bold(24, 29), color = Mg.OrangeText)
                }
            }
        }
        // compact는 높이를 내용에 맞춘다 (weight 여백을 쓰면 남은 칸을 다 차지해 버림)
        if (compact) Spacer(Modifier.height(16.dp)) else Spacer(Modifier.weight(1f))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 9.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummarySigns.forEach { sign ->
                SelectChip(sign.label, record.count(sign) > 0, compact) {
                    onMotion(record.toggle(sign))
                }
            }
            SelectChip("괜찮았어요", record.fine, compact) { onMotion(record.markFine()) }
        }
        if (compact) {
            Spacer(Modifier.height(14.dp))
            Text("보호자 입력 · 진단이 아니에요", style = Mg.Caption, color = Mg.Faint)
        }
    }
}

@Composable
private fun SelectChip(text: String, selected: Boolean, compact: Boolean, onClick: () -> Unit) {
    Pill(
        text,
        background = if (selected) Mg.Orange else Color.White,
        color = if (selected) Color.White else Mg.Ink,
        border = if (selected) null else Mg.Line,
        height = if (compact) 56.dp else 66.dp,
        // Figma 464:46884: 고름 B24 흰 글자 · 안 고름 R24 · 좌우 18 · 체크와 글자 사이 14
        style = if (compact) {
            Mg.semi(22, 29)
        } else if (selected) {
            Mg.bold(24, 32)
        } else {
            Mg.regular(24, 32)
        },
        leading = if (selected) ({ MgIcon(MgIcons.Check, Color.White, 14.dp) }) else null,
        onClick = onClick,
        horizontalPadding = if (compact) 16.dp else 18.dp,
        iconGap = if (compact) 8.dp else 14.dp
    )
}

/** Penalty bars over trip time with rest bands; heights follow the stored score deltas only. */
@Composable
fun ShakeTimeline(trip: TripEntity, events: List<TripEventEntity>, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val start = trip.startedAtEpoch
    val end = trip.endedAtEpoch ?: (events.maxOfOrNull { it.atEpoch } ?: start)
    val penalties = events.filter {
        it.type == TripEventType.SCORE_PENALTY &&
            (it.scoreDelta ?: 0) < 0
    }
    val rests = restBands(events)
    val labelled = penalties.sortedBy { it.scoreDelta }.take(3).map { it.id }.toSet()
    // Figma 464:46831: 막대 이름 B24 망고 · 시간 R24
    val labelStyle = Mg.bold(24, 32).copy(color = Mg.OrangeText)
    val axisStyle = Mg.regular(24, 32).copy(color = Mg.Muted)
    Canvas(modifier) {
        val axisH = 36.dp.toPx()
        val chartH = size.height - axisH
        val span = (end - start).coerceAtLeast(60_000L).toFloat()
        fun x(t: Long) = ((t - start) / span).coerceIn(0f, 1f) * size.width
        rests.forEach { (a, b) ->
            val left = x(a)
            val w = (x(b) - left).coerceAtLeast(40.dp.toPx())
            drawRoundRect(
                Mg.Track,
                Offset(left, chartH * 0.25f),
                Size(w, chartH * 0.72f),
                CornerRadius(8.dp.toPx())
            )
            val text = measurer.measure("휴식", axisStyle.copy(color = Mg.Ink))
            drawText(text, topLeft = Offset(left + (w - text.size.width) / 2, chartH * 0.55f))
        }
        drawLine(Mg.Line, Offset(0f, chartH), Offset(size.width, chartH), 2.dp.toPx())
        val barW = 8.dp.toPx()
        var labelEnd = Float.NEGATIVE_INFINITY
        penalties.sortedBy { it.atEpoch }.forEach { e ->
            val h = (kotlin.math.abs(e.scoreDelta ?: 0) / 8f).coerceIn(0.15f, 1f) * chartH * 0.62f
            val cx = x(e.atEpoch)
            drawRoundRect(
                Mg.Orange,
                Offset(cx - barW / 2, chartH - h),
                Size(barW, h),
                CornerRadius(3.dp.toPx())
            )
            if (e.id in labelled) {
                val text = measurer.measure(penaltyTitle(e.detail), labelStyle)
                val lx = (cx - text.size.width / 2).coerceIn(0f, size.width - text.size.width)
                // Neighbouring events (e.g. two bumps a minute apart) keep their bars but share one label.
                if (lx >= labelEnd + 8.dp.toPx()) {
                    drawText(
                        text,
                        topLeft = Offset(lx, chartH - h - text.size.height - 6.dp.toPx())
                    )
                    labelEnd = lx + text.size.width
                }
            }
        }
        listOf(start, (start + end) / 2, end).forEachIndexed { i, t ->
            val text = measurer.measure(clockLabel(t), axisStyle)
            val lx = when (i) {
                0 -> 0f
                2 -> size.width - text.size.width
                else -> x(t) - text.size.width / 2
            }
            drawText(text, topLeft = Offset(lx, chartH + 8.dp.toPx()))
        }
        if (penalties.isEmpty()) {
            val text = measurer.measure("기록된 흔들림이 없어요", axisStyle)
            drawText(text, topLeft = Offset((size.width - text.size.width) / 2, chartH / 2))
        }
    }
}

fun clockLabel(epoch: Long): String = dateText(epoch, "H:mm")

/** Rest intervals from recorded starts/ends; a completed duration can recover a missing start. */
fun restBands(events: List<TripEventEntity>): List<Pair<Long, Long>> {
    val out = mutableListOf<Pair<Long, Long>>()
    var open: Long? = null
    events.sortedBy { it.atEpoch }.forEach { event ->
        when (event.type) {
            TripEventType.REST_STARTED -> if (open == null) open = event.atEpoch
            TripEventType.REST_COMPLETED, TripEventType.REST_CANCELLED -> {
                val recordedDuration = if (event.type == TripEventType.REST_COMPLETED) {
                    event.detail?.toLongOrNull()?.takeIf { it > 0 && it <= event.atEpoch }
                } else {
                    null
                }
                val start = open ?: recordedDuration?.let { event.atEpoch - it }
                if (start != null && start < event.atEpoch) out += start to event.atEpoch
                open = null
            }
        }
    }
    return out
}
