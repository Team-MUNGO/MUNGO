package com.example.poco.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * mungo 디자인 시스템 · Foundations / Color
 * - 포인트 컬러는 망고 1가지 (블랙 5 : 그레이 4 : 망고 1)
 * - 민트 · 앰버 · 빨강 · 파랑은 상태 칩과 경고에만 쓴다
 * - Page 3 라이트 화면: #FF8200 포인트, #191F28 본문, #6B7684 보조, 주요 버튼 흰 글자
 */

/** 원색 팔레트 (피그마 Color Palette) */
object MungoPalette {
    // Mango · 포인트 (1)
    val mango50 = Color(0xFFFFF4E5)
    val mango100 = Color(0xFFFFE6C7)
    val mango200 = Color(0xFFFFCC8F)
    val mango300 = Color(0xFFFFB257)
    val mango400 = Color(0xFFFF9B26)
    val mango500 = Color(0xFFFF8400) // mungo Mango · primary/main
    val mango600 = Color(0xFFE06F00)
    val mango700 = Color(0xFFA85200)
    val mango800 = Color(0xFF7A3B00)
    val mango900 = Color(0xFF4A2400)

    // Neutral · 정보 · 면 (4)
    val neutral50 = Color(0xFFFAFAFA)
    val neutral100 = Color(0xFFF5F5F5)
    val neutral150 = Color(0xFFF0F0F0)
    val neutral200 = Color(0xFFE6E6E6)
    val neutral300 = Color(0xFFD9D9D9)
    val neutral400 = Color(0xFFC5C5C5)
    val neutral500 = Color(0xFF929292)
    val neutral600 = Color(0xFF808080)
    val neutral700 = Color(0xFF666666)
    val neutral800 = Color(0xFF4D4D4D)
    val neutral900 = Color(0xFF111111) // mungo Black · text/primary

    // Dark · 다크 면 (5)
    val dark700 = Color(0xFF56585D)
    val dark750 = Color(0xFF303237)
    val dark800 = Color(0xFF2E2F34)
    val dark850 = Color(0xFF2A2A2A)
    val dark900 = Color(0xFF212121)
    val dark950 = Color(0xFF18191B)

    // Mint · 상태: 편안함
    val mint50 = Color(0xFFE6FAF5)
    val mint400 = Color(0xFF2CC4A3)
    val mint500 = Color(0xFF14A88A)
    val mint700 = Color(0xFF08705D)
    val mint900 = Color(0xFF053B33)

    // Amber · 상태: 휴식 필요
    val amber50 = Color(0xFFFFF5E0)
    val amber400 = Color(0xFFFFC247)
    val amber500 = Color(0xFFF2A20C)
    val amber700 = Color(0xFF8C5300)
    val amber900 = Color(0xFF3D2A00)

    // Red · 상태: 주의
    val red50 = Color(0xFFFDECEC)
    val red400 = Color(0xFFFF6B6B)
    val red500 = Color(0xFFEB4747)
    val red700 = Color(0xFFB02828)
    val red900 = Color(0xFF401516)

    // Blue · 상태: 정보
    val blue50 = Color(0xFFE5F4FB)
    val blue400 = Color(0xFF5CB8FF)
    val blue500 = Color(0xFF1FA0E6)
    val blue700 = Color(0xFF006A94)
    val blue900 = Color(0xFF0A2E40)

    val white = Color(0xFFFFFFFF)
}

/**
 * 화면 색 토큰.
 * - 앞쪽(Semantic Color): 새 화면 · 컴포넌트는 이 이름을 쓴다
 * - 뒤쪽(예전 이름): 기존 화면 호환용. 값은 mungo 팔레트로 옮겨 두었다
 */
