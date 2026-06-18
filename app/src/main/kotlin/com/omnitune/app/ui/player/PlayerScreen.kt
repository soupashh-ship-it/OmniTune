package com.omnitune.app.ui.player

import androidx.compose.ui.res.painterResource
import com.omnitune.app.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import coil3.compose.AsyncImage
import com.omnitune.app.lyrics.LyricsEntry
import com.omnitune.app.lyrics.LyricsUtils
import com.omnitune.app.playback.PlayerConnection
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    playerConnection: PlayerConnection?,
    onDismiss: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(null) }
    val isPlaying by playerConnection?.isPlaying?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(false) }
    val playbackState by playerConnection?.playbackState?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(Player.STATE_IDLE) }
    val shuffleEnabled by playerConnection?.shuffleModeEnabled?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(false) }
    val repeatMode by playerConnection?.repeatMode?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(REPEAT_MODE_OFF) }

    val thumbnailUrl = mediaMetadata?.thumbnailUrl
    val isSeeking = remember { mutableFloatStateOf(-1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        thumbnailUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(40.dp),
                alpha = 0.3f,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    )
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = mediaMetadata?.id ?: "",
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "album_art",
                modifier = Modifier.weight(0.5f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        if (!thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            LyricsSection(
                playerConnection = playerConnection,
                modifier = Modifier.weight(0.3f),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedContent(
                    targetState = mediaMetadata?.title ?: "No track",
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label = "title",
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                AnimatedContent(
                    targetState = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown artist",
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label = "artist",
                ) { artists ->
                    Text(
                        text = artists,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PlayerProgressBar(
                    playerConnection = playerConnection,
                    isSeeking = isSeeking,
                )

                Spacer(modifier = Modifier.height(16.dp))

                PlayerControls(
                    isPlaying = isPlaying,
                    playbackState = playbackState,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    playerConnection = playerConnection,
                )

                Spacer(modifier = Modifier.height(16.dp))

                PlayerExtrasRow(
                    playerConnection = playerConnection,
                    onOpenQueue = onOpenQueue,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlayerProgressBar(
    playerConnection: PlayerConnection?,
    isSeeking: androidx.compose.runtime.MutableFloatState,
) {
    val player = playerConnection?.player
    val duration = player?.duration ?: 0L
    val currentPosition = player?.currentPosition ?: 0L

    val progress by animateFloatAsState(
        targetValue = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
        label = "progress",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = if (isSeeking.floatValue >= 0f) isSeeking.floatValue else progress,
            onValueChange = { isSeeking.floatValue = it },
            onValueChangeFinished = {
                if (player != null && duration > 0) {
                    player.seekTo((isSeeking.floatValue * duration).toLong())
                }
                isSeeking.floatValue = -1f
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(if (isSeeking.floatValue >= 0f) (isSeeking.floatValue * duration).toLong() else currentPosition),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    playbackState: Int,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    playerConnection: PlayerConnection?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { playerConnection?.player?.shuffleModeEnabled = !shuffleEnabled },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shuffle),
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(
            onClick = { playerConnection?.seekToPrevious() },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_previous),
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp),
            )
        }

        FilledIconButton(
            onClick = {
                val p = playerConnection?.player ?: return@FilledIconButton
                if (playbackState == Player.STATE_ENDED) {
                    p.seekTo(0, 0)
                    p.playWhenReady = true
                } else {
                    if (isPlaying) p.pause() else p.play()
                }
            },
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                painter = if (playbackState == Player.STATE_ENDED) painterResource(R.drawable.ic_play_arrow)
                else if (isPlaying) painterResource(R.drawable.ic_pause) else painterResource(R.drawable.ic_play_arrow),
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(36.dp),
            )
        }

        IconButton(
            onClick = { playerConnection?.seekToNext() },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_next),
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp),
            )
        }

        IconButton(
            onClick = {
                playerConnection?.player?.let { p ->
                    p.repeatMode = when (p.repeatMode) {
                        REPEAT_MODE_OFF -> REPEAT_MODE_ALL
                        REPEAT_MODE_ALL -> REPEAT_MODE_ONE
                        REPEAT_MODE_ONE -> REPEAT_MODE_OFF
                        else -> REPEAT_MODE_OFF
                    }
                }
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_repeat),
                contentDescription = "Repeat",
                tint = if (repeatMode != REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlayerExtrasRow(
    playerConnection: PlayerConnection?,
    onOpenQueue: () -> Unit,
) {
    val currentSongState = playerConnection?.currentSong?.collectAsState(initial = null)
    val songValue = currentSongState?.value
    val liked = songValue?.song?.liked == true

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { playerConnection?.toggleLike() }) {
            Icon(
                painter = if (liked) painterResource(R.drawable.ic_favorite) else painterResource(R.drawable.ic_favorite_border),
                contentDescription = if (liked) "Unlike" else "Like",
                tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = onOpenQueue) {
            Icon(
                painter = painterResource(R.drawable.ic_list),
                contentDescription = "Queue",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val player = playerConnection?.player
        var volume by remember { mutableFloatStateOf(player?.volume ?: 1f) }
        var showVolume by remember { androidx.compose.runtime.mutableStateOf(false) }

        IconButton(onClick = { showVolume = !showVolume }) {
            Text(
                text = if (showVolume) "Done" else "Vol",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(visible = showVolume) {
            Slider(
                value = volume,
                onValueChange = { vol ->
                    volume = vol
                    player?.volume = vol
                },
                modifier = Modifier.width(120.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun LyricsSection(
    playerConnection: PlayerConnection?,
    modifier: Modifier = Modifier,
) {
    val lyricsEntity by playerConnection?.currentLyrics?.collectAsState(initial = null) ?: remember { androidx.compose.runtime.mutableStateOf(null) }
    val player = playerConnection?.player
    val currentMediaId = player?.currentMediaItem?.mediaId

    // Poll position every 200ms so lyrics scroll with playback
    var position by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    LaunchedEffect(playerConnection?.player) {
        while (true) {
            position = player?.currentPosition ?: 0L
            delay(200)
        }
    }

    // Key on both lyricsEntity.id AND currentMediaId to force re-parse on song change
    val parsedLines = remember(lyricsEntity?.id, currentMediaId) {
        lyricsEntity?.lyrics?.let { LyricsUtils.parseLyrics(it) } ?: emptyList()
    }

    if (parsedLines.isEmpty()) return

    val currentLineIndex = remember(position, parsedLines.size) {
        if (parsedLines.isEmpty()) -1 else LyricsUtils.findCurrentLineIndex(parsedLines, position)
    }

    val listState = rememberLazyListState()

    // Auto-scroll to current line
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0) {
            listState.animateScrollToItem(
                index = currentLineIndex,
                scrollOffset = -200
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = true,
        ) {
            itemsIndexed(parsedLines) { index, line ->
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (index == currentLineIndex) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = if (index == currentLineIndex) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
