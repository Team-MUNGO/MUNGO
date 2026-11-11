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
    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun show(message: String) {
        _messages.tryEmit(message)
    }
}
