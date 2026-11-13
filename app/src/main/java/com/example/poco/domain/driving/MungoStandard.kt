package com.example.poco.domain.driving

import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSize

/**
 * mungo 기준점 v2 - 온도 · 노령견 · 흔들림(급가속 · 급제동 · 급회전) 판단 기준.
 *
 * 온도: 개 열중립 범위 20~30°C(Purdue) · 24°C 이상 주의 · 27°C 초과 열사병 위험(동물병원 가이드).
 * 20~30°C는 쉬고 있는 개 기준이라 차 안(햇빛 · 좁은 공간 · 흔들림)은 범위 아래쪽을 쾌적 구간으로 잡는다.
 *
 * | 단계     | 일반 반려견 | 위험군 (단두종 · 노령견 · 어린 강아지) |
 * | 쾌적     | 20~25°C    | 20~24°C |
 * | 주의     | 26~27°C    | 25~26°C |
 * | 위험     | 28°C 이상   | 27°C 이상 |
 * | 너무 낮음 | 20°C 미만   | 20°C 미만 |
 *
 * 노령견: AAHA 기준 예상 수명의 마지막 25%. 몸무게로 대략 소형(10kg 미만) 10살 · 중대형(10~40kg) 8살
 * · 초대형(40kg 초과) 6살부터.
 *
 * 흔들림: 반려견 전용 가속도 연구가 아직 없어서 사람 승객 · 운전 습관 연구값으로 시작하는 가설.
 * 선 승객 편안 1.0~1.5m/s² · 좌우 불편 1.5 / 매우 1.75 / 극심 2.0m/s² · 운전 점수용 급제동 0.3~0.5g
 */
object MungoStandard {
    /** 반려견이 탄 자리에서 이 온도로 맞추자고 제안 (일반 · 위험군 모두 쾌적 범위 안) */
    const val SUGGEST_CELSIUS = 22f

    /** 이 온도 미만이면 "추울 수 있어요" (일반 · 위험군 공통) */
    const val COLD_BELOW_CELSIUS = 20f

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
            if (isSenior(breed, weightKg, ageYears) || (ageYears != null && ageYears < PUPPY_AGE)) {
                add(PetType.SENIOR_PUPPY)
            }
        }
        return types.ifEmpty { setOf(PetType.BASIC) }
    }

    /** 단두종 · 노령견 · 어린 강아지가 하나라도 있으면 위험군 기준 */
    fun temperature(types: Set<PetType>): TemperatureStandard {
        val list = types.ifEmpty { setOf(PetType.BASIC) }
        val group = if (list.any {
                it.riskGroup
            }
        ) {
            TemperatureGroup.RISK
        } else {
            TemperatureGroup.GENERAL
        }
        return TemperatureStandard(list, group)
    }

    // endregion

    // region 노령견

    /** 노령견이 시작되는 나이: 소형 10살 · 중대형 8살 · 초대형 6살 (몸무게를 모르면 견종으로 추정) */
    fun seniorAge(breed: String, weightKg: Float?): Int {
        val name = breed.replace(" ", "")
        return when {
            weightKg == null ->
                if (SMALL_BREEDS.any { name.contains(it) }) SMALL_SENIOR_AGE else MEDIUM_SENIOR_AGE
            weightKg < SMALL_MAX_KG -> SMALL_SENIOR_AGE
            weightKg <= LARGE_MAX_KG -> MEDIUM_SENIOR_AGE
            else -> GIANT_SENIOR_AGE
        }
    }

    fun isSenior(breed: String, weightKg: Float?, ageYears: Int?): Boolean =
        ageYears != null && ageYears >= seniorAge(breed, weightKg)

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

    private const val PUPPY_AGE = 1
    private const val SMALL_MAX_KG = 10f
    private const val LARGE_MAX_KG = 40f
    private const val SMALL_SENIOR_AGE = 10
    private const val MEDIUM_SENIOR_AGE = 8
    private const val GIANT_SENIOR_AGE = 6

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

/**
 * 견종 유형 (안내 문구 · 위험군 판단용). 온도 숫자는 [TemperatureGroup] 두 가지만 쓴다.
 * [riskGroup] = 동물병원 가이드의 위험군 (단두종 · 노령견 · 어린 강아지)
 */
