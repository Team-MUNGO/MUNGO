package com.example.poco.presentation.session.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poco.R
import com.example.poco.core.driving.CabinTemperatureStatus
import com.example.poco.core.driving.GuidancePriority
import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.RestIntervalPolicy
import com.example.poco.core.driving.RestPhase
import com.example.poco.core.driving.WindowCloseStatus
import com.example.poco.core.driving.temperatureStatus
import com.example.poco.core.driving.valueOrNull
import com.example.poco.data.assistant.AssistPlace
import com.example.poco.data.model.DrivingSessionState
import com.example.poco.data.model.WaypointUiState
import com.example.poco.presentation.design.DrawableIcon
import com.example.poco.presentation.design.IconBadge
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.design.MgCard
import com.example.poco.presentation.design.MgIcon
import com.example.poco.presentation.design.MgIcons
import com.example.poco.presentation.design.PetAvatar
import com.example.poco.presentation.design.Pill
import com.example.poco.presentation.design.PrimaryButton
import com.example.poco.presentation.design.ScoreRing
import com.example.poco.presentation.design.SecondaryButton
import com.example.poco.presentation.design.ValueText
import com.example.poco.presentation.session.AssistantPhase
import com.example.poco.presentation.session.AssistantUiState
import com.example.poco.presentation.session.SessionActions
import com.example.poco.presentation.session.IdleStartCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// ---------- Text helpers (shared by full/split layouts and the trip screens) ----------

fun clockText(epoch: Long): String = SimpleDateFormat("a h:mm", Locale.KOREAN).format(Date(epoch))

fun durationText(ms: Long): String = "%d:%02d".format(Locale.ROOT, ms / 3_600_000, ms / 60_000 % 60)

/** "2시간", "1시간 30분", "25분". */
fun koreanDuration(ms: Long): String {
    val minutes = (ms / 60_000).coerceAtLeast(0)
    return when {
        minutes < 60 -> "${minutes}분"
        minutes % 60 == 0L -> "${minutes / 60}시간"
        else -> "${minutes / 60}시간 ${minutes % 60}분"
    }
}

fun intervalText(minutes: Int) = koreanDuration(minutes * 60_000L)

