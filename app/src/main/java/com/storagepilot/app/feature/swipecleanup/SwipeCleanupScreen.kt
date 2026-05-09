package com.storagepilot.app.feature.swipecleanup

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.storagepilot.app.core.theme.*
import com.storagepilot.app.core.ui.components.GlassCard
import com.storagepilot.app.core.util.IntentUtils
import com.storagepilot.app.core.util.formatDate
import com.storagepilot.app.core.util.formatFileSize
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.model.FileCategory
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler

@Composable
fun SwipeCleanupScreen(
    viewModel: SwipeCleanupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SwipeModeSelectionScreen(
        state = state,
        onSelectGroup = { viewModel.selectGroup(it) },
        onSelectRandom30 = { viewModel.selectRandom30() },
        onSetFilter = { viewModel.setFilter(it) },
        onToggleSort = { 
            viewModel.setSortOrder(
                if (state.sortOrder == SortOrder.DATE_DESC) SortOrder.SIZE_DESC else SortOrder.DATE_DESC
            ) 
        }
    )

    if (state.currentMode == CleanupMode.SWIPING) {
        Dialog(
            onDismissRequest = { viewModel.exitSwiping() },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                SwipeSessionScreen(
                    state = state,
                    viewModel = viewModel,
                    onExit = { viewModel.exitSwiping() }
                )
            }
        }
    }
}

@Composable
private fun SwipeModeSelectionScreen(
    state: SwipeCleanupUiState,
    onSelectGroup: (FileGroup) -> Unit,
    onSelectRandom30: () -> Unit,
    onSetFilter: (FileCategory?) -> Unit,
    onToggleSort: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SELECT MODE",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(onClick = onToggleSort) {
                Icon(
                    imageVector = Icons.Filled.Sort,
                    contentDescription = "Sort",
                    tint = if (state.sortOrder == SortOrder.SIZE_DESC) PilotPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // Filters
        val filters = listOf(
            "All" to null,
            "Images" to FileCategory.IMAGES,
            "Videos" to FileCategory.VIDEOS,
            "Documents" to FileCategory.DOCUMENTS,
            "Apps" to FileCategory.APPS,
            "Archives" to FileCategory.ARCHIVES
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { (label, category) ->
                FilterChip(
                    selected = state.activeFilter == category,
                    onClick = { onSetFilter(category) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PilotPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = PilotPrimary,
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Album") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Month") }
            )
        }

        Spacer(Modifier.height(16.dp))

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .clickable(onClick = onSelectRandom30)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, tint = PilotPrimary)
                    Spacer(Modifier.width(12.dp))
                    Text("Random 30", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = PilotPrimary)
            }
        }

        Spacer(Modifier.height(12.dp))

        val currentGroups = if (selectedTab == 0) state.groupsAlbum else state.groupsMonth

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(currentGroups, key = { it.name }) { group ->
                FileGroupCard(group = group, onClick = { onSelectGroup(group) })
            }
        }
    }
}

