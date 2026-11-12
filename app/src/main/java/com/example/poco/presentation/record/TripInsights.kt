package com.example.poco.presentation.record

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.RestIntervalPolicy
import com.example.poco.data.record.MotionRecord
import com.example.poco.data.record.MotionSign
import com.example.poco.data.record.TripEntity
import com.example.poco.data.record.TripEventEntity
import com.example.poco.data.record.TripEventType
import com.example.poco.presentation.design.MgIcons
import com.example.poco.presentation.driving.component.clockText
import com.example.poco.presentation.driving.component.intervalText
import com.example.poco.presentation.driving.component.koreanDuration
import com.example.poco.presentation.driving.component.seatShort
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Everything on the trip screens is derived from the stored trip row and its event log only:
 * no place names, routes or amenities are invented when the record does not contain them.
 */
fun tripDurationMs(trip: TripEntity): Long =
    trip.endedAtEpoch?.let { it - trip.startedAtEpoch } ?: ((trip.drivingMs ?: 0) + (trip.restMs ?: 0))

fun tripTitle(trip: TripEntity): String = when {
    trip.origin != null && trip.destination != null -> "${trip.origin} → ${trip.destination}"
    trip.destination != null -> "→ ${trip.destination}"
    else -> "${clockText(trip.startedAtEpoch)} 출발 · ${trip.petName}와 이동"
}

fun dateText(epoch: Long, pattern: String = "M월 d일 (E)"): String = SimpleDateFormat(pattern, Locale.KOREAN).format(Date(epoch))

fun pastComfortLabel(score: Int) = when {
    score >= 85 -> "편안했어요"
    score >= 70 -> "조금 흔들렸어요"
    else -> "힘들었어요"
}

fun seatLabel(trip: TripEntity) = PetSeat.entries.firstOrNull { it.name == trip.seat }?.let(::seatShort) ?: "확인 불가"

fun turnCount(trip: TripEntity) = (trip.sharpTurnCount ?: 0) + (trip.repeatedTurnCount ?: 0)

/** The completed trip that started just before [trip], for "지난번보다" comparisons. */
fun previousTrip(trips: List<TripEntity>, trip: TripEntity): TripEntity? =
    trips.filter { it.id != trip.id && it.finalScore != null && it.startedAtEpoch < trip.startedAtEpoch }
        .maxByOrNull { it.startedAtEpoch }

fun scoreDeltaText(trip: TripEntity, previous: TripEntity?): String? {
    val now = trip.finalScore ?: return null
    val before = previous?.finalScore ?: return null
    return when {
        now > before -> "지난번보다 ${now - before}점 올랐어요"
        now < before -> "지난번보다 ${before - now}점 내려갔어요"
        else -> "지난번과 같은 점수예요"
    }
}

enum class ItemKind { PENALTY, BONUS, ALERT }

data class TimelineItem(
    val at: Long,
    val kind: ItemKind,
    val icon: ImageVector,
    val title: String,
    val detail: String,
    val delta: Int,
    val type: String,
    val scoreAfter: Int?
)

private fun penaltyInfo(reason: String?): Pair<String, ImageVector> = when (reason) {
    "RAPID_BRAKING" -> "급정거" to MgIcons.Brake
    "RAPID_ACCELERATION" -> "급가속" to MgIcons.Gauge
    "SHARP_TURN" -> "급회전" to MgIcons.Turn
    "REPEATED_TURNS" -> "연속 커브" to MgIcons.Curve
    "SPEED_BUMP" -> "과속방지턱" to MgIcons.Bump
    else -> "흔들림" to MgIcons.Brake
}

fun penaltyTitle(reason: String?) = penaltyInfo(reason).first

/**
 * Event list rows (Figma 465:48555): penalties (speed bumps within 2 minutes merged), one row per
 * rest with its recovery, calm-driving recovery runs, and window/temperature alerts.
 */