fun formatTemperature(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(Locale.ROOT, value)

fun seatShort(seat: PetSeat) = when (seat) {
    PetSeat.PASSENGER -> "조수석"
    PetSeat.REAR_LEFT -> "뒷좌석 왼쪽"
    PetSeat.REAR_RIGHT -> "뒷좌석 오른쪽"
}

fun petName(state: DrivingSessionState) = state.settings?.petName ?: "반려견"

/** Status words under the comfort score (Figma: 편안해요 / 조금 더워해요). Temperature wins when out of range. */
fun comfortLabel(state: DrivingSessionState): String {
    if (!state.active) return "주행 준비"
    val temp = state.settings?.let { temperatureStatus(state.temperature.valueOrNull(), it) }
    return when {
        temp == CabinTemperatureStatus.HOT -> "조금 더워해요"
        temp == CabinTemperatureStatus.COLD -> "조금 추워해요"
        state.comfort.score >= 85 -> "편안해요"
        state.comfort.score >= 70 -> "조금 흔들렸어요"
        else -> "많이 힘들어해요"
    }
}

/** Next-rest tile values (value, unit, detail, warning); the same function feeds both layouts. */
fun restTileText(state: DrivingSessionState): RestTileText {
    val rest = state.rest
    val interval = "${intervalText(rest.intervalMinutes)} 간격"
    if (!state.active) return RestTileText("—", null, interval, false)
    val at = rest.nextRestAtEpoch?.let { "예정 ${clockText(it)} · " }.orEmpty()
    if (rest.restDue) return RestTileText("지금", "쉬어요", "$at$interval", true)
    val minutes = ((rest.remainingMs ?: 0) / 60_000).coerceAtLeast(0)
    return if (minutes >= 60) {
        RestTileText("%d:%02d".format(Locale.ROOT, minutes / 60, minutes % 60), null, "$at$interval", false)
    } else {
        RestTileText("$minutes", "분", "$at$interval", false)
    }
}

data class RestTileText(val value: String, val unit: String?, val detail: String, val warning: Boolean)

fun windowWarningBusy(state: DrivingSessionState) =
    state.closeStatus == WindowCloseStatus.REQUESTING || state.closeStatus == WindowCloseStatus.VERIFYING

// ---------- Tiles ----------

enum class TileSize { Full, Split }

@Composable
private fun MgTile(
    size: TileSize,
    label: String,
    value: String,
    unit: String?,
    detail: String?,
    modifier: Modifier,
    warning: Boolean = false,
    iconRes: Int? = null,
    icon: ImageVector? = null,
    extra: (@Composable () -> Unit)? = null
) {
    val full = size == TileSize.Full
    MgCard(
        modifier,
        color = if (warning) Mg.Peach else Mg.Card,
        border = if (warning) Mg.WarnBorder else null,
        padding = if (full) PaddingValues(start = 24.dp, end = 22.dp, top = 22.dp, bottom = 18.dp) else PaddingValues(start = 22.dp, end = 18.dp, top = 20.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(
                icon = icon, res = iconRes, size = if (full) 48.dp else 40.dp,
                background = if (warning) Color.White else Mg.IconBg
            )
            Text(
                label, style = if (warning) Mg.BodyStrong else Mg.Body,
                color = if (warning) Mg.OrangeText else Mg.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            ValueText(value, unit, if (full) Mg.Value else Mg.ValueSplit, if (warning) Mg.OrangeText else Mg.Ink)
            if (extra != null) {
                Spacer(Modifier.width(16.dp))
                Box(Modifier.weight(1f).padding(bottom = 16.dp)) { extra() }
            }
        }
        if (full && detail != null) {
            Text(detail, style = Mg.Body, color = Mg.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Next rest, or — once a verified waypoint is confirmed by Navi — time to that waypoint (Figma 464:46394). */
@Composable
fun RestOrWaypointTile(state: DrivingSessionState, size: TileSize, modifier: Modifier) {
    val added = state.waypoint as? WaypointUiState.Added
    if (state.active && added != null) {
        val eta = added.etaText?.filter(Char::isDigit)?.toIntOrNull()
        var initial by remember(added.name) { mutableStateOf<Int?>(null) }
        LaunchedEffect(added.name, eta) { if (initial == null && eta != null) initial = eta }
        val progress = if (eta != null && initial != null && initial!! > 0) 1f - eta.toFloat() / initial!! else null
        MgTile(
            size, "${added.name}까지", eta?.toString() ?: "—", if (eta != null) "분" else null,
            if (eta != null) "경유지 · 내비게이션 기준" else "경유지 · 도착 시간은 내비게이션에서 확인",
            modifier, warning = true, iconRes = R.drawable.driving_paw,
            extra = if (size == TileSize.Full && progress != null) {
                { ProgressBar(progress.coerceIn(0.05f, 1f)) }
            } else {
                null
            }
        )
        return
    }
    val t = restTileText(state)
    MgTile(size, "다음 휴식까지", t.value, t.unit, t.detail, modifier, t.warning, iconRes = R.drawable.driving_cup)
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFFFFE2C2), CircleShape)) {
        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Mg.Orange, CircleShape))
    }
}

@Composable
fun DriveTimeTile(state: DrivingSessionState, size: TileSize, modifier: Modifier) {
    MgTile(
        size, "주행 시간", if (state.active) durationText(state.elapsedMs) else "—", null,
        state.startedAtEpoch?.let { "출발 ${clockText(it)}" } ?: "주행을 시작해 주세요",
        modifier, iconRes = R.drawable.driving_clock
    )
}

@Composable
fun TemperatureTile(state: DrivingSessionState, size: TileSize, modifier: Modifier) {
    val temp = state.temperature.valueOrNull()
    val status = state.settings?.let { temperatureStatus(temp, it) } ?: CabinTemperatureStatus.UNKNOWN
    val range = state.settings?.let { "${petName(state)} 기준 ${formatTemperature(it.minTemperatureC)}~${formatTemperature(it.maxTemperatureC)}°C" }
    MgTile(
        size, "실내 온도", temp?.let(::formatTemperature) ?: "—", if (temp != null) "°C" else null,
        if (temp == null && state.active) "실내 측정 온도를 확인할 수 없어요" else range,
        modifier, warning = status == CabinTemperatureStatus.HOT || status == CabinTemperatureStatus.COLD,
        iconRes = R.drawable.driving_thermo
    )
}

@Composable
fun WindowTile(state: DrivingSessionState, size: TileSize, modifier: Modifier) {
    val percent = state.windowPercent.valueOrNull()
    val open = percent != null && percent > 20
    val seat = state.settings?.let { "${seatShort(it.seat)} · ${petName(state)}" } ?: "탑승 위치 미설정"
    MgTile(
        size, "창문",
        when {
            percent == null -> if (state.active) "확인 불가" else "—"
            percent == 0.0 -> "닫힘"
            else -> "${percent.roundToInt()}%"
        },
        if (percent != null && percent > 0.0) "열림" else null,
        if (open) "기준 20%보다 많이 열렸어요" else seat,
        modifier, warning = open, iconRes = R.drawable.driving_window
    )
}

// ---------- Comfort ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComfortCardFull(state: DrivingSessionState, modifier: Modifier) {
    val c = state.comfort
    MgCard(modifier, padding = PaddingValues(top = 62.dp, bottom = 50.dp, start = 7.dp, end = 7.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("실시간 컴포트 점수", style = Mg.Body, color = Mg.Muted)
            Spacer(Modifier.weight(1f))
            ScoreRing(
                if (state.active) c.score / 100f else null, 300.dp, 30.dp,
                description = "컴포트 점수 ${if (state.active) c.score else "없음"}"
            ) {
                Text(if (state.active) "${c.score}" else "—", style = Mg.bold(108, 134), color = Mg.Ink)
            }
            Spacer(Modifier.height(36.dp))
            Text(comfortLabel(state), style = Mg.Title, color = Mg.Ink)
            Spacer(Modifier.weight(1f))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Pill("급정거 ${c.brakingCount}", style = Mg.semi(20, 29))
                Pill("급회전 ${c.turnCount + c.repeatedTurnCount}", style = Mg.semi(20, 29))
                Pill("과속방지턱 ${c.bumpCount}", style = Mg.semi(20, 29))
            }
            Spacer(Modifier.weight(1f))
            PetAvatar(112.dp)
        }
    }
}

