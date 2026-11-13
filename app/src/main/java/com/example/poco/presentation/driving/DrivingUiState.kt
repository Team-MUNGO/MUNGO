package com.example.poco.presentation.driving

import com.example.poco.data.model.CarWindow
import com.example.poco.data.model.PetSeat
import com.example.poco.domain.driving.DrivingEventType
import com.example.poco.domain.driving.PetMood

data class DrivingUiState(
    val petName: String = "반려동물",
    val petSeat: PetSeat = PetSeat.REAR_RIGHT,
    val petPhotoPath: String? = null,
    val petBreed: String? = null,
    val score: Int = 100,
    val mood: PetMood = PetMood.CALM,
    /** 표정 이유 한 줄 */
    val lastEventType: DrivingEventType? = null,
    /** 화면에 보여줄 mungo 음성 안내 (말한 문장 그대로) */
    val voiceMessage: String? = null,
    val hardBrakeCount: Int = 0,
    val sharpTurnCount: Int = 0,
    val calmMinutes: Int = 0,
    val elapsedMinutes: Int = 0,
    val seatTemperature: Float? = null,
    val isTemperatureOff: Boolean = false,
    val isVoiceEnabled: Boolean = true,
    /** 알림은 한 번에 하나. 창문(안전)이 온도(편안함)보다 먼저 */
    val alert: DrivingAlert? = null,
    /** 주행이 끝나 저장된 이동 id → 도착 리포트로 */
    val finishedTripId: Long? = null,
    /** NaviHelper 목적지. null = 길 안내 없이 주행 중 */
    val destinationName: String? = null,
    val remainDistanceM: Int? = null,
    val remainTimeSec: Int? = null,
    /** 알림 질문 뒤 STT로 대답을 듣는 중 */
    val isListening: Boolean = false,
    /** 마지막으로 알아들은 말 (화면에 "괜찮아"처럼 보여줌) */
    val heardText: String? = null
)

sealed interface DrivingAlert {
    /** 07-2 창문 안전 알림: 30km/h 이상 + 반려동물 옆 창문 30% 이상 */
    data class Window(val window: CarWindow, val openPercent: Int, val speedKmh: Int) : DrivingAlert

    /** 온도 알림: mungo 기준점 ① 견종 유형별 더움 · 추움 기준을 넘음. 29.5°C 이상은 위험 */
    data class Temperature(
        val celsius: Float,
        val isHot: Boolean,
        val isDanger: Boolean,
        val comfortRange: ClosedFloatingPointRange<Float>,
        /** 예: 소형 · 단모 + 노견 · 퍼피 */
        val typeLabel: String,
        val targetCelsius: Float
    ) : DrivingAlert
}
