package com.storagepilot.app.domain.repository

import com.storagepilot.app.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for accessing and managing the file index.
 */
interface FileRepository {
    fun getAllFiles(): Flow<List<ScannedFile>>
    fun getFilesByCategory(category: FileCategory): Flow<List<ScannedFile>>
    fun getFilesByFolder(folderPath: String): Flow<List<ScannedFile>>
    fun getLargeFiles(minSizeBytes: Long, limit: Int = 50): Flow<List<ScannedFile>>
    fun getHiddenFiles(): Flow<List<ScannedFile>>
    fun getDuplicateFiles(): Flow<List<ScannedFile>>
    fun searchFiles(query: String): Flow<List<ScannedFile>>
    fun getFileCount(): Flow<Long>
    fun getTotalSize(): Flow<Long>
    fun getSizeByCategory(): Flow<Map<FileCategory, Long>>
    suspend fun deleteFiles(filePaths: List<String>)
    suspend fun getFileByPath(path: String): ScannedFile?
}

/**
 * Repository for managing storage scan sessions.
 */
interface ScanRepository {
    fun getScanProgress(): Flow<ScanProgress>
    suspend fun startFullScan()
    suspend fun startIncrementalScan()
    suspend fun cancelScan()
    fun isScanning(): Flow<Boolean>
    fun getLastScanTime(): Flow<Long?>
}

/**
 * Repository for recycle bin operations.
 */
interface RecycleBinRepository {
    fun getRecycleBinItems(): Flow<List<RecycleBinItem>>
    fun getRecycleBinSize(): Flow<Long>
    suspend fun moveToRecycleBin(files: List<ScannedFile>)
    suspend fun restoreFiles(itemIds: List<Long>)
    suspend fun permanentlyDelete(itemIds: List<Long>)
    suspend fun emptyRecycleBin()
    suspend fun autoCleanExpired()
}

/**
 * Repository for app storage analysis.
 */
interface AppStorageRepository {
    fun getInstalledApps(): Flow<List<AppStorageInfo>>
    fun getCacheHeavyApps(minCacheBytes: Long): Flow<List<AppStorageInfo>>
    fun getRarelyUsedApps(daysSinceLastUse: Int): Flow<List<AppStorageInfo>>
    suspend fun refreshAppData()
}

/**
 * Repository for cleanup statistics.
 */
interface CleanupStatsRepository {
    fun getLifetimeStats(): Flow<CleanupSession>
    fun getRecentSessions(limit: Int): Flow<List<CleanupSession>>
    suspend fun recordCleanup(bytesFreed: Long, filesDeleted: Int, sessionType: String)
}