enum class PetType(
    val label: String,
    val riskGroup: Boolean = false,
    private val keywords: List<String> = emptyList()
) {
    /** 중 · 대형 · 보통 털 (비글 · 보더콜리 · 진돗개) */
    BASIC("기본"),

    /** 10kg 미만 또는 털 짧음 */
    SMALL_SHORT_COAT(
        "소형 · 단모",
        keywords = listOf("치와와", "닥스훈트", "그레이하운드", "휘핏", "핀셔", "미니핀", "도베르만", "바센지")
    ),

    /** 코가 짧아 헐떡임으로 열을 내보내기 어려움 */
    BRACHYCEPHALIC(
        "단두종",
        riskGroup = true,
        keywords = listOf(
            "퍼그", "프렌치불독", "불독", "불도그", "시츄", "시추", "페키니즈", "보스턴테리어", "복서",
            "샤페이", "카발리에", "킹찰스"
        )
    ),

    /** 털이 두껍고 빽빽함 */
    DOUBLE_COAT(
        "이중모 · 북방견",
        keywords = listOf(
            "허스키", "말라뮤트", "사모예드", "포메라니안", "스피츠", "시바", "아키타", "차우차우",
            "버니즈", "피레니즈"
        )
    ),

    /** 노령견(크기별 나이) · 1살 미만 - 체온 조절 능력이 떨어짐 */
    SENIOR_PUPPY("노견 · 퍼피", riskGroup = true),

    /** (참고) 고양이 */
    CAT(
        "고양이",
        keywords = listOf("고양이", "코숏", "코리안숏헤어", "페르시안", "러시안블루", "브리티시", "먼치킨", "스코티시")
    );

    fun matches(breedNoSpace: String): Boolean = keywords.any { breedNoSpace.contains(it) }
}

/** ① 표: 일반 반려견 · 위험군 온도 기준 */
enum class TemperatureGroup(
    val label: String,
    val comfort: ClosedFloatingPointRange<Float>,
    /** 이 온도부터 주의 (실내 온도 카드 주황 테두리) */
    val cautionAt: Float,
    /** 이 온도 이상이면 위험 (경고 · "에어컨을 켜 주세요") */
    val dangerAt: Float
) {
    GENERAL("일반", 20f..25f, cautionAt = 26f, dangerAt = 28f),
    RISK("위험군", 20f..24f, cautionAt = 25f, dangerAt = 27f)
}

/** 겹친 유형을 합친 이 펫의 온도 기준 */
data class TemperatureStandard(val types: Set<PetType>, val group: TemperatureGroup) {
    val comfort: ClosedFloatingPointRange<Float> get() = group.comfort

    /** 이 온도부터 더움(주의) */
    val hotAt: Float get() = group.cautionAt

    /** 이 온도 미만이면 추움 */
    val coldBelow: Float get() = MungoStandard.COLD_BELOW_CELSIUS

    val dangerAt: Float get() = group.dangerAt

    val label: String get() = types.joinToString(" + ") { it.label }

    /** 쾌적 범위 위 (주의 이상) */
    fun isHot(celsius: Float) = celsius > comfort.endInclusive

    fun isCold(celsius: Float) = celsius < coldBelow

    fun isDanger(celsius: Float) = celsius >= dangerAt
}

/** 이 펫이 노령견인지 (크기별 나이) */
val Pet.isSenior: Boolean get() = MungoStandard.isSenior(breed, weightKg, ageYears)

/** 이 펫의 온도 기준 (종류 · 몸무게 · 나이로 자동) */
val Pet.temperatureStandard: TemperatureStandard
    get() = MungoStandard.temperature(MungoStandard.petTypes(breed, weightKg, ageYears))

/**
 * ③ 반려동물별 보정: 흔들림 기준점에 곱한다 (작을수록 더 작은 흔들림부터 알림).
 * 멀미 민감도(낮음 1.2 · 보통 1.0 · 높음 0.8) × 크기(소형 0.9) × 고정 방식(이동장 · 하네스 1.2)
 */
val Pet.shakeThresholdFactor: Float
    get() = motionSensitivity.thresholdFactor *
        (if (size == PetSize.SMALL) SMALL_SIZE_FACTOR else 1f) *
        restraint.thresholdFactor

private const val SMALL_SIZE_FACTOR = 0.9f
