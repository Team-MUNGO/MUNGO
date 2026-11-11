package com.example.poco.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 마지막 글자에 받침이 있으면 true. 한글이 아니면 받침 없음으로 본다 */
private fun String.hasBatchim(): Boolean {
    val last = lastOrNull() ?: return false
    if (last !in '가'..'힣') return false
    return (last - '가') % 28 != 0
}

/** "초코" → "초코가", "보리" → "보리가", "콩" → "콩이" */
val String.iGa: String get() = this + if (hasBatchim()) "이" else "가"

/** "초코" → "초코를", "콩" → "콩을" */
val String.eulReul: String get() = this + if (hasBatchim()) "을" else "를"

/** "초코" → "초코는", "콩" → "콩은" */
val String.eunNeun: String get() = this + if (hasBatchim()) "은" else "는"

/** "초코" → "초코에게" */
val String.ege: String get() = this + "에게"

/** 11월 3일 */
fun Long.toMonthDay(): String = SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(this))

/** 11월 3일 오후 3:02 */
fun Long.toMonthDayTime(): String =
    SimpleDateFormat("M월 d일 a h:mm", Locale.KOREA).format(Date(this))

/** 오후 3:02 → "3:02" */
fun Long.toClock(): String = SimpleDateFormat("h:mm", Locale.KOREA).format(Date(this))

/** 62분 → "1시간 2분", 18분 → "18분" */
fun Int.toDurationText(): String = when {
    this < 60 -> "${this}분"
    this % 60 == 0 -> "${this / 60}시간"
    else -> "${this / 60}시간 ${this % 60}분"
}
