package com.omnitune.shared.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.omnitune.shared.ui.theme.SuvMusicThemeEngine
import kotlin.math.sin

/**
 * 9 customizable waveform seekbar styles.
 */
enum class WaveformStyle(val styleName: String) {
    BARS("Bars"),
    MIRROR("Mirror"),
    ROUNDED("Rounded"),
    GRADIENT("Gradient"),
    SMOOTH("Smooth"),
    STEPPED("Stepped"),
    DOTS("Dots"),
    WAVE("Wave"),
    MINIMAL("Minimal");

    companion object {
        fun fromName(name: String?): WaveformStyle {
            if (name.isNullOrBlank()) return BARS
            return entries.firstOrNull { it.styleName.equals(name, ignoreCase = true) } ?: BARS
        }
    }
}

/**
 * Formats millisecond duration into mm:ss or hh:mm:ss format.
 */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00"
    val totalSeconds = durationMs / 1000L
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes}:${seconds.toString().padStart(2, '0')}"
    }
}

/**
 * Waveform seeker with 9 style options, duration > 0 guard, and interactive scrubbing.
 */
@Composable
fun WaveformSeeker(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    style: WaveformStyle = WaveformStyle.BARS,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    showTimeLabels: Boolean = true
) {
    // Guard against non-positive or NaN durations
    val safeDuration = if (durationMs > 0L) durationMs else 1L
    val actualFraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val displayFraction = if (isDragging) dragFraction else actualFraction
    val displayPositionMs = SuvMusicThemeEngine.calculateWaveformPosition(displayFraction, durationMs)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val targetMs = SuvMusicThemeEngine.calculateWaveformPosition(fraction, durationMs)
                        onSeek(targetMs)
                    }
                }
                .pointerInput(safeDuration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            val targetMs = SuvMusicThemeEngine.calculateWaveformPosition(dragFraction, durationMs)
                            onSeek(targetMs)
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val progressX = w * displayFraction

                when (style) {
                    WaveformStyle.BARS -> {
                        val barCount = 48
                        val spacing = 2.dp.toPx()
                        val barWidth = (w - (barCount - 1) * spacing) / barCount
                        for (i in 0 until barCount) {
                            val barX = i * (barWidth + spacing)
                            val normalizedHeight = (0.25f + 0.75f * ((sin(i * 0.45) + 1.0) / 2.0).toFloat()) * (h * 0.85f)
                            val barColor = if (barX <= progressX) activeColor else inactiveColor
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(barX, (h - normalizedHeight) / 2f),
                                size = Size(barWidth, normalizedHeight),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                    }
                    WaveformStyle.MIRROR -> {
                        val barCount = 40
                        val spacing = 2.dp.toPx()
                        val barWidth = (w - (barCount - 1) * spacing) / barCount
                        for (i in 0 until barCount) {
                            val barX = i * (barWidth + spacing)
                            val halfHeight = (0.2f + 0.8f * ((sin(i * 0.5) + 1.0) / 2.0).toFloat()) * (h * 0.4f)
                            val barColor = if (barX <= progressX) activeColor else inactiveColor
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(barX, h / 2f - halfHeight),
                                size = Size(barWidth, halfHeight * 2f),
                                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                            )
                        }
                    }
                    WaveformStyle.ROUNDED -> {
                        val trackHeight = 8.dp.toPx()
                        val trackY = (h - trackHeight) / 2f
                        drawRoundRect(
                            color = inactiveColor,
                            topLeft = Offset(0f, trackY),
                            size = Size(w, trackHeight),
                            cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                        )
                        if (progressX > 0f) {
                            drawRoundRect(
                                color = activeColor,
                                topLeft = Offset(0f, trackY),
                                size = Size(progressX, trackHeight),
                                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                            )
                        }
                        drawCircle(
                            color = activeColor,
                            radius = 6.dp.toPx(),
                            center = Offset(progressX, h / 2f)
                        )
                    }
                    WaveformStyle.GRADIENT -> {
                        val trackHeight = 6.dp.toPx()
                        val trackY = (h - trackHeight) / 2f
                        drawRoundRect(
                            color = inactiveColor,
                            topLeft = Offset(0f, trackY),
                            size = Size(w, trackHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                        if (progressX > 0f) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(activeColor.copy(alpha = 0.7f), activeColor)
                                ),
                                topLeft = Offset(0f, trackY),
                                size = Size(progressX, trackHeight),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                    }
                    WaveformStyle.SMOOTH -> {
                        val trackHeight = 5.dp.toPx()
                        val trackY = (h - trackHeight) / 2f
                        drawRoundRect(
                            color = inactiveColor,
                            topLeft = Offset(0f, trackY),
                            size = Size(w, trackHeight),
                            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                        )
                        if (progressX > 0f) {
                            drawRoundRect(
                                color = activeColor,
                                topLeft = Offset(0f, trackY),
                                size = Size(progressX, trackHeight),
                                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                            )
                        }
                    }
                    WaveformStyle.STEPPED -> {
                        val steps = 24
                        val stepW = w / steps
                        for (i in 0 until steps) {
                            val x = i * stepW
                            val isFilled = x <= progressX
                            val barH = (0.3f + 0.7f * (i % 4) / 4f) * h * 0.7f
                            drawRect(
                                color = if (isFilled) activeColor else inactiveColor,
                                topLeft = Offset(x, h - barH),
                                size = Size(stepW * 0.8f, barH)
                            )
                        }
                    }
                    WaveformStyle.DOTS -> {
                        val dotCount = 36
                        val spacing = w / (dotCount - 1)
                        for (i in 0 until dotCount) {
                            val cx = i * spacing
                            val dotColor = if (cx <= progressX) activeColor else inactiveColor
                            drawCircle(
                                color = dotColor,
                                radius = 3.dp.toPx(),
                                center = Offset(cx, h / 2f)
                            )
                        }
                    }
                    WaveformStyle.WAVE -> {
                        val segments = 60
                        val step = w / segments
                        var prevX = 0f
                        var prevY = h / 2f
                        for (i in 1..segments) {
                            val currX = i * step
                            val currY = (h / 2f) + sin(i * 0.35) .toFloat() * (h * 0.35f)
                            val lineColor = if (currX <= progressX) activeColor else inactiveColor
                            drawLine(
                                color = lineColor,
                                start = Offset(prevX, prevY),
                                end = Offset(currX, currY),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            prevX = currX
                            prevY = currY
                        }
                    }
                    WaveformStyle.MINIMAL -> {
                        val trackHeight = 3.dp.toPx()
                        val trackY = (h - trackHeight) / 2f
                        drawLine(
                            color = inactiveColor,
                            start = Offset(0f, trackY),
                            end = Offset(w, trackY),
                            strokeWidth = trackHeight
                        )
                        if (progressX > 0f) {
                            drawLine(
                                color = activeColor,
                                start = Offset(0f, trackY),
                                end = Offset(progressX, trackY),
                                strokeWidth = trackHeight
                            )
                        }
                    }
                }
            }
        }

        if (showTimeLabels) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(displayPositionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDuration(durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
