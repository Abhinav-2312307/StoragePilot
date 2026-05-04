package com.storagepilot.app.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storagepilot.app.core.theme.*
import com.storagepilot.app.core.util.formatFileSize

/**
 * An animated ring/donut chart showing storage category breakdown.
 * Uses smooth sweep-angle animations and gradient strokes for a premium feel.
 */
@Composable
fun StorageRingChart(
    segments: List<StorageSegment>,
    usedBytes: Long,
    totalBytes: Long,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 22.dp,
    gapAngle: Float = 3f,
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(segments) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing),
        )
    }

    val totalValue = segments.sumOf { it.bytes.toDouble() }.toFloat().coerceAtLeast(1f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            val strokeWidth = ringWidth.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)

            // Background track
            drawArc(
                color = PilotSurfaceVariant,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Animated segments
            var startAngle = -90f
            segments.forEach { segment ->
                val sweepAngle = (segment.bytes / totalValue) * 360f * animProgress.value
                if (sweepAngle > 0.5f) {
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle,
                        sweepAngle = (sweepAngle - gapAngle).coerceAtLeast(0.1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
                startAngle += sweepAngle
            }

            // Subtle glow ring
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        PilotPrimary.copy(alpha = 0.1f),
                        PilotSecondary.copy(alpha = 0.05f),
                        PilotTertiary.copy(alpha = 0.1f),
                        PilotPrimary.copy(alpha = 0.05f),
                    ),
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(topLeft.x - 4f, topLeft.y - 4f),
                size = Size(arcSize.width + 8f, arcSize.height + 8f),
                style = Stroke(width = strokeWidth + 8f, cap = StrokeCap.Round),
                alpha = 0.3f * animProgress.value,
            )
        }

        // Center content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = usedBytes.formatFileSize(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "of ${totalBytes.formatFileSize()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

data class StorageSegment(
    val label: String,
    val bytes: Long,
    val color: Color,
)
