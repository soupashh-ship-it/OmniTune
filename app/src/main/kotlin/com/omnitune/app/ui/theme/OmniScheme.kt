/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Immutable semantic color scheme for the OmniTune design system.
 *
 * This is the replacement path for the legacy mutable [OmniColors] singleton: new and
 * rebuilt UI reads [LocalOmniColors] exclusively; screens migrate wave-by-wave until the
 * singleton can be deleted. Roles are semantic — never name colors after where they are
 * used, only after what they mean.
 *
 * Rules:
 *  - Text contrast roles (primary/secondary/tertiary/disabled) are the ONLY colors allowed
 *    on text. Accent never colors body text.
 *  - Surfaces step monotonically: background < surface < surfaceElevated.
 *  - Artwork-derived palettes map onto accent roles only; they must not touch text or
 *    surface roles beyond [surfaceQuiet]/[backgroundElevated] tinting done by the theme.
 */
@Immutable
data class OmniScheme(
    // Surfaces
    val background: Color,
    val backgroundElevated: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceRaised: Color,
    val surfaceQuiet: Color,
    val hairline: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    // Content
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val textOnAccent: Color,
    // Accent family
    val accent: Color,
    val accentSecondary: Color,
    val accentTertiary: Color,
    val accentMuted: Color,
    val accentSoft: Color,
    val accentGlow: Color,
    // Status
    val success: Color,
    val warning: Color,
    val error: Color,
    val offline: Color,
) {
    /** Horizontal accent gradient for emphasis moments (play buttons, active states). */
    val accentGradient: Brush get() = Brush.horizontalGradient(listOf(accent, accentSecondary))

    /** Vertical accent gradient used sparingly on primary controls. */
    val accentGradientVertical: Brush get() = Brush.verticalGradient(listOf(accent, accentTertiary))

    companion object {        /**
         * Dark scheme factory. [pureBlack] collapses surfaces toward true black for OLED.
         * [accent] seeds the accent family; secondary/tertiary derive as tonal shifts so any
         * artwork-extracted seed produces a coherent ramp.
         */
        fun dark(
            accent: Color = DefaultAccent,
            pureBlack: Boolean = false,
        ): OmniScheme {
            val bg = if (pureBlack) Color.Black else Color(0xFF0C0C0F)
            return OmniScheme(
                background = bg,
                backgroundElevated = if (pureBlack) Color(0xFF070709) else Color(0xFF141419),
                surface = if (pureBlack) Color(0xFF0E0E12) else Color(0xFF181820),
                surfaceElevated = if (pureBlack) Color(0xFF14141A) else Color(0xFF20202A),
                surfaceRaised = if (pureBlack) Color(0xFF181822) else Color(0xFF242430),
                surfaceQuiet = if (pureBlack) Color(0xFF0A0A0D) else Color(0xFF121217),
                hairline = if (pureBlack) Color(0xFF1C1C22) else Color(0xFF262634),
                borderSubtle = if (pureBlack) Color(0xFF18181F) else Color(0xFF242430),
                borderStrong = if (pureBlack) Color(0xFF262630) else Color(0xFF323242),
                textPrimary = Color(0xFFF6F5F8),
                textSecondary = Color(0xFFA5A1B0),
                textTertiary = Color(0xFF6F6B7A),
                textDisabled = Color(0xFF42404C),
                textOnAccent = if (accent.luminance() > 0.6f) Color(0xFF14100E) else Color(0xFFFDF9F7),
                accent = accent,
                accentSecondary = accent.lighten(0.14f),
                accentTertiary = accent.lighten(0.34f),
                accentMuted = accent.copy(alpha = 0.55f).over(bg),
                accentSoft = accent.copy(alpha = 0.14f),
                accentGlow = accent.copy(alpha = 0.25f),
                success = Color(0xFF34D399),
                warning = Color(0xFFFBBF24),
                error = Color(0xFFF87171),
                offline = Color(0xFF6B7280),
            )
        }

        /** Light scheme. QA'd after dark per the dark-first mandate; same role contract. */
        fun light(
            accent: Color = DefaultAccent,
        ): OmniScheme {
            val bg = Color(0xFFFAF9FB)
            return OmniScheme(
                background = bg,
                backgroundElevated = Color.White,
                surface = Color.White,
                surfaceElevated = Color(0xFFF2F1F5),
                surfaceRaised = Color(0xFFEBEAf0),
                surfaceQuiet = Color(0xFFF4F3F7),
                hairline = Color(0xFFE4E2EA),
                borderSubtle = Color(0xFFE0DEE6),
                borderStrong = Color(0xFFCFCDD8),
                textPrimary = Color(0xFF17151C),
                textSecondary = Color(0xFF5B5766),
                textTertiary = Color(0xFF8A8694),
                textDisabled = Color(0xFFB9B6C2),
                textOnAccent = if (accent.luminance() > 0.72f) Color(0xFF231512) else Color.White,
                accent = accent,
                accentSecondary = accent.darken(0.08f),
                accentTertiary = accent.darken(0.20f),
                accentMuted = accent.copy(alpha = 0.55f).over(bg),
                accentSoft = accent.copy(alpha = 0.12f),
                accentGlow = accent.copy(alpha = 0.20f),
                success = Color(0xFF0F9D6A),
                warning = Color(0xFFB7791F),
                error = Color(0xFFDC4C4C),
                offline = Color(0xFF71717A),
            )
        }

        /** OmniTune's signature coral. Identity anchor; dynamic palettes may override it. */
        val DefaultAccent: Color = Color(0xFFE65D4F)
    }
}

/** CompositionLocal for the active semantic scheme. Provided once by OmniTuneTheme. */
val LocalOmniColors = staticCompositionLocalOf<OmniScheme> { OmniScheme.dark() }

/** Composable accessor for the active semantic scheme. */
@Composable
fun omniColors(): OmniScheme = LocalOmniColors.current

/** Linear-lighten toward white by [fraction] (0..1). Predictable tonal ramp for accents. */
private fun Color.lighten(fraction: Float): Color = Color(
    red = red + (1f - red) * fraction,
    green = green + (1f - green) * fraction,
    blue = blue + (1f - blue) * fraction,
    alpha = alpha,
)

/** Linear-darken toward black by [fraction] (0..1). */
private fun Color.darken(fraction: Float): Color = Color(
    red = red * (1f - fraction),
    green = green * (1f - fraction),
    blue = blue * (1f - fraction),
    alpha = alpha,
)

/** Composite this color over an opaque [background], returning a fully opaque result. */
private fun Color.over(background: Color): Color {
    val a = alpha.coerceIn(0f, 1f)
    return Color(
        red = red * a + background.red * (1f - a),
        green = green * a + background.green * (1f - a),
        blue = blue * a + background.blue * (1f - a),
        alpha = 1f,
    )
}
