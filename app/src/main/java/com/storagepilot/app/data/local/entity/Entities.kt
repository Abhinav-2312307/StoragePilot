package com.storagepilot.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an indexed file in the storage scan database.
 */
@Entity(
    tableName = "file_index",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["category"]),
        Index(value = ["sizeBytes"]),
        Index(value = ["modifiedAt"]),
        Index(value = ["md5Hash"]),
        Index(value = ["parentFolder"]),
        Index(value = ["isHidden"]),
        Index(value = ["isDuplicate"]),
        Index(value = ["extension"]),
    ],
)
data class FileIndexEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val name: String,
    val extension: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long,
    val modifiedAt: Long,
    val category: String,
    val source: String = "UNKNOWN",
    val md5Hash: String? = null,
    val perceptualHash: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0,
    val parentFolder: String,
    val isHidden: Boolean = false,
    val isDuplicate: Boolean = false,
    val scanSessionId: Long = 0,
)

/**
 * Room entity representing a scan session.
 */
@Entity(tableName = "scan_session")
data class ScanSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long = 0,
    val totalFiles: Long = 0,
    val totalSizeBytes: Long = 0,
    val status: String = "running",
)

/**
 * Room entity for the recycle bin.
 */
@Entity(
    tableName = "recycle_bin",
    indices = [Index(value = ["originalPath"], unique = true)],
)
data class RecycleBinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val recyclePath: String,
    val name: String,
    val sizeBytes: Long,
    val deletedAt: Long,
    val autoDeleteAt: Long,
    val category: String,
)

/**
 * Room entity for tracking cleanup statistics.
 */
@Entity(tableName = "cleanup_stats")
data class CleanupStatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val bytesFreed: Long,
    val filesDeleted: Int,
    val sessionType: String,
)
