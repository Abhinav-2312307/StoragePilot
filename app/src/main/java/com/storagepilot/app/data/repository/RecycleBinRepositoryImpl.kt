package com.storagepilot.app.data.repository

import android.os.Environment
import com.storagepilot.app.data.local.dao.FileIndexDao
import com.storagepilot.app.data.local.dao.RecycleBinDao
import com.storagepilot.app.data.local.entity.FileIndexEntity
import com.storagepilot.app.data.local.entity.RecycleBinEntity
import com.storagepilot.app.data.preferences.UserPreferences
import com.storagepilot.app.domain.model.FileCategory
import com.storagepilot.app.domain.model.RecycleBinItem
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.repository.RecycleBinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecycleBinRepositoryImpl @Inject constructor(
    private val recycleBinDao: RecycleBinDao,
    private val fileIndexDao: FileIndexDao,
    private val userPreferences: UserPreferences,
) : RecycleBinRepository {

    companion object {
        private const val RECYCLE_DIR = ".StoragePilot_Recycle"
    }

    private val recycleBinDir: File by lazy {
        File(Environment.getExternalStorageDirectory(), RECYCLE_DIR).also { it.mkdirs() }
    }

    override fun getRecycleBinItems(): Flow<List<RecycleBinItem>> =
        recycleBinDao.getAll().map { list -> list.map(::toModel) }

    override fun getRecycleBinSize(): Flow<Long> = recycleBinDao.getTotalSize()

    override suspend fun moveToRecycleBin(files: List<ScannedFile>) {
        withContext(Dispatchers.IO) {
            val movedPaths = mutableListOf<String>()
            val entities = files.mapNotNull { file ->
                val sourceFile = File(file.path)
                if (!sourceFile.exists()) return@mapNotNull null

                val destFile = File(recycleBinDir, "${System.currentTimeMillis()}_${file.name}")
                val moved = sourceFile.renameTo(destFile)
                if (!moved) return@mapNotNull null

                movedPaths.add(file.path)

                // Read auto-delete days from user preferences
                val autoDeleteDays = userPreferences.recycleBinDays.first().toLong()

                RecycleBinEntity(
                    originalPath = file.path,
                    recyclePath = destFile.absolutePath,
                    name = file.name,
                    sizeBytes = file.sizeBytes,
                    deletedAt = System.currentTimeMillis(),
                    autoDeleteAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(autoDeleteDays),
                    category = file.category.name,
                )
            }
            recycleBinDao.insertAll(entities)
            // Remove deleted files from the file index so they don't appear as ghost entries
            if (movedPaths.isNotEmpty()) {
                fileIndexDao.deleteByPaths(movedPaths)
            }
        }
    }

    override suspend fun restoreFiles(itemIds: List<Long>) {
        withContext(Dispatchers.IO) {
            val items = recycleBinDao.getByIds(itemIds)
            items.forEach { item ->
                val recycledFile = File(item.recyclePath)
                val originalFile = File(item.originalPath)
                originalFile.parentFile?.mkdirs()
                val restored = recycledFile.renameTo(originalFile)
                // Re-add to file index so it shows up in Explorer again
                if (restored && originalFile.exists()) {
                    val category = try { FileCategory.valueOf(item.category) } catch (_: Exception) { FileCategory.OTHER }
                    fileIndexDao.insert(
                        FileIndexEntity(
                            path = item.originalPath,
                            name = item.name,
                            extension = item.name.substringAfterLast('.', ""),
                            mimeType = "",
                            sizeBytes = item.sizeBytes,
                            createdAt = originalFile.lastModified(),
                            modifiedAt = originalFile.lastModified(),
                            category = category.name,
                            parentFolder = originalFile.parent ?: "",
                            isHidden = item.name.startsWith("."),
                        )
                    )
                }
            }
            recycleBinDao.deleteByIds(itemIds)
        }
    }

    override suspend fun permanentlyDelete(itemIds: List<Long>) {
        withContext(Dispatchers.IO) {
            val items = recycleBinDao.getByIds(itemIds)
            items.forEach { File(it.recyclePath).delete() }
            recycleBinDao.deleteByIds(itemIds)
        }
    }

    override suspend fun emptyRecycleBin() {
        withContext(Dispatchers.IO) {
            recycleBinDir.listFiles()?.forEach { it.delete() }
            recycleBinDao.deleteAll()
        }
    }

    override suspend fun autoCleanExpired() {
        withContext(Dispatchers.IO) {
            val expired = recycleBinDao.getExpiredItems(System.currentTimeMillis())
            expired.forEach { File(it.recyclePath).delete() }
            recycleBinDao.deleteByIds(expired.map { it.id })
        }
    }

    private fun toModel(entity: RecycleBinEntity) = RecycleBinItem(
        id = entity.id,
        originalPath = entity.originalPath,
        recyclePath = entity.recyclePath,
        name = entity.name,
        sizeBytes = entity.sizeBytes,
        deletedAt = entity.deletedAt,
        autoDeleteAt = entity.autoDeleteAt,
        category = try { FileCategory.valueOf(entity.category) } catch (_: Exception) { FileCategory.OTHER },
    )
}
