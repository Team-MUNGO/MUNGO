package com.example.poco.presentation.boarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.component.pressable
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.MungoTheme
import com.example.poco.core.util.eunNeun
import com.example.poco.core.util.iGa
import com.example.poco.data.model.Pet
import com.example.poco.data.model.PetSeat
import com.example.poco.presentation.petprofile.label

/* 탑승 준비 2/3: 어디에 앉나요? (왼쪽 = 질문 · 고른 자리, 오른쪽 = 위에서 본 차) */

@Composable
internal fun ColumnScope.SeatLeft(uiState: BoardingUiState, actions: BoardingActions) {
    val name = (uiState.activePet ?: uiState.selectedPets.firstOrNull())?.name ?: "반려견"
    StepTitle(
        title = "${name.eunNeun}\n어디에 앉나요?",
        description = "${name.iGa} 앉은 좌석을 눌러 주세요.\n고른 좌석의 창문 열림과 온도를 지켜봐요."
    )
    Spacer(Modifier.height(108.dp)) // Figma 470:54563: '선택한 자리'가 1단계 '함께 타는 친구'와 같은 높이(678)
    SectionLabel("선택한 자리")
    val isMulti = uiState.selectedPets.size > 1
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        uiState.selectedPets.forEach { pet ->
            SelectedSeatRow(
                pet = pet,
                seat = uiState.seats[pet.id],
                isActive = isMulti && pet.id == uiState.activePetId,
                // 여러 마리면 줄을 눌러 자리를 고를 펫을 바꾼다
                onClick = if (isMulti) ({ actions.onSelectActivePet(pet.id) }) else null
            )
        }
    }
}

@Composable
private fun SelectedSeatRow(pet: Pet, seat: PetSeat?, isActive: Boolean, onClick: (() -> Unit)?) {
    val colors = MungoTheme.colors
    val shape = RoundedCornerShape(MungoRadius.lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .background(colors.bgSurfaceSunken, shape)
            .then(if (isActive) Modifier.border(3.dp, colors.borderAccent, shape) else Modifier)
            .pressable(shape, onClick = onClick)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(colors.bgSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(MungoIcon.SEAT, tint = colors.textAccent, size = 32.dp)
        }
        Spacer(Modifier.width(20.dp))
        Text(
            text = seat?.label() ?: "자리를 골라 주세요",
            style = MungoTheme.typography.title3,
            color = if (seat != null) colors.textPrimary else colors.textTertiary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = pet.name,
            style = MungoTheme.typography.title3,
            color = colors.textAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 위에서 본 차: 앞(▲) · 운전석 | 조수석 · 뒷좌석 왼쪽 | 오른쪽 */
@Composable
internal fun SeatMap(uiState: BoardingUiState, actions: BoardingActions) {
    val colors = MungoTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(CAR_RATIO)
        ) {
            val w = maxWidth
            val h = maxHeight
            val line = colors.borderDefault
            val arrow = colors.textTertiary
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bgSurface, RoundedCornerShape(45))
                    .border(2.dp, colors.borderDefault, RoundedCornerShape(45))
            )
            // 앞 표시 · 앞유리 · 뒷유리
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = size.width
                val ch = size.height
                val tri = Path().apply {
                    moveTo(cw / 2, ch * 0.055f)
                    lineTo(cw / 2 + cw * 0.037f, ch * 0.09f)
                    lineTo(cw / 2 - cw * 0.037f, ch * 0.09f)
                    close()
                }
                drawPath(tri, arrow)
                val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                drawPath(
                    Path().apply {
                        moveTo(cw * 0.16f, ch * 0.155f)
                        quadraticTo(cw / 2, ch * 0.105f, cw * 0.84f, ch * 0.155f)
                    },
                    line,
                    style = stroke
                )
                drawPath(
                    Path().apply {
                        moveTo(cw * 0.18f, ch * 0.86f)
                        quadraticTo(cw / 2, ch * 0.895f, cw * 0.82f, ch * 0.86f)
                    },
                    line,
                    style = stroke
                )
            }
            val frontW = w * 0.37f
            val frontH = h * 0.235f
            val rearW = w * 0.35f
            val rearH = h * 0.225f
            // 앞줄
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = h * 0.23f),
                horizontalArrangement = Arrangement.spacedBy(
                    w * 0.05f,
                    Alignment.CenterHorizontally
                )
            ) {
                DriverSeat(width = frontW, height = frontH)
                SeatCard(PetSeat.PASSENGER, uiState, actions, frontW, frontH)
            }
            // 뒷줄
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = h * 0.515f),
                horizontalArrangement = Arrangement.spacedBy(
                    w * 0.06f,
                    Alignment.CenterHorizontally
                )
            ) {
                SeatCard(PetSeat.REAR_LEFT, uiState, actions, rearW, rearH)
                SeatCard(PetSeat.REAR_RIGHT, uiState, actions, rearW, rearH)
            }
        }
        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MungoIconView(MungoIcon.INFO, tint = colors.textTertiary, size = 26.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "안전벨트는 꼭 채워주세요.",
                style = MungoTheme.typography.body2,
                color = colors.textTertiary
            )
        }
    }
}

