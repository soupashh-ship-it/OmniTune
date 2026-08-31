/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.*
import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitune.app.models.MusicSource
import com.omnitune.app.models.SleepTimerOption
import com.omnitune.app.models.Song
import com.omnitune.app.ui.component.BetaBadge
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.screens.player.formatDuration
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.CircularProgressIndicator

val LocalCurrentDownloadProgress = androidx.compose.runtime.compositionLocalOf<Float?> { null }

@Composable
fun SongInfoSection(
    song: Song?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    isDisliked: Boolean = false,
    onDislikeClick: () -> Unit = {},
    onMoreClick: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    dominantColors: DominantColors,
    isLoading: Boolean = false,
    compact: Boolean = false,
    sleepTimerRemainingMs: Long? = null,
    sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    showMoreButton: Boolean = true,
    isClassic: Boolean = false,
    isAIEnabled: Boolean = false,
    aiStatus: String? = null,
    showInlineLikeCapsule: Boolean = true,
    showTitleArrow: Boolean = false,
    onTitleArrowClick: () -> Unit = {},
    activeAudioSource: MusicSource? = null,
    isSwitchingSource: Boolean = false,
    onSwitchAudioSource: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = song?.id,
                        transitionSpec = {
                            (slideInVertically(
                                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
                            ) { it / 3 } + fadeIn()) togetherWith
                            (slideOutVertically(
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                            ) { -it / 3 } + fadeOut())
                        },
                        label = "song_title_animation"
                    ) { _ ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (showTitleArrow) Modifier.clickable(onClick = onTitleArrowClick) else Modifier)
                        ) {
                            Text(
                                text = song?.title ?: "No song playing",
                                style = (if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall).copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (compact) 20.sp else 24.sp
                                ),
                                color = dominantColors.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = androidx.compose.foundation.MarqueeAnimationMode.Immediately,
                                        initialDelayMillis = 2000
                                    )
                            )
                            if (showTitleArrow) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Next",
                                    tint = dominantColors.onBackground.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    AnimatedContent(
                        targetState = song?.id,
                        transitionSpec = {
                            (slideInVertically(
                                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
                            ) { it / 4 } + fadeIn()) togetherWith
                            (slideOutVertically(
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                            ) { -it / 4 } + fadeOut())
                        },
                        label = "song_artist_animation"
                    ) { _ ->
                        Text(
                            text = song?.artist ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = if (compact) 14.sp else 16.sp
                            ),
                            color = dominantColors.onBackground.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clickable(enabled = !song?.artist.isNullOrEmpty()) {
                                    song?.artist?.let(onArtistClick)
                                }
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = androidx.compose.foundation.MarqueeAnimationMode.Immediately,
                                    initialDelayMillis = 2500
                                )
                        )
                    }
                }

                if (showInlineLikeCapsule && !isClassic) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(dominantColors.onBackground.copy(alpha = 0.08f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onFavoriteClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = if (isFavorite) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(dominantColors.onBackground.copy(alpha = 0.15f))
                        )
                        IconButton(
                            onClick = onDislikeClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "Dislike",
                                tint = if (isDisliked) MaterialTheme.colorScheme.error else dominantColors.onBackground.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (sleepTimerOption != SleepTimerOption.OFF) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = dominantColors.accent
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (sleepTimerOption == SleepTimerOption.END_OF_SONG) {
                            "Sleep at end of song"
                        } else {
                            sleepTimerRemainingMs?.let { "Sleep in " + formatDuration(it) } ?: "Sleep timer"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = dominantColors.accent
                    )
                }
            }
        }

        if (showMoreButton) {
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = dominantColors.onBackground.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun TimeLabelsWithQuality(
    currentPositionProvider: () -> Long,
    durationProvider: () -> Long,
    dominantColors: DominantColors,
    horizontalPadding: androidx.compose.ui.unit.Dp = 8.dp
) {
    val posText = remember(currentPositionProvider) {
        derivedStateOf { formatDuration(currentPositionProvider()) }
    }
    val remainingText = remember(durationProvider, currentPositionProvider) {
        derivedStateOf { "-${formatDuration((durationProvider() - currentPositionProvider()).coerceAtLeast(0L))}" }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = posText.value,
            style = MaterialTheme.typography.labelMedium,
            color = dominantColors.onBackground.copy(alpha = 0.7f)
        )
        Text(
            text = remainingText.value,
            style = MaterialTheme.typography.labelMedium,
            color = dominantColors.onBackground.copy(alpha = 0.7f)
        )
    }
}
