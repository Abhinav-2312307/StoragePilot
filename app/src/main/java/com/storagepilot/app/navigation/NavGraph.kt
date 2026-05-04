package com.storagepilot.app.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.storagepilot.app.feature.analytics.AnalyticsScreen
import com.storagepilot.app.feature.dashboard.DashboardScreen
import com.storagepilot.app.feature.scan.ScanScreen
import com.storagepilot.app.feature.swipecleanup.SwipeCleanupScreen

@Composable
fun StoragePilotNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

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
                    // For now, route to Explorer. Phase 2 refinement later.
                    navController.navigate(Route.Explorer)
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
            com.storagepilot.app.feature.explorer.ExplorerScreen(
                onNavigateToViewer = { path ->
                    com.storagepilot.app.core.util.IntentUtils.openFile(context, path)
                }
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
                onNavigateToViewer = { path ->
                    com.storagepilot.app.core.util.IntentUtils.openFile(context, path)
                }
            )
        }

        composable<Route.LargeFiles> {
            com.storagepilot.app.feature.largefiles.LargeFilesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { path ->
                    com.storagepilot.app.core.util.IntentUtils.openFile(context, path)
                }
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
                onNavigateToViewer = { path ->
                    com.storagepilot.app.core.util.IntentUtils.openFile(context, path)
                }
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
                onNavigateToViewer = { path ->
                    com.storagepilot.app.core.util.IntentUtils.openFile(context, path)
                }
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
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
        )
    }
}


