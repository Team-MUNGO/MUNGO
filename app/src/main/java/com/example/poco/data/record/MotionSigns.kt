package com.example.poco.data.record

/** Guardian-entered signs after arrival (Figma 465:48703). Observations only, not a diagnosis. */
enum class MotionSign(val label: String) {
    YAWN("하품"),
    DROOL("침 흘림"),
    PANT("헐떡임"),
    SHAKE("떨림"),
    VOMIT("구토"),
    RESTLESS("안절부절")
}

/**
 * Stored in [TripEntity.motionSicknessSigns] as "YAWN:3,DROOL:1". "FINE" means the guardian
 * answered "괜찮았어요". Null means nothing was entered (never treated as zero signs).
 */
data class MotionRecord(val counts: Map<MotionSign, Int> = emptyMap(), val fine: Boolean = false) {
    val total: Int get() = counts.values.sum()
    val entered: Boolean get() = fine || counts.isNotEmpty()

    fun count(sign: MotionSign) = counts[sign] ?: 0

    /** Summary chips toggle a sign between 0 and at least 1 (count is kept if already higher). */
    fun toggle(sign: MotionSign): MotionRecord {
        val next = if (count(sign) > 0) counts - sign else counts + (sign to 1)
        return MotionRecord(next, fine = false)
    }

    fun withCount(sign: MotionSign, value: Int): MotionRecord {
        val v = value.coerceIn(0, 99)
        return MotionRecord(if (v == 0) counts - sign else counts + (sign to v), fine = false)
    }

    fun markFine() = MotionRecord(emptyMap(), fine = !fine)

    fun encode(): String? = when {
        counts.isNotEmpty() -> MotionSign.entries.filter { count(it) > 0 }.joinToString(",") { "${it.name}:${count(it)}" }
        fine -> FINE
        else -> null
    }

    companion object {
        private const val FINE = "FINE"

        fun decode(raw: String?): MotionRecord? {
            if (raw.isNullOrBlank()) return null
            if (raw == FINE) return MotionRecord(fine = true)
            val counts = raw.split(",").mapNotNull { part ->
                val (name, n) = part.split(":").let { it[0] to it.getOrNull(1) }
                val sign = MotionSign.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
                sign to (n?.toIntOrNull() ?: 1).coerceAtLeast(1)
            }.toMap()
            return MotionRecord(counts)
        }
    }
}
