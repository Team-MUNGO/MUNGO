package com.example.poco.presentation.session

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.R
import com.example.poco.core.driving.DrivingGear
import com.example.poco.core.driving.RestPhase
import com.example.poco.core.driving.valueOrNull
import com.example.poco.data.assistant.FUSED_LOCATION_PERMISSION
import com.example.poco.data.model.DrivingSessionState
import com.example.poco.presentation.design.DesignCanvas
import com.example.poco.presentation.design.DrawableIcon
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.design.MgAlertDialog
import com.example.poco.presentation.design.MgIcon
import com.example.poco.presentation.design.MgIcons
import com.example.poco.presentation.design.Pill
import com.example.poco.presentation.design.PrimaryButton
import com.example.poco.presentation.design.ScrimDialog
import com.example.poco.presentation.design.SecondaryButton
import com.example.poco.presentation.session.component.ComfortCardFull
import com.example.poco.presentation.session.component.ComfortCardSplit
import com.example.poco.presentation.session.component.DriveTimeTile
import com.example.poco.presentation.session.component.GleoSlot
import com.example.poco.presentation.session.component.RestChecklist
import com.example.poco.presentation.session.component.RestOrWaypointTile
import com.example.poco.presentation.session.component.RestPlaceCard
import com.example.poco.presentation.session.component.RestSummaryFull
import com.example.poco.presentation.session.component.RestTimerSplit
import com.example.poco.presentation.session.component.TemperatureDangerPopup
import com.example.poco.presentation.session.component.TemperatureTile
import com.example.poco.presentation.session.component.TileSize
import com.example.poco.presentation.session.component.WindowTile
import com.example.poco.presentation.session.component.WindowWarningPanel
import com.example.poco.presentation.session.component.WindowWarningPopup
import com.example.poco.presentation.session.component.windowWarningBusy

@Composable
fun SessionRoute(
    onOpenHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    /** 탑승 준비에서 '주행 시작'을 눌렀거나 기어 D로 들어왔으면 바로 세션을 시작한다 */
    autoStart: Boolean = false,
    /** 반려견 프로필이 없으면 마이펫(팀 화면)에서 등록한다 */
    onOpenPetProfile: () -> Unit = {},
    viewModel: SessionViewModel = hiltViewModel(),
    assistant: AssistantViewModel = hiltViewModel()
) {
    val ai by assistant.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, assistant) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) assistant.cancel()
            // 앱 설정에서 위치 권한을 켜고 돌아오면 보류한 휴식 제안을 이어서 찾는다
            if (event == Lifecycle.Event.ON_RESUME) assistant.onResumed()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            assistant.cancel()
        }
    }
    var showAiSetup by remember { mutableStateOf(false) }
    val activity = LocalContext.current.findActivity()
    val locationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            assistant.locationPermission(granted)
            // 첫 실행에서 거부한 뒤 '다시 묻지 않음' 상태면 시스템 창이 뜨지 않는다 → 앱 설정의 권한 화면으로 보낸다
            if (!granted &&
                activity != null &&
                !activity.shouldShowRequestPermissionRationale(FUSED_LOCATION_PERMISSION)
            ) {
                runCatching {
                    activity.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", activity.packageName, null)
                        )
                    )
                }
            }
        }
    LaunchedEffect(ai.needsLocationPermission) {
        if (ai.needsLocationPermission) locationPermission.launch(FUSED_LOCATION_PERMISSION)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    // Denial still permits the session; speed then explicitly reports unavailable.
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.startWithProfile()
        }
    val start = {
        when (profile) {
            is SessionProfile.Ready -> permission.launch(
                arrayOf("android.car.permission.CAR_SPEED", "pleos.car.permission.FUSED_LOCATION")
            )
            SessionProfile.Missing -> onOpenPetProfile()
            SessionProfile.Loading -> Unit
        }
    }
    var autoStarted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(autoStart, profile, state.session.active) {
        // 이미 주행 중에 들어온 화면(내비 보고 돌아옴 등)은 자동 시작을 한 것으로 본다.
        // 안 그러면 주행을 끝내는 순간 active=false가 되면서 새 주행이 바로 시작된다
        if (state.session.active) autoStarted = true
        if (autoStart && !autoStarted && !state.session.active && profile is SessionProfile.Ready) {
            autoStarted = true
            start()
        }
    }
    SessionScreen(
        state,
        onStart = start,
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
        onOpenSettings = onOpenSettings,
        onAllowLocation = assistant::requestRestLocation,
        onDismissTemperature = viewModel::dismissTemperatureDanger
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
}

