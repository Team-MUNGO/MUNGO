package com.example.poco.presentation.session

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.poco.R
import com.example.poco.core.theme.POCOTheme
import com.example.poco.presentation.session.component.DrivingInk
import com.example.poco.presentation.session.component.DrivingMuted
import com.example.poco.presentation.session.component.DrivingOrange

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
            Text(it, style = POCOTheme.typography.body3, color = DrivingMuted, maxLines = 2)
        }
        if (state.question.isNotBlank() && state.phase != AssistantPhase.IDLE) {
            Text("“${state.question}”", maxLines = 2, style = POCOTheme.typography.body2, color = DrivingInk)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = if (state.phase == AssistantPhase.IDLE) onAsk else onCancel,
                enabled = active && !blocked,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DrivingInk),
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                Image(painterResource(R.drawable.driving_mic), null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when (state.phase) {
                        AssistantPhase.IDLE -> "음성으로 질문"
                        AssistantPhase.LISTENING -> "듣고 있어요 · 취소"
                        AssistantPhase.THINKING -> "답변 준비 중 · 취소"
                        AssistantPhase.CONNECTING -> "연결 중 · 취소"
                    }
                )
            }
            TextButton(onClick = onSetup, colors = ButtonDefaults.textButtonColors(contentColor = DrivingInk)) {
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("POCO 음성 AI 연결") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "음성은 Pleos 음성 인식으로 처리합니다. 인식된 질문과 최소 차량 상태를 POCO 서버 및 Gemini에 전송합니다. 휴게소 질문은 권한 허용 후 현재 위치를 POCO 서버에 전송하며, 위치 좌표는 Gemini에 보내지 않습니다."
                )
                Text(
                    "질문·위치는 POCO 서버에 저장하지 않습니다. Gemini에는 질문·차량 상태·장소 후보를 전송합니다. 연결 해제로 전송을 중단할 수 있습니다.",
                    style = POCOTheme.typography.caption1
                )
                Row {
                    Checkbox(accepted, { accepted = it })
                    Text("위 정보 전송에 동의합니다")
                }
                if (!state.configured) Text("AI 서버 주소가 설정되지 않았어요.")
                if (!parked) Text("연결 코드는 P단 정차 상태에서 입력해 주세요.")
                if (state.anonymousAuth && !state.connected) {
                    Text("동의 후 연결을 누르면 자동으로 인증돼요. 연결 코드는 필요하지 않아요.")
                }
                if (parked &&
                    state.configured &&
                    !state.connected &&
                    !state.anonymousAuth
                ) {
                    OutlinedTextField(
                        code,
                        {
                            code = it.take(43)
                        },
                        label = {
                            Text("운영자가 발급한 일회용 연결 코드")
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false
                        )
                    )
                }
                state.error?.let { Text(it) }
                if (state.connected || (state.anonymousAuth && state.consented)) Text("AI 서버에 연결되었어요. 연결 정보는 앱 실행 중에만 유지돼요.")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConsent()
                    if (!state.connected) onConnect(code) else onDismiss()
                    code = ""
                },
                enabled =
                accepted &&
                    parked &&
                    state.configured &&
                    (state.connected || state.anonymousAuth || code.length == 43) &&
                    state.phase != AssistantPhase.CONNECTING
            ) {
                Text(if (state.connected) "확인" else "연결")
            }
        },
        dismissButton = {
            Row {
                if (state.connected) {
                    TextButton(onClick = {
                        onDisconnect()
                        onDismiss()
                    }) { Text("연결 해제") }
                }
                TextButton(onClick = onDismiss) { Text("닫기") }
            }
        }
    )
}
