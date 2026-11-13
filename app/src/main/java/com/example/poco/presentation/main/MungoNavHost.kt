package com.example.poco.presentation.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.poco.core.designsystem.FitWideWindow
import com.example.poco.presentation.boarding.BoardingRoute
import com.example.poco.presentation.common.LocalRailActions
import com.example.poco.presentation.common.RailActions
import com.example.poco.presentation.home.HomeRoute
import com.example.poco.presentation.mypet.MyPetRoute
import com.example.poco.presentation.onboarding.OnboardingRoute
import com.example.poco.presentation.onboarding.ProfileSetupRoute
import com.example.poco.presentation.permission.PermissionGroup
import com.example.poco.presentation.permission.PermissionRoute
import com.example.poco.presentation.record.AppliedInterval
import com.example.poco.presentation.record.MotionSicknessRoute
import com.example.poco.presentation.record.RailTab as RecordRailTab
import com.example.poco.presentation.record.RestIntervalRoute
import com.example.poco.presentation.record.RestSettingsViewModel
import com.example.poco.presentation.record.SettingsRoute
import com.example.poco.presentation.record.TripDetailRoute
import com.example.poco.presentation.record.TripEventsRoute
import com.example.poco.presentation.record.TripHistoryRoute
import com.example.poco.presentation.record.TripSummaryRoute
import com.example.poco.presentation.session.SessionRoute
import com.example.poco.presentation.session.SessionViewModel
import com.example.poco.presentation.splash.SplashRoute
import kotlinx.serialization.Serializable

sealed interface MungoDestination {
    @Serializable data object Splash : MungoDestination

    @Serializable data object Onboarding : MungoDestination

    @Serializable data object ProfileSetup : MungoDestination

    /** @param afterProfileSetup 온보딩에서 프로필을 다 만들고 왔으면 권한 다음 바로 홈 */
    @Serializable data class Permission(val afterProfileSetup: Boolean = false) : MungoDestination

    @Serializable data object PetProfile : MungoDestination

    @Serializable data object Home : MungoDestination

    @Serializable data object Driving : MungoDestination

    /** 탑승 준비 (홈 '탑승 준비 시작') */
    @Serializable data object Boarding : MungoDestination

    /** 펫모드 (보호자 외출 중, 홈 펫모드 카드) */

    /** 이동 기록 목록 (레일 '이동 기록') — Figma 465:48292 */
    @Serializable data class Records(val period: String = "MONTH") : MungoDestination

    @Serializable data class TripSummary(val tripId: String) : MungoDestination

    @Serializable data class TripDetail(val tripId: String) : MungoDestination

    @Serializable data class TripEvents(val tripId: String) : MungoDestination

    @Serializable data class TripMotion(val tripId: String) : MungoDestination

    /** 휴식 간격. [tripId]가 있으면 멀미 기록의 추천에서 들어온 것 */
    @Serializable
    data class RestInterval(val tripId: String? = null, val recommended: Int? = null) :
        MungoDestination

    /** 설정 (레일 '설정') */
    @Serializable data object Settings : MungoDestination
}

private const val REQUEST_PERMISSIONS = "request_permissions"
private const val APPLIED_FROM = "applied_interval_from"
private const val APPLIED_TO = "applied_interval_to"

/**
 * 첫 실행: 스플래시 → 온보딩 소개 → 프로필 만들기(1/3 ~ 완성) → 홈 (차량 권한은 홈에서 시스템 창으로)
 *         (소개에서 건너뛰기: → 권한 → 펫 프로필 → 홈)
 * 다음부터: 스플래시 → 홈 (펫 프로필은 홈의 펫 프로필 설정 버튼 · 펫 추가하기 · 자리 바꾸기에서 진입)
 */
@Composable
fun MungoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val railActions = remember(navController) {
        RailActions(
            onRecords = { navController.navigateToRailTab(MungoDestination.Records()) },
            onSettings = { navController.navigateToRailTab(MungoDestination.Settings) }
        )
    }
    CompositionLocalProvider(LocalRailActions provides railActions) {
        MungoNavGraph(navController, modifier)
    }
}