/** Callbacks shared by both layouts; neither layout owns any timer or calculation. */
data class SessionActions(
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
    val onCloseWindow: () -> Unit,
    /** 휴식 제안이 위치 권한 때문에 보류됐을 때 '위치 권한 허용' */
    val onAllowLocation: () -> Unit = {}
)

/** Window width below which the app is in the split (navigation + mungo panel) layout. */
internal val SplitLayoutMaxWidth = 1000.dp

/** Figma app area (header + content) plus the outer margin: full 1172 x 980, split 578 x 980. */
private const val FULL_REF_WIDTH = 1220f
private const val SPLIT_REF_WIDTH = 618f
private const val REF_HEIGHT = 1028f

@Composable
fun SessionScreen(
    uiState: SessionUiState,
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
    onOpenSettings: () -> Unit = {},
    onAllowLocation: () -> Unit = {},
    onDismissTemperature: () -> Unit = {}
) {
    val state = uiState.session
    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    val actions = SessionActions(
        onStart, {
            confirmEnd = true
        }, onAsk, onCancelAi, onSetupAi, onDismissProposal, onAddWaypoint,
        onCancelWaypoint,
        onStartRest,
        onReadyToDepart,
        onOpenHistory,
        onOpenSettings,
        onDismissWarning,
        onCloseWindow,
        onAllowLocation
    )
    BoxWithConstraints(modifier.fillMaxSize().background(Mg.Bg)) {
        val split = maxWidth < SplitLayoutMaxWidth
        // Each layout has its own reference size; the split panel is not a scaled-down full screen.
        DesignCanvas(if (split) SPLIT_REF_WIDTH else FULL_REF_WIDTH, REF_HEIGHT) {
            Column(
                Modifier.fillMaxSize().padding(
                    horizontal = if (split) 20.dp else 24.dp,
                    vertical = 24.dp
                )
            ) {
                DrivingHeader(state, assistantState, actions, showStatus = split)
                Spacer(Modifier.height(22.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (split) {
                        SplitDrivingLayout(
                            state,
                            assistantState,
                            actions
                        )
                    } else {
                        FullDrivingLayout(state, assistantState, actions)
                    }
                }
            }
            if (state.windowWarning && !split) {
                WindowWarningDialog(state, onDismissWarning, onCloseWindow)
            }
            if (state.temperatureDanger && !state.windowWarning) {
                ScrimDialog(onDismiss = onDismissTemperature) {
                    TemperatureDangerPopup(state, onDismissTemperature)
                }
            }
        }
        if (confirmEnd) {
            EndConfirmDialog(
                onDismiss = { confirmEnd = false },
                onEnd = {
                    confirmEnd = false
                    onEnd()
                }
            )
        }
    }
}

/** Figma "멍고 헤더": paw + mungo, status chip, then the round Gleo (voice) button. */
@Composable
private fun DrivingHeader(
    state: DrivingSessionState,
    ai: AssistantUiState,
    actions: SessionActions,
    showStatus: Boolean
) {
    val resting = state.active && state.rest.phase != RestPhase.DRIVING
    val parked = state.gear.valueOrNull() == DrivingGear.PARK
    Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
        DrawableIcon(R.drawable.driving_paw, size = 20.dp)
        Spacer(Modifier.width(9.dp))
        Text("mungo", style = Mg.bold(28, 35), color = Mg.Ink)
        Spacer(Modifier.width(16.dp))
        // Figma 488:10266 · 488:10634: 전체 화면에선 상태 칩이 왼쪽 카드 맨 위로 옮겨 갔다 (헤더엔 로고만)
        if (showStatus) {
            when {
                !state.active -> Pill(
                    "주행 준비",
                    background = Mg.Chip,
                    color = Mg.Muted,
                    height = 40.dp
                )
                resting -> Pill(
                    "휴식 중",
                    background = Mg.Peach,
                    color = Mg.OrangeText,
                    height = 40.dp,
                    leading = { DrawableIcon(R.drawable.driving_cup, Mg.OrangeText, 16.dp) }
                )
                else -> Pill("주행 중", background = Mg.Peach, color = Mg.OrangeText, height = 40.dp)
            }
        }
        Spacer(Modifier.weight(1f))
        if (!state.active) {
            HeaderLink("이동 기록", actions.onOpenHistory)
            HeaderLink("설정", actions.onOpenSettings)
        } else {
            if (state.rest.phase == RestPhase.DRIVING &&
                parked
            ) {
                HeaderLink("휴식 시작", actions.onStartRest)
            }
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
        Modifier.clip(
            CircleShape
        ).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        style = Mg.BodyStrong,
        color = Mg.Muted
    )
}

@Composable
private fun GleoButton(state: DrivingSessionState, ai: AssistantUiState, actions: SessionActions) {
    val ready = ai.configured && ai.consented && (ai.connected || ai.anonymousAuth)
    val listening = ai.phase != AssistantPhase.IDLE
    // 음성 안내가 꺼져 있으면 회색 · 누를 수 없음 (설정 > 음성 및 알림)
    val voiceOn = com.example.poco.core.designsystem.LocalVoiceEnabled.current
    Box(
        Modifier.size(
            60.dp
        ).clip(CircleShape).background(
            when {
                !voiceOn -> Mg.Chip
                listening -> Mg.Orange
                else -> Color.White
            },
            CircleShape
        )
            .clickable(enabled = voiceOn) {
                when {
                    listening -> actions.onCancelAi()
                    !ready -> actions.onSetupAi()
                    state.active -> actions.onAsk()
                    else -> actions.onSetupAi()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        MgIcon(
            MgIcons.Mic,
            when {
                !voiceOn -> Mg.BarGrey
                listening -> Color.White
                else -> Mg.Ink
            },
            24.dp
        )
    }
}

@Composable
private fun FullDrivingLayout(
    state: DrivingSessionState,
    ai: AssistantUiState,
    actions: SessionActions
) {
    val resting = state.active && state.rest.phase != RestPhase.DRIVING
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        if (resting) {
            RestSummaryFull(state, Modifier.width(420.dp).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight()) {
                // Figma 488:10672 ~ 488:10701: 412 · 30 · 186 · 29 · 120 · 25 · 96
                RestChecklist(
                    state,
                    compact = false,
                    modifier = Modifier.fillMaxWidth().height(412.dp)
                )
                Spacer(Modifier.height(30.dp))
                GleoSlot(
                    state,
                    ai,
                    actions,
                    compact = false,
                    modifier = Modifier.fillMaxWidth().height(186.dp),
                    restMode = true
                )
                Spacer(Modifier.height(29.dp))
                RestPlaceCard(
                    state,
                    compact = false,
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
                Spacer(Modifier.weight(1f))
                RestDepartButton(state, actions, Modifier.fillMaxWidth().height(96.dp))
            }
        } else {
            ComfortCardFull(state, Modifier.width(420.dp).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    Modifier.fillMaxWidth().height(218.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    RestOrWaypointTile(state, TileSize.Full, Modifier.weight(1f).fillMaxHeight())
                    DriveTimeTile(state, TileSize.Full, Modifier.weight(1f).fillMaxHeight())
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth().height(218.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    TemperatureTile(state, TileSize.Full, Modifier.weight(1f).fillMaxHeight())
                    WindowTile(state, TileSize.Full, Modifier.weight(1f).fillMaxHeight())
                }
                Spacer(Modifier.height(20.dp))
                GleoSlot(
                    state,
                    ai,
                    actions,
                    compact = false,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }
}

/**
 * Split screen: mungo is the right-hand panel next to the Pleos navigation map (Figma 464:46962).
 * Status card, 2 x 2 compact tiles and one guidance slot; the map itself belongs to Navi.
 */
@Composable
private fun SplitDrivingLayout(
    state: DrivingSessionState,
    ai: AssistantUiState,
    actions: SessionActions
) {
    if (state.windowWarning) {
        WindowWarningPanel(
            state,
            actions.onDismissWarning,
            actions.onCloseWindow,
            Modifier.fillMaxSize()
        )
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
            GleoSlot(
                state,
                ai,
                actions,
                compact = true,
                modifier = Modifier.fillMaxWidth().weight(1f),
                restMode = true
            )
            Spacer(Modifier.height(20.dp))
            RestDepartButton(state, actions, Modifier.fillMaxWidth().height(96.dp))
        } else {
            // 위쪽 칸은 어떤 카드가 떠도 같은 높이. 남는 높이는 아래 슬롯(음성 카드 · 제안 카드)이 쓴다
            fun Modifier.share(base: Float) = height(base.dp)
            ComfortCardSplit(state, Modifier.fillMaxWidth().share(196f))
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().share(150f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RestOrWaypointTile(state, TileSize.Split, Modifier.weight(1f).fillMaxHeight())
                DriveTimeTile(state, TileSize.Split, Modifier.weight(1f).fillMaxHeight())
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().share(150f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TemperatureTile(state, TileSize.Split, Modifier.weight(1f).fillMaxHeight())
                WindowTile(state, TileSize.Split, Modifier.weight(1f).fillMaxHeight())
            }
            Spacer(Modifier.height(16.dp))
            GleoSlot(
                state,
                ai,
                actions,
                compact = true,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}

@Composable
private fun RestDepartButton(
    state: DrivingSessionState,
    actions: SessionActions,
    modifier: Modifier
) {
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
        PrimaryButton("다시 출발 준비", actions.onReadyToDepart, modifier, style = Mg.semi(30, 36))
    }
}

/** Full layout: modal popup over the mungo area (Figma 464:46215). */
@Composable
private fun WindowWarningDialog(
    state: DrivingSessionState,
    onDismiss: () -> Unit,
    onClose: () -> Unit
) {
    val busy = windowWarningBusy(state)
    // 다른 팝업과 같은 회색 뒷배경 (플랫폼 기본 dim은 차량 화면에서 약하거나 꺼져 있다)
    ScrimDialog(onDismiss = { if (!busy) onDismiss() }) {
        WindowWarningPopup(state, onDismiss, onClose)
    }
}

/** 주행 종료 확인: 창문 경고 팝업과 같은 공통 팝업 스타일 */
@Composable
private fun EndConfirmDialog(onDismiss: () -> Unit, onEnd: () -> Unit) {
    MgAlertDialog(
        title = "주행을 종료할까요?",
        message = "이번 주행 기록을 저장하고 요약을 보여 드려요.",
        icon = MgIcons.Flag,
        dismissText = "계속 주행",
        confirmText = "주행 종료",
        onDismiss = onDismiss,
        onConfirm = onEnd
    )
}

/** Idle driving slot: start a trip, or open history/settings (no session values are invented). */
@Composable
internal fun IdleStartCard(actions: SessionActions, compact: Boolean, modifier: Modifier) {
    Column(
        modifier.background(
            Color.White,
            RoundedCornerShape(Mg.Radius)
        ).padding(if (compact) 24.dp else 28.dp),
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
