package com.example.poco.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.PetPhoto
import com.example.poco.core.designsystem.ScoreRing
import com.example.poco.core.designsystem.component.pressable
import com.example.poco.core.designsystem.hasFullPetImage
import com.example.poco.core.designsystem.previewTrip
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.MungoTheme
import com.example.poco.core.util.eulReul
import com.example.poco.core.util.toMonthDay
import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.PetTrait
import com.example.poco.domain.driving.PetType
import com.example.poco.domain.driving.temperatureStandard
import com.example.poco.presentation.common.MungoNavRail
import com.example.poco.presentation.common.RailTab
import com.example.poco.presentation.design.Mg
import kotlin.math.roundToInt

/*
 * 홈 (mungo 시안 · 1700×1184)
 * [메뉴 레일 | 펫 · 차량 상태 패널 | 드라이브 카드 + 컴포트 점수 · 이번 주 이동]
 * - 아직 차량 정보가 없는 칸(공기 청정 · 날씨 · 차종)은 '—' 또는 생략
 */

@Composable
fun HomeRoute(
    navigateToPetProfile: () -> Unit,
    navigateToBoarding: () -> Unit,
    navigateToDriving: () -> Unit,
    navigateToWeeklyRecords: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenVisible(true)
        onPauseOrDispose { viewModel.onScreenVisible(false) }
    }

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
        onRelink = viewModel::relinkVehicle,
        onOpenWeekTrips = navigateToWeeklyRecords
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onOpenPet: () -> Unit,
    onStartDrive: () -> Unit,
    modifier: Modifier = Modifier,
    onRelink: () -> Unit = {},
    onOpenWeekTrips: () -> Unit = {}
) {
    val colors = MungoTheme.colors
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
                .padding(start = 32.dp, end = 37.dp, top = 39.dp, bottom = 39.dp),
            verticalArrangement = Arrangement.spacedBy(23.dp)
        ) {
            DriveCard(
                pet = uiState.pet,
                onStartDrive = onStartDrive,
                linked = uiState.isVehicleLinked,
                onRelink = onRelink,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(HERO_WEIGHT)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(23.dp)
            ) {
                // 연동이 꺼져도 배치는 같고, 카드 면 · 값 · 그래프만 회색으로 보여 준다
                ComfortCard(
                    score = uiState.lastTrip?.comfortScore,
                    linked = uiState.isVehicleLinked,
                    modifier = cell()
                )
                WeekTripCard(
                    counts = uiState.weekTripCounts,
                    linked = uiState.isVehicleLinked,
                    modifier = Modifier.weight(2f).fillMaxHeight(),
                    onClick = onOpenWeekTrips
                )
            }
        }
    }
}

private fun RowScope.cell(): Modifier = Modifier.weight(1f).fillMaxHeight()

// region 펫 · 차량 상태 패널

/** 연동 중 · 해지 모두 같은 배치. 해지하면 차량 값은 '—', 칩 · 칸은 회색 */
@Composable
private fun PetPanel(uiState: HomeUiState, onOpenPet: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MungoTheme.colors
    val typography = MungoTheme.typography
    val pet = uiState.pet
    val linked = uiState.isVehicleLinked
    val today = remember { System.currentTimeMillis().toMonthDay() }
    Column(
        modifier = modifier
            .background(colors.bgSurface)
            .padding(start = 32.dp, end = 32.dp, top = 44.dp, bottom = 46.dp)
    ) {
        // Figma 470:55182 정보 패널: 날짜 47 · 사진 124 · 이름 292 · 구분선 469 · 상태 칸 670
        Text(text = today, style = Mg.regular(22, 28), color = colors.textTertiary)
        Spacer(Modifier.height(48.dp))
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
            Spacer(Modifier.height(36.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = pet?.name ?: "펫을 추가해 주세요",
                    style = if (pet != null) Mg.bold(45, 55) else typography.title1,
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
                tags.forEach { PetTag(it, linked = linked) }
            }
        }
        Spacer(Modifier.height(60.dp))
        HorizontalDivider(color = colors.borderDefault)
        Spacer(Modifier.height(58.dp))

        // 차량 상태
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    !linked -> "연결 안 됨"
                    uiState.isParked == true -> "차량 정차 중"
                    uiState.isParked == false -> "주행 중"
                    else -> "차량 확인 중"
                },
                style = Mg.bold(37, 46),
                color = if (linked) colors.textPrimary else colors.textTertiary
            )
            if (linked && uiState.isParked != null) {
                Spacer(Modifier.width(12.dp))
                GearBadge(if (uiState.isParked) "P" else "D")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = listOfNotNull(
                uiState.vehicleName,
                if (linked) "mungo 연동 중" else "연동이 꺼져 있어요"
            ).joinToString(" | "),
            style = Mg.regular(24, 31),
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(54.dp))

        // 상태 칸: 실내 온도 · 창문 한 줄, 공기 청정이 아래 한 줄을 다 쓴다
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // 견종 기준보다 덥거나 추우면 면 · 테두리 · 글자로 알린다 (Figma 488:12312)
                StatusTile(
                    icon = MungoIcon.THERMOMETER,
                    label = "실내 온도",
                    value = uiState.seatTemperature?.takeIf { linked }
                        ?.let { "${it.roundToInt()}°C" } ?: "—",
                    alert = linked && uiState.temperatureStatus == CheckStatus.WARNING
                )
                StatusTile(
                    icon = MungoIcon.LOCK,
                    label = "창문",
                    value = when (uiState.isAllWindowsClosed.takeIf { linked }) {
                        true -> "닫힘"
                        false -> "열림"
                        null -> "—"
                    },
                    highlighted = linked && uiState.isAllWindowsClosed == true
                )
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // 공기 청정은 아직 차량 SDK에서 읽지 않는다
                StatusTile(icon = MungoIcon.FAN, label = "공기 청정", value = "—")
            }
        }
    }
}

