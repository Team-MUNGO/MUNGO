package com.example.poco

import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.poco.core.theme.POCOTheme
import com.example.poco.data.assistant.AssistRequest
import com.example.poco.data.assistant.AssistResponse
import com.example.poco.data.assistant.TripSummaryRequest
import com.example.poco.data.record.MotionRecord
import com.example.poco.data.record.TripEntity
import com.example.poco.data.record.TripEventEntity
import com.example.poco.data.record.TripEventType
import com.example.poco.data.record.TripHistoryRepository
import com.example.poco.data.repository.AssistantRepository
import com.example.poco.data.settings.UserSettingsRepository
import com.example.poco.presentation.record.AppliedInterval
import com.example.poco.presentation.record.MotionSicknessRoute
import com.example.poco.presentation.record.TripDetailRoute
import com.example.poco.presentation.record.TripDetailViewModel
import com.example.poco.presentation.record.TripEventsRoute
import com.example.poco.presentation.record.TripHistoryRoute
import com.example.poco.presentation.record.TripHistoryViewModel
import com.example.poco.presentation.record.TripSummaryRoute
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Renders the record screens (Figma 465:48292…48921) from an in-memory fixture so they can be
 * compared with the design without writing anything into the device's real trip database.
 */
class RecordScreensGalleryTest {
    @get:Rule val compose = createComposeRule()

    private val m = 60_000L
    private val now = System.currentTimeMillis()
    private fun trip(id: String, start: Long, minutes: Long, score: Int, signs: String?) =
        TripEntity(
            id = id, petName = "현댕이", petWeightTenths = 123, petSize = "SMALL", seat = "PASSENGER",
            startedAtEpoch = start, endedAtEpoch = start + minutes * m, status = "COMPLETED",
            startRestIntervalMinutes = 120, finalScore = score, restCount = 1, restMs = 16 * m,
            rapidBrakingCount = 1,
            sharpTurnCount = 1,
            speedBumpCount = 2,
            windowWarningCount = 1,
            motionSicknessSigns = signs
        )

    private val trips = MutableStateFlow(
        listOf(
            trip("c", now - 160 * m, 154, 88, "YAWN:3,DROOL:1"),
            trip("b", now - 2 * 24 * 60 * m, 80, 86, "YAWN:1"),
            trip("a", now - 4 * 24 * 60 * m, 25, 92, "FINE")
        )
    )
    private val start = now - 160 * m
    private fun e(
        min: Long,
        type: String,
        delta: Int? = null,
        after: Int? = null,
        detail: String? = null
    ) = TripEventEntity(
        tripId = "c",
        atEpoch = start + min * m,
        type = type,
        scoreDelta = delta,
        scoreAfter = after,
        detail = detail,
        scoreBefore = if (after != null && delta != null) after - delta else null,
        latitude = 37.0, longitude = 127.0, locationAtEpoch = now,
        temperatureC = 23f, windowPercent = 0.0, seat = "PASSENGER", guidanceText = "천천히 가요."
    )

    private val events = listOf(
        e(10, TripEventType.SCORE_PENALTY, -4, 96, "SHARP_TURN"),
        e(20, TripEventType.SCORE_PENALTY, -1, 95, "SPEED_BUMP"),
        e(21, TripEventType.SCORE_PENALTY, -2, 93, "SPEED_BUMP"),
        e(30, TripEventType.WINDOW_WARNING, detail = "50"),
        e(60, TripEventType.REST_STARTED, detail = "WAYPOINT_ARRIVAL"),
        e(63, TripEventType.SCORE_RECOVERY, 1, 94),
        e(66, TripEventType.SCORE_RECOVERY, 1, 95),
        e(76, TripEventType.REST_COMPLETED, detail = "${16 * m}"),
        e(100, TripEventType.SCORE_PENALTY, -7, 88, "RAPID_BRAKING")
    )

