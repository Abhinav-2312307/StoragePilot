package com.storagepilot.app.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.storagepilot.app.core.util.IntentUtils
import com.storagepilot.app.feature.analytics.AnalyticsScreen
import com.storagepilot.app.feature.dashboard.DashboardScreen
import com.storagepilot.app.feature.explorer.DocumentViewerScreen
import com.storagepilot.app.feature.explorer.ExplorerScreen
import com.storagepilot.app.feature.explorer.ImageViewerScreen
import com.storagepilot.app.feature.explorer.VideoPlayerScreen
import com.storagepilot.app.feature.scan.ScanScreen
import com.storagepilot.app.feature.swipecleanup.SwipeCleanupScreen

/**
 * Helper function for smart file routing used across multiple screens.
 * Routes to in-app viewers for supported formats, external apps for others.
 */
private fun handleFileOpen(
    path: String,
    navController: NavHostController,
    context: android.content.Context,
) {
    when (IntentUtils.getFileOpenAction(path)) {
        IntentUtils.FileOpenAction.VIDEO_PLAYER ->
            navController.navigate(Route.VideoPlayer(path))
        IntentUtils.FileOpenAction.PDF_VIEWER ->
            navController.navigate(Route.DocumentViewer(path))
        IntentUtils.FileOpenAction.TEXT_VIEWER ->
            navController.navigate(Route.DocumentViewer(path))
        IntentUtils.FileOpenAction.IMAGE_VIEWER ->
            navController.navigate(Route.ImageViewer(path))
        IntentUtils.FileOpenAction.EXTERNAL ->
            IntentUtils.openFile(context, path)
    }
}

@Composable
fun StoragePilotNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Route.Dashboard,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) },
        exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) },
    ) {
        composable<Route.Dashboard> {
            DashboardScreen(
                onNavigateToScan = { navController.navigate(Route.Scan) },
                onNavigateToCategory = { category ->
                    // Pass the category to Explorer so it opens pre-filtered
                    navController.navigate(Route.Explorer(initialCategory = category.name))
                },
                onNavigateToLargeFiles = { navController.navigate(Route.LargeFiles) },
                onNavigateToDuplicates = { navController.navigate(Route.Duplicates) },
                onNavigateToHidden = { navController.navigate(Route.HiddenStorage) },
                onNavigateToApps = { navController.navigate(Route.AppAnalyzer) },
                onNavigateToRecycleBin = { navController.navigate(Route.RecycleBin) },
                onNavigateToSearch = { navController.navigate(Route.Search) },
                onNavigateToSettings = { navController.navigate(Route.Settings) },
            )
        }

        composable<Route.Explorer> {
            ExplorerScreen(
                onNavigateToVideoPlayer = { path ->
                    navController.navigate(Route.VideoPlayer(path))
                },
                onNavigateToDocumentViewer = { path ->
                    navController.navigate(Route.DocumentViewer(path))
                },
                onNavigateToImageViewer = { path ->
                    navController.navigate(Route.ImageViewer(path))
                },
            )
        }

        composable<Route.SwipeCleanup> {
            SwipeCleanupScreen()
        }

        composable<Route.Analytics> {
            AnalyticsScreen(
                onNavigateToFlowchart = { navController.navigate(Route.SystemFlowchart) }
            )
        }

        composable<Route.Scan> {
            ScanScreen(onScanComplete = { navController.popBackStack() })
        }

        composable<Route.Duplicates> {
            com.storagepilot.app.feature.duplicates.DuplicatesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { path -> handleFileOpen(path, navController, context) }
            )
        }

        composable<Route.LargeFiles> {
            com.storagepilot.app.feature.largefiles.LargeFilesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { path -> handleFileOpen(path, navController, context) }
            )
        }

        composable<Route.AppAnalyzer> {
            com.storagepilot.app.feature.appanalyzer.AppAnalyzerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAppFiles = { packageName, appName ->
                    navController.navigate(Route.AppFiles(packageName, appName))
                }
            )
        }

        composable<Route.HiddenStorage> {
            com.storagepilot.app.feature.hidden.HiddenStorageScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { path -> handleFileOpen(path, navController, context) }
            )
        }

        composable<Route.RecycleBin> {
            com.storagepilot.app.feature.recyclebin.RecycleBinScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.Search> {
            com.storagepilot.app.feature.search.SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { path -> handleFileOpen(path, navController, context) }
            )
        }

        composable<Route.AppFiles> {
            com.storagepilot.app.feature.appanalyzer.AppFilesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.SystemFlowchart> {
            com.storagepilot.app.feature.analytics.SystemFlowchartScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.Settings> {
            com.storagepilot.app.feature.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ═══════════════════════════════════════
        // In-App Viewer Routes
        // ═══════════════════════════════════════

        composable<Route.VideoPlayer> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.VideoPlayer>()
            VideoPlayerScreen(
                filePath = args.filePath,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.DocumentViewer> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.DocumentViewer>()
            DocumentViewerScreen(
                filePath = args.filePath,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Route.ImageViewer> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.ImageViewer>()
            ImageViewerScreen(
                filePath = args.filePath,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
