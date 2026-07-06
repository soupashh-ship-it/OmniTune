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
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Scale
import coil3.size.Size
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.LocalOmniAccents
import com.omnitune.app.ui.theme.PlayerColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fallback gradient colors — used while loading or when extraction fails.
 * Matches the static gradient from the original PlayerScreen.
 */
val PlayerFallbackGradient: List<Color>
    get() = listOf(
        OmniColors.OmniAccentGlow.copy(alpha = 0.40f),
        OmniColors.OmniBackgroundGradientTop.copy(alpha = 0.82f),
        OmniColors.OmniBackgroundElevated,
        OmniColors.OmniBackgroundBase,
    )

private val FallbackGlow = Color(0xFF8B8FFF).copy(alpha = 0.30f)

/**
 * Loads the artwork bitmap from [urls] (trying each in order) and extracts
 * gradient colors via [PlayerColorExtractor].
 */
private data class ArtworkColors(
    val gradient: List<Color>,
    val accentColor: Color,
)

private suspend fun loadArtworkColors(
    context: Context,
    urls: List<String>,
): ArtworkColors? {
    for (url in urls) {
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(Size(200, 200))
                .scale(Scale.FILL)
                .memoryCacheKey("palette:$url")
                .build()
            val result = context.imageLoader.execute(request)
            val bitmap = (result.image as? BitmapImage)?.bitmap ?: continue
            val palette = withContext(Dispatchers.Default) {
                androidx.palette.graphics.Palette.from(bitmap).generate()
            }
            val gradient = PlayerColorExtractor.extractGradientColors(
                palette = palette,
                fallbackColor = OmniColors.OmniBackgroundBase.toArgb()
            )
            val accent = gradient.firstOrNull() ?: Color(0xFF8B8FFF)
            return ArtworkColors(gradient, accent)
        } catch (_: Exception) {
            continue
        }
    }
    return null
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
    val _dynamicAccents = LocalOmniAccents.current // Force recomposition when accent colors change

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
    var extractedAccent by remember { mutableStateOf(Color(0xFF8B8FFF)) }
    var isFromArtwork by remember { mutableStateOf(false) }

    LaunchedEffect(candidates) {
        if (candidates.isEmpty()) {
            extractedColors = emptyList()
            extractedAccent = Color(0xFF8B8FFF)
            isFromArtwork = false
            return@LaunchedEffect
        }
        val colors = loadArtworkColors(
            context = context,
            urls = candidates,
        )
        if (colors != null) {
            android.util.Log.d("OmniGradient", "Extraction SUCCESS - gradient: " + colors.gradient.size + " colors, accent: #" + Integer.toHexString(colors.accentColor.toArgb()))
            extractedColors = colors.gradient
            extractedAccent = colors.accentColor
            isFromArtwork = true
        } else {
            android.util.Log.d("OmniGradient", "Extraction FAILED - null result")
            extractedColors = emptyList()
            extractedAccent = Color(0xFF8B8FFF)
            isFromArtwork = false
        }
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
        dominantColor = displayColors.firstOrNull() ?: Color(0xFF06080F),
        dynamicAccentColor = extractedAccent,
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
    val dynamicAccentColor: Color = Color(0xFF8B8FFF),
    val isFromArtwork: Boolean = false,
)
