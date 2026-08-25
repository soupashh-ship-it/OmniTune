package com.omnitune.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object OmniColors {
    // 2026 Editorial Obsidian & Machined Slate Palette
    // Solid tonal surfaces, calibrated contrast, zero washed-out blur
    var OmniBackgroundBase = Color(0xFF0C0C0F)
    var OmniBackgroundElevated = Color(0xFF141419)
    var OmniBackgroundGradientTop = Color(0xFF141419)
    val OmniBackgroundGradientBottom: Color
        get() = OmniBackgroundBase

    var Background = OmniBackgroundBase
    val BackgroundAlt = Color(0xFF101014)
    var Surface = Color(0xFF181820)
    var SurfaceElevated = Color(0xFF20202A)
    val SurfaceHairline = Color(0xFF262634)
    val SurfacePressed = Color(0xFF2E2E3C)
    val SurfaceQuiet = Color(0xFF121217)
    val SurfacePanel = Color(0xFF1A1A22)
    val SurfaceSubtle = Color(0xFF15151C)
    val SurfaceRaised = Color(0xFF242430)
    val SurfaceFloating = Color(0xFF1C1C26)

    // Machined structural borders & surfaces (replacing generic glassmorphism)
    val OmniGlassSubtle = Color(0xFF141419)
    val OmniGlassMedium = Color(0xFF181820)
    val OmniGlassStrong = Color(0xFF20202A)
    val OmniGlassDock = Color(0xFF121216)
    val OmniGlassPlayer = Color(0xFF16161D)
    val OmniGlassBorderSubtle = Color(0xFF242430)
    val OmniGlassBorderStrong = Color(0xFF323242)

    val GlassSurface = OmniGlassMedium
    val GlassSurfaceStrong = OmniGlassStrong
    val GlassBorder = OmniGlassBorderStrong
    val GlassBorderLight = OmniGlassBorderSubtle
    val BorderHighlight = Color(0xFF363648)

    // Warm Editorial Coral & Amber accent family
    var OmniAccentPrimary = Color(0xFFE65D4F)
    var OmniAccentSecondary = Color(0xFFF58A7E)
    var OmniAccentTertiary = Color(0xFFF5A623)
    var OmniAccentWarm = Color(0xFFFFB347)
    var OmniAccentMuted = Color(0xFFB84B3F)
    var OmniAccentSoft = OmniAccentPrimary.copy(alpha = 0.14f)
    var OmniAccentGlow = OmniAccentPrimary.copy(alpha = 0.25f)
    var OmniAccentOnPrimary = Color(0xFF0C0C0F)

    var Primary = OmniAccentPrimary
    var PrimaryLight = Color(0xFFF58A7E)
    var Secondary = OmniAccentSecondary
    var SecondaryLight = Color(0xFFFFAB91)
    val Hot = Color(0xFFE65D4F)
    val HotLight = Color(0xFFF58A7E)

    // Semantic colors
    val Success = Color(0xFF34D399)
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFF87171)
    val Offline = Color(0xFF6B7280)
    val Downloaded = Color(0xFF34D399)
    var ActivePlayback = OmniAccentPrimary

    // Typographic contrast hierarchy
    val TextPrimary = Color(0xFFF6F5F8)
    val TextSecondary = Color(0xFFA5A1B0)
    val TextTertiary = Color(0xFF6F6B7A)
    val TextDisabled = Color(0xFF42404C)
    val TextOnAccent = OmniAccentOnPrimary
    val TextMuted = TextTertiary
    val BorderSubtle = Color(0xFF242430)

    // Gradients
    var PrimaryGradient = Brush.horizontalGradient(
        colors = listOf(OmniAccentPrimary, OmniAccentSecondary)
    )
    var PrimaryGradientVertical = Brush.verticalGradient(
        colors = listOf(OmniAccentPrimary, OmniAccentTertiary)
    )
    val HotGradient = Brush.horizontalGradient(
        colors = listOf(Hot, OmniAccentSecondary)
    )
    val GlassGradient = Brush.verticalGradient(
        colors = listOf(OmniGlassSubtle, OmniGlassMedium)
    )
    val BackgroundGradient: Brush
        get() = Brush.verticalGradient(
            colors = listOf(OmniBackgroundGradientTop, OmniBackgroundGradientBottom)
        )

    var PrimaryGradientColors = listOf(OmniAccentPrimary, OmniAccentSecondary)
    var HotGradientColors = listOf(Hot, OmniAccentSecondary)
    var OmniPulseGradientColors = listOf(OmniAccentSecondary, OmniAccentPrimary, OmniAccentTertiary)
    fun updateFromTheme(primary: Color, secondary: Color, tertiary: Color, pureBlack: Boolean = false) {
        // Honor the resolved theme palette (theme color / artwork seed / dynamic color)
        // instead of hardcoding the default coral — previously these parameters were
        // ignored, which silently disabled dynamic palettes on legacy screens.
        OmniAccentPrimary = primary
        OmniAccentSecondary = secondary
        OmniAccentTertiary = tertiary
        OmniAccentMuted = primary.copy(alpha = 0.55f)
        OmniAccentGlow = OmniAccentPrimary.copy(alpha = 0.25f)
        OmniAccentSoft = OmniAccentPrimary.copy(alpha = 0.14f)
        Primary = OmniAccentPrimary
        ActivePlayback = OmniAccentPrimary
        PrimaryLight = OmniAccentSecondary
        Secondary = OmniAccentSecondary
        SecondaryLight = tertiary
        PrimaryGradient = Brush.horizontalGradient(
            colors = listOf(OmniAccentPrimary, OmniAccentSecondary)
        )
        PrimaryGradientVertical = Brush.verticalGradient(
            colors = listOf(OmniAccentPrimary, OmniAccentTertiary)
        )
        PrimaryGradientColors = listOf(OmniAccentPrimary, OmniAccentSecondary)
        HotGradientColors = listOf(Hot, OmniAccentSecondary)
        OmniPulseGradientColors = listOf(OmniAccentSecondary, OmniAccentPrimary, OmniAccentTertiary)

        if (pureBlack) {
            OmniBackgroundGradientTop = Color.Black
            OmniBackgroundElevated = Color(0xFF070709)
            OmniBackgroundBase = Color.Black
            Background = Color.Black
            Surface = Color(0xFF0E0E12)
            SurfaceElevated = Color(0xFF14141A)
        } else {
            OmniBackgroundGradientTop = Color(0xFF141419)
            OmniBackgroundElevated = Color(0xFF141419)
            OmniBackgroundBase = Color(0xFF0C0C0F)
            Background = Color(0xFF0C0C0F)
            Surface = Color(0xFF181820)
            SurfaceElevated = Color(0xFF20202A)
        }
    }

}
