package com.example.poco.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.PetPhoto
import com.example.poco.core.designsystem.ScoreRing
import com.example.poco.core.designsystem.component.PocoToggle
import com.example.poco.core.designsystem.component.pressable
import com.example.poco.core.designsystem.hasFullPetImage
import com.example.poco.core.designsystem.previewTrip
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.util.toMonthDay
import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.PetTrait
import com.example.poco.domain.driving.PetType
import com.example.poco.domain.driving.temperatureStandard
import com.example.poco.presentation.common.MungoNavRail
import com.example.poco.presentation.common.RailTab
import kotlin.math.roundToInt

/*
 * 홈 (mungo 시안 · 1700×1184)
 * [메뉴 레일 | 펫 · 차량 상태 패널 | 드라이브 카드 + 컴포트 점수 · 이번 주 이동 · 펫모드]
 * - 펫모드 = 뒷좌석 온도 지켜보기 설정 (켜면 주행 중 펫 자리 온도를 알려 준다)
 * - 아직 차량 정보가 없는 칸(공기 청정 · 날씨 · 차종)은 '—' 또는 생략
 */

@Composable
fun HomeRoute(
    navigateToPetProfile: () -> Unit,
    navigateToBoarding: () -> Unit,
    navigateToDriving: () -> Unit,
    navigateToPetMode: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 기어를 D로 바꾸거나 내비 길 안내를 시작하면 자동으로 주행 시작
    LaunchedEffect(uiState.shouldStartDriving) {
        if (uiState.shouldStartDriving) {
            viewModel.onDrivingStarted()
            navigateToDriving()
        }
    }

    HomeScreen(
        uiState = uiState,
        onOpenPet = navigateToPetProfile,
        onStartDrive = navigateToBoarding,
        onPetModeChange = viewModel::setRearTempWatchEnabled,
        onOpenPetMode = navigateToPetMode
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onOpenPet: () -> Unit,
    onStartDrive: () -> Unit,
    onPetModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onOpenPetMode: () -> Unit = {}
) {
    val colors = POCOTheme.colors
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .safeDrawingPadding()
    ) {
        MungoNavRail(selected = RailTab.HOME, onHome = {}, onMyPet = onOpenPet)
        VerticalLine()
        PetPanel(
            uiState = uiState,
            onOpenPet = onOpenPet,
            modifier = Modifier
                .width(PET_PANEL_WIDTH)
                .fillMaxHeight()
        )
        VerticalLine()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DriveCard(
                pet = uiState.pet,
                onStartDrive = onStartDrive,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.4f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ComfortCard(score = uiState.lastTrip?.comfortScore, modifier = cell())
                WeekTripCard(counts = uiState.weekTripCounts, modifier = cell())
                if (SHOW_PET_MODE) {
                    PetModeCard(
                        temperature = uiState.seatTemperature,
                        enabled = uiState.isRearTempWatchEnabled,
                        onChange = onPetModeChange,
                        onOpen = onOpenPetMode,
                        modifier = cell()
                    )
                }
            }
        }
    }
}

/**
 * 펫모드(보호자 외출 중 화면)는 스마트폰 연동(원격 확인 · 알림)이 없어 공모전 제출본에서는 숨긴다.
 * 화면 · 경로 코드는 남겨 두었으니 다시 보이게 하려면 true로 바꾼다
 */
private const val SHOW_PET_MODE = false

private fun RowScope.cell(): Modifier = Modifier.weight(1f).fillMaxHeight()

// region 펫 · 차량 상태 패널

