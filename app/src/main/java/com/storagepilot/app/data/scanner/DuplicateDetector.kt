package com.storagepilot.app.data.scanner

import com.storagepilot.app.core.util.HashUtils
import com.storagepilot.app.data.local.dao.FileIndexDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicateDetector @Inject constructor(
    private val fileIndexDao: FileIndexDao,
) {
    /**
     * Finds duplicates using a multi-stage process:
     * 1. Group by size
     * 2. For same-sized files, compute partial hash (first 8KB)
     * 3. For partial-hash collisions, compute full MD5
     */
    suspend fun findAndMarkDuplicates() = withContext(Dispatchers.IO) {
        val allFiles = fileIndexDao.getAllFiles().first()
        
        // Stage 1: Group by size (>0 bytes)
        val filesBySize = allFiles.filter { it.sizeBytes > 0 }.groupBy { it.sizeBytes }
        val potentialDuplicates = filesBySize.filter { it.value.size > 1 }

        val duplicateHashes = mutableMapOf<String, MutableList<Long>>()
        val entitiesToUpdate = mutableListOf<com.storagepilot.app.data.local.entity.FileIndexEntity>()

        for ((_, group) in potentialDuplicates) {
            // Stage 2: Partial Hash
            val partialGroups = group.groupBy { file ->
                HashUtils.calculatePartialHash(File(file.path)) ?: "error_${file.id}"
            }.filter { !it.key.startsWith("error_") && it.value.size > 1 }

            for ((_, partialGroup) in partialGroups) {
                // Stage 3: Full Hash
                for (file in partialGroup) {
                    val fullHash = HashUtils.calculateFullHash(File(file.path))
                    if (fullHash != null) {
                        duplicateHashes.getOrPut(fullHash) { mutableListOf() }.add(file.id)
                        entitiesToUpdate.add(file.copy(md5Hash = fullHash))
                    }
                }
            }
        }

        // Save computed hashes
        if (entitiesToUpdate.isNotEmpty()) {
            fileIndexDao.insertAll(entitiesToUpdate)
        }

        // Mark duplicates in DB
        val confirmedDuplicateHashes = duplicateHashes.filter { it.value.size > 1 }.keys.toList()
        if (confirmedDuplicateHashes.isNotEmpty()) {
            fileIndexDao.markDuplicates(confirmedDuplicateHashes)
        }
    }
}
