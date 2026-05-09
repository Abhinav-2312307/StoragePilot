package com.storagepilot.app.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepilot.app.core.theme.PilotPrimary
import com.storagepilot.app.core.ui.components.FileListItem
import com.storagepilot.app.domain.model.FileCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Search Bar
        TopAppBar(
            title = {
                TextField(
                    value = state.query,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    placeholder = { Text("Search files, extensions...") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.focusRequester(focusRequester),
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChanged("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        // Filter Chips — only show browsable categories
        val browsableCategories = remember {
            listOf(
                FileCategory.IMAGES, FileCategory.VIDEOS, FileCategory.AUDIO,
                FileCategory.DOCUMENTS, FileCategory.APPS, FileCategory.ARCHIVES,
                FileCategory.DOWNLOADS, FileCategory.OTHER,
            )
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(browsableCategories) { category ->
                FilterChip(
                    selected = state.selectedCategory == category,
                    onClick = { 
                        if (state.selectedCategory == category) {
                            viewModel.setCategoryFilter(null)
                        } else {
                            viewModel.setCategoryFilter(category)
                        }
                    },
                    label = { Text(category.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PilotPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = PilotPrimary,
                    ),
                )
            }
        }

        // Results
        if (state.isSearching && state.results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.results.isEmpty() && (state.query.isNotBlank() || state.selectedCategory != null)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No files found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = state.results,
                    key = { it.path },
                ) { file ->
                    FileListItem(
                        file = file,
                        onClick = { onNavigateToViewer(file.path) },
                    )
                }
            }
        }
    }
}
