package com.storagepilot.app.feature.analytics

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepilot.app.core.theme.AccentOrange
import com.storagepilot.app.core.theme.CategoryApps
import com.storagepilot.app.core.theme.CategoryImages
import com.storagepilot.app.core.theme.PilotPrimary
import com.storagepilot.app.core.util.IntentUtils
import com.storagepilot.app.core.util.formatFileSize
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemFlowchartScreen(
    onNavigateBack: () -> Unit,
    viewModel: SystemFlowchartViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Zoom & Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset(100f, 500f)) } // Start with some offset
    
    val textMeasurer = rememberTextMeasurer()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Flowchart", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.isLoading || state.rootNode == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                return@Box
            }
            
            // To force recomposition when viewmodel says layout changed
            val layoutVersion = state.layoutVersion
            
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, rotation ->
                            scale = (scale * zoom).coerceIn(0.1f, 5f)
                            offset += pan
                        }
                    }
                    .pointerInput(layoutVersion) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                // Reverse calculate tap coordinates to Canvas space
                                val canvasX = (tapOffset.x - offset.x) / scale
                                val canvasY = (tapOffset.y - offset.y) / scale
                                
                                val clickedNode = findClickedNode(state.rootNode!!, canvasX, canvasY)
                                if (clickedNode != null) {
                                    if (!clickedNode.isFolder) {
                                        IntentUtils.openFile(context, clickedNode.id)
                                    } else {
                                        try {
                                            // Open folder using generic Intent
                                            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3A${clickedNode.id.removePrefix("/storage/emulated/0/")}")
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "vnd.android.document/directory")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Fallback
                                            try {
                                                val fallbackUri = Uri.parse("file://${clickedNode.id}")
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(fallbackUri, "resource/folder")
                                                }
                                                context.startActivity(intent)
                                            } catch (e2: Exception) {}
                                        }
                                    }
                                }
                            },
                            onTap = { tapOffset ->
                                val canvasX = (tapOffset.x - offset.x) / scale
                                val canvasY = (tapOffset.y - offset.y) / scale
                                
                                val clickedNode = findClickedNode(state.rootNode!!, canvasX, canvasY)
                                if (clickedNode != null && clickedNode.isFolder) {
                                    viewModel.toggleNode(clickedNode.id)
                                } else if (clickedNode != null && !clickedNode.isFolder) {
                                    // Single tap on file also opens it for convenience
                                    IntentUtils.openFile(context, clickedNode.id)
                                }
                            }
                        )
                    }
            ) {
                translate(left = offset.x, top = offset.y) {
                    scale(scale, scale, pivot = Offset.Zero) {
                        // Draw Connections first so they are behind nodes
                        drawConnections(state.rootNode!!)
                        
                        // Draw Nodes
                        drawNodes(state.rootNode!!, textMeasurer)
                    }
                }
            }
            
            // Overlays / Hints
            Text(
                "Pinch to Zoom • Drag to Pan\nTap folder to expand • Double-tap to open native",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// Extension functions for Canvas drawing
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConnections(node: FlowchartNode) {
    if (!node.isExpanded || node.children.isEmpty()) return
    
    val childrenToDraw = node.children.take(100) // Render limit
    
    for (child in childrenToDraw) {
        val path = Path().apply {
            moveTo(node.x, node.y)
            // Bezier curve for beautiful flow
            cubicTo(
                node.x + 200f, node.y,
                child.x - 200f, child.y,
                child.x, child.y
            )
        }
        drawPath(
            path = path,
            color = Color.Gray.copy(alpha = 0.3f),
            style = Stroke(width = 3f)
        )
        
        drawConnections(child)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNodes(node: FlowchartNode, textMeasurer: TextMeasurer) {
    val nodeColor = if (node.isFolder) {
        if (node.isExpanded) PilotPrimary else AccentOrange
    } else {
        CategoryImages // Leaves
    }
    
    // Draw Circle
    drawCircle(
        color = nodeColor,
        radius = node.radius,
        center = Offset(node.x, node.y)
    )
    
    // Draw Name Text (to the right of the node)
    val textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    try {
        val textLayout = textMeasurer.measure(node.name, style = textStyle)
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(node.x + node.radius + 8f, node.y - textLayout.size.height / 2f)
        )
    } catch (e: Exception) {}
    
    // Draw Size Text (Inside the node)
    val sizeStyle = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    try {
        val sizeString = node.sizeBytes.formatFileSize()
        val sizeLayout = textMeasurer.measure(sizeString, style = sizeStyle)
        drawText(
            textLayoutResult = sizeLayout,
            topLeft = Offset(
                node.x - sizeLayout.size.width / 2f, 
                node.y - sizeLayout.size.height / 2f
            )
        )
    } catch (e: Exception) {}
    
    if (node.isExpanded) {
        val childrenToDraw = node.children.take(100)
        for (child in childrenToDraw) {
            drawNodes(child, textMeasurer)
        }
    }
}

private fun findClickedNode(node: FlowchartNode, x: Float, y: Float): FlowchartNode? {
    // Check children first (so we click on top-most if they overlap somehow)
    if (node.isExpanded) {
        val childrenToDraw = node.children.take(100)
        for (child in childrenToDraw.reversed()) {
            val found = findClickedNode(child, x, y)
            if (found != null) return found
        }
    }
    
    // Check this node
    val distance = sqrt((node.x - x).pow(2) + (node.y - y).pow(2))
    // Add touch slop margin (e.g. 20f minimum tap target)
    if (distance <= maxOf(node.radius, 20f)) {
        return node
    }
    return null
}
