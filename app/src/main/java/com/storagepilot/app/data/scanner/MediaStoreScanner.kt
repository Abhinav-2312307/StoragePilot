package com.storagepilot.app.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.storagepilot.app.core.util.*
import com.storagepilot.app.data.local.entity.FileIndexEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans media files via the Android MediaStore API.
 * This is the fast path for indexed media (images, videos, audio).
 */
@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Scans all media types and emits batches of indexed file entities.
     */
    fun scanAllMedia(sessionId: Long): Flow<List<FileIndexEntity>> = flow {
        // Images
        emit(queryMediaStore(
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            sessionId = sessionId,
        ))

        // Videos
        emit(queryMediaStore(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            sessionId = sessionId,
        ))

        // Audio
        emit(queryMediaStore(
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            sessionId = sessionId,
        ))

        // Downloads (API 29+)
        emit(queryMediaStore(
            uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            sessionId = sessionId,
        ))
    }.flowOn(Dispatchers.IO)

    private fun queryMediaStore(
        uri: Uri,
        sessionId: Long,
    ): List<FileIndexEntity> {
        val files = mutableListOf<FileIndexEntity>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION,
        )

        val cursor: Cursor? = contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
        )

        cursor?.use {
            val pathCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mimeCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateAddedCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val dateModifiedCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val widthCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)

            while (it.moveToNext()) {
                val path = it.getString(pathCol) ?: continue
                val name = it.getString(nameCol) ?: continue
                val size = it.getLong(sizeCol)
                val mime = it.getString(mimeCol) ?: "application/octet-stream"
                val dateAdded = it.getLong(dateAddedCol) * 1000L
                val dateModified = it.getLong(dateModifiedCol) * 1000L
                val width = it.getInt(widthCol)
                val height = it.getInt(heightCol)
                val duration = it.getLong(durationCol)

                val ext = name.substringAfterLast('.', "").lowercase()
                val category = categorizeFile(ext, mime, path)
                val source = detectFileSource(path)

                files.add(
                    FileIndexEntity(
                        path = path,
                        name = name,
                        extension = ext,
                        mimeType = mime,
                        sizeBytes = size,
                        createdAt = dateAdded,
                        modifiedAt = dateModified,
                        category = category.name,
                        source = source.name,
                        width = width,
                        height = height,
                        durationMs = duration,
                        parentFolder = getParentFolder(path),
                        isHidden = isHiddenPath(path),
                        scanSessionId = sessionId,
                    )
                )
            }
        }

        return files
    }
}
