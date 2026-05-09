package com.storagepilot.app.data.scanner

import com.storagepilot.app.data.local.dao.FileIndexDao
import com.storagepilot.app.data.local.dao.ScanResultDao
import com.storagepilot.app.data.local.entity.ScanSessionEntity
import com.storagepilot.app.domain.model.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core scanning engine that orchestrates MediaStore + FileSystem scanners.
 * Manages scan sessions, progress reporting, and database persistence.
 */
@Singleton
class StorageScanner @Inject constructor(
    private val mediaStoreScanner: MediaStoreScanner,
    private val fileSystemScanner: FileSystemScanner,
    private val fileIndexDao: FileIndexDao,
    private val scanResultDao: ScanResultDao,
) {
    private val _progress = MutableStateFlow<ScanProgress>(ScanProgress.Idle)
    val progress: StateFlow<ScanProgress> = _progress.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    /** Reference to the active scan coroutine job for cancellation support. */
    private var scanJob: Job? = null

    /**
     * Performs a full device storage scan.
     * 1. Creates a new scan session
     * 2. Clears previous index
     * 3. Scans MediaStore (fast)
     * 4. Scans filesystem (thorough)
     * 5. Detects duplicates by hash
     */
    suspend fun performFullScan() {
        if (_isScanning.value) return

        _isScanning.value = true
        _progress.value = ScanProgress.Starting

        try {
            coroutineScope {
                scanJob = coroutineContext[Job]

                val session = ScanSessionEntity(
                    startTime = System.currentTimeMillis(),
                    status = "running",
                )
                val sessionId = scanResultDao.insertSession(session)

                // Clear old data
                withContext(Dispatchers.IO) {
                    fileIndexDao.deleteAll()
                }

                var totalFiles = 0
                var totalBytes = 0L

                // Phase 1: MediaStore scan (fast, indexed media)
                mediaStoreScanner.scanAllMedia(sessionId).collect { batch ->
                    ensureActive() // Check for cancellation between batches
                    withContext(Dispatchers.IO) {
                        fileIndexDao.insertAll(batch)
                    }
                    totalFiles += batch.size
                    totalBytes += batch.sumOf { it.sizeBytes }
                    _progress.value = ScanProgress.BatchProcessed(
                        count = batch.size,
                        totalSoFar = totalFiles,
                    )
                }

                ensureActive()

                // Phase 2: FileSystem scan (non-indexed files)
                val existingPaths = withContext(Dispatchers.IO) {
                    // Get all paths already in DB to avoid duplicates
                    val allFiles = fileIndexDao.getAllFiles().first()
                    allFiles.map { it.path }.toSet()
                }

                fileSystemScanner.scanDirectories(sessionId, existingPaths).collect { batch ->
                    ensureActive() // Check for cancellation between batches
                    withContext(Dispatchers.IO) {
                        fileIndexDao.insertAll(batch)
                    }
                    totalFiles += batch.size
                    totalBytes += batch.sumOf { it.sizeBytes }
                    _progress.value = ScanProgress.Scanning(
                        currentPath = batch.lastOrNull()?.parentFolder ?: "",
                        filesScanned = totalFiles,
                    )
                }

                ensureActive()

                // Phase 3: Mark duplicates
                withContext(Dispatchers.IO) {
                    val dupHashes = fileIndexDao.getDuplicateHashes()
                    if (dupHashes.isNotEmpty()) {
                        fileIndexDao.markDuplicates(dupHashes.map { it.md5Hash })
                    }
                }

                // Finalize session
                scanResultDao.updateSession(
                    session.copy(
                        id = sessionId,
                        endTime = System.currentTimeMillis(),
                        totalFiles = totalFiles.toLong(),
                        totalSizeBytes = totalBytes,
                        status = "complete",
                    )
                )

                _progress.value = ScanProgress.Complete(
                    totalFiles = totalFiles,
                    totalBytes = totalBytes,
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            _progress.value = ScanProgress.Idle
        } catch (e: Exception) {
            _progress.value = ScanProgress.Error(e.message ?: "Unknown scanning error")
        } finally {
            _isScanning.value = false
            scanJob = null
        }
    }

    /**
     * Incremental scan — only processes files modified since last scan.
     */
    suspend fun performIncrementalScan() {
        // For now, runs a full scan. Future optimization: compare timestamps.
        performFullScan()
    }

    /**
     * Cancels the current scan by cancelling the coroutine Job.
     */
    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
        _progress.value = ScanProgress.Idle
    }
}

