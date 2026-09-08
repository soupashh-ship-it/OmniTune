/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.models.SeekbarStyle
import com.omnitune.app.viewmodels.SettingsViewModel
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekbarStyleScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seekbar Style", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Choose how the seekbar appears on the player screen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            SeekbarStyle.entries.forEach { style ->
                SeekbarStylePreviewCard(
                    style = style,
                    isSelected = style == uiState.seekbarStyle,
                    primaryColor = primaryColor,
                    surfaceColor = surfaceColor,
                    onClick = { viewModel.setSeekbarStyle(style) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You can also change this from the player by long-pressing on the seekbar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun SeekbarStylePreviewCard(
    style: SeekbarStyle,
    isSelected: Boolean,
    primaryColor: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = spring(),
        label = "bg",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(),
        label = "border",
    )

    val styleName = when (style) {
        SeekbarStyle.M3E_WAVY -> "M3 Expressive Wavy"
        SeekbarStyle.WAVEFORM -> "Waveform"
        SeekbarStyle.WAVE_LINE -> "Wave Line"
        SeekbarStyle.CLASSIC -> "Classic (YouTube Music)"
        SeekbarStyle.DOTS -> "Dots"
        SeekbarStyle.GRADIENT_BAR -> "Gradient Bar"
        SeekbarStyle.NEON -> "Neon Glow"
        SeekbarStyle.BLOCKS -> "Blocks / Segments"
        SeekbarStyle.MATERIAL -> "Material 3 Slider"
    }

    val styleDescription = when (style) {
        SeekbarStyle.M3E_WAVY -> "Modern Android 13+ expressive wavy animation"
        SeekbarStyle.WAVEFORM -> "Soundcloud-style audio waveform bars"
        SeekbarStyle.WAVE_LINE -> "Smooth flowing sine wave"
        SeekbarStyle.CLASSIC -> "Minimal flat progress bar with thumb"
        SeekbarStyle.DOTS -> "Row of connected glowing dots"
        SeekbarStyle.GRADIENT_BAR -> "Smooth multi-color gradient transition"
        SeekbarStyle.NEON -> "Vibrant futuristic neon aesthetic"
        SeekbarStyle.BLOCKS -> "Equalized segmented visualizer blocks"
        SeekbarStyle.MATERIAL -> "Standard Material Design slider"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = styleName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = styleDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (isSelected) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.titleLarge,
                        color = primaryColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas preview of the seekbar style
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(horizontal = 8.dp),
            ) {
                when (style) {
                    SeekbarStyle.M3E_WAVY -> drawWavyPreview(primaryColor, surfaceColor)
                    SeekbarStyle.WAVEFORM -> drawWaveformPreview(primaryColor, surfaceColor)
                    SeekbarStyle.WAVE_LINE -> drawWaveLinePreview(primaryColor, surfaceColor)
                    SeekbarStyle.CLASSIC, SeekbarStyle.MATERIAL -> drawClassicPreview(primaryColor, surfaceColor)
                    SeekbarStyle.DOTS -> drawDotsPreview(primaryColor, surfaceColor)
                    SeekbarStyle.GRADIENT_BAR -> drawGradientPreview(primaryColor, surfaceColor)
                    SeekbarStyle.NEON -> drawNeonPreview(primaryColor, surfaceColor)
                    SeekbarStyle.BLOCKS -> drawBlocksPreview(primaryColor, surfaceColor)
                }
            }
        }
    }
}

private fun DrawScope.drawClassicPreview(primaryColor: Color, surfaceColor: Color) {
    val progress = 0.6f
    val trackHeight = 4.dp.toPx()
    val y = size.height / 2

    // Inactive track
    drawRoundRect(
        color = surfaceColor,
        topLeft = Offset(0f, y - trackHeight / 2),
        size = Size(size.width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2),
    )

    // Active track
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(0f, y - trackHeight / 2),
        size = Size(size.width * progress, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2),
    )

    // Thumb
    drawCircle(
        color = primaryColor,
        radius = 6.dp.toPx(),
        center = Offset(size.width * progress, y),
    )
}