@Composable
private fun MungoNavGraph(navController: NavHostController, modifier: Modifier) {
    val recordRail: (RecordRailTab) -> Unit = { tab ->
        when (tab) {
            RecordRailTab.HOME -> navController.navigateToRailTab(MungoDestination.Home)
            RecordRailTab.HISTORY -> navController.navigateToRailTab(MungoDestination.Records())
            RecordRailTab.PET -> navController.navigate(MungoDestination.PetProfile)
            RecordRailTab.SETTINGS -> navController.navigateToRailTab(MungoDestination.Settings)
        }
    }
    NavHost(
        navController = navController,
        startDestination = MungoDestination.Splash,
        modifier = modifier
    ) {
        composable<MungoDestination.Splash> {
            FitWideWindow {
                SplashRoute(
                    navigateToOnboarding = {
                        navController.navigate(MungoDestination.Onboarding) {
                            popUpTo(MungoDestination.Splash) { inclusive = true }
                        }
                    },
                    navigateToHome = {
                        navController.navigate(MungoDestination.Home) {
                            popUpTo(MungoDestination.Splash) { inclusive = true }
                        }
                    }
                )
            }
        }
        composable<MungoDestination.Onboarding> {
            FitWideWindow {
                OnboardingRoute(
                    navigateToProfileSetup = {
                        navController.navigate(MungoDestination.ProfileSetup)
                    },
                    navigateToPermission = {
                        navController.navigate(MungoDestination.Permission()) {
                            popUpTo(MungoDestination.Onboarding) { inclusive = true }
                        }
                    }
                )
            }
        }
        composable<MungoDestination.ProfileSetup> {
            FitWideWindow {
                ProfileSetupRoute(
                    navigateBack = { navController.popBackStack() },
                    // 프로필 완성 '시작하기' → 바로 홈 (Figma 온보딩_page_05 → Home)
                    navigateNext = {
                        navController.navigate(MungoDestination.Home) {
                            popUpTo(MungoDestination.Onboarding) { inclusive = true }
                        }
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            REQUEST_PERMISSIONS,
                            true
                        )
                    }
                )
            }
        }
        composable<MungoDestination.Permission> { entry ->
            FitWideWindow {
                val afterProfileSetup =
                    entry.toRoute<MungoDestination.Permission>().afterProfileSetup
                PermissionRoute(
                    navigateNext = {
                        val next: MungoDestination =
                            if (afterProfileSetup) {
                                MungoDestination.Home
                            } else {
                                MungoDestination.PetProfile
                            }
                        navController.navigate(next) {
                            popUpTo<MungoDestination.Permission> { inclusive = true }
                        }
                    }
                )
            }
        }
        composable<MungoDestination.PetProfile> {
            FitWideWindow {
                MyPetRoute(
                    navigateHome = {
                        // 홈 · 탑승 준비에서 들어왔으면 되돌아가고, 첫 실행이면 홈을 새로 연다
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(MungoDestination.Home) {
                                popUpTo(MungoDestination.PetProfile) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
        composable<MungoDestination.Home> { entry ->
            FitWideWindow {
                // 온보딩에서 바로 왔으면 차량 정보 권한을 시스템 창으로 한 번 요청한다 (거부해도 홈은 그대로)
                val askPermissions =
                    remember {
                        entry.savedStateHandle.remove<Boolean>(REQUEST_PERMISSIONS) ==
                            true
                    }
                val permissionLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) {
                    }
                LaunchedEffect(askPermissions) {
                    if (askPermissions) {
                        permissionLauncher.launch(
                            PermissionGroup.entries.flatMap {
                                it.permissions
                            }.distinct().toTypedArray()
                        )
                    }
                }
                HomeRoute(
                    navigateToPetProfile = { navController.navigate(MungoDestination.PetProfile) },
                    navigateToBoarding = { navController.navigate(MungoDestination.Boarding) },
                    navigateToDriving = { navController.navigate(MungoDestination.Driving) },
                    navigateToWeeklyRecords = {
                        navController.navigateToRailTab(MungoDestination.Records("WEEK"))
                    }
                )
            }
        }
        composable<MungoDestination.Boarding> {
            FitWideWindow {
                BoardingRoute(
                    navigateBack = { navController.popBackStack() },
                    navigateToPetProfile = { navController.navigate(MungoDestination.PetProfile) },
                    navigateToDriving = {
                        navController.navigate(MungoDestination.Driving) {
                            popUpTo<MungoDestination.Boarding> { inclusive = true }
                        }
                    }
                )
            }
        }
        // 주행 화면: 홈 · 탑승 준비(주행 시작) · 기어 D 자동 시작 모두 여기로 온다.
        // 컴포트 점수 · 휴식 · AI 어시스턴트 · 이동 기록 저장을 하는 주행 세션 (Figma Page 3)
        composable<MungoDestination.Driving> {
            val session: SessionViewModel = hiltViewModel()
            val state by session.uiState.collectAsStateWithLifecycle()
            val ended = state.session.lastEndedTripId
            val startedWith = remember { ended }
            LaunchedEffect(ended) {
                // Only a session that ends here opens its summary (not an older one).
                if (ended != null && ended != startedWith) {
                    // 설정 > 알림 '도착 요약'을 끄면 요약 없이 홈으로
                    val next: MungoDestination =
                        if (session.arrivalSummary.value) {
                            MungoDestination.TripSummary(
                                ended
                            )
                        } else {
                            MungoDestination.Home
                        }
                    navController.navigate(next) {
                        popUpTo<MungoDestination.Driving> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            SessionRoute(
                onOpenHistory = { navController.navigate(MungoDestination.Records()) },
                onOpenSettings = { navController.navigate(MungoDestination.Settings) },
                autoStart = true,
                onOpenPetProfile = { navController.navigate(MungoDestination.PetProfile) },
                viewModel = session
            )
        }
        composable<MungoDestination.Records> { entry ->
            val initialPeriod = entry.toRoute<MungoDestination.Records>().period
            TripHistoryRoute(
                onOpen = { navController.navigate(MungoDestination.TripDetail(it)) },
                onNavigate = recordRail,
                initialPeriod =
                com.example.poco.presentation.record.Period.entries.firstOrNull {
                    it.name ==
                        initialPeriod
                }
                    ?: com.example.poco.presentation.record.Period.MONTH
            )
        }
        composable<MungoDestination.Settings> {
            SettingsRoute(
                onNavigate = recordRail,
                onOpenInterval = { navController.navigate(MungoDestination.RestInterval()) }
            )
        }
        composable<MungoDestination.TripSummary> { entry ->
            val id = entry.toRoute<MungoDestination.TripSummary>().tripId
            TripSummaryRoute(
                id,
                onDetail = { navController.navigate(MungoDestination.TripDetail(id)) },
                onDone = { navController.navigateToRailTab(MungoDestination.Home) }
            )
        }
        composable<MungoDestination.TripDetail> { entry ->
            val id = entry.toRoute<MungoDestination.TripDetail>().tripId
            TripDetailRoute(
                id,
                onBack = { navController.popBackStack() },
                onEvents = { navController.navigate(MungoDestination.TripEvents(id)) },
                onMotion = { navController.navigate(MungoDestination.TripMotion(id)) }
            )
        }
        composable<MungoDestination.TripEvents> { entry ->
            TripEventsRoute(entry.toRoute<MungoDestination.TripEvents>().tripId, onBack = {
                navController.popBackStack()
            })
        }
        composable<MungoDestination.TripMotion> { entry ->
            val id = entry.toRoute<MungoDestination.TripMotion>().tripId
            val settings: RestSettingsViewModel = hiltViewModel()
            val handle = entry.savedStateHandle
            val from by handle.getStateFlow<Int?>(APPLIED_FROM, null).collectAsStateWithLifecycle()
            val to by handle.getStateFlow<Int?>(APPLIED_TO, null).collectAsStateWithLifecycle()
            MotionSicknessRoute(
                id,
                onBack = { navController.popBackStack() },
                onOpenInterval = { navController.navigate(MungoDestination.RestInterval(id, it)) },
                applied = if (from != null && to != null) AppliedInterval(from!!, to!!) else null,
                onUndo = { change ->
                    settings.setInterval(change.from)
                    handle[APPLIED_FROM] = null
                    handle[APPLIED_TO] = null
                },
                onToastShown = {}
            )
        }
        composable<MungoDestination.RestInterval> { entry ->
            val args = entry.toRoute<MungoDestination.RestInterval>()
            RestIntervalRoute(
                recommended = args.recommended,
                tripId = args.tripId,
                onCancel = { navController.popBackStack() },
                onApplied = { change ->
                    if (args.tripId != null) {
                        navController.previousBackStackEntry?.savedStateHandle?.let {
                            it[APPLIED_FROM] = change.from
                            it[APPLIED_TO] = change.to
                        }
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}

/** 레일 탭 이동: 홈 위에 한 단계만 쌓고 같은 탭은 다시 열지 않는다 */
private fun NavHostController.navigateToRailTab(destination: MungoDestination) {
    navigate(destination) {
        popUpTo<MungoDestination.Home>()
        launchSingleTop = true
    }
}
