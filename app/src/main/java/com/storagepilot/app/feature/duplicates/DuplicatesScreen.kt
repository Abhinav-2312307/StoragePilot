package com.storagepilot.app.feature.duplicates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepilot.app.core.theme.AccentRed
import com.storagepilot.app.core.theme.CategoryDuplicates
import com.storagepilot.app.core.ui.components.FileListItem
import com.storagepilot.app.core.ui.components.GlassCard
import com.storagepilot.app.core.util.formatFileSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String) -> Unit,
    viewModel: DuplicatesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicate Files") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.duplicateGroups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Delete, // Using a generic icon for empty state
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = CategoryDuplicates,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No duplicates found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                // Header Stats
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Wasted Space",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = state.totalWastedBytes.formatFileSize(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentRed,
                            )
                        }
                        Text(
                            text = "${state.duplicateGroups.size} groups",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                // List of Groups
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.duplicateGroups) { group ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Duplicate Group • ${group.totalWastedBytes.formatFileSize()} waste",
                                style = MaterialTheme.typography.labelMedium,
                                color = CategoryDuplicates,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            
                            // Show all files in the group
                            group.files.forEachIndexed { index, file ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.weight(1f)) {
                                        FileListItem(
                                            file = file,
                                            onClick = { onNavigateToViewer(file.path) },
                                        )
                                    }
                                    // Don't show delete on the FIRST file (keep it as original)
                                    if (index > 0) {
                                        IconButton(
                                            onClick = { viewModel.deleteFile(file) },
                                            modifier = Modifier.padding(end = 16.dp),
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete Duplicate",
                                                tint = AccentRed,
                                            )
                                        }
                                    }
                                }
                            }
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
