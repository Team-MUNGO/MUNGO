package com.example.poco.data.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AssistantModelsTest {
    @Test fun recentLocationOnlyAcceptsValidatedEpochMilliseconds() {
        val now = 1_794_441_600_000L
        assertNotNull(freshLocation(37.0, 127.0, now, now))
        assertEquals(30_000L, freshLocation(37.0, 127.0, now - 30_000, now)?.ageMs)
        assertNull(freshLocation(37.0, 127.0, now - 30_001, now))
        assertNull(freshLocation(37.0, 127.0, now + 1, now))
        assertNull(freshLocation(37.0, 127.0, 100_000, now))
        assertNull(freshLocation(Double.NaN, 127.0, now, now))
        assertNull(freshLocation(91.0, 127.0, now, now))
    }

    private val place =
        AssistPlace("ChIJvSMioIapZDURijUJ9O6phq8", "덕평휴게소", 37.24, 127.39, null, null, true)
    private val request = AssistRequest(AssistType.USER_QUESTION, "req-1", null, "q")
    private fun ok(places: List<AssistPlace>, recommended: String?, maps: String? = "google_maps") =
        AssistResponse(
            "req-1",
            null,
            AssistType.USER_QUESTION,
            AssistStatus.OK,
            "답변",
            "답변",
            places,
            recommended,
            null,
            AssistSources(
                "gemini",
                maps
            )
        )

    /** #21: invented IDs, bad coordinates, non-Maps sources or mismatched requests are rejected. */
    @Test fun responseValidationRejectsUnverifiedPlaces() {
        ok(listOf(place), place.placeId).validate(request)
        assertThrows(IllegalArgumentException::class.java) {
            ok(listOf(place), "ChIJinvented000000").validate(request)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ok(listOf(place.copy(latitude = 91.0)), null).validate(request)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ok(listOf(place, place), null).validate(request)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ok(listOf(place), null, maps = null).validate(request)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ok(listOf(place.copy(placeId = "short")), null).validate(request)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ok(emptyList(), null).copy(requestId = "other").validate(request)
        }
    }

    @Test fun onlyFlaggedMapsPlacesBecomeWaypoints() {
        val response = ok(listOf(place), place.placeId)
        assertNotNull(place.toVerifiedPlace(response))
        assertNull(place.copy(canAddWaypoint = false).toVerifiedPlace(response))
        assertNull(place.toVerifiedPlace(response.copy(sources = AssistSources("gemini", null))))
    }
}
