package com.example.poco.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.poco.presentation.boarding.BoardingRoute
import com.example.poco.presentation.driving.DrivingRoute
import com.example.poco.presentation.home.HomeRoute
import com.example.poco.presentation.mypet.MyPetRoute
import com.example.poco.presentation.onboarding.OnboardingRoute
import com.example.poco.presentation.onboarding.ProfileSetupRoute
import com.example.poco.presentation.permission.PermissionRoute
import com.example.poco.presentation.petmode.PetModeRoute
import com.example.poco.presentation.splash.SplashRoute
import kotlinx.serialization.Serializable

sealed interface PocoDestination {
    @Serializable data object Splash : PocoDestination

    @Serializable data object Onboarding : PocoDestination

    @Serializable data object ProfileSetup : PocoDestination

    /** @param afterProfileSetup 온보딩에서 프로필을 다 만들고 왔으면 권한 다음 바로 홈 */
    @Serializable data class Permission(val afterProfileSetup: Boolean = false) : PocoDestination

    @Serializable data object PetProfile : PocoDestination

    @Serializable data object Home : PocoDestination

    @Serializable data object Driving : PocoDestination

    /** 탑승 준비 (홈 '탑승 준비 시작') */
    @Serializable data object Boarding : PocoDestination

    /** 펫모드 (보호자 외출 중, 홈 펫모드 카드) */
    @Serializable data object PetMode : PocoDestination
}

/**
 * 첫 실행: 스플래시 → 온보딩 소개 → 프로필 만들기(1/3 ~ 완성) → 권한 → 홈
 *         (소개에서 건너뛰기: → 권한 → 펫 프로필 → 홈)
 * 다음부터: 스플래시 → 홈 (펫 프로필은 홈의 펫 프로필 설정 버튼 · 펫 추가하기 · 자리 바꾸기에서 진입)
 */
@Composable
fun PocoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = PocoDestination.Splash,
        modifier = modifier
    ) {
        composable<PocoDestination.Splash> {
            SplashRoute(
                navigateToOnboarding = {
                    navController.navigate(PocoDestination.Onboarding) {
                        popUpTo(PocoDestination.Splash) { inclusive = true }
                    }
                },
                navigateToHome = {
                    navController.navigate(PocoDestination.Home) {
                        popUpTo(PocoDestination.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable<PocoDestination.Onboarding> {
            OnboardingRoute(
                navigateToProfileSetup = { navController.navigate(PocoDestination.ProfileSetup) },
                navigateToPermission = {
                    navController.navigate(PocoDestination.Permission()) {
                        popUpTo(PocoDestination.Onboarding) { inclusive = true }
                    }
                }
            )
        }
        composable<PocoDestination.ProfileSetup> {
            ProfileSetupRoute(
                navigateBack = { navController.popBackStack() },
                navigateNext = {
                    navController.navigate(PocoDestination.Permission(afterProfileSetup = true)) {
                        popUpTo(PocoDestination.Onboarding) { inclusive = true }
                    }
                }
            )
        }
        composable<PocoDestination.Permission> { entry ->
            val afterProfileSetup = entry.toRoute<PocoDestination.Permission>().afterProfileSetup
            PermissionRoute(
                navigateNext = {
                    val next: PocoDestination =
                        if (afterProfileSetup) PocoDestination.Home else PocoDestination.PetProfile
                    navController.navigate(next) {
                        popUpTo<PocoDestination.Permission> { inclusive = true }
                    }
                }
            )
        }
        composable<PocoDestination.PetProfile> {
            MyPetRoute(
                navigateHome = {
                    // 홈 · 탑승 준비에서 들어왔으면 되돌아가고, 첫 실행이면 홈을 새로 연다
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(PocoDestination.Home) {
                            popUpTo(PocoDestination.PetProfile) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable<PocoDestination.Home> {
            HomeRoute(
                navigateToPetProfile = { navController.navigate(PocoDestination.PetProfile) },
                navigateToBoarding = { navController.navigate(PocoDestination.Boarding) },
                navigateToDriving = { navController.navigate(PocoDestination.Driving) },
                navigateToPetMode = { navController.navigate(PocoDestination.PetMode) }
            )
        }
        composable<PocoDestination.PetMode> {
            PetModeRoute(navigateBack = { navController.popBackStack() })
        }
        composable<PocoDestination.Boarding> {
            BoardingRoute(
                navigateBack = { navController.popBackStack() },
                navigateToPetProfile = { navController.navigate(PocoDestination.PetProfile) },
                navigateToDriving = {
                    navController.navigate(PocoDestination.Driving) {
                        popUpTo<PocoDestination.Boarding> { inclusive = true }
                    }
                }
            )
        }
        composable<PocoDestination.Driving> {
            DrivingRoute(navigateBack = { navController.popBackStack() })
        }
    }
}
