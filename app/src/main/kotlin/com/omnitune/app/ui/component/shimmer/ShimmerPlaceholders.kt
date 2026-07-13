/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.component.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing

// ── Base shimmer shape ────────────────────────────────────────────────

/**
 * A rounded rectangle that acts as a shimmer-able placeholder shape.
 * Must be placed inside a [ShimmerHost] or [ShimmerRowHost] to animate.
 */
@Composable
fun ShimmerShape(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(OmniColors.SurfaceRaised),
    )
}

// ── Text shimmer ──────────────────────────────────────────────────────

/**
 * A shimmer text line with randomized (but deterministic) width.
 * Place inside [ShimmerHost].
 */
@Composable
fun ShimmerTextLine(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    widthFraction: Float = 0.72f,
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(shape)
            .background(OmniColors.SurfaceRaised),
    )
}

// ── Track/List row shimmer ────────────────────────────────────────────

/**
 * A shimmer placeholder for a single track/song list row.
 * Matches the layout of [com.omnitune.app.ui.component.OmniMusicRow].
 *
 * Layout:
 * ```
 * [artwork]  [title line]
 *            [subtitle line]
 * ```
 */
@Composable
fun ShimmerTrackRow(
    modifier: Modifier = Modifier,
    artworkSize: Dp = 44.dp,
    contentPadding: Dp = OmniSpacing.compact,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = contentPadding, vertical = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShimmerShape(modifier = Modifier.size(artworkSize))
        Spacer(Modifier.width(OmniSpacing.small))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ShimmerTextLine(
                widthFraction = 0.72f,
                height = 14.dp,
            )
            ShimmerTextLine(
                widthFraction = 0.48f,
                height = 10.dp,
            )
        }
    }
}

// ── Grid/Card shimmer ─────────────────────────────────────────────────

/**
 * A shimmer placeholder for an album/playlist grid card.
 *
 * Layout:
 * ```
 * [───── square artwork ─────]
 * [title line               ]
 * [subtitle line            ]
 * ```
 */
@Composable
fun ShimmerGridItem(
    modifier: Modifier = Modifier,
    width: Dp = 144.dp,
) {
    Column(
        modifier = modifier
            .padding(12.dp)
            .width(width),
    ) {
        ShimmerShape(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        Spacer(Modifier.height(OmniSpacing.small))
        ShimmerTextLine(
            widthFraction = 0.82f,
            height = 13.dp,
        )
        Spacer(Modifier.height(4.dp))
        ShimmerTextLine(
            widthFraction = 0.55f,
            height = 10.dp,
        )
    }
}

// ── Hero banner shimmer ───────────────────────────────────────────────

/**
 * A shimmer placeholder for a hero/banner section on the Home screen.
 *
 * Layout:
 * ```
 * [───────────── wide artwork ─────────────]
 * [title line                              ]
 * [subtitle line                           ]
 * ```
 */
@Composable
fun ShimmerHeroBanner(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.small),
    ) {
        ShimmerShape(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.2f),
        )
        Spacer(Modifier.height(OmniSpacing.small))
        ShimmerTextLine(
            widthFraction = 0.65f,
            height = 15.dp,
        )
        Spacer(Modifier.height(4.dp))
        ShimmerTextLine(
            widthFraction = 0.40f,
            height = 11.dp,
        )
    }
}

// ── Full-screen loading state shimmer ─────────────────────────────────

/**
 * A full list loading state with repeating track rows inside a [ShimmerHost].
 */
@Composable
fun ShimmerTrackList(
    modifier: Modifier = Modifier,
    rowCount: Int = 6,
) {
    ShimmerHost(modifier = modifier) {
        repeat(rowCount) {
            ShimmerTrackRow()
        }
    }
}
