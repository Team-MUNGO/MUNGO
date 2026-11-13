package com.example.poco.presentation.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.R
import com.example.poco.core.theme.pocoFontFamily

/**
 * Design tokens sampled from Figma "Danzle_프론트 참고" Page 3 (464:45661). All sizes below are in
 * Figma frame pixels; [DesignCanvas] maps one Figma pixel to one dp at the reference size.
 */
val LocalDesignFont = staticCompositionLocalOf { pocoFontFamily }

object Mg {
    val Bg = Color(0xFFF4F5F7)
    val Card = Color.White
    val Ink = Color(0xFF191F28)
    val Muted = Color(0xFF6B7684)
    val Faint = Color(0xFF8B95A1)
    val Orange = Color(0xFFFF8200)
    val OrangeText = Color(0xFFB35400)
    val Peach = Color(0xFFFFF7EE)
    /** Figma 488:10409 · 10759 · 11013: 전체 화면 카드 맨 위 상태 칩 (주행중 · 휴식중 · 도착) */
    val StatusChip = Color(0xFFFFF9E7)
    val IconBg = Color(0xFFFFF1E2)
    val WarnBorder = Color(0xFFFF9F3C)

    /** 값이 기준을 벗어난 상태 타일 (Figma 488:12312 실내 온도 경고): 연한 망고 면 + 망고 3px 테두리 */
    /** 주행 컴포트 말풍선 (Figma 488:10411) */
    val Bubble = Color(0xFF1E3351)
    val AlertBg = Color(0xFFFFF1E2)
    val AlertStroke = Color(0xFFFF8200)
    val AlertStrokeWidth = 3.dp
    val Chip = Color(0xFFF2F4F6)
    val Segment = Color(0xFFE8EBEE)
    val Yellow = Color(0xFFFFF9E7)
    val BarGrey = Color(0xFFC9CED4)
    val Track = Color(0xFFE8EAED)
    val Line = Color(0xFFE5E8EB)
    val Scrim = Color(0x8C191F28)
    val DarkPill = Color(0xFF2B323C)
    val OnDarkMuted = Color(0xFFC9CED4)

    val Radius = 24.dp

    @Composable
    private fun style(weight: FontWeight, size: Int, line: Int) = TextStyle(
        fontFamily = LocalDesignFont.current,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
    )

    @Composable
    fun bold(size: Int, line: Int = (size * 1.3f).toInt()) = style(FontWeight.Bold, size, line)
    @Composable
    fun semi(size: Int, line: Int = (size * 1.32f).toInt()) = style(FontWeight.SemiBold, size, line)
    @Composable
    fun medium(size: Int, line: Int = (size * 1.32f).toInt()) = style(FontWeight.Medium, size, line)

    @Composable
    fun regular(size: Int, line: Int = (size * 1.3f).toInt()) = style(FontWeight.Normal, size, line)

    /** Page 3 driving text boxes. */
    val Body: TextStyle @Composable get() = regular(24, 32)
    val BodyStrong: TextStyle @Composable get() = bold(24, 32)
    val Caption: TextStyle @Composable get() = regular(20, 27)
    val Title: TextStyle @Composable get() = bold(34, 44)
    val Quote: TextStyle @Composable get() = semi(35, 46)
    val Value: TextStyle @Composable get() = bold(58, 73)
    val ValueSplit: TextStyle @Composable get() = bold(44, 55)
    val Unit: TextStyle @Composable get() = regular(24, 32)
}

/**
 * Renders [content] on a fixed design canvas: one Figma pixel = one dp at [refWidth] x [refHeight],
 * scaled uniformly to the actual window. Full and split layouts use their own references.
 */
