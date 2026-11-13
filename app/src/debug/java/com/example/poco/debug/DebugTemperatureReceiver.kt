package com.example.poco.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.poco.core.debug.DebugTemperature
import timber.log.Timber

/** 디버그 빌드 전용: `--ef celsius 29` → 실내 온도 29°C로 덮어쓰기, 음수 → 해제 */
class DebugTemperatureReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val celsius = intent.getFloatExtra("celsius", -1f)
        val value = celsius.takeIf { it >= 0f }
        DebugTemperature.set(value)
        Timber.i("DEBUG_TEMP 실내 온도 덮어쓰기: %s", value ?: "해제")
    }
}
