/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration for a glass-effect surface.
 *
 * @property blurRadius      How much to blur the content beneath (0.dp = no blur / fallback).
 * @property surfaceTint     The base tint color brushed over the blurred content.
 * @property surfaceAlpha    Alpha applied to [surfaceTint].
 * @property overlayColor    A second gradient stop color (if null, a solid tint is used).
 * @property overlayAlpha    Alpha applied to [overlayColor].
 * @property borderColor     Color for the subtle edge highlight.
 * @property borderAlpha     Alpha for [borderColor].
 * @property borderWidth     Stroke width of the border.
 * @property shadowElevation Elevation for the drop shadow.
 * @property shadowAmbient   Ambient shadow color.
 * @property shadowSpot      Spot shadow color (often uses accent glow).
 */
data class GlassSurfaceStyle(
    val blurRadius: Dp = 0.dp,
    val surfaceTint: Color = Color.White,
    val surfaceAlpha: Float = 0.02f,
    val overlayColor: Color? = null,
    val overlayAlpha: Float = 0f,
    val borderColor: Color = Color.White,
    val borderAlpha: Float = 0.02f,
    val borderWidth: Dp = 1.dp,
    val shadowElevation: Dp = 0.dp,
    val shadowAmbient: Color = Color.Black.copy(alpha = 0f),
    val shadowSpot: Color = Color.Black.copy(alpha = 0f),
)

/**
 * Pre-configured glass styles for common OmniTune surfaces.
 *
 * On Android 12+ (API 31+) real [Modifier.blur] is applied to the background
 * layer only, keeping foreground content sharp.
 * On older devices the blur is skipped and only the tint/border/shadows are drawn.
 */
object OmniGlassDefaults {

    // ── Navigation bar styles ───────────────────────────────────────────

    val NavigationBarDark = GlassSurfaceStyle(
        blurRadius = 0.dp,
        surfaceTint = Color(0xFF121216),
        surfaceAlpha = 1f,
        borderColor = Color(0xFF242430),
        borderAlpha = 1f,
        borderWidth = 1.dp,
        shadowElevation = 8.dp,
        shadowAmbient = Color.Black.copy(alpha = 0.4f),
        shadowSpot = Color.Black.copy(alpha = 0.3f),
    )

    val NavigationBarLight = GlassSurfaceStyle(
        blurRadius = 0.dp,
        surfaceTint = Color(0xFFF0EFF4),
        surfaceAlpha = 1f,
        borderColor = Color(0xFFD6D4DE),
        borderAlpha = 1f,
        borderWidth = 1.dp,
        shadowElevation = 6.dp,
    )

    val NavigationBarPureBlack = GlassSurfaceStyle(
        blurRadius = 0.dp,
        surfaceTint = Color.Black,
        surfaceAlpha = 1f,
        borderColor = Color(0xFF1E1E26),
        borderAlpha = 1f,
        borderWidth = 1.dp,
        shadowElevation = 8.dp,
        shadowAmbient = Color.Black.copy(alpha = 0.5f),
    )

    // ── MiniPlayer styles ───────────────────────────────────────────────

    val MiniPlayerDark = GlassSurfaceStyle(
        blurRadius = 0.dp,
        surfaceTint = Color(0xFF181822),
        surfaceAlpha = 1f,
        borderColor = Color(0xFF282836),
        borderAlpha = 1f,
        borderWidth = 1.dp,
        shadowElevation = 12.dp,
        shadowAmbient = Color.Black.copy(alpha = 0.5f),
        shadowSpot = Color.Black.copy(alpha = 0.4f),
    )

    val MiniPlayerPureBlack = GlassSurfaceStyle(
        blurRadius = 0.dp,
        surfaceTint = Color(0xFF0C0C10),
        surfaceAlpha = 1f,
        borderColor = Color(0xFF1C1C24),
        borderAlpha = 1f,
        borderWidth = 1.dp,
        shadowElevation = 12.dp,
        shadowAmbient = Color.Black.copy(alpha = 0.5f),
    )

