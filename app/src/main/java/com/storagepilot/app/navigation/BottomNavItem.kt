package com.storagepilot.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation items for the main tab bar.
 */
data class BottomNavItem(
    val route: Route,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Route.Dashboard,
        label = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
    ),
    BottomNavItem(
        route = Route.Explorer(),
        label = "Files",
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder,
    ),
    BottomNavItem(
        route = Route.SwipeCleanup,
        label = "Cleanup",
        selectedIcon = Icons.Filled.SwipeRight,
        unselectedIcon = Icons.Outlined.SwipeRight,
    ),
    BottomNavItem(
        route = Route.Analytics,
        label = "Analytics",
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
    ),
)
