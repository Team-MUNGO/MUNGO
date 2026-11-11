package com.example.poco.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.designsystem.CarSeatMap
import com.example.poco.core.designsystem.PetPhoto
import com.example.poco.core.designsystem.PocoIcon
import com.example.poco.core.designsystem.PocoIconView
import com.example.poco.core.designsystem.ScoreRing
import com.example.poco.core.designsystem.ShakeTimeline
import com.example.poco.core.designsystem.component.CheckRow
import com.example.poco.core.designsystem.component.CheckState
import com.example.poco.core.designsystem.component.IconBox
import com.example.poco.core.designsystem.component.PocoButton
import com.example.poco.core.designsystem.component.PocoSoftChip
import com.example.poco.core.designsystem.component.PocoStatusBar
import com.example.poco.core.designsystem.component.PocoToggle
import com.example.poco.core.designsystem.hasFullPetImage
import com.example.poco.core.designsystem.previewTrip
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.theme.PocoLayout
import com.example.poco.core.theme.PocoRadius
import com.example.poco.core.theme.PocoSize
import com.example.poco.core.theme.PocoSpacing
import com.example.poco.core.theme.PocoStroke
import com.example.poco.core.util.ege
import com.example.poco.core.util.toClock
import com.example.poco.core.util.toMonthDay
import com.example.poco.data.model.MotionSensitivity
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.data.model.TripSummary
import com.example.poco.presentation.petprofile.label
import kotlin.math.roundToInt

@Composable
fun HomeRoute(
    navigateToPetProfile: () -> Unit,
    navigateToDriving: () -> Unit,
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

    // 자리 바꾸기 모달 (펫이 없으면 펫 프로필에서 먼저 추가)
    var showSeatDialog by rememberSaveable { mutableStateOf(false) }
    uiState.pet?.takeIf { showSeatDialog }?.let { pet ->
        SeatChangeDialog(
            pet = pet,
            onDismiss = { showSeatDialog = false },
            onConfirm = { seat ->
                viewModel.changeSeat(seat)
                showSeatDialog = false
            }
        )
    }

    HomeScreen(
        uiState = uiState,
        // 톱니(펫 프로필 설정) → 펫 프로필
        onOpenSettings = navigateToPetProfile,
        onEditPet = navigateToPetProfile,
        onChangeSeat = {
            if (uiState.pet != null) {
                showSeatDialog = true
            } else {
                viewModel.onSeatChangeWithoutPet()
                navigateToPetProfile()
            }
        },
        onRecordMeal = viewModel::recordMealNow,
        onVoiceGuideChange = viewModel::setVoiceGuideEnabled,
        onStartDrive = navigateToDriving
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onOpenSettings: () -> Unit,
    onEditPet: () -> Unit,
    onChangeSeat: () -> Unit,
    onRecordMeal: () -> Unit,
    onVoiceGuideChange: (Boolean) -> Unit,
    onStartDrive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(POCOTheme.colors.backgroundDefault)
            .safeDrawingPadding()
            .padding(horizontal = PocoLayout.screenHorizontal)
    ) {
        PocoStatusBar(
            gearLabel = when (uiState.isParked) {
                true -> "정차 중 · P"
                false -> "주행 중 · D"
                null -> null
            }
        )
        Spacer(Modifier.height(PocoLayout.screenTop))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = PocoLayout.screenBottom),
            horizontalArrangement = Arrangement.spacedBy(PocoLayout.columnGap)
        ) {
            CompanionCard(
                uiState = uiState,
                onOpenSettings = onOpenSettings,
                onEditPet = onEditPet,
                onChangeSeat = onChangeSeat,
                onRecordMeal = onRecordMeal,
                modifier = Modifier
                    .weight(PocoLayout.HOME_COLUMN_LEFT)
                    .fillMaxHeight()
            )
            TripCard(
                trip = uiState.lastTrip,
                isVoiceGuideEnabled = uiState.isVoiceGuideEnabled,
                onVoiceGuideChange = onVoiceGuideChange,
                modifier = Modifier
                    .weight(PocoLayout.HOME_COLUMN_MIDDLE)
                    .fillMaxHeight()
            )
            StartDriveCard(
                onClick = onStartDrive,
                modifier = Modifier
                    .weight(PocoLayout.HOME_COLUMN_RIGHT)
                    .fillMaxHeight()
            )
        }
    }
}

