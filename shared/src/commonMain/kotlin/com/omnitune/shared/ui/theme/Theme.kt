package com.omnitune.shared.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

val LocalSuvMusicPalette = compositionLocalOf { SuvMusicPalette.DEFAULT }
val LocalPureBlack = compositionLocalOf { false }
val LocalDominantColors = compositionLocalOf<DominantColors?> { null }

@Composable
fun SuvMusicTheme(
    palette: SuvMusicPalette = SuvMusicPalette.DEFAULT,
    darkTheme: Boolean = true,
    pureBlack: Boolean = false,
    albumArtColors: DominantColors? = null,
    content: @Composable () -> Unit
) {
    val animatedAccent by animateColorAsState(
        targetValue = albumArtColors?.accent ?: getPaletteColors(palette).primary,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "theme_accent"
    )

    val animatedSecondary by animateColorAsState(
        targetValue = albumArtColors?.secondary ?: getPaletteColors(palette).secondary,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "theme_secondary"
    )

    val baseScheme = buildColorScheme(
        palette = palette,
        darkTheme = darkTheme,
        pureBlack = pureBlack
    )

    val activeScheme = if (albumArtColors != null) {
        baseScheme.copy(
            primary = animatedAccent,
            secondary = animatedSecondary,
            tertiary = animatedAccent
        )
    } else {
        baseScheme
    }

    CompositionLocalProvider(
        LocalSuvMusicPalette provides palette,
        LocalPureBlack provides pureBlack,
        LocalDominantColors provides albumArtColors
    ) {
        MaterialTheme(
            colorScheme = activeScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