    // ── Card / Panel styles ─────────────────────────────────────────────

    val CardDark = GlassSurfaceStyle(
        blurRadius = 0.dp,
        surfaceTint = Color(0xFF181820),
        surfaceAlpha = 1f,
        borderColor = Color(0xFF262634),
        borderAlpha = 1f,
        borderWidth = 1.dp,
        shadowElevation = 2.dp,
        shadowAmbient = Color.Black.copy(alpha = 0.2f),
    )

    val CardElevated = GlassSurfaceStyle(
        blurRadius = 0.dp,
        surfaceTint = Color(0xFF20202A),
        surfaceAlpha = 1f,
        borderColor = Color(0xFF2E2E3E),
        borderAlpha = 1f,
        borderWidth = 1.dp,
        shadowElevation = 6.dp,
        shadowAmbient = Color.Black.copy(alpha = 0.3f),
        shadowSpot = Color.Black.copy(alpha = 0.2f),
    )

    // ── Selectors ───────────────────────────────────────────────────────

    fun navigationBarStyle(
        isDark: Boolean = true,
        isPureBlack: Boolean = false,
    ): GlassSurfaceStyle = when {
        isPureBlack -> NavigationBarPureBlack
        isDark -> NavigationBarDark
        else -> NavigationBarLight
    }

    fun miniPlayerStyle(
        isDark: Boolean = true,
        isPureBlack: Boolean = false,
    ): GlassSurfaceStyle = when {
        isPureBlack -> MiniPlayerPureBlack
        else -> MiniPlayerDark
    }
}

// ── Background blur composable ────────────────────────────────────────

/**
 * A glass-surface container that applies blur only to the **background layer**
 * while keeping all foreground [content] sharp and crisp.
 *
 * Architecture (layered inside a Box):
 * ```
 * ┌─ shadow ─────────────────────────┐
 * │  ┌─ clip ──────────────────────┐ │
 * │  │  ┌─ blurred background ───┐ │ │
 * │  │  │  (tint + blur)         │ │ │
 * │  │  ├────────────────────────┤ │ │
 * │  │  │  sharp foreground      │ │ │
 * │  │  │  (icons, text, etc.)   │ │ │
 * │  │  └────────────────────────┘ │ │
 * │  └─────────────────────────────┘ │
 * └──────────────────────────────────┘
 * ```
 *
 * @param shape     The shape to clip the surface to.
 * @param style     The [GlassSurfaceStyle] to apply.
 * @param modifier  Optional modifier for the outer container.
 * @param content   The sharp, un-blurred foreground content.
 */
@Composable
fun OmniGlassSurface(
    shape: Shape,
    style: GlassSurfaceStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val shouldBlur = canBlur && style.blurRadius > 0.dp

    Box(
        modifier = modifier
            .then(
                if (style.shadowElevation > 0.dp) {
                    Modifier.shadow(
                        elevation = style.shadowElevation,
                        shape = shape,
                        ambientColor = style.shadowAmbient,
                        spotColor = style.shadowSpot,
                    )
                } else Modifier
            )
            .clip(shape),
    ) {
        // ── Background layer (blurred) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (style.overlayColor != null) {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    style.surfaceTint.copy(alpha = style.surfaceAlpha),
                                    style.overlayColor.copy(alpha = style.overlayAlpha),
                                )
                            )
                        )
                    } else {
                        Modifier.background(
                            style.surfaceTint.copy(alpha = style.surfaceAlpha)
                        )
                    }
                )
                .then(
                    if (shouldBlur) Modifier.blur(radius = style.blurRadius) else Modifier
                )
                .border(
                    width = style.borderWidth,
                    color = style.borderColor.copy(alpha = style.borderAlpha),
                    shape = shape,
                ),
        )

        // ── Foreground content (sharp) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape),
            content = content,
        )
    }
}
