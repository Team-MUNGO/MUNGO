package com.example.poco.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * 디자인 시스템 DS-01 Color (Light 기본 + Night)
 * 이름 옆 주석 = 피그마 토큰 이름
 */

// region Brand
val brandNavy = Color(0xFF121F3D) // brand/navy: 기본 버튼 · 제목 · 로고
val brandLime = Color(0xFFA9FF65) // brand/lime: 점수 링 · 체크 · 토글 노브
val brandLimeSoft = Color(0xFFE6FBD3) // brand/lime-soft: 칩 · 성공 배경 · 선택 자리
val brandTangerine = Color(0xFFFF8A00) // brand/tangerine: 경고 아이콘 · 이벤트 막대 · 로고 코 (면·아이콘 전용, 글자 금지)
// endregion

// region Neutral
val bgDefault = Color(0xFFEEF3F8) // bg/default: 앱 배경 · 입력칸 · 기본 세그먼트
val bgCard = Color(0xFFFFFFFF) // bg/card: 카드 · 상태 바
val borderDefaultColor = Color(0xFFE1E8F0) // border/default: 카드 구분선 · 칩 테두리
val borderInputColor = Color(0xFFD6E0EA) // border/input: 입력 · 비선택 칩
val borderStrongColor = Color(0xFFB9C7D6) // border/strong: 보조 버튼 · 점선 버튼
val textPrimaryColor = Color(0xFF121F3D) // text/primary: 본문 · 제목
val textSecondaryColor = Color(0xFF587083) // text/secondary: 라벨 · 설명
val textDisabledColor = Color(0xFF9AA7B3) // text/disabled: 비활성 · 빈 상태 제목
// endregion

// region Status (주행 중 알림 상태 · 글자색은 4.5:1 통과값)
val warnText = Color(0xFFA3472A) // warn/text: 더움 · 감점 글자
val warnBg = Color(0xFFFFF1E0) // warn/bg: 더움 알림 · 보통 칩
val warnBorder = Color(0xFFF2A06A) // warn/border: 경고 카드 테두리
val coldText = Color(0xFF1D4ED8) // cold/text: 추움 글자
val coldBg = Color(0xFFEAF2FE) // cold/bg: 추움 알림 배경
val coldAccent = Color(0xFF2563EB) // cold/accent: 추움 아이콘 원
val successText = Color(0xFF276A39) // success/text: 가산 점수 · 완료 문구
val successAccent = Color(0xFF7BC943) // success/accent: 탑승 자리 · 창문 OK 표시
// endregion

// region Night (야간 테마 · Pleos 가이드 필수)
val nightBg = Color(0xFF121F3D) // night/bg
val nightCard = Color(0xFF1B2B4F) // night/card
val nightBorder = Color(0xFF2E4372) // night/border
val nightText = Color(0xFFF5F8FF) // night/text
val nightText2 = Color(0xFF9AA7B3) // night/text-2
val nightAccent = Color(0xFFA9FF65) // night/accent

// 시안에 야간 값이 없는 토큰 → 야간 배경에서 보이도록 임시로 정한 값 (디자이너 확인 필요)
private val nightField = Color(0xFF233660)
private val nightLimeSoft = Color(0xFF2C4A3C)
private val nightWarnText = Color(0xFFFFB38A)
private val nightWarnBg = Color(0xFF3A2C2A)
private val nightColdText = Color(0xFF8DB4FF)
private val nightColdBg = Color(0xFF1E2F5C)
private val nightSuccessText = Color(0xFF9BE07A)
// endregion

// 예전 이름 (팀 코드 호환용)
val primaryMain = Color(0xFF043B78)
val primaryLight = Color(0xFFE8F1FA)
val primaryPressed = Color(0xFF092E55)
val textPrimary = textPrimaryColor
val textSecondary = textSecondaryColor
val textOnPrimary = Color(0xFFFFFFFF)
val backgroundDefault = bgDefault
val backgroundCard = bgCard
val statusSuccess = successText
val statusWarning = warnBorder
val statusError = Color(0xFFE35D5D) // 시안에 없음
val statusInfo = coldAccent
val borderDefault = borderDefaultColor
val logoColor = brandLime
val logoBackground = brandNavy