fun timelineItems(trip: TripEntity, events: List<TripEventEntity>): List<TimelineItem> {
    val out = mutableListOf<TimelineItem>()
    val sorted = events.sortedWith(compareBy({ it.atEpoch }, { it.id }))
    var restStart: TripEventEntity? = null
    var restGain = 0
    var calmStart: Long? = null
    var calmEnd = 0L
    var calmGain = 0
    var calmAfter: Int? = null
    fun flushCalm() {
        val start = calmStart ?: return
        val minutes = ((calmEnd - start) / 60_000).coerceAtLeast(1)
        out += TimelineItem(start, ItemKind.BONUS, MgIcons.Calm, "평온 유지 ${minutes}분", "평온하게 달려 점수가 회복됐어요", calmGain, "CALM", calmAfter)
        calmStart = null
        calmGain = 0
    }
    for (e in sorted) {
        when (e.type) {
            TripEventType.REST_STARTED -> {
                flushCalm()
                restStart = e
                restGain = 0
            }
            TripEventType.REST_COMPLETED, TripEventType.REST_CANCELLED -> {
                val start = restStart ?: continue
                val minutes = ((e.atEpoch - start.atEpoch) / 60_000).coerceAtLeast(1)
                out += TimelineItem(
                    start.atEpoch, if (restGain > 0) ItemKind.BONUS else ItemKind.ALERT, MgIcons.Cup,
                    if (e.type == TripEventType.REST_COMPLETED) "휴식 ${minutes}분" else "짧은 정차 ${minutes}분",
                    if (start.detail == "WAYPOINT_ARRIVAL") "경유지 도착 후 휴식" else "직접 시작한 휴식",
                    restGain, "REST", null
                )
                restStart = null
            }
            TripEventType.SCORE_RECOVERY -> {
                val delta = e.scoreDelta ?: 0
                if (restStart != null) {
                    restGain += delta
                } else {
                    if (calmStart == null) calmStart = e.atEpoch
                    calmEnd = e.atEpoch
                    calmGain += delta
                    calmAfter = e.scoreAfter
                }
            }
            TripEventType.SCORE_PENALTY -> {
                flushCalm()
                val (title, icon) = penaltyInfo(e.detail)
                val delta = e.scoreDelta ?: 0
                val last = out.lastOrNull()
                if (e.detail == "SPEED_BUMP" && last != null && last.type == "SPEED_BUMP" && e.atEpoch - last.at <= 120_000) {
                    val count = (Regex("(\\d+)회").find(last.title)?.groupValues?.get(1)?.toInt() ?: 1) + 1
                    out[out.lastIndex] = last.copy(title = "과속방지턱 ${count}회", delta = last.delta + delta, scoreAfter = e.scoreAfter, detail = scoreDetail(last.scoreAfter?.minus(last.delta), e.scoreAfter))
                } else {
                    out += TimelineItem(e.atEpoch, ItemKind.PENALTY, icon, title, scoreDetail(e.scoreAfter?.minus(delta), e.scoreAfter), delta, e.detail ?: "", e.scoreAfter)
                }
            }
            TripEventType.WINDOW_WARNING -> {
                flushCalm()
                out += TimelineItem(e.atEpoch, ItemKind.ALERT, MgIcons.Window, "창문 ${e.detail ?: ""}% 열림", "${seatLabel(trip)}에서 창문 알림", 0, "WINDOW", null)
            }
            TripEventType.TEMPERATURE_OUT_OF_RANGE -> {
                flushCalm()
                val (status, value) = (e.detail ?: "").split(":").let { it.getOrNull(0) to it.getOrNull(1) }
                out += TimelineItem(
                    e.atEpoch, ItemKind.ALERT, MgIcons.Sun,
                    if (status == "HOT") "실내 온도 높음" else "실내 온도 낮음",
                    value?.let { "실내 ${it}°C · 기준 범위 밖" } ?: "기준 범위 밖", 0, "TEMPERATURE", null
                )
            }
        }
    }
    flushCalm()
    return out
}

private fun scoreDetail(before: Int?, after: Int?) =
    if (before != null && after != null) "점수 $before → $after" else "점수 변화 기록"

fun tipFor(type: String): String = when (type) {
    "RAPID_BRAKING" -> "앞차와 거리를 넉넉히 두고 천천히 멈춰 보세요."
    "RAPID_ACCELERATION" -> "출발할 때는 천천히 가속해 보세요."
    "SHARP_TURN" -> "커브에 들어가기 전에 속도를 줄여 보세요."
    "REPEATED_TURNS" -> "연속 커브에서는 일정한 속도를 유지해 보세요."
    "SPEED_BUMP" -> "과속방지턱 앞에서는 충분히 감속해 주세요."
    "REST" -> "휴식 3분마다 점수가 1점씩 올라요."
    "CALM" -> "평온하게 달리면 점수가 천천히 회복돼요."
    "WINDOW" -> "반려견 쪽 창문은 20% 이하로 열어 주세요."
    "TEMPERATURE" -> "에어컨이나 히터로 기준 온도를 맞춰 주세요."
    else -> "천천히, 부드럽게 운전해 주세요."
}

/** Measured cabin temperature closest before [at] (within 5 minutes), never interpolated. */
fun temperatureAt(events: List<TripEventEntity>, at: Long): String? =
    events.filter { it.type == TripEventType.TEMPERATURE_SAMPLE && it.atEpoch <= at && at - it.atEpoch <= 5 * 60_000 }
        .maxByOrNull { it.atEpoch }?.detail?.let { "$it°C" }

/** "현댕이의 오늘 한 줄": a deterministic sentence from the stored counts. */
fun oneLine(trip: TripEntity, events: List<TripEventEntity>): String {
    val items = timelineItems(trip, events)
    val penalties = items.filter { it.kind == ItemKind.PENALTY }
    val rested = items.any { it.type == "REST" && it.delta > 0 }
    val first = when {
        penalties.isEmpty() -> "흔들림 없이 평온하게 다녀왔어요."
        penalties.size <= 2 -> {
            val worst = penalties.minBy { it.delta }
            "${clockText(worst.at)} ${worst.title} 말고는 대부분 평온했어요."
        }
        else -> "흔들림이 ${penalties.size}번 있었어요."
    }
    return if (rested) "$first 휴식 뒤 점수가 다시 올랐어요." else first
}

