package com.storagepilot.app.feature.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.storagepilot.app.core.theme.AccentRed
import com.storagepilot.app.core.ui.components.GlassCard
import com.storagepilot.app.core.util.formatDate
import com.storagepilot.app.core.util.formatFileSize
import com.storagepilot.app.domain.model.FileCategory
import com.storagepilot.app.domain.model.ScannedFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    file: ScannedFile,
    onNavigateBack: () -> Unit,
    onDelete: (ScannedFile) -> Unit,
    onShare: (String) -> Unit,
) {
    var showInfoPanel by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onShare(file.path) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { showInfoPanel = !showInfoPanel }) {
                        Icon(Icons.Filled.Info, contentDescription = "Info")
                    }
                    IconButton(onClick = { onDelete(file) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AccentRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                )
            )
        },
        containerColor = Color.Black,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            // Preview Content
            when (file.category) {
                FileCategory.IMAGES, FileCategory.VIDEOS -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(file.path)
                            .crossfade(true)
                            .build(),
                        contentDescription = file.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    Text(
                        text = "Preview not available for ${file.extension.uppercase()} files.",
                        color = Color.White,
                    )
                }
            }

            // Info Panel Overlay
            if (showInfoPanel) {
                GlassCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "File Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Divider(color = Color.White.copy(alpha = 0.2f))
                        InfoRow("Location", file.parentFolder)
                        InfoRow("Size", file.sizeBytes.formatFileSize())
                        InfoRow("Created", file.createdAt.formatDate())
                        InfoRow("Modified", file.modifiedAt.formatDate())
                        if (file.width > 0 && file.height > 0) {
                            InfoRow("Resolution", "${file.width} x ${file.height}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
