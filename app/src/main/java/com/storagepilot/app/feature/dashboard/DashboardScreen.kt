package com.storagepilot.app.feature.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepilot.app.core.theme.*
import com.storagepilot.app.core.ui.components.*
import com.storagepilot.app.core.util.formatFileSize
import com.storagepilot.app.core.util.formatPercent
import com.storagepilot.app.core.util.formatRelativeTime
import com.storagepilot.app.domain.model.FileCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToScan: () -> Unit,
    onNavigateToCategory: (FileCategory) -> Unit,
    onNavigateToLargeFiles: () -> Unit,
    onNavigateToDuplicates: () -> Unit,
    onNavigateToHidden: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Top Bar ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "StoragePilot",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PulsingDot(color = AccentGreen, size = 6.dp)
                    Text(
                        text = "Offline · Private · Local Only",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGreen,
                    )
                }
                state.lastScanTime?.let { scanTime ->
                    Text(
                        text = "Last scan: ${scanTime.formatRelativeTime()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onNavigateToSearch) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Storage Ring Chart ──
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            val segments = buildSegments(state.categoryBreakdown)
            val usedPercent = if (state.totalBytes > 0) {
                (state.usedBytes.toFloat() / state.totalBytes * 100f)
            } else 0f

            StorageRingChart(
                segments = segments,
                usedBytes = state.usedBytes,
                totalBytes = state.totalBytes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(8.dp),
            )

            Spacer(Modifier.height(12.dp))

            // Usage bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Used: ${state.usedBytes.formatFileSize()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = usedPercent.formatPercent(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        usedPercent > 90 -> AccentRed
                        usedPercent > 75 -> AccentOrange
                        else -> AccentGreen
                    },
                )
            }

            Spacer(Modifier.height(6.dp))

            AnimatedProgressBar(
                progress = if (state.totalBytes > 0) state.usedBytes.toFloat() / state.totalBytes else 0f,
                gradientColors = when {
                    usedPercent > 90 -> listOf(AccentRed, AccentOrange)
                    usedPercent > 75 -> listOf(AccentOrange, AccentYellow)
                    else -> listOf(PilotPrimary, PilotSecondary)
                },
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${state.freeBytes.formatFileSize()} free",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Scan Button ──
        if (!state.hasScanned || state.isScanning) {
            Button(
                onClick = { viewModel.startScan() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isScanning,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PilotPrimary,
                ),
            ) {
                if (state.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Scanning...")
                } else {
                    Icon(Icons.Filled.Radar, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Deep Scan Storage",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // ── File Count Summary ──
        if (state.hasScanned) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Storage Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = { viewModel.startScan() }) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Rescan",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Rescan", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Category Grid ──
            val categories = buildCategoryCards(state.categoryBreakdown)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { card ->
                            CategoryCard(
                                icon = card.icon,
                                label = card.label,
                                sizeBytes = card.sizeBytes,
                                accentColor = card.color,
                                onClick = { 
                                    when (card.category) {
                                        FileCategory.SYSTEM -> { /* Do nothing */ }
                                        FileCategory.INSTALLED_APPS -> onNavigateToApps()
                                        else -> onNavigateToCategory(card.category)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Fill remaining space if odd count
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Quick Actions ──
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WarningCard(
                    icon = Icons.Outlined.FileCopy,
                    title = "Find Duplicates",
                    subtitle = "Detect and remove duplicate files",
                    accentColor = CategoryDuplicates,
                    onClick = onNavigateToDuplicates,
                    modifier = Modifier.fillMaxWidth(),
                )
                WarningCard(
                    icon = Icons.Outlined.Storage,
                    title = "Large Files",
                    subtitle = "Find storage-heavy files",
                    accentColor = AccentOrange,
                    onClick = onNavigateToLargeFiles,
                    modifier = Modifier.fillMaxWidth(),
                )
                WarningCard(
                    icon = Icons.Outlined.VisibilityOff,
                    title = "Hidden Storage",
                    subtitle = "Detect hidden & junk files",
                    accentColor = CategoryHidden,
                    onClick = onNavigateToHidden,
                    modifier = Modifier.fillMaxWidth(),
                )
                WarningCard(
                    icon = Icons.Outlined.Apps,
                    title = "App Analyzer",
                    subtitle = "Inspect app storage & cache",
                    accentColor = CategoryApps,
                    onClick = onNavigateToApps,
                    modifier = Modifier.fillMaxWidth(),
                )
                WarningCard(
                    icon = Icons.Outlined.DeleteSweep,
                    title = "Recycle Bin",
                    subtitle = "Restore or permanently delete",
                    accentColor = AccentRed,
                    onClick = onNavigateToRecycleBin,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Helper data & functions ──

private data class CategoryCardData(
    val category: FileCategory,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val sizeBytes: Long,
    val color: androidx.compose.ui.graphics.Color,
)

private fun buildCategoryCards(breakdown: Map<FileCategory, Long>): List<CategoryCardData> {
    return listOf(
        CategoryCardData(FileCategory.IMAGES, Icons.Outlined.Image, "Images", breakdown[FileCategory.IMAGES] ?: 0, CategoryImages),
        CategoryCardData(FileCategory.VIDEOS, Icons.Outlined.Videocam, "Videos", breakdown[FileCategory.VIDEOS] ?: 0, CategoryVideos),
        CategoryCardData(FileCategory.AUDIO, Icons.Outlined.MusicNote, "Audio", breakdown[FileCategory.AUDIO] ?: 0, CategoryAudio),
        CategoryCardData(FileCategory.DOCUMENTS, Icons.Outlined.Description, "Docs", breakdown[FileCategory.DOCUMENTS] ?: 0, CategoryDocuments),
        CategoryCardData(FileCategory.APPS, Icons.Outlined.Android, "APKs", breakdown[FileCategory.APPS] ?: 0, CategoryApps),
        CategoryCardData(FileCategory.INSTALLED_APPS, Icons.Outlined.Apps, "Apps", breakdown[FileCategory.INSTALLED_APPS] ?: 0, PilotSecondary),
        CategoryCardData(FileCategory.ARCHIVES, Icons.Outlined.FolderZip, "Archives", breakdown[FileCategory.ARCHIVES] ?: 0, CategoryArchives),
        CategoryCardData(FileCategory.DOWNLOADS, Icons.Outlined.Download, "Downloads", breakdown[FileCategory.DOWNLOADS] ?: 0, CategoryDownloads),
        CategoryCardData(FileCategory.SYSTEM, Icons.Outlined.Memory, "System OS", breakdown[FileCategory.SYSTEM] ?: 0, CategorySystem),
        CategoryCardData(FileCategory.OTHER, Icons.Outlined.Inventory2, "Other", breakdown[FileCategory.OTHER] ?: 0, CategoryOther),
    ).filter { it.sizeBytes > 0 }
        .sortedByDescending { it.sizeBytes }
}

private fun buildSegments(breakdown: Map<FileCategory, Long>): List<StorageSegment> {
    val colorMap = mapOf(
        FileCategory.IMAGES to CategoryImages,
        FileCategory.VIDEOS to CategoryVideos,
        FileCategory.AUDIO to CategoryAudio,
        FileCategory.DOCUMENTS to CategoryDocuments,
        FileCategory.APPS to CategoryApps,
        FileCategory.INSTALLED_APPS to PilotSecondary,
        FileCategory.ARCHIVES to CategoryArchives,
        FileCategory.DOWNLOADS to CategoryDownloads,
        FileCategory.SYSTEM to CategorySystem,
        FileCategory.OTHER to CategoryOther,
    )
    return breakdown.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }
        .map { (cat, bytes) ->
            StorageSegment(
                label = cat.displayName,
                bytes = bytes,
                color = colorMap[cat] ?: CategoryOther,
            )
        }
}
