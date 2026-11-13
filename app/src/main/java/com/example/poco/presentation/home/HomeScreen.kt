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
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.util.eulReul
import com.example.poco.core.util.toMonthDay
import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.PetTrait
import com.example.poco.data.model.TripSummary
import com.example.poco.domain.driving.PetType
import com.example.poco.domain.driving.temperatureStandard
import com.example.poco.presentation.common.MungoNavRail
import com.example.poco.presentation.common.RailTab
import com.example.poco.presentation.design.Mg
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/*
 * 홈 (mungo 시안 · 1700×1184)
 * [메뉴 레일 | 펫 · 차량 상태 패널 | 드라이브 카드 + 컴포트 점수 · 이번 주 이동]
 * - 펫모드는 이번 버전에서 빼서, 그 자리는 이번 주 이동 카드가 넓게 쓴다
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
        // Figma 히어로 655 : 아래 카드 428, 위 · 아래 39 · 왼쪽 32 · 오른쪽 37 · 간격 23
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
                if (uiState.isVehicleLinked) {
                    // 펫모드 카드가 빠진 자리는 이번 주 이동이 이어서 쓴다 (컴포트 카드는 원래 폭 그대로 1 : 2)
                    ComfortCard(score = uiState.lastTrip?.comfortScore, modifier = cell())
                    WeekTripCard(
                        counts = uiState.weekTripCounts,
                        modifier = Modifier.weight(2f).fillMaxHeight(),
                        onClick = onOpenWeekTrips
                    )
                } else {
                    // Figma 488:11669: 연동이 꺼지면 기록만 회색으로 보여 준다
                    RecentTripOffCard(
                        trip = uiState.lastTrip,
                        modifier = cell(),
                        onClick = onOpenWeekTrips
                    )
                    WeekTripOffCard(
                        counts = uiState.weekTripCounts,
                        modifier = cell(),
                        onClick = onOpenWeekTrips
                    )
                    RestIntervalOffCard(
                        minutes = uiState.restIntervalMinutes,
                        reason = uiState.pet?.restReason(),
                        modifier = cell()
                    )
                }
            }
        }
    }
}

private fun RowScope.cell(): Modifier = Modifier.weight(1f).fillMaxHeight()

// region 펫 · 차량 상태 패널

@Composable
private fun PetPanel(uiState: HomeUiState, onOpenPet: () -> Unit, modifier: Modifier = Modifier) {
    if (uiState.isVehicleLinked) {
        LinkedPetPanel(uiState, onOpenPet, modifier)
    } else {
        UnlinkedPetPanel(uiState, onOpenPet, modifier)
    }
}

@Composable
private fun LinkedPetPanel(
    uiState: HomeUiState,
    onOpenPet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    val typography = POCOTheme.typography
    val pet = uiState.pet
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
                tags.forEach { PetTag(it, linked = uiState.isVehicleLinked) }
            }
        }
        Spacer(Modifier.height(60.dp))
        HorizontalDivider(color = colors.borderDefault)
        Spacer(Modifier.height(58.dp))

        // 차량 상태
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    uiState.isParked == true -> "차량 정차 중"
                    uiState.isParked == false -> "주행 중"
                    else -> "차량 확인 중"
                },
                style = Mg.bold(37, 46),
                color = colors.textPrimary
            )
            if (uiState.isParked != null) {
                Spacer(Modifier.width(12.dp))
                GearBadge(if (uiState.isParked) "P" else "D")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = listOfNotNull(uiState.vehicleName, "mungo 연동 중").joinToString(" | "),
            style = Mg.regular(24, 31),
            color = colors.textSecondary
        )
        Spacer(Modifier.height(54.dp))

        // 상태 칸: 실내 온도 · 창문 한 줄, 공기 청정이 아래 한 줄을 다 쓴다 (연동 해지 화면과 같은 배치)
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
                    value = uiState.seatTemperature?.let { "${it.roundToInt()}°C" } ?: "—",
                    alert = uiState.temperatureStatus == CheckStatus.WARNING
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
    val colors = POCOTheme.colors
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
    val colors = POCOTheme.colors
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
    val colors = POCOTheme.colors
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
private fun com.example.poco.core.theme.POCOColors.alertSurface(): Color =
    if (isDark) bgAccentSubtle else Mg.AlertBg

// region 연동 해지 패널 (Figma 488:11669 정보 패널)

@Composable
private fun UnlinkedPetPanel(
    uiState: HomeUiState,
    onOpenPet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    val pet = uiState.pet
    val today = remember { System.currentTimeMillis().toMonthDay() }
    Column(
        modifier = modifier
            .background(colors.bgSurface)
            .padding(start = 32.dp, end = 32.dp, top = 52.dp, bottom = 47.dp)
    ) {
        Text(text = today, style = Mg.regular(20, 24), color = colors.textSecondary)
        Spacer(Modifier.height(47.dp))
        Column(
            modifier = Modifier.pressable(RoundedCornerShape(MungoRadius.md), onClick = onOpenPet)
        ) {
            Box(
                modifier = Modifier
                    .size(OFF_AVATAR_SIZE)
                    .background(colors.bgSurfaceSunken, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PetPhoto(
                    photoPath = pet?.photoPath,
                    breed = pet?.breed,
                    modifier = Modifier.size(
                        if (hasFullPetImage(pet?.photoPath, pet?.breed)) OFF_AVATAR_SIZE else 72.dp
                    )
                )
            }
            Spacer(Modifier.height(30.dp))
            Text(text = "프로필", style = Mg.regular(20, 24), color = colors.textSecondary)
            Spacer(Modifier.height(7.dp))
            Text(
                text = pet?.name ?: "펫을 추가해 주세요",
                style = Mg.bold(48, 57),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            pet?.detailLine(" · ")?.takeIf { it.isNotEmpty() }?.let { detail ->
                Spacer(Modifier.height(13.dp))
                Text(
                    text = detail,
                    style = Mg.regular(23, 27),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        pet?.tags()?.takeIf { it.isNotEmpty() }?.let { tags ->
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .height(46.dp)
                            .background(colors.bgSurfaceSunken, CircleShape)
                            .padding(horizontal = 21.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = tag, style = Mg.bold(20, 24), color = colors.textPrimary)
                    }
                }
            }
        }
        Spacer(Modifier.height(34.dp))
        HorizontalDivider(thickness = 2.dp, color = colors.borderDefault)
        Spacer(Modifier.height(30.dp))
        Text(text = "차량", style = Mg.regular(20, 24), color = colors.textSecondary)
        Spacer(Modifier.height(10.dp))
        Text(text = "연결 안 됨", style = Mg.bold(37, 44), color = colors.textTertiary)
        Spacer(Modifier.height(12.dp))
        Text(
            text = listOfNotNull(uiState.vehicleName, "연동이 꺼져 있어요").joinToString(" · "),
            style = Mg.regular(21, 25),
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(39.dp))
        // 차량 값은 읽지 않으니 모두 '–' (공기 청정이 한 줄을 다 쓴다)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(19.dp)
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OffStatusTile(MungoIcon.THERMOMETER, "실내 온도", Modifier.weight(1f))
                OffStatusTile(MungoIcon.LOCK, "창문 잠금", Modifier.weight(1f))
            }
            OffStatusTile(MungoIcon.FAN, "공기 청정", Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun OffStatusTile(icon: MungoIcon, label: String, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    val iconColor = if (colors.isDark) colors.textSecondary else OFF_ICON
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colors.bgSurfaceSunken, RoundedCornerShape(OFF_TILE_RADIUS))
            .padding(start = 27.dp, end = 20.dp, top = 32.dp)
    ) {
        MungoIconView(icon, tint = iconColor, size = 26.dp)
        Spacer(Modifier.height(28.dp))
        Text(text = label, style = Mg.regular(21, 25), color = iconColor)
        Spacer(Modifier.height(20.dp))
        Text(text = "–", style = Mg.bold(35, 42), color = colors.textPrimary)
    }
}

// endregion

// endregion

// region 드라이브 카드

@Composable
private fun DriveCard(
    pet: Pet?,
    onStartDrive: () -> Unit,
    modifier: Modifier = Modifier,
    linked: Boolean = true,
    onRelink: () -> Unit = {}
) {
    if (linked) {
        LinkedDriveCard(pet, onStartDrive, modifier)
    } else {
        UnlinkedDriveCard(pet, onRelink, modifier)
    }
}

@Composable
private fun LinkedDriveCard(pet: Pet?, onStartDrive: () -> Unit, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
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
                        .heightIn(min = 62.dp)
                        .padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MungoIconView(MungoIcon.CAR, tint = colors.textPrimary, size = 24.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(text = "탑승 준비 전", style = Mg.semi(23, 30), color = colors.textPrimary)
                }
                Spacer(Modifier.height(36.dp))
                Text(
                    text = "$name,\n드라이브 갈까요?",
                    style = Mg.bold(65, 81),
                    color = colors.textPrimary
                )
                Spacer(Modifier.weight(1f))
                // 가장 큰 터치 영역: 탑승 준비 시작 → 탑승 준비 (기어 D · 내비 길 안내면 바로 주행)
                val shape = CircleShape
                Row(
                    modifier = Modifier
                        .background(colors.bgAccent, shape)
                        .pressable(shape = shape, onClick = onStartDrive)
                        .heightIn(min = 108.dp)
                        .padding(horizontal = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "탑승 준비 시작", style = Mg.bold(40, 52), color = colors.textOnAccent)
                    Spacer(Modifier.width(16.dp))
                    MungoIconView(MungoIcon.PLAY_FILLED, tint = colors.textOnAccent, size = 32.dp)
                }
            }
            DriveIllustration(pet)
        }
    }
}

/** Figma 488:11750 히어로 (연동 해지): 칩 → 두 줄 제목 → 안내 → 둥근 사각 '다시 연결하기' */
@Composable
private fun UnlinkedDriveCard(pet: Pet?, onRelink: () -> Unit, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    val name = pet?.name ?: "우리 아이"
    Row(
        modifier = modifier
            .background(colors.bgSurface, RoundedCornerShape(HERO_RADIUS))
            .padding(start = 53.dp, end = 29.dp, top = 56.dp, bottom = 53.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .background(colors.bgAccentSubtle, CircleShape)
                    .padding(horizontal = 23.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MungoIconView(MungoIcon.CAR, tint = colors.textAccent, size = 22.dp)
                Spacer(Modifier.width(10.dp))
                Text(text = "차량 연동이 꺼져 있어요", style = Mg.bold(21, 25), color = colors.textAccent)
            }
            Spacer(Modifier.height(34.dp))
            Text(
                text = "차량을 연결하면\n${name.eulReul} 지켜볼게요",
                style = Mg.bold(54, 75),
                color = colors.textPrimary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "이동 기록과 프로필은 그대로 있어요",
                style = Mg.regular(25, 30),
                color = colors.textSecondary
            )
            Spacer(Modifier.weight(1f))
            val shape = RoundedCornerShape(RELINK_RADIUS)
            Box(
                modifier = Modifier
                    .size(width = 428.dp, height = 107.dp)
                    .background(colors.bgAccent, shape)
                    .pressable(shape = shape, onClick = onRelink),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "다시 연결하기", style = Mg.bold(32, 38), color = colors.textOnAccent)
            }
        }
        DriveIllustration(pet)
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
private fun WeekTripCard(
    counts: List<Int>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val colors = POCOTheme.colors
    HomeCard(modifier = modifier.pressable(RoundedCornerShape(MungoRadius.xl), onClick = onClick)) {
        CardHeader(icon = MungoIcon.CALENDAR, title = "이번 주 이동")
        Spacer(Modifier.height(16.dp))
        BigValue(value = counts.sum().toString(), unit = "회")
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
                        style = Mg.regular(17, 22),
                        color = colors.textTertiary
                    )
                }
            }
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
            style = Mg.regular(28, 36),
            color = Color(0xFF4D4D4D),
            modifier = Modifier.weight(1f)
        )
        MungoIconView(MungoIcon.CHEVRON_RIGHT, tint = colors.textTertiary, size = 28.dp)
    }
}

