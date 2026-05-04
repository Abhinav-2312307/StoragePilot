package com.storagepilot.app.feature.swipecleanup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.usecase.DeleteFilesUseCase
import com.storagepilot.app.domain.usecase.SearchFilesUseCase
import com.storagepilot.app.domain.model.FileCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class CleanupMode {
    SELECT_GROUP,
    SWIPING
}

enum class SortOrder {
    DATE_DESC,
    SIZE_DESC
}

data class FileGroup(
    val name: String,
    val files: List<ScannedFile>,
    val previewPath: String,
    val totalCount: Int,
    var reviewedCount: Int = 0
)

data class SwipeCleanupUiState(
    val currentMode: CleanupMode = CleanupMode.SELECT_GROUP,
    val groupsAlbum: List<FileGroup> = emptyList(),
    val groupsMonth: List<FileGroup> = emptyList(),
    val selectedGroup: FileGroup? = null,
    
    val files: List<ScannedFile> = emptyList(),
    val currentIndex: Int = 0,
    val deletedCount: Int = 0,
    val deletedBytes: Long = 0,
    val keptCount: Int = 0,
    val isLoading: Boolean = true,
    val lastDeletedFile: ScannedFile? = null,
    val activeFilter: FileCategory? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val markedForDeletion: Set<String> = emptySet()
)