@Composable
fun ComfortCardSplit(state: DrivingSessionState, modifier: Modifier) {
    val c = state.comfort
    MgCard(modifier, padding = PaddingValues(horizontal = 34.dp, vertical = 32.dp)) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(30.dp)) {
            ScoreRing(if (state.active) c.score / 100f else null, 132.dp, 16.dp, description = "컴포트 점수 ${if (state.active) c.score else "없음"}") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (state.active) "${c.score}" else "—", style = Mg.bold(46, 54), color = Mg.Ink)
                    Text("컴포트", style = Mg.Body, color = Mg.Muted)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${petName(state)} 상태", style = Mg.Body, color = Mg.Muted)
                Text(comfortLabel(state), style = Mg.bold(32, 43), color = Mg.Ink)
                Text(
                    "급정거 ${c.brakingCount} · 급회전 ${c.turnCount + c.repeatedTurnCount} · 과속방지턱 ${c.bumpCount}",
                    style = Mg.Body, color = Mg.Muted, maxLines = 1
                )
            }
        }
    }
}

// ---------- Guidance slot ----------

/**
 * One fixed slot for guidance: rest/place proposal, waypoint confirmation or the Gleo voice card.
 * Higher-priority guidance (arrival, 5 minutes) replaces a proposal in the same slot, so the tiles
 * above never move when cards appear or disappear.
 */
@Composable
fun GleoSlot(
    state: DrivingSessionState,
    ai: AssistantUiState,
    actions: SessionActions,
    compact: Boolean,
    modifier: Modifier,
    restMode: Boolean = false
) {
    if (!state.active) {
        IdleStartCard(actions, compact, modifier)
        return
    }
    val top = state.guidance
    val proposal = state.proposal
    val proposalVisible = !restMode && proposal != null &&
        (top == null || top.priority.rank >= GuidancePriority.REST_PROPOSAL.rank || top.id == proposal.eventId)
    val waypoint = state.waypoint
    val added = waypoint as? WaypointUiState.Added
    // Figma 464:47342: the confirmation card returns to the Gleo card after 5 seconds.
    var showAdded by remember(added?.name) { mutableStateOf(added != null) }
    LaunchedEffect(added?.name) {
        if (added != null) {
            delay(5_000)
            showAdded = false
        }
    }
    when {
        proposalVisible -> ProposalCard(state, actions, compact, modifier)
        !restMode && waypoint is WaypointUiState.Adding -> WaypointAddingCard(waypoint.name, compact, modifier)
        !restMode && added != null && (showAdded || added.cancelling || added.cancelError != null) &&
            (top == null || top.priority.rank > GuidancePriority.WAYPOINT_FIVE_MINUTES.rank) ->
            WaypointAddedCard(added, showAdded, actions, compact, modifier)
        else -> GleoCard(state, ai, actions, compact, restMode, modifier)
    }
}

