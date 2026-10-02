package com.omnitune.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Reusable album artwork and thumbnail image loader with squircle clipping,
 * fallback icon, and shadow elevation support.
 */
@Composable
fun OmniArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = SquircleShape(cornerRadius = 12.dp, cornerSmoothing = 0.6f),
    elevation: Dp = 0.dp,
    contentDescription: String? = null
) {
    val containerModifier = if (elevation > 0.dp) {
        modifier
            .size(size)
            .shadow(elevation, shape)
            .clip(shape)
    } else {
        modifier
            .size(size)
            .clip(shape)
    }

    Box(
        modifier = containerModifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = contentDescription ?: "Music placeholder",
                modifier = Modifier.size(size * 0.5f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
