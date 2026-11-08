package com.example.poco.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.poco.R

val pocoFontBold = FontFamily(Font(R.font.pretendard_bold, FontWeight.Bold))
val pocoFontSemiBold = FontFamily(Font(R.font.pretendard_semibold, FontWeight.SemiBold))
val pocoFontMedium = FontFamily(Font(R.font.pretendard_medium, FontWeight.Medium))
val pocoFontRegular = FontFamily(Font(R.font.pretendard_regular, FontWeight.Normal))

private fun pocoTextStyle(
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = 0.sp
): TextStyle = TextStyle(
    fontFamily = fontFamily,
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

@Immutable
data class POCOTypography(
    val heading1: TextStyle,
    val heading2: TextStyle,
    val title1: TextStyle,
    val body1: TextStyle,
    val body2: TextStyle,
    val body3: TextStyle,
    val caption1: TextStyle,
    val score: TextStyle,
    val hmiHeading1: TextStyle,
    val hmiTitle: TextStyle,
    val hmiButton: TextStyle,
    val hmiBody: TextStyle,
    val hmiLabel: TextStyle,
    val hmiCaption: TextStyle
)

val defaultPOCOTypography = POCOTypography(
    heading1 = pocoTextStyle(
        fontFamily = pocoFontBold,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = -0.5.sp
    ),
    heading2 = pocoTextStyle(
        fontFamily = pocoFontSemiBold,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.0.sp
    ),
    title1 = pocoTextStyle(
        fontFamily = pocoFontSemiBold,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.0.sp
    ),
    body1 = pocoTextStyle(
        fontFamily = pocoFontMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.0.sp
    ),
    body2 = pocoTextStyle(
        fontFamily = pocoFontMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.0.sp
    ),
    body3 = pocoTextStyle(
        fontFamily = pocoFontRegular,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.0.sp
    ),
    caption1 = pocoTextStyle(
        fontFamily = pocoFontMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.0.sp
    ),
    score = pocoTextStyle(
        fontFamily = pocoFontBold,
        fontWeight = FontWeight.Bold,
        fontSize = 96.sp,
        lineHeight = 104.sp,
        letterSpacing = -2.0.sp
    ),
    hmiHeading1 = pocoTextStyle(
        fontFamily = pocoFontBold,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 72.sp,
        letterSpacing = -1.0.sp
    ),
    hmiTitle = pocoTextStyle(
        fontFamily = pocoFontBold,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 44.sp,
        letterSpacing = -0.5.sp
    ),
    hmiButton = pocoTextStyle(
        fontFamily = pocoFontBold,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.0.sp
    ),
    hmiBody = pocoTextStyle(
        fontFamily = pocoFontRegular,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.0.sp
    ),
    hmiLabel = pocoTextStyle(
        fontFamily = pocoFontMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.0.sp
    ),
    hmiCaption = pocoTextStyle(
        fontFamily = pocoFontRegular,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.0.sp
    )
)

val LocalPOCOTypographyProvider = staticCompositionLocalOf { defaultPOCOTypography }