/**
 * 프로필 칩: 모양(알약)은 같고 연동 상태에 따라 색만 바뀐다 (Figma 470:55188 연동 중 · 470:56703 연결해제)
 * 연동 중 = 연한 망고 배경 + 망고 글자, 해지 = 회색 배경 + 진한 글자
 */
@Composable
private fun PetTag(text: String, linked: Boolean) {
    val colors = MungoTheme.colors
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .background(if (linked) colors.bgAccentSubtle else colors.bgSurfaceSunken, CircleShape)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = Mg.semi(24, 31),
            color = if (linked) colors.textAccent else colors.textPrimary
        )
    }
}

@Composable
private fun GearBadge(gear: String) {
    val colors = MungoTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(colors.bgAccent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = gear,
            style = Mg.bold(26, 34),
            color = colors.textOnAccent
        )
    }
}

@Composable
private fun RowScope.StatusTile(
    icon: MungoIcon,
    label: String,
    value: String,
    highlighted: Boolean = false,
    alert: Boolean = false
) {
    val colors = MungoTheme.colors
    val shape = RoundedCornerShape(MungoRadius.lg)
    val accent = highlighted || alert
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(
                when {
                    alert -> colors.alertSurface()
                    highlighted -> colors.bgAccentSubtle
                    else -> colors.bgSurfaceSunken
                },
                shape
            )
            .then(
                when {
                    alert -> Modifier.border(ALERT_STROKE, colors.borderAccent, shape)
                    highlighted -> Modifier.border(
                        2.dp,
                        colors.borderAccent.copy(alpha = 0.4f),
                        shape
                    )
                    else -> Modifier
                }
            )
            .padding(start = 14.dp, end = 28.dp, top = 17.dp, bottom = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(colors.bgSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MungoIconView(
                    icon,
                    tint = if (accent) colors.textAccent else colors.textPrimary,
                    size = 22.dp
                )
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text = label,
                style = if (alert) Mg.bold(24, 30) else Mg.regular(24, 30),
                color = if (alert) colors.textAccent else colors.textSecondary,
                maxLines = 1
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = if (alert) Mg.bold(35, 42) else Mg.semi(35, 42),
            color = if (accent) colors.textAccent else colors.textPrimary,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

/** 기준을 벗어난 값의 면 (라이트 #FFF1E2 · 다크는 망고 subtle) */
@Composable
private fun com.example.poco.core.theme.MungoColors.alertSurface(): Color =
    if (isDark) bgAccentSubtle else Mg.AlertBg

// endregion

// region 드라이브 카드

/**
 * 드라이브 카드: 연동 중 · 해지 모두 같은 배치.
 * 해지하면 칩 · 제목 · 버튼 문구가 바뀌고, 버튼은 '다시 연결하기'로 연동을 되살린다
 */
@Composable
private fun DriveCard(
    pet: Pet?,
    onStartDrive: () -> Unit,
    modifier: Modifier = Modifier,
    linked: Boolean = true,
    onRelink: () -> Unit = {}
) {
    val colors = MungoTheme.colors
    val name = pet?.name ?: "우리 아이"
    HomeCard(modifier = modifier, padding = 48.dp) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // 상태 칩 (해지 = 연한 망고 배경 + 망고 글자)
                val chipText = if (linked) colors.textPrimary else colors.textAccent
                Row(
                    modifier = Modifier
                        .background(
                            if (linked) colors.bgSurfaceSunken else colors.bgAccentSubtle,
                            CircleShape
                        )
                        .heightIn(min = 62.dp)
                        .padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MungoIconView(MungoIcon.CAR, tint = chipText, size = 24.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (linked) "탑승 준비 전" else "차량 연동이 꺼져 있어요",
                        style = Mg.semi(23, 30),
                        color = chipText
                    )
                }
                Spacer(Modifier.height(36.dp))
                Text(
                    text = if (linked) {
                        "$name,\n드라이브 갈까요?"
                    } else {
                        "차량을 연결하면\n${name.eulReul} 지켜볼게요"
                    },
                    style = Mg.bold(65, 81),
                    color = colors.textPrimary
                )
                Spacer(Modifier.weight(1f))
                // 가장 큰 터치 영역: 탑승 준비 시작 → 탑승 준비 (기어 D · 내비 길 안내면 바로 주행)
                val shape = CircleShape
                Row(
                    modifier = Modifier
                        .background(colors.bgAccent, shape)
                        .pressable(shape = shape, onClick = if (linked) onStartDrive else onRelink)
                        .heightIn(min = 108.dp)
                        .padding(horizontal = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (linked) "탑승 준비 시작" else "다시 연결하기",
                        style = Mg.bold(40, 52),
                        color = colors.textOnAccent
                    )
                    Spacer(Modifier.width(16.dp))
                    MungoIconView(
                        if (linked) MungoIcon.PLAY_FILLED else MungoIcon.CAR,
                        tint = colors.textOnAccent,
                        size = 32.dp
                    )
                }
            }
            DriveIllustration(pet)
        }
    }
}

