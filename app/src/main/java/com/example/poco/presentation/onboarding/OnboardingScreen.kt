package com.example.poco.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.poco.core.designsystem.MungoIcon
import com.example.poco.core.designsystem.MungoIconView
import com.example.poco.core.designsystem.MungoLogo
import com.example.poco.core.designsystem.component.PocoButton
import com.example.poco.core.designsystem.component.PocoButtonSize
import com.example.poco.core.theme.MungoLayout
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.theme.PocoSpacing
import com.example.poco.presentation.design.Mg

/** 온보딩 1페이지에서 소개하는 기능 */
private data class OnboardingFeature(
    val icon: MungoIcon,
    val title: String,
    val description: String
)

private val features = listOf(
    OnboardingFeature(
        icon = MungoIcon.WAVE,
        title = "부드러운 운전 코칭",
        description = "급제동 · 급회전을 줄이도록 미리 알려 드려요"
    ),
    OnboardingFeature(
        icon = MungoIcon.THERMOMETER,
        title = "뒷좌석 온도 · 창문 안내",
        description = "덥거나 춥거나 창문이 열리면 바로 알려요"
    ),
    OnboardingFeature(
        icon = MungoIcon.HOUSE,
        title = "컨디션 맞춤 휴식 제안",
        description = "멀미 · 쉬는 간격에 맞춰 쉬어 갈 때를 알려요"
    )
)

/**
 * 온보딩 1페이지: 앱 소개.
 * 시작하기 → 프로필 만들기 / 건너뛰기 → 권한 화면 (권한 동의는 건너뛸 수 없음)
 */
@Composable
fun OnboardingRoute(navigateToProfileSetup: () -> Unit, navigateToPermission: () -> Unit) {
    OnboardingScreen(onStart = navigateToProfileSetup, onSkip = navigateToPermission)
}

@Composable
private fun OnboardingScreen(
    onStart: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = POCOTheme.colors
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .safeDrawingPadding()
            .padding(MungoLayout.screenPadding)
    ) {
        IntroPanel(
            onStart = onStart,
            modifier = Modifier
                .width(560.dp)
                .fillMaxHeight()
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 76.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PocoSpacing.space24)
            ) {
                Text(
                    text = "건너뛰기",
                    style = POCOTheme.typography.title2.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(MungoRadius.sm))
                        .clickable(onClick = onSkip)
                        .heightIn(min = 64.dp)
                        .padding(horizontal = PocoSpacing.space16, vertical = PocoSpacing.space12)
                )
                features.forEachIndexed { index, feature ->
                    FeatureCard(number = index + 1, feature = feature)
                }
            }
        }
    }
}

/** 왼쪽 흰 패널: 로고 · 제목 · 설명 · 시작하기 */
@Composable
private fun IntroPanel(onStart: () -> Unit, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    Column(
        modifier = modifier
            .background(colors.bgSurface, RoundedCornerShape(MungoRadius.xl))
            .padding(horizontal = 56.dp, vertical = 56.dp)
    ) {
        MungoLogo(height = 44.dp)
        Spacer(Modifier.height(80.dp))
        Text(
            text = "반려동물과\n드라이브, 이제 더\n편안하게 떠나요",
            style = Mg.bold(60, 72),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(PocoSpacing.space32))
        Text(
            text = "운전 습관과 차내 환경을 살펴\n우리 아이 컨디션을 지켜 드려요.",
            style = Mg.regular(26, 36),
            color = colors.textSecondary
        )
        Spacer(Modifier.weight(1f))
        PocoButton(
            text = "시작하기",
            onClick = onStart,
            size = PocoButtonSize.LARGE,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        )
    }
}

/** 기능 소개 카드: 망고 아이콘 타일 · 제목 · 설명 · 번호(01) */
@Composable
private fun FeatureCard(number: Int, feature: OnboardingFeature, modifier: Modifier = Modifier) {
    val colors = POCOTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .background(colors.bgSurface, RoundedCornerShape(MungoRadius.lg))
            .padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(colors.bgAccentSubtle, RoundedCornerShape(MungoRadius.lg)),
            contentAlignment = Alignment.Center
        ) {
            MungoIconView(feature.icon, tint = colors.bgAccent, size = 40.dp)
        }
        Spacer(Modifier.width(PocoSpacing.space32))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature.title,
                style = POCOTheme.typography.title1,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(PocoSpacing.space8))
            Text(
                text = feature.description,
                style = Mg.regular(26, 36),
                color = colors.textSecondary
            )
        }
        Text(
            text = "%02d".format(number),
            style = POCOTheme.typography.title3.copy(fontWeight = FontWeight.Bold),
            color = colors.textDisabled,
            modifier = Modifier
                .align(Alignment.Top)
                .padding(top = 40.dp)
        )
    }
}

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun OnboardingScreenPreview() {
    POCOTheme(darkTheme = false) { OnboardingScreen(onStart = {}, onSkip = {}) }
}

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun OnboardingScreenDarkPreview() {
    POCOTheme(darkTheme = true) { OnboardingScreen(onStart = {}, onSkip = {}) }
}
