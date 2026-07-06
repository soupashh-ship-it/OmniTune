/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object OmniColors {
    // Backgrounds — tinted dark with a subtle blue undertone
    val OmniBackgroundBase = Color(0xFF06080F)
    val OmniBackgroundElevated = Color(0xFF0D1019)
    val OmniBackgroundGradientTop = Color(0xFF0A0D16)
    val OmniBackgroundGradientBottom = OmniBackgroundBase

    val Background = OmniBackgroundBase
    val BackgroundAlt = Color(0xFF080B14)
    val Surface = Color(0xFF0C101A)
    val SurfaceElevated = OmniBackgroundElevated
    val SurfaceHairline = Color.White.copy(alpha = 0.06f)
    val SurfacePressed = Color.White.copy(alpha = 0.08f)
    val SurfaceQuiet = Color(0xFF0B0F1A).copy(alpha = 0.68f)
    val SurfacePanel = Color(0xFF101522).copy(alpha = 0.78f)
    val SurfaceSubtle = Color(0xFF0D111C).copy(alpha = 0.74f)
    val SurfaceRaised = Color(0xFF131928).copy(alpha = 0.92f)
    val SurfaceFloating = Color(0xFF0A0E18).copy(alpha = 0.96f)

    // Glass surfaces
    val OmniGlassSubtle = Color.White.copy(alpha = 0.01f)
    val OmniGlassMedium = Color.White.copy(alpha = 0.02f)
    val OmniGlassStrong = Color.White.copy(alpha = 0.04f)
    val OmniGlassDock = Color(0xFF0A0E18).copy(alpha = 0.94f)
    val OmniGlassPlayer = Color(0xFF080C14).copy(alpha = 0.96f)
    val OmniGlassBorderSubtle = Color.White.copy(alpha = 0.01f)
    val OmniGlassBorderStrong = Color.White.copy(alpha = 0.02f)

    val GlassSurface = OmniGlassMedium
    val GlassSurfaceStrong = OmniGlassStrong
    val GlassBorder = OmniGlassBorderStrong
    val GlassBorderLight = OmniGlassBorderSubtle

    // Accents — single lavender primary with a cooler blue-violet for contrast
    val OmniAccentPrimary = Color(0xFF8B8FFF)       // Lavender — the one accent
    val OmniAccentSecondary = Color(0xFF6B7FFF)      // Cooler blue-violet for gradient endpoints
    val OmniAccentTertiary = Color(0xFFB8A0FF)       // Warmer lilac for highlights
    val OmniAccentWarm = Color(0xFFFFC46B)            // Kept for semantic warning only
    val OmniAccentMuted = Color(0xFF6B6AAA)           // Desaturated lavender for muted elements
    val OmniAccentSoft = OmniAccentPrimary.copy(alpha = 0.12f)
    val OmniAccentGlow = OmniAccentPrimary.copy(alpha = 0.30f)
    val OmniAccentOnPrimary = Color(0xFF05060A)

    val Primary = OmniAccentPrimary
    val PrimaryLight = Color(0xFFADA8FF)
    val Secondary = OmniAccentSecondary
    val SecondaryLight = Color(0xFF9B9AFF)
    val Hot = Color(0xFFFF5C93)                        // Kept for likes/favorites only
    val HotLight = Color(0xFFFF83AD)

    // Semantic colors
    val Success = Color(0xFF4EDB8F)
    val Warning = Color(0xFFFFC46B)
    val Error = Color(0xFFFF6363)
    val Offline = Color(0xFF6E7787)
    val Downloaded = Color(0xFF6EE7B7)
    val ActivePlayback = OmniAccentPrimary

    // Text
    val TextPrimary = Color(0xFFF2F3F8)
    val TextSecondary = Color(0xFFA8B0C4)
    val TextTertiary = Color(0xFF7A8299)
    val TextDisabled = Color(0xFF444B5C)
    val TextOnAccent = OmniAccentOnPrimary
    val TextMuted = TextTertiary
    val BorderSubtle = Color.White.copy(alpha = 0.07f)

    // Gradients — lavender-to-blue-violet for depth, not flat single-hue
    val PrimaryGradient = Brush.horizontalGradient(
        colors = listOf(OmniAccentPrimary, OmniAccentSecondary)
    )
    val PrimaryGradientVertical = Brush.verticalGradient(
        colors = listOf(OmniAccentPrimary, OmniAccentTertiary)
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
