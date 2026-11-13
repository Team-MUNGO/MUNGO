package com.example.poco.presentation.boarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poco.presentation.design.Mg
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.component.pressable
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.util.ege
import com.example.poco.presentation.petprofile.label
import kotlin.math.roundToInt

/* 탑승 준비 3/3: 출발 전에 함께 확인해요 (필수 2 · 자동 2 · 선택 1) */

@Composable
internal fun ColumnScope.ChecklistLeft(uiState: BoardingUiState, actions: BoardingActions) {
    val colors = POCOTheme.colors
    StepTitle(title = "출발 전에\n함께 확인해요", description = "항목을 눌러서 체크해주세요.")
    Spacer(Modifier.height(92.dp)) // Figma 470:54652: 음성 안내 y 583
    VoiceRow(
        isListening = uiState.isListening,
        heardText = uiState.heardText,
        example = "mungo, 다 확인했어",
        onMic = actions.onMic
    )
    Spacer(Modifier.height(45.dp))
    HorizontalDivider(color = colors.borderDefault)
    Spacer(Modifier.height(50.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSurfaceSunken, RoundedCornerShape(MungoRadius.lg))
            .padding(28.dp)
    ) {
        Text(text = "확인한 항목", style = Mg.medium(24, 32), color = Color(0xFF9CA7B4))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = uiState.checkedCount.toString(),
                style = Mg.bold(64, 77),
                color = colors.textPrimary
            )
            Text(
                text = " / ${CheckItem.entries.size}",
                style = Mg.medium(40, 52),
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val left = uiState.requiredLeft
            MungoIconView(
                if (left > 0) MungoIcon.INFO else MungoIcon.CHECK,
                tint = if (left > 0) colors.textAccent else colors.statusCalm,
                size = 22.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (left > 0) "필수 체크 항목 ${left}개가 남았어요" else "필수 항목을 모두 확인했어요",
                style = Mg.semi(24, 32),
                color = if (left > 0) colors.textAccent else colors.textSecondary
            )
        }
    }
}

@Composable
internal fun Checklist(uiState: BoardingUiState, actions: BoardingActions) {
    val colors = POCOTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(19.dp)) { // Figma 488:7706: 139 높이 · 19 간격
            CheckItem.entries.forEach { item ->
                CheckRow(
                    item = item,
                    uiState = uiState,
                    onClick = { actions.onToggleCheck(item) }
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MungoIconView(MungoIcon.INFO, tint = colors.textTertiary, size = 24.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "‘자동’은 차량 데이터로 멍고가 먼저 확인한 항목이에요",
                style = Mg.regular(24, 32),
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun CheckRow(item: CheckItem, uiState: BoardingUiState, onClick: () -> Unit) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(MungoRadius.lg)
    val checked = uiState.isChecked(item)
    val auto = uiState.autoResult(item)
    val (title, description) = item.texts(uiState)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 139.dp)
            .background(colors.bgSurface, shape)
            .pressable(shape, enabled = auto != true, onClick = onClick)
            .padding(horizontal = 36.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckBox(checked)
        Spacer(Modifier.width(28.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = Mg.bold(26, 34),
                color = colors.textPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                style = POCOTheme.typography.caption1,
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.width(20.dp))
        when (item.kind) {
            CheckKind.REQUIRED -> Badge("필수", colors.bgAccentSubtle, colors.textAccent)
            CheckKind.OPTIONAL -> Badge("선택", colors.bgSurfaceSunken, colors.textPrimary)
            CheckKind.AUTO -> when (auto) {
                true -> Badge("자동", colors.bgSurfaceSunken, colors.textPrimary, withCheck = true)
                false -> Badge("확인 필요", colors.statusAlertSubtle, colors.statusAlert)
                null -> Badge("직접", colors.bgSurfaceSunken, colors.textPrimary)
            }
        }
    }
}

@Composable
private fun CheckBox(checked: Boolean) {
    val colors = POCOTheme.colors
    val shape = RoundedCornerShape(MungoRadius.sm)
    Box(
        modifier = Modifier
            .size(52.dp)
            .then(
                if (checked) {
                    Modifier.background(colors.bgAccent, shape)
                } else {
                    Modifier.border(3.dp, colors.borderStrong, shape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) MungoIconView(MungoIcon.CHECK, tint = colors.textOnAccent, size = 30.dp)
    }
}

@Composable
private fun Badge(text: String, background: Color, content: Color, withCheck: Boolean = false) {
    Row(
        modifier = Modifier
            .heightIn(min = 52.dp)
            .background(background, CircleShape)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (withCheck) {
            MungoIconView(MungoIcon.CHECK, tint = content, size = 20.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = Mg.bold(24, 32),
            color = content
        )
    }
}

/** 항목 제목 · 설명. 자동 항목은 차량에서 읽은 값에 따라 바뀐다 */
private fun CheckItem.texts(state: BoardingUiState): Pair<String, String> {
    val name = state.selectedPets.firstOrNull()?.name ?: "반려견"
    val auto = state.autoResult(this)
    val unknown = "차량 정보를 못 읽었어요 · 직접 확인해 주세요"
    return when (this) {
        CheckItem.CAR_SEAT -> "반려용 카시트가 고정됐나요?" to "좌석 벨트나 ISOFIX에 걸어 주세요"
        CheckItem.HARNESS -> "하네스에 안전벨트 리드줄을 연결했나요?" to "목줄이 아닌 하네스에 연결해 주세요"
        CheckItem.WINDOW -> {
            val seats = state.selectedIds.mapNotNull { state.seats[it] }.distinct()
                .joinToString("·") { it.label() }
                .ifEmpty { "반려견 옆" }
            "$seats 창문이 잘 잠겨있나요?" to when (auto) {
                true -> "차량에서 확인했어요"
                false -> "창문이 열려 있어요 · 닫고 다시 확인해 주세요"
                null -> unknown
            }
        }
        CheckItem.TEMPERATURE -> {
            val title = state.seatTemperature
                ?.let { "실내 온도가 ${it.roundToInt()}°C로 설정 되어 있나요?" }
                ?: "실내 온도가 알맞게 설정 되어 있나요?"
            val comfort = state.temperatureStandard.comfort
            val range = "${comfort.start.toCelsius()}~${comfort.endInclusive.toCelsius()}°C"
            title to when (auto) {
                true -> "${name.ege} 알맞은 온도예요"
                false -> "${name.ege} 조금 덥거나 추워요 · $range 추천"
                null -> unknown
            }
        }
        CheckItem.SUPPLIES -> "물·배변 봉투를 챙겼나요?" to "1시간 넘게 달린다면 챙겨 주세요"
    }
}