data class Tip(val title: String, val body: String)

fun recommendedInterval(current: Int, record: MotionRecord?): Int? {
    if (record == null || record.total == 0) return null
    val next = RestIntervalPolicy.clamp(current - RestIntervalPolicy.STEP_MINUTES)
    return next.takeIf { it < current }
}

fun detailTip(trip: TripEntity, previous: TripEntity?, currentInterval: Int): Tip {
    val now = MotionRecord.decode(trip.motionSicknessSigns)
    val before = previous?.let { MotionRecord.decode(it.motionSicknessSigns) }
    if (now != null && before != null && now.total > before.total) {
        val sign = MotionSign.entries.maxBy { now.count(it) - before.count(it) }
        val rec = recommendedInterval(currentInterval, now)
        return Tip(
            "${sign.label}이 지난번보다 늘었어요.",
            rec?.let { "다음엔 ${intervalText(it)}마다 쉬어 가는 걸 추천해요. 출발 전에 다시 알려 드릴게요." } ?: "다음 이동에서도 휴식을 챙겨 주세요."
        )
    }
    val brakes = trip.rapidBrakingCount ?: 0
    return when {
        brakes >= 2 -> Tip("급정거가 ${brakes}번 있었어요.", "앞차와 거리를 넉넉히 두면 ${trip.petName}가 덜 놀라요.")
        turnCount(trip) >= 2 -> Tip("급회전이 ${turnCount(trip)}번 있었어요.", "커브에 들어가기 전에 속도를 미리 줄여 보세요.")
        else -> Tip("평온한 이동이었어요.", "지금처럼 ${intervalText(currentInterval)}마다 쉬어 가요.")
    }
}

/** Trips with a guardian entry, oldest first, ending with [trip] (Figma "최근 4번 이동 비교"). */
fun recentMotionTrips(trips: List<TripEntity>, trip: TripEntity, limit: Int = 4): List<Pair<TripEntity, MotionRecord?>> {
    val earlier = trips.filter { it.id != trip.id && it.startedAtEpoch < trip.startedAtEpoch && MotionRecord.decode(it.motionSicknessSigns) != null }
        .sortedByDescending { it.startedAtEpoch }.take(limit - 1).reversed()
    return (earlier + trip).map { it to MotionRecord.decode(it.motionSicknessSigns) }
}

/** Only states a pattern when stored entries support it (at least one long and one short trip). */
fun motionPattern(entries: List<Pair<TripEntity, MotionRecord?>>): Tip? {
    val known = entries.mapNotNull { (t, r) -> r?.let { t to it } }
    val long = known.filter { tripDurationMs(it.first) > 60 * 60_000 }
    val short = known.filter { tripDurationMs(it.first) <= 60 * 60_000 }
    if (long.isEmpty() || short.isEmpty()) return null
    val longAvg = long.map { it.second.total }.average()
    val shortAvg = short.map { it.second.total }.average()
    if (longAvg <= shortAvg) return null
    val calm = short.filter { it.second.total == 0 }.minByOrNull { tripDurationMs(it.first) }
    return Tip(
        "1시간 넘게 달린 날 징후가 늘었어요.",
        calm?.let { "${koreanDuration(tripDurationMs(it.first))} 이동(${dateText(it.first.startedAtEpoch, "M/d")})에선 하나도 없었어요." }
            ?: "짧은 이동에서는 징후가 적었어요."
    )
}

enum class Period(val label: String) { WEEK("이번 주"), MONTH("이번 달"), ALL("전체") }

private fun startOfWeek(now: Long, weeksAgo: Int = 0): Long = Calendar.getInstance(Locale.KOREA).apply {
    timeInMillis = now
    firstDayOfWeek = Calendar.MONDAY
    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    if (timeInMillis > now) add(Calendar.WEEK_OF_YEAR, -1)
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    add(Calendar.WEEK_OF_YEAR, -weeksAgo)
}.timeInMillis

private fun startOfMonth(now: Long): Long = Calendar.getInstance(Locale.KOREA).apply {
    timeInMillis = now
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun filterTrips(trips: List<TripEntity>, period: Period, now: Long): List<TripEntity> = when (period) {
    Period.WEEK -> trips.filter { it.startedAtEpoch >= startOfWeek(now) }
    Period.MONTH -> trips.filter { it.startedAtEpoch >= startOfMonth(now) }
    Period.ALL -> trips
}

/** Section headers in the list: 이번 주, 지난주, then month names. */
fun sectionOf(trip: TripEntity, now: Long): String = when {
    trip.startedAtEpoch >= startOfWeek(now) -> "이번 주"
    trip.startedAtEpoch >= startOfWeek(now, 1) -> "지난주"
    else -> dateText(trip.startedAtEpoch, "yyyy년 M월")
}
