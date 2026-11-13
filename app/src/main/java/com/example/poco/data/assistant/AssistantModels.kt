package com.example.poco.data.assistant

import com.example.poco.core.driving.VerifiedPlace
import kotlinx.serialization.Serializable

object AssistType {
    const val USER_QUESTION = "USER_QUESTION"
    const val REST_RECOMMENDATION = "REST_RECOMMENDATION"
}

object AssistStatus {
    const val OK = "OK"
    const val NO_RESULTS = "NO_RESULTS"
    const val LOCATION_REQUIRED = "LOCATION_REQUIRED"
}

/** Only values the app actually measured are set; null fields are omitted from the JSON. */
@Serializable
data class AssistContext(
    val comfortScore: Int? = null,
    val elapsedMinutes: Long? = null,
    val minutesToRest: Long? = null,
    val restIntervalMinutes: Int? = null,
    val temperatureC: Float? = null,
    val minTemperatureC: Float? = null,
    val maxTemperatureC: Float? = null,
    val temperatureStatus: String? = null,
    val windowPercent: Double? = null,
    val petSize: String? = null,
    val petWeightKg: Double? = null,
    /** 답변에서 "반려견" 대신 부를 이름 */
    val petName: String? = null,
    /** Pleos 내비 길 안내 중일 때만: 목적지 이름 · 남은 시간(분) */
    val routeDestination: String? = null,
    val routeRemainingMinutes: Long? = null
)

@Serializable
data class AssistantLocation(val latitude: Double, val longitude: Double, val ageMs: Long)

@Serializable
data class AssistRequest(
    val type: String,
    val requestId: String,
    val eventId: String? = null,
    val question: String? = null,
    val searchQuery: String? = null,
    val context: AssistContext = AssistContext(),
    val location: AssistantLocation? = null
)

/** Facts from Google Maps only (server-verified). Gemini prose is kept separately in the response. */
@Serializable
data class AssistPlace(
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val mapsUrl: String? = null,
    val canAddWaypoint: Boolean = false
)

@Serializable
data class AssistSources(val text: String? = null, val places: String? = null)

@Serializable
data class AssistResponse(
    val requestId: String,
    val eventId: String? = null,
    val type: String,
    val status: String,
    val displayText: String,
    val ttsText: String,
    val places: List<AssistPlace> = emptyList(),
    val recommendedPlaceId: String? = null,
    val searchQuery: String? = null,
    val sources: AssistSources = AssistSources()
) {
    val recommended: AssistPlace? get() = places.firstOrNull { it.placeId == recommendedPlaceId }
}

class AssistantException(val code: String, message: String) : IllegalStateException(message)

/** Defensive client-side check: nothing unverified can reach the UI or NaviHelper. */
fun AssistResponse.validate(request: AssistRequest): AssistResponse {
    require(requestId == request.requestId && type == request.type && eventId == request.eventId)
    require(status in setOf(AssistStatus.OK, AssistStatus.NO_RESULTS, AssistStatus.LOCATION_REQUIRED))
    require(displayText.isNotBlank() && displayText.length <= 300 && ttsText.length <= 300)
    require(places.size <= 5 && places.map { it.placeId }.distinct().size == places.size)
    places.forEach {
        require(Regex("[A-Za-z0-9_-]{10,300}").matches(it.placeId))
        require(it.name.isNotBlank() && it.name.length <= 100)
        require(it.latitude.isFinite() && it.latitude in -90.0..90.0)
        require(it.longitude.isFinite() && it.longitude in -180.0..180.0)
    }
    require(places.isEmpty() || sources.places == "google_maps")
    require(recommendedPlaceId == null || places.any { it.placeId == recommendedPlaceId })
    require(status != AssistStatus.LOCATION_REQUIRED || !searchQuery.isNullOrBlank() || type == AssistType.REST_RECOMMENDATION)
    return this
}

/** Only Maps-sourced places flagged by the server can become a Navi waypoint. */
fun AssistPlace.toVerifiedPlace(response: AssistResponse): VerifiedPlace? =
    takeIf { canAddWaypoint && response.sources.places == "google_maps" }
        ?.let { VerifiedPlace(placeId, name, latitude, longitude, address) }

/** "멍고의 오늘 한 줄" (/summary): only recorded trip values, no coordinates or vehicle identifiers. */
@Serializable
data class TripSummaryCounts(
    val rapidBraking: Int,
    val sharpTurn: Int,
    val speedBump: Int,
    val rapidAcceleration: Int,
    val windowWarning: Int,
    val temperatureAlert: Int
)

/** [minute] = minutes since departure (clock times are never sent, so the AI cannot invent them). */
@Serializable
data class TripSummaryEvent(val minute: Int, val kind: String, val scoreDelta: Int? = null)

@Serializable
data class TripSummaryInput(
    val petName: String,
    val durationMinutes: Int,
    val finalScore: Int,
    val minScore: Int? = null,
    /** Only when an earlier completed trip exists */
    val previousScore: Int? = null,
    val restCount: Int,
    val restMinutes: Int,
    val counts: TripSummaryCounts,
    val events: List<TripSummaryEvent>,
    val motionSigns: List<String>
)

@Serializable
data class TripSummaryRequest(val requestId: String, val trip: TripSummaryInput)

@Serializable
data class TripSummaryResponse(val requestId: String, val text: String)
