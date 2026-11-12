package com.example.poco.core.driving

/** Weight is entered in 0.1 kg steps and stored as tenths to avoid floating-point boundary errors. */
enum class PetSize(val label: String) { SMALL("소형"), MEDIUM("중형"), LARGE("대형") }

/** Boundaries belong to the larger class: 10.0 kg is MEDIUM, 25.0 kg is LARGE. */
fun petSizeFor(weightTenths: Int): PetSize {
    require(weightTenths > 0) { "몸무게는 0.1kg 이상이어야 해요." }
    return when {
        weightTenths < 100 -> PetSize.SMALL
        weightTenths < 250 -> PetSize.MEDIUM
        else -> PetSize.LARGE
    }
}

/** "12.3" -> 123. Rejects more than one decimal place instead of silently rounding. */
fun parseWeightTenths(text: String): Int? {
    val match = Regex("""^\s*(\d{1,3})(?:\.(\d))?\s*$""").find(text) ?: return null
    val tenths = match.groupValues[1].toInt() * 10 + (match.groupValues[2].toIntOrNull() ?: 0)
    return tenths.takeIf { it in 1..1500 }
}

fun formatWeight(tenths: Int): String = "${tenths / 10}.${tenths % 10}kg"
