package com.example.poco.presentation.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.data.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState = _uiState.asStateFlow()

    /** 필수 항목은 끌 수 없다 → 끄려고 하면 "꼭 필요해요" 배너만 띄운다 */
    fun toggle(group: PermissionGroup) {
        _uiState.update { state ->
            val isOn = group in state.enabledGroups
            if (isOn && group.isRequired) return@update state.copy(requiredBanner = group)
            val groups = state.enabledGroups.toMutableSet()
            if (isOn) groups.remove(group) else groups.add(group)
            state.copy(enabledGroups = groups.toPersistentSet())
        }
    }

    /** 배너 [확인] → 권한 설정 화면으로 복귀 */
    fun dismissRequiredBanner() {
        _uiState.update { it.copy(requiredBanner = null) }
    }

    /** 허용하고 시작 / 나중에 - 어느 쪽이든 권한 화면은 첫 실행에만 보여준다 */
    fun complete(granted: Map<String, Boolean> = emptyMap()) {
        if (granted.isNotEmpty()) Timber.d("권한 결과: $granted")
        viewModelScope.launch {
            onboardingRepository.setPermissionCompleted()
                .onFailure { Timber.e(it, "권한 동의 상태 저장 실패") }
            _uiState.update { it.copy(isCompleted = true) }
        }
    }
}
