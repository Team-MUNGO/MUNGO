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
import com.example.poco.core.designsystem.component.PocoToast
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.theme.PocoSpacing
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
            POCOTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    PocoNavHost()
                    ToastHost(
                        toastMessenger = toastMessenger,
                        // 시스템 바(하단 내비 등) 위로 올린다
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(bottom = PocoSpacing.space48)
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
    message?.let { PocoToast(text = it.text, detail = it.detail, modifier = modifier) }
}

private const val TOAST_DURATION_MS = 2_500L
