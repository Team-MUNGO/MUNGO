package com.example.poco.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.theme.POCOTheme
import com.example.poco.presentation.driving.DrivingRoute
import com.example.poco.presentation.driving.DrivingViewModel
import com.example.poco.presentation.record.AppliedInterval
import com.example.poco.presentation.record.MotionSicknessRoute
import com.example.poco.presentation.record.PetProfileRoute
import com.example.poco.presentation.record.RailTab
import com.example.poco.presentation.record.RestIntervalRoute
import com.example.poco.presentation.record.SettingsRoute
import com.example.poco.presentation.record.TripDetailRoute
import com.example.poco.presentation.record.TripEventsRoute
import com.example.poco.presentation.record.TripHistoryRoute
import com.example.poco.presentation.record.TripSummaryRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { POCOTheme { PocoApp() } }
    }
}

/**
 * Routes: "driving", "history", "settings", "pet", "summary:<id>", "detail:<id>", "events:<id>",
 * "motion:<id>", "interval:<tripId|->:<recommended|->".
 */
@Composable
fun PocoApp(driving: DrivingViewModel = hiltViewModel()) {
    var route by rememberSaveable { mutableStateOf("driving") }
    var summarized by rememberSaveable { mutableStateOf<String?>(null) }
    var appliedFrom by rememberSaveable { mutableStateOf<Int?>(null) }
    var appliedTo by rememberSaveable { mutableStateOf<Int?>(null) }
    val state by driving.uiState.collectAsStateWithLifecycle()
    val ended = state.session.lastEndedTripId
    LaunchedEffect(ended) {
        // Only a newly ended session opens its summary (rotation/recomposition cannot reopen it).
        if (ended != null && ended != summarized) {
            summarized = ended
            route = "summary:$ended"
        }
    }
    val rail: (RailTab) -> Unit = {
        route = when (it) {
            RailTab.HOME -> "driving"
            RailTab.HISTORY -> "history"
            RailTab.PET -> "pet"
            RailTab.SETTINGS -> "settings"
        }
    }
    fun back() {
        route = when {
            route.startsWith("events:") || route.startsWith("motion:") -> "detail:" + route.substringAfter(":")
            route.startsWith("interval:") -> route.removePrefix("interval:").substringBefore(":").let { if (it == "-") "settings" else "motion:$it" }
            route.startsWith("detail:") -> "history"
            else -> "driving"
        }
    }
    BackHandler(enabled = route != "driving") { back() }
    when {
        route == "history" -> TripHistoryRoute(onOpen = { route = "detail:$it" }, onNavigate = rail)
        route == "settings" -> SettingsRoute(onNavigate = rail, onOpenInterval = { route = "interval:-:-" })
        route == "pet" -> PetProfileRoute(onNavigate = rail)
        route.startsWith("summary:") -> route.removePrefix("summary:").let { id ->
            TripSummaryRoute(id, onDetail = { route = "detail:$id" }, onDone = { route = "driving" })
        }
        route.startsWith("detail:") -> route.removePrefix("detail:").let { id ->
            TripDetailRoute(id, onBack = ::back, onEvents = { route = "events:$id" }, onMotion = { route = "motion:$id" })
        }
        route.startsWith("events:") -> TripEventsRoute(route.removePrefix("events:"), onBack = ::back)
        route.startsWith("motion:") -> route.removePrefix("motion:").let { id ->
            val applied = if (appliedFrom != null && appliedTo != null) AppliedInterval(appliedFrom!!, appliedTo!!) else null
            MotionSicknessRoute(
                id,
                onBack = ::back,
                onOpenInterval = { route = "interval:$id:$it" },
                applied = applied,
                onUndo = { change ->
                    driving.setRestInterval(change.from)
                    appliedFrom = null
                    appliedTo = null
                },
                onToastShown = {}
            )
        }
        route.startsWith("interval:") -> route.removePrefix("interval:").split(":").let { parts ->
            val tripId = parts.getOrNull(0)?.takeIf { it != "-" }
            RestIntervalRoute(
                recommended = parts.getOrNull(1)?.toIntOrNull(),
                tripId = tripId,
                onCancel = ::back,
                onApplied = { change ->
                    if (tripId != null) {
                        appliedFrom = change.from
                        appliedTo = change.to
                    }
                    back()
                }
            )
        }
        else -> DrivingRoute(
            onOpenHistory = { route = "history" },
            onOpenSettings = { route = "settings" },
            viewModel = driving
        )
    }
}