// region 왼쪽: 오늘의 동승자 · 출발 체크 · 탑승 위치

@Composable
private fun CompanionCard(
    uiState: HomeUiState,
    onOpenSettings: () -> Unit,
    onEditPet: () -> Unit,
    onChangeSeat: () -> Unit,
    onRecordMeal: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 펫 · 출발 체크 · 탑승 위치를 카드 높이에 맞춰 고르게 나눈다 (작은 창에서는 스크롤)
    BoxWithConstraints(modifier = modifier) {
        val cardHeight = maxHeight
        HomeCard(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = cardHeight),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column { PetSummary(uiState, onOpenSettings, onEditPet) }
            Column { DepartureChecks(uiState, onRecordMeal) }
            Column {
                SectionDivider()
                SeatSection(pet = uiState.pet, onChangeSeat = onChangeSeat)
            }
        }
    }
}

@Composable
private fun PetSummary(uiState: HomeUiState, onOpenSettings: () -> Unit, onEditPet: () -> Unit) {
    val colors = POCOTheme.colors
    val pet = uiState.pet
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardLabel("오늘 함께 타는 펫")
            Spacer(Modifier.weight(1f))
            PocoIconView(
                PocoIcon.SETTINGS,
                tint = colors.textSecondary,
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onOpenSettings)
                    .padding(PocoSpacing.space2)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(PocoSize.avatarHome)
                    .background(colors.fieldBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PetPhoto(
                    photoPath = pet?.photoPath,
                    breed = pet?.breed,
                    modifier = Modifier.size(
                        if (hasFullPetImage(pet?.photoPath, pet?.breed)) {
                            80.dp
                        } else {
                            62.dp
                        }
                    )
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                if (pet == null) {
                    Text(
                        text = "펫을 추가해 주세요",
                        style = POCOTheme.typography.hmiTitle,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    PocoButton(text = "펫 추가하기", onClick = onEditPet)
                } else {
                    Text(
                        text = pet.name,
                        style = POCOTheme.typography.hmiHeading1,
                        color = colors.textPrimary
                    )
                    Text(
                        text = pet.detailLine(),
                        style = POCOTheme.typography.hmiCaption,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    LimeChip("멀미 민감도 ${pet.motionSensitivity.label()}")
                }
            }
        }
    }
}

@Composable
private fun DepartureChecks(uiState: HomeUiState, onRecordMeal: () -> Unit) {
    val pet = uiState.pet
    val petName = pet?.name ?: "반려견"
    Column {
        SectionDivider()
        CardLabel("출발 체크")
        CheckRow(
            icon = PocoIcon.CLOCK,
            title = mealTitle(uiState.minutesSinceMeal),
            description = mealDescription(uiState.mealStatus),
            state = uiState.mealStatus.toCheckState(),
            onClick = onRecordMeal
        )
        CheckRow(
            icon = PocoIcon.THERMOMETER,
            title = "${pet?.seat?.shortLabel() ?: "뒷좌석"} " +
                (uiState.seatTemperature?.let { "${it.roundToInt()}°C" } ?: "온도 확인 중"),
            description = when (uiState.temperatureStatus) {
                CheckStatus.OK -> "${petName.ege} 알맞은 온도예요"
                CheckStatus.WARNING -> "${petName.ege} 조금 덥거나 추워요"
                CheckStatus.UNKNOWN -> "차량 공조 정보를 기다리고 있어요"
            },
            state = uiState.temperatureStatus.toCheckState()
        )
        CheckRow(
            icon = PocoIcon.WINDOW,
            title = when (uiState.isAllWindowsClosed) {
                true -> "창문 모두 닫힘"
                false -> "열린 창문이 있어요"
                null -> "창문 확인 중"
            },
            description = when (uiState.windowStatus) {
                CheckStatus.OK -> "$petName 옆 창문까지 확인했어요"
                CheckStatus.WARNING -> "출발 전에 창문을 닫아 주세요"
                CheckStatus.UNKNOWN -> "차량 창문 정보를 기다리고 있어요"
            },
            state = uiState.windowStatus.toCheckState()
        )
    }
}

@Composable
private fun SeatSection(pet: Pet?, onChangeSeat: () -> Unit) {
    val colors = POCOTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        CarSeatMap(
            petSeat = pet?.seat,
            photoPath = pet?.photoPath,
            breed = pet?.breed,
            width = 120.dp
        )
        Spacer(Modifier.width(20.dp))
        Column {
            CardLabel("탑승 위치")
            Text(
                text = pet?.seat?.label() ?: "자리를 정해 주세요",
                style = if (pet !=
                    null
                ) {
                    POCOTheme.typography.hmiHeading1
                } else {
                    POCOTheme.typography.hmiTitle
                },
                color = colors.textPrimary
            )
            if (pet != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(colors.logoColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PocoIconView(
                            PocoIcon.CHECK,
                            tint = colors.logoBackground,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "자리에 잘 앉아 있어요",
                        style = POCOTheme.typography.hmiCaption,
                        color = colors.textPrimary
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "창문 알림은 이 자리 옆\n창문을 기준으로 해요",
                style = POCOTheme.typography.hmiCaption,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .border(
                        PocoStroke.control,
                        colors.borderStrong,
                        RoundedCornerShape(PocoRadius.r12)
                    )
                    .clickable(onClick = onChangeSeat)
                    .heightIn(min = PocoSize.touchMin).padding(horizontal = PocoSpacing.space24),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "자리 바꾸기",
                    style = POCOTheme.typography.hmiCaption,
                    color = colors.textPrimary
                )
                Spacer(Modifier.width(6.dp))
                Text(text = "›", style = POCOTheme.typography.hmiBody, color = colors.textPrimary)
            }
        }
    }
}

// endregion

// region 가운데: 지난 이동 · 이번 이동 설정

@Composable
private fun TripCard(
    trip: TripSummary?,
    isVoiceGuideEnabled: Boolean,
    onVoiceGuideChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    HomeCard(modifier = modifier) {
        Row {
            CardLabel("지난 이동")
            Spacer(Modifier.weight(1f))
            trip?.let {
                CardLabel(
                    listOfNotNull(
                        it.startedAtMillis.toMonthDay(),
                        it.destination
                    ).joinToString(" · ")
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "아직 함께한 이동이 없어요\n첫 이동을 마치면 여기에 기록돼요",
                    style = POCOTheme.typography.hmiCaption,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(
                    score = trip.comfortScore,
                    modifier = Modifier.size(PocoSize.scoreRingSmall),
                    strokeWidth = 22.dp,
                    scoreStyle = POCOTheme.typography.score,
                    label = "컴포트 점수",
                    progressColor = colors.logoColor
                )
                Spacer(Modifier.width(20.dp))
                Column {
                    Text(
                        text = trip.comfortScore.verdict(),
                        style = POCOTheme.typography.hmiTitle,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "급제동 ${trip.hardBrake.count}회 · 급회전 ${trip.sharpTurn.count}회",
                        style = POCOTheme.typography.hmiCaption,
                        color = colors.textSecondary
                    )
                    trip.scoreDelta?.takeIf { it != 0 }?.let { delta ->
                        Spacer(Modifier.height(8.dp))
                        LimeChip(
                            if (delta > 0) "▲ 지난번보다 ${delta}점 올랐어요" else "▼ 지난번보다 ${-delta}점 내려갔어요"
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            CardLabel("흔들림 타임라인")
            Spacer(Modifier.height(8.dp))
            val mid = (trip.startedAtMillis + trip.endedAtMillis) / 2
            ShakeTimeline(
                bars = trip.timeline,
                labels = listOf(
                    "${trip.startedAtMillis.toClock()} 출발",
                    mid.toClock(),
                    "${trip.endedAtMillis.toClock()} 도착"
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        SectionDivider()
        CardLabel("이번 이동 설정")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(icon = PocoIcon.SPEAKER, background = colors.fieldBackground, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "음성 안내",
                style = POCOTheme.typography.hmiBody,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            PocoToggle(checked = isVoiceGuideEnabled, onCheckedChange = onVoiceGuideChange)
        }
    }
}

// endregion

// region 오른쪽: 주행 시작

/** 화면에서 가장 큰 터치 영역 - 한 손, 한 번. 기어 D로도 자동 시작 */
@Composable
private fun StartDriveCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    Column(
        modifier = modifier
            .background(colors.logoBackground, RoundedCornerShape(PocoRadius.r28))
            .clickable(onClick = onClick)
            .padding(PocoSpacing.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(PocoSize.buttonHeightLarge)
                .background(colors.logoColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            PocoIconView(
                PocoIcon.PLAY,
                tint = colors.logoBackground,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "주행 시작",
            style = POCOTheme.typography.hmiHeading1,
            color = androidx.compose.ui.graphics.Color.White
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "기어 D 또는 내비 길 안내를 시작하면\n자동으로 시작돼요",
            style = POCOTheme.typography.hmiCaption,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

// endregion

// region 공통

@Composable
private fun HomeCard(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(POCOTheme.colors.backgroundCard, RoundedCornerShape(PocoRadius.r28))
            .padding(PocoSpacing.space32),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
private fun CardLabel(text: String) {
    Text(
        text = text,
        style = POCOTheme.typography.hmiCaption,
        color = POCOTheme.colors.textSecondary
    )
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = POCOTheme.colors.borderDefault)
    Spacer(Modifier.height(12.dp))
}

/** 연한 라임 바탕 칩 (멀미 민감도 · 점수 변화) */
@Composable
private fun LimeChip(text: String) {
    PocoSoftChip(text = text)
}

private fun CheckStatus.toCheckState(): CheckState = when (this) {
    CheckStatus.OK -> CheckState.OK
    CheckStatus.WARNING -> CheckState.WARNING
    CheckStatus.UNKNOWN -> CheckState.UNKNOWN
}

/** "몰티즈 · 3살 · 4.2kg" */
private fun Pet.detailLine(): String = listOfNotNull(
    breed.takeIf { it.isNotBlank() },
    ageYears?.let { "${it}살" },
    weightKg?.let { "${it}kg" }
).joinToString(" · ")

/** 체크 줄에서는 짧게: 뒷좌석 / 조수석 */
private fun PetSeat.shortLabel(): String = if (this == PetSeat.PASSENGER) "조수석" else "뒷좌석"

private fun Int.verdict(): String = when {
    this >= 90 -> "편안한 이동이었어요"
    this >= 70 -> "조금 흔들렸어요"
    else -> "많이 흔들렸어요"
}

private fun mealTitle(minutes: Long?): String = when {
    minutes == null -> "식사 시간 기록 없음"
    minutes < 1 -> "방금 식사했어요"
    minutes < 60 -> "마지막 식사 ${minutes}분 전"
    minutes % 60 == 0L -> "마지막 식사 ${minutes / 60}시간 전"
    else -> "마지막 식사 ${minutes / 60}시간 ${minutes % 60}분 전"
}

private fun mealDescription(status: CheckStatus): String = when (status) {
    CheckStatus.OK -> "공복 1~2시간, 출발하기 좋아요"
    CheckStatus.WARNING -> "식사 직후예요 · 1시간은 쉬었다 출발해요"
    CheckStatus.UNKNOWN -> "눌러서 방금 식사했다고 기록해요"
}

// endregion

private fun previewState() = HomeUiState(
    pet = Pet(
        id = 1,
        name = "현댕이",
        breed = "몰티즈",
        ageYears = 3,
        weightKg = 4.2f,
        seat = PetSeat.REAR_LEFT,
        motionSensitivity = MotionSensitivity.LOW
    ),
    isParked = true,
    minutesSinceMeal = 100,
    seatTemperature = 22f,
    isAllWindowsClosed = true,
    lastTrip = previewTrip().copy(comfortScore = 100, scoreDelta = 18)
)

@Preview(widthDp = 1920, heightDp = 1080)
@Composable
private fun HomeScreenPreview() {
    POCOTheme(darkTheme = false) {
        HomeScreen(previewState(), {}, {}, {}, {}, {}, {})
    }
}

@Preview(widthDp = 1920, heightDp = 1080, name = "다크 모드")
@Composable
private fun HomeScreenDarkPreview() {
    POCOTheme(darkTheme = true) {
        HomeScreen(previewState(), {}, {}, {}, {}, {}, {})
    }
}

@Preview(widthDp = 1920, heightDp = 1080, name = "첫 실행 (빈 상태)")
@Composable
private fun HomeScreenEmptyPreview() {
    POCOTheme(darkTheme = false) {
        HomeScreen(HomeUiState(), {}, {}, {}, {}, {}, {})
    }
}
