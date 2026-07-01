/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object OmniColors {
    // Backgrounds
    val OmniBackgroundBase = Color(0xFF040508)
    val OmniBackgroundElevated = Color(0xFF0B0E16)
    val OmniBackgroundGradientTop = Color(0xFF070A11)
    val OmniBackgroundGradientBottom = OmniBackgroundBase

    val Background = OmniBackgroundBase
    val BackgroundAlt = Color(0xFF070A12)
    val Surface = Color(0xFF0A0D14)
    val SurfaceElevated = OmniBackgroundElevated
    val SurfaceHairline = Color.White.copy(alpha = 0.055f)
    val SurfacePressed = Color.White.copy(alpha = 0.08f)
    val SurfaceQuiet = Color(0xFF0A0E17).copy(alpha = 0.68f)
    val SurfacePanel = Color(0xFF0E1420).copy(alpha = 0.78f)

    // Glass surfaces
    val OmniGlassSubtle = Color.White.copy(alpha = 0.01f)
    val OmniGlassMedium = Color.White.copy(alpha = 0.02f)
    val OmniGlassStrong = Color.White.copy(alpha = 0.04f)
    val OmniGlassDock = Color(0xFF080B12).copy(alpha = 0.94f)
    val OmniGlassPlayer = Color(0xFF06090E).copy(alpha = 0.96f)
    val OmniGlassBorderSubtle = Color.White.copy(alpha = 0.01f)
    val OmniGlassBorderStrong = Color.White.copy(alpha = 0.02f)

    val GlassSurface = OmniGlassMedium
    val GlassSurfaceStrong = OmniGlassStrong
    val GlassBorder = OmniGlassBorderStrong
    val GlassBorderLight = OmniGlassBorderSubtle

    // Accents
    val OmniAccentPrimary = Color(0xFF7F8CFF)
    val OmniAccentSecondary = Color(0xFF55D0F5)
    val OmniAccentTertiary = Color(0xFF6EE7CF)
    val OmniAccentWarm = Color(0xFFFFC46B)
    val OmniAccentMuted = Color(0xFF756AC4)
    val OmniAccentGlow = Color(0x4D7F8CFF)
    val OmniAccentOnPrimary = Color(0xFF05060A)

    val Primary = OmniAccentPrimary
    val PrimaryLight = Color(0xFFA99CFF)
    val Secondary = OmniAccentSecondary
    val SecondaryLight = Color(0xFF8CE6FF)
    val Hot = Color(0xFFFF5C93)
    val HotLight = Color(0xFFFF83AD)

    // Semantic colors
    val Success = Color(0xFF4EDB8F)
    val Warning = Color(0xFFFFC46B)
    val Error = Color(0xFFFF6363)
    val Offline = Color(0xFF6E7787)
    val Downloaded = Color(0xFF6EE7B7)
    val ActivePlayback = OmniAccentSecondary

    // Text
    val TextPrimary = Color(0xFFF7F8FB)
    val TextSecondary = Color(0xFFB3BAC8)
    val TextTertiary = Color(0xFF8A93A5)
    val TextDisabled = Color(0xFF4C5361)
    val TextOnAccent = OmniAccentOnPrimary
    val TextMuted = TextTertiary

    // Gradients
    val PrimaryGradient = Brush.horizontalGradient(
        colors = listOf(OmniAccentPrimary, OmniAccentSecondary)
    )
    val PrimaryGradientVertical = Brush.verticalGradient(
        colors = listOf(OmniAccentPrimary, PrimaryLight)
    )
    val HotGradient = Brush.horizontalGradient(
        colors = listOf(Hot, OmniAccentPrimary)
    )
    val GlassGradient = Brush.verticalGradient(
        colors = listOf(
            OmniGlassSubtle,
            OmniGlassMedium,
        )
    )
    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(OmniBackgroundGradientTop, OmniBackgroundGradientBottom)
    )

    // Gradient color pairs for brushes
    val PrimaryGradientColors = listOf(OmniAccentPrimary, OmniAccentSecondary)
    val HotGradientColors = listOf(Hot, OmniAccentPrimary)
    val OmniPulseGradientColors = listOf(OmniAccentSecondary, OmniAccentPrimary, OmniAccentTertiary)
}
