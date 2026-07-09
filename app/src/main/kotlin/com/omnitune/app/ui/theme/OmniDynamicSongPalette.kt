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
            val seed = colors
                .filter { it.isUsableSongColor() }
                .maxByOrNull { it.songColorScore() }
                ?: fallbackAccent
            return fromAccent(seed.toReadableAccent())
        }

        private fun fromAccent(accent: Color): OmniDynamicSongPalette {
            val backdrop = accent.toDarkTone(value = 0.21f, saturationMin = 0.42f)
            val deepBackdrop = accent.toDarkTone(value = 0.085f, saturationMin = 0.36f)
            val ink = Color(0xFF05070D)
            val background = lerp(ink, deepBackdrop, 0.58f)
            val backgroundSecondary = lerp(Color(0xFF111827), backdrop, 0.46f)
            val surface = lerp(Color(0xFF121722), backdrop, 0.24f).copy(alpha = 0.95f)
            val surfaceElevated = lerp(Color(0xFF172031), backdrop, 0.30f).copy(alpha = 0.97f)
            val miniSurface = lerp(Color(0xE6111724), backdrop, 0.34f).copy(alpha = 0.97f)
            val controlSurface = lerp(Color(0xFF151C2B), backdrop, 0.44f).copy(alpha = 0.92f)

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
                gradientStart = lerp(backgroundSecondary, accent.toDarkTone(value = 0.30f, saturationMin = 0.48f), 0.42f).copy(alpha = 0.98f),
                gradientEnd = ink,
            )
        }
    }
}

private fun Color.isUsableSongColor(): Boolean {
    val hsv = toHsv()
    val brightness = hsv[2]
    val saturation = hsv[1]
    if (brightness < 0.12f || brightness > 0.88f) return false
    if (saturation < 0.24f) return false
    if (luminance() < 0.025f || luminance() > 0.74f) return false
    val tooCloseToBase = kotlin.math.abs(red - OmniColors.OmniBackgroundBase.red) < 0.04f &&
        kotlin.math.abs(green - OmniColors.OmniBackgroundBase.green) < 0.04f &&
        kotlin.math.abs(blue - OmniColors.OmniBackgroundBase.blue) < 0.04f
    return !tooCloseToBase
}

private fun Color.songColorScore(): Float {
    val hsv = toHsv()
    val saturation = hsv[1]
    val brightness = hsv[2]
    val brightnessComfort = 1f - kotlin.math.abs(brightness - 0.46f)
    val colorfulness = saturation.coerceIn(0f, 1f)
    val darkSafety = if (brightness <= 0.62f) 0.18f else -0.10f
    return colorfulness * 0.62f + brightnessComfort * 0.30f + darkSafety
}

private fun Color.toReadableAccent(): Color {
    val hsv = toHsv()
    hsv[1] = hsv[1].coerceIn(0.48f, 0.82f)
    hsv[2] = hsv[2].coerceIn(0.56f, 0.74f)
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
