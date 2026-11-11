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
 * - 본문 글꼴: Noto Sans KR, 굵기 3단계만 (Regular 400 · Medium 500 · Bold 700)
 *   가변 글꼴(variable font) 한 파일로 세 굵기를 모두 쓴다
 * - 로고 글꼴: Fredoka Bold (로고 전용, 화면 글꼴로 쓰지 않음)
 * - Pleos 1920×1080 · 밀도 160 → 시안 px = sp 그대로
 */

private fun notoSansKr(weight: FontWeight) = Font(
    resId = R.font.noto_sans_kr,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val pocoFontFamily = FontFamily(
    notoSansKr(FontWeight.Normal),
    notoSansKr(FontWeight.Medium),
    notoSansKr(FontWeight.Bold)
)

/** 로고 전용 (Fredoka Bold) */
val pocoLogoFontFamily = FontFamily(
    Font(
        resId = R.font.fredoka,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

// 예전 이름 (팀 코드 호환용) - 모두 Noto Sans KR
val pocoFontBold = pocoFontFamily
val pocoFontSemiBold = pocoFontFamily
val pocoFontMedium = pocoFontFamily
val pocoFontRegular = pocoFontFamily

private fun pocoTextStyle(
    fontWeight: FontWeight,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = 0.sp
): TextStyle = TextStyle(
    fontFamily = pocoFontFamily,
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
private val displayXl = pocoTextStyle(FontWeight.Bold, 120.sp, 120.sp, (-2.5).sp)

/** display/l · 홈 · 세로 점수 · 주행 OK */
private val displayL = pocoTextStyle(FontWeight.Bold, 96.sp, 100.sp, (-2).sp)

/** heading/xl · 화면 제목 · 이름 · 주행 시작 · 주행 OK */
private val headingXl = pocoTextStyle(FontWeight.Bold, 52.sp, 64.sp, (-1).sp)

/** heading/l · 가운데 배너 제목 · 주행 OK */
private val headingL = pocoTextStyle(FontWeight.Bold, 44.sp, 56.sp, (-1).sp)

/** heading/m · 현댕이 상태 · 리포트 제목 · 주행 OK */
private val headingM = pocoTextStyle(FontWeight.Bold, 36.sp, 48.sp, (-0.5).sp)

/** title/m · 상태 값 · 일반 버튼 · 주행 OK */
private val titleM = pocoTextStyle(FontWeight.Bold, 28.sp, 40.sp, (-0.5).sp)

/** body/l · 세그먼트 · 입력값 · 목록 제목 · 주행 OK */
private val bodyL = pocoTextStyle(FontWeight.Medium, 24.sp, 36.sp)

/** body/m · 카드 라벨 · 설명 · 주행 중엔 라벨만 */
private val bodyM = pocoTextStyle(FontWeight.Normal, 22.sp, 32.sp)

/** caption · 보조 설명 · 축 라벨 · 주행 중엔 라벨만 */
private val captionStyle = pocoTextStyle(FontWeight.Normal, 20.sp, 28.sp)

/** micro · 태그 · 힌트 · 정차 전용 */
private val micro = pocoTextStyle(FontWeight.Bold, 18.sp, 26.sp)

/** nano · 게이지 눈금 · NEW 뱃지 · 주행 중엔 라벨만 */
private val nano = pocoTextStyle(FontWeight.Bold, 16.sp, 22.sp)

// endregion

/**
 * 주행 중 최소 크기: 핵심 정보 32 이상 · 보조 24 이상 · 24 미만은 값 위의 짧은 라벨에만.
 * 한 문장 3줄 이하 · 주행 중 카드 문장은 2줄 이하.
 */
@Immutable
data class POCOTypography(
    val displayXl: TextStyle,
    val displayL: TextStyle,
    val headingXl: TextStyle,
    val headingL: TextStyle,
    val headingM: TextStyle,
    val titleM: TextStyle,
    val bodyL: TextStyle,
    val bodyM: TextStyle,
    val caption: TextStyle,
    val micro: TextStyle,
    val nano: TextStyle,
    // 예전 이름 (팀 코드 호환용) → 가장 가까운 DS-02 토큰
    val heading1: TextStyle = headingM,
    val heading2: TextStyle = titleM,
    val title1: TextStyle = bodyL,
    val body1: TextStyle = bodyL,
    val body2: TextStyle = bodyM,
    val body3: TextStyle = captionStyle,
    val caption1: TextStyle = micro,
    val score: TextStyle = displayL,
    val hmiHeading1: TextStyle = headingXl,
    val hmiTitle: TextStyle = titleM,
    val hmiButton: TextStyle = titleM,
    val hmiBody: TextStyle = bodyL,
    val hmiLabel: TextStyle = bodyL,
    val hmiCaption: TextStyle = captionStyle
)

val defaultPOCOTypography = POCOTypography(
    displayXl = displayXl,
    displayL = displayL,
    headingXl = headingXl,
    headingL = headingL,
    headingM = headingM,
    titleM = titleM,
    bodyL = bodyL,
    bodyM = bodyM,
    caption = captionStyle,
    micro = micro,
    nano = nano
)

val LocalPOCOTypographyProvider = staticCompositionLocalOf { defaultPOCOTypography }