@Composable
private fun DriveIllustration(pet: Pet?) {
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

// endregion

// region 아래 카드 (연동 해지면 Figma 488:11767 · 488:11775처럼 회색 면 · 회색 값)

@Composable
private fun ComfortCard(score: Int?, modifier: Modifier = Modifier, linked: Boolean = true) {
    HomeCard(modifier = modifier, linked = linked) {
        CardHeader(icon = MungoIcon.MAP, title = "컴포트 점수", linked = linked)
        Spacer(Modifier.height(16.dp))
        BigValue(value = score?.toString() ?: "0", unit = "점", linked = linked)
        Spacer(Modifier.height(16.dp))
        // 남은 공간의 가로 · 세로 가운데에, 칸 높이가 허락하는 만큼 (최대 RING_SIZE) 크게
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val ringModifier = Modifier
                .heightIn(max = RING_SIZE)
                .fillMaxHeight()
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
            if (score != null) {
                ScoreRing(
                    score = score,
                    modifier = ringModifier,
                    strokeWidth = RING_STROKE,
                    progressColor = if (linked) null else offStrongColor()
                )
            } else {
                EmptyRing(modifier = ringModifier, linked = linked)
            }
        }
    }
}

@Composable
private fun WeekTripCard(
    counts: List<Int>,
    modifier: Modifier = Modifier,
    linked: Boolean = true,
    onClick: () -> Unit = {}
) {
    val colors = MungoTheme.colors
    val active = if (linked) colors.bgAccent else offStrongColor()
    val idle = if (linked) colors.borderDefault else offTrackColor()
    HomeCard(
        modifier = modifier.pressable(RoundedCornerShape(MungoRadius.xl), onClick = onClick),
        linked = linked
    ) {
        CardHeader(icon = MungoIcon.CALENDAR, title = "이번 주 이동", linked = linked)
        Spacer(Modifier.height(16.dp))
        BigValue(value = counts.sum().toString(), unit = "회", linked = linked)
        Spacer(Modifier.weight(1f))
        // 요일 칸이 카드 폭을 나눠 쓴다 (넓어져도 막대가 같이 길어져 빈 곳이 생기지 않게)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WEEK_DAYS.forEachIndexed { index, day ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(
                                if (counts.getOrElse(index) { 0 } > 0) active else idle,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = day,
                        style = Mg.regular(17, 22),
                        color = colors.textTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun CardHeader(icon: MungoIcon, title: String, linked: Boolean = true) {
    val colors = MungoTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(if (linked) colors.bgSurfaceSunken else offTagColor(), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(
                icon,
                tint = if (linked) colors.textPrimary else colors.textTertiary,
                size = 24.dp
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = Mg.regular(28, 36),
            color = if (linked) Color(0xFF4D4D4D) else colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        MungoIconView(MungoIcon.CHEVRON_RIGHT, tint = colors.textTertiary, size = 28.dp)
    }
}

@Composable
private fun BigValue(value: String, unit: String?, linked: Boolean = true) {
    val colors = MungoTheme.colors
    val color = if (linked) colors.textPrimary else colors.textTertiary
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = Mg.bold(if (unit == null) 64 else 67, 80),
            color = color
        )
        unit?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = it,
                style = Mg.semi(30, 38),
                color = color,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
    }
}

/** 아직 이동 기록이 없을 때: 빈 링 + '–' */
@Composable
private fun EmptyRing(modifier: Modifier = Modifier, linked: Boolean = true) {
    val colors = MungoTheme.colors
    val track = if (linked) colors.borderDefault else offTrackColor()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = RING_STROKE.toPx()
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
        Text(text = "–", style = MungoTheme.typography.title1, color = colors.textTertiary)
    }
}

