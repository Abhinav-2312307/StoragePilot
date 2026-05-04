package com.storagepilot.app.feature.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepilot.app.core.theme.*
import com.storagepilot.app.core.ui.components.*
import com.storagepilot.app.core.util.formatFileSize
import com.storagepilot.app.domain.model.FileCategory

@Composable
fun AnalyticsScreen(
    onNavigateToFlowchart: () -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel(),
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

        Text(
            text = "Storage Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "${state.totalFileCount} files indexed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        // ── Storage Overview Ring ──
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            val segments = state.categoryBreakdown.entries
                .filter { it.value > 0 }
                .sortedByDescending { it.value }
                .map { (cat, bytes) ->
                    StorageSegment(cat.displayName, bytes, categoryColor(cat))
                }

            StorageRingChart(
                segments = segments,
                usedBytes = state.usedBytes,
                totalBytes = state.totalBytes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(8.dp),
            )

            Spacer(Modifier.height(16.dp))

            // Legend
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.categoryBreakdown.entries
                    .filter { it.value > 0 }
                    .sortedByDescending { it.value }
                    .forEach { (cat, bytes) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(categoryColor(cat), MaterialTheme.shapes.extraSmall),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = cat.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = bytes.formatFileSize(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = categoryColor(cat),
                            )
                        }
                    }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── System Flowchart CTA ──
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToFlowchart() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(PilotPrimary.copy(alpha = 0.2f), MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AccountTree,
                        contentDescription = "Flowchart",
                        tint = PilotPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Interactive System Flowchart",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Explore your storage hierarchy as a visual map",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = "Go",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Cleanup Lifetime Stats ──
        Text(
            text = "Cleanup History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassCardCompact(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.lifetimeBytesFreed.formatFileSize(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen,
                )
                Text(
                    text = "Total Freed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GlassCardCompact(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${state.lifetimeFilesDeleted}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PilotPrimary,
                )
                Text(
                    text = "Files Cleaned",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Category Bar Chart ──
        Text(
            text = "Category Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            val maxBytes = state.categoryBreakdown.values.maxOrNull() ?: 1L

            state.categoryBreakdown.entries
                .filter { it.value > 0 }
                .sortedByDescending { it.value }
                .forEach { (cat, bytes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = cat.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(60.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        AnimatedProgressBar(
                            progress = bytes.toFloat() / maxBytes,
                            modifier = Modifier.weight(1f),
                            height = 10.dp,
                            gradientColors = listOf(
                                categoryColor(cat),
                                categoryColor(cat).copy(alpha = 0.6f),
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = bytes.formatFileSize(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(55.dp),
                        )
                    }
                }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun categoryColor(category: FileCategory): Color {
    return when (category) {
        FileCategory.IMAGES -> CategoryImages
        FileCategory.VIDEOS -> CategoryVideos
        FileCategory.AUDIO -> CategoryAudio
        FileCategory.DOCUMENTS -> CategoryDocuments
        FileCategory.APPS -> CategoryApps
        FileCategory.INSTALLED_APPS -> PilotSecondary
        FileCategory.ARCHIVES -> CategoryArchives
        FileCategory.DOWNLOADS -> CategoryDownloads
        FileCategory.CACHE -> CategoryCache
        FileCategory.HIDDEN -> CategoryHidden
        FileCategory.DUPLICATES -> CategoryDuplicates
        FileCategory.SYSTEM -> CategorySystem
        FileCategory.OTHER -> CategoryOther
    }
}
