/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import com.omnitune.app.constants.DynamicSongColorsKey
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.LocalOmniAccents
import com.omnitune.app.ui.theme.OmniDynamicSongPalette
import com.omnitune.app.ui.theme.PlayerColorExtractor
import com.omnitune.app.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

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

private val FallbackGlow = Color(0xFFED5564).copy(alpha = 0.30f)
private val songPaletteCache = ConcurrentHashMap<String, ArtworkColors>()

/**
 * Loads the artwork bitmap from [urls] (trying each in order) and extracts
 * gradient colors via [PlayerColorExtractor].
 */
private data class ArtworkColors(
    val gradient: List<Color>,
    val palette: OmniDynamicSongPalette,
)

private suspend fun loadArtworkColors(
    context: Context,
    urls: List<String>,
    fallbackAccent: Color,
): ArtworkColors? {
    val cacheKey = urls.joinToString(separator = "|")
    songPaletteCache[cacheKey]?.let { return it }

    for (url in urls) {
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(Size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
                .scale(Scale.FILL)
                .memoryCacheKey("palette:$url")
                .build()
            val result = withContext(Dispatchers.IO) {
                context.imageLoader.execute(request)
            }
            val bitmap = (result.image as? BitmapImage)?.bitmap ?: continue
            val palette = withContext(Dispatchers.Default) {
                androidx.palette.graphics.Palette.from(bitmap)
                    .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                    .generate()
            }
            val gradient = PlayerColorExtractor.extractGradientColors(
                palette = palette,
                fallbackColor = OmniColors.OmniBackgroundBase.toArgb()
            )
            val colors = ArtworkColors(
                gradient = gradient,
                palette = OmniDynamicSongPalette.fromArtworkColors(
                    colors = gradient,
                    fallbackAccent = fallbackAccent,
                ),
            )
            songPaletteCache[cacheKey] = colors
            songPaletteCache[url] = colors
            return colors
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
    val dynamicAccents = LocalOmniAccents.current
    val dynamicSongColors by rememberPreference(DynamicSongColorsKey, true)

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
    var extractedPalette by remember { mutableStateOf<OmniDynamicSongPalette?>(null) }
    var isFromArtwork by remember { mutableStateOf(false) }

    LaunchedEffect(candidates, dynamicAccents.primary, dynamicSongColors) {
        if (!dynamicSongColors || candidates.isEmpty()) {
            extractedColors = emptyList()
            extractedPalette = null
            isFromArtwork = false
            return@LaunchedEffect
        }
        val colors = loadArtworkColors(
            context = context,
            urls = candidates,
            fallbackAccent = dynamicAccents.primary,
        )
        if (colors != null) {
            extractedColors = colors.gradient
            extractedPalette = colors.palette
            isFromArtwork = true
        } else {
            extractedColors = emptyList()
            extractedPalette = null
            isFromArtwork = false
        }
    }

    val fallbackPalette = OmniDynamicSongPalette.fallback(dynamicAccents.primary)
    val targetPalette = if (dynamicSongColors && isFromArtwork) extractedPalette ?: fallbackPalette else fallbackPalette
    val colorAnimation = spring<Color>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    )
    val animatedPalette = targetPalette.copy(
        background = animateColorAsState(targetPalette.background, colorAnimation, label = "song_background").value,
        backgroundSecondary = animateColorAsState(targetPalette.backgroundSecondary, colorAnimation, label = "song_background_secondary").value,
        surface = animateColorAsState(targetPalette.surface, colorAnimation, label = "song_surface").value,
        surfaceElevated = animateColorAsState(targetPalette.surfaceElevated, colorAnimation, label = "song_surface_elevated").value,
        accent = animateColorAsState(targetPalette.accent, colorAnimation, label = "song_accent").value,
        accentSoft = animateColorAsState(targetPalette.accentSoft, colorAnimation, label = "song_accent_soft").value,
        miniPlayerSurface = animateColorAsState(targetPalette.miniPlayerSurface, colorAnimation, label = "song_mini_surface").value,
        playerControlSurface = animateColorAsState(targetPalette.playerControlSurface, colorAnimation, label = "song_control_surface").value,
        gradientStart = animateColorAsState(targetPalette.gradientStart, colorAnimation, label = "song_gradient_start").value,
        gradientEnd = animateColorAsState(targetPalette.gradientEnd, colorAnimation, label = "song_gradient_end").value,
    )

    val displayColors = if (dynamicSongColors && isFromArtwork && extractedColors.isNotEmpty()) {
        extractedColors
    } else {
        PlayerFallbackGradient
    }
    val accentGlow = if (dynamicSongColors && isFromArtwork) animatedPalette.accent.copy(alpha = 0.24f) else FallbackGlow.copy(alpha = 0.16f)

    return PlayerGradientState(
        backgroundBrush = Brush.verticalGradient(
            listOf(
                animatedPalette.gradientStart,
                animatedPalette.backgroundSecondary,
                animatedPalette.background,
                animatedPalette.gradientEnd,
            )
        ),
        accentGlow = accentGlow,
        dominantColor = displayColors.firstOrNull() ?: animatedPalette.backgroundSecondary,
        dynamicAccentColor = animatedPalette.accent,
        palette = animatedPalette,
        isFromArtwork = dynamicSongColors && isFromArtwork,
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
    val dynamicAccentColor: Color = Color(0xFFED5564),
    val palette: OmniDynamicSongPalette = OmniDynamicSongPalette.fallback(),
    val isFromArtwork: Boolean = false,
)