private fun gleoText(state: DrivingSessionState, restMode: Boolean): String {
    state.guidance?.let { return it.displayText }
    state.aiRestStatus?.let { return it }
    state.gleoResponse?.takeIf { it.isNotBlank() }?.let { return it }
    return when (val w = state.waypoint) {
        is WaypointUiState.Failed -> w.message
        is WaypointUiState.Cancelled -> "${w.name} 경유를 취소했어요."
        else -> if (restMode) "물을 먼저 주고, 10분 정도 같이 걸어 주세요." else "${petName(state)}가 편안하게 가는지 지켜볼게요."
    }
}

@Composable
private fun GleoCard(
    state: DrivingSessionState,
    ai: AssistantUiState,
    actions: SessionActions,
    compact: Boolean,
    restMode: Boolean,
    modifier: Modifier
) {
    val text = gleoText(state, restMode)
    val dark = @Composable { m: Modifier, withHint: Boolean ->
        Column(
            m.background(Mg.Ink, RoundedCornerShape(Mg.Radius)).padding(horizontal = if (compact) 28.dp else 42.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 22.dp else 34.dp)) {
                Box(Modifier.size(44.dp).background(Mg.Orange, CircleShape), contentAlignment = Alignment.Center) {
                    MgIcon(MgIcons.Speaker, Color.White, 22.dp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!(restMode && compact)) {
                        Text(if (compact) "글레오" else "Gleo", style = if (compact) Mg.Body else Mg.Caption, color = Mg.OnDarkMuted)
                    }
                    Text(
                        text, style = if (compact || restMode) Mg.semi(23, 31) else Mg.Quote, color = Color.White,
                        maxLines = if (restMode && compact) 2 else 3, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (withHint) {
                Spacer(Modifier.weight(1f))
                AskHint(state, ai, actions, dark = true, modifier = Modifier.height(60.dp))
            }
        }
    }
    when {
        restMode -> dark(modifier, false)
        compact -> dark(modifier, true)
        else -> Column(modifier) {
            Spacer(Modifier.weight(0.6f))
            dark(Modifier.fillMaxWidth().height(236.dp), false)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AskHint(state, ai, actions, dark = false, modifier = Modifier.weight(1f).height(72.dp))
                CancelWaypointPill(state, actions, Modifier.height(72.dp))
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun CancelWaypointPill(state: DrivingSessionState, actions: SessionActions, modifier: Modifier) {
    val added = state.waypoint as? WaypointUiState.Added ?: return
    SecondaryButton(
        if (added.cancelling) "취소 확인 중" else "경유지 취소", actions.onCancelWaypoint, modifier.widthIn(min = 180.dp),
        enabled = !added.cancelling
    )
}

/** The Figma voice-command hint doubles as the "ask Gleo" button. */
@Composable
private fun AskHint(state: DrivingSessionState, ai: AssistantUiState, actions: SessionActions, dark: Boolean, modifier: Modifier) {
    val ready = ai.configured && ai.consented && (ai.connected || ai.anonymousAuth)
    val text = when {
        ai.error != null && ai.phase == AssistantPhase.IDLE -> ai.error
        ai.phase == AssistantPhase.CONNECTING -> "글레오에 연결하고 있어요"
        ai.phase == AssistantPhase.LISTENING -> if (ai.question.isBlank()) "듣고 있어요…" else "“${ai.question}”"
        ai.phase == AssistantPhase.THINKING -> "답을 찾고 있어요 · “${ai.question}”"
        !ai.configured -> "AI 서버가 설정되지 않았어요"
        !ready -> "눌러서 글레오 AI 연결하기"
        else -> if (state.waypoint is WaypointUiState.Added) "“글레오, 휴게소 어디쯤이야?”" else "“글레오, ${petName(state)} 괜찮아?”"
    }
    val onClick = {
        when {
            ai.phase != AssistantPhase.IDLE -> actions.onCancelAi()
            !ready -> actions.onSetupAi()
            else -> actions.onAsk()
        }
    }
    Row(
        modifier.clip(CircleShape).background(if (dark) Mg.DarkPill else Color.White, CircleShape)
            .clickable(onClick = onClick).padding(horizontal = if (dark) 22.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(if (dark) 28.dp else 40.dp).background(if (dark) Color.White else Mg.Ink, CircleShape),
            contentAlignment = Alignment.Center
        ) { MgIcon(MgIcons.Mic, if (dark) Mg.Ink else Color.White, if (dark) 16.dp else 20.dp) }
        Text(
            text, Modifier.weight(1f, fill = !dark), style = Mg.semi(22, 29),
            color = if (dark) Color.White else Mg.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = if (dark) TextAlign.Start else TextAlign.Center
        )
        if (!dark) Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun ProposalCard(state: DrivingSessionState, actions: SessionActions, compact: Boolean, modifier: Modifier) {
    val proposal = state.proposal ?: return
    val response = proposal.response
    val place: AssistPlace? = response.recommended ?: response.places.firstOrNull()
    val adding = state.waypoint is WaypointUiState.Adding
    val failed = state.waypoint as? WaypointUiState.Failed
    val minutes = state.rest.remainingMs?.takeIf { !state.rest.restDue }?.let { it / 60_000 }
    val name = place?.name ?: "장소 정보 없음"
    val title = when {
        !proposal.automatic -> name
        compact -> minutes?.let { "${it}분 뒤 · $name" } ?: name
        else -> minutes?.let { "${it}분 뒤 · ${name}에서 쉬어 갈까요?" } ?: "${name}에서 쉬어 갈까요?"
    }
    MgCard(modifier, padding = PaddingValues(start = 28.dp, end = 28.dp, top = 26.dp, bottom = 24.dp)) {
        Pill(
            if (proposal.automatic) "반려견 맞춤 제안" else "주변 장소 추천",
            background = Mg.Peach, color = Mg.OrangeText, height = 38.dp,
            leading = { DrawableIcon(R.drawable.driving_paw, Mg.OrangeText, 16.dp) }
        )
        Spacer(Modifier.height(18.dp))
        Text(title, style = Mg.semi(27, 36), color = Mg.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text(response.displayText, style = Mg.Body, color = Mg.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        if (state.ttsSpeaking) SpeakingLine("글레오가 같은 내용을 말하고 있어요")
        // Google Maps attribution must accompany grounded place content.
        Text("장소 정보: Google Maps · 운영 여부는 내비게이션에서 확인", style = Mg.Caption, color = Mg.Faint, maxLines = 1)
        failed?.let { Text(it.message, style = Mg.Caption, color = Mg.OrangeText, maxLines = 2) }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 20.dp else 25.dp)) {
            SecondaryButton(
                "나중에", actions.onDismissProposal, Modifier.weight(1f).height(96.dp), enabled = !adding
            )
            PrimaryButton(
                when {
                    adding -> "경유지 확인 중"
                    place?.canAddWaypoint != true -> "경유지 추가 불가"
                    failed != null -> "다시 시도"
                    else -> "경유지로 추가"
                },
                actions.onAddWaypoint,
                Modifier.weight(if (compact) 1.75f else 2f).height(96.dp),
                // Only Maps-verified coordinates can be submitted; failure/absence keeps it disabled.
                enabled = state.active && !state.windowWarning && place?.canAddWaypoint == true && !adding,
                icon = MgIcons.Route
            )
        }
    }
}

@Composable
private fun SpeakingLine(text: String, color: Color = Mg.Muted) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MgIcon(MgIcons.Speaker, color, 18.dp)
        Text(text, style = Mg.Body, color = color)
    }
}

@Composable
private fun WaypointAddingCard(name: String, compact: Boolean, modifier: Modifier) {
    MgCard(modifier, color = Mg.Peach, padding = PaddingValues(28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconBadge(MgIcons.Route, size = 64.dp, background = Mg.Orange, tint = Color.White)
            Text("${name}을\n경유지로 요청하고 있어요", style = Mg.bold(if (compact) 24 else 26, 34), color = Mg.Ink)
        }
        Spacer(Modifier.height(16.dp))
        Text("내비게이션이 실제로 반영했는지 확인하는 중이에요.", style = Mg.Body, color = Mg.Muted)
    }
}

@Composable
private fun WaypointAddedCard(
    added: WaypointUiState.Added,
    countingDown: Boolean,
    actions: SessionActions,
    compact: Boolean,
    modifier: Modifier
) {
    MgCard(modifier, color = Mg.Peach, padding = PaddingValues(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconBadge(MgIcons.Route, size = 64.dp, background = Mg.Orange, tint = Color.White)
            Text("${added.name}를\n경유지로 넣었어요", style = Mg.bold(if (compact) 24 else 26, 36), color = Mg.Ink)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            added.etaText?.let { "$it 뒤 도착 · 내비게이션 기준" } ?: "도착 시간은 내비게이션에서 확인해 주세요",
            style = Mg.Body, color = Mg.Muted
        )
        Spacer(Modifier.height(10.dp))
        when {
            added.cancelError != null -> Text(added.cancelError, style = Mg.Body, color = Mg.OrangeText, maxLines = 2)
            countingDown -> SpeakingLine("5초 뒤 글레오 안내로 돌아가요")
        }
        Spacer(Modifier.weight(1f))
        SecondaryButton(
            if (added.cancelling) "경유지 취소 확인 중" else "경유지 취소", actions.onCancelWaypoint,
            Modifier.fillMaxWidth().height(96.dp),
            enabled = !added.cancelling
        )
    }
}

// ---------- Resting ----------

private fun restClock(ms: Long): String = "%d:%02d".format(Locale.ROOT, ms / 60_000, ms / 1_000 % 60)

private fun restPlaceName(state: DrivingSessionState): String? = when (val w = state.waypoint) {
    is WaypointUiState.Arrived -> w.name
    is WaypointUiState.Added -> w.name
    else -> null
}

private fun recoveryText(state: DrivingSessionState): String {
    val start = state.rest.scoreAtRestStart
    val now = state.comfort.score
    return if (start != null && now > start) "컴포트 $start → $now 회복 중" else "컴포트 $now · 쉬면서 회복해요"
}

@Composable
fun RestSummaryFull(state: DrivingSessionState, modifier: Modifier) {
    val rest = state.rest
    val recommendedMs = RestIntervalPolicy.RECOMMENDED_REST_MINUTES * 60_000L
    val left = recommendedMs - rest.currentRestMs
    MgCard(modifier, padding = PaddingValues(start = 32.dp, end = 32.dp, top = 30.dp, bottom = 34.dp)) {
        Text("${restPlaceName(state) ?: "잠시"}에서\n쉬어 가는 중", style = Mg.semi(27, 44), color = Mg.Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "추천 ${RestIntervalPolicy.RECOMMENDED_REST_MINUTES}분 · " +
                if (left > 0) "${(left + 59_999) / 60_000}분 남았어요" else "충분히 쉬었어요",
            style = Mg.Body, color = Mg.Muted
        )
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            ScoreRing(rest.currentRestMs.toFloat() / recommendedMs, 256.dp, 28.dp, description = "휴식 경과 ${restClock(rest.currentRestMs)}") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(restClock(rest.currentRestMs), style = Mg.bold(58, 70), color = Mg.Ink)
                    Text("쉬는 중", style = Mg.Body, color = Mg.Muted)
                }
            }
            Spacer(Modifier.height(42.dp))
            Pill(recoveryText(state), leading = { MgIcon(MgIcons.Gauge, Mg.Ink, 18.dp) })
            Spacer(Modifier.weight(1f))
            PetAvatar(140.dp)
            Spacer(Modifier.height(26.dp))
            Text("휴식 3분마다 점수가 1점씩 올라요", style = Mg.Body, color = Mg.Muted)
        }
    }
}

@Composable
fun RestTimerSplit(state: DrivingSessionState, modifier: Modifier) {
    val rest = state.rest
    val recommendedMs = RestIntervalPolicy.RECOMMENDED_REST_MINUTES * 60_000L
    MgCard(modifier, padding = PaddingValues(horizontal = 34.dp, vertical = 32.dp)) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(30.dp)) {
            ScoreRing(rest.currentRestMs.toFloat() / recommendedMs, 132.dp, 16.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(restClock(rest.currentRestMs), style = Mg.bold(30, 38), color = Mg.Ink)
                    Text("쉬는 중", style = Mg.Caption, color = Mg.Muted)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(restPlaceName(state) ?: "휴식 중", style = Mg.Body, color = Mg.Muted, maxLines = 1)
                Text("쉬어 가는 중", style = Mg.bold(32, 43), color = Mg.Ink)
                Text("추천 ${RestIntervalPolicy.RECOMMENDED_REST_MINUTES}분 · ${recoveryText(state)}", style = Mg.Body, color = Mg.Muted, maxLines = 1)
            }
        }
    }
}

