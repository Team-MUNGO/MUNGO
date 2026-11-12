package com.example.poco.core.common

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 앱 전체 토스트 (네이비 pill). 화면이 바뀌어도 보이도록 MainActivity가 띄운다.
 * 예) ViewModel에서 toastMessenger.show("보리를 동승자로 등록했어요")
 */
@Singleton
class ToastMessenger @Inject constructor() {
    private val _messages = MutableSharedFlow<ToastMessage>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<ToastMessage> = _messages.asSharedFlow()

    /** [detail]: 오른쪽에 회색으로 붙는 보조 문구 (예: "다음 이동부터 반영돼요") */
    fun show(message: String, detail: String? = null) {
        _messages.tryEmit(ToastMessage(message, detail))
    }
}

data class ToastMessage(val text: String, val detail: String? = null)
