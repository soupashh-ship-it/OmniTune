package com.omnitune.shared.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnitune.shared.domain.models.Lyrics
import com.omnitune.shared.domain.models.SongItem
import com.omnitune.shared.playback.PlaybackController
import com.omnitune.shared.playback.RepeatMode
import com.omnitune.shared.ui.components.BounceButton
import com.omnitune.shared.ui.components.MeshGradientBackground
import com.omnitune.shared.ui.components.OmniArtwork
import com.omnitune.shared.ui.components.SquircleShape
import com.omnitune.shared.ui.theme.LocalDominantColors

enum class PlayerSheetView {
    MAIN,
    QUEUE,
    LYRICS
}

/**
 * Expandable full-screen player sheet with gesture drag to collapse,
 * large squircle artwork, seek bar, playback controls, and queue/lyrics tabs.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandablePlayerSheet(
    song: SongItem,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    shuffleMode: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onCollapse: () -> Unit,
    queue: List<SongItem> = emptyList(),
    currentIndex: Int = -1,
    onTrackSelect: (Int) -> Unit = {},
    onRemoveFromQueue: (Int) -> Unit = {},
    onReorderQueue: (Int, Int) -> Unit = { _, _ -> },
    onClearQueue: () -> Unit = {},
    lyrics: Lyrics? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    var activeView by remember { mutableStateOf(PlayerSheetView.MAIN) }
    var isFavorite by remember { mutableStateOf(false) }

    val dominantColors = LocalDominantColors.current

    Surface(
        modifier = modifier
            .fillMaxSize()
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    if (delta > 35f && activeView == PlayerSheetView.MAIN) {
                        onCollapse()
                    }
                }
            ),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MeshGradientBackground(
                dominantColors = dominantColors,
                backgroundColor = MaterialTheme.colorScheme.background
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top App Bar / Sheet Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onCollapse) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse Player",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = if (activeView == PlayerSheetView.QUEUE) "Playing Queue"
                        else if (activeView == PlayerSheetView.LYRICS) "Lyrics"
                        else song.album?.title ?: "Playing from YouTube Music",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )

                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Sub-View Switcher (Main Player, Queue, Lyrics)
                when (activeView) {
                    PlayerSheetView.QUEUE -> {
                        ModernQueueView(
                            queue = queue,
                            currentIndex = currentIndex,
                            isPlaying = isPlaying,
                            onTrackSelect = onTrackSelect,
                            onRemoveItem = onRemoveFromQueue,
                            onReorder = onReorderQueue,
                            onClearQueue = onClearQueue,
                            modifier = Modifier.weight(1f),
                            accentColor = accentColor
                        )
                    }
                    PlayerSheetView.LYRICS -> {
                        LyricsScreen(
                            lyrics = lyrics,
                            currentPositionMs = currentPositionMs,
                            onSeekTo = onSeekTo,
                            modifier = Modifier.weight(1f),
                            accentColor = accentColor
                        )
                    }
                    PlayerSheetView.MAIN -> {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Large 280dp Squircle Artwork
                        OmniArtwork(
                            url = song.thumbnailUrl,
                            size = 280.dp,
                            shape = SquircleShape(cornerRadius = 24.dp, cornerSmoothing = 0.6f),
                            elevation = 16.dp,
                            contentDescription = "Full artwork"
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Song Details Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Waveform Seeker
                        WaveformSeeker(
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            onSeek = onSeekTo,
                            activeColor = accentColor,
                            style = WaveformStyle.BARS
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Playback Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = onToggleShuffle) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (shuffleMode) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            BounceButton(
                                onClick = onSkipPrevious,
                                size = 48.dp,
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous track",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Large Play/Pause Primary Action Button
                            BounceButton(
                                onClick = onPlayPause,
                                size = 68.dp,
                                shape = CircleShape
                            ) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(accentColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = MaterialTheme.colorScheme.background,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            BounceButton(
                                onClick = onSkipNext,
                                size = 48.dp,
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next track",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(onClick = onToggleRepeat) {
                                Icon(
                                    imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    contentDescription = "Repeat",
                                    tint = if (repeatMode != RepeatMode.OFF) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // Bottom Action Bar: Toggle Queue, Toggle Lyrics, Share
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BounceButton(
                        onClick = {
                            activeView = if (activeView == PlayerSheetView.LYRICS) PlayerSheetView.MAIN else PlayerSheetView.LYRICS
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lyrics,
                                contentDescription = "Lyrics",
                                tint = if (activeView == PlayerSheetView.LYRICS) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lyrics",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (activeView == PlayerSheetView.LYRICS) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    BounceButton(
                        onClick = {
                            activeView = if (activeView == PlayerSheetView.QUEUE) PlayerSheetView.MAIN else PlayerSheetView.QUEUE
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Queue",
                                tint = if (activeView == PlayerSheetView.QUEUE) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Queue",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (activeView == PlayerSheetView.QUEUE) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
