package com.example.poco.presentation.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/** Figma 팝업 'dim': #191F28 55% */
val PopupScrim = Color(0xFF191F28).copy(alpha = 0.55f)

/**
 * 뒷배경을 Figma dim(#191F28 55%)으로 덮는 팝업.
 * 차량 화면은 플랫폼 기본 dim이 꺼져 있거나 약할 수 있어서 직접 그린다. 바깥을 누르면 [onDismiss]
 */
@Composable
fun ScrimDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        Box(
            Modifier
                .fillMaxSize()
                .background(PopupScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // 카드 안을 누른 건 바깥 누름으로 치지 않는다
            Box(
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
            ) { content() }
        }
    }
}
