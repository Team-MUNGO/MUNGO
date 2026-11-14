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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.PetPhoto
import com.example.poco.core.designsystem.ScoreRing
import com.example.poco.core.designsystem.ShihTzuIllustration
import com.example.poco.core.designsystem.component.pressable
import com.example.poco.core.designsystem.hasFullPetImage
import com.example.poco.core.designsystem.previewTrip
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.MungoTheme
import com.example.poco.core.util.eulReul
import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.PetTrait
import com.example.poco.domain.driving.TemperatureGroup
import com.example.poco.domain.driving.isSenior
import com.example.poco.domain.driving.temperatureStandard
import com.example.poco.presentation.common.MungoNavRail
import com.example.poco.presentation.common.RailTab
import com.example.poco.presentation.design.Mg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/*
 * 홈 (Figma 페이지 4 · Home 510:38 · 1700×1184)
 * [메뉴 레일 | 펫 · 차량 상태 패널 | 드라이브 카드 + 최근 이동 · 이번 주 이동 · 휴식 추천 간격]
 * - 공기 청정은 공조 SDK에서 읽고 누르면 켜고 끈다. 지원하지 않는 차 · 못 읽는 값은 '—'
 * - 연동을 해지해도 배치는 같고, 아래 카드는 회색 면 · 회색 값이 되며 눌리지 않는다
 */

@Composable
fun HomeRoute(
    navigateToPetProfile: () -> Unit,
    navigateToBoarding: () -> Unit,
    navigateToDriving: () -> Unit,
    navigateToRecords: () -> Unit = {},
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
        onOpenRecentTrip = navigateToRecords,
        onOpenWeekTrips = navigateToWeeklyRecords,
        onToggleAirCleaner = viewModel::toggleAirCleaner
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onOpenPet: () -> Unit,
    onStartDrive: () -> Unit,
    modifier: Modifier = Modifier,
    onRelink: () -> Unit = {},
    onOpenRecentTrip: () -> Unit = {},
    onOpenWeekTrips: () -> Unit = {},
    onToggleAirCleaner: () -> Unit = {}
) {
    val colors = MungoTheme.colors
    val linked = uiState.isVehicleLinked
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
                .fillMaxHeight(),
            onToggleAirCleaner = onToggleAirCleaner
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
                linked = linked,
                onRelink = onRelink,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(HERO_WEIGHT)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 최근 이동 · 이번 주 이동은 누르면 이동 기록 탭으로 (연동 해지 중에는 눌리지 않는다)
                RecentTripCard(
                    score = uiState.lastTrip?.comfortScore,
                    endedAtMillis = uiState.lastTrip?.endedAtMillis,
                    linked = linked,
                    modifier = cell(),
                    onClick = onOpenRecentTrip
                )
                WeekTripCard(
                    counts = uiState.weekTripCounts,
                    linked = linked,
                    modifier = cell(),
                    onClick = onOpenWeekTrips
                )
                RestIntervalCard(
                    minutes = uiState.restIntervalMinutes,
                    reason = uiState.pet?.restReason(),
                    linked = linked,
                    modifier = cell()
                )
            }
        }
    }
}

private fun RowScope.cell(): Modifier = Modifier.weight(1f).fillMaxHeight()

// region 펫 · 차량 상태 패널

