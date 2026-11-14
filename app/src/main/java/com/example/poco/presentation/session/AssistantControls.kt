package com.example.poco.presentation.session

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.poco.R
import com.example.poco.core.theme.MungoTheme
import com.example.poco.presentation.design.Mg
import com.example.poco.presentation.design.MgAlertDialog
import com.example.poco.presentation.design.MgIcons
import com.example.poco.presentation.session.component.DrivingInk
import com.example.poco.presentation.session.component.DrivingMuted

@Composable
internal fun AssistantControls(
    state: AssistantUiState,
    active: Boolean,
    blocked: Boolean,
    onAsk: () -> Unit,
    onCancel: () -> Unit,
    onSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        state.error?.let {
            Text(it, style = MungoTheme.typography.body3, color = DrivingMuted, maxLines = 2)
        }
        if (state.question.isNotBlank() && state.phase != AssistantPhase.IDLE) {
            Text(
                "“${state.question}”",
                maxLines = 2,
                style = MungoTheme.typography.body2,
                color = DrivingInk
            )
        }
        // 음성 안내가 꺼져 있으면 음성 질문 버튼은 회색 · 누를 수 없음
        val voiceOn = com.example.poco.core.designsystem.LocalVoiceEnabled.current
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = if (state.phase == AssistantPhase.IDLE) onAsk else onCancel,
                enabled = active && !blocked && voiceOn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = DrivingInk
                ),
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                Image(
                    painterResource(R.drawable.driving_mic),
                    null,
                    Modifier.size(20.dp),
                    colorFilter = if (voiceOn) {
                        null
                    } else {
                        androidx.compose.ui.graphics.ColorFilter.tint(DrivingMuted)
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when (state.phase) {
                        AssistantPhase.IDLE -> if (voiceOn) "음성으로 질문" else "음성 안내 꺼짐"
                        AssistantPhase.LISTENING -> "듣고 있어요 · 취소"
                        AssistantPhase.THINKING -> "답변 준비 중 · 취소"
                        AssistantPhase.CONNECTING -> "연결 중 · 취소"
                    }
                )
            }
            TextButton(
                onClick = onSetup,
                colors = ButtonDefaults.textButtonColors(contentColor = DrivingInk)
            ) {
                Text(if (state.connected || state.consented) "AI 연결 설정" else "AI 연결")
            }
        }
    }
}

@Composable
internal fun AssistantConnectionDialog(
    state: AssistantUiState,
    parked: Boolean,
    onDismiss: () -> Unit,
    onConsent: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit
) {
    // Deliberately not saveable: invitation codes must not enter saved instance state.
    var code by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf(state.consented) }
    val canConfirm = accepted &&
        parked &&
        state.configured &&
        (state.connected || state.anonymousAuth || code.length == 43) &&
        state.phase != AssistantPhase.CONNECTING
    // 공통 팝업 스타일 (회색 뒷배경 · 흰 카드 · 주황 버튼)
    MgAlertDialog(
        title = "mungo 음성 AI 연결",
        message = "질문을 음성으로 듣고 AI가 답해 드려요.",
        icon = MgIcons.Mic,
        dismissText = "닫기",
        confirmText = if (state.connected) "확인" else "연결",
        confirmEnabled = canConfirm,
        onDismiss = onDismiss,
        onConfirm = {
            onConsent()
            if (!state.connected) onConnect(code) else onDismiss()
            code = ""
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .background(Mg.Bg, RoundedCornerShape(20.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "음성은 Pleos 음성 인식으로 처리합니다. 인식된 질문과 최소 차량 상태를 mungo 서버 및 Gemini에 전송합니다. 휴게소 질문은 권한 허용 후 현재 위치를 mungo 서버에 전송하며, 위치 좌표는 Gemini에 보내지 않습니다.",
                style = Mg.regular(21, 31),
                color = Mg.Ink
            )
            Text(
                "질문·위치는 mungo 서버에 저장하지 않습니다. Gemini에는 질문·차량 상태·장소 후보를 전송합니다. 연결 해제로 전송을 중단할 수 있습니다.",
                style = Mg.regular(19, 28),
                color = Mg.Muted
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(accepted, { accepted = it })
                Text("위 정보 전송에 동의합니다", style = Mg.semi(22, 30), color = Mg.Ink)
            }
            val notes = buildList {
                if (!state.configured) add("AI 서버 주소가 설정되지 않았어요.")
                if (!parked) add("연결 코드는 P단 정차 상태에서 입력해 주세요.")
                if (state.anonymousAuth && !state.connected) {
                    add("동의 후 연결을 누르면 자동으로 인증돼요. 연결 코드는 필요하지 않아요.")
                }
                state.error?.let { add(it) }
                if (state.connected || (state.anonymousAuth && state.consented)) {
                    add("AI 서버에 연결되었어요. 연결 정보는 앱 실행 중에만 유지돼요.")
                }
            }
            notes.forEach { Text(it, style = Mg.regular(20, 29), color = Mg.OrangeText) }
            if (parked && state.configured && !state.connected && !state.anonymousAuth) {
                OutlinedTextField(
                    code,
                    { code = it.take(43) },
                    label = { Text("운영자가 발급한 일회용 연결 코드") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (state.connected) {
                TextButton(onClick = {
                    onDisconnect()
                    onDismiss()
                }) { Text("연결 해제", style = Mg.semi(22, 30), color = Mg.Muted) }
            }
        }
    }
}
