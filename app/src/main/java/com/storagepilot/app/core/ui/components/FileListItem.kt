package com.storagepilot.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.storagepilot.app.core.theme.*
import com.storagepilot.app.core.util.formatDate
import com.storagepilot.app.core.util.formatFileSize
import com.storagepilot.app.domain.model.FileCategory
import com.storagepilot.app.domain.model.ScannedFile

@Composable
fun FileListItem(
    file: ScannedFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            if (file.category == FileCategory.IMAGES || file.category == FileCategory.VIDEOS) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(file.path)
                        .crossfade(true)
                        .size(300)
                        .build(),
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Icon(
                            imageVector = if (file.category == FileCategory.IMAGES) Icons.Filled.Image else Icons.Filled.VideoFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    },
                    error = {
                        Icon(
                            imageVector = if (file.category == FileCategory.IMAGES) Icons.Filled.Image else Icons.Filled.VideoFile,
                            contentDescription = null,
                            tint = if (file.category == FileCategory.IMAGES) CategoryImages else CategoryVideos,
                        )
                    },
                    success = {
                        SubcomposeAsyncImageContent()
                    },
                )
            } else {
                val (icon, tint) = when (file.category) {
                    FileCategory.DOCUMENTS -> Icons.Filled.Description to CategoryDocuments
                    FileCategory.AUDIO -> Icons.Filled.AudioFile to CategoryAudio
                    FileCategory.APPS -> Icons.Filled.Android to CategoryApps
                    FileCategory.ARCHIVES -> Icons.Filled.FolderZip to CategoryArchives
                    FileCategory.DOWNLOADS -> Icons.Filled.Download to CategoryDownloads
                    FileCategory.VIDEOS -> Icons.Filled.VideoFile to CategoryVideos
                    else -> Icons.Filled.InsertDriveFile to PilotPrimary
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                )
            }
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            // Parent folder path for context
            Text(
                text = file.parentFolder.removePrefix("/storage/emulated/0/"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = file.sizeBytes.formatFileSize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = PilotPrimary,
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = file.modifiedAt.formatDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
