package com.example.poco.presentation.session.component

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.poco.R
import com.example.poco.core.driving.CabinTemperatureStatus
import com.example.poco.core.driving.GuidancePriority
import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.RestIntervalPolicy
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
import com.example.poco.presentation.record.subjectParticle
import com.example.poco.presentation.session.AssistantPhase
import com.example.poco.presentation.session.AssistantUiState
import com.example.poco.presentation.session.IdleStartCard
import com.example.poco.presentation.session.SessionActions
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
        RestTileText(
            "%d:%02d".format(Locale.ROOT, minutes / 60, minutes % 60),
            null,
            "$at$interval",
            false
        )
    } else {
        RestTileText("$minutes", "분", "$at$interval", false)
    }
}

data class RestTileText(
    val value: String,
    val unit: String?,
    val detail: String,
    val warning: Boolean
)

fun windowWarningBusy(state: DrivingSessionState) =
    state.closeStatus == WindowCloseStatus.REQUESTING ||
        state.closeStatus == WindowCloseStatus.VERIFYING

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
    // Figma 488:12128 / 488:12312: 기준을 벗어나면 면 · 3px 테두리 · 라벨 · 값 · 단위가 모두 망고 톤 (단위도 Bold)
    MgCard(
        modifier,
        color = if (warning) Mg.AlertBg else Mg.Card,
        border = if (warning) Mg.AlertStroke else null,
        borderWidth = Mg.AlertStrokeWidth,
        padding = if (full) {
            PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = 22.dp,
                bottom = 34.dp
            )
        } else {
            PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp)
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (full) 12.dp else 10.dp)
        ) {
            IconBadge(
                icon = icon,
                res = iconRes,
                size = if (full) 48.dp else 44.dp,
                background = if (warning) Color.White else Mg.IconBg
            )
            Text(
                label,
                style = if (warning) Mg.BodyStrong else Mg.Body,
                color = if (warning) {
                    Mg.OrangeText
                } else if (full) {
                    TileLabel
                } else {
                    Mg.Muted
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.Bottom) {
            ValueText(
                value,
                unit,
                if (full) Mg.Value else Mg.ValueSplit,
                if (warning) Mg.OrangeText else Mg.Ink,
                unitStyle = if (warning) {
                    Mg.BodyStrong
                } else if (full) {
                    Mg.medium(24, 32)
                } else {
                    Mg.Unit
                }
            )
            if (extra != null) {
                Spacer(Modifier.width(16.dp))
                Box(Modifier.weight(1f).padding(bottom = 16.dp)) { extra() }
            }
        }
        // 전체 화면 타일엔 보조 문구가 없다 (Figma 488:10266). detail은 접근성 설명으로만 남긴다
    }
}

private val TileLabel = Color(0xFF4F5865)

/** Next rest,
or — once a verified waypoint is confirmed by Navi — time to that waypoint (Figma 464:46394). */
@Composable
fun RestOrWaypointTile(state: DrivingSessionState, size: TileSize, modifier: Modifier) {
    val added = state.waypoint as? WaypointUiState.Added
    if (state.active && added != null) {
        val eta = added.etaText?.filter(Char::isDigit)?.toIntOrNull()
        MgTile(
            size,
            "${added.name}까지",
            eta?.toString() ?: "—",
            if (eta != null) "분" else null,
            if (eta != null) "경유지 · 내비게이션 기준" else "경유지 · 도착 시간은 내비게이션에서 확인",
            modifier,
            warning = true,
            iconRes = R.drawable.driving_paw
            // Figma 488:9920: 경유지 타일은 이름 · 남은 분만 (진행 막대 없음)
        )
        return
    }
    val t = restTileText(state)
    MgTile(
        size,
        "다음 휴식까지",
        t.value,
        t.unit,
        t.detail,
        modifier,
        t.warning,
        iconRes = R.drawable.driving_cup
    )
}

@Composable
fun DriveTimeTile(state: DrivingSessionState, size: TileSize, modifier: Modifier) {
    MgTile(
        size,
        "주행 시간",
        if (state.active) durationText(state.elapsedMs) else "—",
        null,
        state.startedAtEpoch?.let { "출발 ${clockText(it)}" } ?: "주행을 시작해 주세요",
        modifier,
        iconRes = R.drawable.driving_clock
    )
}