// endregion

// region 공통

/** 연동 해지면 카드 면만 회색으로 (배치 · 크기는 그대로) */
@Composable
private fun HomeCard(
    modifier: Modifier = Modifier,
    padding: Dp = 32.dp,
    linked: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(
                if (linked) MungoTheme.colors.bgSurface else offCardColor(),
                RoundedCornerShape(MungoRadius.xl)
            )
            .padding(padding),
        content = content
    )
}

@Composable
private fun offCardColor(): Color = MungoTheme.colors.let {
    if (it.isDark) it.bgSurface else Color(0xFFE8EBEE)
}

@Composable
private fun offTrackColor(): Color = MungoTheme.colors.let {
    if (it.isDark) it.bgNeutral else Color(0xFFD9DDE2)
}

@Composable
private fun offStrongColor(): Color = MungoTheme.colors.let {
    if (it.isDark) it.textTertiary else Color(0xFFB0B8C1)
}

@Composable
private fun offTagColor(): Color = MungoTheme.colors.let {
    if (it.isDark) it.bgSurfaceSunken else Color(0xFFDDE1E5)
}

@Composable
private fun VerticalLine() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(MungoTheme.colors.borderDefault)
    )
}

/** "시츄 | 10살 | 7kg" */
private fun Pet.detailLine(separator: String = " | "): String = listOfNotNull(
    breed.takeIf { it.isNotBlank() },
    ageYears?.let { "${it}살" },
    weightKg?.let { "${it.toKgLabel()}kg" }
).joinToString(separator)

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
private const val HERO_WEIGHT = 655f / 428f
private val ALERT_STROKE = 3.dp
private val AVATAR_SIZE = 132.dp
private val DRIVE_ILLUSTRATION = 380.dp
private val RING_SIZE = 200.dp
private val RING_STROKE = 20.dp
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
internal fun HomeScreenPreview() {
    MungoTheme(darkTheme = false) { HomeScreen(previewState(), {}, {}) }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "다크 모드")
@Composable
private fun HomeScreenDarkPreview() {
    MungoTheme(darkTheme = true) {
        HomeScreen(previewState().copy(lastTrip = previewTrip()), {}, {})
    }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "첫 실행 (빈 상태)")
@Composable
private fun HomeScreenEmptyPreview() {
    MungoTheme(darkTheme = false) { HomeScreen(HomeUiState(), {}, {}) }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "실내 온도 경고 (더움)")
@Composable
internal fun HomeScreenHotPreview() {
    MungoTheme(darkTheme = false) { HomeScreen(previewState().copy(seatTemperature = 29f), {}, {}) }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "차량 연동 해지")
@Composable
internal fun HomeScreenUnlinkedPreview() {
    MungoTheme(darkTheme = false) {
        HomeScreen(
            previewState().copy(
                isVehicleLinked = false,
                isParked = null,
                seatTemperature = null,
                isAllWindowsClosed = null,
                vehicleName = "그랜저",
                lastTrip = previewTrip(),
                weekTripCounts = listOf(1, 0, 2, 0, 1, 3, 0)
            ),
            {},
            {}
        )
    }
}
