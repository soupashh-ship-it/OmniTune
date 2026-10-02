package com.omnitune.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.omnitune.shared.ui.theme.DominantColors

/**
 * Animated ambient gradient background with fluid radial blobs moving smoothly across
 * the viewport and a soft vertical scrim.
 */
@Composable
fun MeshGradientBackground(
    dominantColors: DominantColors? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background
) {
    val colors = dominantColors ?: DominantColors(
        primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
        accent = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
        onBackground = MaterialTheme.colorScheme.onBackground
    )

    val animatedPrimary by animateColorAsState(
        targetValue = colors.primary,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "mesh_primary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = colors.secondary,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "mesh_secondary"
    )
    val animatedAccent by animateColorAsState(
        targetValue = colors.accent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "mesh_accent"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "mesh_gradient_motion")

    val x1 by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blob1_x"
    )
    val y1 by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blob1_y"
    )

    val x2 by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blob2_x"
    )
    val y2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blob2_y"
    )

    val x3 by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blob3_x"
    )
    val y3 by infiniteTransition.animateFloat(
        initialValue = 0.75f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blob3_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .drawWithCache {
                val scrimBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        backgroundColor.copy(alpha = 0.6f),
                        backgroundColor
                    ),
                    startY = 0f,
                    endY = size.height
                )

                onDrawBehind {
                    val w = size.width
                    val h = size.height

                    // Blob 1: Primary
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(animatedPrimary.copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(w * x1, h * y1),
                            radius = w * 1.1f
                        ),
                        radius = w * 1.1f,
                        center = Offset(w * x1, h * y1)
                    )

                    // Blob 2: Secondary
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(animatedSecondary.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(w * x2, h * y2),
                            radius = w * 1.0f
                        ),
                        radius = w * 1.0f,
                        center = Offset(w * x2, h * y2)
                    )

                    // Blob 3: Accent
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(animatedAccent.copy(alpha = 0.3f), Color.Transparent),
                            center = Offset(w * x3, h * y3),
                            radius = w * 0.9f
                        ),
                        radius = w * 0.9f,
                        center = Offset(w * x3, h * y3)
                    )

                    drawRect(brush = scrimBrush)
                }
            }
    )
}
