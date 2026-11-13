package com.example.poco.presentation.splash

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.R
import com.example.poco.core.designsystem.MungoLogo
import com.example.poco.core.theme.MungoSpacing
import com.example.poco.core.theme.MungoTheme
import com.example.poco.presentation.design.Mg
import kotlinx.coroutines.delay

@Composable
fun SplashRoute(
    navigateToOnboarding: () -> Unit,
    navigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.destination) {
        when (uiState.destination) {
            SplashDestination.ONBOARDING -> navigateToOnboarding()
            SplashDestination.HOME -> navigateToHome()
            null -> Unit
        }
    }

    LaunchedEffect(Unit) { viewModel.onIntroShown() }
    SplashIntro(onFinished = viewModel::onIntroFinished)
}

/**
 * 인트로 영상 (res/raw/mungo_intro.mp4, 1552×1080 · 30fps · 4.6초 · 소리 없음,
 * 원본: design/brand/mungo_intro_original.mp4).
 * 끝나면 [onFinished] → 온보딩/홈으로. 화면을 누르면 건너뛴다.
 * 재생에 실패하거나 3초 안에 준비되지 않으면 바로 넘어가고, 미리보기(Preview)에서는 정지 화면 [SplashScreen]을 보여준다.
 *
 * TextureView에 직접 그린다: VideoView(SurfaceView)는 첫 장면 전 검은 화면을 가리려고 덮개를 씌워야 하는데,
 * 에뮬레이터 · 느린 기기에서는 덮개가 늦게 걷혀 앞부분(발자국)이 잘려 보였다.
 */
@Composable
private fun SplashIntro(onFinished: () -> Unit) {
    if (LocalInspectionMode.current) {
        SplashScreen()
        return
    }
    val finish = rememberUpdatedState(onFinished)
    var isPrepared by remember { mutableStateOf(false) }
    // 준비가 3초 넘게 걸리면(디코더 문제 등) 기다리지 않고 넘어간다
    LaunchedEffect(Unit) {
        delay(PREPARE_TIMEOUT_MS)
        if (!isPrepared) finish.value()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 영상 바탕이 순수 흰색이라 같은 색으로 이어지게
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            ),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(INTRO_RATIO, matchHeightConstraintsFirst = true),
            factory = { context ->
                val player = MediaPlayer()
                TextureView(context).apply {
                    tag = player
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            texture: SurfaceTexture,
                            w: Int,
                            h: Int
                        ) {
                            runCatching {
                                player.setSurface(Surface(texture))
                                player.setDataSource(
                                    context,
                                    Uri.parse(
                                        "android.resource://${context.packageName}/${R.raw.mungo_intro}"
                                    )
                                )
                                // 소리 없는 영상: 음악 · 내비 음성을 끊지 않도록 오디오 포커스를 가져가지 않는다
                                player.setVolume(0f, 0f)
                                player.setOnPreparedListener {
                                    isPrepared = true
                                    it.start()
                                }
                                player.setOnCompletionListener { finish.value() }
                                player.setOnErrorListener { _, _, _ ->
                                    finish.value()
                                    true
                                }
                                player.prepareAsync()
                            }.onFailure { finish.value() }
                        }

                        override fun onSurfaceTextureSizeChanged(
                            texture: SurfaceTexture,
                            w: Int,
                            h: Int
                        ) = Unit

                        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean =
                            true

                        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                    }
                }
            },
            onRelease = { view -> (view.tag as? MediaPlayer)?.release() }
        )
    }
}

private const val INTRO_RATIO = 1552f / 1080f
private const val PREPARE_TIMEOUT_MS = 3_000L

/** 정지 화면 (미리보기용): 흰 바탕 가운데 로고 + 한 줄 소개, 아래에 Pleos Connect 표기 (피그마 스플래시) */
@Composable
private fun SplashScreen(modifier: Modifier = Modifier) {
    val colors = MungoTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgSurface)
            .safeDrawingPadding()
    ) {
        Column(
            // Figma 470:54315: 로고 160 (y 450) · 소개 R30 (y 653) · 가운데보다 조금 위
            modifier = Modifier.align(Alignment.Center).offset(y = (-58).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MungoLogo(height = 160.dp)
            Spacer(Modifier.height(43.dp))
            Text(
                text = "소중한 반려동물과 함께하는 안심 드라이브",
                style = Mg.regular(30, 36),
                color = colors.textSecondary
            )
        }
        Text(
            text = "mungo ©",
            style = Mg.regular(22, 26),
            color = Color(0xFF9AA4B0),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MungoSpacing.space48)
        )
    }
}

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun SplashScreenPreview() {
    MungoTheme(darkTheme = false) { SplashScreen() }
}

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun SplashScreenDarkPreview() {
    MungoTheme(darkTheme = true) { SplashScreen() }
}
