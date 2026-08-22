/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

/**
 * A purely aesthetic animated bar visualizer.
 * Uses simulated wave animation (safe — no RECORD_AUDIO permission needed).
 * If [isPlaying] is false, bars collapse to flat.
 *
 * For real FFT data, replace the waveform math with Visualizer API output.
 */
@Composable
fun AudioVisualizer(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    barCount: Int = 32,
    color: Color = Color.White.copy(alpha = 0.6f),
    activeColor: Color = Color.White,
) {
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            phase += 0.15f
            delay(16L) // ~60fps
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / (barCount * 2f)
        val maxBarHeight = height * 0.85f

        for (i in 0 until barCount) {
            val x = i * (barWidth * 2f) + barWidth / 2f
            val rawAmplitude = if (isPlaying) {
                abs(sin(phase + i * 0.4f)) * 0.6f +
                abs(sin(phase * 1.7f + i * 0.25f)) * 0.3f +
                abs(sin(phase * 0.5f + i * 0.6f)) * 0.1f
            } else 0.05f

            val barHeight = rawAmplitude * maxBarHeight
            val top = height / 2f - barHeight / 2f
            val bottom = height / 2f + barHeight / 2f
            val barColor = if (rawAmplitude > 0.5f) activeColor else color

            drawLine(
                color = barColor,
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}
