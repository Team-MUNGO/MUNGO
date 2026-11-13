package com.example.poco.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poco.R
import com.example.poco.core.theme.POCOTheme

/*
 * mungo 아이콘 · 로고
 * - 아이콘: Lucide(ISC 라이선스) 24px 원본 경로를 그대로 옮김. 선 2 · 끝 둥글게
 * - 로고: res/drawable-nodpi/mungo_logo_light · dark (디자이너 원본에서 잘라 글자색만 바꿈)
 */

private val PAW_PATHS = listOf(
    "M9 4a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
    "M16 8a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
    "M18 16a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
    "M9 10a5 5 0 0 1 5 5v3.5a3.5 3.5 0 0 1-6.84 1.045Q6.52 17.48 4.46 16.84A3.5 3.5 0 0 1 5.5 10Z"
)

enum class MungoIcon(internal val paths: List<String>, internal val filled: Boolean = false) {
    PAW(PAW_PATHS),

    /** 반려동물 아바타용 꽉 찬 발바닥 */
    PAW_FILLED(PAW_PATHS, filled = true),

    /** 부드러운 운전 (물결) */
    WAVE(listOf("M2 12c1.5-3.5 3.5-5 5-5c3 0 4 10 7 10c1.5 0 3.5-1.5 5-5")),
    THERMOMETER(listOf("M14 4v10.54a4 4 0 1 1-4 0V4a2 2 0 0 1 4 0Z")),
    HOUSE(
        listOf(
            "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8",
            "M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"
        )
    ),
    MIC(
        listOf(
            "M12 19v3",
            "M19 10v2a7 7 0 0 1-14 0v-2",
            "M9 5a3 3 0 0 1 6 0v7a3 3 0 0 1-6 0Z"
        )
    ),
    ARROW_LEFT(listOf("m12 19-7-7 7-7", "M19 12H5")),
    CHECK(listOf("M20 6 9 17l-5-5")),
    CLOSE(listOf("M18 6 6 18", "m6 6 12 12")),
    CHEVRON_RIGHT(listOf("m9 18 6-6-6-6")),
    MAP_PIN(
        listOf(
            "M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0",
            "M9 10a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"
        )
    ),
    CLOCK(listOf("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M12 6v6l4 2")),

    CHEVRON_DOWN(listOf("m6 9 6 6 6-6")),
    SEARCH(listOf("m21 21-4.34-4.34", "M3 11a8 8 0 1 0 16 0a8 8 0 1 0 -16 0")),
    MINUS(listOf("M5 12h14")),
    PLUS(listOf("M5 12h14", "M12 5v14")),
    INFO(listOf("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M12 16v-4", "M12 8h.01")),

    /** 멀미 (소용돌이) */
    SPIRAL(
        listOf(
            "M12 12a1.5 1.5 0 0 1 3 0a3 3 0 0 1-6 0a4.5 4.5 0 0 1 9 0a6 6 0 0 1-12 0a7.5 7.5 0 0 1 15 0"
        )
    ),

    /** 불안 (맥박) */
    ACTIVITY(
        listOf(
            "M22 12h-2.48a2 2 0 0 0-1.93 1.46l-2.35 8.36a.25.25 0 0 1-.48 0L9.24 2.18a.25.25 0 0 0-.48 0l-2.35 8.36A2 2 0 0 1 4.49 12H2"
        )
    ),
    SUN(
        listOf(
            "M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0",
            "M12 2v2",
            "M12 20v2",
            "m4.93 4.93 1.41 1.41",
            "m17.66 17.66 1.41 1.41",
            "M2 12h2",
            "M20 12h2",
            "m6.34 17.66-1.41 1.41",
            "m19.07 4.93-1.41 1.41"
        )
    ),
    SNOWFLAKE(
        listOf(
            "M12 2v20",
            "m3.34 7 17.32 10",
            "m20.66 7-17.32 10",
            "m9 4 3 2 3-2",
            "m9 20 3-2 3 2"
        )
    ),
    DROPLET(
        listOf(
            "M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z"
        )
    ),
    HEART(
        listOf(
            "M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"
        )
    ),
    SPEAKER(
        listOf(
            "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z",
            "M16 9a5 5 0 0 1 0 6",
            "M19.364 18.364a9 9 0 0 0 0-12.728"
        )
    ),

    /** 창문 (2×2 격자) */
    WINDOW(
        listOf(
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
            "M12 3v18",
            "M3 12h18"
        )
    ),

    /** 휴식 쉼터 (컵) */
    COFFEE(
        listOf(
            "M10 2v2",
            "M14 2v2",
            "M6 2v2",
            "M16 8a1 1 0 0 1 1 1v8a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1h14a4 4 0 1 1 0 8h-1"
        )
    ),
    CAMERA(
        listOf(
            "M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z",
            "M9 13a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"
        )
    ),

    /** 휴게소 (피크닉 테이블) */
    REST_AREA(listOf("M4 7h16", "M3 12h18", "M7 7l-3 12", "M17 7l3 12", "M9 12v7", "M15 12v7")),

