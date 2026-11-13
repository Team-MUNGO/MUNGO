package com.example.poco.presentation.permission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.poco.core.designsystem.MungoBaseIcon
import com.example.poco.core.designsystem.MungoBaseIconView
import com.example.poco.core.designsystem.MungoMark
import com.example.poco.core.designsystem.MungoWordmark
import com.example.poco.core.designsystem.component.MungoButton
import com.example.poco.core.designsystem.component.MungoButtonStyle
import com.example.poco.core.designsystem.component.MungoChip
import com.example.poco.core.designsystem.component.PermissionBanner
import com.example.poco.core.designsystem.component.PermissionCard
import com.example.poco.core.theme.MungoBaseLayout
import com.example.poco.core.theme.MungoBaseRadius
import com.example.poco.core.theme.MungoBaseSize
import com.example.poco.core.theme.MungoSpacing
import com.example.poco.core.theme.MungoTheme
import com.example.poco.core.util.eulReul

@Composable
fun PermissionRoute(navigateNext: () -> Unit, viewModel: PermissionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> viewModel.complete(result) }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) navigateNext()
    }

    PermissionScreen(
        uiState = uiState,
        onToggle = viewModel::toggle,
        onDismissBanner = viewModel::dismissRequiredBanner,
        onLater = { viewModel.complete() },
        onAllow = {
            val permissions = uiState.enabledGroups.flatMap { it.permissions }
            if (permissions.isEmpty()) {
                viewModel.complete()
            } else {
                permissionLauncher.launch(permissions.toTypedArray())
            }
        }
    )
}

@Composable
private fun PermissionScreen(
    uiState: PermissionUiState,
    onToggle: (PermissionGroup) -> Unit,
    onDismissBanner: () -> Unit,
    onLater: () -> Unit,
    onAllow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MungoTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundDefault)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = MungoBaseLayout.screenHorizontal,
                    vertical = MungoSpacing.space48
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MungoWordmark(height = 40.dp)
                Spacer(Modifier.weight(1f))
                MungoChip(text = "처음 한 번만 확인해요", icon = MungoBaseIcon.SHIELD)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(MungoBaseSize.avatarHome * 1.25f)
                            .background(
                                colors.backgroundCard,
                                RoundedCornerShape(MungoBaseRadius.r36)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        MungoMark(modifier = Modifier.size(MungoBaseSize.avatarHome))
                    }
                    Spacer(Modifier.height(MungoSpacing.space40))
                    Text(
                        text = "${uiState.petName.eulReul} 지켜보려면\n차량 정보가 필요해요",
                        style = MungoTheme.typography.headingXl,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(MungoSpacing.space20))
                    Text(
                        text = "주행 중에는 화면 대신 음성으로 알려드려요.\n언제든 설정에서 바꿀 수 있어요.",
                        style = MungoTheme.typography.bodyM,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(MungoSpacing.space24))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MungoBaseIconView(
                            MungoBaseIcon.LOCK,
                            tint = colors.safe,
                            modifier = Modifier.size(MungoBaseSize.icon24)
                        )
                        Spacer(Modifier.width(MungoSpacing.space8))
                        Text(
                            text = "정보는 이 차 안에서만 쓰여요",
                            style = MungoTheme.typography.bodyM,
                            color = colors.textPrimary
                        )
                    }
                }

                Spacer(Modifier.width(MungoSpacing.space48))

                Column(
                    modifier = Modifier.weight(1.3f),
                    verticalArrangement = Arrangement.spacedBy(MungoSpacing.space12)
                ) {
                    PermissionGroup.entries.forEach { group ->
                        PermissionCard(
                            icon = group.icon,
                            title = group.title,
                            description = group.descriptionFor(uiState.petName),
                            checked = group in uiState.enabledGroups,
                            onCheckedChange = { onToggle(group) }
                        )
                    }
                    Spacer(Modifier.height(MungoSpacing.space12))
                    Row(horizontalArrangement = Arrangement.spacedBy(MungoSpacing.space16)) {
                        MungoButton(
                            text = "나중에",
                            onClick = onLater,
                            style = MungoButtonStyle.SECONDARY,
                            modifier = Modifier.weight(1f)
                        )
                        MungoButton(
                            text = "허용하고 시작",
                            onClick = onAllow,
                            modifier = Modifier.weight(1.7f)
                        )
                    }
                }
            }
        }

        // 필수 권한 배너: 위쪽 가운데 오버레이 · [확인] → 권한 설정으로 복귀
        uiState.requiredBanner?.let { group ->
            PermissionBanner(
                title = group.requiredTitle,
                description = "이 항목을 켜야 mungo를 시작할 수 있어요.",
                onConfirm = onDismissBanner,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = MungoSpacing.space48)
            )
        }
    }
}

@Preview(widthDp = 1920, heightDp = 1080)
@Composable
private fun PermissionScreenPreview() {
    MungoTheme(darkTheme = false) {
        PermissionScreen(PermissionUiState(), {}, {}, {}, {})
    }
}

@Preview(widthDp = 1920, heightDp = 1080, name = "필수 권한 배너")
@Composable
private fun PermissionScreenBannerPreview() {
    MungoTheme(darkTheme = false) {
        PermissionScreen(PermissionUiState(requiredBanner = PermissionGroup.WINDOW), {}, {}, {}, {})
    }
}

@Preview(widthDp = 1920, heightDp = 1080, name = "다크 모드")
@Composable
private fun PermissionScreenDarkPreview() {
    MungoTheme(darkTheme = true) {
        PermissionScreen(PermissionUiState(), {}, {}, {}, {})
    }
}
