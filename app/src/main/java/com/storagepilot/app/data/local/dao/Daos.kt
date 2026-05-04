package com.storagepilot.app.data.local.dao

import androidx.room.*
import com.storagepilot.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for file index operations — the core table for all storage analysis.
 */
@Dao
interface FileIndexDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<FileIndexEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: FileIndexEntity): Long

    @Update
    suspend fun update(file: FileIndexEntity)

    @Delete
    suspend fun delete(file: FileIndexEntity)

    @Query("DELETE FROM file_index WHERE path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    @Query("DELETE FROM file_index")
    suspend fun deleteAll()

    @Query("SELECT * FROM file_index ORDER BY modifiedAt DESC")
    fun getAllFiles(): Flow<List<FileIndexEntity>>

    @Query("SELECT * FROM file_index WHERE category = :category ORDER BY sizeBytes DESC")
    fun getFilesByCategory(category: String): Flow<List<FileIndexEntity>>

    @Query("SELECT * FROM file_index WHERE parentFolder = :folder ORDER BY name ASC")
    fun getFilesByFolder(folder: String): Flow<List<FileIndexEntity>>

    @Query("SELECT * FROM file_index WHERE sizeBytes >= :minSize ORDER BY sizeBytes DESC LIMIT :limit")
    fun getLargeFiles(minSize: Long, limit: Int): Flow<List<FileIndexEntity>>

    @Query("SELECT * FROM file_index WHERE isHidden = 1 ORDER BY sizeBytes DESC")
    fun getHiddenFiles(): Flow<List<FileIndexEntity>>

    @Query("SELECT * FROM file_index WHERE isDuplicate = 1 ORDER BY md5Hash, sizeBytes DESC")
    fun getDuplicateFiles(): Flow<List<FileIndexEntity>>

    @Query("""
        SELECT * FROM file_index 
        WHERE name LIKE '%' || :query || '%' 
           OR path LIKE '%' || :query || '%'
           OR extension LIKE '%' || :query || '%'
        ORDER BY modifiedAt DESC
        LIMIT 200
    """)
    fun searchFiles(query: String): Flow<List<FileIndexEntity>>

    @Query("SELECT COUNT(*) FROM file_index")
    fun getFileCount(): Flow<Long>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM file_index")
    fun getTotalSize(): Flow<Long>

    @Query("SELECT category, COALESCE(SUM(sizeBytes), 0) as totalSize FROM file_index GROUP BY category")
    fun getSizeByCategory(): Flow<List<CategorySize>>

    @Query("SELECT * FROM file_index WHERE path = :path LIMIT 1")
    suspend fun getFileByPath(path: String): FileIndexEntity?

    @Query("SELECT md5Hash, COUNT(*) as cnt FROM file_index WHERE md5Hash IS NOT NULL GROUP BY md5Hash HAVING cnt > 1")
    suspend fun getDuplicateHashes(): List<DuplicateHashCount>

    @Query("UPDATE file_index SET isDuplicate = 1 WHERE md5Hash IN (:hashes)")
    suspend fun markDuplicates(hashes: List<String>)

    @Query("SELECT DISTINCT parentFolder FROM file_index ORDER BY parentFolder")
    fun getAllFolders(): Flow<List<String>>
}

/**
 * Projection for category-based size aggregation.
 */
data class CategorySize(
    val category: String,
    val totalSize: Long,
)

/**
 * Projection for finding duplicate hash groups.
 */
data class DuplicateHashCount(
    val md5Hash: String,
    val cnt: Int,
)

// ═══════════════════════════════════════════════════════════════

/**
 * DAO for scan session management.
 */
@Dao
interface ScanResultDao {

    @Insert
    suspend fun insertSession(session: ScanSessionEntity): Long

    @Update
    suspend fun updateSession(session: ScanSessionEntity)

    @Query("SELECT * FROM scan_session ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSession(): ScanSessionEntity?

    @Query("SELECT * FROM scan_session ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ScanSessionEntity>>
}

// ═══════════════════════════════════════════════════════════════

/**
 * DAO for recycle bin operations.
 */
@Dao
interface RecycleBinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RecycleBinEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecycleBinEntity>)

    @Delete
    suspend fun delete(item: RecycleBinEntity)

    @Query("DELETE FROM recycle_bin WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM recycle_bin")
    suspend fun deleteAll()

    @Query("SELECT * FROM recycle_bin ORDER BY deletedAt DESC")
    fun getAll(): Flow<List<RecycleBinEntity>>

    @Query("SELECT * FROM recycle_bin WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<RecycleBinEntity>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM recycle_bin")
    fun getTotalSize(): Flow<Long>

    @Query("SELECT * FROM recycle_bin WHERE autoDeleteAt <= :currentTime")
    suspend fun getExpiredItems(currentTime: Long): List<RecycleBinEntity>
}

// ═══════════════════════════════════════════════════════════════

/**
 * DAO for cleanup statistics.
 */
@Dao
interface CleanupStatsDao {

    @Insert
    suspend fun insert(stat: CleanupStatEntity)

    @Query("SELECT COALESCE(SUM(bytesFreed), 0) as bytesFreed, COALESCE(SUM(filesDeleted), 0) as filesDeleted FROM cleanup_stats")
    fun getLifetimeStats(): Flow<LifetimeStatsProjection>

    @Query("SELECT * FROM cleanup_stats ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<CleanupStatEntity>>
}

/**
 * Projection for aggregated lifetime cleanup stats.
 */
data class LifetimeStatsProjection(
    val bytesFreed: Long,
    val filesDeleted: Int,
)
