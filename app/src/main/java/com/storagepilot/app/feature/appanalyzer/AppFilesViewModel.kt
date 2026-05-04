package com.storagepilot.app.feature.appanalyzer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppFilesUiState(
    val files: List<ScannedFile> = emptyList(),
    val appName: String = "",
    val isLoading: Boolean = true,
)

@HiltViewModel
class AppFilesViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val packageName: String = savedStateHandle["packageName"] ?: ""
    private val appName: String = savedStateHandle["appName"] ?: ""

    private val _uiState = MutableStateFlow(AppFilesUiState(appName = appName))
    val uiState: StateFlow<AppFilesUiState> = _uiState.asStateFlow()

    init {
        loadAppFiles()
    }

    private fun loadAppFiles() {
        viewModelScope.launch {
            fileRepository.getAllFiles().collect { allFiles ->
                // Look for files whose path contains the exact package name or app name
                val filtered = allFiles.filter { file ->
                    // Exclude internal android files if they match just "Android" loosely,
                    // ensure we only match if it really belongs to the app.
                    val path = file.path.lowercase()
                    val pkgMatch = packageName.isNotEmpty() && path.contains(packageName.lowercase())
                    // App Name might be too generic (e.g. "Google"), but we will try matching folder names
                    val appNameFolderMatch = appName.isNotEmpty() && path.contains("/${appName.lowercase()}/")
                    
                    pkgMatch || appNameFolderMatch
                }.sortedByDescending { it.sizeBytes }
                
                _uiState.update {
                    it.copy(
                        files = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }
}
