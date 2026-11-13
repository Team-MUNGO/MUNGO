package com.example.poco.core.designsystem

import androidx.compose.runtime.compositionLocalOf

/**
 * 설정 > 음성 및 알림 > '멍고 음성 안내'가 켜져 있는지.
 * 꺼져 있으면 TTS · STT를 쓰지 않으므로, 모든 마이크 버튼은 회색으로 바뀌고 눌리지 않는다.
 * MungoNavHost에서 한 번 제공한다 (프리뷰 등 제공하지 않으면 켜짐).
 */
val LocalVoiceEnabled = compositionLocalOf { true }
