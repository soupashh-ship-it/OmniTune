/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import kotlin.math.PI
import kotlin.math.sin

//
// ── OmniTune Loader ───────────────────────────────────────────────────
//
// A completely original loading animation built from OmniTune's own brand
// identity: the **signal bars** — 4 vertical bars that form the app's logo
// mark (first seen in the Home screen header).
//
// Concept: "audio visualizer" — the four bars cascade up and down like an
// equalizer, while the whole group gently breathes in scale, rotates
// smoothly, and pulses with a soft glow at the base.
//
// This is 100% original to OmniTune and shares NO code or visual design
// with Velune's V-shaped loader. The signal bars are OmniTune's own brand.
//
// Layers:
//   1. Soft glow behind the tallest bar (index 3)
//   2. 4 rounded pill bars anchored at the bottom, heights oscillate as a
//      cascading wave (like an audio spectrum visualizer)
//   3. Gentle breathing scale and rotation for fluidity
//   4. Gradient fill — tallest bar uses the secondary accent, rest use primary
//

/** Default bar heights for the signal-mark identity (in dp-equivalent units). */
private val BAR_HEIGHTS = listOf(0.32f, 0.58f, 0.42f, 0.78f) // fractions of total height

/**
 * OmniTune's primary loading spinner — an animated signal-bars visualizer.
 *
 * The animation runs three simultaneous effects:
 * - **Cascading wave**: each bar's height oscillates with a staggered phase,
 *   creating a ripple/equalizer feel.
 * - **Breathing scale**: the whole group scales between 0.85 and 1.0.
 * - **Gentle rotation**: the group rocks -4° to +4° for fluid motion.
 *
 * @param size   Diameter of the loader bounding box in dp. Default 40dp.
 * @param color  Primary accent color. If null, defaults to [OmniColors.ActivePlayback].
 */
@Composable
fun OmniTuneLoader(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color? = null,
) {
    OmniSignalLoader(
        modifier = modifier,
        size = size,
        color = color ?: OmniColors.ActivePlayback,
    )
}

/**
 * Internal composable that renders the signal-bars animation on a Canvas.
 */
