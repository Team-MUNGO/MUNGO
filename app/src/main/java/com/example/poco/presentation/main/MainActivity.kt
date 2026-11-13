package com.example.poco.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.poco.core.common.ToastMessage
import com.example.poco.core.common.ToastMessenger
import com.example.poco.core.designsystem.component.MungoToast
import com.example.poco.core.theme.MungoSpacing
import com.example.poco.core.theme.MungoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var toastMessenger: ToastMessenger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MungoTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    MungoNavHost()
                    ToastHost(
                        toastMessenger = toastMessenger,
                        // 시스템 바(하단 내비 등) 위로 올린다
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(bottom = MungoSpacing.space48)
                    )
                }
            }
        }
    }
}

/** 토스트를 2.5초 보여준다 */
@Composable
private fun ToastHost(toastMessenger: ToastMessenger, modifier: Modifier = Modifier) {
    var message by remember { mutableStateOf<ToastMessage?>(null) }
    LaunchedEffect(toastMessenger) {
        toastMessenger.messages.collect { message = it }
    }
    LaunchedEffect(message) {
        if (message != null) {
            delay(TOAST_DURATION_MS)
            message = null
        }
    }
    message?.let { MungoToast(text = it.text, detail = it.detail, modifier = modifier) }
}

private const val TOAST_DURATION_MS = 2_500L
