package com.storagepilot.app.core.util

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object HashUtils {

    /**
     * Calculates MD5 hash for the first 8KB of a file (fast partial hash).
     */
    fun calculatePartialHash(file: File): String? {
        if (!file.exists() || !file.canRead()) return null
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { input ->
                val read = input.read(buffer)
                if (read > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculates MD5 hash for the entire file (slow but accurate).
     */
    fun calculateFullHash(file: File): String? {
        if (!file.exists() || !file.canRead()) return null
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { input ->
                var read = input.read(buffer)
                while (read != -1) {
                    digest.update(buffer, 0, read)
                    read = input.read(buffer)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