@Composable
private fun OmniSignalLoader(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = OmniColors.ActivePlayback,
) {
    val transition = rememberInfiniteTransition(label = "omni_signal_loader")

    // ── Cascading wave phase (0 → 1, loops) ──────────────────────────
    // Controls bar-height oscillation. All four bars share this phase but
    // each gets a staggered offset to create a ripple.
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "signal_wave",
    )

    // ── Breathing scale (0.85 → 1) ───────────────────────────────────
    val breathe by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "signal_breathe",
    )

    // ── Gentle rotation (-4° → 4°) ────────────────────────────────────
    val rotation by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "signal_rotation",
    )

    // ── Base alpha pulse (0.75 → 1) ───────────────────────────────────
    val baseAlpha by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "signal_alpha",
    )

    // ── Shimmer sweep phase (0 → 1, wraps) — a bright band moves across
    //     the bars from left to right, then resets.
    val sweepPhase by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "signal_sweep",
    )

    val secondary = OmniColors.OmniAccentSecondary
    val primary = color

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = size.toPx()
            val h = size.toPx()
            val cx = w / 2f
            val cy = h / 2f

            // Bar dimensions
            val barCount = 4
            val barSpacing = w * 0.08f                // gap between bars
            val barWidth = ((w - barSpacing * (barCount - 1)) / barCount).coerceAtMost(size.toPx() * 0.22f)
            val totalBarsWidth = barCount * barWidth + (barCount - 1) * barSpacing
            val startX = cx - totalBarsWidth / 2f

            // Staggered phase offsets for cascading wave
            val phaseOffsets = listOf(0.00f, 0.20f, 0.40f, 0.60f)

            // Apply breathing scale and rotation around center
            rotate(
                degrees = rotation,
                pivot = Offset(cx, cy),
            ) {
                // ── Background glow behind tallest bar ──────────────────
                val glowAlpha = 0.08f * baseAlpha
                if (glowAlpha > 0.005f) {
                    drawCircle(
                        color = secondary.copy(alpha = glowAlpha),
                        radius = w * 0.50f * breathe,
                        center = Offset(cx, h * 0.66f),
                    )
                }

                // ── Draw signal bars ────────────────────────────────────
                for (i in 0 until barCount) {
                    // Staggered sine wave: each bar's height oscillates
                    // around its base height with the wave phase
                    val phase = phaseOffsets[i]
                    // sin goes from -1 to 1, map to 0.4..1.6 multiplier
                    val waveT = sin((wavePhase * 2 * PI + phase * 2 * PI).toFloat())
                    val heightMul = 0.60f + 0.40f * (waveT * 0.50f + 0.50f)

                    val baseHeight = BAR_HEIGHTS[i] * h * breathe
                    val barHeight = (baseHeight * heightMul).coerceIn(h * 0.12f, h * 0.90f)
                    val barTop = h - barHeight
                    val barLeft = startX + i * (barWidth + barSpacing)

                    // Glow under tall bars
                    if (i == 3 && barHeight > h * 0.50f) {
                        val glowH = barHeight * 0.15f
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    secondary.copy(alpha = 0f),
                                    secondary.copy(alpha = 0.12f * baseAlpha),
                                ),
                                startY = barTop,
                                endY = barTop + glowH,
                            ),
                            topLeft = Offset(barLeft - barWidth * 0.25f, barTop),
                            size = Size(barWidth * 1.5f, glowH),
                        )
                    }

                    // Gradient per bar: brighter at top, subtle at bottom
                    val barColor = if (i == 3) secondary else primary

                    // Rounded pill bar
                    drawRoundRect(
                        color = barColor.copy(alpha = baseAlpha),
                        topLeft = Offset(barLeft, barTop),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                    )

                    // Subtle highlight on top of bar
                    if (barHeight > h * 0.25f) {
                        val highlightH = barHeight * 0.18f
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f * baseAlpha),
                                    Color.White.copy(alpha = 0f),
                                ),
                                startY = barTop,
                                endY = barTop + highlightH,
                            ),
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, highlightH),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                        )
                    }
                }

                // ── Shimmer sweep — a bright band glides left→right ────
                // Uses a horizontal gradient that follows sweepPhase.
                // The band is ~40% of total bar width, bright white at center
                // fading to transparent on both sides.
                val sweepBandWidth = totalBarsWidth * 0.40f
                val sweepCenterX = startX + (sweepPhase * totalBarsWidth)
                val sweepLeft = sweepCenterX - sweepBandWidth / 2f
                val sweepRight = sweepCenterX + sweepBandWidth / 2f

                // Only draw if the sweep band overlaps the bars
                if (sweepRight > startX && sweepLeft < startX + totalBarsWidth) {
                    val clipLeft = maxOf(sweepLeft, startX)
                    val clipRight = minOf(sweepRight, startX + totalBarsWidth)
                    val sweepWidth = clipRight - clipLeft

                    if (sweepWidth > 1f) {
                        val sweepAlpha = 0.18f * baseAlpha
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0f),
                                    Color.White.copy(alpha = sweepAlpha),
                                    Color.White.copy(alpha = sweepAlpha),
                                    Color.White.copy(alpha = 0f),
                                ),
                                startX = clipLeft,
                                endX = clipRight,
                            ),
                            topLeft = Offset(clipLeft, h * 0.10f),
                            size = Size(sweepWidth, h * 0.80f),
                        )
                    }
                }
            }
        }
    }
}

// ── OmniWaveformLoader ───────────────────────────────────────────────
//
// Kept as-is. Used separately in MiniPlayer's loading state.

/**
 * A standing waveform loader (bars). Kept for compatibility with [OmniLoadingPulse].
 */
@Composable
fun OmniWaveformLoader(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = OmniColors.ActivePlayback,
) {
    val transition = rememberInfiniteTransition(label = "omni_waveform_loader")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "omni_waveform_phase",
    )

    val barSpacing = size * 0.10f
    Row(
        modifier = modifier.size(size),
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val levels = listOf(0.38f, 0.72f, 0.52f, 0.88f, 0.44f)
        levels.forEachIndexed { index, base ->
            val offset = ((phase + index * 0.17f) % 1f)
            val barHeightFrac = (base * (0.72f + offset * 0.36f)).coerceIn(0.28f, 0.96f)
            Box(
                modifier = Modifier
                    .size(width = barSpacing, height = size * barHeightFrac)
                    .clip(OmniShapes.Pill)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                color.copy(alpha = 0.95f),
                                OmniColors.OmniAccentPrimary.copy(alpha = 0.54f),
                            ),
                        ),
                    ),
            )
        }
    }
}

/**
 * A combined loading indicator: signal loader + waveform bars.
 */
@Composable
fun OmniLoadingPulse(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = OmniColors.ActivePlayback,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
    ) {
        OmniTuneLoader(size = size, color = color)
        OmniWaveformLoader(size = size * 0.72f, color = color)
    }
}

// ── OmniPulseLoader (retained for compatibility) ──────────────────────
//
// Legacy circular ripple + orbital loader. Kept so any existing references
// to OmniPulseLoader still compile, but OmniTuneLoader now uses the new
// signal-bars animation.

