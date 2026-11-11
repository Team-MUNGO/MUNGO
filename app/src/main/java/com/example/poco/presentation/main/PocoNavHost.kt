package com.example.poco.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.poco.presentation.driving.DrivingRoute
import com.example.poco.presentation.home.HomeRoute
import com.example.poco.presentation.permission.PermissionRoute
import com.example.poco.presentation.petprofile.PetProfileRoute
import com.example.poco.presentation.splash.SplashRoute
import kotlinx.serialization.Serializable

sealed interface PocoDestination {
    @Serializable data object Splash : PocoDestination

    @Serializable data object Permission : PocoDestination

    @Serializable data object PetProfile : PocoDestination

    @Serializable data object Home : PocoDestination

    @Serializable data object Driving : PocoDestination
}

/**
 * 첫 실행: 스플래시 → 권한 → 펫 프로필 → 홈
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
                navigateToPermission = {
                    navController.navigate(PocoDestination.Permission) {
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
        composable<PocoDestination.Permission> {
            PermissionRoute(
                navigateNext = {
                    navController.navigate(PocoDestination.PetProfile) {
                        popUpTo(PocoDestination.Permission) { inclusive = true }
                    }
                }
            )
        }
        composable<PocoDestination.PetProfile> {
            PetProfileRoute(
                navigateBack = {
                    // 홈에서 들어왔으면 되돌아가고, 첫 실행이면 홈을 새로 연다
                    if (!navController.popBackStack(PocoDestination.Home, inclusive = false)) {
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
                navigateToDriving = { navController.navigate(PocoDestination.Driving) }
            )
        }
        composable<PocoDestination.Driving> {
            DrivingRoute(navigateBack = { navController.popBackStack() })
        }
    }
}
