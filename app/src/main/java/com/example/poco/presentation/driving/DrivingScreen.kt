package com.example.poco.presentation.driving

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.R
import com.example.poco.core.driving.DrivingGear
import com.example.poco.core.driving.DrivingSettings
import com.example.poco.core.driving.RestPhase
import com.example.poco.core.driving.WindowCloseStatus
import com.example.poco.core.driving.valueOrNull
import com.example.poco.data.model.DrivingSessionState
import com.example.poco.presentation.design.DesignCanvas
import com.example.poco.presentation.design.DrawableIcon
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.design.MgIcon
import com.example.poco.presentation.design.MgIcons
import com.example.poco.presentation.design.Pill
import com.example.poco.presentation.design.PrimaryButton
import com.example.poco.presentation.design.SecondaryButton
import com.example.poco.presentation.driving.component.ComfortCardFull
import com.example.poco.presentation.driving.component.ComfortCardSplit
import com.example.poco.presentation.driving.component.DriveTimeTile
import com.example.poco.presentation.driving.component.GleoSlot
import com.example.poco.presentation.driving.component.RestChecklist
import com.example.poco.presentation.driving.component.RestOrWaypointTile
import com.example.poco.presentation.driving.component.RestPlaceCard
import com.example.poco.presentation.driving.component.RestSummaryFull
import com.example.poco.presentation.driving.component.RestTimerSplit
import com.example.poco.presentation.driving.component.TemperatureTile
import com.example.poco.presentation.driving.component.TileSize
import com.example.poco.presentation.driving.component.WindowTile
import com.example.poco.presentation.driving.component.WindowWarningPanel
import com.example.poco.presentation.driving.component.WindowWarningPopup
import com.example.poco.presentation.driving.component.windowWarningBusy

@Composable
fun DrivingRoute(
    onOpenHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: DrivingViewModel = hiltViewModel(),
    assistant: AssistantViewModel = hiltViewModel()
) {
    val ai by assistant.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, assistant) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) assistant.cancel()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            assistant.cancel()
        }
    }
    var showAiSetup by remember { mutableStateOf(false) }
    val locationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), assistant::locationPermission)
    LaunchedEffect(ai.needsLocationPermission) {
        if (ai.needsLocationPermission) locationPermission.launch("pleos.car.permission.FUSED_LOCATION")
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSetup by rememberSaveable { mutableStateOf(false) }
    var pending by remember { mutableStateOf<DrivingSettings?>(null) }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            pending?.let(viewModel::start)
            pending = null
        }
    DrivingScreen(
        state,
        onStart = { showSetup = true },
        onEnd = viewModel::end,
        onDismissWarning = viewModel::dismissWarning,
        onCloseWindow = viewModel::closeWindow,
        assistantState = ai,
        onAsk = assistant::ask,
        onCancelAi = assistant::cancel,
        onSetupAi = { showAiSetup = true },
        onDismissProposal = assistant::dismissProposal,
        onAddWaypoint = assistant::addWaypoint,
        onCancelWaypoint = viewModel::cancelWaypoint,
        onStartRest = viewModel::startRest,
        onReadyToDepart = viewModel::readyToDepart,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings
    )
    if (showAiSetup) {
        AssistantConnectionDialog(
            ai,
            state.session.gear.valueOrNull() == DrivingGear.PARK,
            { showAiSetup = false },
            assistant::consent,
            assistant::connect,
            assistant::disconnect
        )
    }
    if (showSetup) {
        DrivingSetupDialog(viewModel.savedSettings, onDismiss = { showSetup = false }) {
            showSetup = false
            pending = it
            // Denial still permits the session; speed then explicitly reports unavailable.
            permission.launch("android.car.permission.CAR_SPEED")
        }
    }
}

