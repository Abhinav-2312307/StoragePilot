package com.storagepilot.app.feature.recyclebin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.storagepilot.app.core.theme.AccentRed
import com.storagepilot.app.core.theme.PilotPrimary
import com.storagepilot.app.core.theme.PilotSecondary
import com.storagepilot.app.core.ui.components.GlassCard
import com.storagepilot.app.core.util.formatDate
import com.storagepilot.app.core.util.formatFileSize
import com.storagepilot.app.domain.model.FileCategory
import com.storagepilot.app.domain.model.RecycleBinItem
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecycleBinViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showEmptyConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.items.isNotEmpty()) {
                        TextButton(onClick = { showEmptyConfirmDialog = true }) {
                            Text("Empty Bin", color = AccentRed)
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
            } else if (state.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = PilotPrimary.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Recycle bin is empty",
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
                                text = "Recoverable Space",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = state.totalSize.formatFileSize(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = PilotSecondary,
                            )
                        }
                        Text(
                            text = "${state.items.size} files",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.items, key = { it.id }) { item ->
                        RecycleBinItemRow(
                            item = item,
                            onRestore = { viewModel.restoreItem(item) },
                            onPermanentlyDelete = { viewModel.permanentlyDeleteItem(item) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }

        if (showEmptyConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyConfirmDialog = false },
                title = { Text("Empty Recycle Bin?") },
                text = { Text("This will permanently delete all files in the recycle bin. This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.emptyRecycleBin()
                            showEmptyConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Text("Empty")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun RecycleBinItemRow(
    item: RecycleBinItem,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (item.category == FileCategory.IMAGES || item.category == FileCategory.VIDEOS) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.recyclePath) // Load from recycle path
                        .crossfade(true)
                        .size(300)
                        .build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = PilotPrimary,
                )
            }
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.sizeBytes.formatFileSize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = PilotSecondary,
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                )
                
                val daysLeft = TimeUnit.MILLISECONDS.toDays(item.autoDeleteAt - System.currentTimeMillis())
                Text(
                    text = "${daysLeft.coerceAtLeast(0)}d left",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (daysLeft < 3) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row {
            IconButton(onClick = onRestore) {
                Icon(Icons.Filled.Restore, contentDescription = "Restore", tint = PilotSecondary)
            }
            IconButton(onClick = onPermanentlyDelete) {
                Icon(Icons.Filled.DeleteForever, contentDescription = "Delete Permanently", tint = AccentRed)
            }
        }
    }
}
