package com.example.poco.domain.driving

import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSize

/**
 * POCO 기준점 v1 - 견종별 온도 · 흔들림(급가속 · 급제동 · 급회전) 판단 기준.
 *
 * 반려견 전용 가속도 기준 연구가 아직 없어서 사람 승객 · 운전 습관 연구값으로 시작하는 가설.
 * 근거: 개 열중립 범위 20~30°C(NRC) · 단모견 하한 15°C · 실내 사육 상한 29.5°C(USDA)
 * · 선 승객 편안 1.0~1.5m/s² · 좌우 불편 1.5 / 매우 1.75 / 극심 2.0m/s² · 운전 점수용 급제동 0.3~0.5g
 */
object PocoStandard {
    /** 모든 유형: 이 온도 이상이면 즉시 위험 경고 */
    const val DANGER_CELSIUS = 29.5f

    /** 반려견이 탄 자리에서 이 온도로 맞추자고 제안 (모든 유형의 쾌적 범위 안) */
    const val SUGGEST_CELSIUS = 22f

    // region ① 온도 기준 · 견종 유형

    /** 펫 프로필의 종류 · 몸무게 · 나이로 유형을 자동으로 정한다. 겹치면 모두 돌려준다 */
    fun petTypes(breed: String, weightKg: Float?, ageYears: Int?): Set<PetType> {
        val name = breed.replace(" ", "")
        if (PetType.CAT.matches(name)) return setOf(PetType.CAT)
        val types = buildSet {
            if (PetType.BRACHYCEPHALIC.matches(name)) add(PetType.BRACHYCEPHALIC)
            if (PetType.DOUBLE_COAT.matches(name)) add(PetType.DOUBLE_COAT)
            val isSmall = weightKg?.let { it < PetSize.SMALL_MAX_KG }
                ?: SMALL_BREEDS.any { name.contains(it) }
            if (isSmall || PetType.SMALL_SHORT_COAT.matches(name)) add(PetType.SMALL_SHORT_COAT)
            if (ageYears != null && (ageYears >= SENIOR_AGE || ageYears < PUPPY_AGE)) {
                add(PetType.SENIOR_PUPPY)
            }
        }
        return types.ifEmpty { setOf(PetType.BASIC) }
    }

    /** 유형이 겹칠 때는 더 엄격한 값 (예: 단두종 + 노견 → 더움 24°C · 추움 19°C) */
    fun temperature(types: Set<PetType>): TemperatureStandard {
        val list = types.ifEmpty { setOf(PetType.BASIC) }
        val hotAt = list.minOf { it.hotAt }
        val coldAt = list.maxOf { it.coldAt }
        val start = list.maxOf { it.comfort.start }
        val end = list.minOf { it.comfort.endInclusive }
        // 쾌적 범위가 서로 안 겹치면 알림 기준 사이 가운데로
        val comfort = if (start <= end) start..end else (coldAt + 1f)..(hotAt - 1f)
        return TemperatureStandard(list, comfort, hotAt = hotAt, coldAt = coldAt)
    }

    // endregion

    // region ② 흔들림 기준

    /** 앞뒤(급가속 · 급제동) 가속도 → 단계 */
    fun longitudinalGrade(accelMps2: Float, factor: Float = 1f): ShakeGrade =
        grade(accelMps2, factor, LONG_MILD_MPS2, LONG_EVENT_MPS2, DANGER_MPS2)

    /** 좌우(급회전) 가속도 → 단계 */
    fun lateralGrade(accelMps2: Float, factor: Float = 1f): ShakeGrade =
        grade(accelMps2, factor, LAT_MILD_MPS2, LAT_EVENT_MPS2, DANGER_MPS2)

    private fun grade(
        accelMps2: Float,
        factor: Float,
        mild: Float,
        event: Float,
        danger: Float
    ): ShakeGrade {
        val a = kotlin.math.abs(accelMps2)
        return when {
            a >= danger * factor -> ShakeGrade.DANGER
            a >= event * factor -> ShakeGrade.EVENT
            a >= mild * factor -> ShakeGrade.MILD
            else -> ShakeGrade.CALM
        }
    }

    const val LONG_MILD_MPS2 = 1.5f
    const val LONG_EVENT_MPS2 = 2.5f
    const val LAT_MILD_MPS2 = 1.5f
    const val LAT_EVENT_MPS2 = 2.0f
    const val DANGER_MPS2 = 4.0f

    // endregion

    private const val SENIOR_AGE = 8
    private const val PUPPY_AGE = 1

    /** 몸무게를 모를 때 소형으로 보는 견종 */
    private val SMALL_BREEDS = listOf(
        "몰티즈", "말티즈", "토이푸들", "푸들", "포메라니안", "비숑", "요크셔", "치와와", "시츄", "시추",
        "퍼그", "닥스훈트", "미니핀", "파피용"
    )
}

