package com.storagepilot.app.feature.hidden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.usecase.DeleteFilesUseCase
import com.storagepilot.app.domain.usecase.SearchFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HiddenStorageUiState(
    val files: List<ScannedFile> = emptyList(),
    val totalSize: Long = 0L,
    val isLoading: Boolean = true,
)

@HiltViewModel
class HiddenStorageViewModel @Inject constructor(
    private val searchFilesUseCase: SearchFilesUseCase,
    private val deleteFilesUseCase: DeleteFilesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HiddenStorageUiState())
    val uiState: StateFlow<HiddenStorageUiState> = _uiState.asStateFlow()

    init {
        loadHiddenFiles()
    }

    private fun loadHiddenFiles() {
        viewModelScope.launch {
            searchFilesUseCase.getHidden().collect { files ->
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

    fun deleteFile(file: ScannedFile) {
        viewModelScope.launch {
            deleteFilesUseCase.safeDelete(listOf(file))
        }
    }
}
