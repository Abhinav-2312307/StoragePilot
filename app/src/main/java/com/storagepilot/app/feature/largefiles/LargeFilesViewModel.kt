package com.storagepilot.app.feature.largefiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.data.preferences.UserPreferences
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.usecase.DeleteFilesUseCase
import com.storagepilot.app.domain.usecase.GetLargeFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LargeFilesUiState(
    val files: List<ScannedFile> = emptyList(),
    val totalSize: Long = 0L,
    val thresholdMb: Int = 50,
    val isLoading: Boolean = true,
)

@HiltViewModel
class LargeFilesViewModel @Inject constructor(
    private val getLargeFilesUseCase: GetLargeFilesUseCase,
    private val deleteFilesUseCase: DeleteFilesUseCase,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LargeFilesUiState())
    val uiState: StateFlow<LargeFilesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.largeFileThresholdMb.collect { threshold ->
                _uiState.update { it.copy(thresholdMb = threshold) }
                loadLargeFiles(threshold)
            }
        }
    }

    private fun loadLargeFiles(thresholdMb: Int) {
        viewModelScope.launch {
            getLargeFilesUseCase.execute(minSizeMb = thresholdMb.toLong(), limit = 100).collect { files ->
                val total = files.sumOf { it.sizeBytes }
                _uiState.update {
                    it.copy(
                        files = files,
                        totalSize = total,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun updateThreshold(mb: Int) {
        viewModelScope.launch {
            userPreferences.setLargeFileThreshold(mb)
        }
    }

    fun deleteFile(file: ScannedFile) {
        viewModelScope.launch {
            deleteFilesUseCase.safeDelete(listOf(file))
        }
    }
}
