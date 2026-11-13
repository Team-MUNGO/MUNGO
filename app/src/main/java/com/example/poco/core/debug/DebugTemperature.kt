package com.example.poco.core.debug

import com.example.poco.BuildConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * 에뮬레이터 테스트용 실내 온도 덮어쓰기 (디버그 빌드만).
 * user 빌드 에뮬레이터는 VHAL 값 주입이 막혀 있어서, 디버그 빌드에서만
 * `adb shell am broadcast -p com.example.poco -a com.example.poco.DEBUG_TEMP --ef celsius 29`
 * 로 온도를 바꾼다. 음수를 보내면 해제. 릴리스 빌드는 항상 실제 차량 값을 쓴다
 */
object DebugTemperature {
    private val override = MutableStateFlow<Float?>(null)
    val celsius = override.asStateFlow()

    fun set(value: Float?) {
        if (BuildConfig.DEBUG) override.value = value
    }

    /** 덮어쓴 값이 있으면 그 값을, 없으면 [real]을 그대로 */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> overriding(real: Flow<T>, wrap: (Float) -> T): Flow<T> = if (!BuildConfig.DEBUG) {
        real
    } else {
        override.flatMapLatest { value -> if (value == null) real else flowOf(wrap(value)) }
    }
}
