/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.component.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.omnitune.app.ui.component.DominantColors

/**
 * The now-playing artwork that glass surfaces frost themselves against.
 */
val LocalGlassArtwork = compositionLocalOf<GlassArtwork?> { null }

@Immutable
data class GlassArtwork(
    val artworkUrl: String?,
    val colors: DominantColors,
    val isDarkTheme: Boolean
)

/**
 * Heavily blurred album art with a legibility scrim and a dominant-colour wash.
 */
@Composable
fun ArtworkBlurBackdrop(
    artworkUrl: String?,
    isDarkTheme: Boolean,
    dominantColors: DominantColors,
    modifier: Modifier = Modifier,
    blurRadius: Float = 60f,
    intensity: Float = 1f,
    scrimAlpha: Float = if (isDarkTheme) 0.55f else 0.40f
) {
    val i = intensity.coerceIn(0.3f, 1.5f)

    val radius = remember(blurRadius) { (blurRadius * 1.4f).coerceAtMost(80f) }
    val fallbackBlurDp = remember(blurRadius) { blurRadius * 0.9f }
    val blurEffect = remember(radius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.graphics.RenderEffect.createBlurEffect(
                radius,
                radius,
                android.graphics.Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        } else {
            null
        }
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurEffect != null) {
                        Modifier.graphicsLayer { renderEffect = blurEffect }
                    } else {
                        Modifier.blur(fallbackBlurDp.dp)
                    }
                )
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                Color.Black.copy(alpha = scrimAlpha * i * 0.7f),
                                Color.Black.copy(alpha = scrimAlpha * i),
                                Color.Black.copy(alpha = scrimAlpha * i * 1.2f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = scrimAlpha * i * 0.5f),
                                Color.White.copy(alpha = scrimAlpha * i * 0.8f),
                                Color.White.copy(alpha = scrimAlpha * i * 1.1f)
                            )
                        }
                    )
                )
        )

        // Dominant-colour wash
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            (if (isDarkTheme) dominantColors.primary else dominantColors.primary.copy(alpha = 0.5f))
                                .copy(alpha = 0.18f * i),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
