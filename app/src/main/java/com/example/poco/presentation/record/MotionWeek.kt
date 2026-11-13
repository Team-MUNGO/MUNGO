package com.example.poco.presentation.record

import com.example.poco.data.record.MotionRecord
import com.example.poco.data.record.MotionSign
import com.example.poco.data.record.TripEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 멀미 징후 표의 하루 (이동이 없으면 trips = 0) */
data class MotionDay(
    val date: LocalDate,
    val trips: Int,
    val drivingMs: Long,
    /** 그날 이동 중 하나라도 도착 후 기록했는지 */
    val recorded: Boolean,
    /** 그날 "보였어요"로 체크된 징후 */
    val signs: Set<MotionSign>
)

/** Figma 515:347 "이동기록_04_멀미징후": [end] 포함 7일 */
data class MotionWeek(
    val start: LocalDate,
    val end: LocalDate,
    val days: List<MotionDay>,
    val tripCount: Int,
    /** 징후가 하나라도 보인 이동 수 */
    val signTrips: Int,
    /** 징후별로 보인 이동 수 */
    val perSign: Map<MotionSign, Int>
) {
    /** 가장 자주 보인 징후 (없으면 null) */
    val top: Pair<MotionSign, Int>? get() = perSign.entries
        .filter { it.value > 0 }
        .maxWithOrNull(
            compareBy<Map.Entry<MotionSign, Int>> {
                it.value
            }.thenBy { -it.key.ordinal }
        )
        ?.toPair()
}

fun LocalDate.atEpoch(zone: ZoneId = ZoneId.systemDefault()) =
    atStartOfDay(zone).toInstant().toEpochMilli()

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun motionWeek(
    trips: List<TripEntity>,
    end: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): MotionWeek {
    val start = end.minusDays(MOTION_WEEK_DAYS - 1L)
    val inWeek = trips.filter { it.startedAtEpoch.toLocalDate(zone) in start..end }
        .map { it to MotionRecord.decode(it.motionSicknessSigns) }
    val days = (0 until MOTION_WEEK_DAYS).map { i ->
        val date = start.plusDays(i.toLong())
        val today = inWeek.filter { it.first.startedAtEpoch.toLocalDate(zone) == date }
        MotionDay(
            date = date,
            trips = today.size,
            drivingMs = today.sumOf { (t, _) -> t.drivingMs ?: tripDurationMs(t) },
            recorded = today.any { it.second?.entered == true },
            signs = today.flatMap { (_, r) ->
                MotionSign.entries.filter { (r?.count(it) ?: 0) > 0 }
            }.toSet()
        )
    }
    return MotionWeek(
        start = start,
        end = end,
        days = days,
        tripCount = inWeek.size,
        signTrips = inWeek.count { (it.second?.total ?: 0) > 0 },
        perSign = MotionSign.entries.associateWith { sign ->
            inWeek.count { (it.second?.count(sign) ?: 0) > 0 }
        }
    )
}

const val MOTION_WEEK_DAYS = 7

/** 패턴을 말하려면 이동이 이만큼은 있어야 한다 (Figma 각주: 이동이 3번 이상일 때 판단해요) */
const val MOTION_PATTERN_MIN_TRIPS = 3