@Composable
private fun PetPanel(uiState: HomeUiState, onOpenPet: () -> Unit, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    val typography = POCOTheme.typography
    val pet = uiState.pet
    val today = remember { System.currentTimeMillis().toMonthDay() }
    Column(
        modifier = modifier
            .background(colors.bgSurface)
            .padding(horizontal = 32.dp, vertical = 32.dp)
    ) {
        Text(text = today, style = typography.body2, color = colors.textTertiary)
        Spacer(Modifier.height(24.dp))
        // 펫 사진 · 이름 (누르면 마이펫)
        Column(
            modifier = Modifier.pressable(RoundedCornerShape(MungoRadius.md), onClick = onOpenPet)
        ) {
            Box(
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .background(colors.bgSurfaceSunken, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PetPhoto(
                    photoPath = pet?.photoPath,
                    breed = pet?.breed,
                    modifier = Modifier.size(
                        if (hasFullPetImage(pet?.photoPath, pet?.breed)) AVATAR_SIZE else 96.dp
                    )
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = pet?.name ?: "펫을 추가해 주세요",
                    style = if (pet != null) typography.h3 else typography.title1,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                pet?.detailLine()?.takeIf { it.isNotEmpty() }?.let { detail ->
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = detail,
                        style = typography.body2,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
        pet?.tags()?.takeIf { it.isNotEmpty() }?.let { tags ->
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                tags.forEach { PetTag(it) }
            }
        }
        Spacer(Modifier.height(32.dp))
        HorizontalDivider(color = colors.borderDefault)
        Spacer(Modifier.height(32.dp))

        // 차량 상태
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (uiState.isParked) {
                    true -> "차량 정차 중"
                    false -> "주행 중"
                    null -> "차량 확인 중"
                },
                style = typography.title1,
                color = colors.textPrimary
            )
            if (uiState.isParked != null) {
                Spacer(Modifier.width(12.dp))
                GearBadge(if (uiState.isParked) "P" else "D")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = "Pleos Connect", style = typography.body3, color = colors.textTertiary)
        Spacer(Modifier.height(32.dp))

        // 2 × 2 상태 칸
        val petModeOn = uiState.isRearTempWatchEnabled
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatusTile(
                    icon = MungoIcon.THERMOMETER,
                    label = "실내 온도",
                    value = uiState.seatTemperature?.let { "${it.roundToInt()}°C" } ?: "—"
                )
                StatusTile(
                    icon = MungoIcon.LOCK,
                    label = "창문",
                    value = when (uiState.isAllWindowsClosed) {
                        true -> "닫힘"
                        false -> "열림"
                        null -> "—"
                    },
                    highlighted = uiState.isAllWindowsClosed == true
                )
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (SHOW_PET_MODE) {
                    StatusTile(
                        icon = MungoIcon.PAW,
                        label = "펫모드",
                        value = if (petModeOn) "켜짐" else "대기",
                        highlighted = petModeOn
                    )
                } else {
                    StatusTile(
                        icon = MungoIcon.MIC,
                        label = "음성 안내",
                        value = if (uiState.isVoiceGuideEnabled) "켜짐" else "꺼짐",
                        highlighted = uiState.isVoiceGuideEnabled
                    )
                }
                // 공기 청정은 아직 Pleos SDK에서 읽지 않는다
                StatusTile(icon = MungoIcon.FAN, label = "공기 청정", value = "—")
            }
        }
    }
}

@Composable
private fun PetTag(text: String) {
    val colors = POCOTheme.colors
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .background(colors.bgAccentSubtle, RoundedCornerShape(MungoRadius.sm))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = POCOTheme.typography.body3, color = colors.textAccent)
    }
}

@Composable
private fun GearBadge(gear: String) {
    val colors = POCOTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(colors.bgAccent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = gear,
            style = POCOTheme.typography.title2.copy(fontWeight = FontWeight.Bold),
            color = colors.textOnAccent
        )
    }
}

