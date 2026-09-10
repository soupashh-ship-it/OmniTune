/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.omnitune.app.models.Song
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.component.LoadingIndicator
import com.omnitune.app.ui.component.glass.ArtworkBlurBackdrop
import com.omnitune.app.ui.component.glass.LocalGlassArtwork
import com.omnitune.app.ui.theme.SquircleShape

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RelatedSheet(
    isVisible: Boolean,
    relatedSongs: List<Song>,
    isLoading: Boolean,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    selectedIndices: Set<Int>,
    onToggleSelection: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onAddSelectedToQueue: () -> Unit,
    onAddSelectedToPlaylist: () -> Unit,
    onSongClick: (Song) -> Unit,
    onMoreClick: (Song) -> Unit,
    onClose: () -> Unit,
    dominantColors: DominantColors,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    if (!isVisible) return

    val isSelectionMode = selectedIndices.isNotEmpty()
    val haptic = LocalHapticFeedback.current

    val backgroundColor = if (isDarkTheme) Color.Black else MaterialTheme.colorScheme.surface
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryContentColor = contentColor.copy(alpha = 0.6f)
    val glassArtwork = LocalGlassArtwork.current
    val glassArtworkUrl = glassArtwork?.artworkUrl

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (glassArtwork != null && !glassArtworkUrl.isNullOrBlank()) {
            ArtworkBlurBackdrop(
                artworkUrl = glassArtworkUrl,
                isDarkTheme = isDarkTheme,
                dominantColors = dominantColors,
                modifier = Modifier.fillMaxSize(),
                scrimAlpha = if (isDarkTheme) 0.72f else 0.60f
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dominantColors.primary.copy(alpha = if (isDarkTheme) 0.15f else 0.1f),
                                backgroundColor
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (isSelectionMode) onClearSelection() else onClose()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isSelectionMode) "Clear selection" else "Close",
                                tint = contentColor
                            )
                        }

                        Column {
                            Text(
                                text = if (isSelectionMode) "${selectedIndices.size} selected" else "Discover Similar",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = contentColor
                            )
                            if (!isSelectionMode) {
                                Text(
                                    text = "Based on what you're listening to",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryContentColor
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isSelectionMode) {
                            IconButton(onClick = onSelectAll) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Select all",
                                    tint = dominantColors.accent
                                )
                            }
                            IconButton(onClick = onAddSelectedToQueue) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = "Add selected to queue",
                                    tint = dominantColors.accent
                                )
                            }
                            IconButton(onClick = onAddSelectedToPlaylist) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = "Add selected to playlist",
                                    tint = dominantColors.accent
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(color = dominantColors.accent)
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 32.dp),
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = secondaryContentColor
                        )
                        FilledTonalButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            } else if (relatedSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No related tracks available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = secondaryContentColor
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(
                        items = relatedSongs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        val isSelected = selectedIndices.contains(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            onToggleSelection(index)
                                        } else {
                                            onSongClick(song)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleSelection(index)
                                    }
                                )
                                .background(
                                    if (isSelected) dominantColors.accent.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelectionMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleSelection(index) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = dominantColors.accent,
                                        uncheckedColor = secondaryContentColor
                                    ),
                                    modifier = Modifier.scale(0.85f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = song.title,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(SquircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = contentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = secondaryContentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!isSelectionMode) {
                                IconButton(
                                    onClick = { onMoreClick(song) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = secondaryContentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
