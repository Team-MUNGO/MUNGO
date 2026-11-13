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

    /** 서버가 한국어로 바꾼 이름 · 역지오코딩 주소가 그대로 경유지에 들어간다 (좌표는 Maps 값 그대로) */
    @Test fun koreanNameAndAddressReachTheWaypoint() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val decoded = json.decodeFromString<AssistPlace>(
            """{"placeId":"ChIJenglishPlace0001","name":"해피 도그 파크","latitude":37.3,""" +
                """"longitude":127.3,"address":"경기도 이천시 마장면 덕평로 154",""" +
                """"canAddWaypoint":true,"originalName":"Happy Dog Park"}"""
        )
        val response = ok(listOf(decoded), decoded.placeId).validate(request)
        val waypoint = response.recommended!!.toVerifiedPlace(response)!!
        assertEquals("해피 도그 파크", waypoint.name)
        assertEquals("경기도 이천시 마장면 덕평로 154", waypoint.address)
        assertEquals(37.3, waypoint.latitude, 0.0)
        assertEquals(127.3, waypoint.longitude, 0.0)
        assertThrows(IllegalArgumentException::class.java) {
            ok(listOf(place.copy(address = "x".repeat(201))), null).validate(request)
        }
    }
}
