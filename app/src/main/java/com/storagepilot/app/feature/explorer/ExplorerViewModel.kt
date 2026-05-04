package com.storagepilot.app.feature.explorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.domain.model.FileCategory
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.usecase.GetStorageReportUseCase
import com.storagepilot.app.domain.usecase.SearchFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption {
    SIZE_DESC,
    SIZE_ASC,
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
}

data class ExplorerUiState(
    val files: List<ScannedFile> = emptyList(),
    val currentCategory: FileCategory? = null,
    val currentSort: SortOption = SortOption.SIZE_DESC,
    val isLoading: Boolean = true,
)

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val searchFilesUseCase: SearchFilesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    init {
        // Load all files initially
        loadFiles()
    }

    fun setCategory(category: FileCategory?) {
        _uiState.update { it.copy(currentCategory = category, isLoading = true) }
        loadFiles()
    }

    fun setSortOption(sortOption: SortOption) {
        _uiState.update { it.copy(currentSort = sortOption) }
        sortCurrentFiles()
    }

    private fun loadFiles() {
        val category = _uiState.value.currentCategory
        viewModelScope.launch {
            val flow = if (category != null) {
                searchFilesUseCase.getByCategory(category)
            } else {
                searchFilesUseCase.search("") // Empty query gets all (limited to 200 in DAO, we should update to support full pagination or all files)
            }
            
            flow.collect { files ->
                _uiState.update { it.copy(files = files) }
                sortCurrentFiles()
            }
        }
    }

    private fun sortCurrentFiles() {
        _uiState.update { state ->
            val sorted = when (state.currentSort) {
                SortOption.SIZE_DESC -> state.files.sortedByDescending { it.sizeBytes }
                SortOption.SIZE_ASC -> state.files.sortedBy { it.sizeBytes }
                SortOption.DATE_DESC -> state.files.sortedByDescending { it.modifiedAt }
                SortOption.DATE_ASC -> state.files.sortedBy { it.modifiedAt }
                SortOption.NAME_ASC -> state.files.sortedBy { it.name.lowercase() }
            }
            state.copy(files = sorted, isLoading = false)
        }
    }
}