/**
 * Legacy circular sonar/ripple animation — retained for compatibility.
 * The new default is [OmniTuneLoader] (signal bars).
 */
@Composable
fun OmniPulseLoader(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = OmniColors.ActivePlayback,
) {
    val infinite = rememberInfiniteTransition(label = "omni_pulse_loader")

    val ripplePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple",
    )
    val arcAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "arc_rotation",
    )
    val breathe by infinite.animateFloat(
        initialValue = 0.80f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "core_breathe",
    )
    val coreAlpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "core_alpha",
    )

    val secondary = OmniColors.OmniAccentSecondary
    val primary = color

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val w = size.toPx()
            val cx = w / 2f
            val cy = w / 2f
            val maxR = w / 2f - 2.dp.toPx()
            val strokeW = w * 0.09f

            repeat(3) { ringIndex ->
                val offset = ringIndex * 0.28f
                val t = ((ripplePhase - offset) % 1f + 1f) % 1f
                val radius = maxR * (0.10f + t * 0.90f)
                val ringAlpha = (1f - t) * 0.30f
                if (ringAlpha > 0.01f) {
                    drawCircle(
                        color = primary.copy(alpha = ringAlpha),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = strokeW * 0.60f, cap = StrokeCap.Round),
                    )
                }
            }

            val sweep = 120f
            val startAngle = arcAngle
            val arcBrush = Brush.sweepGradient(
                colors = listOf(
                    primary.copy(alpha = 0.90f),
                    secondary.copy(alpha = 0.70f),
                    primary.copy(alpha = 0.30f),
                ),
                center = Offset(cx, cy),
            )
            drawArc(
                brush = arcBrush,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(strokeW / 2f, strokeW / 2f),
                size = Size(w - strokeW, w - strokeW),
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
            )

            val midAngle = (startAngle + sweep / 2f) * (PI.toFloat() / 180f)
            val orbitR = maxR - strokeW / 2f
            val dotX = cx + orbitR * kotlin.math.cos(midAngle)
            val dotY = cy + orbitR * kotlin.math.sin(midAngle)
            drawCircle(
                color = secondary,
                radius = strokeW * 0.55f,
                center = Offset(dotX, dotY),
            )

            val coreR = maxR * 0.18f * breathe
            drawCircle(
                color = primary.copy(alpha = 0.12f * coreAlpha),
                radius = maxR * 0.38f * breathe,
            )
            drawCircle(
                color = primary.copy(alpha = coreAlpha),
                radius = coreR,
            )
            drawCircle(
                color = secondary.copy(alpha = 0.70f * coreAlpha),
                radius = coreR * 0.35f,
                center = Offset(cx - coreR * 0.25f, cy - coreR * 0.25f),
            )
        }
    }
}

// ── Legacy placeholders (unchanged) ───────────────────────────────────

@Composable
fun OmniDiscPulse(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = OmniColors.ActivePlayback,
) {
    val transition = rememberInfiniteTransition(label = "omni_disc_loader")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "omni_disc_pulse",
    )
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val radius = this.size.minDimension / 2f
            drawCircle(
                color = color.copy(alpha = 0.12f + pulse * 0.14f),
                radius = radius * (0.74f + pulse * 0.16f),
            )
            drawCircle(
                color = color.copy(alpha = 0.62f),
                radius = radius * 0.52f,
                style = Stroke(width = radius * 0.12f, cap = StrokeCap.Round),
            )
            drawCircle(
                color = OmniColors.OmniAccentPrimary.copy(alpha = 0.55f),
                radius = radius * 0.16f,
            )
        }
    }
}

@Composable
fun OmniThumbnailPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        OmniColors.SurfaceQuiet,
                        OmniColors.SurfacePanel,
                        OmniColors.OmniAccentSecondary.copy(alpha = 0.16f),
                    ),
                ),
            ),
    )
}

@Composable
fun OmniSkeletonLine(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(12.dp)
            .clip(OmniShapes.Pill)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        OmniColors.SurfaceQuiet,
                        OmniColors.OmniAccentSecondary.copy(alpha = 0.10f),
                        OmniColors.SurfacePanel,
                    ),
                ),
            ),
    )
}

@Composable
fun OmniTrackLoadingRow(
    modifier: Modifier = Modifier,
    artworkSize: Dp = 52.dp,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OmniThumbnailPlaceholder(modifier = Modifier.size(artworkSize))
        Spacer(modifier = Modifier.size(width = OmniSpacing.small, height = 0.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
        ) {
            OmniSkeletonLine(modifier = Modifier.fillMaxWidth(0.72f))
            OmniSkeletonLine(
                modifier = Modifier
                    .fillMaxWidth(0.44f)
                    .height(9.dp),
            )
        }
    }
}
