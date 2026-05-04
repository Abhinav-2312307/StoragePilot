package com.storagepilot.app.data.repository

import com.storagepilot.app.data.local.dao.FileIndexDao
import com.storagepilot.app.data.local.entity.FileIndexEntity
import com.storagepilot.app.domain.model.FileCategory
import com.storagepilot.app.domain.model.FileSource
import com.storagepilot.app.domain.model.ScannedFile
import com.storagepilot.app.domain.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val fileIndexDao: FileIndexDao,
) : FileRepository {

    override fun getAllFiles(): Flow<List<ScannedFile>> =
        fileIndexDao.getAllFiles().map { it.map(::toModel) }

    override fun getFilesByCategory(category: FileCategory): Flow<List<ScannedFile>> =
        fileIndexDao.getFilesByCategory(category.name).map { it.map(::toModel) }

    override fun getFilesByFolder(folderPath: String): Flow<List<ScannedFile>> =
        fileIndexDao.getFilesByFolder(folderPath).map { it.map(::toModel) }

    override fun getLargeFiles(minSizeBytes: Long, limit: Int): Flow<List<ScannedFile>> =
        fileIndexDao.getLargeFiles(minSizeBytes, limit).map { it.map(::toModel) }

    override fun getHiddenFiles(): Flow<List<ScannedFile>> =
        fileIndexDao.getHiddenFiles().map { it.map(::toModel) }

    override fun getDuplicateFiles(): Flow<List<ScannedFile>> =
        fileIndexDao.getDuplicateFiles().map { it.map(::toModel) }

    override fun searchFiles(query: String): Flow<List<ScannedFile>> =
        fileIndexDao.searchFiles(query).map { it.map(::toModel) }

    override fun getFileCount(): Flow<Long> = fileIndexDao.getFileCount()

    override fun getTotalSize(): Flow<Long> = fileIndexDao.getTotalSize()

    override fun getSizeByCategory(): Flow<Map<FileCategory, Long>> =
        fileIndexDao.getSizeByCategory().map { list ->
            list.associate { cs ->
                val cat = try { FileCategory.valueOf(cs.category) } catch (_: Exception) { FileCategory.OTHER }
                cat to cs.totalSize
            }
        }

    override suspend fun deleteFiles(filePaths: List<String>) {
        withContext(Dispatchers.IO) {
            filePaths.forEach { path ->
                try {
                    File(path).delete()
                } catch (_: Exception) { }
            }
            fileIndexDao.deleteByPaths(filePaths)
        }
    }

    override suspend fun getFileByPath(path: String): ScannedFile? =
        fileIndexDao.getFileByPath(path)?.let(::toModel)

    private fun toModel(entity: FileIndexEntity): ScannedFile = ScannedFile(
        id = entity.id,
        path = entity.path,
        name = entity.name,
        extension = entity.extension,
        mimeType = entity.mimeType,
        sizeBytes = entity.sizeBytes,
        createdAt = entity.createdAt,
        modifiedAt = entity.modifiedAt,
        category = try { FileCategory.valueOf(entity.category) } catch (_: Exception) { FileCategory.OTHER },
        source = try { FileSource.valueOf(entity.source) } catch (_: Exception) { FileSource.UNKNOWN },
        md5Hash = entity.md5Hash,
        perceptualHash = entity.perceptualHash,
        width = entity.width,
        height = entity.height,
        durationMs = entity.durationMs,
        parentFolder = entity.parentFolder,
        isHidden = entity.isHidden,
        isDuplicate = entity.isDuplicate,
    )
}