@Immutable
data class POCOColors(
    val primaryMain: Color,
    val primaryLight: Color,
    val primaryPressed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnPrimary: Color,
    val backgroundDefault: Color,
    val backgroundCard: Color,
    val statusSuccess: Color,
    val statusWarning: Color,
    val statusError: Color,
    val statusInfo: Color,
    val borderDefault: Color,
    /** brand/lime (두 모드 공통) */
    val logoColor: Color,
    /** brand/navy (두 모드 공통) */
    val logoBackground: Color,
    /** 주 버튼 · 선택된 세그먼트 · 켜진 토글 트랙. 라이트 = 남색, 다크 = 라임 */
    val accentFill: Color = brandNavy,
    /** [accentFill] 위의 글자 · 아이콘. 라이트 = 흰색, 다크 = 남색 */
    val onAccentFill: Color = Color.White,
    /** 켜진 토글의 원. 라이트 = 라임, 다크 = 남색 */
    val toggleThumb: Color = brandLime,
    /** 입력칸 · 비선택 세그먼트 배경 (= bg/default) */
    val fieldBackground: Color = bgDefault,
    /** 안심 · 확인 의미의 초록 (= success/text) */
    val safe: Color = com.example.poco.core.theme.successText,
    /** 로고 글자 · 강아지 몸. 라이트 = 남색, 다크 = 라임 */
    val logoInk: Color = brandNavy,
    /** 로고 강아지 얼굴 · 핸들 안쪽. 라이트 = 흰색, 다크 = 남색 */
    val logoPaper: Color = Color.White,
    val limeSoft: Color = brandLimeSoft,
    val tangerine: Color = brandTangerine,
    val borderInput: Color = borderInputColor,
    val borderStrong: Color = borderStrongColor,
    val textDisabled: Color = textDisabledColor,
    val warnText: Color = com.example.poco.core.theme.warnText,
    val warnBg: Color = com.example.poco.core.theme.warnBg,
    val warnBorder: Color = com.example.poco.core.theme.warnBorder,
    val coldText: Color = com.example.poco.core.theme.coldText,
    val coldBg: Color = com.example.poco.core.theme.coldBg,
    val coldAccent: Color = com.example.poco.core.theme.coldAccent,
    val successText: Color = com.example.poco.core.theme.successText,
    val successAccent: Color = com.example.poco.core.theme.successAccent,
    val isDark: Boolean = false
)

val defaultPOCOColors = POCOColors(
    primaryMain = primaryMain,
    primaryLight = primaryLight,
    primaryPressed = primaryPressed,
    textPrimary = textPrimary,
    textSecondary = textSecondary,
    textOnPrimary = textOnPrimary,
    backgroundDefault = backgroundDefault,
    backgroundCard = backgroundCard,
    statusSuccess = statusSuccess,
    statusWarning = statusWarning,
    statusError = statusError,
    statusInfo = statusInfo,
    borderDefault = borderDefault,
    logoColor = logoColor,
    logoBackground = logoBackground
)

val darkPOCOColors = POCOColors(
    primaryMain = nightAccent,
    primaryLight = nightField,
    primaryPressed = Color(0xFF8FE04C),
    textPrimary = nightText,
    textSecondary = nightText2,
    textOnPrimary = brandNavy,
    backgroundDefault = nightBg,
    backgroundCard = nightCard,
    statusSuccess = nightSuccessText,
    statusWarning = warnBorder,
    statusError = Color(0xFFFF7A7A),
    statusInfo = nightColdText,
    borderDefault = nightBorder,
    logoColor = brandLime,
    logoBackground = brandNavy,
    accentFill = nightAccent,
    onAccentFill = brandNavy,
    toggleThumb = brandNavy,
    fieldBackground = nightField,
    safe = nightSuccessText,
    logoInk = nightAccent,
    logoPaper = nightBg,
    limeSoft = nightLimeSoft,
    borderInput = nightBorder,
    borderStrong = Color(0xFF4A6197),
    textDisabled = Color(0xFF6B7A95),
    warnText = nightWarnText,
    warnBg = nightWarnBg,
    coldText = nightColdText,
    coldBg = nightColdBg,
    successText = nightSuccessText,
    isDark = true
)

val LocalPOCOColorsProvider = staticCompositionLocalOf { defaultPOCOColors }
