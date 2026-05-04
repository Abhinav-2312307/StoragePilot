package com.storagepilot.app.feature.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.domain.model.RecycleBinItem
import com.storagepilot.app.domain.repository.RecycleBinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecycleBinUiState(
    val items: List<RecycleBinItem> = emptyList(),
    val totalSize: Long = 0L,
    val isLoading: Boolean = true,
)

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val recycleBinRepository: RecycleBinRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecycleBinUiState())
    val uiState: StateFlow<RecycleBinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recycleBinRepository.getRecycleBinItems().collect { items ->
                val size = items.sumOf { it.sizeBytes }
                _uiState.update {
                    it.copy(
                        items = items.sortedByDescending { item -> item.deletedAt },
                        totalSize = size,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun restoreItem(item: RecycleBinItem) {
        viewModelScope.launch {
            recycleBinRepository.restoreFiles(listOf(item.id))
        }
    }

    fun permanentlyDeleteItem(item: RecycleBinItem) {
        viewModelScope.launch {
            recycleBinRepository.permanentlyDelete(listOf(item.id))
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            recycleBinRepository.emptyRecycleBin()
        }
    }
}
