package com.example.poco.data.model

/** 앱에서 사용하는 창문 위치  (SDK 타입과 분리) */
enum class CarWindow {
    DRIVER,
    PASSENGER,
    REAR_LEFT,
    REAR_RIGHT
}

/** 창문이 움직일 수 있는 위치 범위 */
data class WindowPositionRange(
    val min: Int,
    val max: Int,
    val canRead: Boolean = true,
    val canWrite: Boolean = true
)
