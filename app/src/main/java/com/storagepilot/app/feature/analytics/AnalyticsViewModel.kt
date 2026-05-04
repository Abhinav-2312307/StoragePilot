package com.storagepilot.app.feature.analytics

import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.domain.model.FileCategory
import com.storagepilot.app.domain.usecase.GetStorageReportUseCase
import com.storagepilot.app.domain.repository.CleanupStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val totalFileCount: Long = 0L,
    val categoryBreakdown: Map<FileCategory, Long> = emptyMap(),
    val lifetimeBytesFreed: Long = 0L,
    val lifetimeFilesDeleted: Int = 0,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getStorageReportUseCase: GetStorageReportUseCase,
    private val cleanupStatsRepository: CleanupStatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadDeviceStorage()

        viewModelScope.launch {
            getStorageReportUseCase.getCategoryBreakdown().collect { breakdown ->
                _uiState.update { it.copy(categoryBreakdown = breakdown) }
            }
        }

        viewModelScope.launch {
            getStorageReportUseCase.getTotalFileCount().collect { count ->
                _uiState.update { it.copy(totalFileCount = count) }
            }
        }

        viewModelScope.launch {
            cleanupStatsRepository.getLifetimeStats().collect { stats ->
                _uiState.update {
                    it.copy(
                        lifetimeBytesFreed = stats.bytesFreed,
                        lifetimeFilesDeleted = stats.filesDeleted,
                    )
                }
            }
        }
    }

    private fun loadDeviceStorage() {
        try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val total = stat.totalBytes
            val free = stat.availableBytes
            _uiState.update {
                it.copy(totalBytes = total, usedBytes = total - free, freeBytes = free)
            }
        } catch (_: Exception) { }
    }
}
