package com.storagepilot.app.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// Extended Color System for Storage Categories
// ═══════════════════════════════════════════════════════════════

data class StoragePilotColors(
    val surfaceElevated: Color,
    val images: Color,
    val videos: Color,
    val audio: Color,
    val documents: Color,
    val apps: Color,
    val archives: Color,
    val downloads: Color,
    val other: Color,
    val cache: Color,
    val duplicates: Color,
    val hidden: Color,
    val healthy: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val glassAlpha: Float,
    val glowAlpha: Float,
)

val LocalStoragePilotColors = staticCompositionLocalOf {
    StoragePilotColors(
        surfaceElevated = PilotSurfaceElevated,
        images = CategoryImages,
        videos = CategoryVideos,
        audio = CategoryAudio,
        documents = CategoryDocuments,
        apps = CategoryApps,
        archives = CategoryArchives,
        downloads = CategoryDownloads,
        other = CategoryOther,
        cache = CategoryCache,
        duplicates = CategoryDuplicates,
        hidden = CategoryHidden,
        healthy = StatusHealthy,
        warning = StatusWarning,
        danger = StatusDanger,
        info = StatusInfo,
        glassAlpha = 0.08f,
        glowAlpha = 0.3f,
    )
}

private val StoragePilotDarkScheme = darkColorScheme(
    primary = PilotPrimary,
    onPrimary = PilotBlack,
    primaryContainer = PilotPrimaryDark,
    onPrimaryContainer = TextWhite,
    secondary = PilotSecondary,
    onSecondary = PilotBlack,
    secondaryContainer = PilotSecondaryDark,
    onSecondaryContainer = TextWhite,
    tertiary = PilotTertiary,
    onTertiary = PilotBlack,
    tertiaryContainer = PilotTertiaryDark,
    onTertiaryContainer = TextWhite,
    background = PilotBlack,
    onBackground = TextPrimary,
    surface = PilotSurface,
    onSurface = TextPrimary,
    surfaceVariant = PilotSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
    onError = TextWhite,
    outline = Color.White.copy(alpha = 0.12f),
    outlineVariant = Color.White.copy(alpha = 0.06f),
)

@Composable
fun StoragePilotTheme(
    content: @Composable () -> Unit,
) {
    val extendedColors = StoragePilotColors(
        surfaceElevated = PilotSurfaceElevated,
        images = CategoryImages,
        videos = CategoryVideos,
        audio = CategoryAudio,
        documents = CategoryDocuments,
        apps = CategoryApps,
        archives = CategoryArchives,
        downloads = CategoryDownloads,
        other = CategoryOther,
        cache = CategoryCache,
        duplicates = CategoryDuplicates,
        hidden = CategoryHidden,
        healthy = StatusHealthy,
        warning = StatusWarning,
        danger = StatusDanger,
        info = StatusInfo,
        glassAlpha = 0.08f,
        glowAlpha = 0.3f,
    )

    CompositionLocalProvider(
        LocalStoragePilotColors provides extendedColors,
    ) {
        MaterialTheme(
            colorScheme = StoragePilotDarkScheme,
            typography = StoragePilotTypography,
            shapes = StoragePilotShapes,
            content = content,
        )
    }
}

object PilotColors {
    val extended: StoragePilotColors
        @Composable
        get() = LocalStoragePilotColors.current
}