    private val history = object : TripHistoryRepository {
        override fun finishedTrips() = trips
        override fun trip(id: String) = trips.map { list -> list.firstOrNull { it.id == id } }
        override fun events(id: String) = flowOf(if (id == "c") events else emptyList())
        override suspend fun setMotionSickness(id: String, record: MotionRecord) {
            trips.value =
                trips.value.map {
                    if (it.id ==
                        id
                    ) {
                        it.copy(motionSicknessSigns = record.encode())
                    } else {
                        it
                    }
                }
        }
        override suspend fun setOneLine(id: String, text: String) {
            trips.value = trips.value.map { if (it.id == id) it.copy(aiOneLine = text) else it }
        }
    }
    private val settings = object : UserSettingsRepository {
        override val restIntervalMinutes = MutableStateFlow(120)
        override val voiceGuidanceEnabled = MutableStateFlow(true)
        override suspend fun setRestIntervalMinutes(minutes: Int) {
            restIntervalMinutes.value =
                minutes
        }
        override suspend fun setVoiceGuidanceEnabled(enabled: Boolean) {
            voiceGuidanceEnabled.value =
                enabled
        }
    }

    /** "오늘 한 줄" AI 응답 흉내 (네트워크 없이) */
    private val assistant = object : AssistantRepository {
        override val configured = true
        override val connected = true
        override val anonymousAuth = true
        override var consented = true
        override suspend fun connect(invitation: String) = Unit
        override suspend fun assist(request: AssistRequest): AssistResponse = error("unused")
        override suspend fun summarizeTrip(request: TripSummaryRequest) =
            "출발 100분쯤 급정거 말고는 평온했어요. 휴식 뒤 점수도 다시 올랐어요."
        override fun disconnect() = Unit
    }

    @Test fun recordScreens() {
        val screen = mutableStateOf("history")
        val detail = TripDetailViewModel(history, settings, assistant)
        val list = TripHistoryViewModel(history)
        compose.setContent {
            POCOTheme {
                when (screen.value) {
                    "history" -> TripHistoryRoute({}, {}, list)
                    "summary" -> TripSummaryRoute("c", {}, {}, detail)
                    "detail" -> TripDetailRoute("c", {}, {}, {}, detail)
                    "events" -> TripEventsRoute("c", {}, detail)
                    "motion" -> MotionSicknessRoute("c", {}, {}, null, {}, {}, detail)
                    "motion-applied" -> MotionSicknessRoute("c", {
                    }, {}, AppliedInterval(120, 90), {}, {}, detail)
                }
            }
        }
        val expected = mapOf(
            "history" to "이동 기록",
            "summary" to "이동기록 보기",
            "detail" to "컴포트 점수 변화",
            "events" to "그때 현댕이는?",
            "motion" to "다음 이동에 이렇게 바꿔 볼까요?",
            "motion-applied" to "적용됨"
        )
        expected.forEach { (name, text) ->
            compose.runOnIdle { screen.value = name }
            compose.waitForIdle()
            assertTrue(
                name,
                compose.onAllNodesWithText(
                    text,
                    substring = true
                ).fetchSemanticsNodes().isNotEmpty()
            )
            capture("record-$name.png")
        }
    }

    /** 주행 완료 화면을 Pleos 화면 분할 크기들로 그려 본다 (전체 · 2분할 · 3분할) */
    @Test fun summarySplitSizes() {
        val size = mutableStateOf(1682 to 1210)
        val detail = TripDetailViewModel(history, settings, assistant)
        compose.setContent {
            POCOTheme {
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier.size(size.value.first.dp, size.value.second.dp)
                ) { TripSummaryRoute("c", {}, {}, detail) }
            }
        }
        listOf(1682 to 1210, 1100 to 1210, 836 to 1210, 555 to 1210, 836 to 600).forEach { (w, h) ->
            compose.runOnIdle { size.value = w to h }
            compose.waitForIdle()
            capture("summary-${w}x$h.png")
        }
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
