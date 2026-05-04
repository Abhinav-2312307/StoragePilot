package com.storagepilot.app.data.repository

import android.os.Environment
import com.storagepilot.app.data.local.dao.RecycleBinDao
import com.storagepilot.app.data.local.entity.RecycleBinEntity
import com.storagepilot.app.domain.model.FileCategory
import com.storagepilot.app.domain.model.RecycleBinItem
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.repository.RecycleBinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecycleBinRepositoryImpl @Inject constructor(
    private val recycleBinDao: RecycleBinDao,
) : RecycleBinRepository {

    companion object {
        private const val RECYCLE_DIR = ".StoragePilot_Recycle"
        private val AUTO_DELETE_DAYS = 30L
    }

    private val recycleBinDir: File by lazy {
        File(Environment.getExternalStorageDirectory(), RECYCLE_DIR).also { it.mkdirs() }
    }

    override fun getRecycleBinItems(): Flow<List<RecycleBinItem>> =
        recycleBinDao.getAll().map { list -> list.map(::toModel) }

    override fun getRecycleBinSize(): Flow<Long> = recycleBinDao.getTotalSize()

    override suspend fun moveToRecycleBin(files: List<ScannedFile>) {
        withContext(Dispatchers.IO) {
            val entities = files.mapNotNull { file ->
                val sourceFile = File(file.path)
                if (!sourceFile.exists()) return@mapNotNull null

                val destFile = File(recycleBinDir, "${System.currentTimeMillis()}_${file.name}")
                val moved = sourceFile.renameTo(destFile)
                if (!moved) return@mapNotNull null

                RecycleBinEntity(
                    originalPath = file.path,
                    recyclePath = destFile.absolutePath,
                    name = file.name,
                    sizeBytes = file.sizeBytes,
                    deletedAt = System.currentTimeMillis(),
                    autoDeleteAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(AUTO_DELETE_DAYS),
                    category = file.category.name,
                )
            }
            recycleBinDao.insertAll(entities)
        }
    }

    override suspend fun restoreFiles(itemIds: List<Long>) {
        withContext(Dispatchers.IO) {
            val items = recycleBinDao.getByIds(itemIds)
            items.forEach { item ->
                val recycledFile = File(item.recyclePath)
                val originalFile = File(item.originalPath)
                originalFile.parentFile?.mkdirs()
                recycledFile.renameTo(originalFile)
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