/** Callbacks shared by both layouts; neither layout owns any timer or calculation. */
data class DrivingActions(
    val onStart: () -> Unit,
    val onEnd: () -> Unit,
    val onAsk: () -> Unit,
    val onCancelAi: () -> Unit,
    val onSetupAi: () -> Unit,
    val onDismissProposal: () -> Unit,
    val onAddWaypoint: () -> Unit,
    val onCancelWaypoint: () -> Unit,
    val onStartRest: () -> Unit,
    val onReadyToDepart: () -> Unit,
    val onOpenHistory: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onDismissWarning: () -> Unit,
    val onCloseWindow: () -> Unit
)

/** Window width below which the app is in the split (navigation + mungo panel) layout. */
internal val SplitLayoutMaxWidth = 1000.dp

/** Figma app area (header + content) plus the outer margin: full 1172 x 980, split 578 x 980. */
private const val FullRefWidth = 1220f
private const val SplitRefWidth = 618f
private const val RefHeight = 1028f

@Composable
fun DrivingScreen(
    uiState: DrivingUiState,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    onDismissWarning: () -> Unit,
    onCloseWindow: () -> Unit,
    modifier: Modifier = Modifier,
    assistantState: AssistantUiState = AssistantUiState(),
    onAsk: () -> Unit = {},
    onCancelAi: () -> Unit = {},
    onSetupAi: () -> Unit = {},
    onDismissProposal: () -> Unit = {},
    onAddWaypoint: () -> Unit = {},
    onCancelWaypoint: () -> Unit = {},
    onStartRest: () -> Unit = {},
    onReadyToDepart: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val state = uiState.session
    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    val actions = DrivingActions(
        onStart, { confirmEnd = true }, onAsk, onCancelAi, onSetupAi, onDismissProposal, onAddWaypoint,
        onCancelWaypoint, onStartRest, onReadyToDepart, onOpenHistory, onOpenSettings, onDismissWarning, onCloseWindow
    )
    BoxWithConstraints(modifier.fillMaxSize().background(Mg.Bg)) {
        val split = maxWidth < SplitLayoutMaxWidth
        // Each layout has its own reference size; the split panel is not a scaled-down full screen.
        DesignCanvas(if (split) SplitRefWidth else FullRefWidth, RefHeight) {
            Column(Modifier.fillMaxSize().padding(horizontal = if (split) 20.dp else 24.dp, vertical = 24.dp)) {
                DrivingHeader(state, assistantState, actions)
                Spacer(Modifier.height(22.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (split) SplitDrivingLayout(state, assistantState, actions) else FullDrivingLayout(state, assistantState, actions)
                }
            }
            if (state.windowWarning && !split) {
                WindowWarningDialog(state, onDismissWarning, onCloseWindow)
            }
        }
        if (confirmEnd) {
            AlertDialog(
                onDismissRequest = { confirmEnd = false },
                title = { Text("주행을 종료할까요?") },
                text = { Text("이번 주행 기록을 저장하고 요약을 보여 드려요.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmEnd = false
                        onEnd()
                    }) { Text("주행 종료") }
                },
                dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("계속 주행") } }
            )
        }
    }
}