/** 연동 중 · 해지 모두 같은 배치. 해지하면 차량 값은 '—', 강조 칸은 회색 */
@Composable
private fun PetPanel(
    uiState: HomeUiState,
    onOpenPet: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleAirCleaner: () -> Unit = {}
) {
    val colors = MungoTheme.colors
    val typography = MungoTheme.typography
    val pet = uiState.pet
    val linked = uiState.isVehicleLinked
    val today = remember { DATE_FORMAT.format(Date()) }
    Column(
        modifier = modifier
            .background(colors.bgSurface)
            .padding(start = 32.dp, end = 32.dp, top = 52.dp, bottom = 46.dp)
    ) {
        // Figma 510:84 날짜 · 510:85 사진 · 510:87 이름 · 510:95 구분선 · 510:102 상태 칸
        Text(text = today, style = Mg.regular(20, 24), color = colors.textSecondary)
        Spacer(Modifier.height(47.dp))
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
                        if (hasFullPetImage(pet?.photoPath, pet?.breed)) AVATAR_SIZE else 74.dp
                    )
                )
            }
            Spacer(Modifier.height(31.dp))
            Text(text = "프로필", style = Mg.regular(20, 24), color = colors.textSecondary)
            Spacer(Modifier.height(7.dp))
            Text(
                text = pet?.name ?: "펫을 추가해 주세요",
                style = if (pet != null) Mg.bold(48, 58) else typography.title1,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            pet?.detailLine()?.takeIf { it.isNotEmpty() }?.let { detail ->
                Spacer(Modifier.height(13.dp))
                Text(
                    text = detail,
                    style = Mg.regular(23, 28),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        pet?.let(::petChips)?.takeIf { it.all > 0 }?.let { chips ->
            Spacer(Modifier.height(19.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                chips.shown.forEach { PetTag(it) }
                // "+N"을 누르면 마이펫(케어 기준)으로
                if (chips.more > 0) PetTag("+${chips.more}", onClick = onOpenPet)
            }
        }
        Spacer(Modifier.height(35.dp))
        HorizontalDivider(color = colors.borderDefault)
        Spacer(Modifier.height(31.dp))

        // 차량 상태 (Figma 510:96 ~ 510:101)
        Text(text = "차량", style = Mg.regular(20, 24), color = colors.textSecondary)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (linked) "연결됨" else "연결 안 됨",
                style = Mg.bold(37, 44),
                color = if (linked) colors.textPrimary else colors.textTertiary
            )
            if (linked) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .background(colors.bgAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    MungoIconView(MungoIcon.CHECK, tint = colors.textOnAccent, size = 20.dp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = vehicleLine(uiState),
            style = Mg.regular(21, 25),
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(38.dp))

        // 상태 칸: 실내 온도 · 창문 한 줄, 공기 청정이 아래 한 줄을 다 쓴다
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(19.dp)
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(17.dp)) {
                // 견종 기준보다 덥거나 추우면 창문 닫힘과 같은 주황 강조로 알린다
                StatusTile(
                    icon = MungoIcon.THERMOMETER,
                    label = "실내 온도",
                    value = uiState.seatTemperature?.takeIf { linked }
                        ?.let { "${it.roundToInt()}°C" } ?: "–",
                    alert = linked && uiState.temperatureStatus == CheckStatus.WARNING
                )
                StatusTile(
                    icon = MungoIcon.LOCK,
                    label = "창문",
                    value = when (uiState.isAllWindowsClosed.takeIf { linked }) {
                        true -> "닫힘"
                        false -> "열림"
                        null -> "–"
                    },
                    highlighted = linked && uiState.isAllWindowsClosed == true
                )
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(17.dp)) {
                // 공기 청정: 차(공조 SDK)에서 읽은 켜짐/꺼짐. 누르면 켜고 끈다. 지원하지 않는 차는 '–'
                val airCleaner = uiState.isAirCleanerOn.takeIf { linked }
                StatusTile(
                    icon = MungoIcon.FAN,
                    label = if (airCleaner != null) "공기 청정 · 눌러서 바꾸기" else "공기 청정",
                    value = when {
                        airCleaner == null -> "–"
                        uiState.isAirCleanerChanging -> "바꾸는 중"
                        airCleaner -> "켜짐"
                        else -> "꺼짐"
                    },
                    highlighted = airCleaner == true,
                    onClick = if (airCleaner != null && !uiState.isAirCleanerChanging) {
                        onToggleAirCleaner
                    } else {
                        null
                    }
                )
            }
        }
    }
}

/** "그랜저 · 정차 중 · Pleos Connect" (해지하면 "그랜저 · 연동이 꺼져 있어요") */
private fun vehicleLine(uiState: HomeUiState): String = if (uiState.isVehicleLinked) {
    listOfNotNull(
        uiState.vehicleName,
        when (uiState.isParked) {
            true -> "정차 중"
            false -> "주행 중"
            null -> null
        },
        "Pleos Connect"
    )
} else {
    listOfNotNull(uiState.vehicleName, "연동이 꺼져 있어요")
}.joinToString(" · ")

/** 프로필 칩 (Figma 510:89): 회색 알약 + 진한 글자 */
@Composable
private fun PetTag(text: String, onClick: (() -> Unit)? = null) {
    val colors = MungoTheme.colors
    Box(
        modifier = Modifier
            .pressable(CircleShape, onClick = onClick)
            .heightIn(min = 45.dp)
            .background(colors.bgSurfaceSunken, CircleShape)
            .padding(horizontal = 19.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = Mg.bold(20, 24), color = colors.textPrimary)
    }
}