@Composable
private fun RowScope.StatusTile(
    icon: MungoIcon,
    label: String,
    value: String,
    highlighted: Boolean = false
) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(MungoRadius.lg)
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(if (highlighted) colors.bgAccentSubtle else colors.bgSurfaceSunken, shape)
            .then(
                if (highlighted) {
                    Modifier.border(
                        2.dp,
                        colors.borderAccent.copy(alpha = 0.4f),
                        shape
                    )
                } else {
                    Modifier
                }
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.bgSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MungoIconView(
                    icon,
                    tint = if (highlighted) colors.textAccent else colors.textPrimary,
                    size = 22.dp
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(text = label, style = POCOTheme.typography.body2, color = colors.textSecondary)
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = POCOTheme.typography.title1,
            color = if (highlighted) colors.textAccent else colors.textPrimary,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

// endregion

// region 드라이브 카드

@Composable
private fun DriveCard(pet: Pet?, onStartDrive: () -> Unit, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    val typography = POCOTheme.typography
    val name = pet?.name ?: "우리 아이"
    HomeCard(modifier = modifier, padding = 48.dp) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // 상태 칩
                Row(
                    modifier = Modifier
                        .background(colors.bgSurfaceSunken, CircleShape)
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MungoIconView(MungoIcon.CAR, size = 24.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(text = "탑승 준비 전", style = typography.title3, color = colors.textPrimary)
                }
                Spacer(Modifier.height(36.dp))
                Text(
                    text = "$name,\n드라이브 갈까요?",
                    style = typography.h1,
                    color = colors.textPrimary
                )
                Spacer(Modifier.weight(1f))
                // 가장 큰 터치 영역: 탑승 준비 시작 → 탑승 준비 (기어 D · 내비 길 안내면 바로 주행)
                val shape = CircleShape
                Row(
                    modifier = Modifier
                        .background(colors.bgAccent, shape)
                        .pressable(shape = shape, onClick = onStartDrive)
                        .heightIn(min = 104.dp)
                        .padding(horizontal = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "탑승 준비 시작",
                        style = typography.title1,
                        color = colors.textOnAccent
                    )
                    Spacer(Modifier.width(20.dp))
                    MungoIconView(MungoIcon.PLAY_FILLED, tint = colors.textOnAccent, size = 28.dp)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(DRIVE_ILLUSTRATION),
                contentAlignment = Alignment.Center
            ) {
                PetPhoto(
                    photoPath = pet?.photoPath,
                    breed = pet?.breed,
                    modifier = Modifier.size(DRIVE_ILLUSTRATION)
                )
            }
        }
    }
}

// endregion

// region 아래 카드 3개

@Composable
private fun ComfortCard(score: Int?, modifier: Modifier = Modifier) {
    HomeCard(modifier = modifier) {
        CardHeader(icon = MungoIcon.MAP, title = "컴포트 점수")
        Spacer(Modifier.height(16.dp))
        BigValue(value = score?.toString() ?: "0", unit = "점")
        Spacer(Modifier.weight(1f))
        if (score != null) {
            ScoreRing(score = score, modifier = Modifier.size(RING_SIZE), strokeWidth = 16.dp)
        } else {
            EmptyRing()
        }
    }
}

@Composable
private fun WeekTripCard(counts: List<Int>, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    HomeCard(modifier = modifier) {
        CardHeader(icon = MungoIcon.CALENDAR, title = "이번 주 이동")
        Spacer(Modifier.height(16.dp))
        BigValue(value = counts.sum().toString(), unit = "회")
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WEEK_DAYS.forEachIndexed { index, day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(width = 28.dp, height = 12.dp)
                            .background(
                                if (counts.getOrElse(index) { 0 } >
                                    0
                                ) {
                                    colors.bgAccent
                                } else {
                                    colors.borderDefault
                                },
                                CircleShape
                            )
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = day,
                        style = POCOTheme.typography.caption1,
                        color = colors.textTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun PetModeCard(
    temperature: Float?,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 카드를 누르면 펫모드 화면 (보호자 외출), 토글은 주행 중 뒷좌석 온도 지켜보기
    HomeCard(
        modifier = modifier
            .clip(RoundedCornerShape(MungoRadius.xl))
            .clickable(onClick = onOpen)
    ) {
        CardHeader(icon = MungoIcon.THERMOMETER, title = "펫모드")
        Spacer(Modifier.height(16.dp))
        BigValue(value = temperature?.let { "${it.roundToInt()}°C" } ?: "—", unit = null)
        Spacer(Modifier.weight(1f))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            PocoToggle(checked = enabled, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun CardHeader(icon: MungoIcon, title: String) {
    val colors = POCOTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(colors.bgSurfaceSunken, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(icon, size = 24.dp)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = POCOTheme.typography.title3,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        MungoIconView(MungoIcon.CHEVRON_RIGHT, tint = colors.textTertiary, size = 28.dp)
    }
}

@Composable
private fun BigValue(value: String, unit: String?) {
    val colors = POCOTheme.colors
    Row(verticalAlignment = Alignment.Bottom) {
        Text(text = value, style = POCOTheme.typography.h1, color = colors.textPrimary)
        unit?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = it,
                style = POCOTheme.typography.title2,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
    }
}

/** 아직 이동 기록이 없을 때: 빈 링 + '–' */
@Composable
private fun EmptyRing(size: Dp = RING_SIZE) {
    val colors = POCOTheme.colors
    val track = colors.borderDefault
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(this.size.width - stroke, this.size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(text = "–", style = POCOTheme.typography.title1, color = colors.textTertiary)
    }
}

// endregion

// region 공통

@Composable
private fun HomeCard(
    modifier: Modifier = Modifier,
    padding: Dp = 32.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(POCOTheme.colors.bgSurface, RoundedCornerShape(MungoRadius.xl))
            .padding(padding),
        content = content
    )
}

@Composable
private fun VerticalLine() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(POCOTheme.colors.borderDefault)
    )
}

/** "시츄 | 10살 | 7kg" */
private fun Pet.detailLine(): String = listOfNotNull(
    breed.takeIf { it.isNotBlank() },
    ageYears?.let { "${it}살" },
    weightKg?.let { "${it.toKgLabel()}kg" }
).joinToString(" | ")

/** 7.0 → "7", 4.2 → "4.2" */
private fun Float.toKgLabel(): String =
    if (this % 1f == 0f) roundToInt().toString() else "%.1f".format(this)

/** 이름 아래 꼬리표 (최대 2개): 나이 · 더위/추위 위험 · 멀미 */
private fun Pet.tags(): List<String> = buildList {
    val age = ageYears
    if (age != null && age >= SENIOR_AGE) add("노령견")
    if (age != null && age < 1) add("어린 강아지")
    val types = temperatureStandard.types
    val heatRisk = PetType.BRACHYCEPHALIC in types ||
        PetType.DOUBLE_COAT in types ||
        PetTrait.HEAT_SENSITIVE in traits
    if (heatRisk) add("더위 고위험")
    if (PetTrait.COLD_SENSITIVE in traits) add("추위 주의")
    if (PetTrait.MOTION_SICK in traits) add("멀미 주의")
}.take(MAX_TAGS)

private val PET_PANEL_WIDTH = 460.dp
private val AVATAR_SIZE = 132.dp
private val DRIVE_ILLUSTRATION = 380.dp
private val RING_SIZE = 136.dp
private val WEEK_DAYS = listOf("월", "화", "수", "목", "금", "토", "일")
private const val SENIOR_AGE = 8
private const val MAX_TAGS = 2

// endregion

private fun previewState() = HomeUiState(
    pet = Pet(
        id = 1,
        name = "현댕이",
        breed = "시츄",
        ageYears = 10,
        weightKg = 7f,
        seat = PetSeat.REAR_LEFT,
        motionSensitivity = MotionSensitivity.NORMAL
    ),
    isParked = true,
    seatTemperature = 24f,
    isAllWindowsClosed = true,
    isRearTempWatchEnabled = true
)

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun HomeScreenPreview() {
    POCOTheme(darkTheme = false) { HomeScreen(previewState(), {}, {}, {}) }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "다크 모드")
@Composable
private fun HomeScreenDarkPreview() {
    POCOTheme(darkTheme = true) {
        HomeScreen(previewState().copy(lastTrip = previewTrip()), {}, {}, {})
    }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "첫 실행 (빈 상태)")
@Composable
private fun HomeScreenEmptyPreview() {
    POCOTheme(darkTheme = false) { HomeScreen(HomeUiState(), {}, {}, {}) }
}