@Composable
private fun BigValue(value: String, unit: String?) {
    val colors = POCOTheme.colors
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = Mg.bold(
                if (unit ==
                    null
                ) {
                    64
                } else {
                    67
                },
                80
            ),
            color = colors.textPrimary
        )
        unit?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = it,
                style = Mg.semi(30, 38),
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

// region 연동 해지 아래 카드 (Figma 488:11767 · 488:11775 · 488:11794) - 값은 회색으로만

@Composable
private fun OffCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(MungoRadius.xl)
    Column(
        modifier = modifier
            .clip(shape)
            .background(offCardColor(), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 35.dp, end = 30.dp, top = 40.dp, bottom = 30.dp),
        content = content
    )
}

@Composable
private fun OffLabel(text: String) {
    Text(
        text = text,
        style = Mg.regular(21, 25),
        color = POCOTheme.colors.textSecondary,
        maxLines = 1
    )
}

@Composable
private fun OffValue(value: String, unit: String?) {
    val colors = POCOTheme.colors
    Row(verticalAlignment = Alignment.Bottom) {
        Text(text = value, style = Mg.bold(67, 80), color = colors.textTertiary)
        unit?.let {
            Spacer(Modifier.width(6.dp))
            Text(
                text = it,
                style = Mg.regular(27, 32),
                color = colors.textTertiary,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }
    }
}

@Composable
private fun RecentTripOffCard(
    trip: TripSummary?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val colors = POCOTheme.colors
    OffCard(modifier = modifier, onClick = onClick) {
        OffLabel(trip?.let { "최근 이동 · ${it.endedAtMillis.relativeDay()}" } ?: "최근 이동")
        Spacer(Modifier.height(22.dp))
        OffValue(value = trip?.comfortScore?.toString() ?: "–", unit = trip?.let { "점" })
        Spacer(Modifier.weight(1f))
        val track = offTrackColor()
        val progress = offStrongColor()
        val sweep = (trip?.comfortScore ?: 0).coerceIn(0, 100) * 3.6f
        Canvas(modifier = Modifier.size(OFF_RING)) {
            val stroke = OFF_RING_STROKE.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
            if (sweep > 0f) {
                drawArc(
                    progress,
                    -90f,
                    sweep,
                    false,
                    topLeft,
                    arc,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Spacer(Modifier.height(36.dp))
        Text(text = "리포트 보기 ›", style = Mg.regular(20, 24), color = colors.textTertiary)
    }
}

@Composable
private fun WeekTripOffCard(
    counts: List<Int>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val colors = POCOTheme.colors
    val max = counts.maxOrNull() ?: 0
    OffCard(modifier = modifier, onClick = onClick) {
        OffLabel("이번 주 이동")
        Spacer(Modifier.height(22.dp))
        OffValue(value = counts.sum().toString(), unit = "회")
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            WEEK_DAYS.forEachIndexed { index, day ->
                val count = counts.getOrElse(index) { 0 }
                // 0회는 낮은 점, 가장 많이 다닌 날이 가장 길고 진하다
                val barHeight = if (count == 0 ||
                    max == 0
                ) {
                    OFF_BAR_MIN
                } else {
                    OFF_BAR_LOW +
                        (OFF_BAR_MAX - OFF_BAR_LOW) * (count.toFloat() / max)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(width = 21.dp, height = barHeight)
                            .background(
                                if (count > 0 &&
                                    count == max
                                ) {
                                    offStrongColor()
                                } else {
                                    offTrackColor()
                                },
                                RoundedCornerShape(5.dp)
                            )
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(text = day, style = Mg.regular(17, 20), color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun RestIntervalOffCard(minutes: Int, reason: String?, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    OffCard(modifier = modifier) {
        OffLabel("휴식 추천 간격")
        Spacer(Modifier.height(22.dp))
        if (minutes % 60 == 0) {
            OffValue(value = (minutes / 60).toString(), unit = "시간마다")
        } else {
            OffValue(value = minutes.toString(), unit = "분마다")
        }
        Spacer(Modifier.weight(1f))
        reason?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 107.dp)
                    .background(offTagColor(), RoundedCornerShape(21.dp))
                    .padding(start = 23.dp, end = 16.dp, top = 22.dp, bottom = 22.dp)
            ) {
                MungoIconView(
                    MungoIcon.TAG,
                    tint = colors.textTertiary,
                    size = 20.dp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(text = it, style = Mg.bold(20, 32), color = colors.textTertiary, maxLines = 2)
            }
        }
        Spacer(Modifier.height(5.dp))
    }
}

@Composable
private fun offCardColor(): Color = POCOTheme.colors.let {
    if (it.isDark) it.bgSurface else Color(0xFFE8EBEE)
}

@Composable
private fun offTrackColor(): Color = POCOTheme.colors.let {
    if (it.isDark) it.bgNeutral else Color(0xFFD9DDE2)
}

@Composable
private fun offStrongColor(): Color = POCOTheme.colors.let {
    if (it.isDark) it.textTertiary else Color(0xFFB0B8C1)
}

@Composable
private fun offTagColor(): Color = POCOTheme.colors.let {
    if (it.isDark) it.bgSurfaceSunken else Color(0xFFDDE1E5)
}

/** 오늘 · 어제 · N일 전 */
private fun Long.relativeDay(): String {
    val zone = ZoneId.systemDefault()
    val days = ChronoUnit.DAYS.between(
        Instant.ofEpochMilli(this).atZone(zone).toLocalDate(),
        LocalDate.now(zone)
    )
    return when {
        days <= 0L -> "오늘"
        days == 1L -> "어제"
        else -> "${days}일 전"
    }
}

/** 휴식 카드 꼬리표: 왜 이 간격으로 쉬는지 한 줄 */
private fun Pet.restReason(): String {
    val types = temperatureStandard.types
    val heatRisk =
        PetType.BRACHYCEPHALIC in types ||
            PetType.DOUBLE_COAT in types ||
            PetTrait.HEAT_SENSITIVE in traits
    return when {
        heatRisk -> "더위에 약해 자주\n쉬어 가요"
        PetTrait.MOTION_SICK in traits -> "멀미가 있어 자주\n쉬어 가요"
        (ageYears ?: 0) >= SENIOR_AGE -> "노령견이라 자주\n쉬어 가요"
        else -> "정한 간격마다\n쉬어 가요"
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

/** "시츄 | 10살 | 7kg" (해지 패널은 " · ") */
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
private val HERO_RADIUS = 37.dp
private val RELINK_RADIUS = 29.dp
private val ALERT_STROKE = 3.dp
private val OFF_AVATAR_SIZE = 102.dp
private val OFF_TILE_RADIUS = 27.dp
private val OFF_ICON = Color(0xFF4E5968)
private val OFF_RING = 99.dp
private val OFF_RING_STROKE = 13.dp
private val OFF_BAR_MIN = 11.dp
private val OFF_BAR_LOW = 24.dp
private val OFF_BAR_MAX = 75.dp
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
internal fun HomeScreenPreview() {
    POCOTheme(darkTheme = false) { HomeScreen(previewState(), {}, {}) }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "다크 모드")
@Composable
private fun HomeScreenDarkPreview() {
    POCOTheme(darkTheme = true) {
        HomeScreen(previewState().copy(lastTrip = previewTrip()), {}, {})
    }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "첫 실행 (빈 상태)")
@Composable
private fun HomeScreenEmptyPreview() {
    POCOTheme(darkTheme = false) { HomeScreen(HomeUiState(), {}, {}) }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "실내 온도 경고 (더움)")
@Composable
internal fun HomeScreenHotPreview() {
    POCOTheme(darkTheme = false) { HomeScreen(previewState().copy(seatTemperature = 29f), {}, {}) }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "차량 연동 해지")
@Composable
internal fun HomeScreenUnlinkedPreview() {
    POCOTheme(darkTheme = false) {
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