private data class RestTask(val title: String, val hint: String, val done: String)

private val RestTasks = listOf(
    RestTask("물 먼저 주기", "물그릇을 꺼내 주세요", "마셨어요"),
    RestTask("10분 산책 · 배변", "주차장 안쪽 안전한 곳에서", "다녀왔어요"),
    RestTask("리드줄 다시 연결", "출발하기 전에", "연결했어요")
)

/** Guardian checklist (Figma "쉬는 동안 할 일"); local to this rest, not part of the trip record. */
@Composable
fun RestChecklist(state: DrivingSessionState, compact: Boolean, modifier: Modifier) {
    var checked by rememberSaveable(state.rest.restStartedAtEpoch) { mutableStateOf(listOf(false, false, false)) }
    MgCard(modifier, padding = PaddingValues(start = 28.dp, end = 28.dp, top = 22.dp, bottom = 18.dp)) {
        Text("쉬는 동안 이렇게 해 주세요", style = Mg.BodyStrong, color = Mg.Ink)
        Spacer(Modifier.height(if (compact) 12.dp else 16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            RestTasks.forEachIndexed { i, task ->
                val done = checked[i]
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { checked = checked.toMutableList().also { it[i] = !done } },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(
                        Modifier.size(28.dp).background(if (done) Mg.Orange else Color.White, RoundedCornerShape(7.dp))
                            .then(if (done) Modifier else Modifier.border(2.dp, Mg.BarGrey, RoundedCornerShape(7.dp))),
                        contentAlignment = Alignment.Center
                    ) { if (done) MgIcon(MgIcons.Check, Color.White, 18.dp) }
                    Column {
                        Text(
                            task.title, style = Mg.BodyStrong.copy(textDecoration = if (done) TextDecoration.LineThrough else null),
                            color = if (done) Mg.Muted else Mg.Ink
                        )
                        Text(if (done) task.done else task.hint, style = Mg.Body, color = Mg.Muted)
                    }
                }
            }
        }
    }
}

