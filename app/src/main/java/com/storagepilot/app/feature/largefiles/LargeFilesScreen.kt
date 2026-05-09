package com.storagepilot.app.feature.largefiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import com.storagepilot.app.domain.model.ScannedFile
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepilot.app.core.theme.AccentOrange
import com.storagepilot.app.core.theme.AccentRed
import com.storagepilot.app.core.ui.components.FileListItem
import com.storagepilot.app.core.ui.components.GlassCard
import com.storagepilot.app.core.util.formatFileSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeFilesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (String) -> Unit,
    viewModel: LargeFilesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterMenu by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<ScannedFile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Large Files") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("> 10 MB") },
                                onClick = { 
                                    viewModel.updateThreshold(10)
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("> 50 MB") },
                                onClick = { 
                                    viewModel.updateThreshold(50)
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("> 100 MB") },
                                onClick = { 
                                    viewModel.updateThreshold(100)
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("> 500 MB") },
                                onClick = { 
                                    viewModel.updateThreshold(500)
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("> 1 GB") },
                                onClick = { 
                                    viewModel.updateThreshold(1024)
                                    showFilterMenu = false
                                }
                            )
                        }
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
                                text = "Total Size (> ${state.thresholdMb}MB)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = state.totalSize.formatFileSize(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange,
                            )
                        }
                        Text(
                            text = "${state.files.size} files",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (state.files.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No files larger than ${state.thresholdMb}MB found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.files, key = { it.path }) { file ->
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
                                IconButton(
                                    onClick = { fileToDelete = file },
                                    modifier = Modifier.padding(end = 16.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = AccentRed,
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }

        // Delete confirmation dialog
        fileToDelete?.let { file ->
            AlertDialog(
                onDismissRequest = { fileToDelete = null },
                title = { Text("Delete File?") },
                text = { Text("Move \"${file.name}\" to the Recycle Bin?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteFile(file)
                            fileToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { fileToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