@Composable
fun TemperatureTile(state: DrivingSessionState, size: TileSize, modifier: Modifier) {
    val temp = state.temperature.valueOrNull()
    val status =
        state.settings?.let { temperatureStatus(temp, it) } ?: CabinTemperatureStatus.UNKNOWN
    val range = state.settings?.let {
        "${petName(
            state
        )} 기준 ${formatTemperature(it.minTemperatureC)}~${formatTemperature(it.maxTemperatureC)}°C"
    }
    MgTile(
        size,
        "실내 온도",
        temp?.let(::formatTemperature) ?: "—",
        if (temp != null) "°C" else null,
        if (temp == null && state.active) "실내 측정 온도를 확인할 수 없어요" else range,
        modifier,
        warning =
        status == CabinTemperatureStatus.HOT || status == CabinTemperatureStatus.COLD,
        iconRes = R.drawable.driving_thermo
    )
}

@Composable
fun WindowTile(state: DrivingSessionState, size: TileSize, modifier: Modifier) {
    val percent = state.windowPercent.valueOrNull()
    val open = percent != null && percent > 20
    val seat = state.settings?.let { "${seatShort(it.seat)} · ${petName(state)}" } ?: "탑승 위치 미설정"
    MgTile(
        size,
        "창문",
        when {
            percent == null -> if (state.active) "확인 불가" else "—"
            percent == 0.0 -> "닫힘"
            else -> "${percent.roundToInt()}%"
        },
        if (percent != null && percent > 0.0) "열림" else null,
        if (open) "기준 20%보다 많이 열렸어요" else seat,
        modifier,
        warning = open,
        iconRes = R.drawable.driving_window
    )
}

// ---------- Comfort ----------

/**
 * 말풍선 문구 (Figma 488:10411: 편안해요! / 조금 더워요). 온도가 기준을 벗어나면 온도가 먼저.
 * TODO: 흔들림 안내 기준은 기획 확정 후 교체 (지금은 점수 구간만 본다)
 */
fun comfortBubble(state: DrivingSessionState): String {
    if (!state.active) return "주행 준비"
    val temp = state.settings?.let { temperatureStatus(state.temperature.valueOrNull(), it) }
    return when {
        temp == CabinTemperatureStatus.HOT -> "조금 더워요"
        temp == CabinTemperatureStatus.COLD -> "조금 추워요"
        state.comfort.score >= 85 -> "편안해요!"
        state.comfort.score >= 70 -> "조금 흔들렸어요"
        else -> "많이 힘들어해요"
    }
}

/** Figma 488:10298 (420×898): 주행중 칩 · 점수 링 · 반려견 + 말풍선. 횟수 칩은 말풍선 안내로 대체됐다 */
@Composable
fun ComfortCardFull(state: DrivingSessionState, modifier: Modifier) {
    val c = state.comfort
    MgCard(
        modifier,
        padding = PaddingValues(top = 59.dp, bottom = 77.dp, start = 7.dp, end = 7.dp)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            CardStatusChip(if (state.active) "주행중" else "대기 중")
            Spacer(Modifier.height(39.dp))
            Text("실시간 컴포트 점수", style = Mg.semi(24, 29), color = Mg.Ink)
            // 링이 Figma(300)보다 커진 만큼 위아래 간격을 똑같이 나눠 가진다 (Figma는 59 · 59)
            Spacer(Modifier.weight(1f))
            ScoreRing(
                // 100점(세 자리 + '점')이 링 안쪽에 들어가는 크기 기준. 글자 크기는 점수와 상관없이 같다
                if (state.active) c.score / 100f else null,
                340.dp,
                28.dp,
                description = "컴포트 점수 ${if (state.active) c.score else "없음"}"
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (state.active) "${c.score}" else "—",
                        style = Mg.bold(112, 138),
                        color = Mg.Ink
                    )
                    if (state.active) {
                        Text(
                            "점",
                            Modifier.padding(start = 4.dp, bottom = 24.dp),
                            style = Mg.medium(27, 33),
                            color = ScoreUnit
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            PetAvatar(118.dp)
            Spacer(Modifier.height(18.dp))
            SpeechBubble(comfortBubble(state))
        }
    }
}

private val ScoreUnit = Color(0xFF7C899D)

/** Figma 488:10409 · 488:10760: 왼쪽 카드 맨 위 상태 칩 (주행중 · 휴식중) */
@Composable
private fun CardStatusChip(text: String) {
    Pill(
        text,
        height = 52.dp,
        horizontalPadding = 24.dp,
        background = Mg.StatusChip,
        color = Mg.OrangeText,
        style = Mg.semi(24, 29)
    )
}

/** 위로 꼬리가 달린 남색 말풍선 (꼬리 20×17, 본체 최소 222×71 · 모서리 8) */
@Composable
internal fun SpeechBubble(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(20.dp, 17.dp)) {
            drawPath(
                Path().apply {
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                },
                Mg.Bubble
            )
        }
        Box(
            Modifier.widthIn(
                min = 222.dp
            ).height(71.dp).background(Mg.Bubble, RoundedCornerShape(8.dp))
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = Mg.bold(34, 44), color = Color.White, maxLines = 1)
        }
    }
}

