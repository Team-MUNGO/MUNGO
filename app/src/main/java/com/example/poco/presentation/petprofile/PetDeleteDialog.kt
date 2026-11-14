package com.example.poco.presentation.petprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.poco.core.theme.MungoTheme
import com.example.poco.core.util.eulReul
import com.example.poco.presentation.design.MgAlertDialog
import com.example.poco.presentation.design.MgDialogCard
import com.example.poco.presentation.design.MgIcons

/** 펫 삭제 확인. 지우면 되돌릴 수 없어서 한 번 더 묻는다 (공통 팝업 스타일 · 회색 뒷배경) */
@Composable
fun PetDeleteDialog(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    MgAlertDialog(
        title = "${name.eulReul} 삭제할까요?",
        message = DELETE_MESSAGE,
        icon = MgIcons.Close,
        dismissText = "취소",
        confirmText = "삭제",
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

private const val DELETE_MESSAGE = "프로필과 사진이 지워지고 되돌릴 수 없어요."

@Preview(widthDp = 1700, heightDp = 1184)
@Composable
private fun PetDeleteDialogPreview() {
    MungoTheme {
        MgDialogCard(
            title = "말티푸를 삭제할까요?",
            message = DELETE_MESSAGE,
            icon = MgIcons.Close,
            dismissText = "취소",
            confirmText = "삭제",
            onConfirm = {}
        )
    }
}
