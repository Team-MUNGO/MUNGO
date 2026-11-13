package com.example.poco.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.poco.core.designsystem.CarSeatMap
import com.example.poco.core.designsystem.MungoBaseIcon
import com.example.poco.core.designsystem.MungoBaseIconView
import com.example.poco.core.designsystem.component.MungoButton
import com.example.poco.core.designsystem.component.MungoButtonStyle
import com.example.poco.core.designsystem.component.MungoTag
import com.example.poco.core.designsystem.component.MungoTagStyle
import com.example.poco.core.theme.MungoBaseRadius
import com.example.poco.core.theme.MungoBaseSize
import com.example.poco.core.theme.MungoSpacing
import com.example.poco.core.theme.MungoStroke
import com.example.poco.core.theme.MungoTheme
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.presentation.petprofile.label

/**
 * 홈 "자리 바꾸기" 모달.
 * 고른 자리는 펫 프로필의 탑승 자리에도 그대로 저장된다 (창문 알림 기준 창문이 바뀜).
 */
@Composable
fun SeatChangeDialog(pet: Pet, onDismiss: () -> Unit, onConfirm: (PetSeat) -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SeatChangeContent(pet = pet, onDismiss = onDismiss, onConfirm = onConfirm)
    }
}

@Composable
private fun SeatChangeContent(pet: Pet, onDismiss: () -> Unit, onConfirm: (PetSeat) -> Unit) {
    val colors = MungoTheme.colors
    var selected by rememberSaveable { mutableStateOf(pet.seat) }
    Column(
        modifier = Modifier
            .widthIn(max = 1120.dp)
            .background(colors.backgroundCard, RoundedCornerShape(MungoBaseRadius.r36))
            .padding(MungoSpacing.space40),
        verticalArrangement = Arrangement.spacedBy(MungoSpacing.space24)
    ) {
        // 제목 · 닫기
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${pet.name} 자리 바꾸기",
                    style = MungoTheme.typography.headingL,
                    color = colors.textPrimary
                )
                Text(
                    text = "창문 알림이 새 자리 옆 창문을 기준으로 바뀌어요",
                    style = MungoTheme.typography.bodyM,
                    color = colors.textSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(MungoBaseSize.touchMin)
                    .background(colors.fieldBackground, CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                // 닫기(×) = 더하기 아이콘을 45° 회전
                MungoBaseIconView(
                    MungoBaseIcon.PLUS,
                    tint = colors.textPrimary,
                    modifier = Modifier
                        .size(MungoBaseSize.icon28)
                        .rotate(45f)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(MungoSpacing.space24)) {
            // 위에서 본 차
            Column(
                modifier = Modifier
                    .background(colors.fieldBackground, RoundedCornerShape(MungoBaseRadius.r24))
                    .padding(MungoSpacing.space24),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CarSeatMap(
                    petSeat = selected,
                    previousSeat = pet.seat,
                    showLabels = true,
                    photoPath = pet.photoPath,
                    breed = pet.breed,
                    width = 240.dp
                )
                Spacer(Modifier.height(MungoSpacing.space12))
                Text(
                    text = "▲ 앞쪽 · 초록 막대 = 알림 기준 창문",
                    style = MungoTheme.typography.micro,
                    color = colors.textSecondary
                )
            }

            // 자리 고르기
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MungoSpacing.space12)
            ) {
                PetSeat.entries.forEach { seat ->
                    SeatOption(
                        seat = seat,
                        isCurrent = seat == pet.seat,
                        selected = seat == selected,
                        onClick = { selected = seat }
                    )
                }
                Spacer(Modifier.height(MungoSpacing.space4))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MungoBaseIconView(
                        MungoBaseIcon.WARNING,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(MungoBaseSize.icon24)
                    )
                    Spacer(Modifier.width(MungoSpacing.space8))
                    Text(
                        text = "안고 타기는 도로교통법 위반이라 고를 수 없어요",
                        style = MungoTheme.typography.caption,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // 하단
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "펫 프로필의 탑승 자리도 같이 바뀌어요",
                style = MungoTheme.typography.caption,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            MungoButton(text = "취소", onClick = onDismiss, style = MungoButtonStyle.SECONDARY)
            Spacer(Modifier.width(MungoSpacing.space16))
            MungoButton(
                text = "이 자리로 바꾸기",
                onClick = { onConfirm(selected) },
                enabled = selected != pet.seat
            )
        }
    }
}

@Composable
private fun SeatOption(seat: PetSeat, isCurrent: Boolean, selected: Boolean, onClick: () -> Unit) {
    val colors = MungoTheme.colors
    val shape = RoundedCornerShape(MungoBaseRadius.r24)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.limeSoft else colors.backgroundCard, shape)
            .border(
                width = MungoStroke.control,
                color = if (selected) colors.accentFill else colors.borderDefault,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = MungoSpacing.space24, vertical = MungoSpacing.space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioMark(selected)
        Spacer(Modifier.width(MungoSpacing.space20))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = seat.label(),
                    style = MungoTheme.typography.titleM,
                    color = colors.textPrimary
                )
                Spacer(Modifier.width(MungoSpacing.space12))
                when {
                    isCurrent -> MungoTag(text = "지금 자리", style = MungoTagStyle.NEW)
                    seat == PetSeat.REAR_RIGHT -> MungoTag(
                        text = "추천",
                        style = MungoTagStyle.SUCCESS
                    )
                    seat == PetSeat.PASSENGER -> MungoTag(
                        text = "에어백 주의",
                        style = MungoTagStyle.WARN
                    )
                }
            }
            Text(
                text = seat.guide(),
                style = MungoTheme.typography.caption,
                color = colors.textSecondary
            )
        }
    }
}

/** 선택: 네이비 원 + 라임 점 / 비선택: 회색 테두리 */
@Composable
private fun RadioMark(selected: Boolean) {
    val colors = MungoTheme.colors
    Box(
        modifier = Modifier
            .size(32.dp)
            .then(
                if (selected) {
                    Modifier.background(colors.logoBackground, CircleShape)
                } else {
                    Modifier.border(MungoStroke.control, colors.borderStrong, CircleShape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(colors.logoColor, CircleShape)
            )
        }
    }
}

/** 자리별 안내 한 줄 */
private fun PetSeat.guide(): String = when (this) {
    PetSeat.REAR_LEFT -> "운전석 바로 뒤 · 왼쪽 뒤 창문 기준"
    PetSeat.REAR_RIGHT -> "인도 쪽 문이라 타고 내리기 안전해요"
    PetSeat.PASSENGER -> "에어백이 터지면 다칠 수 있어요"
}

/** 확인 토스트 문구. 예) 현댕이 자리를 뒷좌석 오른쪽으로 바꿨어요 (자리 이름이 모두 받침으로 끝나 "으로") */
fun seatChangedMessage(petName: String, seat: PetSeat): String =
    "$petName 자리를 ${seat.label()}으로 바꿨어요"

@Preview(widthDp = 1280, heightDp = 760)
@Composable
private fun SeatChangePreview() {
    MungoTheme(darkTheme = false) {
        SeatChangeContent(
            pet = Pet(
                id = 1,
                name = "현댕이",
                breed = "몰티즈",
                ageYears = 3,
                weightKg = 4.2f,
                seat = PetSeat.REAR_LEFT,
                motionSensitivity = com.example.poco.data.model.MotionSensitivity.NORMAL
            ),
            onDismiss = {},
            onConfirm = {}
        )
    }
}
