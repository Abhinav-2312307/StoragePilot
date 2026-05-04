package com.storagepilot.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.domain.model.FileCategory
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.usecase.SearchFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<ScannedFile> = emptyList(),
    val isSearching: Boolean = false,
    val selectedCategory: FileCategory? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchFilesUseCase: SearchFilesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        // Debounce search queries to avoid excessive DB calls
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query, _uiState.value.selectedCategory)
                }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query, isSearching = true) }
        searchQuery.value = query
    }

    fun setCategoryFilter(category: FileCategory?) {
        _uiState.update { it.copy(selectedCategory = category, isSearching = true) }
        performSearch(_uiState.value.query, category)
    }

    private fun performSearch(query: String, category: FileCategory?) {
        viewModelScope.launch {
            val resultsFlow = if (query.isBlank() && category != null) {
                searchFilesUseCase.getByCategory(category)
            } else if (query.isNotBlank()) {
                searchFilesUseCase.search(query)
            } else {
                flowOf(emptyList())
            }

            resultsFlow.collect { files ->
                val filtered = if (category != null && query.isNotBlank()) {
                    files.filter { it.category == category }
                } else {
                    files
                }
                
                _uiState.update { 
                    it.copy(
                        results = filtered, 
                        isSearching = false
                    ) 
                }
            }
        }
    }
}
