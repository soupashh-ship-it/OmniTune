/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts vibrant, dominant colors from album artwork to generate
 * dynamic gradient backgrounds for the player interface.
 */
object PlayerColorExtractor {

    private val DefaultThemeColor = Color(0xFF4FC3F7)

    /**
     * Extracts and derives up to 6 distinct gradient colors from a [Palette].
     *
     * @param palette      The [Palette] generated from album artwork bitmap.
     * @param fallbackArgb Fallback ARGB int if extraction yields nothing useful.
     * @return A list of colors suitable for a vertical gradient background.
     */
    suspend fun extractGradientColors(
        palette: Palette,
        fallbackArgb: Int,
    ): List<Color> = withContext(Dispatchers.Default) {
        val vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) }
        val darkVibrant = palette.darkVibrantSwatch?.rgb?.let { Color(it) }
        val lightVibrant = palette.lightVibrantSwatch?.rgb?.let { Color(it) }
        val dominant = palette.dominantSwatch?.rgb?.let { Color(it) }
        val muted = palette.mutedSwatch?.rgb?.let { Color(it) }
        val darkMuted = palette.darkMutedSwatch?.rgb?.let { Color(it) }
        val lightMuted = palette.lightMutedSwatch?.rgb?.let { Color(it) }

        val available = mutableListOf<Color>()

        fun addIfUnique(c: Color?, boost: Float) {
            if (c != null && !isSimilarToAny(c, available)) {
                available.add(enhanceVividness(c, boost))
            }
        }

        // Collect distinct colors, preferring vibrant swatches
        addIfUnique(vibrant, 1.30f)
        addIfUnique(lightVibrant, 1.25f)
        addIfUnique(darkVibrant, 1.20f)
        addIfUnique(dominant, 1.10f)
        addIfUnique(muted, 1.00f)
        addIfUnique(darkMuted, 0.90f)
        addIfUnique(lightMuted, 1.00f)

        val fallbackSeed =
            Color(fallbackArgb).takeUnless { isNearGray(it) } ?: DefaultThemeColor

        val seed = available.firstOrNull() ?: fallbackSeed

        // Derive additional hues to fill out to ~6 colors for rich gradients
        val hueOffsets = listOf(25f, -25f, 55f, -55f, 120f, -120f, 180f, 150f, -150f)
        val valueTargets = floatArrayOf(0.82f, 0.74f, 0.68f, 0.60f, 0.86f, 0.70f)

        run {
            val candidates = (available.toList() + seed).distinct()
            var baseIdx = 0
            var targetIdx = 0
            while (available.size < 6) {
                val base = candidates[baseIdx % candidates.size]
                val offset = hueOffsets[targetIdx % hueOffsets.size]
                val vt = valueTargets[available.size % valueTargets.size]
                val derived = tuneForMesh(
                    color = hueShift(base, offset),
                    satMin = 0.62f,
                    satBoost = 1.08f,
                    valueTarget = vt,
                    valueMin = 0.38f,
                    valueMax = 0.90f,
                )
                if (!isSimilarToAny(derived, available)) {
                    available.add(derived)
                }
                baseIdx++
                targetIdx++
                if (baseIdx > 40) break
            }
        }

        // Guarantee at least one entry
        if (available.isEmpty()) {
            available.add(
                tuneForMesh(fallbackSeed, 0.62f, 1.08f, 0.75f, 0.38f, 0.90f)
            )
        }

        return@withContext available.toList()
    }

    /**
     * Generate a [Palette] from an artwork [Bitmap].
     * Converts to a palette-friendly config and runs Palette.generate().
     */
    suspend fun generatePalette(bitmap: Bitmap): Palette = withContext(Dispatchers.Default) {
        val scaled = if (bitmap.width * bitmap.height > Config.BITMAP_AREA) {
            val ratio = kotlin.math.sqrt(
                Config.BITMAP_AREA.toFloat() / (bitmap.width * bitmap.height)
            )
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            bitmap
        }
        Palette.from(scaled).maximumColorCount(Config.MAX_COLOR_COUNT).generate()
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private fun enhanceVividness(color: Color, factor: Float = 1.4f): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[1] = (hsv[1] * factor).coerceIn(0.55f, 1.0f)
        hsv[2] = (hsv[2] * 1.02f).coerceIn(0.32f, 0.88f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    private fun isNearGray(color: Color): Boolean {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        return hsv[1] < 0.08f || hsv[2] < 0.08f
    }

    private fun isSimilarColor(a: Color, b: Color): Boolean {
        val ha = FloatArray(3); android.graphics.Color.colorToHSV(a.toArgb(), ha)
        val hb = FloatArray(3); android.graphics.Color.colorToHSV(b.toArgb(), hb)
        val hueDiff = minOf(kotlin.math.abs(ha[0] - hb[0]), 360f - kotlin.math.abs(ha[0] - hb[0]))
        if (hueDiff < 12f && kotlin.math.abs(ha[1] - hb[1]) < 0.12f && kotlin.math.abs(ha[2] - hb[2]) < 0.12f) return true

        val threshold = 28
        return (kotlin.math.abs(a.red * 255 - b.red * 255) < threshold &&
                kotlin.math.abs(a.green * 255 - b.green * 255) < threshold &&
                kotlin.math.abs(a.blue * 255 - b.blue * 255) < threshold)
    }

    private fun isSimilarToAny(color: Color, colors: List<Color>): Boolean =
        colors.any { isSimilarColor(color, it) }

    private fun hueShift(color: Color, degrees: Float): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[0] = ((hsv[0] + degrees) % 360f + 360f) % 360f
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    private fun tuneForMesh(
        color: Color,
        satMin: Float,
        satBoost: Float,
        valueTarget: Float,
        valueMin: Float,
        valueMax: Float,
    ): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[1] = (maxOf(hsv[1], satMin) * satBoost).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] * 0.85f + valueTarget * 0.15f).coerceIn(valueMin, valueMax)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }


    /**
     * Extracts a single dominant accent color suitable for UI elements
     * (sliders, buttons, text highlights). Returns a vivid, bright color
     * that works well on dark backgrounds.
     */
    suspend fun extractDominantAccent(
        palette: Palette,
        fallbackArgb: Int,
    ): Color = withContext(Dispatchers.Default) {
        val vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) }
        val lightVibrant = palette.lightVibrantSwatch?.rgb?.let { Color(it) }
        val dominant = palette.dominantSwatch?.rgb?.let { Color(it) }
        val muted = palette.mutedSwatch?.rgb?.let { Color(it) }

        // Pick the most vivid swatch, boost it for UI use
        val raw = vibrant ?: lightVibrant ?: dominant ?: muted
            ?: Color(fallbackArgb).takeUnless { isNearGray(it) }
            ?: DefaultThemeColor

        // Ensure the color is vivid and bright enough for UI on dark backgrounds
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(raw.toArgb(), hsv)
        hsv[1] = (hsv[1] * 1.4f).coerceIn(0.50f, 1.0f)  // boost saturation
        hsv[2] = hsv[2].coerceIn(0.55f, 0.85f)             // keep brightness in usable range
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    object Config {
        const val MAX_COLOR_COUNT = 32
        const val BITMAP_AREA = 8000
    }
}