/** Figma "멍고 헤더": paw + mungo, status chip, then the round Gleo (voice) button. */
@Composable
private fun DrivingHeader(state: DrivingSessionState, ai: AssistantUiState, actions: DrivingActions) {
    val resting = state.active && state.rest.phase != RestPhase.DRIVING
    val parked = state.gear.valueOrNull() == DrivingGear.PARK
    Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
        DrawableIcon(R.drawable.driving_paw, size = 20.dp)
        Spacer(Modifier.width(9.dp))
        Text("mungo", style = Mg.bold(27, 34), color = Mg.Ink)
        Spacer(Modifier.width(16.dp))
        when {
            !state.active -> Pill("주행 준비", background = Mg.Chip, color = Mg.Muted, height = 40.dp)
            resting -> Pill(
                "휴식 중", background = Mg.Peach, color = Mg.OrangeText, height = 40.dp,
                leading = { DrawableIcon(R.drawable.driving_cup, Mg.OrangeText, 16.dp) }
            )
            else -> Pill("주행 중", background = Mg.Peach, color = Mg.OrangeText, height = 40.dp)
        }
        if (state.simulatedSource) {
            Spacer(Modifier.width(10.dp))
            Pill("에뮬레이터 데이터", background = Color(0xFFFFF4E8), color = Mg.OrangeText, height = 40.dp, style = Mg.Caption)
        }
        Spacer(Modifier.weight(1f))
        if (!state.active) {
            HeaderLink("이동 기록", actions.onOpenHistory)
            HeaderLink("설정", actions.onOpenSettings)
        } else {
            if (state.rest.phase == RestPhase.DRIVING && parked) HeaderLink("휴식 시작", actions.onStartRest)
            HeaderLink("주행 종료", actions.onEnd)
        }
        Spacer(Modifier.width(8.dp))
        GleoButton(state, ai, actions)
    }
}

@Composable
private fun HeaderLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        Modifier.clip(CircleShape).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        style = Mg.BodyStrong,
        color = Mg.Muted
    )
}

