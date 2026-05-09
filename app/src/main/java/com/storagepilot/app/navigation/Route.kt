package com.storagepilot.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for StoragePilot.
 */
@Serializable
sealed interface Route {
    @Serializable data object Dashboard : Route
    @Serializable data class Explorer(val initialCategory: String? = null) : Route
    @Serializable data object SwipeCleanup : Route
    @Serializable data object Analytics : Route
    @Serializable data object Scan : Route
    @Serializable data object Duplicates : Route
    @Serializable data object LargeFiles : Route
    @Serializable data object AppAnalyzer : Route
    @Serializable data object HiddenStorage : Route
    @Serializable data object RecycleBin : Route
    @Serializable data object Search : Route
    @Serializable data class AppFiles(val packageName: String, val appName: String) : Route
    @Serializable data object SystemFlowchart : Route
    @Serializable data object Settings : Route
    @Serializable data class VideoPlayer(val filePath: String) : Route
    @Serializable data class DocumentViewer(val filePath: String) : Route
    @Serializable data class ImageViewer(val filePath: String) : Route
}
