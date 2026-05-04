package com.storagepilot.app.feature.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepilot.app.core.theme.PilotPrimary
import com.storagepilot.app.core.ui.components.FileListItem
import com.storagepilot.app.domain.model.FileCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    onNavigateToViewer: (String) -> Unit,
    viewModel: ExplorerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "File Explorer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        Icons.Filled.Sort,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Largest First") },
                        onClick = { 
                            viewModel.setSortOption(SortOption.SIZE_DESC)
                            showSortMenu = false
                        },
                        trailingIcon = { if (state.currentSort == SortOption.SIZE_DESC) Icon(Icons.Filled.Sort, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Smallest First") },
                        onClick = { 
                            viewModel.setSortOption(SortOption.SIZE_ASC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Newest First") },
                        onClick = { 
                            viewModel.setSortOption(SortOption.DATE_DESC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Oldest First") },
                        onClick = { 
                            viewModel.setSortOption(SortOption.DATE_ASC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Name (A-Z)") },
                        onClick = { 
                            viewModel.setSortOption(SortOption.NAME_ASC)
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        // Category Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = state.currentCategory == null,
                    onClick = { viewModel.setCategory(null) },
                    label = { Text("All Files") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PilotPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = PilotPrimary,
                    ),
                )
            }
            items(FileCategory.values()) { category ->
                FilterChip(
                    selected = state.currentCategory == category,
                    onClick = { viewModel.setCategory(category) },
                    label = { Text(category.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PilotPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = PilotPrimary,
                    ),
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

        // File List
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No files found in this category",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = state.files,
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
