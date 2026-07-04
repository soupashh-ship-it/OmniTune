/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.size.Scale
import coil3.size.Size
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.PlayerColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fallback gradient colors — used while loading or when extraction fails.
 * Matches the static gradient from the original PlayerScreen.
 */
val PlayerFallbackGradient: List<Color> = listOf(
    OmniColors.OmniBackgroundGradientTop.copy(alpha = 0.82f),
    OmniColors.OmniBackgroundElevated,
    OmniColors.OmniBackgroundBase,
)

private val FallbackGlow = OmniColors.OmniAccentGlow

/**
 * Loads the artwork bitmap from [urls] (trying each in order) and extracts
 * gradient colors via [PlayerColorExtractor].
 */
private suspend fun loadArtworkGradient(
    context: Context,
    urls: List<String>,
): List<Color> {
    for (url in urls) {
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(Size(200, 200))
                .scale(Scale.FILL)
                .memoryCacheKey("palette:$url")
                .build()
            val result = SingletonImageLoader.get(context).execute(request)
            val bitmap = (result.image as? BitmapImage)?.bitmap ?: continue
            val palette = withContext(Dispatchers.Default) {
                PlayerColorExtractor.generatePalette(bitmap)
            }
            return PlayerColorExtractor.extractGradientColors(
                palette = palette,
                fallbackArgb = OmniColors.OmniBackgroundBase.toArgb(),
            )
        } catch (_: Exception) {
            continue
        }
    }
    return emptyList()
}

/**
 * Observes [thumbnailUrl] / [videoId] and loads gradient colors from artwork.
 *
 * Returns a [PlayerGradientState] with the background brush and accent glow color.
 */
@Composable
fun rememberPlayerGradient(
    thumbnailUrl: String?,
    videoId: String?,
): PlayerGradientState {
    val context = LocalContext.current

    val candidates = remember(thumbnailUrl, videoId) {
        buildList {
            if (videoId?.matches(Regex("^[a-zA-Z0-9_-]{11}$")) == true) {
                add("https://i.ytimg.com/vi/$videoId/maxresdefault.jpg")
                add("https://i.ytimg.com/vi/$videoId/sddefault.jpg")
            }
            if (!thumbnailUrl.isNullOrBlank()) {
                add(thumbnailUrl)
            }
        }
    }

    var extractedColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    var isFromArtwork by remember { mutableStateOf(false) }

    LaunchedEffect(candidates) {
        if (candidates.isEmpty()) {
            extractedColors = emptyList()
            isFromArtwork = false
            return@LaunchedEffect
        }
        val colors = loadArtworkGradient(
            context = context,
            urls = candidates,
        )
        extractedColors = colors
        isFromArtwork = colors.isNotEmpty()
    }

    val displayColors = if (isFromArtwork) extractedColors else PlayerFallbackGradient

    val accentGlow = if (isFromArtwork) {
        extractedColors.first().copy(alpha = 0.15f)
    } else {
        FallbackGlow.copy(alpha = 0.16f)
    }

    return PlayerGradientState(
        backgroundBrush = Brush.verticalGradient(displayColors),
        accentGlow = accentGlow,
        dominantColor = displayColors.firstOrNull() ?: OmniColors.OmniBackgroundBase,
        isFromArtwork = isFromArtwork,
    )
}

/**
 * State holder for the dynamically extracted player background.
 *
 * @property isFromArtwork Whether colors were successfully extracted from artwork.
 *                         False when using fallback colors (loading/error/no artwork).
 */
data class PlayerGradientState(
    val backgroundBrush: Brush,
    val accentGlow: Color,
    val dominantColor: Color,
    val isFromArtwork: Boolean = false,
)
