/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object OmniColors {
    // ── Backgrounds ──
    val Background = Color(0xFF05060A)
    val BackgroundAlt = Color(0xFF080A12)
    val Surface = Color(0xFF0C0E16)
    val SurfaceElevated = Color(0xFF10131C)

    // ── Glass ──
    val GlassSurface = Color.White.copy(alpha = 0.06f)
    val GlassSurfaceStrong = Color.White.copy(alpha = 0.10f)
    val GlassBorder = Color.White.copy(alpha = 0.14f)
    val GlassBorderLight = Color.White.copy(alpha = 0.08f)

    // ── Accents ──
    val Primary = Color(0xFF7C5CFF)        // Violet
    val PrimaryLight = Color(0xFF9B7FFF)
    val Secondary = Color(0xFF00D4FF)       // Cyan
    val SecondaryLight = Color(0xFF33DEFF)
    val Hot = Color(0xFFFF3D81)             // Pink
    val HotLight = Color(0xFFFF6B9E)

    // ── Text ──
    val TextPrimary = Color(0xFFF5F7FA)
    val TextSecondary = Color(0xFFA7ADB8)
    val TextMuted = Color(0xFF5F6673)

    // ── Functional ──
    val Success = Color(0xFF3DDC84)
    val Warning = Color(0xFFFFB74D)
    val Error = Color(0xFFFF5252)

    // ── Gradients ──
    val PrimaryGradient = Brush.horizontalGradient(
        colors = listOf(Primary, Secondary)
    )
    val PrimaryGradientVertical = Brush.verticalGradient(
        colors = listOf(Primary, PrimaryLight)
    )
    val HotGradient = Brush.horizontalGradient(
        colors = listOf(Hot, Primary)
    )
    val GlassGradient = Brush.verticalGradient(
        colors = listOf(
            GlassSurface,
            GlassSurfaceStrong,
        )
    )
    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(Background, BackgroundAlt)
    )

    // ── Gradient color pairs (for brushes) ──
    val PrimaryGradientColors = listOf(Primary, Secondary)
    val HotGradientColors = listOf(Hot, Primary)
}
