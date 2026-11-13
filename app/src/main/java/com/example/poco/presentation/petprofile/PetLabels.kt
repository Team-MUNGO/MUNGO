package com.example.poco.presentation.petprofile

import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.PetSize
import com.example.poco.data.model.Restraint
import com.example.poco.data.model.VoiceFrequency

fun PetSeat.label(): String = when (this) {
    PetSeat.REAR_LEFT -> "뒷좌석 왼쪽"
    PetSeat.REAR_RIGHT -> "뒷좌석 오른쪽"
    PetSeat.PASSENGER -> "조수석"
}

fun MotionSensitivity.label(): String = when (this) {
    MotionSensitivity.LOW -> "낮음"
    MotionSensitivity.NORMAL -> "보통"
    MotionSensitivity.HIGH -> "높음"
}

/** mungo 기준점 ③: 흔들림 기준점에 곱하는 값 */
fun MotionSensitivity.description(): String = "기준 ×$thresholdFactor"

fun Restraint.label(): String = when (this) {
    Restraint.NONE -> "그냥 앉아요"
    Restraint.CRATE_HARNESS -> "이동장 · 하네스"
}

fun Restraint.description(): String = "기준 ×$thresholdFactor"

fun VoiceFrequency.label(): String = when (this) {
    VoiceFrequency.LOW -> "적게"
    VoiceFrequency.NORMAL -> "보통"
    VoiceFrequency.HIGH -> "자주"
}

fun VoiceFrequency.description(): String = when (this) {
    VoiceFrequency.LOW -> "꼭 필요할 때만"
    VoiceFrequency.NORMAL -> "3분에 1번까지"
    VoiceFrequency.HIGH -> "모든 이벤트"
}

fun PetSize.label(): String = when (this) {
    PetSize.SMALL -> "소형"
    PetSize.MEDIUM -> "중형"
    PetSize.LARGE -> "대형"
}

/** "몰티즈 · 소형" */
fun Pet.summary(): String = listOfNotNull(breed.takeIf { it.isNotBlank() }, size?.label())
    .joinToString(" · ")