/** 상태 칸 (Figma 510:102): 왼쪽 위에 아이콘 · 이름 · 값을 차례로 */
@Composable
private fun RowScope.StatusTile(
    icon: MungoIcon,
    label: String,
    value: String,
    highlighted: Boolean = false,
    alert: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = MungoTheme.colors
    val shape = RoundedCornerShape(MungoRadius.lg)
    // 온도 경고도 창문 '닫힘'과 같은 홈 강조 스타일 (주행 화면의 굵은 테두리는 쓰지 않는다)
    val accent = highlighted || alert
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .pressable(shape, onClick = onClick)
            .background(if (accent) colors.bgAccentSubtle else colors.bgSurfaceSunken, shape)
            .then(
                if (accent) {
                    Modifier.border(2.dp, colors.borderAccent.copy(alpha = 0.4f), shape)
                } else {
                    Modifier
                }
            )
            .padding(start = 27.dp, end = 20.dp, top = 34.dp, bottom = 20.dp)
    ) {
        MungoIconView(
            icon,
            tint = if (accent) colors.textAccent else colors.textPrimary,
            size = 24.dp
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = label,
            style = Mg.regular(21, 30),
            color = colors.textSecondary,
            maxLines = 1
        )
        Spacer(Modifier.height(15.dp))
        Text(
            text = value,
            style = Mg.bold(35, 42),
            color = if (accent) colors.textAccent else colors.textPrimary
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
 * 드라이브 카드 (Figma 510:123): 연동 중 · 해지 모두 같은 배치.
 * 해지하면 칩 · 제목 · 버튼 문구가 바뀌고, 버튼은 '다시 연결하기'로 연동을 되살린다.
 * 오른쪽 그림은 등록한 사진 · 품종과 상관없이 늘 mungo 시츄 일러스트
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
    Box(
        modifier = modifier.background(colors.bgSurface, RoundedCornerShape(MungoRadius.xl))
    ) {
        // 연동 중 = 앉은 시츄 (510:137) · 해지 = 서 있는 시츄 (488:11766)
        ShihTzuIllustration(
            standing = !linked,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 29.dp, bottom = if (linked) 71.dp else 108.dp, top = 40.dp)
                .then(
                    if (linked) {
                        Modifier.size(width = 441.dp, height = 460.dp)
                    } else {
                        Modifier.size(width = 423.dp, height = 385.dp)
                    }
                )
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 53.dp, top = 56.dp, bottom = 53.dp)
        ) {
            // 상태 칩 (해지 = 연한 망고 배경 + 망고 글자)
            val chipText = if (linked) colors.textPrimary else colors.textAccent
            Row(
                modifier = Modifier
                    .background(
                        if (linked) colors.bgSurfaceSunken else colors.bgAccentSubtle,
                        CircleShape
                    )
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MungoIconView(
                    if (linked) MungoIcon.MAP_PIN else MungoIcon.CAR,
                    tint = chipText,
                    size = 20.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (linked) "탑승 준비 전" else "차량 연동이 꺼져 있어요",
                    style = Mg.bold(21, 26),
                    color = chipText
                )
            }
            Spacer(Modifier.height(40.dp))
            Text(
                text = if (linked) {
                    "$name,\n드라이브 갈까요?"
                } else {
                    "차량을 연결하면\n${name.eulReul} 지켜볼게요"
                },
                style = Mg.bold(54, 75),
                color = colors.textPrimary
            )
            if (!linked) {
                Spacer(Modifier.height(15.dp))
                Text(
                    text = "이동 기록과 프로필은 그대로 있어요",
                    style = Mg.regular(25, 30),
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.weight(1f))
            // 가장 큰 터치 영역: 탑승 준비 시작 → 탑승 준비 (기어 D · 내비 길 안내면 바로 주행)
            val shape = RoundedCornerShape(MungoRadius.lg)
            Row(
                modifier = Modifier
                    .background(colors.bgAccent, shape)
                    .pressable(shape = shape, onClick = if (linked) onStartDrive else onRelink)
                    .width(428.dp)
                    .heightIn(min = 107.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 해지 상태 '다시 연결하기'는 글자만 (Figma 488:11763)
                if (linked) {
                    MungoIconView(MungoIcon.PLAY_FILLED, tint = colors.textOnAccent, size = 24.dp)
                    Spacer(Modifier.width(20.dp))
                }
                Text(
                    text = if (linked) "탑승 준비 시작" else "다시 연결하기",
                    style = Mg.bold(32, 38),
                    color = colors.textOnAccent
                )
            }
        }
    }
}

// endregion

// region 아래 카드 (연동 해지면 Figma 488:11767 · 488:11775처럼 회색 면 · 회색 값)

/**
 * 최근 이동 (Figma 510:138 · 해지 488:11767): 마지막 이동의 컴포트 점수를 링으로.
 * 기록이 없으면 '아직 없어요' + 빈 링, 있으면 '최근 이동 · 어제' + 점수 + '리포트 보기 ›'
 */
@Composable
private fun RecentTripCard(
    score: Int?,
    endedAtMillis: Long?,
    modifier: Modifier = Modifier,
    linked: Boolean = true,
    onClick: () -> Unit = {}
) {
    HomeCard(
        modifier = modifier.pressable(
            RoundedCornerShape(MungoRadius.xl),
            onClick = onClick.takeIf { linked }
        ),
        linked = linked
    ) {
        CardLabel(
            listOfNotNull("최근 이동", endedAtMillis?.toRelativeDay()).joinToString(" · "),
            linked = linked
        )
        if (score != null) {
            Spacer(Modifier.height(23.dp))
            BigValue(value = score.toString(), unit = "점", linked = linked)
        } else {
            val colors = MungoTheme.colors
            Spacer(Modifier.height(36.dp))
            Text(
                text = "아직 없어요",
                style = Mg.bold(40, 48),
                color = if (linked) colors.textPrimary else colors.textTertiary
            )
        }
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
        if (score != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "리포트 보기 ›",
                style = Mg.regular(20, 24),
                color = MungoTheme.colors.textTertiary
            )
        }
    }
}

