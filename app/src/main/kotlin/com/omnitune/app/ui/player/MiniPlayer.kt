/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 * Licensed Under GPL-3.0
 */

package com.omnitune.app.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_BUFFERING
import coil3.compose.AsyncImage
import com.omnitune.app.ui.component.OmniTuneLoader
import com.omnitune.app.extensions.togglePlayPause
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import kotlin.math.roundToInt

private const val MINI_PLAYER_HEIGHT = 56

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    playerConnection: PlayerConnection? = null,
) {
    val isPlaying by playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val playbackState by playerConnection?.playbackState?.collectAsState() ?: remember { mutableStateOf(Player.STATE_IDLE) }
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
    val canSkipNext by playerConnection?.canSkipNext?.collectAsState() ?: remember { mutableStateOf(false) }
    val isLoading = playbackState == STATE_BUFFERING
    val hasPlayer = playerConnection != null
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    // Read position/duration from player, updating every 200ms
    var position by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    val currentMediaId = mediaMetadata?.id
    LaunchedEffect(playerConnection?.player, currentMediaId) {
        while (true) {
            val player = playerConnection?.player
            if (player != null) {
                val dur = player.duration
                val pos = player.currentPosition
                // Handle STATE_ENDED: show full bar, then reset on next song
                if (playbackState == Player.STATE_ENDED) {
                    if (dur > 0) {
                        position = dur.toFloat()
                        duration = dur.toFloat()
                    }
                } else {
                    duration = if (dur > 0) dur.toFloat() else 0f
                    position = if (pos in 0..dur) pos.toFloat() else 0f
                }
            } else {
                duration = 0f
                position = 0f
            }
            delay(200)
        }
    }

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    val swipeSensitivity = 0.73f
    val autoSwipeThreshold = (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MINI_PLAYER_HEIGHT.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .background(
                if (pureBlack) Color.Black
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragStartTime = System.currentTimeMillis()
                        totalDragDistance = 0f
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetXAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = animationSpec
                            )
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val pc = playerConnection ?: return@detectHorizontalDragGestures
                        val p = pc.player
                        val adjustedDragAmount =
                            if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                        val canSkipPrev = p.previousMediaItemIndex != -1
                        val canSkipNxt = p.nextMediaItemIndex != -1
                        val allowLeft = adjustedDragAmount < 0 && canSkipNxt
                        val allowRight = adjustedDragAmount > 0 && canSkipPrev
                        if (allowLeft || allowRight) {
                            totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                            coroutineScope.launch {
                                offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                            }
                        }
                    },
                    onDragEnd = {
                        val pc = playerConnection ?: return@detectHorizontalDragGestures
                        val p = pc.player
                        val dragDuration = System.currentTimeMillis() - dragStartTime
                        val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                        val currentOffset = offsetXAnimatable.value
                        val minDistanceThreshold = 50f
                        val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f
                        val shouldChangeSong = (
                            kotlin.math.abs(currentOffset) > minDistanceThreshold &&
                            velocity > velocityThreshold
                        ) || (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                        if (shouldChangeSong) {
                            val isRightSwipe = currentOffset > 0
                            val canSkipPrev = p.previousMediaItemIndex != -1

                            if (isRightSwipe && canSkipPrev) {
                                p.seekToPreviousMediaItem()
                            } else if (!isRightSwipe && canSkipNext) {
                                pc.seekToNext()
                            }
                        }
                        coroutineScope.launch {
                            offsetXAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = animationSpec
                            )
                        }
                    }
                )
            }
    ) {
        LinearProgressIndicator(
            progress = { if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .padding(end = 12.dp),
        ) {
            Box(Modifier.weight(1f)) {
                mediaMetadata?.let {
                    MiniMediaInfo(
                        mediaMetadata = it,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }

            if (hasPlayer) {
                val pc = playerConnection
                IconButton(
                    onClick = {
                        val player = pc.player
                        if (playbackState == Player.STATE_ENDED) {
                            player.seekTo(0, 0)
                            player.playWhenReady = true
                        } else {
                            player.togglePlayPause()
                        }
                    },
                ) {
                    if (isLoading) {
                        OmniTuneLoader(size = 24.dp)
                    } else {
                        Icon(
                            painter = painterResource(
                                if (playbackState == Player.STATE_ENDED) android.R.drawable.ic_media_play
                                else if (isPlaying) android.R.drawable.ic_media_pause
                                else android.R.drawable.ic_media_play
                            ),
                            contentDescription = when {
                                playbackState == Player.STATE_ENDED -> "Replay"
                                isPlaying -> "Pause"
                                else -> "Play"
                            },
                        )
                    }
                }

                IconButton(
                    enabled = canSkipNext,
                    onClick = { pc.seekToNext() },
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_media_next),
                        contentDescription = "Next",
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniMediaInfo(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        ) {
            AnimatedContent(
                targetState = mediaMetadata.title,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "",
            ) { title ->
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
            }

            AnimatedContent(
                targetState = mediaMetadata.artists.joinToString { it.name },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "",
            ) { artists ->
                Text(
                    text = artists,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
