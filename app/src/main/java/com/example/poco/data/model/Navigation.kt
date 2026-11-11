package com.example.poco.data.model

/** FusedLocation SDK 위치 한 점 */
data class VehicleLocation(
    val latitude: Double,
    val longitude: Double,
    /** 진행 방향 (도, 0 = 북) */
    val headingDeg: Float,
    /** 위치 신뢰도 0..1 */
    val confidence: Float,
    val timestampMillis: Long
)

/** NaviHelper SDK에서 오는 안내 (앱에서 쓰는 것만) */
sealed interface NaviEvent {
    /** 다가오는 회전 안내 (TBT) */
    data class UpcomingTurns(val turns: List<UpcomingTurn>) : NaviEvent

    /** 다가오는 주의 구간 (과속방지턱 · 어린이보호구역 등) */
    data class UpcomingCautions(val cautions: List<UpcomingCaution>) : NaviEvent

    data class Destination(val name: String, val remainDistanceM: Int, val remainTimeSec: Int) :
        NaviEvent

    data class Arrived(val name: String, val distanceM: Int, val durationSec: Int) : NaviEvent

    data class SpeedLimit(val kmh: Int) : NaviEvent

    /** 내비에서 길 안내를 시작함 → 홈에서 주행 자동 시작 */
    data class RouteStarted(val name: String, val distanceM: Int, val durationSec: Int) :
        NaviEvent

    /** 길 안내가 끝남 (도착 · 종료) */
    data object RouteEnded : NaviEvent

    /** 운전자가 길 안내를 취소함 → 주행은 계속, 목적지 정보만 지움 */
    data object RouteCancelled : NaviEvent
}

data class UpcomingTurn(val kind: TurnKind, val distanceM: Int, val description: String)

/** 반려견이 흔들리기 쉬운 회전만 구분 */
enum class TurnKind {
    /** 급회전 · 유턴 · 회전교차로 · IC 진출입 → 미리 안내 */
    SHARP,
    NORMAL,
    REST_AREA,
    OTHER
}

data class UpcomingCaution(val kind: CautionKind, val distanceM: Int)

enum class CautionKind {
    SPEED_BUMP,
    SCHOOL_ZONE,
    DOWNHILL,
    NARROW_ROAD,
    SLIPPERY,
    OTHER
}