@HiltViewModel
class SwipeCleanupViewModel @Inject constructor(
    private val searchFilesUseCase: SearchFilesUseCase,
    private val deleteFilesUseCase: DeleteFilesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SwipeCleanupUiState())
    val uiState: StateFlow<SwipeCleanupUiState> = _uiState.asStateFlow()

    private var allFiles: List<ScannedFile> = emptyList()

    init {
        loadFiles()
    }

    private fun loadFiles() {
        viewModelScope.launch {
            searchFilesUseCase.getAllFiles()
                .collect { files ->
                    allFiles = files
                    generateGroups()
                }
        }
    }

    private fun generateGroups() {
        val state = _uiState.value
        val filteredFiles = if (state.activeFilter != null) {
            allFiles.filter { it.category == state.activeFilter }
        } else {
            allFiles
        }
        
        val sortedFiles = if (state.sortOrder == SortOrder.SIZE_DESC) {
            filteredFiles.sortedByDescending { it.sizeBytes }
        } else {
            filteredFiles.sortedByDescending { it.modifiedAt }
        }
        
        // Generate Album groups
        val albumMap = sortedFiles.groupBy { it.parentFolder.substringAfterLast('/') }
        val albums = albumMap.map { (folder, filesInFolder) ->
            FileGroup(
                name = folder.ifEmpty { "Root" },
                files = filesInFolder,
                previewPath = filesInFolder.firstOrNull { it.category.name == "IMAGES" || it.category.name == "VIDEOS" }?.path ?: filesInFolder.firstOrNull()?.path ?: "",
                totalCount = filesInFolder.size
            )
        }.sortedByDescending { it.files.size }

        // Generate Month groups
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val monthMap = filteredFiles.groupBy { dateFormat.format(Date(it.modifiedAt)) }
        val months = monthMap.map { (month, filesInMonth) ->
            FileGroup(
                name = month,
                files = filesInMonth,
                previewPath = filesInMonth.firstOrNull { it.category.name == "IMAGES" || it.category.name == "VIDEOS" }?.path ?: filesInMonth.firstOrNull()?.path ?: "",
                totalCount = filesInMonth.size
            )
        }.sortedByDescending { it.files.size }

        _uiState.update {
            it.copy(
                groupsAlbum = albums,
                groupsMonth = months,
                isLoading = false
            )
        }
    }

    fun setFilter(category: FileCategory?) {
        _uiState.update { it.copy(activeFilter = category, isLoading = true) }
        generateGroups()
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order, isLoading = true) }
        
        val state = _uiState.value
        if (state.currentMode == CleanupMode.SWIPING && state.selectedGroup != null) {
            val resorted = if (order == SortOrder.SIZE_DESC) {
                state.files.sortedByDescending { it.sizeBytes }
            } else {
                state.files.sortedByDescending { it.modifiedAt }
            }
            _uiState.update { 
                it.copy(
                    files = resorted,
                    currentIndex = 0,
                    markedForDeletion = emptySet(),
                    deletedCount = 0,
                    keptCount = 0,
                    lastDeletedFile = null
                ) 
            }
        }
        
        generateGroups()
    }

    fun selectGroup(group: FileGroup) {
        _uiState.update {
            it.copy(
                currentMode = CleanupMode.SWIPING,
                selectedGroup = group,
                files = group.files,
                currentIndex = group.reviewedCount,
                deletedCount = 0,
                deletedBytes = 0,
                keptCount = 0,
                lastDeletedFile = null
            )
        }
    }

    fun selectRandom30() {
        val state = _uiState.value
        val filteredFiles = if (state.activeFilter != null) {
            allFiles.filter { it.category == state.activeFilter }
        } else {
            allFiles
        }
        
        val sortedFiles = if (state.sortOrder == SortOrder.SIZE_DESC) {
            filteredFiles.sortedByDescending { it.sizeBytes }
        } else {
            filteredFiles.sortedByDescending { it.modifiedAt }
        }
        
        val randomFiles = sortedFiles.shuffled().take(30)
        val group = FileGroup(
            name = "Random 30",
            files = randomFiles,
            previewPath = randomFiles.firstOrNull()?.path ?: "",
            totalCount = randomFiles.size
        )
        selectGroup(group)
    }

    fun exitSwiping() {
        _uiState.update {
            it.copy(
                currentMode = CleanupMode.SELECT_GROUP,
                selectedGroup = null,
                files = emptyList()
            )
        }
    }

    fun swipeLeft() {
        val state = _uiState.value
        val file = state.files.getOrNull(state.currentIndex) ?: return

        _uiState.update {
            val newMarked = it.markedForDeletion + file.path
            it.copy(
                markedForDeletion = newMarked,
                currentIndex = it.currentIndex + 1,
                deletedCount = newMarked.size,
                lastDeletedFile = file,
            )
        }
        updateGroupProgress()
    }

    fun swipeRight() {
        val state = _uiState.value
        val file = state.files.getOrNull(state.currentIndex) ?: return

        _uiState.update {
            val newMarked = it.markedForDeletion - file.path
            it.copy(
                markedForDeletion = newMarked,
                currentIndex = it.currentIndex + 1,
                keptCount = it.keptCount + 1,
                deletedCount = newMarked.size,
                lastDeletedFile = null,
            )
        }
        updateGroupProgress()
    }

    fun skip() {
        _uiState.update {
            it.copy(currentIndex = it.currentIndex + 1, lastDeletedFile = null)
        }
        updateGroupProgress()
    }

    fun jumpToIndex(index: Int) {
        if (index in 0.._uiState.value.files.size) {
            _uiState.update {
                it.copy(currentIndex = index, lastDeletedFile = null)
            }
            updateGroupProgress()
        }
    }

    private fun updateGroupProgress() {
        val state = _uiState.value
        state.selectedGroup?.reviewedCount = state.currentIndex
    }

    fun undoLastDelete() {
        _uiState.update {
            it.copy(
                lastDeletedFile = null,
                currentIndex = maxOf(0, it.currentIndex - 1)
            )
        }
        updateGroupProgress()
    }

    fun commitDeletions() {
        val state = _uiState.value
        val filesToDelete = state.files.filter { state.markedForDeletion.contains(it.path) }
        if (filesToDelete.isNotEmpty()) {
            viewModelScope.launch {
                deleteFilesUseCase.safeDelete(filesToDelete)
                _uiState.update { it.copy(markedForDeletion = emptySet(), deletedCount = 0) }
                exitSwiping()
            }
        } else {
            exitSwiping()
        }
    }
}
