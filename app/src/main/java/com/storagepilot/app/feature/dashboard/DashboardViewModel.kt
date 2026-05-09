package com.storagepilot.app.feature.dashboard

import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.domain.model.FileCategory
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.usecase.GetStorageReportUseCase
import com.storagepilot.app.domain.usecase.ScanStorageUseCase
import com.storagepilot.app.domain.usecase.AnalyzeAppsUseCase
import com.storagepilot.app.domain.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val categoryBreakdown: Map<FileCategory, Long> = emptyMap(),
    val totalFileCount: Long = 0L,
    val indexedSize: Long = 0L,
    val largestFiles: List<ScannedFile> = emptyList(),
    val isScanning: Boolean = false,
    val hasScanned: Boolean = false,
    val lastScanTime: Long? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getStorageReportUseCase: GetStorageReportUseCase,
    private val scanStorageUseCase: ScanStorageUseCase,
    private val analyzeAppsUseCase: AnalyzeAppsUseCase,
    private val scanRepository: ScanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDeviceStorage()
        observeIndex()
        observeScanState()
        observeLastScanTime()
    }

    private fun loadDeviceStorage() {
        try {
            // Environment.getDataDirectory() gives the true partition size that the user can access
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes
            val free = stat.availableBytes
            val used = total - free

            _uiState.update {
                it.copy(
                    totalBytes = total,
                    usedBytes = used,
                    freeBytes = free,
                )
            }
            recalculateSystemStorage()
        } catch (_: Exception) { }
    }

    private fun observeIndex() {
        viewModelScope.launch {
            getStorageReportUseCase.getCategoryBreakdown().collect { breakdown ->
                _uiState.update {
                    it.copy(
                        categoryBreakdown = it.categoryBreakdown + breakdown,
                        hasScanned = breakdown.isNotEmpty(),
                    )
                }
                recalculateSystemStorage()
            }
        }

        // Fetch installed apps
        viewModelScope.launch {
            analyzeAppsUseCase.getAllApps().collect { apps ->
                val totalAppSize = apps.sumOf { it.totalSizeBytes }
                _uiState.update { state ->
                    val newBreakdown = state.categoryBreakdown.toMutableMap()
                    newBreakdown[FileCategory.INSTALLED_APPS] = totalAppSize
                    state.copy(categoryBreakdown = newBreakdown)
                }
                recalculateSystemStorage()
            }
        }

        viewModelScope.launch {
            getStorageReportUseCase.getTotalFileCount().collect { count ->
                _uiState.update { it.copy(totalFileCount = count) }
            }
        }

        viewModelScope.launch {
            getStorageReportUseCase.getTotalSize().collect { size ->
                _uiState.update { it.copy(indexedSize = size) }
            }
        }

        viewModelScope.launch {
            getStorageReportUseCase.getLargestFiles(10).collect { files ->
                _uiState.update { it.copy(largestFiles = files) }
            }
        }
    }

    private fun recalculateSystemStorage() {
        _uiState.update { state ->
            val scannedTotal = state.categoryBreakdown
                .filterKeys { it != FileCategory.SYSTEM }
                .values.sum()
            
            // System space is the used space minus whatever files and apps we found
            var systemSpace = state.usedBytes - scannedTotal
            if (systemSpace < 0) systemSpace = 0
            
            val newBreakdown = state.categoryBreakdown.toMutableMap()
            if (systemSpace > 0) {
                newBreakdown[FileCategory.SYSTEM] = systemSpace
            } else {
                newBreakdown.remove(FileCategory.SYSTEM)
            }
            
            state.copy(categoryBreakdown = newBreakdown)
        }
    }

    private fun observeScanState() {
        viewModelScope.launch {
            scanStorageUseCase.isScanning().collect { scanning ->
                _uiState.update { it.copy(isScanning = scanning) }
            }
        }
    }

    fun startScan() {
        viewModelScope.launch {
            scanStorageUseCase.startFullScan()
            loadDeviceStorage() // Refresh after scan
        }
    }

    private fun observeLastScanTime() {
        viewModelScope.launch {
            scanRepository.getLastScanTime().collect { time ->
                _uiState.update { it.copy(lastScanTime = time) }
            }
        }
    }
}