/** 이번 주 이동 (Figma 510:144 · 해지 488:11775): 횟수 + 요일별 막대 그래프 (횟수만큼 높게) */
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
        modifier = modifier.pressable(
            RoundedCornerShape(MungoRadius.xl),
            onClick = onClick.takeIf { linked }
        ),
        linked = linked
    ) {
        CardLabel("이번 주 이동", linked = linked)
        Spacer(Modifier.height(23.dp))
        BigValue(value = counts.sum().toString(), unit = "회", linked = linked)
        Spacer(Modifier.weight(1f))
        val max = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WEEK_DAYS.forEachIndexed { index, day ->
                val count = counts.getOrElse(index) { 0 }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    // 0회는 낮은 알약, 그 외에는 이번 주 최다 횟수 대비 높이
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BAR_MAX_HEIGHT),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    BAR_MIN_HEIGHT + (BAR_MAX_HEIGHT - BAR_MIN_HEIGHT) * count / max
                                )
                                .background(
                                    if (count > 0) active else idle,
                                    RoundedCornerShape(BAR_RADIUS)
                                )
                        )
                    }
                    Spacer(Modifier.height(13.dp))
                    Text(text = day, style = Mg.regular(17, 20), color = colors.textSecondary)
                }
            }
        }
    }
}

/** 휴식 추천 간격 (Figma 510:163): 설정한 간격 + 펫 특성에 따른 이유 꼬리표 */
@Composable
private fun RestIntervalCard(
    minutes: Int,
    reason: String?,
    modifier: Modifier = Modifier,
    linked: Boolean = true
) {
    val colors = MungoTheme.colors
    val (value, unit) = minutes.toIntervalLabel()
    HomeCard(modifier = modifier, linked = linked) {
        CardLabel("휴식 추천 간격", linked = linked)
        Spacer(Modifier.height(23.dp))
        BigValue(value = value, unit = unit, linked = linked)
        Spacer(Modifier.weight(1f))
        reason?.let {
            val tint = if (linked) colors.textAccent else colors.textTertiary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 107.dp)
                    .background(
                        if (linked) colors.alertSurface() else offTagColor(),
                        RoundedCornerShape(MungoRadius.md)
                    )
                    .padding(start = 23.dp, end = 16.dp, top = 26.dp, bottom = 20.dp)
            ) {
                MungoIconView(
                    MungoIcon.TAG,
                    tint = tint,
                    size = 22.dp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(text = it, style = Mg.bold(20, 32), color = tint)
            }
        }
    }
}

@Composable
private fun CardLabel(title: String, linked: Boolean = true) {
    val colors = MungoTheme.colors
    Text(
        text = title,
        style = Mg.regular(21, 25),
        color = if (linked) colors.textSecondary else colors.textTertiary
    )
}

