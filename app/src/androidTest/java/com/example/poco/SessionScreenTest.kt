package com.example.poco

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.PetSeat
import com.example.poco.core.driving.Reading
import com.example.poco.core.driving.WindowCloseStatus
import com.example.poco.core.theme.POCOTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.poco.data.assistant.AssistPlace
import com.example.poco.data.assistant.AssistResponse
import com.example.poco.data.assistant.AssistSources
import com.example.poco.data.model.PlaceProposal
import com.example.poco.data.model.RestUiState
import com.example.poco.data.model.DrivingSessionState
import com.example.poco.presentation.session.AssistantUiState
import com.example.poco.presentation.session.AssistantPhase
import com.example.poco.presentation.session.SessionScreen
import com.example.poco.presentation.session.SessionUiState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SessionScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyResponsePreservesTileGeometry() {
        val state = mutableStateOf(
            SessionUiState(
                DrivingSessionState(
                    active = true,
                    settings = DrivingSettings("현댕이", PetSeat.PASSENGER, 22f, 24f),
                    temperature = Reading.Value(23f),
                    windowPercent = Reading.Value(0.0)
                )
            )
        )
        val ai = mutableStateOf(AssistantUiState(configured = true, consented = true, anonymousAuth = true))
        compose.setContent { POCOTheme { SessionScreen(state.value, {}, {}, {}, {}, assistantState = ai.value) } }
        val before = compose.onNodeWithText("실내 온도").fetchSemanticsNode().boundsInRoot
        // Figma 488:10266: 말한 안내가 없어도 음성 카드는 지금 상태 문장을, 아래엔 질문 안내 줄을 늘 보여 준다
        compose.onNodeWithText("현댕이가 편안하게 가고 있어요", substring = true).assertExists()
        val mungoLabels = compose.onAllNodesWithText("mungo").fetchSemanticsNodes().size
        compose.onNodeWithText("“mungo, 현댕이 괜찮아?”").assertExists()
        capture("driving-empty.png")
        compose.runOnIdle { ai.value = ai.value.copy(phase = AssistantPhase.LISTENING, question = "mungo, 현댕이 괜찮아?") }
        compose.onNodeWithText("“mungo, 현댕이 괜찮아?”").assertExists()
        assertEquals(mungoLabels, compose.onAllNodesWithText("mungo").fetchSemanticsNodes().size)
        val questionBeforeAnswer = compose.onNodeWithText("“mungo, 현댕이 괜찮아?”").fetchSemanticsNode().boundsInRoot
        capture("driving-listening.png")
        compose.runOnIdle { ai.value = ai.value.copy(phase = AssistantPhase.THINKING) }
        compose.onNodeWithText("“mungo, 현댕이 괜찮아?”").assertExists()
        compose.runOnIdle {
            ai.value = ai.value.copy(phase = AssistantPhase.IDLE, answerText = "새로운 mungo 응답입니다.")
            state.value = SessionUiState(state.value.session.copy(gleoResponse = "새로운 mungo 응답입니다."))
        }
        compose.onNodeWithText("“새로운 mungo 응답입니다.”").assertExists()
        val answer = compose.onNodeWithText("“새로운 mungo 응답입니다.”").fetchSemanticsNode().boundsInRoot
        val question = compose.onNodeWithText("“mungo, 현댕이 괜찮아?”").fetchSemanticsNode().boundsInRoot
        assertTrue(answer.bottom < question.top)
        // 카드 · 질문 줄 자리가 고정이라 답이 와도 질문 줄은 움직이지 않는다
        assertEquals(questionBeforeAnswer.top, question.top)
        capture("driving-question-answer.png")
        val after = compose.onNodeWithText("실내 온도").fetchSemanticsNode().boundsInRoot
        assertEquals(before, after)
    }

    @Test fun actualTtsShowsGuidanceAndIdleDoesNotShowStaleResponse() {
        val session = mutableStateOf(DrivingSessionState(active = true, gleoResponse = "실제 음성 안내", ttsSpeaking = true))
        compose.setContent { POCOTheme { SessionScreen(SessionUiState(session.value), {}, {}, {}, {}) } }
        compose.onNodeWithText("“실제 음성 안내”").assertExists()
        compose.runOnIdle { session.value = session.value.copy(ttsSpeaking = false) }
        compose.onNodeWithText("“실제 음성 안내”").assertDoesNotExist()
    }

    @Test fun proposalPreservesTileGeometryAndUnverifiedPlaceIsDisabled() {
        val session = mutableStateOf(DrivingSessionState(active = true))
        compose.setContent { POCOTheme { SessionScreen(SessionUiState(session.value), {}, {}, {}, {}) } }
        val before = compose.onNodeWithText("실내 온도").fetchSemanticsNode().boundsInRoot
        val place = AssistPlace("ChIJtestfixture0001", "테스트 전용 휴게소", 37.0, 127.0, null, null, canAddWaypoint = false)
        compose.runOnIdle {
            session.value = session.value.copy(
                proposal = PlaceProposal(
                    "rest-1",
                    AssistResponse("r", "rest-1", "REST_RECOMMENDATION", "OK", "주변 후보예요.", "주변 후보예요.", listOf(place), place.placeId, null, AssistSources("gemini", "google_maps")),
                    automatic = true
                )
            )
        }
        compose.onNodeWithText("테스트 전용 휴게소", substring = true).assertExists()
        compose.onNodeWithText("경유지 추가 불가").assertIsNotEnabled()
        assertEquals(before, compose.onNodeWithText("실내 온도").fetchSemanticsNode().boundsInRoot)
        capture("driving-proposal.png")
    }

    /** #5/#16: full and split layouts render the same session values (no per-layout state). */
    @Test fun fullAndSplitLayoutsShowSameSessionState() {
        val session = DrivingSessionState(
            active = true,
            settings = DrivingSettings("현댕이", PetSeat.PASSENGER, 22f, 24f),
            temperature = Reading.Value(23f),
            windowPercent = Reading.Value(0.0),
            rest = RestUiState(intervalMinutes = 90, remainingMs = 35 * 60_000L)
        )
        val narrow = mutableStateOf(false)
        compose.setContent {
            POCOTheme {
                Box(Modifier.width(if (narrow.value) 560.dp else 1280.dp).fillMaxHeight()) {
                    SessionScreen(SessionUiState(session), {}, {}, {}, {})
                }
            }
        }
        listOf("35", "분", "23", "°C", "닫힘").forEach { assertTrue(it, exists(it)) }
        capture("driving-full.png")
        compose.runOnIdle { narrow.value = true }
        listOf("35", "분", "23", "°C", "닫힘").forEach { assertTrue(it, exists(it)) }
        capture("driving-split.png")
    }

    @Test fun windowFailureRemainsVisibleWithRetry() {
        compose.setContent {
            POCOTheme {
                SessionScreen(
                    SessionUiState(
                        DrivingSessionState(
                            active = true,
                            settings = DrivingSettings("현댕이", PetSeat.PASSENGER, 22f, 24f),
                            windowPercent = Reading.Value(50.0),
                            windowWarning = true,
                            canCloseWindow = true,
                            closeStatus = WindowCloseStatus.FAILED,
                            closeError = "창문을 닫지 못했어요."
                        )
                    ),
                    {},
                    {},
                    {},
                    {}
                )
            }
        }
        compose.onNodeWithText("다시 닫기").assertExists()
        compose.onNodeWithText("창문을 닫지 못했어요.").assertExists()
        val bitmap = compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "driving-window-warning.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    /** Visual check against Figma Page 3: each state in the full and split layout. */
    @Test fun designStatesGallery() {
        val base = DrivingSessionState(
            active = true,
            settings = DrivingSettings("현댕이", PetSeat.PASSENGER, 22f, 24f, 123),
            temperature = Reading.Value(25f),
            windowPercent = Reading.Value(0.0),
            gear = Reading.Value(com.example.poco.core.driving.DrivingGear.DRIVE),
            elapsedMs = 87 * 60_000L,
            startedAtEpoch = System.currentTimeMillis() - 87 * 60_000L,
            comfort = com.example.poco.core.driving.ComfortScoreState(score = 82, brakingCount = 1, turnCount = 1, bumpCount = 4),
            rest = RestUiState(intervalMinutes = 120, remainingMs = 20 * 60_000L, nextRestAtEpoch = System.currentTimeMillis() + 20 * 60_000L)
        )
        val place = AssistPlace("ChIJvSMioIapZDURijUJ9O6phq8", "덕평휴게소", 37.24, 127.39, null, null, canAddWaypoint = true)
        val proposal = PlaceProposal(
            "rest-1",
            AssistResponse("r", "rest-1", "REST_RECOMMENDATION", "OK", "반려견과 쉬기 좋은 휴게소 후보예요.", "반려견과 쉬기 좋은 휴게소 후보예요.", listOf(place), place.placeId, null, AssistSources("gemini", "google_maps")),
            automatic = true
        )
        val states = mapOf(
            "proposal" to base.copy(proposal = proposal, ttsSpeaking = true),
            "waypoint" to base.copy(
                temperature = Reading.Value(24f), comfort = base.comfort.copy(score = 81),
                waypoint = com.example.poco.data.model.WaypointUiState.Added("덕평휴게소", "15분"),
                gleoResponse = "15분 뒤 덕평휴게소에 들를게요. 도착하면 물부터 챙겨 주세요."
            ),
            "window" to base.copy(temperature = Reading.Value(24f), comfort = base.comfort.copy(score = 92), windowPercent = Reading.Value(50.0), windowWarning = true, canCloseWindow = true, ttsSpeaking = true),
            "resting" to base.copy(
                gear = Reading.Value(com.example.poco.core.driving.DrivingGear.PARK),
                comfort = base.comfort.copy(score = 83),
                waypoint = com.example.poco.data.model.WaypointUiState.Arrived("덕평휴게소"),
                rest = RestUiState(phase = com.example.poco.core.driving.RestPhase.RESTING, currentRestMs = 7 * 60_000L + 12_000, restStartedAtEpoch = 1L, scoreAtRestStart = 81)
            )
        )
        val current = mutableStateOf(states.getValue("proposal"))
        val narrow = mutableStateOf(false)
        compose.setContent {
            POCOTheme {
                Box(Modifier.width(if (narrow.value) 600.dp else 1280.dp).fillMaxHeight()) {
                    SessionScreen(SessionUiState(current.value), {}, {}, {}, {})
                }
            }
        }
        states.forEach { (name, state) ->
            if (name == "window") return@forEach
            compose.runOnIdle { current.value = state; narrow.value = false }
            capture("design-$name-full.png")
            compose.runOnIdle { narrow.value = true }
            capture("design-$name-split.png")
        }
        compose.runOnIdle { current.value = states.getValue("window"); narrow.value = true }
        capture("design-window-split.png")
    }

    private fun exists(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