/** The verified rest place (Navi-arrived waypoint); amenities are never invented. */
@Composable
fun RestPlaceCard(state: DrivingSessionState, compact: Boolean, modifier: Modifier) {
    val name = restPlaceName(state)
    MgCard(modifier, padding = PaddingValues(horizontal = 26.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            if (name != null) IconBadge(res = R.drawable.driving_paw, size = 60.dp) else IconBadge(res = R.drawable.driving_cup, size = 60.dp)
            Column {
                Text(name ?: "휴식 기록 중", style = Mg.BodyStrong, color = Mg.Ink, maxLines = 1)
                Text(
                    if (name != null) "Google Maps에서 확인한 장소예요" else "출발하면 이번 휴식이 이동 기록에 저장돼요",
                    style = Mg.Body, color = Mg.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------- Window warning ----------

private fun windowTitle(state: DrivingSessionState): String {
    val percent = state.windowPercent.valueOrNull()?.roundToInt()?.let { "$it%" } ?: "많이"
    return "${petName(state)} 쪽 창문이 $percent 열려 있어요"
}

private fun windowSpeech(state: DrivingSessionState) = when {
    state.ttsSpeaking -> "글레오가 같은 내용을 말하고 있어요"
    state.ttsError != null -> "음성 안내를 재생하지 못했어요"
    else -> "반려견의 안전을 위해 창문을 닫아 주세요"
}

private fun closeLabel(state: DrivingSessionState) = when (state.closeStatus) {
    WindowCloseStatus.REQUESTING -> "닫기 요청 중"
    WindowCloseStatus.VERIFYING -> "닫힘 확인 중"
    WindowCloseStatus.FAILED -> "다시 닫기"
    WindowCloseStatus.IDLE -> "창문 닫기"
}

@Composable
private fun WindowErrors(state: DrivingSessionState) {
    state.closeError?.let { Text(it, style = Mg.BodyStrong, color = Mg.OrangeText, textAlign = TextAlign.Center) }
    if (!state.canCloseWindow) {
        Text("이 창문은 앱에서 닫을 수 없어요. 차량의 창문 스위치를 이용해 주세요.", style = Mg.Body, color = Mg.OrangeText, textAlign = TextAlign.Center)
    }
}

/** Full layout popup (Figma 464:46215): 760 wide, icon, two lines, Gleo line, 괜찮아요 / 창문 닫기. */
@Composable
fun WindowWarningPopup(state: DrivingSessionState, onDismiss: () -> Unit, onClose: () -> Unit) {
    val busy = windowWarningBusy(state)
    Column(
        Modifier.width(760.dp).background(Color.White, RoundedCornerShape(32.dp)).padding(horizontal = 40.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(res = R.drawable.driving_window, size = 92.dp, background = Mg.Orange, tint = Mg.Ink)
        Spacer(Modifier.height(26.dp))
        Text(windowTitle(state), style = Mg.bold(30, 38), color = Mg.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Text("기준 20%보다 많이 열렸어요.\n얼굴을 내밀면 눈을 다칠 수 있어요.", style = Mg.Body.copy(lineHeight = Mg.Body.fontSize * 1.55f), color = Mg.Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        SpeakingLine(windowSpeech(state))
        WindowErrors(state)
        Spacer(Modifier.height(36.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SecondaryButton("괜찮아요", onDismiss, Modifier.weight(240f).height(96.dp), enabled = !busy, style = Mg.semi(24, 31))
            PrimaryButton(closeLabel(state), onClose, Modifier.weight(424f).height(96.dp), enabled = state.canCloseWindow && !busy, icon = null)
        }
    }
}

/** Split layout (Figma 464:47750): the whole mungo panel turns into the warning. */
@Composable
fun WindowWarningPanel(state: DrivingSessionState, onDismiss: () -> Unit, onClose: () -> Unit, modifier: Modifier) {
    val busy = windowWarningBusy(state)
    MgCard(modifier, color = Mg.Peach, border = Mg.WarnBorder, padding = PaddingValues(horizontal = 30.dp, vertical = 30.dp)) {
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(22.dp))
            IconBadge(res = R.drawable.driving_window, size = 100.dp, background = Mg.Orange, tint = Mg.Ink)
            Spacer(Modifier.height(26.dp))
            Text(windowTitle(state).replace("창문이 ", "창문이\n"), style = Mg.bold(32, 48), color = Mg.Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Text("기준 20%보다 많이 열렸어요.\n얼굴을 내밀면 눈을 다칠 수 있어요.", style = Mg.Body, color = Mg.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            SpeakingLine(windowSpeech(state))
            WindowErrors(state)
            Spacer(Modifier.height(30.dp))
            Row(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ScoreRing(state.comfort.score / 100f, 60.dp, 8.dp) {
                    Text("${state.comfort.score}", style = Mg.bold(20, 29), color = Mg.Ink)
                }
                Column {
                    Text("${petName(state)} 상태", style = Mg.Body, color = Mg.Muted)
                    Text(
                        comfortLabel(state) + (state.temperature.valueOrNull()?.let { " · 실내 ${formatTemperature(it)}°C" } ?: ""),
                        style = Mg.BodyStrong, color = Mg.Ink
                    )
                }
            }
        }
        PrimaryButton(closeLabel(state), onClose, Modifier.fillMaxWidth().height(100.dp), enabled = state.canCloseWindow && !busy)
        Spacer(Modifier.height(16.dp))
        SecondaryButton("괜찮아요", onDismiss, Modifier.fillMaxWidth().height(100.dp), enabled = !busy, style = Mg.semi(24, 31))
    }
}
