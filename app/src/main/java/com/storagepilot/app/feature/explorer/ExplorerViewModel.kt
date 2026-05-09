package com.storagepilot.app.feature.explorer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.domain.model.FileCategory
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.usecase.SearchFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExplorerUiState(
    val files: List<ScannedFile> = emptyList(),
    val filteredFiles: List<ScannedFile> = emptyList(),
    val selectedCategory: FileCategory? = null,
    val sortOption: SortOption = SortOption.SIZE_DESC,
    val isLoading: Boolean = true,
    val hasScannedBefore: Boolean = false,
)

enum class SortOption(val label: String) {
    SIZE_DESC("Largest First"),
    SIZE_ASC("Smallest First"),
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    NAME_ASC("Name (A-Z)"),
}

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val searchFilesUseCase: SearchFilesUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Read initial category from navigation arguments
    private val initialCategory: String? = savedStateHandle["initialCategory"]

    private val _uiState = MutableStateFlow(
        ExplorerUiState(
            selectedCategory = initialCategory?.let {
                try { FileCategory.valueOf(it) } catch (_: Exception) { null }
            }
        )
    )
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            searchFilesUseCase.getAllFiles().collect { files ->
                _uiState.update {
                    it.copy(
                        files = files,
                        isLoading = false,
                        hasScannedBefore = files.isNotEmpty(),
                    )
                }
                applyFilters()
            }
        }
    }

    fun setCategory(category: FileCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFilters()
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var result = state.files

        // Apply category filter
        state.selectedCategory?.let { cat ->
            result = result.filter { it.category == cat }
        }

        // Apply sorting
        result = when (state.sortOption) {
            SortOption.SIZE_DESC -> result.sortedByDescending { it.sizeBytes }
            SortOption.SIZE_ASC -> result.sortedBy { it.sizeBytes }
            SortOption.DATE_DESC -> result.sortedByDescending { it.modifiedAt }
            SortOption.DATE_ASC -> result.sortedBy { it.modifiedAt }
            SortOption.NAME_ASC -> result.sortedBy { it.name.lowercase() }
        }

        _uiState.update { it.copy(filteredFiles = result) }
    }
}
