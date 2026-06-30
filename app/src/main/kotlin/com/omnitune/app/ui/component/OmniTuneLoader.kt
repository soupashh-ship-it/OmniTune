/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing

@Composable
fun OmniTuneLoader(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color? = null,
) {
    OmniWaveformLoader(
        modifier = modifier,
        size = size,
        color = color ?: OmniColors.ActivePlayback,
    )
}

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
        OmniDiscPulse(size = size, color = color)
        OmniWaveformLoader(size = size * 0.72f, color = color)
    }
}

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
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "omni_waveform_phase",
    )

    Row(
        modifier = modifier.size(size),
        horizontalArrangement = Arrangement.spacedBy(size * 0.10f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val levels = listOf(0.38f, 0.72f, 0.52f, 0.88f, 0.44f)
        levels.forEachIndexed { index, base ->
            val offset = ((phase + index * 0.17f) % 1f)
            val height = (base * (0.72f + offset * 0.36f)).coerceIn(0.28f, 0.96f)
            Box(
                modifier = Modifier
                    .width(size * 0.10f)
                    .fillMaxHeight(height)
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
            animation = tween(1200, easing = FastOutSlowInEasing),
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
        Spacer(modifier = Modifier.width(OmniSpacing.small))
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