@Composable
private fun GleoButton(state: DrivingSessionState, ai: AssistantUiState, actions: DrivingActions) {
    val ready = ai.configured && ai.consented && (ai.connected || ai.anonymousAuth)
    val listening = ai.phase != AssistantPhase.IDLE
    Box(
        Modifier.size(60.dp).clip(CircleShape).background(if (listening) Mg.Orange else Color.White, CircleShape)
            .clickable {
                when {
                    listening -> actions.onCancelAi()
                    !ready -> actions.onSetupAi()
                    state.active -> actions.onAsk()
                    else -> actions.onSetupAi()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        MgIcon(MgIcons.Mic, if (listening) Color.White else Mg.Ink, 24.dp)
    }
}

@Composable
private fun FullDrivingLayout(state: DrivingSessionState, ai: AssistantUiState, actions: DrivingActions) {
    val resting = state.active && state.rest.phase != RestPhase.DRIVING
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        if (resting) {
            RestSummaryFull(state, Modifier.width(420.dp).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight()) {
                RestChecklist(state, compact = false, modifier = Modifier.fillMaxWidth().height(386.dp))
                Spacer(Modifier.weight(1f))
                GleoSlot(state, ai, actions, compact = false, modifier = Modifier.fillMaxWidth().height(186.dp), restMode = true)
                Spacer(Modifier.weight(1f))
                RestPlaceCard(state, compact = false, modifier = Modifier.fillMaxWidth().height(120.dp))
                Spacer(Modifier.weight(1f))
                RestDepartButton(state, actions, Modifier.fillMaxWidth().height(96.dp))
            }
        } else {
            ComfortCardFull(state, Modifier.width(420.dp).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().height(218.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    RestOrWaypointTile(state, TileSize.Full, Modifier.weight(1f).fillMaxHeight())
                    DriveTimeTile(state, TileSize.Full, Modifier.weight(1f).fillMaxHeight())
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth().height(218.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    TemperatureTile(state, TileSize.Full, Modifier.weight(1f).fillMaxHeight())
                    WindowTile(state, TileSize.Full, Modifier.weight(1f).fillMaxHeight())
                }
                Spacer(Modifier.height(20.dp))
                GleoSlot(state, ai, actions, compact = false, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

/**
 * Split screen: mungo is the right-hand panel next to the Pleos navigation map (Figma 464:46962).
 * Status card, 2 x 2 compact tiles and one guidance slot; the map itself belongs to Navi.
 */
@Composable
private fun SplitDrivingLayout(state: DrivingSessionState, ai: AssistantUiState, actions: DrivingActions) {
    if (state.windowWarning) {
        WindowWarningPanel(state, actions.onDismissWarning, actions.onCloseWindow, Modifier.fillMaxSize())
        return
    }
    val resting = state.active && state.rest.phase != RestPhase.DRIVING
    Column(Modifier.fillMaxSize()) {
        if (resting) {
            RestTimerSplit(state, Modifier.fillMaxWidth().height(196.dp))
            Spacer(Modifier.height(16.dp))
            RestChecklist(state, compact = true, modifier = Modifier.fillMaxWidth().height(316.dp))
            Spacer(Modifier.height(16.dp))
            RestPlaceCard(state, compact = true, modifier = Modifier.fillMaxWidth().height(112.dp))
            Spacer(Modifier.height(16.dp))
            GleoSlot(state, ai, actions, compact = true, modifier = Modifier.fillMaxWidth().weight(1f), restMode = true)
            Spacer(Modifier.height(20.dp))
            RestDepartButton(state, actions, Modifier.fillMaxWidth().height(96.dp))
        } else {
            ComfortCardSplit(state, Modifier.fillMaxWidth().height(196.dp))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RestOrWaypointTile(state, TileSize.Split, Modifier.weight(1f).fillMaxHeight())
                DriveTimeTile(state, TileSize.Split, Modifier.weight(1f).fillMaxHeight())
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TemperatureTile(state, TileSize.Split, Modifier.weight(1f).fillMaxHeight())
                WindowTile(state, TileSize.Split, Modifier.weight(1f).fillMaxHeight())
            }
            Spacer(Modifier.height(16.dp))
            GleoSlot(state, ai, actions, compact = true, modifier = Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun RestDepartButton(state: DrivingSessionState, actions: DrivingActions, modifier: Modifier) {
    if (state.rest.phase == RestPhase.READY_TO_DEPART) {
        Box(
            modifier.background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "출발 준비 완료 · 주행을 시작하면 재출발로 기록돼요",
                style = Mg.BodyStrong,
                color = Mg.OrangeText,
                textAlign = TextAlign.Center
            )
        }
    } else {
        PrimaryButton("다시 출발 준비", actions.onReadyToDepart, modifier, style = Mg.semi(23, 30))
    }
}

/** Full layout: modal popup over the mungo area (Figma 464:46215). */
@Composable
private fun WindowWarningDialog(state: DrivingSessionState, onDismiss: () -> Unit, onClose: () -> Unit) {
    val busy = windowWarningBusy(state)
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        WindowWarningPopup(state, onDismiss, onClose)
    }
}

@Composable
private fun DrivingSetupDialog(
    saved: DrivingSettings?,
    onDismiss: () -> Unit,
    onStart: (DrivingSettings) -> Unit
) {
    var draft by remember { mutableStateOf(saved) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("주행 준비") },
        text = { PetSettingsForm(saved) { draft = it } },
        confirmButton = {
            TextButton(onClick = { draft?.let(onStart) }, enabled = draft != null) { Text("주행 시작") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

/** Idle driving slot: start a trip, or open history/settings (no session values are invented). */
@Composable
internal fun IdleStartCard(actions: DrivingActions, compact: Boolean, modifier: Modifier) {
    Column(
        modifier.background(Color.White, RoundedCornerShape(Mg.Radius)).padding(if (compact) 24.dp else 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("반려견과 출발할 준비가 됐나요?", style = Mg.semi(if (compact) 25 else 27, 36), color = Mg.Ink)
        Text("주행을 시작하면 컴포트 점수와 휴식 시간을 함께 챙길게요.", style = Mg.Body, color = Mg.Muted)
        Spacer(Modifier.weight(1f))
        PrimaryButton("주행 시작", actions.onStart, Modifier.fillMaxWidth().height(96.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("이동 기록", actions.onOpenHistory, Modifier.weight(1f).height(72.dp))
            SecondaryButton("설정", actions.onOpenSettings, Modifier.weight(1f).height(72.dp))
        }
    }
}