private fun DrawScope.drawWavyPreview(primaryColor: Color, surfaceColor: Color) {
    val progress = 0.6f
    val y = size.height / 2
    val wavePath = Path()
    val activeWidth = size.width * progress

    wavePath.moveTo(0f, y)
    var x = 0f
    val wavelength = 24.dp.toPx()
    val amplitude = 6.dp.toPx()

    while (x <= activeWidth) {
        val waveY = y + sin(x / wavelength * 2 * Math.PI.toFloat()) * amplitude
        wavePath.lineTo(x, waveY)
        x += 2f
    }

    drawPath(
        path = wavePath,
        color = primaryColor,
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
    )

    // Flat inactive track
    drawLine(
        color = surfaceColor,
        start = Offset(activeWidth, y),
        end = Offset(size.width, y),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawWaveformPreview(primaryColor: Color, surfaceColor: Color) {
    val progress = 0.6f
    val barWidth = 3.dp.toPx()
    val barGap = 2.dp.toPx()
    val totalBarWidth = barWidth + barGap
    val barCount = (size.width / totalBarWidth).toInt()
    val random = Random(42)

    for (i in 0 until barCount) {
        val x = i * totalBarWidth
        val heightFraction = 0.2f + random.nextFloat() * 0.8f
        val barHeight = size.height * heightFraction
        val y = (size.height - barHeight) / 2
        val isPlayed = (x / size.width) <= progress

        drawRoundRect(
            color = if (isPlayed) primaryColor else surfaceColor,
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2),
        )
    }
}

private fun DrawScope.drawWaveLinePreview(primaryColor: Color, surfaceColor: Color) {
    val progress = 0.6f
    val y = size.height / 2
    val path = Path()
    val wavelength = 32.dp.toPx()
    val amplitude = 8.dp.toPx()

    path.moveTo(0f, y)
    for (i in 0..size.width.toInt() step 2) {
        val waveY = y + sin(i.toFloat() / wavelength * 2 * Math.PI.toFloat()) * amplitude
        path.lineTo(i.toFloat(), waveY)
    }

    // Inactive full wave
    drawPath(
        path = path,
        color = surfaceColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
    )

    // Active wave
    val activePath = Path()
    activePath.moveTo(0f, y)
    for (i in 0..(size.width * progress).toInt() step 2) {
        val waveY = y + sin(i.toFloat() / wavelength * 2 * Math.PI.toFloat()) * amplitude
        activePath.lineTo(i.toFloat(), waveY)
    }

    drawPath(
        path = activePath,
        color = primaryColor,
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawDotsPreview(primaryColor: Color, surfaceColor: Color) {
    val progress = 0.6f
    val dotSpacing = 12.dp.toPx()
    val dotCount = (size.width / dotSpacing).toInt()
    val y = size.height / 2

    for (i in 0 until dotCount) {
        val x = i * dotSpacing + dotSpacing / 2
        val isPlayed = (x / size.width) <= progress

        drawCircle(
            color = if (isPlayed) primaryColor else surfaceColor,
            radius = if (isPlayed) 3.5.dp.toPx() else 2.5.dp.toPx(),
            center = Offset(x, y),
        )
    }
}

private fun DrawScope.drawGradientPreview(primaryColor: Color, surfaceColor: Color) {
    val progress = 0.6f
    val trackHeight = 6.dp.toPx()
    val y = size.height / 2

    drawRoundRect(
        color = surfaceColor,
        topLeft = Offset(0f, y - trackHeight / 2),
        size = Size(size.width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2),
    )

    val gradient = Brush.horizontalGradient(
        colors = listOf(primaryColor.copy(alpha = 0.5f), primaryColor, Color(0xFFFF5722)),
        startX = 0f,
        endX = size.width * progress,
    )

    drawRoundRect(
        brush = gradient,
        topLeft = Offset(0f, y - trackHeight / 2),
        size = Size(size.width * progress, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2),
    )
}

private fun DrawScope.drawNeonPreview(primaryColor: Color, surfaceColor: Color) {
    val progress = 0.6f
    val trackHeight = 4.dp.toPx()
    val y = size.height / 2

    drawRoundRect(
        color = surfaceColor,
        topLeft = Offset(0f, y - trackHeight / 2),
        size = Size(size.width, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2),
    )

    // Outer glow
    drawRoundRect(
        color = primaryColor.copy(alpha = 0.3f),
        topLeft = Offset(0f, y - trackHeight),
        size = Size(size.width * progress, trackHeight * 2),
        cornerRadius = CornerRadius(trackHeight),
    )

    // Inner bright core
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(0f, y - trackHeight / 2),
        size = Size(size.width * progress, trackHeight),
        cornerRadius = CornerRadius(trackHeight / 2),
    )
}

private fun DrawScope.drawBlocksPreview(primaryColor: Color, surfaceColor: Color) {
    val progress = 0.6f
    val blockWidth = 8.dp.toPx()
    val blockGap = 3.dp.toPx()
    val totalWidth = blockWidth + blockGap
    val blockCount = (size.width / totalWidth).toInt()
    val blockHeight = 8.dp.toPx()
    val y = (size.height - blockHeight) / 2

    for (i in 0 until blockCount) {
        val x = i * totalWidth
        val isPlayed = (x / size.width) <= progress

        drawRoundRect(
            color = if (isPlayed) primaryColor else surfaceColor,
            topLeft = Offset(x, y),
            size = Size(blockWidth, blockHeight),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
    }
}
