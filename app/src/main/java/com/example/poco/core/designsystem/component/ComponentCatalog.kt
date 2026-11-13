package com.example.poco.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.poco.core.designsystem.PocoIcon
import com.example.poco.core.designsystem.PocoMark
import com.example.poco.core.designsystem.PocoWordmark
import com.example.poco.core.designsystem.ScoreRing
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.theme.PocoSpacing

/** Android Studio에서 이 파일을 열고 Split/Design 탭으로 DS-04 · DS-05 컴포넌트 전체를 확인 */
@Preview(widthDp = 1920, heightDp = 1600, name = "라이트")
@Composable
private fun ComponentCatalogPreview() {
    ComponentCatalog(darkTheme = false)
}

@Preview(widthDp = 1920, heightDp = 1600, name = "야간")
@Composable
private fun ComponentCatalogDarkPreview() {
    ComponentCatalog(darkTheme = true)
}

@Composable
private fun ComponentCatalog(darkTheme: Boolean) {
    POCOTheme(darkTheme = darkTheme) {
        Column(
            modifier = Modifier
                .background(POCOTheme.colors.backgroundDefault)
                .padding(PocoSpacing.space48),
            verticalArrangement = Arrangement.spacedBy(PocoSpacing.space32)
        ) {
            IconsAndControls()
            InputsAndChips()
            Feedback()
            ListsAndLogo()
        }
    }
}

@Composable
private fun IconsAndControls() {
    val colors = POCOTheme.colors
    Label("Icon · 타일 56")
    Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space12)) {
        PocoIcon.entries.forEach {
            IconBox(icon = it, background = colors.backgroundCard, tint = colors.textPrimary)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space48)) {
        Column(verticalArrangement = Arrangement.spacedBy(PocoSpacing.space12)) {
            Label("Button · 76 / 96")
            Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space16)) {
                PocoButton(text = "허용하고 시작", onClick = {})
                PocoButton(text = "나중에", onClick = {}, style = PocoButtonStyle.SECONDARY)
                PocoButton(text = "등록하기", onClick = {}, enabled = false)
            }
            PocoButton(text = "저장하고 홈으로", onClick = {}, size = PocoButtonSize.LARGE)
        }
        Column(verticalArrangement = Arrangement.spacedBy(PocoSpacing.space12)) {
            Label("Toggle 96×56 · Segment 80")
            Row(
                horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space16),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PocoToggle(checked = true, onCheckedChange = {})
                PocoToggle(checked = false, onCheckedChange = {})
                PocoSegment(
                    "보통",
                    selected = true,
                    onClick = {},
                    subtitle = "감점 ×1.0",
                    modifier = Modifier.width(200.dp)
                )
                PocoSegment(
                    "높음",
                    selected = false,
                    onClick = {},
                    subtitle = "감점 ×1.3",
                    modifier = Modifier.width(200.dp)
                )
            }
        }
    }
}

@Composable
private fun InputsAndChips() {
    Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space48)) {
        Column(verticalArrangement = Arrangement.spacedBy(PocoSpacing.space12)) {
            Label("Input · 76 · 포커스 3 + 링 6")
            Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space16)) {
                PocoInput(
                    label = "이름",
                    value = "",
                    onValueChange = {},
                    placeholder = "예: 현댕이",
                    helper = "한글·영문 최대 10자",
                    modifier = Modifier.width(280.dp)
                )
                PocoInput(
                    label = "몸무게",
                    value = "4.2",
                    onValueChange = {},
                    unit = "kg",
                    inlineUnit = true,
                    trailing = { PocoTag(text = "소형", style = PocoTagStyle.SUCCESS) },
                    modifier = Modifier.width(280.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(PocoSpacing.space12)) {
            Label("Chip · Tag · Badge")
            Row(
                horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space12),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PocoSoftChip("멀미 민감도 보통")
                PocoChip("뒷좌석 오른쪽")
                PocoStatusChip("주행 중")
                PocoTag("필수 권한", PocoTagStyle.WARN)
                PocoTag("선택 권한", PocoTagStyle.SUCCESS)
                PocoTag("NEW", PocoTagStyle.NEW)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space12)) {
                StatusCircle(StatusTone.OK)
                StatusCircle(StatusTone.HOT)
                StatusCircle(StatusTone.COLD)
                PocoChip("처음 한 번만 확인해요", icon = PocoIcon.SHIELD)
            }
        }
    }
}

@Composable
private fun Feedback() {
    val colors = POCOTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space48)) {
        Column(
            modifier = Modifier.width(700.dp),
            verticalArrangement = Arrangement.spacedBy(PocoSpacing.space12)
        ) {
            Label("Info row · 92")
            InfoRow(PocoIcon.THERMOMETER, "뒷좌석 온도", "23°C", "쾌적해요", StatusTone.OK)
            InfoRow(PocoIcon.THERMOMETER, "뒷좌석 온도", "27°C", "조금 더워요", StatusTone.HOT)
            InfoRow(PocoIcon.THERMOMETER, "뒷좌석 온도", "17°C", "조금 추워요", StatusTone.COLD)
            Label("Voice card · 116")
            VoiceCard(message = "커브가 이어져요, 천천히 가요", isVoiceOn = true, onTurnOn = {})
            VoiceCard(message = null, isVoiceOn = false, onTurnOn = {})
        }
        Column(
            modifier = Modifier.width(700.dp),
            verticalArrangement = Arrangement.spacedBy(PocoSpacing.space12)
        ) {
            Label("Alert card · 232")
            AlertCard(
                icon = PocoIcon.THERMOMETER,
                eyebrow = "뒷좌석 온도 알림",
                title = "뒷좌석이 27도예요",
                description = "현댕이에게 조금 더워요 · 알맞은 온도 20~24°C",
                primaryText = "22°C로 낮추기",
                onPrimary = {},
                secondaryText = "괜찮아요",
                onSecondary = {}
            )
            Label("Score ring 220 · Stat tile")
            Row(
                horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space24),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreRing(
                    score = 82,
                    modifier = Modifier.size(220.dp),
                    strokeWidth = 22.dp,
                    scoreStyle = POCOTheme.typography.displayL,
                    label = "컴포트 점수",
                    progressColor = colors.logoColor
                )
                Column(verticalArrangement = Arrangement.spacedBy(PocoSpacing.space12)) {
                    StatTile("급제동", "3회", colors.tangerine, Modifier.width(180.dp))
                    StatTile("평온", "31분", colors.successAccent, Modifier.width(180.dp))
                }
            }
        }
    }
}

@Composable
private fun ListsAndLogo() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space48),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.width(960.dp),
            verticalArrangement = Arrangement.spacedBy(PocoSpacing.space12)
        ) {
            Label("Permission banner · 960")
            PermissionBanner(
                title = "창문 정보는 꼭 필요해요",
                description = "이 항목을 켜야 mungo를 시작할 수 있어요.",
                onConfirm = {}
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(PocoSpacing.space12)) {
            Label("List · Feedback")
            PetListItem(
                name = "현댕이",
                summary = "몰티즈 · 소형",
                selected = true,
                onClick = {},
                modifier = Modifier.width(560.dp)
            )
            PocoToast("보리를 동승자로 등록했어요")
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space48),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Label("Logo · StatusBar")
        PocoWordmark(height = 56.dp)
        PocoMark(modifier = Modifier.size(96.dp))
        PocoStatusBar(gearLabel = "정차 중 · P", modifier = Modifier.width(900.dp))
    }
}

@Composable
private fun Label(text: String) {
    Text(text = "❖ $text", style = POCOTheme.typography.micro, color = Color(0xFF8B5CF6))
}
