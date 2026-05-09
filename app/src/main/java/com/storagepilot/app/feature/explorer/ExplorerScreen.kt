package com.storagepilot.app.feature.explorer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepilot.app.core.theme.PilotPrimary
import com.storagepilot.app.core.ui.components.FileListItem
import com.storagepilot.app.core.util.IntentUtils
import com.storagepilot.app.domain.model.FileCategory

/**
 * Browsable categories that make sense in a file explorer context.
 * Excludes virtual/derived categories like CACHE, HIDDEN, DUPLICATES, SYSTEM, INSTALLED_APPS.
 */
private val BROWSABLE_CATEGORIES = listOf(
    FileCategory.IMAGES,
    FileCategory.VIDEOS,
    FileCategory.AUDIO,
    FileCategory.DOCUMENTS,
    FileCategory.APPS,
    FileCategory.ARCHIVES,
    FileCategory.DOWNLOADS,
    FileCategory.OTHER,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    onNavigateToVideoPlayer: (String) -> Unit,
    onNavigateToDocumentViewer: (String) -> Unit,
    onNavigateToImageViewer: (String) -> Unit,
    viewModel: ExplorerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Explorer") },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option.label,
                                            fontWeight = if (option == state.sortOption) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (option == state.sortOption) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = PilotPrimary)
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Category filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PilotPrimary.copy(alpha = 0.2f),
                        ),
                    )
                }
                items(BROWSABLE_CATEGORIES) { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text(category.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PilotPrimary.copy(alpha = 0.2f),
                        ),
                    )
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (!state.hasScannedBefore) {
                // No scan has been performed yet
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.Radar,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = PilotPrimary.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Scan your storage first",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Run a Deep Scan from the Dashboard to index your files for browsing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else if (state.filteredFiles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No files found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (state.selectedCategory != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No ${state.selectedCategory!!.displayName} files found. Try a different category.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.filteredFiles, key = { it.path }) { file ->
                        FileListItem(
                            file = file,
                            onClick = {
                                // Smart file routing: open in-app viewer or external app
                                when (IntentUtils.getFileOpenAction(file.path)) {
                                    IntentUtils.FileOpenAction.VIDEO_PLAYER ->
                                        onNavigateToVideoPlayer(file.path)
                                    IntentUtils.FileOpenAction.PDF_VIEWER ->
                                        onNavigateToDocumentViewer(file.path)
                                    IntentUtils.FileOpenAction.TEXT_VIEWER ->
                                        onNavigateToDocumentViewer(file.path)
                                    IntentUtils.FileOpenAction.IMAGE_VIEWER ->
                                        onNavigateToImageViewer(file.path)
                                    IntentUtils.FileOpenAction.EXTERNAL ->
                                        IntentUtils.openFile(context, file.path)
                                }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}
