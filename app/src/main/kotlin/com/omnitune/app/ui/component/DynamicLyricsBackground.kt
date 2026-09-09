/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import coil3.compose.AsyncImage
import com.omnitune.app.ui.utils.LyricsStyle

@Composable
fun DynamicLyricsBackground(
    artworkUrl: String?,
    style: LyricsStyle,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val overlayColor = if (isDarkTheme) Color.Black else Color.White
    
    Box(modifier = modifier.fillMaxSize().background(overlayColor)) {
        val infiniteTransition = rememberInfiniteTransition(label = "blobs")
        val density = LocalDensity.current
        
        val blob1Offset by infiniteTransition.animateValue(
            initialValue = (-100).dp,
            targetValue = 100.dp,
            typeConverter = androidx.compose.ui.unit.Dp.VectorConverter,
            animationSpec = infiniteRepeatable(
                animation = tween(10000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blob1"
        )
        
        val blob2Offset by infiniteTransition.animateValue(
            initialValue = 150.dp,
            targetValue = (-150).dp,
            typeConverter = androidx.compose.ui.unit.Dp.VectorConverter,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blob2"
        )

        // Primary Blob
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset {
                    with(density) {
                        IntOffset(blob1Offset.roundToPx(), blob2Offset.roundToPx())
                    }
                }
                .alpha(0.4f)
                .blur(40.dp)
                .background(
                    when(style) {
                        LyricsStyle.Romantic -> Color(0xFFE91E63)
                        LyricsStyle.Energetic -> Color(0xFFFF5722)
                        LyricsStyle.Sad -> Color(0xFF607D8B)
                        LyricsStyle.Chill -> Color(0xFF2196F3)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    CircleShape
                )
        )

        // Secondary Blob
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset {
                    with(density) {
                        IntOffset(blob2Offset.roundToPx(), blob1Offset.roundToPx())
                    }
                }
                .alpha(0.3f)
                .blur(32.dp)
                .background(
                    when(style) {
                        LyricsStyle.Romantic -> Color(0xFFFF80AB)
                        LyricsStyle.Energetic -> Color(0xFFFFC107)
                        LyricsStyle.Sad -> Color(0xFFB0BEC5)
                        LyricsStyle.Chill -> Color(0xFF81D4FA)
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    CircleShape
                )
        )

        // Blurred Artwork
        if (artworkUrl != null) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (style is LyricsStyle.Chill) 48.dp else 40.dp)
                    .alpha(if (isDarkTheme) 0.5f else 0.3f),
                contentScale = ContentScale.Crop
            )
        }
        
        // Dynamic Overlay Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            overlayColor.copy(alpha = 0.2f),
                            overlayColor.copy(alpha = 0.6f),
                            overlayColor.copy(alpha = 0.85f)
                        )
                    )
                )
        )
    }
}
