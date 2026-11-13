package com.example.poco.core.designsystem

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

/**
 * 입력칸 바깥(빈 배경)을 누르면 포커스를 풀어 키보드를 내린다.
 * 안드로이드는 바깥을 눌러도 키보드가 저절로 내려가지 않아 화면 루트에 붙인다.
 * 버튼 · 입력칸처럼 탭을 직접 처리하는 자식은 그대로 동작한다 (그 탭은 여기까지 오지 않음)
 */
@Composable
fun Modifier.clearFocusOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    return pointerInput(focusManager) { detectTapGestures { focusManager.clearFocus() } }
}