    CALENDAR(
        listOf(
            "M8 2v4",
            "M16 2v4",
            "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z",
            "M3 10h18"
        )
    ),
    SETTINGS(
        listOf(
            "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08" +
                "a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51" +
                "a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08" +
                "a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18" +
                "a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39" +
                "a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09" +
                "a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25" +
                "a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2Z",
            "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"
        )
    ),
    LOCK(
        listOf(
            "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2Z",
            "M7 11V7a5 5 0 0 1 10 0v4"
        )
    ),

    /** 공기 청정 (선풍기 날개) */
    FAN(
        listOf(
            "M10.827 16.379a6.082 6.082 0 0 1-8.618-7.002l5.412 1.45a6.082 6.082 0 0 1 " +
                "7.002-8.618l-1.45 5.412a6.082 6.082 0 0 1 8.618 7.002l-5.412-1.45" +
                "a6.082 6.082 0 0 1-7.002 8.618l1.45-5.412Z",
            "M12 12v.01"
        )
    ),
    MAP(
        listOf(
            "M14.106 5.553a2 2 0 0 0 1.788 0l3.659-1.83A1 1 0 0 1 21 4.619v12.764" +
                "a1 1 0 0 1-.553.894l-4.553 2.277a2 2 0 0 1-1.788 0l-4.212-2.106" +
                "a2 2 0 0 0-1.788 0l-3.659 1.83A1 1 0 0 1 3 19.381V6.618a1 1 0 0 1 .553-.894" +
                "l4.553-2.277a2 2 0 0 1 1.788 0Z",
            "M15 5.764v15",
            "M9 3.236v15"
        )
    ),
    CAR(
        listOf(
            "M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10" +
                "s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.4 2.9" +
                "A3.7 3.7 0 0 0 2 12v4c0 .6.4 1 1 1h2",
            "M5 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M9 17h6",
            "M15 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"
        )
    ),

    /** 좌석 (등받이 + 방석) */
    SEAT(
        listOf(
            "M8 3h8a3 3 0 0 1 3 3v6a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3Z",
            "M6 17h12a2 2 0 0 1 0 4H6a2 2 0 0 1 0-4Z"
        )
    ),

    /** 운전대 */
    STEERING_WHEEL(
        listOf(
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0",
            "M10 11a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
            "M12 13v9",
            "M10.1 10.4 2.3 8.6",
            "M13.9 10.4l7.8-1.8"
        )
    ),

    /** 꽉 찬 재생 삼각형 (버튼 안 화살표) */
    PLAY_FILLED(
        listOf("M7 4.5v15a1 1 0 0 0 1.5.86l12-7.5a1 1 0 0 0 0-1.72l-12-7.5A1 1 0 0 0 7 4.5Z"),
        filled = true
    ),

    /** 방패 + 체크 (평온 · 안전) */
    SHIELD_CHECK(
        listOf(
            "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1" +
                "c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
            "m9 12 2 2 4-4"
        )
    ),

    /** 연필 (수정) */
    PENCIL(
        listOf(
            "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352" +
                "a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z",
            "m15 5 4 4"
        )
    ),

    /** 배터리 */
    BATTERY(
        listOf(
            "M4 6h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z",
            "M22 14v-4",
            "M6 10v4",
            "M10 10v4"
        )
    ),

    /** 꼬리표 (휴식 추천 이유) */
    TAG(
        listOf(
            "M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414" +
                "l8.704 8.704a2.426 2.426 0 0 0 3.42 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42z",
            "M7 7.5a.5.5 0 1 0 1 0a.5.5 0 1 0 -1 0"
        )
    )
}

private val iconCache = HashMap<MungoIcon, ImageVector>()

val MungoIcon.imageVector: ImageVector
    get() = iconCache.getOrPut(this) {
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            paths.forEach { data ->
                addPath(
                    pathData = addPathNodes(data),
                    fill = if (filled) SolidColor(Color.Black) else null,
                    stroke = if (filled) null else SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()
    }

@Composable
fun MungoIconView(
    icon: MungoIcon,
    modifier: Modifier = Modifier,
    tint: Color = POCOTheme.colors.textPrimary,
    size: Dp = 24.dp,
    contentDescription: String? = null
) {
    Icon(
        imageVector = icon.imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}

// region Logo

/**
 * 가로형 로고: 망고 발바닥 + mungo 글자 (원본: design/brand/mungo_logo_sheet@4x.png 맨 아래 가로형).
 * 라이트 = 검정 글자, 다크 = 흰 글자. 원본 비율 1192:284
 * @param height 로고 높이 (스플래시 140, 화면 안 작은 로고 44)
 */
@Composable
fun MungoLogo(modifier: Modifier = Modifier, height: Dp = 40.dp) {
    Image(
        painter = painterResource(
            if (POCOTheme.colors.isDark) R.drawable.mungo_logo_dark else R.drawable.mungo_logo_light
        ),
        contentDescription = "mungo",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(height)
            .aspectRatio(LOGO_RATIO)
    )
}

private const val LOGO_RATIO = 1192f / 284f

/** 로고의 망고 발바닥만 (정사각형). 메뉴 레일 맨 위 같은 작은 자리 */
@Composable
fun MungoPaw(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.mungo_paw),
        contentDescription = "mungo",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

// endregion
