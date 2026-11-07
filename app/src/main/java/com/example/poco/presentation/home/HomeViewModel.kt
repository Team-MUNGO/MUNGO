package com.example.poco.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poco.domain.model.CarWindow
import com.example.poco.domain.repository.WindowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class HomeViewModel @Inject constructor(private val windowRepository: WindowRepository) :
    ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSupportedWindows()
        observeDriverWindow()
    }

    /**
     * [예시 1] 일회성 조회 - suspend 함수 + Result
     * Repository가 Result로 감싸서 주기 때문에 onSuccess / onFailure로 분기한다.
     */
    private fun loadSupportedWindows() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            windowRepository.getSupportedWindows()
                .onSuccess { windows ->
                    _uiState.update {
                        it.copy(isLoading = false, supportedWindows = windows.toImmutableMap())
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "지원 창문 조회 실패")
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
        }
    }

    /**
     * [예시 2] 실시간 관찰 - Flow
     * Flow는 Result로 감싸져 있지 않으므로 catch를 꼭 붙여야 한다. (없으면 onFailed 시 크래시)
     * viewModelScope에서 collect하므로 ViewModel이 사라질 때 리스너도 자동 해제된다.
     */
    private fun observeDriverWindow() {
        viewModelScope.launch {
            windowRepository.observeWindowPosition(CarWindow.DRIVER)
                .catch { e ->
                    Timber.e(e, "운전석 창문 관찰 실패")
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
                .collect { position ->
                    _uiState.update { it.copy(driverWindowPosition = position) }
                }
        }
    }
}
