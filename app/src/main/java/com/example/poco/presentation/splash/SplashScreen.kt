package com.example.poco.presentation.splash

import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.R
import com.example.poco.core.designsystem.MungoLogo
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.theme.PocoSpacing

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
 * 인트로 영상 (res/raw/mungo_intro.mp4, 1700×1184 · 4.6초 · 소리 없음).
 * 끝나면 [onFinished] → 온보딩/홈으로. 화면을 누르면 건너뛴다.
 * 재생에 실패하면 바로 넘어가고, 미리보기(Preview)에서는 정지 화면 [SplashScreen]을 보여준다
 */
@Composable
private fun SplashIntro(onFinished: () -> Unit) {
    if (LocalInspectionMode.current) {
        SplashScreen()
        return
    }
    val background = POCOTheme.colors.bgSurface.toArgb()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(POCOTheme.colors.bgSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            )
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                VideoView(context).apply {
                    // 소리 없는 영상: 음악 · 내비 음성을 끊지 않도록 오디오 포커스를 가져가지 않는다
                    setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
                    // 첫 장면이 그려지기 전 검은 화면이 보이지 않게 바탕색으로 가려 둔다
                    setBackgroundColor(background)
                    setOnInfoListener { _, what, _ ->
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                        false
                    }
                    setOnCompletionListener { onFinished() }
                    setOnErrorListener { _, _, _ ->
                        onFinished()
                        true
                    }
                    setVideoURI(
                        Uri.parse("android.resource://${context.packageName}/${R.raw.mungo_intro}")
                    )
                    start()
                }
            },
            onRelease = { it.stopPlayback() }
        )
    }
}

/** 정지 화면 (미리보기용): 흰 바탕 가운데 로고 + 한 줄 소개, 아래에 Pleos Connect 표기 (피그마 스플래시) */
@Composable
private fun SplashScreen(modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgSurface)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MungoLogo(height = 140.dp)
            Spacer(Modifier.height(PocoSpacing.space48))
            Text(
                text = "소중한 반려동물과 함께하는 안심 드라이브",
                style = POCOTheme.typography.body1,
                color = colors.textTertiary
            )
        }
        Text(
            text = "Pleos Connect ©",
            style = POCOTheme.typography.body3,
            color = colors.textTertiary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = PocoSpacing.space48)
        )
    }
}

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun SplashScreenPreview() {
    POCOTheme(darkTheme = false) { SplashScreen() }
}

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun SplashScreenDarkPreview() {
    POCOTheme(darkTheme = true) { SplashScreen() }
}
