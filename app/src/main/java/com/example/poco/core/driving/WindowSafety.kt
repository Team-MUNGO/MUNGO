package com.example.poco.core.driving

import com.example.poco.data.model.WindowPositionRange

/** Standard WINDOW_POS: 0 closed, max fully open. Tilt/negative ranges are unsupported. */
fun normalizeWindowPosition(raw: Int?, range: WindowPositionRange): Double? {
    if (raw == null || range.min != 0 || range.max <= 0 || raw !in 0..range.max) return null
    return raw.toDouble() / range.max * 100
}

class WindowDangerDetector(private val threshold: Double = 20.0) {
    private var inDanger = false

    /** Missing callbacks never re-arm an already announced danger interval. */
    fun update(percent: Double?): Boolean {
        if (percent == null || !percent.isFinite()) return false
        val danger = percent > threshold
        val entered = danger && !inDanger
        inDanger = danger
        return entered
    }
}

enum class WindowCloseStatus { IDLE, REQUESTING, VERIFYING, FAILED }
