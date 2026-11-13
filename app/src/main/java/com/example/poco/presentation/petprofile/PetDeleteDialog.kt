package com.example.poco.presentation.petprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.poco.core.designsystem.component.MungoButton
import com.example.poco.core.designsystem.component.MungoButtonStyle
import com.example.poco.core.theme.MungoRadius
import com.example.poco.core.theme.MungoTheme
import com.example.poco.core.util.eulReul

/** 펫 삭제 확인. 지우면 되돌릴 수 없어서 한 번 더 묻는다 */
@Composable
fun PetDeleteDialog(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PetDeleteContent(name = name, onDismiss = onDismiss, onConfirm = onConfirm)
    }
}

@Composable
private fun PetDeleteContent(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val colors = MungoTheme.colors
    Column(
        modifier = Modifier
            .width(720.dp)
            .background(colors.bgSurface, RoundedCornerShape(MungoRadius.xl))
            .padding(48.dp)
    ) {
        Text(
            text = "${name.eulReul} 삭제할까요?",
            style = MungoTheme.typography.h3,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "프로필과 사진이 지워지고 되돌릴 수 없어요.",
            style = MungoTheme.typography.body2,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MungoButton(
                text = "취소",
                onClick = onDismiss,
                style = MungoButtonStyle.SECONDARY,
                modifier = Modifier.weight(1f)
            )
            MungoButton(
                text = "삭제",
                onClick = onConfirm,
                style = MungoButtonStyle.NEUTRAL_STRONG,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun PetDeleteDialogPreview() {
    MungoTheme { PetDeleteContent(name = "현댕이", onDismiss = {}, onConfirm = {}) }
}
