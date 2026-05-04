package com.storagepilot.app.feature.scan

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepilot.app.core.theme.*
import com.storagepilot.app.core.ui.components.*
import com.storagepilot.app.core.util.formatFileSize
import com.storagepilot.app.domain.model.ScanProgress

@Composable
fun ScanScreen(
    onScanComplete: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto-navigate back on completion
    LaunchedEffect(state.progress) {
        if (state.progress is ScanProgress.Complete) {
            kotlinx.coroutines.delay(1500)
            onScanComplete()
        }
    }

    // Spinning radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
        ),
        label = "radar_rotation",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val progress = state.progress) {
            is ScanProgress.Idle -> {
                Icon(
                    Icons.Filled.Radar,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = PilotPrimary,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Ready to Scan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Deep scan your device storage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.startScan() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = PilotPrimary),
                ) {
                    Text("Start Deep Scan", fontWeight = FontWeight.SemiBold)
                }
            }

            is ScanProgress.Starting, is ScanProgress.Scanning, is ScanProgress.BatchProcessed -> {
                Icon(
                    Icons.Filled.Radar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .rotate(rotation),
                    tint = PilotPrimary,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Scanning...",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))

                val statusText = when (progress) {
                    is ScanProgress.Scanning -> "Scanning: ${progress.currentPath}\n${progress.filesScanned} files found"
                    is ScanProgress.BatchProcessed -> "${progress.totalSoFar} files indexed"
                    else -> "Initializing scanner..."
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))

                AnimatedProgressBar(
                    progress = -1f, // Indeterminate
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = listOf(PilotPrimary, PilotSecondary),
                )

                Spacer(Modifier.height(24.dp))

                OutlinedButton(
                    onClick = { viewModel.cancelScan() },
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text("Cancel")
                }
            }

            is ScanProgress.Complete -> {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = AccentGreen,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Scan Complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${progress.totalFiles} files · ${progress.totalBytes.formatFileSize()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is ScanProgress.Error -> {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = AccentRed,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Scan Failed",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentRed,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = progress.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.startScan() },
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text("Retry")
                }
            }
        }
    }
}