@Composable
fun ComfortCardSplit(state: DrivingSessionState, modifier: Modifier) {
    val c = state.comfort
    MgCard(modifier, padding = PaddingValues(horizontal = 30.dp, vertical = 16.dp)) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            // 100 · 컴포트가 링 안쪽에 들어가는 크기 기준
            ScoreRing(
                if (state.active) c.score / 100f else null,
                160.dp,
                15.dp,
                description = "컴포트 점수 ${if (state.active) c.score else "없음"}"
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (state.active) "${c.score}" else "—",
                        style = Mg.bold(50, 61),
                        color = Mg.Ink
                    )
                    Text("컴포트", style = Mg.Body, color = Mg.Muted)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${petName(state)} 상태", style = Mg.Body, color = Mg.Muted)
                Text(comfortLabel(state), style = Mg.bold(36, 47), color = Mg.Ink)
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
    val proposalVisible = !restMode &&
        proposal != null &&
        (
            top == null ||
                top.priority.rank >= GuidancePriority.REST_PROPOSAL.rank ||
                top.id == proposal.eventId
            )
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
        proposalVisible && ai.phase == AssistantPhase.IDLE -> ProposalCard(
            state,
            actions,
            compact,
            modifier
        )
        !restMode && waypoint is WaypointUiState.Adding -> WaypointAddingCard(
            waypoint.name,
            compact,
            modifier
        )
        !restMode &&
            added != null &&
            (showAdded || added.cancelling || added.cancelError != null) &&
            (top == null || top.priority.rank > GuidancePriority.WAYPOINT_FIVE_MINUTES.rank) ->
            WaypointAddedCard(added, showAdded, actions, compact, modifier)
        else -> GleoCard(state, ai, actions, compact, restMode, modifier)
    }
}

/**
 * 아직 말한 안내가 없을 때 음성 카드에 보여 줄 문장 (Figma 488:10406: "현댕이가 편안하게 가고 있어요.").
 * 지금 측정값(온도 · 점수 · 다음 휴식)으로만 만든다.
 */
internal fun idleGuidance(state: DrivingSessionState, restMode: Boolean): String {
    val pet = petName(state)
    val subject = "$pet${subjectParticle(pet)}"
    if (restMode) return "10분 정도 같이 걷는 걸 추천해요.\n출발 전에 리드줄을 다시 연결해 주세요."
    val temp = state.settings?.let { temperatureStatus(state.temperature.valueOrNull(), it) }
    // 휴식 제안은 휴식 20분 전에 온다 (RestRecommendationCoordinator)
    val toProposal = state.rest.remainingMs?.let { it / 60_000 - 20 }
    val restLine = when {
        state.rest.restDue -> "지금 쉬어 갈 곳을 찾아볼게요."
        toProposal != null && toProposal > 0 -> "${toProposal}분 뒤 쉴 곳을 알려 드릴게요."
        else -> "곧 쉴 곳을 알려 드릴게요."
    }
    return when {
        temp == CabinTemperatureStatus.HOT -> "$subject 조금 더워해요.\n실내 온도를 낮춰 주세요."
        temp == CabinTemperatureStatus.COLD -> "$subject 조금 추워해요.\n실내 온도를 높여 주세요."
        state.comfort.score >= 85 -> "$subject 편안하게 가고 있어요.\n$restLine"
        state.comfort.score >= 70 -> "$subject 조금 흔들렸어요.\n천천히 부드럽게 가 볼까요?"
        else -> "$subject 많이 힘들어해요.\n안전한 곳에서 잠시 쉬어 가요."
    }
}

