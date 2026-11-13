package com.example.poco.core.driving

import kotlin.math.abs

/**
 * 흔들림 정도를 [intervalMs](기본 10초)마다 한 칸씩 모은다.
 * 칸마다 들어온 앞뒤 · 좌우 가속도(m/s²) 중 가장 큰 값을 남긴다.
 * 값이 하나도 들어오지 않은 칸(속도 끊김 · 휴식 중)은 남기지 않는다.
 */
class ShakeSampler(private val startedAt: Long, private val intervalMs: Long = 10_000L) {
    data class Sample(val at: Long, val peakMps2: Double)

    private var bucketStart = startedAt
    private var peak: Double? = null

    /** 가속도 한 개. 이미 지난 칸에 속한 늦은 값은 버린다 */
    fun add(at: Long, accelMps2: Double) {
        if (!accelMps2.isFinite() || at < bucketStart) return
        val magnitude = abs(accelMps2)
        peak = maxOf(peak ?: 0.0, magnitude)
    }

    /** [now]까지 끝난 칸을 돌려준다. 칸의 시각 = 그 칸이 끝난 때 */
    fun flush(now: Long): List<Sample> {
        if (now - bucketStart < intervalMs) return emptyList()
        val out = mutableListOf<Sample>()
        val end = bucketStart + intervalMs
        peak?.let { out += Sample(end, it) }
        peak = null
        // 여러 칸을 건너뛰었으면 (값이 없던 칸은 남기지 않고) 지금 칸으로 옮긴다
        bucketStart = end + (now - end) / intervalMs * intervalMs
        return out
    }

    /** 주행이 끝날 때 채우다 만 칸까지 남긴다 */
    fun finish(now: Long): List<Sample> {
        val out = flush(now).toMutableList()
        peak?.let { if (now > bucketStart) out += Sample(now, it) }
        peak = null
        return out
    }
}
