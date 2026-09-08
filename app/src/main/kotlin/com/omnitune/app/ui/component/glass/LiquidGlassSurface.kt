/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.component.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * iOS-style Liquid Glass surface.
 *
 * Layered composition:
 *   1. Adaptive shadow
 *   2. Blurred backdrop (RenderEffect on API 31+, .blur() fallback)
 *   3. Frosted base tint + grain
 *   4. Specular highlight gradient
 *   5. Luminous rim border
 *   6. Content slot
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    blurAmount: Float = 60f,
    intensity: Float = 1f,
    tint: Color = Color.Unspecified,
    isDarkTheme: Boolean,
    drawShadow: Boolean = true,
    drawRim: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val i = intensity.coerceIn(0f, 1.5f)
    val baseAlpha = if (isDarkTheme) 0.45f * i else 0.35f * i
    val effectiveTint = if (tint == Color.Unspecified) {
        if (isDarkTheme) Color(0xFF1A1A1E).copy(alpha = baseAlpha)
        else Color.White.copy(alpha = baseAlpha)
    } else {
        if (!isDarkTheme) {
            Color.White.copy(alpha = 0.2f * i).compositeOver(tint.copy(alpha = baseAlpha * 0.5f))
        } else {
            tint.copy(alpha = baseAlpha)
        }
    }

    val specularHighlight = remember(isDarkTheme, i) {
        Brush.verticalGradient(
            colors = if (isDarkTheme) listOf(
                Color.White.copy(alpha = 0.20f * i),
                Color.White.copy(alpha = 0.07f * i),
                Color.Transparent,
                Color.Transparent,
                Color.Black.copy(alpha = 0.10f * i)
            ) else listOf(
                Color.White.copy(alpha = 0.28f * i),
                Color.White.copy(alpha = 0.09f * i),
                Color.Transparent,
                Color.Transparent,
                Color.Black.copy(alpha = 0.05f * i)
            )
        )
    }

    val topSheen = remember(isDarkTheme, i) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = (if (isDarkTheme) 0.30f else 0.42f) * i),
            0.12f to Color.Transparent
        )
    }

    val borderBrush = remember(isDarkTheme, i) {
        Brush.verticalGradient(
            colors = if (isDarkTheme) listOf(
                Color.White.copy(alpha = 0.42f * i),
                Color.White.copy(alpha = 0.12f * i),
                Color.White.copy(alpha = 0.05f * i),
                Color.White.copy(alpha = 0.22f * i)
            ) else listOf(
                Color.White.copy(alpha = 0.55f * i),
                Color.White.copy(alpha = 0.18f * i),
                Color.Black.copy(alpha = 0.03f * i),
                Color.Black.copy(alpha = 0.10f * i)
            )
        )
    }

    val grainFractions = remember {
        val rand = Random(42)
        FloatArray(800 * 2) { rand.nextFloat() }
    }
    val grainColor = remember(isDarkTheme) {
        Color.White.copy(alpha = if (isDarkTheme) 0.03f else 0.02f)
    }

    Box(modifier = modifier) {
        if (drawShadow) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(
                        elevation = if (isDarkTheme) 32.dp else 20.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.45f else 0.15f),
                        spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.35f else 0.10f)
                    )
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurAmount > 0.5f) {
                        Modifier.graphicsLayer {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                blurAmount,
                                blurAmount,
                                android.graphics.Shader.TileMode.DECAL
                            ).asComposeRenderEffect()
                        }
                    } else if (blurAmount > 0.5f) {
                        Modifier.blur((blurAmount / 1.5f).dp)
                    } else Modifier
                )
                .drawWithCache {
                    val grainPoints = ArrayList<Offset>(grainFractions.size / 2)
                    var idx = 0
                    while (idx < grainFractions.size) {
                        grainPoints.add(
                            Offset(
                                grainFractions[idx] * size.width,
                                grainFractions[idx + 1] * size.height
                            )
                        )
                        idx += 2
                    }
                    onDrawWithContent {
                        drawRect(color = effectiveTint)
                        drawPoints(
                            points = grainPoints,
                            pointMode = PointMode.Points,
                            color = grainColor,
                            strokeWidth = 1f
                        )
                        drawContent()
                    }
                }
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(specularHighlight)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(topSheen)
        )

        if (drawRim) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(width = 0.8.dp, brush = borderBrush, shape = shape)
            )
        }

        content()
    }
}