@Composable
private fun DriverSeat(width: Dp, height: Dp) {
    val colors = MungoTheme.colors
    Column(
        modifier = Modifier
            .size(width, height)
            .background(colors.bgSurfaceSunken, RoundedCornerShape(MungoRadius.xl)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MungoIconView(MungoIcon.STEERING_WHEEL, tint = colors.textTertiary, size = 72.dp)
        Spacer(Modifier.height(20.dp))
        Text(text = "운전석", style = MungoTheme.typography.body2, color = colors.textSecondary)
    }
}

@Composable
private fun SeatCard(
    seat: PetSeat,
    uiState: BoardingUiState,
    actions: BoardingActions,
    width: Dp,
    height: Dp
) {
    val colors = MungoTheme.colors
    val shape = RoundedCornerShape(MungoRadius.xl)
    val ownerId = uiState.seats.entries.firstOrNull { it.value == seat }?.key
    val owner = uiState.selectedPets.firstOrNull { it.id == ownerId }
    val isActiveOwner = owner != null && owner.id == uiState.activePetId
    val (title, sub) = when (seat) {
        PetSeat.PASSENGER -> "조수석" to null
        PetSeat.REAR_LEFT -> "뒷좌석" to "왼쪽"
        PetSeat.REAR_RIGHT -> "뒷좌석" to "오른쪽"
    }
    Box(modifier = Modifier.size(width, height)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isActiveOwner) colors.bgAccentSubtle else colors.bgSurface, shape)
                .border(
                    width = if (isActiveOwner) 3.dp else 2.dp,
                    color = when {
                        isActiveOwner -> colors.borderAccent
                        owner != null -> colors.borderAccent.copy(alpha = 0.4f)
                        else -> colors.borderDefault
                    },
                    shape = shape
                )
                .pressable(shape, onClick = { actions.onSelectSeat(seat) }),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (owner != null) {
                PetCircle(pet = owner, size = width * 0.5f)
                Spacer(Modifier.height(16.dp))
            } else {
                MungoIconView(MungoIcon.SEAT, size = 56.dp)
                Spacer(Modifier.height(20.dp))
            }
            Text(
                text = if (owner != null) seat.label() else title,
                style = MungoTheme.typography.title3,
                color = colors.textPrimary
            )
            if (owner == null && sub != null) {
                Text(text = sub, style = MungoTheme.typography.body3, color = colors.textTertiary)
            }
        }
        if (isActiveOwner) {
            CheckBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 12.dp, y = (-12).dp)
            )
        }
    }
}

/** 차 가로 : 세로 */
private const val CAR_RATIO = 0.67f