private fun quoted(text: String) = if (text.startsWith("“")) text else "“$text”"

@Composable
private fun GleoCard(
    state: DrivingSessionState,
    ai: AssistantUiState,
    actions: SessionActions,
    compact: Boolean,
    restMode: Boolean,
    modifier: Modifier
) {
    val recognizing = ai.phase == AssistantPhase.LISTENING || ai.phase == AssistantPhase.THINKING
    val spokenText = if (state.ttsSpeaking) {
        state.guidance?.displayText ?: state.gleoResponse
    } else {
        null
    }
    val heard = if (recognizing) {
        null
    } else {
        ai.answerText?.takeIf { it.isNotBlank() }
            ?: spokenText?.takeIf { it.isNotBlank() }
    }
    if (!compact) {
        // Figma 488:10399 / 488:10688: 음성 카드는 늘 자리를 지킨다. 말한 안내가 없으면 지금 상태로 만든 문장
        val text = heard ?: idleGuidance(state, restMode)
        Column(modifier) {
            if (!restMode) Spacer(Modifier.height(21.dp))
            VoiceCard(if (recognizing) null else text, Modifier.fillMaxWidth().weight(1f))
            if (!restMode) {
                // Figma 488:10392: 음성 명령 안내 줄 (누르면 질문)
                Spacer(Modifier.height(41.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AskHint(
                        state,
                        ai,
                        actions,
                        dark = false,
                        modifier = Modifier.weight(1f).height(72.dp)
                    )
                    CancelWaypointPill(state, actions, Modifier.height(72.dp))
                }
                Spacer(Modifier.height(18.dp))
            }
        }
        return
    }
    // 분할 화면도 음성 카드는 늘 자리를 지킨다 (Figma 488:12146 · 488:13084). 말한 안내가 없으면 지금 상태로 만든 문장
    val text = if (recognizing) null else heard ?: idleGuidance(state, restMode)
    if (restMode) {
        // Figma 488:13084: 578 × 110, 라운드 24 · 주황 스피커 44 · B24 두 줄
        Row(
            modifier.background(Mg.Ink, RoundedCornerShape(24.dp)).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).background(Mg.Orange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MgIcon(MgIcons.Speaker, Color.White, 20.dp)
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text?.let(::quoted) ?: "듣고 있어요…",
                Modifier.weight(1f),
                style = Mg.bold(24, 30).copy(lineBreak = LineBreak.Heading),
                color = if (text == null) Mg.OnDarkMuted else Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        return
    }
    // Figma 488:12146: 578 × 354, 라운드 26 · "mungo" R24 · 스피커 52 · B25 문장 · 카드 안 음성 질문 알약
    Column(
        modifier.background(Mg.Ink, RoundedCornerShape(26.dp))
            .padding(start = 26.dp, end = 26.dp, top = 56.dp, bottom = 32.dp)
    ) {
        Text(
            "mungo",
            Modifier.padding(start = 70.dp),
            style = Mg.regular(24, 30),
            color = Mg.OnDarkMuted
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(52.dp).background(Mg.Orange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MgIcon(MgIcons.Speaker, Color.White, 24.dp)
            }
            Spacer(Modifier.width(18.dp))
            Text(
                text?.let(::quoted) ?: "듣고 있어요…",
                Modifier.weight(1f),
                style = Mg.bold(25, 38).copy(lineBreak = LineBreak.Heading),
                color = if (text == null) Mg.OnDarkMuted else Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.padding(start = 2.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AskHint(
                state,
                ai,
                actions,
                dark = true,
                modifier = Modifier.weight(1f, fill = false).height(60.dp)
            )
            CancelWaypointPill(state, actions, Modifier.height(60.dp))
        }
    }
}

/** Figma 488:10399: 남색 카드 · 왼쪽에 "mungo" + 주황 스피커, 가운데 정렬 SB35 따옴표 문장 */
@Composable
private fun VoiceCard(text: String?, modifier: Modifier) {
    Row(
        modifier.background(
            Mg.Ink,
            RoundedCornerShape(Mg.Radius)
        ).padding(start = 48.dp, end = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("mungo", style = Mg.regular(17, 20), color = Mg.OnDarkMuted)
            Box(
                Modifier.size(51.dp).background(Mg.Orange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MgIcon(MgIcons.Speaker, Color.White, 24.dp)
            }
        }
        Text(
            text?.let(::quoted) ?: "듣고 있어요…",
            Modifier.weight(1f).padding(start = 24.dp),
            style = Mg.semi(35, 53).copy(lineBreak = LineBreak.Heading),
            color = if (text ==
                null
            ) {
                Mg.OnDarkMuted
            } else {
                Color.White
            },
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CancelWaypointPill(
    state: DrivingSessionState,
    actions: SessionActions,
    modifier: Modifier
) {
    val added = state.waypoint as? WaypointUiState.Added ?: return
    SecondaryButton(
        if (added.cancelling) "취소 확인 중" else "경유지 취소",
        actions.onCancelWaypoint,
        modifier.widthIn(min = 180.dp),
        enabled = !added.cancelling
    )
}

/** The Figma voice-command hint doubles as the "ask Gleo" button. */
@Composable
private fun AskHint(
    state: DrivingSessionState,
    ai: AssistantUiState,
    actions: SessionActions,
    dark: Boolean,
    modifier: Modifier
) {
    val ready = ai.configured && ai.consented && (ai.connected || ai.anonymousAuth)
    val text = when {
        ai.restNeedsLocation && ai.phase == AssistantPhase.IDLE -> "위치 권한 허용하고 휴식 장소 찾기"
        ai.error != null && ai.phase == AssistantPhase.IDLE -> ai.error
        ai.phase == AssistantPhase.CONNECTING -> "mungo에 연결하고 있어요"
        ai.phase == AssistantPhase.LISTENING ->
            if (ai.question.isBlank()) "듣고 있어요…" else "“${ai.question}”"
        ai.phase == AssistantPhase.THINKING -> "“${ai.question}”"
        ai.question.isNotBlank() -> "“${ai.question}”"
        !ai.configured -> "AI 서버가 설정되지 않았어요"
        !ready -> "눌러서 mungo AI 연결하기"
        else -> if (state.waypoint is WaypointUiState.Added) {
            "“mungo, 휴게소 어디쯤이야?”"
        } else {
            "“mungo, ${petName(
                state
            )} 괜찮아?”"
        }
    }
    val onClick = {
        when {
            ai.phase != AssistantPhase.IDLE -> actions.onCancelAi()
            ai.restNeedsLocation -> actions.onAllowLocation()
            !ready -> actions.onSetupAi()
            else -> actions.onAsk()
        }
    }
    Row(
        modifier.clip(
            CircleShape
        ).background(if (dark) Color(0xFF2C3440) else Color.White, CircleShape)
            .clickable(
                onClick = onClick
            ).padding(start = if (dark) 14.dp else 12.dp, end = if (dark) 26.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (dark) 10.dp else 14.dp)
    ) {
        Box(
            Modifier.size(
                if (dark) 32.dp else 48.dp
            ).background(if (dark) Color.White else Mg.Bubble, CircleShape),
            contentAlignment = Alignment.Center
        ) { MgIcon(MgIcons.Mic, if (dark) Mg.Ink else Color.White, if (dark) 18.dp else 20.dp) }
        Text(
            text,
            Modifier.weight(1f, fill = !dark),
            style = if (dark) Mg.regular(24, 32) else Mg.medium(24, 32),
            color = if (dark) Color(0xFFE5E8EB) else Mg.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (dark) TextAlign.Start else TextAlign.Center
        )
        if (!dark) Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun ProposalCard(
    state: DrivingSessionState,
    actions: SessionActions,
    compact: Boolean,
    modifier: Modifier
) {
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
    MgCard(
        modifier,
        padding = PaddingValues(start = 28.dp, end = 28.dp, top = 26.dp, bottom = 24.dp)
    ) {
        Pill(
            if (proposal.automatic) "반려견 맞춤 제안" else "주변 장소 추천",
            background = Mg.Peach,
            color = Mg.OrangeText,
            height = 38.dp,
            leading = { DrawableIcon(R.drawable.driving_paw, Mg.OrangeText, 16.dp) }
        )
        Spacer(Modifier.height(18.dp))
        Text(
            title,
            style = Mg.semi(27, 36),
            color = Mg.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            response.displayText,
            style = Mg.Body,
            color = Mg.Muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(10.dp))
        if (state.ttsSpeaking) SpeakingLine("mungo가 같은 내용을 말하고 있어요")
        // Google Maps attribution must accompany grounded place content.
        Text(
            "장소 정보: Google Maps · 운영 여부는 내비게이션에서 확인",
            style = Mg.Caption,
            color = Mg.Faint,
            maxLines = 1
        )
        failed?.let { Text(it.message, style = Mg.Caption, color = Mg.OrangeText, maxLines = 2) }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 20.dp else 25.dp)) {
            SecondaryButton(
                "나중에",
                actions.onDismissProposal,
                Modifier.weight(1f).height(96.dp),
                enabled = !adding
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
                enabled =
                state.active && !state.windowWarning && place?.canAddWaypoint == true && !adding,
                icon = MgIcons.Route
            )
        }
    }
}

@Composable
private fun SpeakingLine(text: String, color: Color = Mg.Muted) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MgIcon(MgIcons.Speaker, color, 18.dp)
        Text(text, style = Mg.Body, color = color)
    }
}

@Composable
private fun WaypointAddingCard(name: String, compact: Boolean, modifier: Modifier) {
    MgCard(modifier, color = Mg.Peach, padding = PaddingValues(28.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconBadge(MgIcons.Route, size = 64.dp, background = Mg.Orange, tint = Color.White)
            Text(
                "${name}을\n경유지로 요청하고 있어요",
                style = Mg.bold(if (compact) 24 else 26, 34),
                color = Mg.Ink
            )
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
    MgCard(
        modifier,
        color = Mg.Peach,
        padding = PaddingValues(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconBadge(MgIcons.Route, size = 64.dp, background = Mg.Orange, tint = Color.White)
            Text(
                "${added.name}를\n경유지로 넣었어요",
                style = Mg.bold(if (compact) 24 else 26, 36),
                color = Mg.Ink
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            added.etaText?.let { "$it 뒤 도착 · 내비게이션 기준" } ?: "도착 시간은 내비게이션에서 확인해 주세요",
            style = Mg.Body,
            color = Mg.Muted
        )
        Spacer(Modifier.height(10.dp))
        when {
            added.cancelError != null -> Text(
                added.cancelError,
                style = Mg.Body,
                color = Mg.OrangeText,
                maxLines = 2
            )
            countingDown -> SpeakingLine("5초 뒤 mungo 안내로 돌아가요")
        }
        Spacer(Modifier.weight(1f))
        SecondaryButton(
            if (added.cancelling) "경유지 취소 확인 중" else "경유지 취소",
            actions.onCancelWaypoint,
            Modifier.fillMaxWidth().height(96.dp),
            enabled = !added.cancelling
        )
    }
}

// ---------- Resting ----------

private fun restClock(ms: Long): String =
    "%d:%02d".format(Locale.ROOT, ms / 60_000, ms / 1_000 % 60)

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

/**
 * Figma 488:10659 (420×898): 휴식중 칩 · 컴포트 점수 변화 · 회복 안내 · 휴식 타이머 링(256) · 반려견 + 말풍선.
 * 칩 · 링 · 사진 · 말풍선 위치는 주행 카드와 같은 줄에 맞춘다.
 */
@Composable
fun RestSummaryFull(state: DrivingSessionState, modifier: Modifier) {
    val rest = state.rest
    val recommendedMs = RestIntervalPolicy.RECOMMENDED_REST_MINUTES * 60_000L
    val start = rest.scoreAtRestStart
    val now = state.comfort.score
    MgCard(
        modifier,
        padding = PaddingValues(top = 59.dp, bottom = 77.dp, start = 7.dp, end = 7.dp)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            CardStatusChip("휴식중")
            Spacer(Modifier.height(21.dp))
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MgIcon(MgIcons.Gauge, Mg.OrangeText, 16.dp, Modifier.padding(top = 7.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("컴포트 점수", style = Mg.semi(24, 29), color = Mg.Ink)
                    Text(
                        if (start != null &&
                            start != now
                        ) {
                            "${start}점 → ${now}점"
                        } else {
                            "${now}점"
                        },
                        style = Mg.semi(24, 29),
                        color = Mg.Ink
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("휴식 3분마다 점수가 1점씩 올라요", style = Mg.regular(24, 29), color = Mg.Muted)
            Spacer(Modifier.weight(1f))
            ScoreRing(
                rest.currentRestMs.toFloat() / recommendedMs,
                256.dp,
                26.dp,
                description = "휴식 경과 ${restClock(rest.currentRestMs)}"
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(restClock(rest.currentRestMs), style = Mg.bold(64, 76), color = Mg.Ink)
                    Text(
                        "추천 휴식\n${RestIntervalPolicy.RECOMMENDED_REST_MINUTES}분",
                        style = Mg.regular(24, 29),
                        color = Mg.Muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(53.dp))
            PetAvatar(118.dp)
            Spacer(Modifier.height(18.dp))
            SpeechBubble("휴식중")
        }
    }
}

@Composable
fun RestTimerSplit(state: DrivingSessionState, modifier: Modifier) {
    val rest = state.rest
    val recommendedMs = RestIntervalPolicy.RECOMMENDED_REST_MINUTES * 60_000L
    MgCard(modifier, padding = PaddingValues(horizontal = 34.dp, vertical = 32.dp)) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            ScoreRing(rest.currentRestMs.toFloat() / recommendedMs, 132.dp, 16.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(restClock(rest.currentRestMs), style = Mg.bold(30, 38), color = Mg.Ink)
                    Text("쉬는 중", style = Mg.Caption, color = Mg.Muted)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    restPlaceName(state) ?: "휴식 중",
                    style = Mg.Body,
                    color = Mg.Muted,
                    maxLines = 1
                )
                Text("쉬어 가는 중", style = Mg.bold(36, 47), color = Mg.Ink)
                Text(
                    "추천 ${RestIntervalPolicy.RECOMMENDED_REST_MINUTES}분 · ${recoveryText(state)}",
                    style = Mg.Body,
                    color = Mg.Muted,
                    maxLines = 1
                )
            }
        }
    }
}

private data class RestTask(val title: String, val hint: String, val done: String)

private val RestTasks = listOf(
    RestTask("물 먼저 주기", "물그릇을 꺼내 주세요", "마셨어요"),
    RestTask("10분간 산책 및 배변", "주차장 안쪽 안전한 곳에서", "다녀왔어요"),
    RestTask("리드줄 다시 연결", "출발하기 전에", "연결했어요")
)

/** Guardian checklist (Figma "쉬는 동안 할 일"); local to this rest, not part of the trip record. */
@Composable
fun RestChecklist(state: DrivingSessionState, compact: Boolean, modifier: Modifier) {
    var checked by rememberSaveable(state.rest.restStartedAtEpoch) {
        mutableStateOf(listOf(false, false, false))
    }
    // Figma 488:10672: 제목 B33, 항목 B24 / R24 (#6B7684), 체크 28 · 글자 시작 +46
    MgCard(
        modifier,
        padding = if (compact) {
            PaddingValues(start = 28.dp, end = 28.dp, top = 22.dp, bottom = 18.dp)
        } else {
            PaddingValues(start = 38.dp, end = 38.dp, top = 44.dp, bottom = 30.dp)
        }
    ) {
        Text(
            if (compact) "쉬는 동안 이렇게 해 주세요" else "쉬는 동안 추천하는 활동이에요",
            style = if (compact) Mg.BodyStrong else Mg.bold(33, 40),
            color = Mg.Ink
        )
        Spacer(Modifier.height(if (compact) 12.dp else 22.dp))
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
                        Modifier.padding(
                            start = if (compact) 0.dp else 6.dp
                        ).size(
                            28.dp
                        ).background(if (done) Mg.Orange else Color.White, RoundedCornerShape(7.dp))
                            .then(
                                if (done) {
                                    Modifier
                                } else {
                                    Modifier.border(
                                        2.dp,
                                        Mg.BarGrey,
                                        RoundedCornerShape(7.dp)
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) { if (done) MgIcon(MgIcons.Check, Color.White, 18.dp) }
                    Column {
                        Text(
                            task.title,
                            style = Mg.BodyStrong.copy(
                                textDecoration = if (done) TextDecoration.LineThrough else null
                            ),
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
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (name !=
                null
            ) {
                IconBadge(res = R.drawable.driving_paw, size = 60.dp)
            } else {
                IconBadge(res = R.drawable.driving_cup, size = 60.dp)
            }
            Column {
                Text(name ?: "휴식 기록 중", style = Mg.BodyStrong, color = Mg.Ink, maxLines = 1)
                Text(
                    if (name != null) "Google Maps에서 확인한 장소예요" else "출발하면 이번 휴식이 이동 기록에 저장돼요",
                    style = Mg.Body,
                    color = Mg.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    state.ttsSpeaking -> "mungo가 같은 내용을 말하고 있어요"
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
    state.closeError?.let {
        Text(it, style = Mg.BodyStrong, color = Mg.OrangeText, textAlign = TextAlign.Center)
    }
    if (!state.canCloseWindow) {
        Text(
            "이 창문은 앱에서 닫을 수 없어요. 차량의 창문 스위치를 이용해 주세요.",
            style = Mg.Body,
            color = Mg.OrangeText,
            textAlign = TextAlign.Center
        )
    }
}

/** Full layout popup (Figma 464:46215): 760 wide, icon, two lines, Gleo line, 괜찮아요 / 창문 닫기. */
@Composable
fun WindowWarningPopup(state: DrivingSessionState, onDismiss: () -> Unit, onClose: () -> Unit) {
    val busy = windowWarningBusy(state)
    Column(
        Modifier.width(
            760.dp
        ).background(
            Color.White,
            RoundedCornerShape(32.dp)
        ).padding(horizontal = 40.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(
            res = R.drawable.driving_window,
            size = 92.dp,
            background = Mg.Orange,
            tint = Mg.Ink
        )
        Spacer(Modifier.height(26.dp))
        Text(
            windowTitle(state),
            style = Mg.bold(30, 38),
            color = Mg.Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "기준 20%보다 많이 열렸어요.\n얼굴을 내밀면 눈을 다칠 수 있어요.",
            style = Mg.Body.copy(
                lineHeight =
                Mg.Body.fontSize * 1.55f
            ),
            color = Mg.Muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        SpeakingLine(windowSpeech(state))
        WindowErrors(state)
        Spacer(Modifier.height(36.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SecondaryButton(
                "괜찮아요",
                onDismiss,
                Modifier.weight(240f).height(96.dp),
                enabled = !busy,
                style = Mg.semi(24, 31)
            )
            PrimaryButton(
                closeLabel(state),
                onClose,
                Modifier.weight(424f).height(96.dp),
                enabled =
                state.canCloseWindow && !busy,
                icon = null
            )
        }
    }
}

/** Split layout (Figma 464:47750): the whole mungo panel turns into the warning. */
@Composable
fun WindowWarningPanel(
    state: DrivingSessionState,
    onDismiss: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier
) {
    val busy = windowWarningBusy(state)
    MgCard(
        modifier,
        color = Mg.Peach,
        border = Mg.WarnBorder,
        padding = PaddingValues(horizontal = 30.dp, vertical = 30.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(22.dp))
            IconBadge(
                res = R.drawable.driving_window,
                size = 100.dp,
                background = Mg.Orange,
                tint = Mg.Ink
            )
            Spacer(Modifier.height(26.dp))
            Text(
                windowTitle(state).replace("창문이 ", "창문이\n"),
                style = Mg.bold(32, 48),
                color = Mg.Ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "기준 20%보다 많이 열렸어요.\n얼굴을 내밀면 눈을 다칠 수 있어요.",
                style = Mg.Body,
                color = Mg.Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            SpeakingLine(windowSpeech(state))
            WindowErrors(state)
            Spacer(Modifier.height(30.dp))
            Row(
                Modifier.fillMaxWidth().background(
                    Color.White,
                    RoundedCornerShape(16.dp)
                ).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ScoreRing(state.comfort.score / 100f, 60.dp, 8.dp) {
                    Text("${state.comfort.score}", style = Mg.bold(20, 29), color = Mg.Ink)
                }
                Column {
                    Text("${petName(state)} 상태", style = Mg.Body, color = Mg.Muted)
                    Text(
                        comfortLabel(state) +
                            (
                                state.temperature.valueOrNull()?.let {
                                    " · 실내 ${formatTemperature(it)}°C"
                                }
                                    ?: ""
                                ),
                        style = Mg.BodyStrong,
                        color = Mg.Ink
                    )
                }
            }
        }
        PrimaryButton(
            closeLabel(state),
            onClose,
            Modifier.fillMaxWidth().height(100.dp),
            enabled =
            state.canCloseWindow && !busy
        )
        Spacer(Modifier.height(16.dp))
        SecondaryButton(
            "괜찮아요",
            onDismiss,
            Modifier.fillMaxWidth().height(100.dp),
            enabled = !busy,
            style = Mg.semi(24, 31)
        )
    }
}
