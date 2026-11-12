package com.example.poco.data.model

/** 마이펫 아래 통계: 함께한 이동 · 평균 컴포트 점수 · 마지막 이동 */
data class TripStats(
    val count: Int = 0,
    val averageScore: Int? = null,
    val lastStartedAtMillis: Long? = null
)
