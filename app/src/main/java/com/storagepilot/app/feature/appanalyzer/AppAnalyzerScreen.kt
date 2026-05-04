package com.storagepilot.app.feature.appanalyzer

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepilot.app.core.theme.CategoryApps
import com.storagepilot.app.core.theme.CategoryCache
import com.storagepilot.app.core.theme.AccentOrange
import com.storagepilot.app.core.theme.PilotPrimary
import com.storagepilot.app.core.ui.components.GlassCard
import com.storagepilot.app.core.ui.components.WarningCard
import com.storagepilot.app.core.util.formatFileSize
import com.storagepilot.app.domain.model.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppAnalyzerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAppFiles: (String, String) -> Unit,
    viewModel: AppAnalyzerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Analyzer") },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Largest First") },
                                onClick = { viewModel.setSortOption(AppSortOption.SIZE_DESC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Smallest First") },
                                onClick = { viewModel.setSortOption(AppSortOption.SIZE_ASC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Cache Size") },
                                onClick = { viewModel.setSortOption(AppSortOption.CACHE_DESC); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)") },
                                onClick = { viewModel.setSortOption(AppSortOption.NAME_ASC); showSortMenu = false }
                            )
                        }
                    }
                },
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
            } else {
                if (!state.hasPermission) {
                    WarningCard(
                        icon = Icons.Outlined.Settings,
                        title = "Unlock Deep Analytics",
                        subtitle = "Grant Usage Access to see precise App Data and Cache sizes.",
                        accentColor = AccentOrange,
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            // Handle potential ActivityNotFoundException if device doesn't support direct package uri
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // Header Stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total App Size",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = state.totalAppSize.formatFileSize(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CategoryApps,
                        )
                    }
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total Cache",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = state.totalCacheSize.formatFileSize(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CategoryCache,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.apps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedApp = app
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // Icon
                            val iconBitmap = remember(app.icon) { app.icon?.toBitmap()?.asImageBitmap() }
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap,
                                    contentDescription = app.name,
                                    modifier = Modifier.size(48.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Android,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = CategoryApps,
                                )
                            }

                            // Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Size: ${app.totalSizeBytes.formatFileSize()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CategoryApps,
                                    )
                                    if (app.cacheSizeBytes > 0) {
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        Text(
                                            text = "Cache: ${app.cacheSizeBytes.formatFileSize()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CategoryCache,
                                        )
                                    }
                                }
                            }

                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
        
        selectedApp?.let { app ->
            ModalBottomSheet(
                onDismissRequest = { selectedApp = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val iconBitmap = remember(app.icon) { app.icon?.toBitmap()?.asImageBitmap() }
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = app.name,
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.Android,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = CategoryApps,
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Breakdown
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("App Size", style = MaterialTheme.typography.bodyLarge)
                        Text(app.appSizeBytes.formatFileSize(), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("User Data", style = MaterialTheme.typography.bodyLarge)
                        Text(app.dataSizeBytes.formatFileSize(), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cache", style = MaterialTheme.typography.bodyLarge, color = CategoryCache)
                        Text(app.cacheSizeBytes.formatFileSize(), fontWeight = FontWeight.Bold, color = CategoryCache)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Space Used", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(app.totalSizeBytes.formatFileSize(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CategoryApps)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${app.packageName}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PilotPrimary)
                    ) {
                        Text("Manage App (Clear Cache/Data)", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = {
                            selectedApp = null // close bottom sheet
                            onNavigateToAppFiles(app.packageName, app.name)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Browse Files (Internal)")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
