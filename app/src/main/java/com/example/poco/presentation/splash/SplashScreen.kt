package com.example.poco.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.designsystem.OnboardingColors
import com.example.poco.core.designsystem.PetAvatar
import com.example.poco.core.theme.POCOTheme

@Composable
fun SplashRoute(
    navigateToPermission: () -> Unit,
    navigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.destination) {
        when (uiState.destination) {
            SplashDestination.PERMISSION -> navigateToPermission()
            SplashDestination.HOME -> navigateToHome()
            null -> Unit
        }
    }

    SplashScreen(uiState = uiState)
}

@Composable
private fun SplashScreen(uiState: SplashUiState, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = SplashViewModel.SPLASH_DURATION_MS.toInt(),
                easing = LinearEasing
            )
        )
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(OnboardingColors.darkBackground)
            .safeDrawingPadding()
            .padding(horizontal = 64.dp, vertical = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "POCO",
                style = POCOTheme.typography.hmiLabel,
                color = OnboardingColors.accent
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "안녕, ${uiState.petName}!\n오늘도 함께 달려요",
                style = POCOTheme.typography.hmiHeading1,
                color = Color.White
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "${uiState.petName}의 컨디션은 POCO가 지켜볼게요.\n운전자님은 앞만 보세요.",
                style = POCOTheme.typography.hmiBody,
                color = OnboardingColors.onDarkSecondary
            )
            Spacer(Modifier.height(40.dp))
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .width(320.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = OnboardingColors.accent,
                trackColor = OnboardingColors.onDarkTrack,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            uiState.connectionStatus.label()?.let { label ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = label,
                    style = POCOTheme.typography.hmiCaption,
                    color = OnboardingColors.onDarkSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            PetHalo(
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .aspectRatio(1f)
            )
            PetAvatar(
                modifier = Modifier
                    .fillMaxHeight(0.5f)
                    .aspectRatio(1f)
            )
        }
    }
}

/** 캐릭터 뒤의 동심원 */
@Composable
private fun PetHalo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2
        drawCircle(OnboardingColors.haloFill, radius = radius * 0.72f)
        listOf(0.72f, 0.86f, 1f).forEachIndexed { index, ratio ->
            drawCircle(
                color = OnboardingColors.accent.copy(alpha = 0.35f - index * 0.1f),
                radius = radius * ratio - 2f,
                style = Stroke(width = 3f)
            )
        }
    }
}

private fun ConnectionStatus.label(): String? = when (this) {
    ConnectionStatus.PREPARING -> "POCO를 준비하고 있어요"
    ConnectionStatus.CONNECTING -> "차량 데이터 연결 중…"
    ConnectionStatus.CONNECTED -> "차량 데이터 연결 완료"
    ConnectionStatus.DELAYED -> "차량 연결이 늦어지고 있어요 · 홈에서 계속 시도할게요"
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun SplashScreenPreview() {
    POCOTheme {
        SplashScreen(uiState = SplashUiState(connectionStatus = ConnectionStatus.CONNECTING))
    }
}
