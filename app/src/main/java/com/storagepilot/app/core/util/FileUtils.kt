package com.storagepilot.app.core.util

import android.webkit.MimeTypeMap
import com.storagepilot.app.domain.model.FileCategory
import com.storagepilot.app.domain.model.FileSource
import java.io.File
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

/**
 * Formats byte sizes into human-readable strings.
 */
fun Long.formatFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(this.toDouble()) / Math.log10(1024.0)).toInt()
    val safeGroup = digitGroups.coerceIn(0, units.lastIndex)
    return DecimalFormat("#,##0.#").format(this / Math.pow(1024.0, safeGroup.toDouble())) + " " + units[safeGroup]
}

/**
 * Formats duration in milliseconds to mm:ss or hh:mm:ss.
 */
fun Long.formatDuration(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

/**
 * Formats a timestamp to a readable date string.
 */
fun Long.formatDate(): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(this))
}

fun Long.formatDateTime(): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy · HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(this))
}

/**
 * Formats a percentage with one decimal place.
 */
fun Float.formatPercent(): String = DecimalFormat("0.0").format(this) + "%"

/**
 * Gets the MIME type from a file extension.
 */
fun String.getMimeType(): String {
    val ext = this.substringAfterLast('.', "").lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
}

/**
 * Categorizes a file based on its extension and MIME type.
 */
fun categorizeFile(extension: String, mimeType: String, path: String): FileCategory {
    val ext = extension.lowercase()
    val mime = mimeType.lowercase()

    return when {
        // Images
        mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif", "raw", "cr2", "nef") ->
            FileCategory.IMAGES

        // Videos
        mime.startsWith("video/") || ext in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v", "ts") ->
            FileCategory.VIDEOS

        // Audio
        mime.startsWith("audio/") || ext in listOf("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus", "amr") ->
            FileCategory.AUDIO

        // Documents
        ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "json", "xml", "html", "md", "log") ->
            FileCategory.DOCUMENTS

        // Apps & APKs
        ext in listOf("apk", "xapk", "apks", "aab") ->
            FileCategory.APPS

        // Archives
        ext in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst") ->
            FileCategory.ARCHIVES

        else -> FileCategory.OTHER
    }
}

/**
 * Determines the source app from the file path.
 */
fun detectFileSource(path: String): FileSource {
    val lowerPath = path.lowercase()
    return when {
        lowerPath.contains("/dcim/camera") -> FileSource.CAMERA
        lowerPath.contains("/screenshots") -> FileSource.SCREENSHOT
        lowerPath.contains("/whatsapp") -> FileSource.WHATSAPP
        lowerPath.contains("/telegram") -> FileSource.TELEGRAM
        lowerPath.contains("/instagram") -> FileSource.INSTAGRAM
        lowerPath.contains("/download") -> FileSource.DOWNLOAD
        lowerPath.contains("/bluetooth") -> FileSource.BLUETOOTH
        else -> FileSource.UNKNOWN
    }
}

/**
 * Checks if a file or folder is hidden (starts with .).
 */
fun isHiddenPath(path: String): Boolean {
    return path.split("/").any { it.startsWith(".") && it.length > 1 }
}

/**
 * Gets the parent folder name from a full path.
 */
fun getParentFolder(path: String): String {
    return File(path).parent ?: "/"
}

/**
 * Formats a timestamp as a relative time string (e.g., "2 hours ago", "3 days ago").
 */
fun Long.formatRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
            "$mins min${if (mins > 1) "s" else ""} ago"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "$hours hour${if (hours > 1) "s" else ""} ago"
        }
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "$days day${if (days > 1) "s" else ""} ago"
        }
        else -> formatDate()
    }
}
