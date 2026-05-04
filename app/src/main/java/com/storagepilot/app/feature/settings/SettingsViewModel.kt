package com.storagepilot.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val recycleBinDays: Int = 30,
    val showHiddenFiles: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.recycleBinDays.collect { days ->
                _uiState.update { it.copy(recycleBinDays = days) }
            }
        }
        viewModelScope.launch {
            userPreferences.showHiddenFiles.collect { show ->
                _uiState.update { it.copy(showHiddenFiles = show) }
            }
        }
    }

    fun updateRecycleBinDays(days: Int) {
        viewModelScope.launch {
            userPreferences.setRecycleBinDays(days)
        }
    }

    fun updateShowHiddenFiles(show: Boolean) {
        viewModelScope.launch {
            userPreferences.setShowHiddenFiles(show)
        }
    }
}
