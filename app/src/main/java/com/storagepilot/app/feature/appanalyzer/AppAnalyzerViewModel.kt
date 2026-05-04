package com.storagepilot.app.feature.appanalyzer

import android.app.AppOpsManager
import android.os.Build
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.domain.model.AppInfo
import com.storagepilot.app.domain.usecase.AnalyzeAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppAnalyzerUiState(
    val apps: List<AppInfo> = emptyList(),
    val totalAppSize: Long = 0L,
    val totalCacheSize: Long = 0L,
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val currentSort: AppSortOption = AppSortOption.SIZE_DESC,
)

enum class AppSortOption {
    SIZE_DESC, SIZE_ASC, CACHE_DESC, NAME_ASC
}

@HiltViewModel
class AppAnalyzerViewModel @Inject constructor(
    private val analyzeAppsUseCase: AnalyzeAppsUseCase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppAnalyzerUiState())
    val uiState: StateFlow<AppAnalyzerUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        _uiState.update { it.copy(isLoading = true, hasPermission = checkPermission()) }
        viewModelScope.launch {
            analyzeAppsUseCase.getAllApps().collect { appStorageInfos ->
                val pm = context.packageManager
                val apps = appStorageInfos.map { info ->
                    val icon = try { pm.getApplicationIcon(info.packageName) } catch(e: Exception) { null }
                    AppInfo(
                        packageName = info.packageName,
                        name = info.appName,
                        icon = icon,
                        totalSizeBytes = info.totalSizeBytes,
                        cacheSizeBytes = info.cacheSizeBytes,
                        dataSizeBytes = info.dataSizeBytes,
                        appSizeBytes = info.appSizeBytes
                    )
                }

                val totalSize = apps.sumOf { it.totalSizeBytes }
                val totalCache = apps.sumOf { it.cacheSizeBytes }

                _uiState.update {
                    it.copy(
                        apps = sortApps(apps, it.currentSort),
                        totalAppSize = totalSize,
                        totalCacheSize = totalCache,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun setSortOption(option: AppSortOption) {
        _uiState.update { 
            it.copy(
                currentSort = option,
                apps = sortApps(it.apps, option)
            )
        }
    }

    private fun sortApps(apps: List<AppInfo>, option: AppSortOption): List<AppInfo> {
        return when (option) {
            AppSortOption.SIZE_DESC -> apps.sortedByDescending { it.totalSizeBytes }
            AppSortOption.SIZE_ASC -> apps.sortedBy { it.totalSizeBytes }
            AppSortOption.CACHE_DESC -> apps.sortedByDescending { it.cacheSizeBytes }
            AppSortOption.NAME_ASC -> apps.sortedBy { it.name.lowercase() }
        }
    }

    private fun checkPermission(): Boolean {
        val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
