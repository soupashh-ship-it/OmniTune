package com.omnitune.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object OmniColors {
    // Reference-derived charcoal/coral dark palette
    // Stable reference values — not M3 dynamic.
    var OmniBackgroundBase = Color(0xFF111217)
    var OmniBackgroundElevated = Color(0xFF18171E)
    var OmniBackgroundGradientTop = Color(0xFF2D1C27)
    val OmniBackgroundGradientBottom: Color
    get() = OmniBackgroundBase

    var Background = OmniBackgroundBase
    val BackgroundAlt = Color(0xFF15151C)
    var Surface = Color(0xFF17161D)
    var SurfaceElevated = OmniBackgroundElevated
    val SurfaceHairline = Color.White.copy(alpha = 0.06f)
    val SurfacePressed = Color.White.copy(alpha = 0.08f)
    val SurfaceQuiet = Color(0xFF15141B).copy(alpha = 0.72f)
    val SurfacePanel = Color(0xFF1A1921).copy(alpha = 0.84f)
    val SurfaceSubtle = Color(0xFF18171E).copy(alpha = 0.78f)
    val SurfaceRaised = Color(0xFF201D24).copy(alpha = 0.94f)
    val SurfaceFloating = Color(0xFF15141B).copy(alpha = 0.97f)

    // Glass surfaces
    val OmniGlassSubtle = Color.White.copy(alpha = 0.01f)
    val OmniGlassMedium = Color.White.copy(alpha = 0.02f)
    val OmniGlassStrong = Color.White.copy(alpha = 0.04f)
    val OmniGlassDock = Color(0xFF18171E).copy(alpha = 0.96f)
    val OmniGlassPlayer = Color(0xFF1A1820).copy(alpha = 0.97f)
    val OmniGlassBorderSubtle = Color.White.copy(alpha = 0.01f)
    val OmniGlassBorderStrong = Color.White.copy(alpha = 0.02f)

    val GlassSurface = OmniGlassMedium
    val GlassSurfaceStrong = OmniGlassStrong
    val GlassBorder = OmniGlassBorderStrong
    val GlassBorderLight = OmniGlassBorderSubtle

    // Coral accent family — stable, not M3-derived
    var OmniAccentPrimary = Color(0xFFE47A82)
    var OmniAccentSecondary = Color(0xFFFF9AA2)
    var OmniAccentTertiary = Color(0xFFF99392)
    var OmniAccentWarm = Color(0xFFFFC46B)
    var OmniAccentMuted = Color(0xFFB35C64)
    var OmniAccentSoft = OmniAccentPrimary.copy(alpha = 0.12f)
    var OmniAccentGlow = OmniAccentPrimary.copy(alpha = 0.30f)
    var OmniAccentOnPrimary = Color(0xFF05060A)

    var Primary = OmniAccentPrimary
    var PrimaryLight = Color(0xFFFF8C92)
    var Secondary = OmniAccentSecondary
    var SecondaryLight = Color(0xFFFFA49A)
    val Hot = Color(0xFFFF5C93)
    val HotLight = Color(0xFFFF83AD)

    // Semantic colors
    val Success = Color(0xFF4EDB8F)
    val Warning = Color(0xFFFFC46B)
    val Error = Color(0xFFFF6363)
    val Offline = Color(0xFF6E7787)
    val Downloaded = Color(0xFF6EE7B7)
    var ActivePlayback = OmniAccentPrimary

    // Text
    val TextPrimary = Color(0xFFF3F0F3)
    val TextSecondary = Color(0xFFB7B1B8)
    val TextTertiary = Color(0xFF8F8992)
    val TextDisabled = Color(0xFF444B5C)
    val TextOnAccent = OmniAccentOnPrimary
    val TextMuted = TextTertiary
    val BorderSubtle = Color.White.copy(alpha = 0.07f)

    // Gradients
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
        colors = listOf(OmniGlassSubtle, OmniGlassMedium)
    )
    val BackgroundGradient: Brush
    get() = Brush.verticalGradient(
        colors = listOf(OmniBackgroundGradientTop, OmniBackgroundGradientBottom)
    )

    var PrimaryGradientColors = listOf(OmniAccentPrimary, OmniAccentSecondary)
    var HotGradientColors = listOf(Hot, OmniAccentPrimary)
    var OmniPulseGradientColors = listOf(OmniAccentSecondary, OmniAccentPrimary, OmniAccentTertiary)

    fun updateFromTheme(primary: Color, secondary: Color, tertiary: Color, pureBlack: Boolean = false) {
        // Stable reference palette — do NOT derive from M3 dynamic
        OmniAccentPrimary = Color(0xFFE47A82)
        OmniAccentSecondary = Color(0xFFFF9AA2)
        OmniAccentTertiary = Color(0xFFF99392)
        OmniAccentGlow = OmniAccentPrimary.copy(alpha = 0.30f)
        OmniAccentSoft = OmniAccentPrimary.copy(alpha = 0.12f)
        Primary = OmniAccentPrimary
        ActivePlayback = OmniAccentPrimary
        PrimaryLight = Color(0xFFFF8C92)
        Secondary = OmniAccentSecondary
        SecondaryLight = Color(0xFFFFA49A)
        PrimaryGradient = Brush.horizontalGradient(
            colors = listOf(OmniAccentPrimary, OmniAccentSecondary)
        )
        PrimaryGradientVertical = Brush.verticalGradient(
            colors = listOf(OmniAccentPrimary, OmniAccentTertiary)
        )
        PrimaryGradientColors = listOf(OmniAccentPrimary, OmniAccentSecondary)
        HotGradientColors = listOf(Hot, OmniAccentPrimary)
        OmniPulseGradientColors = listOf(OmniAccentSecondary, OmniAccentPrimary, OmniAccentTertiary)

        if (pureBlack) {
            OmniBackgroundGradientTop = Color.Black
            OmniBackgroundElevated = Color(0xFF050507)
            OmniBackgroundBase = Color.Black
            Background = Color.Black
            Surface = Color(0xFF050507)
            SurfaceElevated = Color(0xFF08090D)
        } else {
            // Stable charcoal reference — no dynamic M3 blending
            OmniBackgroundGradientTop = Color(0xFF2D1C27)
            OmniBackgroundElevated = Color(0xFF18171E)
            OmniBackgroundBase = Color(0xFF111217)
            Background = Color(0xFF111217)
            Surface = Color(0xFF17161D)
            SurfaceElevated = Color(0xFF18171E)
        }
    }

}
