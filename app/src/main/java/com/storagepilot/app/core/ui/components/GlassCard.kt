package com.storagepilot.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.storagepilot.app.core.theme.GlassBorder
import com.storagepilot.app.core.theme.GlassWhite

/**
 * A glassmorphism-styled card with subtle transparency and border.
 * Used as the base container for all dashboard cards.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large

    Column(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassWhite,
                        Color.Transparent,
                    ),
                ),
                shape = shape,
            )
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                shape = shape,
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassBorder,
                        Color.Transparent,
                    ),
                ),
                shape = shape,
            )
            .padding(16.dp),
        content = content,
    )
}

/**
 * A compact glass card variant for smaller widgets.
 */
@Composable
fun GlassCardCompact(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium

    Column(
        modifier = modifier
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = shape,
            )
            .border(
                width = 0.5.dp,
                color = GlassBorder,
                shape = shape,
            )
            .padding(12.dp),
        content = content,
    )
}
