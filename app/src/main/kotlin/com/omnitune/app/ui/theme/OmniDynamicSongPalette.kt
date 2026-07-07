/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/**
 * Artwork-reactive colors for now-playing surfaces.
 *
 * The palette keeps raw artwork colors away from large surfaces. Album colors
 * become a controlled accent first, then dark tonal surfaces are derived from it.
 */
data class OmniDynamicSongPalette(
    val background: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val miniPlayerSurface: Color,
    val playerControlSurface: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
) {
    companion object {
        fun fallback(accentSeed: Color = OmniColors.OmniAccentPrimary): OmniDynamicSongPalette {
            val accent = accentSeed.toReadableAccent()
            return fromAccent(accent)
        }

        fun fromArtworkColors(colors: List<Color>, fallbackAccent: Color = OmniColors.OmniAccentPrimary): OmniDynamicSongPalette {
            val seed = colors.firstOrNull { it.isUsableSongColor() } ?: fallbackAccent
            return fromAccent(seed.toReadableAccent())
        }

        private fun fromAccent(accent: Color): OmniDynamicSongPalette {
            val backdrop = accent.toDarkTone(value = 0.18f, saturationMin = 0.34f)
            val deepBackdrop = accent.toDarkTone(value = 0.075f, saturationMin = 0.30f)
            val background = lerp(OmniColors.OmniBackgroundBase, deepBackdrop, 0.42f)
            val backgroundSecondary = lerp(OmniColors.OmniBackgroundElevated, backdrop, 0.36f)
            val surface = lerp(OmniColors.Surface, backdrop, 0.20f).copy(alpha = 0.94f)
            val surfaceElevated = lerp(OmniColors.SurfaceRaised, backdrop, 0.24f).copy(alpha = 0.96f)
            val miniSurface = lerp(OmniColors.OmniGlassDock, backdrop, 0.28f).copy(alpha = 0.96f)
            val controlSurface = lerp(Color(0xFF121826), backdrop, 0.36f).copy(alpha = 0.90f)

            return OmniDynamicSongPalette(
                background = background,
                backgroundSecondary = backgroundSecondary,
                surface = surface,
                surfaceElevated = surfaceElevated,
                accent = accent,
                accentSoft = accent.copy(alpha = 0.16f),
                onAccent = if (accent.luminance() > 0.52f) Color(0xFF05060A) else Color.White,
                textPrimary = OmniColors.TextPrimary,
                textSecondary = OmniColors.TextSecondary,
                miniPlayerSurface = miniSurface,
                playerControlSurface = controlSurface,
                gradientStart = lerp(backgroundSecondary, backdrop, 0.46f).copy(alpha = 0.98f),
                gradientEnd = Color(0xFF03050A),
            )
        }
    }
}

private fun Color.isUsableSongColor(): Boolean {
    val hsv = toHsv()
    val brightness = hsv[2]
    val saturation = hsv[1]
    if (brightness < 0.10f || brightness > 0.92f) return false
    if (saturation < 0.16f) return false
    val tooCloseToBase = kotlin.math.abs(red - OmniColors.OmniBackgroundBase.red) < 0.04f &&
        kotlin.math.abs(green - OmniColors.OmniBackgroundBase.green) < 0.04f &&
        kotlin.math.abs(blue - OmniColors.OmniBackgroundBase.blue) < 0.04f
    return !tooCloseToBase
}

private fun Color.toReadableAccent(): Color {
    val hsv = toHsv()
    hsv[1] = hsv[1].coerceIn(0.42f, 0.86f)
    hsv[2] = hsv[2].coerceIn(0.58f, 0.78f)
    return Color(AndroidColor.HSVToColor(hsv))
}

private fun Color.toDarkTone(value: Float, saturationMin: Float): Color {
    val hsv = toHsv()
    hsv[1] = hsv[1].coerceAtLeast(saturationMin).coerceAtMost(0.82f)
    hsv[2] = value.coerceIn(0.04f, 0.32f)
    return Color(AndroidColor.HSVToColor(hsv))
}

private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    return hsv
}