@Composable
fun DesignCanvas(refWidth: Float, refHeight: Float, modifier: Modifier = Modifier, fontFamily: FontFamily = pocoFontFamily, content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize().background(Mg.Bg)) {
        val density = LocalDensity.current
        val scale = minOf(maxWidth.value / refWidth, maxHeight.value / refHeight).coerceIn(0.35f, 2f)
        CompositionLocalProvider(
            LocalDensity provides Density(density.density * scale, density.fontScale),
            LocalDesignFont provides fontFamily
        ) {
            Box(Modifier.fillMaxSize(), content = content)
        }
    }
}

@Composable
fun MgCard(
    modifier: Modifier = Modifier,
    color: Color = Mg.Card,
    border: Color? = null,
    radius: Dp = Mg.Radius,
    padding: PaddingValues = PaddingValues(24.dp),
    borderWidth: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier.clip(shape).background(color, shape)
            .then(if (border != null) Modifier.border(borderWidth, border, shape) else Modifier)
            .padding(padding),
        content = content
    )
}

@Composable
fun MgIcon(icon: ImageVector, tint: Color = Mg.OrangeText, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Icon(icon, null, modifier.size(size), tint = tint)
}

@Composable
fun DrawableIcon(res: Int, tint: Color? = null, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Image(painterResource(res), null, modifier.size(size), colorFilter = tint?.let { ColorFilter.tint(it) })
}

/** 48 px circle with a 20 px glyph (tile headers). */
@Composable
fun IconBadge(
    icon: ImageVector? = null,
    res: Int? = null,
    size: Dp = 48.dp,
    background: Color = Mg.IconBg,
    tint: Color = Mg.OrangeText
) {
    Box(Modifier.size(size).background(background, CircleShape), contentAlignment = Alignment.Center) {
        val glyph = size * 0.42f
        if (icon != null) MgIcon(icon, tint, glyph) else if (res != null) DrawableIcon(res, tint, glyph)
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = Mg.Chip,
    color: Color = Mg.Ink,
    style: TextStyle = Mg.BodyStrong,
    border: Color? = null,
    height: Dp = 44.dp,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    horizontalPadding: Dp = 16.dp,
    iconGap: Dp = 8.dp
) {
    Row(
        modifier.height(height).clip(CircleShape).background(background, CircleShape)
            .then(if (border != null) Modifier.border(2.dp, border, CircleShape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(iconGap)
    ) {
        leading?.invoke()
        Text(text, style = style, color = color, maxLines = 1)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    style: TextStyle = Mg.semi(25, 34)
) {
    Button(
        onClick, modifier, enabled = enabled, shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Mg.Orange, contentColor = Color.White,
            disabledContainerColor = Mg.Track, disabledContentColor = Mg.Faint
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        if (icon != null) {
            MgIcon(icon, LocalContentColorOr(enabled), 22.dp)
            Box(Modifier.size(10.dp))
        }
        Text(text, style = style, maxLines = 1)
    }
}

private fun LocalContentColorOr(enabled: Boolean) = if (enabled) Color.White else Mg.Faint

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    style: TextStyle = Mg.semi(22, 29)
) {
    OutlinedButton(
        onClick, modifier, enabled = enabled, shape = CircleShape,
        border = BorderStroke(2.dp, Mg.Line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Mg.Ink),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        if (icon != null) {
            MgIcon(icon, Mg.Ink, 20.dp)
            Box(Modifier.size(8.dp))
        }
        Text(text, style = style, maxLines = 1)
    }
}

/** Comfort ring: grey track and an orange arc of score %, round caps (Figma "컴포트 점수 링"). */
@Composable
fun ScoreRing(
    progress: Float?,
    size: Dp,
    stroke: Dp,
    modifier: Modifier = Modifier,
    description: String = "",
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = description }) {
            val s = stroke.toPx()
            val inset = s / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - s, this.size.height - s)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(Mg.Track, -90f, 360f, false, topLeft, arcSize, style = Stroke(s, cap = StrokeCap.Round))
            if (progress != null && progress > 0f) {
                drawArc(
                    Mg.Orange, -90f, 360f * progress.coerceIn(0f, 1f), false, topLeft, arcSize,
                    style = Stroke(s, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

@Composable
fun PetAvatar(size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.driving_pet), null, modifier.size(size).clip(CircleShape))
}

@Composable
fun ValueText(value: String, unit: String?, valueStyle: TextStyle, color: Color = Mg.Ink, unitColor: Color = color, unitStyle: TextStyle = Mg.Unit) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = valueStyle, color = color, maxLines = 1)
        if (unit != null) {
            Text(unit, Modifier.padding(start = 6.dp, bottom = 8.dp), style = unitStyle, color = unitColor, maxLines = 1)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, style: TextStyle = Mg.semi(24, 33)) {
    Text(text, modifier, style = style, color = Mg.Ink)
}

@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier, style: TextStyle = Mg.Caption) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MgIcon(MgIcons.Info, Mg.Faint, 20.dp)
        Text(text, style = style, color = Mg.Muted)
    }
}

