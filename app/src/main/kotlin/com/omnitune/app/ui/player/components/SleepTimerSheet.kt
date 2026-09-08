/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnitune.app.models.SleepTimerOption
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.component.glass.ArtGlassSheet

/**
 * Sleep timer bottom sheet with dynamic colors and timer chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    isVisible: Boolean,
    currentOption: SleepTimerOption,
    remainingTimeFormatted: String?,
    onSelectOption: (SleepTimerOption, Int?) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    dominantColors: DominantColors? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val finalBackgroundColor = if (isDarkTheme) {
        dominantColors?.secondary ?: backgroundColor
    } else {
        MaterialTheme.colorScheme.surface
    }
    val finalContentColor = if (isDarkTheme) {
        dominantColors?.onBackground ?: Color.White
    } else {
        Color.Black
    }
    val finalAccentColor = dominantColors?.accent ?: accentColor

    var showCustomTimerDialog by remember { mutableStateOf(false) }

    if (showCustomTimerDialog) {
        CustomTimerDialog(
            onDismiss = { showCustomTimerDialog = false },
            onConfirm = { minutes ->
                showCustomTimerDialog = false
                onSelectOption(SleepTimerOption.CUSTOM, minutes)
            },
            accentColor = finalAccentColor,
            backgroundColor = finalBackgroundColor,
            contentColor = finalContentColor
        )
    }

    if (isVisible) {
        ArtGlassSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            fallbackContainerColor = finalBackgroundColor,
            showDragHandle = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = finalAccentColor,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sleep Timer",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = finalContentColor
                )

                if (remainingTimeFormatted != null && currentOption != SleepTimerOption.OFF && currentOption != SleepTimerOption.END_OF_SONG) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .background(
                                color = finalAccentColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = remainingTimeFormatted,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = finalAccentColor,
                            letterSpacing = 2.sp
                        )
                    }
                } else if (currentOption == SleepTimerOption.END_OF_SONG) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stops after this song",
                        style = MaterialTheme.typography.bodyMedium,
                        color = finalAccentColor
                    )
                }

                if (currentOption == SleepTimerOption.FADE_OUT_GENTLE || currentOption == SleepTimerOption.FADE_OUT_FAST) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (currentOption == SleepTimerOption.FADE_OUT_FAST) {
                            "Lowering volume by 5% every minute"
                        } else {
                            "Lowering volume by 5% every 2 minutes"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = finalAccentColor
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TimerChip(
                            label = "Off",
                            icon = Icons.Default.TimerOff,
                            isSelected = currentOption == SleepTimerOption.OFF,
                            accentColor = finalAccentColor,
                            contentColorOnBackground = finalContentColor,
                            onClick = { onSelectOption(SleepTimerOption.OFF, null) },
                            modifier = Modifier.weight(1f)
                        )
                        TimerChip(
                            label = "5 min",
                            isSelected = currentOption == SleepTimerOption.FIVE_MIN,
                            accentColor = finalAccentColor,
                            contentColorOnBackground = finalContentColor,
                            onClick = { onSelectOption(SleepTimerOption.FIVE_MIN, null) },
                            modifier = Modifier.weight(1f)
                        )
                        TimerChip(
                            label = "10 min",
                            isSelected = currentOption == SleepTimerOption.TEN_MIN,
                            accentColor = finalAccentColor,
                            contentColorOnBackground = finalContentColor,
                            onClick = { onSelectOption(SleepTimerOption.TEN_MIN, null) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TimerChip(
                            label = "15 min",
                            isSelected = currentOption == SleepTimerOption.FIFTEEN_MIN,
                            accentColor = finalAccentColor,
                            contentColorOnBackground = finalContentColor,
                            onClick = { onSelectOption(SleepTimerOption.FIFTEEN_MIN, null) },
                            modifier = Modifier.weight(1f)
                        )
                        TimerChip(
                            label = "30 min",
                            isSelected = currentOption == SleepTimerOption.THIRTY_MIN,
                            accentColor = finalAccentColor,
                            contentColorOnBackground = finalContentColor,
                            onClick = { onSelectOption(SleepTimerOption.THIRTY_MIN, null) },
                            modifier = Modifier.weight(1f)
                        )
                        TimerChip(
                            label = "45 min",
                            isSelected = currentOption == SleepTimerOption.FORTY_FIVE_MIN,
                            accentColor = finalAccentColor,
                            contentColorOnBackground = finalContentColor,
                            onClick = { onSelectOption(SleepTimerOption.FORTY_FIVE_MIN, null) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TimerChip(
                            label = "1 hour",
                            isSelected = currentOption == SleepTimerOption.ONE_HOUR,
                            accentColor = finalAccentColor,
                            contentColorOnBackground = finalContentColor,
                            onClick = { onSelectOption(SleepTimerOption.ONE_HOUR, null) },
                            modifier = Modifier.weight(1f)
                        )
                        TimerChip(
                            label = "2 hours",
                            isSelected = currentOption == SleepTimerOption.TWO_HOURS,
                            accentColor = finalAccentColor,
                            contentColorOnBackground = finalContentColor,
                            onClick = { onSelectOption(SleepTimerOption.TWO_HOURS, null) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TimerChip(
                            label = "Custom",
                            icon = Icons.Default.Edit,
                            isSelected = currentOption == SleepTimerOption.CUSTOM,
                            accentColor = finalAccentColor,
                            contentColorOnBackground = finalContentColor,
                            onClick = { showCustomTimerDialog = true },
                            modifier = Modifier.weight(1f)
                        )

                        TimerChip(
                            label = "End of song",
                            icon = Icons.Default.MusicNote,
                            isSelected = currentOption == SleepTimerOption.END_OF_SONG,
                            accentColor = finalAccentColor,
                            contentColorOnBackground = finalContentColor,
                            onClick = { onSelectOption(SleepTimerOption.END_OF_SONG, null) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TimerChip(
                            label = "Fade 5% / 2m",
                            icon = Icons.Default.TrendingDown,
                            isSelected = currentOption == SleepTimerOption.FADE_OUT_GENTLE,
                            accentColor = finalAccentColor,
                            contentColorOnBackground = finalContentColor,
                            onClick = { onSelectOption(SleepTimerOption.FADE_OUT_GENTLE, null) },
                            modifier = Modifier.weight(1f)
                        )

                        TimerChip(
                            label = "Fade 5% / 1m",
                            icon = Icons.Default.TrendingDown,
                            isSelected = currentOption == SleepTimerOption.FADE_OUT_FAST,
                            accentColor = finalAccentColor,
                            contentColorOnBackground = finalContentColor,
                            onClick = { onSelectOption(SleepTimerOption.FADE_OUT_FAST, null) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TimerChip(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColorOnBackground: Color = MaterialTheme.colorScheme.onSurface
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(150),
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else contentColorOnBackground.copy(alpha = 0.08f),
        animationSpec = tween(200),
        label = "bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else contentColorOnBackground,
        animationSpec = tween(200),
        label = "content"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .then(
                if (!isSelected) Modifier.border(
                    width = 1.dp,
                    color = contentColorOnBackground.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )

            if (isSelected && icon == null) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CustomTimerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    accentColor: Color,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    var text by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = backgroundColor,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Custom Timer",
                    style = MaterialTheme.typography.headlineSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter duration in minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.all { char -> char.isDigit() }) text = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = accentColor,
                        unfocusedIndicatorColor = contentColor.copy(alpha = 0.3f),
                        focusedTextColor = contentColor,
                        unfocusedTextColor = contentColor
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Cancel", color = accentColor)
                    }

                    Button(
                        onClick = {
                            val minutes = text.toIntOrNull()
                            if (minutes != null && minutes > 0) {
                                onConfirm(minutes)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