@Composable
private fun BigValue(value: String, unit: String, linked: Boolean = true) {
    val colors = MungoTheme.colors
    val color = if (linked) colors.textPrimary else colors.textTertiary
    Row(verticalAlignment = Alignment.Bottom) {
        Text(text = value, style = Mg.bold(67, 80), color = color)
        Spacer(Modifier.width(8.dp))
        Text(
            text = unit,
            style = Mg.regular(27, 32),
            color = color,
            modifier = Modifier.padding(bottom = 8.dp)
        )
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
    linked: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(
                if (linked) MungoTheme.colors.bgSurface else offCardColor(),
                RoundedCornerShape(MungoRadius.xl)
            )
            .padding(start = 35.dp, end = 29.dp, top = 40.dp, bottom = 32.dp),
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

/** "시츄 · 10살 · 7kg" */
private fun Pet.detailLine(separator: String = " · "): String = listOfNotNull(
    breed.takeIf { it.isNotBlank() },
    ageYears?.let { "${it}살" },
    weightKg?.let { "${it.toKgLabel()}kg" }
).joinToString(separator)

/** 7.0 → "7", 4.2 → "4.2" */
private fun Float.toKgLabel(): String =
    if (this % 1f == 0f) roundToInt().toString() else "%.1f".format(this)

/** 온도 위험군(단두종 · 노령견 · 어린 강아지)이거나 더위에 약하다고 고른 경우 */
private val Pet.isHeatRisk: Boolean
    get() = temperatureStandard.group == TemperatureGroup.RISK ||
        PetTrait.HEAT_SENSITIVE in traits

/** 휴식 추천 꼬리표: 자주 쉬어야 하는 이유가 있을 때만 */
private fun Pet.restReason(): String? = when {
    isHeatRisk -> "더위에 약해 자주 쉬어 가요"
    PetTrait.MOTION_SICK in traits -> "멀미가 있어 자주 쉬어 가요"
    isSenior -> "나이가 많아 자주 쉬어 가요"
    else -> null
}

/** 120 → ("2", "시간마다"), 90 → ("1.5", "시간마다"), 30 → ("30", "분마다") */
private fun Int.toIntervalLabel(): Pair<String, String> = when {
    this < 60 -> "$this" to "분마다"
    this % 60 == 0 -> "${this / 60}" to "시간마다"
    else -> "%.1f".format(this / 60f) to "시간마다"
}

/** 오늘 · 어제 · 3일 전 · (일주일 넘으면) 11월 3일 */
private fun Long.toRelativeDay(now: Long = System.currentTimeMillis()): String {
    val zone = java.time.ZoneId.systemDefault()
    val day = java.time.Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
    val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return when (val days = java.time.temporal.ChronoUnit.DAYS.between(day, today)) {
        0L -> "오늘"
        1L -> "어제"
        in 2L..6L -> "${days}일 전"
        else -> "${day.monthValue}월 ${day.dayOfMonth}일"
    }
}

private val DATE_FORMAT = SimpleDateFormat("M월 d일 EEEE", Locale.KOREA)
private val BAR_MIN_HEIGHT = 11.dp
private val BAR_MAX_HEIGHT = 75.dp
private val BAR_RADIUS = 6.dp
private val PET_PANEL_WIDTH = 464.dp
private const val HERO_WEIGHT = 655f / 428f
private val AVATAR_SIZE = 102.dp
private val RING_SIZE = 200.dp
private val RING_STROKE = 20.dp
private val WEEK_DAYS = listOf("월", "화", "수", "목", "금", "토", "일")

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
    isRearTempWatchEnabled = true,
    vehicleName = "그랜저"
)

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
internal fun HomeScreenPreview() {
    MungoTheme(darkTheme = false) { HomeScreen(previewState(), {}, {}) }
}

@Preview(widthDp = 1700, heightDp = 1184, name = "최근 이동 있음")
@Composable
internal fun HomeScreenWithTripPreview() {
    MungoTheme(darkTheme = false) {
        HomeScreen(
            previewState().copy(
                lastTrip = previewTrip(),
                weekTripCounts = listOf(1, 0, 2, 0, 1, 3, 0)
            ),
            {},
            {}
        )
    }
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
                lastTrip = previewTrip(),
                weekTripCounts = listOf(1, 0, 2, 0, 1, 3, 0)
            ),
            {},
            {}
        )
    }
}