/** 흔들림 단계 (② 표의 4단계) */
enum class ShakeGrade {
    /** 기록만 · 평온 시간에 포함 */
    CALM,

    /** 감점 없음 · 같은 구간이 반복되면 리포트에 '조심할 곳' */
    MILD,

    /** 급제동 · 급가속 −5 · 급회전 −3 · 음성 안내 1회 */
    EVENT,

    /** 감점 + "정차 후 괜찮은지 확인해 주세요" */
    DANGER
}

/** ① 표: 견종 유형별 쾌적 범위 · 더움 알림(이상) · 추움 알림(이하) */
enum class PetType(
    val label: String,
    val comfort: ClosedFloatingPointRange<Float>,
    val hotAt: Float,
    val coldAt: Float,
    private val keywords: List<String> = emptyList()
) {
    /** 중 · 대형 · 보통 털 (비글 · 보더콜리 · 진돗개) */
    BASIC("기본", 20f..24f, hotAt = 26f, coldAt = 17f),

    /** 10kg 미만 또는 털 짧음 - 몸이 작고 털이 짧으면 열을 빨리 잃음 */
    SMALL_SHORT_COAT(
        "소형 · 단모",
        21f..25f,
        hotAt = 26f,
        coldAt = 18f,
        keywords = listOf("치와와", "닥스훈트", "그레이하운드", "휘핏", "핀셔", "미니핀", "도베르만", "바센지")
    ),

    /** 코가 짧아 헐떡임으로 열을 내보내기 어려움 → 더위 기준을 가장 엄격하게 */
    BRACHYCEPHALIC(
        "단두종",
        19f..23f,
        hotAt = 24f,
        coldAt = 17f,
        keywords = listOf(
            "퍼그", "프렌치불독", "불독", "불도그", "시츄", "시추", "페키니즈", "보스턴테리어", "복서",
            "샤페이", "카발리에", "킹찰스"
        )
    ),

    /** 털이 두껍고 빽빽함 - 추위엔 강하고 더위에 약함 */
    DOUBLE_COAT(
        "이중모 · 북방견",
        18f..22f,
        hotAt = 24f,
        coldAt = 12f,
        keywords = listOf(
            "허스키", "말라뮤트", "사모예드", "포메라니안", "스피츠", "시바", "아키타", "차우차우",
            "버니즈", "피레니즈"
        )
    ),

    /** 8살 이상 · 1살 미만 - 체온 조절 능력이 떨어져 위아래 모두 좁게 */
    SENIOR_PUPPY("노견 · 퍼피", 21f..24f, hotAt = 25f, coldAt = 19f),

    /** (참고) 고양이 - 차 안 기준으로 좁힘 */
    CAT(
        "고양이",
        20f..25f,
        hotAt = 26f,
        coldAt = 18f,
        keywords = listOf("고양이", "코숏", "코리안숏헤어", "페르시안", "러시안블루", "브리티시", "먼치킨", "스코티시")
    );

    fun matches(breedNoSpace: String): Boolean = keywords.any { breedNoSpace.contains(it) }
}

/** 겹친 유형을 합친 이 펫의 온도 기준 */
data class TemperatureStandard(
    val types: Set<PetType>,
    val comfort: ClosedFloatingPointRange<Float>,
    /** 이 온도 이상이면 더움 알림 */
    val hotAt: Float,
    /** 이 온도 이하면 추움 알림 */
    val coldAt: Float
) {
    val label: String get() = types.joinToString(" + ") { it.label }

    fun isHot(celsius: Float) = celsius >= hotAt

    fun isCold(celsius: Float) = celsius <= coldAt

    fun isDanger(celsius: Float) = celsius >= PocoStandard.DANGER_CELSIUS
}

/** 이 펫의 온도 기준 (종류 · 몸무게 · 나이로 자동) */
val Pet.temperatureStandard: TemperatureStandard
    get() = PocoStandard.temperature(PocoStandard.petTypes(breed, weightKg, ageYears))

/**
 * ③ 반려동물별 보정: 흔들림 기준점에 곱한다 (작을수록 더 작은 흔들림부터 알림).
 * 멀미 민감도(낮음 1.2 · 보통 1.0 · 높음 0.8) × 크기(소형 0.9) × 고정 방식(이동장 · 하네스 1.2)
 */
val Pet.shakeThresholdFactor: Float
    get() = motionSensitivity.thresholdFactor *
        (if (size == PetSize.SMALL) SMALL_SIZE_FACTOR else 1f) *
        restraint.thresholdFactor

private const val SMALL_SIZE_FACTOR = 0.9f
