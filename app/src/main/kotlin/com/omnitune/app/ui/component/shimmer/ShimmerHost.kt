/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.component.shimmer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.valentinilk.shimmer.defaultShimmerTheme
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer

/**
 * Custom shimmer theme for OmniTune.
 * Uses a smooth 900ms animation with a 200ms delay between cycles.
 * The shader colors use OmniTune's glass tokens for a subtle, music-first aesthetic.
 */
val OmniShimmerTheme = defaultShimmerTheme.copy(
    animationSpec = infiniteRepeatable(
        animation = tween(
            durationMillis = 900,
            easing = LinearEasing,
            delayMillis = 200,
        ),
        repeatMode = RepeatMode.Restart,
    ),
    shaderColors = listOf(
        Color.White.copy(alpha = 0.06f),
        Color.White.copy(alpha = 0.14f),
        Color.White.copy(alpha = 0.06f),
    ),
)

/**
 * Wraps [content] in a Column with a shimmer animation overlay.
 * Use inside loading states to animate placeholder shapes.
 *
 * Example:
 * ```kotlin
 * ShimmerHost {
 *     repeat(3) { OmniShimmerTrackRow() }
 * }
 * ```
 */
@Composable
fun ShimmerHost(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.View, theme = OmniShimmerTheme)
    Column(
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        modifier = modifier.shimmer(shimmer),
        content = content,
    )
}


