package com.storagepilot.app.feature.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.domain.model.ScanProgress
import com.storagepilot.app.domain.usecase.ScanStorageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
    val progress: ScanProgress = ScanProgress.Idle,
    val isScanning: Boolean = false,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanStorageUseCase: ScanStorageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scanStorageUseCase.observeProgress().collect { progress ->
                _uiState.update { it.copy(progress = progress) }
            }
        }
        viewModelScope.launch {
            scanStorageUseCase.isScanning().collect { scanning ->
                _uiState.update { it.copy(isScanning = scanning) }
            }
        }
    }

    fun startScan() {
        viewModelScope.launch {
            scanStorageUseCase.startFullScan()
        }
    }

    fun cancelScan() {
        viewModelScope.launch {
            scanStorageUseCase.cancelScan()
        }
    }
}