@Composable
fun RowScope.Spacer1() = Box(Modifier.weight(1f))

@Composable
fun CenteredText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) =
    Text(text, modifier, style = style, color = color, textAlign = TextAlign.Center)

/** Line icons drawn on a 24 x 24 grid (2 px stroke, round caps) to match the Figma glyphs. */
object MgIcons {
    private fun line(name: String, vararg paths: String, filled: Set<Int> = emptySet()): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEachIndexed { i, d ->
                addPath(
                    addPathNodes(d),
                    fill = if (i in filled) SolidColor(Color.Black) else null,
                    stroke = if (i in filled) null else SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()

    private const val CIRCLE = "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z"

    val Route = line("route", "M6 15.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z", "M18 3.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z", "M8.5 18H15a3.5 3.5 0 0 0 0-7H9a3.5 3.5 0 0 1 0-7h6.5")
    val Flag = line("flag", "M6 21V4", "M6 4h11l-2.5 4l2.5 4H6")
    val Close = line("close", "M6 6l12 12", "M18 6L6 18")
    val Check = line("check", "M5 12.5l4.5 4.5L19 7.5")
    val Gauge = line("gauge", "M4 17a8 8 0 0 1 16 0", "M12 17l4.5-4.5")
    val Brake = line("brake", CIRCLE, "M12 7.5v5.5", "M12 16.5v0.2")
    val Turn = line("turn", "M7 20v-6a5 5 0 0 1 5-5h7", "M15 5l4 4l-4 4")
    val Doc = line("doc", "M7 3h7l4 4v14H7z", "M14 3v4h4", "M10 12h5", "M10 16h5")
    val Bulb = line("bulb", "M9.5 18h5", "M10.5 21h3", "M12 3a6 6 0 0 0-3.6 10.8c.7.6 1.1 1.3 1.1 2.2h5c0-.9.4-1.6 1.1-2.2A6 6 0 0 0 12 3z")
    val ChevronRight = line("chev-r", "M9 5l7 7l-7 7")
    val ChevronLeft = line("chev-l", "M15 5l-7 7l7 7")
    val Share = line("share", "M12 15V3.5", "M8 7.5l4-4l4 4", "M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-7")
    val Pencil = line("pencil", "M4 20h4L19 9l-4-4L4 16z", "M13.5 6.5l4 4")
    val Home = line("home", "M4 11l8-7l8 7", "M6 9.5V20h12V9.5")
    val Calendar = line("calendar", "M5 5h14v15H5z", "M5 10h14", "M9 3v4", "M15 3v4")
    val Gear = line("gear", "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z", "M12 3v2.5M12 18.5V21M3 12h2.5M18.5 12H21M5.6 5.6l1.8 1.8M16.6 16.6l1.8 1.8M5.6 18.4l1.8-1.8M16.6 7.4l1.8-1.8")
    val Heart = line("heart", "M12 20s-7.5-4.6-7.5-10.2A4.2 4.2 0 0 1 12 7.3a4.2 4.2 0 0 1 7.5 2.5C19.5 15.4 12 20 12 20z")
    val Sliders = line("sliders", "M4 8h9M17 8h3M4 16h3M11 16h9", "M15 6v4", "M9 14v4")
    val Info = line("info", CIRCLE, "M12 11v5.5", "M12 7.8v0.2")
    val Sun = line("sun", "M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8z", "M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4")
    val ArrowRight = line("arrow-r", "M5 12h14", "M13 6l6 6l-6 6")
    val Yawn = line("yawn", CIRCLE, "M8.5 9.5h1.5M14 9.5h1.5", "M12 13a2 2 0 1 0 0 4a2 2 0 1 0 0-4z")
    val Drop = line("drop", "M12 3.5s6 6.4 6 10.8a6 6 0 0 1-12 0c0-4.4 6-10.8 6-10.8z")
    val Pant = line("pant", "M3 12h4.5l2-4l3 8l2-4H21")
    val Shake = line("shake", "M8 4c-2 2.7-2 5.3 0 8s2 5.3 0 8", "M13 4c-2 2.7-2 5.3 0 8s2 5.3 0 8", "M18 4c-2 2.7-2 5.3 0 8s2 5.3 0 8")
    val Vomit = line("vomit", CIRCLE, "M8.5 9.5h1.5M14 9.5h1.5", "M8.5 16c2.3-1.6 4.7-1.6 7 0")
    val Restless = line("restless", "M3 12h4l2-6l4 12l2-6h6")
    val Bump = line("bump", "M3 17h18", "M6 17a6 4.5 0 0 1 12 0")
    val Curve = line("curve", "M7 20c0-5.5 10-4.5 10-10c0-3-2.5-5.5-5-6")
    val Calm = line("calm", "M3 9.5c2-2 4-2 6 0s4 2 6 0s4-2 6 0", "M3 15.5c2-2 4-2 6 0s4 2 6 0s4-2 6 0")
    val Mic = line("mic", "M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z", "M5.5 11.5a6.5 6.5 0 0 0 13 0", "M12 18v3")
    val Speaker = line("speaker", "M4 9.5h3.5L12 5.5v13l-4.5-4H4z", "M16 9a4 4 0 0 1 0 6", "M18.5 6.5a7.5 7.5 0 0 1 0 11")
    val Cup = line("cup", "M5 8h11v5.5a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5z", "M16 9.5h1.5a2.5 2.5 0 0 1 0 5H16", "M8 3.5v2M11.5 3.5v2")
    val Window = line("window", "M4.5 5h15v14h-15z", "M4.5 12h15", "M12 5v14")
    val Clock = line("clock", CIRCLE, "M12 7.5V12l3 2")
    val Thermo = line("thermo", "M10 14.5V5a2 2 0 0 1 4 0v9.5a4 4 0 1 1-4 0z", "M12 10v6")
    val Paw = line("paw", "M12 12.5c-3 0-5.5 3-5.5 5a2.5 2.5 0 0 0 3 2.3c1.6-.4 3.4-.4 5 0a2.5 2.5 0 0 0 3-2.3c0-2-2.5-5-5.5-5z", "M5.5 8.5a1.8 2.2 0 1 0 0.1 0z", "M9.5 4.5a1.8 2.2 0 1 0 0.1 0z", "M14.5 4.5a1.8 2.2 0 1 0 0.1 0z", "M18.5 8.5a1.8 2.2 0 1 0 0.1 0z", filled = setOf(0, 1, 2, 3, 4))
    val Car = line("car", "M4 16.5V12.5l2.2-5A2 2 0 0 1 8 6.3h8a2 2 0 0 1 1.8 1.2l2.2 5v4z", "M4 12.5h16", "M7.5 16.5v2", "M16.5 16.5v2")
    val Bell = line("bell", "M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 1.5h-15z", "M10 20.5h4")
    val Plus = line("plus", "M12 5v14", "M5 12h14")
    val Minus = line("minus", "M5 12h14")
}
