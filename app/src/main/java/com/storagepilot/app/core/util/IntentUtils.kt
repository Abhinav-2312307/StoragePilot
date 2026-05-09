package com.storagepilot.app.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object IntentUtils {

    // Text-based extensions that can be viewed in-app
    val TEXT_EXTENSIONS = setOf(
        "txt", "json", "xml", "csv", "md", "log", "html", "htm",
        "css", "js", "kt", "java", "py", "c", "cpp", "h", "swift",
        "yaml", "yml", "toml", "ini", "cfg", "conf", "sh", "bat",
        "sql", "properties", "gradle", "pro", "gitignore"
    )

    val PDF_EXTENSION = "pdf"

    val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v", "ts"
    )

    /**
     * Opens a file using the device's default application for its MIME type.
     */
    fun openFile(context: Context, path: String) {
        try {
            val file = File(path)
            if (!file.exists()) {
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
                return
            }

            // Get URI using FileProvider defined in AndroidManifest.xml
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            // Determine MimeType directly from file extension (more reliable than parsing URI)
            val extension = file.extension.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: "application/octet-stream"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Create chooser to let user pick if multiple apps exist
            val chooser = Intent.createChooser(intent, "Open with")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    /**
     * Opens the parent folder of a file in the device's file manager.
     */
    fun openFolder(context: Context, folderPath: String) {
        try {
            // Try using DocumentsUI (the most reliable approach on modern Android)
            val storagePath = "/storage/emulated/0/"
            if (folderPath.startsWith(storagePath)) {
                val relativePath = folderPath.removePrefix(storagePath)
                val encodedPath = Uri.encode(relativePath, "/")
                val uri = Uri.parse(
                    "content://com.android.externalstorage.documents/document/primary%3A${
                        relativePath.replace("/", "%2F")
                    }"
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {
                    // Fall through to fallback
                }
            }

            // Fallback: Try generic file manager intent
            val uri = Uri.parse("file://$folderPath")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "resource/folder")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                // Last resort: Open the Files app / any file manager
                try {
                    val filesIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("content://com.android.externalstorage.documents/root/primary")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(filesIntent)
                } catch (_: Exception) {
                    Toast.makeText(context, "No file manager found", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open folder", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares a file via the system share sheet.
     */
    fun shareFile(context: Context, path: String) {
        try {
            val file = File(path)
            if (!file.exists()) {
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val extension = file.extension.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: "application/octet-stream"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Share via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share file", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Determines how a file should be opened based on its extension.
     */
    fun getFileOpenAction(path: String): FileOpenAction {
        val extension = File(path).extension.lowercase()
        return when {
            extension == PDF_EXTENSION -> FileOpenAction.PDF_VIEWER
            extension in VIDEO_EXTENSIONS -> FileOpenAction.VIDEO_PLAYER
            extension in TEXT_EXTENSIONS -> FileOpenAction.TEXT_VIEWER
            extension in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif") ->
                FileOpenAction.IMAGE_VIEWER
            else -> FileOpenAction.EXTERNAL
        }
    }

    enum class FileOpenAction {
        VIDEO_PLAYER,
        PDF_VIEWER,
        TEXT_VIEWER,
        IMAGE_VIEWER,
        EXTERNAL,
    }
}