@Immutable
data class POCOColors(
    // region bg
    /** 화면 바탕 */
    val bgCanvas: Color,
    /** 카드 · 패널 기본 면 */
    val bgSurface: Color,
    /** 떠 있는 카드 · 팝업 (다크는 그림자 대신 한 단계 밝게) */
    val bgSurfaceRaised: Color,
    /** 입력칸 · 눌린 영역 */
    val bgSurfaceSunken: Color,
    /** 주요 버튼 · 활성 상태 */
    val bgAccent: Color,
    /** 선택된 항목 배경 */
    val bgAccentSubtle: Color,
    /** 중립 강조 버튼 (Pleos 저장 버튼 톤) */
    val bgNeutralStrong: Color,
    /** 보조 버튼 */
    val bgNeutral: Color,
    /** 팝업 뒤 딤 */
    val bgScrim: Color,
    // endregion
    // region text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    /** 강조 텍스트 · 링크 */
    val textAccent: Color,
    /** accent 배경 위 글자 */
    val textOnAccent: Color,
    /** neutral-strong 배경 위 글자 */
    val textOnNeutralStrong: Color,
    // endregion
    // region border
    val borderDefault: Color,
    val borderStrong: Color,
    /** 선택 · 포커스 테두리 */
    val borderAccent: Color,
    // endregion
    // region status
    /** 반려동물 편안함 (Positive) */
    val statusCalm: Color,
    val statusCalmSubtle: Color,
    /** 휴식 필요 (Warning) */
    val statusRest: Color,
    val statusRestSubtle: Color,
    /** 주의 필요 (Negative) */
    val statusAlert: Color,
    val statusAlertSubtle: Color,
    /** 정보 (Link) */
    val statusInfo: Color,
    val statusInfoSubtle: Color,
    // endregion
    /** 눌림 오버레이 (Hover는 차량에 없음) */
    val statePressed: Color,
    /** 그림자 (라이트 모드만) */
    val shadow: Color,
    val isDark: Boolean,

    // region 예전 이름 (기존 화면 호환용 → 위 토큰으로 연결)
    val primaryMain: Color = bgAccent,
    val primaryLight: Color = bgAccentSubtle,
    val primaryPressed: Color = MungoPalette.mango600,
    val textOnPrimary: Color = textOnAccent,
    val backgroundDefault: Color = bgCanvas,
    val backgroundCard: Color = bgSurface,
    val statusSuccess: Color = statusCalm,
    val statusWarning: Color = statusRest,
    val statusError: Color = statusAlert,
    /** 점수 링 · 확인 원 (예전 라임) → 망고 */
    val logoColor: Color = bgAccent,
    /** 진한 태그 · 상태 칩 바탕 (예전 남색) → 진회색 */
    val logoBackground: Color = MungoPalette.dark750,
    val accentFill: Color = bgAccent,
    val onAccentFill: Color = textOnAccent,
    val toggleThumb: Color = MungoPalette.white,
    val fieldBackground: Color = bgSurfaceSunken,
    val safe: Color = statusCalm,
    val limeSoft: Color = bgAccentSubtle,
    val tangerine: Color = bgAccent,
    val borderInput: Color = borderDefault,
    val warnText: Color = statusRest,
    val warnBg: Color = statusRestSubtle,
    val warnBorder: Color = if (isDark) MungoPalette.amber700 else MungoPalette.amber400,
    val coldText: Color = statusInfo,
    val coldBg: Color = statusInfoSubtle,
    val coldAccent: Color = if (isDark) MungoPalette.blue400 else MungoPalette.blue500,
    val successText: Color = statusCalm,
    val successAccent: Color = if (isDark) MungoPalette.mint400 else MungoPalette.mint500
    // endregion
)

val defaultPOCOColors = POCOColors(
    bgCanvas = Color(0xFFF4F5F7),
    bgSurface = MungoPalette.white,
    bgSurfaceRaised = MungoPalette.white,
    bgSurfaceSunken = Color(0xFFF2F4F6),
    bgAccent = Color(0xFFFF8200),
    bgAccentSubtle = Color(0xFFFFF7EE),
    bgNeutralStrong = Color(0xFF191F28),
    bgNeutral = MungoPalette.neutral150,
    bgScrim = Color(0x80000000),
    textPrimary = Color(0xFF191F28),
    textSecondary = Color(0xFF6B7684),
    textTertiary = Color(0xFF8B95A1),
    textDisabled = MungoPalette.neutral400,
    textAccent = Color(0xFFB35400),
    textOnAccent = Color.White,
    textOnNeutralStrong = MungoPalette.white,
    borderDefault = Color(0xFFE5E8EB),
    borderStrong = MungoPalette.neutral400,
    borderAccent = Color(0xFFFF8200),
    statusCalm = MungoPalette.mint700,
    statusCalmSubtle = MungoPalette.mint50,
    statusRest = MungoPalette.amber700,
    statusRestSubtle = MungoPalette.amber50,
    statusAlert = MungoPalette.red700,
    statusAlertSubtle = MungoPalette.red50,
    statusInfo = MungoPalette.blue700,
    statusInfoSubtle = MungoPalette.blue50,
    statePressed = Color(0x14000000), // 검정 8%
    shadow = Color(0x1A000000),
    isDark = false
)

val darkPOCOColors = POCOColors(
    bgCanvas = MungoPalette.dark950,
    bgSurface = MungoPalette.dark900,
    bgSurfaceRaised = MungoPalette.dark850,
    bgSurfaceSunken = MungoPalette.dark950,
    bgAccent = MungoPalette.mango400,
    bgAccentSubtle = MungoPalette.mango900,
    bgNeutralStrong = MungoPalette.neutral200,
    bgNeutral = MungoPalette.dark750,
    bgScrim = Color(0x99000000),
    textPrimary = MungoPalette.white,
    textSecondary = MungoPalette.neutral400,
    textTertiary = MungoPalette.neutral500,
    textDisabled = MungoPalette.neutral800,
    textAccent = MungoPalette.mango400,
    textOnAccent = MungoPalette.neutral900,
    textOnNeutralStrong = MungoPalette.neutral900,
    borderDefault = MungoPalette.dark800,
    borderStrong = MungoPalette.dark700,
    borderAccent = MungoPalette.mango400,
    statusCalm = MungoPalette.mint400,
    statusCalmSubtle = MungoPalette.mint900,
    statusRest = MungoPalette.amber400,
    statusRestSubtle = MungoPalette.amber900,
    statusAlert = MungoPalette.red400,
    statusAlertSubtle = MungoPalette.red900,
    statusInfo = MungoPalette.blue400,
    statusInfoSubtle = MungoPalette.blue900,
    statePressed = Color(0x1FFFFFFF), // 흰색 12%
    shadow = Color.Transparent,
    isDark = true
)

val LocalPOCOColorsProvider = staticCompositionLocalOf { defaultPOCOColors }
