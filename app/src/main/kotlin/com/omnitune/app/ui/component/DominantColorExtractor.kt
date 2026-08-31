/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.component

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data class holding dominant colors extracted from an image
 */
data class DominantColors(
    val primary: Color = Color(0xFF1A1A1A),
    val secondary: Color = Color(0xFF2A2A2A),
    val accent: Color = Color(0xFF888888),
    val onBackground: Color = Color.White
)

/**
 * Process-level LRU cache of extracted colors keyed by "url|isDarkTheme".
 */
private val dominantColorsCache: MutableMap<String, DominantColors> =
    java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, DominantColors>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DominantColors>): Boolean = size > 100
        }
    )

/**
 * Extracts dominant colors from an image URL
 */
@Composable
fun rememberDominantColors(
    imageUrl: String?,
    isDarkTheme: Boolean = true,
    defaultColors: DominantColors? = null
): DominantColors {
    val themeAwareDefaults = defaultColors ?: if (isDarkTheme) {
        DominantColors(
            primary = Color(0xFF1A1A1A),
            secondary = Color(0xFF2A2A2A),
            accent = Color(0xFF888888),
            onBackground = Color.White
        )
    } else {
        DominantColors(
            primary = Color(0xFFF5F5F5),
            secondary = Color(0xFFE8E8E8),
            accent = Color(0xFF666666),
            onBackground = Color(0xFF1A1A1A)
        )
    }
    
    var colors by remember(imageUrl, isDarkTheme) {
        val seeded = imageUrl?.let { dominantColorsCache["$it|$isDarkTheme"] } ?: themeAwareDefaults
        mutableStateOf(seeded)
    }
    val context = LocalContext.current

    LaunchedEffect(imageUrl, isDarkTheme) {
        if (imageUrl == null) {
            colors = themeAwareDefaults
            return@LaunchedEffect
        }

        val cacheKey = "$imageUrl|$isDarkTheme"
        dominantColorsCache[cacheKey]?.let { cached ->
            colors = cached
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val loader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .size(100)
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.image.toBitmap()
                    val newColors = extractColorsFromBitmap(bitmap, isDarkTheme)
                    dominantColorsCache[cacheKey] = newColors
                    withContext(Dispatchers.Main) {
                        colors = newColors
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    return colors
}

private fun extractColorsFromBitmap(bitmap: Bitmap, isDarkTheme: Boolean = true): DominantColors {
    val width = bitmap.width
    val height = bitmap.height
    
    val colors = mutableListOf<Int>()
    val step = maxOf(1, minOf(width, height) / 10)
    
    for (x in 0 until width step step) {
        for (y in 0 until height step step) {
            val pixel = bitmap.getPixel(x, y)
            colors.add(pixel)
        }
    }
    
    if (colors.isEmpty()) return DominantColors()
    
    var totalR = 0L
    var totalG = 0L
    var totalB = 0L
    
    colors.forEach { color ->
        totalR += android.graphics.Color.red(color)
        totalG += android.graphics.Color.green(color)
        totalB += android.graphics.Color.blue(color)
    }
    
    val avgR = (totalR / colors.size).toInt()
    val avgG = (totalG / colors.size).toInt()
    val avgB = (totalB / colors.size).toInt()
    
    val primary: Color
    val secondary: Color
    val onBackground: Color
    
    if (isDarkTheme) {
        primary = Color(
            red = (avgR * 0.3f / 255f).coerceIn(0f, 1f),
            green = (avgG * 0.3f / 255f).coerceIn(0f, 1f),
            blue = (avgB * 0.3f / 255f).coerceIn(0f, 1f)
        )
        
        secondary = Color(
            red = (avgR * 0.5f / 255f).coerceIn(0f, 1f),
            green = (avgG * 0.5f / 255f).coerceIn(0f, 1f),
            blue = (avgB * 0.5f / 255f).coerceIn(0f, 1f)
        )
        
        onBackground = Color.White
    } else {
        primary = Color(
            red = (avgR * 0.2f / 255f + 0.85f).coerceIn(0f, 1f),
            green = (avgG * 0.2f / 255f + 0.85f).coerceIn(0f, 1f),
            blue = (avgB * 0.2f / 255f + 0.85f).coerceIn(0f, 1f)
        )
        
        secondary = Color(
            red = (avgR * 0.3f / 255f + 0.75f).coerceIn(0f, 1f),
            green = (avgG * 0.3f / 255f + 0.75f).coerceIn(0f, 1f),
            blue = (avgB * 0.3f / 255f + 0.75f).coerceIn(0f, 1f)
        )
        
        onBackground = Color(0xFF1A1A1A)
    }
    
    val hsl = FloatArray(3)
    ColorUtils.RGBToHSL(avgR, avgG, avgB, hsl)
    hsl[1] = minOf(1f, hsl[1] * 1.2f)
    hsl[2] = if (isDarkTheme) 0.6f else 0.5f
    val accent = ensureContrast(hsl, background = primary, lightenToPass = isDarkTheme)
    
    return DominantColors(
        primary = primary,
        secondary = secondary,
        accent = accent,
        onBackground = onBackground
    )
}

private const val MIN_ACCENT_CONTRAST = 3.0

private fun ensureContrast(accentHsl: FloatArray, background: Color, lightenToPass: Boolean): Color {
    val backgroundInt = android.graphics.Color.rgb(
        (background.red * 255).toInt(),
        (background.green * 255).toInt(),
        (background.blue * 255).toInt()
    )
    val hsl = accentHsl.copyOf()
    var candidate = ColorUtils.HSLToColor(hsl)
    var iterations = 0
    while (ColorUtils.calculateContrast(candidate, backgroundInt) < MIN_ACCENT_CONTRAST && iterations < 20) {
        hsl[2] = if (lightenToPass) minOf(1f, hsl[2] + 0.05f) else maxOf(0f, hsl[2] - 0.05f)
        candidate = ColorUtils.HSLToColor(hsl)
        iterations++
    }
    return Color(candidate)
}
