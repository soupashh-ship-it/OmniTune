/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object OmniColors {
    // Backgrounds — tinted dark with a subtle blue undertone
    var OmniBackgroundBase = Color(0xFF06080F)
    var OmniBackgroundElevated = Color(0xFF0D1019)
    var OmniBackgroundGradientTop = Color(0xFF0A0D16)
    val OmniBackgroundGradientBottom: Color
    get() = OmniBackgroundBase

    var Background = OmniBackgroundBase
    val BackgroundAlt = Color(0xFF080B14)
    var Surface = Color(0xFF0C101A)
    var SurfaceElevated = OmniBackgroundElevated
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
    var OmniAccentPrimary = Color(0xFF8B8FFF)       // Lavender — the one accent
    var OmniAccentSecondary = Color(0xFF6B7FFF)      // Cooler blue-violet for gradient endpoints
    var OmniAccentTertiary = Color(0xFFB8A0FF)       // Warmer lilac for highlights
    var OmniAccentWarm = Color(0xFFFFC46B)            // Kept for semantic warning only
    var OmniAccentMuted = Color(0xFF6B6AAA)           // Desaturated lavender for muted elements
    var OmniAccentSoft = OmniAccentPrimary.copy(alpha = 0.12f)
    var OmniAccentGlow = OmniAccentPrimary.copy(alpha = 0.30f)
    var OmniAccentOnPrimary = Color(0xFF05060A)

    var Primary = OmniAccentPrimary
    var PrimaryLight = Color(0xFFADA8FF)
    var Secondary = OmniAccentSecondary
    var SecondaryLight = Color(0xFF9B9AFF)
    val Hot = Color(0xFFFF5C93)                        // Kept for likes/favorites only
    val HotLight = Color(0xFFFF83AD)

    // Semantic colors
    val Success = Color(0xFF4EDB8F)
    val Warning = Color(0xFFFFC46B)
    val Error = Color(0xFFFF6363)
    val Offline = Color(0xFF6E7787)
    val Downloaded = Color(0xFF6EE7B7)
    var ActivePlayback = OmniAccentPrimary

    // Text
    val TextPrimary = Color(0xFFF2F3F8)
    val TextSecondary = Color(0xFFA8B0C4)
    val TextTertiary = Color(0xFF7A8299)
    val TextDisabled = Color(0xFF444B5C)
    val TextOnAccent = OmniAccentOnPrimary
    val TextMuted = TextTertiary
    val BorderSubtle = Color.White.copy(alpha = 0.07f)

    // Gradients — lavender-to-blue-violet for depth, not flat single-hue
    var PrimaryGradient = Brush.horizontalGradient(
        colors = listOf(OmniAccentPrimary, OmniAccentSecondary)
    )
    var PrimaryGradientVertical = Brush.verticalGradient(
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
    val BackgroundGradient: Brush
    get() = Brush.verticalGradient(
        colors = listOf(OmniBackgroundGradientTop, OmniBackgroundGradientBottom)
    )

    // Gradient color pairs for brushes
    var PrimaryGradientColors = listOf(OmniAccentPrimary, OmniAccentSecondary)
    var HotGradientColors = listOf(Hot, OmniAccentPrimary)
    var OmniPulseGradientColors = listOf(OmniAccentSecondary, OmniAccentPrimary, OmniAccentTertiary)

    /**
     * Call this from OmniTuneTheme when the color scheme changes.
     * Updates the accent colors to match the current MaterialTheme color scheme.
     */
    fun updateFromTheme(primary: Color, secondary: Color, tertiary: Color, pureBlack: Boolean = false) {
        OmniAccentPrimary = primary
        OmniAccentSecondary = secondary
        OmniAccentTertiary = tertiary
        OmniAccentGlow = primary.copy(alpha = 0.30f)
        OmniAccentSoft = primary.copy(alpha = 0.12f)
        Primary = primary
        ActivePlayback = primary
        PrimaryLight = Color(
            red = (primary.red + 1f) / 2f,
            green = (primary.green + 1f) / 2f,
            blue = (primary.blue + 1f) / 2f,
        )
        Secondary = secondary
        SecondaryLight = Color(
            red = (secondary.red + 1f) / 2f,
            green = (secondary.green + 1f) / 2f,
            blue = (secondary.blue + 1f) / 2f,
        )
        PrimaryGradient = Brush.horizontalGradient(
            colors = listOf(primary, secondary)
        )
        PrimaryGradientVertical = Brush.verticalGradient(
            colors = listOf(primary, tertiary)
        )
        PrimaryGradientColors = listOf(primary, secondary)
        HotGradientColors = listOf(Hot, primary)
        OmniPulseGradientColors = listOf(secondary, primary, tertiary)

        if (pureBlack) {
            OmniBackgroundGradientTop = Color.Black
            OmniBackgroundElevated = Color(0xFF050507)
            OmniBackgroundBase = Color.Black
            Background = Color.Black
            Surface = Color(0xFF050507)
            SurfaceElevated = Color(0xFF08090D)
        } else {
            // Tint backgrounds/surfaces with a visible hint of the primary color
            val blend = { color: Color ->
                Color(
                    red = (color.red * 0.85f + primary.red * 0.15f),
                    green = (color.green * 0.85f + primary.green * 0.15f),
                    blue = (color.blue * 0.85f + primary.blue * 0.15f),
                    alpha = color.alpha,
                )
            }
            OmniBackgroundGradientTop = blend(Color(0xFF0A0D16))
            OmniBackgroundElevated = blend(Color(0xFF0D1019))
            OmniBackgroundBase = blend(Color(0xFF06080F))
            Background = blend(Color(0xFF06080F))
            Surface = blend(Color(0xFF0C101A))
            SurfaceElevated = blend(Color(0xFF0D1019))
        }
    }
}
