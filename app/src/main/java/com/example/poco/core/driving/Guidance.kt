package com.example.poco.core.driving

/** Lower rank = higher priority. Screen card and TTS are driven by the same guidance event. */
enum class GuidancePriority(val rank: Int) {
    WINDOW_WARNING(1),
    WAYPOINT_ARRIVAL(2),
    WAYPOINT_FIVE_MINUTES(3),
    REST_PROPOSAL(4),
    AI_ANSWER(5),
    INFO(6)
}

data class Guidance(
    val id: String,
    val priority: GuidancePriority,
    val displayText: String,
    val ttsText: String? = displayText
)

/** Decision for one posted guidance; pure so the policy is unit-testable. */
data class GuidanceDecision(val accepted: Boolean, val speak: Boolean, val interruptCurrent: Boolean)

/**
 * Event-ID ledger. A guidance ID is handled once per session: recomposition, screen switches and
 * repeated sensor callbacks cannot replay it. Voice OFF shows the card only and the speech is
 * dropped, never queued for later. Lower-priority speech is interrupted by higher priority; a
 * lower-priority item arriving during higher-priority speech is shown later but not spoken.
 */
class GuidanceLedger {
    private val handled = mutableSetOf<String>()
    private val active = linkedMapOf<String, Guidance>()
    var speaking: Guidance? = null
        private set

    fun post(guidance: Guidance, voiceEnabled: Boolean): GuidanceDecision {
        if (!handled.add(guidance.id)) return GuidanceDecision(false, false, false)
        active[guidance.id] = guidance
        val current = speaking
        val canSpeak = voiceEnabled && !guidance.ttsText.isNullOrBlank() &&
            (current == null || guidance.priority.rank < current.priority.rank)
        return GuidanceDecision(true, canSpeak, canSpeak && current != null)
    }

    fun startedSpeaking(guidance: Guidance) {
        speaking = guidance
    }

    fun finishedSpeaking(id: String) {
        if (speaking?.id == id) speaking = null
    }

    fun remove(id: String) {
        active.remove(id)
        if (speaking?.id == id) speaking = null
    }

    fun removeWhere(predicate: (Guidance) -> Boolean) {
        active.values.filter(predicate).forEach { remove(it.id) }
    }

    fun wasHandled(id: String) = id in handled

    /** Highest priority first, newest first within the same priority. */
    fun top(): Guidance? = active.values.reversed().minByOrNull { it.priority.rank }

    fun activeOf(priority: GuidancePriority) = active.values.filter { it.priority == priority }
}
