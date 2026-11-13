@file:OptIn(ExperimentalTextApi::class)

package com.example.poco.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.poco.R

/*
 * 디자인 시스템 DS-01 Font · DS-02 Typography
 * - Page 3 홈 · 온보딩 · 프로필 · 이동 기록: Pretendard (400 · 500 · 600 · 700)
 * - 설정 · 주행 · 도착 요약: Noto Sans KR (가변 글꼴, DesignCanvas에서 선택)
 * - 로고 글꼴: Fredoka Bold (mungo 글자 로고 전용, 화면 글꼴로 쓰지 않음)
 * - Pleos 1920×1080 · 밀도 160 → 시안 px = sp 그대로
 */

private fun notoSansKr(weight: FontWeight) = Font(
    resId = R.font.noto_sans_kr,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val mungoNotoFontFamily = FontFamily(
    notoSansKr(FontWeight.Normal),
    notoSansKr(FontWeight.Medium),
    notoSansKr(FontWeight.SemiBold),
    notoSansKr(FontWeight.Bold)
)

/** Page 3 home, onboarding, profile and trip-report typeface. */
val mungoFontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)

/** 로고 전용 (Fredoka Bold) */
val mungoLogoFontFamily = FontFamily(
    Font(
        resId = R.font.fredoka,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

// 예전 이름 (팀 코드 호환용) — 굵기는 TextStyle에서 지정한다
val mungoFontBold = mungoFontFamily
val mungoFontSemiBold = mungoFontFamily
val mungoFontMedium = mungoFontFamily
val mungoFontRegular = mungoFontFamily

private fun mungoTextStyle(
    fontWeight: FontWeight,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = 0.sp
): TextStyle = TextStyle(
    fontFamily = mungoFontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

// region DS-02 토큰 (크기 · 행간 · 굵기 · 자간)

/** display/xl · 주행 중 점수 (가로) · 주행 OK */
private val displayXl = mungoTextStyle(FontWeight.Bold, 120.sp, 120.sp, (-2.5).sp)

/** display/l · 홈 · 세로 점수 · 주행 OK */
private val displayL = mungoTextStyle(FontWeight.Bold, 96.sp, 100.sp, (-2).sp)

/** heading/xl · 화면 제목 · 이름 · 주행 시작 · 주행 OK */
private val headingXl = mungoTextStyle(FontWeight.Bold, 52.sp, 64.sp, (-1).sp)

/** heading/l · 가운데 배너 제목 · 주행 OK */
private val headingL = mungoTextStyle(FontWeight.Bold, 44.sp, 56.sp, (-1).sp)

/** heading/m · 현댕이 상태 · 리포트 제목 · 주행 OK */
private val headingM = mungoTextStyle(FontWeight.Bold, 36.sp, 48.sp, (-0.5).sp)

/** title/m · 상태 값 · 일반 버튼 · 주행 OK */
private val titleM = mungoTextStyle(FontWeight.Bold, 28.sp, 40.sp, (-0.5).sp)

/** body/l · 세그먼트 · 입력값 · 목록 제목 · 주행 OK */
private val bodyL = mungoTextStyle(FontWeight.Medium, 24.sp, 36.sp)

/** body/m · 카드 라벨 · 설명 · 주행 중엔 라벨만 */
private val bodyM = mungoTextStyle(FontWeight.Normal, 22.sp, 32.sp)

/** caption · 보조 설명 · 축 라벨 · 주행 중엔 라벨만 */
private val captionStyle = mungoTextStyle(FontWeight.Normal, 20.sp, 28.sp)

/** micro · 태그 · 힌트 · 정차 전용 */
private val micro = mungoTextStyle(FontWeight.Bold, 18.sp, 26.sp)

/** nano · 게이지 눈금 · NEW 뱃지 · 주행 중엔 라벨만 */
private val nano = mungoTextStyle(FontWeight.Bold, 16.sp, 22.sp)

// endregion

// region mungo Typography (Car · 1920×1080 가정, 시안 px = sp)

/** display/display1 · 144 Bold 100% · 온도 · 남은 시간 같은 큰 숫자 */
private val display1 = mungoTextStyle(FontWeight.Bold, 144.sp, 144.sp)

/** display/display2 · 96 Regular 115% · 정차 중 한 문장 메시지 */
private val display2 = mungoTextStyle(FontWeight.Normal, 96.sp, 110.sp)

/** heading/h1 · 64 Bold 120% · 정차 화면 대제목 */
private val h1 = mungoTextStyle(FontWeight.Bold, 64.sp, 77.sp)

/** heading/h2 · 48 Bold 125% · 주행 중 칸 핵심 정보 */
private val h2 = mungoTextStyle(FontWeight.Bold, 48.sp, 60.sp)

/** heading/h3 · 40 Bold 130% · 카드 제목 */
private val h3 = mungoTextStyle(FontWeight.Bold, 40.sp, 52.sp)

/** title/title1 · 32 Bold 130% · 섹션 제목 */
private val title1 = mungoTextStyle(FontWeight.Bold, 32.sp, 42.sp)

/** title/title2 · 28 Medium 135% · 리스트 제목 */
private val title2 = mungoTextStyle(FontWeight.Medium, 28.sp, 38.sp)

/** title/title3 · 24 Medium 140% · 작은 제목 · 버튼 라벨 */
private val title3 = mungoTextStyle(FontWeight.Medium, 24.sp, 34.sp)

/** body/body1 · 28 Regular 150% · 본문 큰 */
private val body1 = mungoTextStyle(FontWeight.Normal, 28.sp, 42.sp)

/** body/body2 · 24 Regular 150% · 본문 기본 */
private val body2 = mungoTextStyle(FontWeight.Normal, 24.sp, 36.sp)

/** body/body3 · 20 Regular 150% · 보조 설명 */
private val body3 = mungoTextStyle(FontWeight.Normal, 20.sp, 30.sp)

/** caption/caption1 · 18 Regular 150% · 캡션 (차량 최소 크기) */
private val caption1 = mungoTextStyle(FontWeight.Normal, 18.sp, 27.sp)

// endregion

/**
 * 글꼴 토큰.
 * - 앞쪽(display1 ~ caption1): mungo 시안 토큰. 새 화면 · 컴포넌트는 이것을 쓴다
 * - 뒤쪽(displayXl ~ nano, hmi*): 예전 DS-02 토큰. 기존 화면 호환용
 * 주행 중 최소 크기: 핵심 정보 32 이상 · 보조 24 이상 · 24 미만은 값 위의 짧은 라벨에만.
 */
@Immutable
data class MungoTypography(
    val display1: TextStyle = com.example.poco.core.theme.display1,
    val display2: TextStyle = com.example.poco.core.theme.display2,
    val h1: TextStyle = com.example.poco.core.theme.h1,
    val h2: TextStyle = com.example.poco.core.theme.h2,
    val h3: TextStyle = com.example.poco.core.theme.h3,
    val title1: TextStyle = com.example.poco.core.theme.title1,
    val title2: TextStyle = com.example.poco.core.theme.title2,
    val title3: TextStyle = com.example.poco.core.theme.title3,
    val body1: TextStyle = com.example.poco.core.theme.body1,
    val body2: TextStyle = com.example.poco.core.theme.body2,
    val body3: TextStyle = com.example.poco.core.theme.body3,
    val caption1: TextStyle = com.example.poco.core.theme.caption1,
    // 예전 DS-02 토큰
    val displayXl: TextStyle = com.example.poco.core.theme.displayXl,
    val displayL: TextStyle = com.example.poco.core.theme.displayL,
    val headingXl: TextStyle = com.example.poco.core.theme.headingXl,
    val headingL: TextStyle = com.example.poco.core.theme.headingL,
    val headingM: TextStyle = com.example.poco.core.theme.headingM,
    val titleM: TextStyle = com.example.poco.core.theme.titleM,
    val bodyL: TextStyle = com.example.poco.core.theme.bodyL,
    val bodyM: TextStyle = com.example.poco.core.theme.bodyM,
    val caption: TextStyle = captionStyle,
    val micro: TextStyle = com.example.poco.core.theme.micro,
    val nano: TextStyle = com.example.poco.core.theme.nano,
    val score: TextStyle = displayL,
    val hmiHeading1: TextStyle = headingXl,
    val hmiTitle: TextStyle = titleM,
    val hmiButton: TextStyle = titleM,
    val hmiBody: TextStyle = bodyL,
    val hmiLabel: TextStyle = bodyL,
    val hmiCaption: TextStyle = captionStyle
)

val defaultMungoTypography = MungoTypography()

val LocalMungoTypographyProvider = staticCompositionLocalOf { defaultMungoTypography }
