package com.example.poco.presentation.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** mungo 팝업 카드 크기 · 모양 (Figma 464:46215 창문 경고 팝업 기준) */
object MgDialogStyle {
    val Width = 760.dp
    val Shape = RoundedCornerShape(32.dp)
    val Padding = 40.dp
    val IconSize = 92.dp
    val ButtonHeight = 96.dp
}

/**
 * 모든 확인 팝업의 공통 모양: 흰 카드(760 · 라운드 32) 가운데 정렬로
 * 주황 원 아이콘 → 굵은 제목 → 회색 설명 → [보조 버튼 | 주황 주 버튼].
 * [dismissText]가 없으면 주 버튼 하나만 꽉 차게. [extra]에는 설명 아래 추가 내용(목록 · 입력칸 등).
 */
@Composable
fun MgDialogCard(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    dismissText: String? = null,
    onDismiss: () -> Unit = {},
    confirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
    width: Dp = MgDialogStyle.Width,
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column(
        modifier
            .width(width)
            .background(Color.White, MgDialogStyle.Shape)
            .padding(MgDialogStyle.Padding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null || iconRes != null) {
            IconBadge(
                icon = icon,
                res = iconRes,
                size = MgDialogStyle.IconSize,
                background = Mg.Orange,
                // 창문 경고 팝업처럼 주황 원 위 흰 그림 (그림 파일 아이콘은 자체 색을 쓴다)
                tint = if (icon != null) Color.White else Mg.Ink
            )
            Spacer(Modifier.height(26.dp))
        }
        Text(title, style = Mg.bold(30, 38), color = Mg.Ink, textAlign = TextAlign.Center)
        message?.let {
            Spacer(Modifier.height(18.dp))
            Text(
                it,
                style = Mg.Body.copy(lineHeight = Mg.Body.fontSize * 1.55f),
                color = Mg.Muted,
                textAlign = TextAlign.Center
            )
        }
        extra?.let {
            Spacer(Modifier.height(24.dp))
            it()
        }
        Spacer(Modifier.height(36.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (dismissText != null) {
                SecondaryButton(
                    dismissText,
                    onDismiss,
                    Modifier.weight(240f).height(MgDialogStyle.ButtonHeight),
                    enabled = dismissEnabled,
                    style = Mg.semi(24, 31)
                )
            }
            PrimaryButton(
                confirmText,
                onConfirm,
                Modifier.weight(424f).height(MgDialogStyle.ButtonHeight),
                enabled = confirmEnabled,
                icon = null
            )
        }
    }
}

/** 뒷배경을 회색(dim)으로 덮고 [MgDialogCard]를 띄운다. 바깥을 누르면 [onDismiss] */
@Composable
fun MgAlertDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    dismissText: String? = null,
    confirmEnabled: Boolean = true,
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    ScrimDialog(onDismiss = onDismiss) {
        MgDialogCard(
            title = title,
            confirmText = confirmText,
            onConfirm = onConfirm,
            message = message,
            icon = icon,
            iconRes = iconRes,
            dismissText = dismissText,
            onDismiss = onDismiss,
            confirmEnabled = confirmEnabled,
            extra = extra
        )
    }
}
