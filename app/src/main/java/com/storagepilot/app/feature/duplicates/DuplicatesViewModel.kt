package com.storagepilot.app.feature.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.domain.model.DuplicateGroup
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.usecase.DeleteFilesUseCase
import com.storagepilot.app.domain.usecase.DetectDuplicatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DuplicatesUiState(
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val totalWastedBytes: Long = 0L,
    val isLoading: Boolean = true,
)

@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    private val detectDuplicatesUseCase: DetectDuplicatesUseCase,
    private val deleteFilesUseCase: DeleteFilesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicatesUiState())
    val uiState: StateFlow<DuplicatesUiState> = _uiState.asStateFlow()

    init {
        loadDuplicates()
    }

    private fun loadDuplicates() {
        viewModelScope.launch {
            detectDuplicatesUseCase.getDuplicates().collect { duplicates ->
                // Group duplicates by their hash
                val groups = duplicates
                    .filter { it.md5Hash != null }
                    .groupBy { it.md5Hash!! }
                    .map { (hash, files) ->
                        // Calculate wasted bytes (n-1 files size)
                        val sizePerFile = files.firstOrNull()?.sizeBytes ?: 0L
                        val wasted = sizePerFile * (files.size - 1)
                        DuplicateGroup(hash, files.sortedByDescending { it.modifiedAt }, wasted)
                    }
                    .sortedByDescending { it.totalWastedBytes }
                
                val totalWasted = groups.sumOf { it.totalWastedBytes }

                _uiState.update { 
                    it.copy(
                        duplicateGroups = groups,
                        totalWastedBytes = totalWasted,
                        isLoading = false
                    ) 
                }
            }
        }
    }

    fun deleteFile(file: ScannedFile) {
        viewModelScope.launch {
            deleteFilesUseCase.safeDelete(listOf(file))
            // The DB flow will automatically update the UI list
        }
    }
}
