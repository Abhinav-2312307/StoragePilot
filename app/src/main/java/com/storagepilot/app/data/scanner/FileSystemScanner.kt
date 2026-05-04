package com.storagepilot.app.data.scanner

import android.os.Environment
import com.storagepilot.app.core.util.*
import com.storagepilot.app.data.local.entity.FileIndexEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.yield
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct filesystem scanner for files not indexed by MediaStore.
 * Traverses hidden folders, app data, caches, and residual directories.
 */
@Singleton
class FileSystemScanner @Inject constructor() {

    companion object {
        private const val BATCH_SIZE = 500
        private val TARGET_DIRS = listOf(
            "Download",
            "DCIM",
            "Pictures",
            "Movies",
            "Music",
            "Documents",
            "WhatsApp",
            "Telegram",
            "Android/media",
            "Android/data",
            ".thumbnails",
            "Backups",
            "Recordings",
        )
    }

    /**
     * Scans the filesystem and emits batches of file entities.
     * Uses iterative traversal to avoid stack overflow on deep directories.
     */
    fun scanDirectories(
        sessionId: Long,
        existingPaths: Set<String> = emptySet(),
    ): Flow<List<FileIndexEntity>> = flow {
        val root = Environment.getExternalStorageDirectory()
        val batch = mutableListOf<FileIndexEntity>()
        val stack = ArrayDeque<File>()

        // Start with priority directories first, then root
        TARGET_DIRS.forEach { dir ->
            val targetDir = File(root, dir)
            if (targetDir.exists() && targetDir.isDirectory) {
                stack.addLast(targetDir)
            }
        }
        // Also add root for full coverage
        stack.addLast(root)

        val visited = HashSet<String>()

        while (stack.isNotEmpty()) {
            yield() // Cooperative cancellation
            val current = stack.removeLast()
            val canonicalPath = current.canonicalPath

            if (canonicalPath in visited) continue
            visited.add(canonicalPath)

            if (current.isFile) {
                if (current.path !in existingPaths) {
                    val entity = fileToEntity(current, sessionId)
                    if (entity != null) {
                        batch.add(entity)
                        if (batch.size >= BATCH_SIZE) {
                            emit(batch.toList())
                            batch.clear()
                        }
                    }
                }
            } else if (current.isDirectory) {
                try {
                    current.listFiles()?.forEach { child ->
                        stack.addLast(child)
                    }
                } catch (_: SecurityException) {
                    // Skip directories we can't access
                }
            }
        }

        // Emit remaining batch
        if (batch.isNotEmpty()) {
            emit(batch.toList())
        }
    }.flowOn(Dispatchers.IO)

    private fun fileToEntity(file: File, sessionId: Long): FileIndexEntity? {
        return try {
            val ext = file.extension.lowercase()
            val mime = file.name.getMimeType()
            val category = categorizeFile(ext, mime, file.path)
            val source = detectFileSource(file.path)

            FileIndexEntity(
                path = file.absolutePath,
                name = file.name,
                extension = ext,
                mimeType = mime,
                sizeBytes = file.length(),
                createdAt = file.lastModified(),
                modifiedAt = file.lastModified(),
                category = category.name,
                source = source.name,
                parentFolder = file.parent ?: "/",
                isHidden = isHiddenPath(file.path),
                scanSessionId = sessionId,
            )
        } catch (_: Exception) {
            null
        }
    }
}