@Composable
private fun FileGroupCard(group: FileGroup, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (group.previewPath.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(group.previewPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Outlined.Folder,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (group.reviewedCount > 0) "In progress • ${group.reviewedCount}/${group.totalCount}" else "${group.reviewedCount}/${group.totalCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = if (group.totalCount > 0) group.reviewedCount.toFloat() / group.totalCount else 0f,
                    modifier = Modifier.size(28.dp),
                    color = AccentOrange,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 3.dp
                )
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SwipeSessionScreen(
    state: SwipeCleanupUiState,
    viewModel: SwipeCleanupViewModel,
    onExit: () -> Unit
) {
    val context = LocalContext.current

    BackHandler { onExit() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = state.selectedGroup?.name ?: "Swipe Cleanup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (state.markedForDeletion.isNotEmpty()) {
                Button(
                    onClick = { viewModel.commitDeletions() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Delete (${state.markedForDeletion.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                IconButton(onClick = { 
                    viewModel.setSortOrder(
                        if (state.sortOrder == SortOrder.DATE_DESC) SortOrder.SIZE_DESC else SortOrder.DATE_DESC
                    ) 
                }) {
                    Icon(
                        imageVector = Icons.Filled.Sort,
                        contentDescription = "Sort",
                        tint = if (state.sortOrder == SortOrder.SIZE_DESC) PilotPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(text = "🗑 ${state.deletedCount} deleted", style = MaterialTheme.typography.labelMedium, color = AccentRed)
            Text(text = "💾 ${state.deletedBytes.formatFileSize()} freed", style = MaterialTheme.typography.labelMedium, color = AccentGreen)
            Text(text = "✓ ${state.keptCount} kept", style = MaterialTheme.typography.labelMedium, color = PilotSecondary)
        }

        Spacer(Modifier.height(16.dp))

        val currentFile = state.files.getOrNull(state.currentIndex)

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PilotPrimary)
            }
        } else if (currentFile == null) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = AccentGreen)
                    Spacer(Modifier.height(16.dp))
                    Text("All caught up!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("No more files to review in this group", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    
                    if (state.markedForDeletion.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.commitDeletions() }, 
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
                        ) {
                            Text("Confirm & Delete ${state.markedForDeletion.size} Items", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    
                    OutlinedButton(
                        onClick = onExit, 
                        modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
                    ) {
                        Text("Back to Selection")
                    }
                }
            }
        } else {
            SwipeableFileCard(
                file = currentFile,
                onSwipeLeft = { viewModel.swipeLeft() },
                onSwipeRight = { viewModel.swipeRight() },
                onOpen = {
                    IntentUtils.openFile(context, currentFile.path)
                },
                onLocate = {
                    val file = File(currentFile.path)
                    IntentUtils.openFolder(context, file.parent ?: "")
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }

        if (currentFile != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledIconButton(
                    onClick = { viewModel.swipeLeft() },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = AccentRed.copy(alpha = 0.15f)),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AccentRed)
                }

                OutlinedIconButton(onClick = { viewModel.skip() }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Skip", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (state.lastDeletedFile != null) {
                    OutlinedIconButton(onClick = { viewModel.undoLastDelete() }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.Undo, contentDescription = "Undo", tint = AccentOrange)
                    }
                }

                FilledIconButton(
                    onClick = { viewModel.swipeRight() },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Keep", tint = AccentGreen)
                }
            }
        }
        
        // Bottom Gallery Timeline
        val listState = rememberLazyListState()
        
        LaunchedEffect(state.currentIndex) {
            if (state.files.isNotEmpty() && state.currentIndex < state.files.size) {
                listState.animateScrollToItem(maxOf(0, state.currentIndex - 2))
            }
        }

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            itemsIndexed(state.files) { index, file ->
                val isCurrent = index == state.currentIndex
                val isMarked = state.markedForDeletion.contains(file.path)
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = if (isCurrent) 2.dp else 0.dp,
                            color = if (isCurrent) PilotPrimary else androidx.compose.ui.graphics.Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { viewModel.jumpToIndex(index) },
                    contentAlignment = Alignment.Center
                ) {
                    if (file.category == FileCategory.IMAGES || file.category == FileCategory.VIDEOS) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(file.path).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Outlined.InsertDriveFile, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    if (isMarked) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(AccentRed.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                        }
                    } else if (index < state.currentIndex) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(AccentGreen.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableFileCard(
    file: ScannedFile,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onOpen: () -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val animatedOffset = remember { Animatable(0f) }
    val threshold = 300f

    LaunchedEffect(file.path) {
        offsetX = 0f
        animatedOffset.snapTo(0f)
    }

    val rotation = (offsetX / 30f).coerceIn(-15f, 15f)
    val alpha = 1f - (abs(offsetX) / 600f).coerceIn(0f, 0.5f)

    GlassCard(
        modifier = modifier
            .graphicsLayer {
                translationX = offsetX
                rotationZ = rotation
                this.alpha = alpha
            }
            .pointerInput(file.path) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            offsetX < -threshold -> { onSwipeLeft(); offsetX = 0f }
                            offsetX > threshold -> { onSwipeRight(); offsetX = 0f }
                            else -> {
                                scope.launch {
                                    animatedOffset.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                                    offsetX = animatedOffset.value
                                }
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount -> offsetX += dragAmount },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable(onClick = onOpen),
            contentAlignment = Alignment.Center
        ) {
            when (file.category) {
                FileCategory.IMAGES, FileCategory.VIDEOS -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(file.path)
                            .crossfade(true)
                            .size(800)
                            .build(),
                        contentDescription = file.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                    )
                    if (file.category == FileCategory.VIDEOS) {
                        Icon(
                            Icons.Filled.PlayCircleOutline,
                            contentDescription = "Play Video",
                            modifier = Modifier.size(64.dp),
                            tint = TextWhite.copy(alpha = 0.8f)
                        )
                    }
                }
                FileCategory.DOCUMENTS -> Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(64.dp), tint = CategoryDocuments)
                FileCategory.APPS -> Icon(Icons.Outlined.Android, contentDescription = null, modifier = Modifier.size(64.dp), tint = CategoryApps)
                FileCategory.ARCHIVES -> Icon(Icons.Outlined.FolderZip, contentDescription = null, modifier = Modifier.size(64.dp), tint = CategoryArchives)
                else -> Icon(Icons.Outlined.InsertDriveFile, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (offsetX < -50f) {
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp).size(60.dp).clip(CircleShape).background(AccentRed.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Delete, contentDescription = null, tint = TextWhite, modifier = Modifier.size(28.dp)) }
            }

            if (offsetX > 50f) {
                Box(
                    modifier = Modifier.align(Alignment.CenterStart).padding(16.dp).size(60.dp).clip(CircleShape).background(AccentGreen.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Favorite, contentDescription = null, tint = TextWhite, modifier = Modifier.size(28.dp)) }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = file.sizeBytes.formatFileSize(), style = MaterialTheme.typography.bodySmall, color = PilotSecondary, fontWeight = FontWeight.Medium)
                    Text(text = file.modifiedAt.formatDate(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = file.parentFolder,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            
            IconButton(onClick = onLocate) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = "Locate Folder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
