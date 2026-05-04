package com.storagepilot.app.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.log10

class FlowchartNode(
    val id: String,
    val name: String,
    val isFolder: Boolean,
    var sizeBytes: Long = 0,
    val children: MutableList<FlowchartNode> = mutableListOf(),
    var isExpanded: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var radius: Float = 10f,
)

data class FlowchartUiState(
    val rootNode: FlowchartNode? = null,
    val isLoading: Boolean = true,
    val layoutVersion: Int = 0, // Used to force recomposition when layout changes
)

@HiltViewModel
class SystemFlowchartViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlowchartUiState())
    val uiState: StateFlow<FlowchartUiState> = _uiState.asStateFlow()

    init {
        loadFlowchart()
    }

    private fun loadFlowchart() {
        viewModelScope.launch {
            fileRepository.getAllFiles().collect { files ->
                if (files.isEmpty()) return@collect
                
                withContext(Dispatchers.Default) {
                    try {
                        val root = buildTree(files)
                        calculateSizes(root)
                        
                        // Sort children AFTER sizes are calculated so folders are sorted correctly!
                        fun sortChildren(node: FlowchartNode) {
                            node.children.sortByDescending { it.sizeBytes }
                            node.children.forEach { sortChildren(it) }
                        }
                        sortChildren(root)
                        
                        // Expand root by default
                        root.isExpanded = true
                        // DO NOT expand subfolders to save memory on launch
                        
                        layoutTree(root, 0, 0f)
                        
                        _uiState.update { 
                            it.copy(
                                rootNode = root,
                                isLoading = false,
                                layoutVersion = it.layoutVersion + 1
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun buildTree(files: List<ScannedFile>): FlowchartNode {
        val root = FlowchartNode(
            id = "/storage/emulated/0",
            name = "Internal Storage",
            isFolder = true
        )

        val prefix = "/storage/emulated/0/"
        
        files.forEach { file ->
            val relativePath = if (file.path.startsWith(prefix)) {
                file.path.substring(prefix.length)
            } else {
                file.path.substringAfterLast("0/", file.path)
            }
            
            val parts = relativePath.split("/")
            var current = root
            
            for (i in 0 until parts.size - 1) {
                val part = parts[i]
                if (part.isEmpty()) continue
                
                var child = current.children.find { it.name == part }
                if (child == null) {
                    child = FlowchartNode(
                        id = current.id + "/" + part,
                        name = part,
                        isFolder = true
                    )
                    current.children.add(child)
                }
                current = child
            }
            
            val fileName = parts.last()
            if (fileName.isNotEmpty()) {
                current.children.add(
                    FlowchartNode(
                        id = file.path,
                        name = fileName,
                        isFolder = false,
                        sizeBytes = file.sizeBytes
                    )
                )
            }
        }
        
        // Sorting moved to after size calculation

        
        return root
    }

    private fun calculateSizes(node: FlowchartNode): Long {
        if (!node.isFolder) return node.sizeBytes
        node.sizeBytes = node.children.sumOf { calculateSizes(it) }
        return node.sizeBytes
    }

    private fun layoutTree(node: FlowchartNode, depth: Int, startY: Float): Float {
        node.x = depth * 400f // Horizontal spacing between levels
        
        // Dynamic radius calculation using log scale
        val minRadius = 15f
        val maxRadius = 120f
        val scale = if (node.sizeBytes > 0) log10(node.sizeBytes.toDouble()).toFloat() else 0f
        node.radius = (scale * 10f).coerceIn(minRadius, maxRadius)
        
        // If leaf or collapsed, just take up space and return next Y
        if (!node.isExpanded || node.children.isEmpty()) {
            node.y = startY
            return startY + node.radius * 2f + 40f // Add padding
        }
        
        var currentY = startY
        
        // Layout children
        // We only layout up to 50 children to prevent insane canvas overload if a folder has 5000 files
        val childrenToDraw = node.children.take(100) 
        
        for (child in childrenToDraw) {
            currentY = layoutTree(child, depth + 1, currentY)
        }
        
        // Center the parent vertically among its drawn children
        if (childrenToDraw.isNotEmpty()) {
            node.y = (childrenToDraw.first().y + childrenToDraw.last().y) / 2f
        } else {
            node.y = startY
        }
        
        return currentY
    }

    fun toggleNode(nodeId: String) {
        val currentState = _uiState.value
        val root = currentState.rootNode ?: return
        
        viewModelScope.launch(Dispatchers.Default) {
            val node = findNode(root, nodeId)
            if (node != null && node.isFolder) {
                if (!node.isExpanded) {
                    // Accordion behavior: Collapse all, then expand path to this node
                    collapseAll(root)
                    expandPathTo(root, nodeId)
                } else {
                    // Turn off: Just collapse this branch
                    collapseAll(node)
                }
                
                // Recalculate layout starting from 0f
                layoutTree(root, 0, 0f)
                
                _uiState.update { 
                    it.copy(layoutVersion = it.layoutVersion + 1)
                }
            }
        }
    }

    private fun collapseAll(node: FlowchartNode) {
        node.isExpanded = false
        for (child in node.children) {
            collapseAll(child)
        }
    }

    private fun expandPathTo(current: FlowchartNode, targetId: String): Boolean {
        if (current.id == targetId) {
            current.isExpanded = true
            return true
        }
        var foundPath = false
        for (child in current.children) {
            if (expandPathTo(child, targetId)) {
                foundPath = true
            }
        }
        if (foundPath) {
            current.isExpanded = true
        }
        return foundPath
    }

    private fun findNode(current: FlowchartNode, id: String): FlowchartNode? {
        if (current.id == id) return current
        for (child in current.children) {
            val found = findNode(child, id)
            if (found != null) return found
        }
        return null
    }
}
