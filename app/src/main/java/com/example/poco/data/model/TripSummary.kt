package com.example.poco.data.model

/** 끝난 이동 한 번의 기록 (도착 리포트 · 홈 "지난 이동") */
data class TripSummary(
    val id: Long = 0,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    /** 내비 목적지. 아직 연동 전이라 비어 있을 수 있음 */
    val destination: String? = null,
    val distanceKm: Float,
    val comfortScore: Int,
    val hardBrake: EventStat,
    val hardAccel: EventStat,
    val sharpTurn: EventStat,
    /** 평온 회복 (가점) */
    val calmRecovery: EventStat,
    /** 공조 온도 알림 (감점) */
    val temperatureAlert: EventStat,
    /** 창문 안전 알림 횟수 - 점수에는 넣지 않는다 */
    val windowAlertCount: Int,
    val calmMinutes: Int,
    val timeline: List<TimelineBar>,
    /** 직전 이동 대비 점수 변화. 직전 이동이 없으면 null */
    val scoreDelta: Int? = null
) {
    val durationMinutes: Int get() = ((endedAtMillis - startedAtMillis) / 60_000L).toInt()
}

/** 사건 횟수와 점수 영향(감점은 음수, 가점은 양수) */
data class EventStat(val count: Int = 0, val points: Int = 0)

/** 흔들림 타임라인 막대 하나 */
data class TimelineBar(val level: ShakeLevel, val intensity: Float)

enum class ShakeLevel {
    CALM,
    TURN,
    BRAKE
}
