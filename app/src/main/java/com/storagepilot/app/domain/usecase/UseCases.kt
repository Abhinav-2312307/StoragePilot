package com.storagepilot.app.domain.usecase

import com.storagepilot.app.domain.model.*
import com.storagepilot.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Initiates a full device storage scan.
 */
class ScanStorageUseCase @Inject constructor(
    private val scanRepository: ScanRepository,
) {
    fun observeProgress(): Flow<ScanProgress> = scanRepository.getScanProgress()
    fun isScanning(): Flow<Boolean> = scanRepository.isScanning()
    suspend fun startFullScan() = scanRepository.startFullScan()
    suspend fun startIncrementalScan() = scanRepository.startIncrementalScan()
    suspend fun cancelScan() = scanRepository.cancelScan()
}

/**
 * Generates the storage analytics report.
 */
class GetStorageReportUseCase @Inject constructor(
    private val fileRepository: FileRepository,
) {
    fun getCategoryBreakdown(): Flow<Map<FileCategory, Long>> =
        fileRepository.getSizeByCategory()

    fun getTotalFileCount(): Flow<Long> = fileRepository.getFileCount()
    fun getTotalSize(): Flow<Long> = fileRepository.getTotalSize()

    fun getLargestFiles(limit: Int = 20): Flow<List<ScannedFile>> =
        fileRepository.getLargeFiles(minSizeBytes = 0, limit = limit)
}

/**
 * Detects duplicate files across the storage.
 */
class DetectDuplicatesUseCase @Inject constructor(
    private val fileRepository: FileRepository,
) {
    fun getDuplicates(): Flow<List<ScannedFile>> = fileRepository.getDuplicateFiles()
}

/**
 * Handles file deletion with recycle bin support.
 */
class DeleteFilesUseCase @Inject constructor(
    private val fileRepository: FileRepository,
    private val recycleBinRepository: RecycleBinRepository,
    private val cleanupStatsRepository: CleanupStatsRepository,
) {
    suspend fun safeDelete(files: List<ScannedFile>) {
        recycleBinRepository.moveToRecycleBin(files)
        val totalBytes = files.sumOf { it.sizeBytes }
        cleanupStatsRepository.recordCleanup(
            bytesFreed = totalBytes,
            filesDeleted = files.size,
            sessionType = "manual",
        )
    }

    suspend fun permanentDelete(files: List<ScannedFile>) {
        fileRepository.deleteFiles(files.map { it.path })
        val totalBytes = files.sumOf { it.sizeBytes }
        cleanupStatsRepository.recordCleanup(
            bytesFreed = totalBytes,
            filesDeleted = files.size,
            sessionType = "permanent",
        )
    }
}

/**
 * Restores files from the recycle bin.
 */
class RestoreFilesUseCase @Inject constructor(
    private val recycleBinRepository: RecycleBinRepository,
) {
    suspend fun restore(itemIds: List<Long>) = recycleBinRepository.restoreFiles(itemIds)
    fun getRecycleBinItems(): Flow<List<RecycleBinItem>> =
        recycleBinRepository.getRecycleBinItems()
}

/**
 * Finds large files on the device.
 */
class GetLargeFilesUseCase @Inject constructor(
    private val fileRepository: FileRepository,
) {
    fun execute(minSizeMb: Long = 50, limit: Int = 100): Flow<List<ScannedFile>> =
        fileRepository.getLargeFiles(minSizeBytes = minSizeMb * 1024 * 1024, limit = limit)
}

/**
 * Searches files by various criteria.
 */
class SearchFilesUseCase @Inject constructor(
    private val fileRepository: FileRepository,
) {
    fun search(query: String): Flow<List<ScannedFile>> = fileRepository.searchFiles(query)

    fun getAllFiles(): Flow<List<ScannedFile>> = fileRepository.getAllFiles()

    fun getByCategory(category: FileCategory): Flow<List<ScannedFile>> =
        fileRepository.getFilesByCategory(category)

    fun getHidden(): Flow<List<ScannedFile>> = fileRepository.getHiddenFiles()
}

/**
 * Analyzes installed app storage.
 */
class AnalyzeAppsUseCase @Inject constructor(
    private val appStorageRepository: AppStorageRepository,
) {
    fun getAllApps(): Flow<List<AppStorageInfo>> = appStorageRepository.getInstalledApps()
    fun getCacheHeavy(): Flow<List<AppStorageInfo>> =
        appStorageRepository.getCacheHeavyApps(minCacheBytes = 10 * 1024 * 1024)
    fun getRarelyUsed(): Flow<List<AppStorageInfo>> =
        appStorageRepository.getRarelyUsedApps(daysSinceLastUse = 30)
    suspend fun refresh() = appStorageRepository.refreshAppData()
}
