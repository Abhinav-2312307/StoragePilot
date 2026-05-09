package com.storagepilot.app.feature.explorer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagepilot.app.core.theme.PilotPrimary
import com.storagepilot.app.core.theme.PilotSurfaceVariant
import com.storagepilot.app.core.util.IntentUtils
import com.storagepilot.app.core.util.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset

/**
 * In-app document viewer that handles:
 * - PDF files (using Android's PdfRenderer)
 * - Text-based files (using buffered reader)
 * - Unsupported formats (shows info + "Open Externally" button)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    filePath: String,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val file = remember(filePath) { File(filePath) }
    val extension = remember(filePath) { file.extension.lowercase() }
    val fileName = remember(filePath) { file.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = file.length().formatFileSize(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Open externally button
                    IconButton(onClick = { IntentUtils.openFile(context, filePath) }) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "Open Externally")
                    }
                    // Share button
                    IconButton(onClick = { IntentUtils.shareFile(context, filePath) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            extension == "pdf" -> {
                PdfViewerContent(
                    file = file,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }

            extension in IntentUtils.TEXT_EXTENSIONS -> {
                TextViewerContent(
                    file = file,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }

            else -> {
                // Unsupported format - show info and open externally option
                UnsupportedFormatContent(
                    file = file,
                    extension = extension,
                    onOpenExternally = { IntentUtils.openFile(context, filePath) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
        }
    }
}

/**
 * Renders PDF pages using Android's built-in PdfRenderer.
 */
@Composable
private fun PdfViewerContent(
    file: File,
    modifier: Modifier = Modifier,
) {
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Render PDF pages off the main thread
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val bitmaps = mutableListOf<Bitmap>()

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    // Render at 2x density for sharpness
                    val bitmap = Bitmap.createBitmap(
                        page.width * 2,
                        page.height * 2,
                        Bitmap.Config.ARGB_8888,
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps.add(bitmap)
                }

                renderer.close()
                fd.close()

                pages = bitmaps
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to render PDF"
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PilotPrimary)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Rendering PDF...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else if (errorMessage != null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Could not render PDF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(pages) { index, bitmap ->
                // Page number indicator
                Text(
                    text = "Page ${index + 1} of ${pages.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                // PDF page
                Card(
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Page ${index + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * Displays text-based file content with syntax-friendly formatting.
 */
@Composable
private fun TextViewerContent(
    file: File,
    modifier: Modifier = Modifier,
) {
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isTruncated by remember { mutableStateOf(false) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                val maxBytes = 512 * 1024 // 512KB limit for text display
                val reader = BufferedReader(
                    InputStreamReader(file.inputStream(), Charset.defaultCharset())
                )
                val sb = StringBuilder()
                var totalRead = 0
                val buffer = CharArray(8192)
                var read: Int

                while (reader.read(buffer).also { read = it } != -1) {
                    totalRead += read * 2 // rough byte estimate
                    if (totalRead > maxBytes) {
                        isTruncated = true
                        break
                    }
                    sb.append(buffer, 0, read)
                }
                reader.close()

                content = sb.toString()
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to read file"
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PilotPrimary)
        }
    } else if (errorMessage != null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "Error: $errorMessage",
                color = MaterialTheme.colorScheme.error,
            )
        }
    } else {
        Column(modifier = modifier) {
            if (isTruncated) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "File truncated (showing first 512KB of ${file.length().formatFileSize()})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SelectionContainer {
                Text(
                    text = content,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                )
            }
        }
    }
}

/**
 * Fallback screen for unsupported document formats.
 */
@Composable
private fun UnsupportedFormatContent(
    file: File,
    extension: String,
    onOpenExternally: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Filled.Description,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = PilotPrimary.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${file.length().formatFileSize()} • .${extension.uppercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "This file format requires an external app to view.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onOpenExternally,
                colors = ButtonDefaults.buttonColors(containerColor = PilotPrimary),
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open Externally", fontWeight = FontWeight.Bold)
            }
        }
    }
}
