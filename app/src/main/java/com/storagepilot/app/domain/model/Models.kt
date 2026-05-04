package com.storagepilot.app.domain.model

/**
 * Categories for organizing scanned files.
 */
enum class FileCategory(val displayName: String) {
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    APPS("Apps & APKs"),
    ARCHIVES("Archives"),
    DOWNLOADS("Downloads"),
    CACHE("Cache"),
    HIDDEN("Hidden"),
    DUPLICATES("Duplicates"),
    OTHER("Other"),
    SYSTEM("System & OS"),
    INSTALLED_APPS("Apps"),
}

/**
 * Source application that produced the file.
 */
enum class FileSource(val displayName: String) {
    CAMERA("Camera"),
    SCREENSHOT("Screenshots"),
    WHATSAPP("WhatsApp"),
    TELEGRAM("Telegram"),
    INSTAGRAM("Instagram"),
    DOWNLOAD("Downloads"),
    BLUETOOTH("Bluetooth"),
    UNKNOWN("Unknown"),
}

/**
 * Represents a fully analyzed file from device storage.
 */
data class ScannedFile(
    val id: Long = 0,
    val path: String,
    val name: String,
    val extension: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long,
    val modifiedAt: Long,
    val category: FileCategory,
    val source: FileSource = FileSource.UNKNOWN,
    val md5Hash: String? = null,
    val perceptualHash: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0,
    val parentFolder: String,
    val isHidden: Boolean = false,
    val isDuplicate: Boolean = false,
)

/**
 * Storage analytics report.
 */
data class StorageReport(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val imageBytes: Long = 0,
    val videoBytes: Long = 0,
    val audioBytes: Long = 0,
    val documentBytes: Long = 0,
    val appBytes: Long = 0,
    val archiveBytes: Long = 0,
    val downloadBytes: Long = 0,
    val cacheBytes: Long = 0,
    val hiddenBytes: Long = 0,
    val duplicateBytes: Long = 0,
    val otherBytes: Long = 0,
    val totalFiles: Long = 0,
    val largestFiles: List<ScannedFile> = emptyList(),
    val categoryBreakdown: Map<FileCategory, Long> = emptyMap(),
)

/**
 * Group of duplicate files sharing the same content.
 */
data class DuplicateGroup(
    val hash: String,
    val files: List<ScannedFile>,
    val totalWastedBytes: Long,
)

/**
 * Installed application storage analysis.
 */
data class AppStorageInfo(
    val packageName: String,
    val appName: String,
    val appSizeBytes: Long,
    val dataSizeBytes: Long,
    val cacheSizeBytes: Long,
    val totalSizeBytes: Long,
    val lastUsedTimestamp: Long,
    val isSystemApp: Boolean,
)

/**
 * Cleanup session tracking.
 */
data class CleanupSession(
    val id: Long = 0,
    val timestamp: Long,
    val bytesFreed: Long,
    val filesDeleted: Int,
    val sessionType: String,
)

/**
 * Progress state for the scanning engine.
 */
sealed class ScanProgress {
    data object Idle : ScanProgress()
    data object Starting : ScanProgress()
    data class Scanning(
        val currentPath: String,
        val filesScanned: Int,
        val totalEstimate: Int = 0,
    ) : ScanProgress()
    data class BatchProcessed(val count: Int, val totalSoFar: Int) : ScanProgress()
    data class Complete(val totalFiles: Int, val totalBytes: Long) : ScanProgress()
    data class Error(val message: String) : ScanProgress()
}

/**
 * Recycle bin entry for safe deletion.
 */
data class RecycleBinItem(
    val id: Long = 0,
    val originalPath: String,
    val recyclePath: String,
    val name: String,
    val sizeBytes: Long,
    val deletedAt: Long,
    val autoDeleteAt: Long,
    val category: FileCategory,
)
