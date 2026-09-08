/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 *
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.omnitune.app.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.omnitune.app.ui.component.BounceButton
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.utils.formatDurationMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FullScreenVideoPlayer(
    player: Player,
    playerState: PlayerState,
    actions: PlayerScreenActions,
    dominantColors: DominantColors,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var areControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isVideoDownloading by remember { mutableStateOf(false) }
    var showForwardIndicator by remember { mutableStateOf(false) }
    var showRewindIndicator by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var brightness by remember { mutableFloatStateOf(-1.0f) }
    var volumeLevel by remember { mutableFloatStateOf(0.7f) }
    var gestureStatusText by remember { mutableStateOf("") }
    var showGestureStatus by remember { mutableStateOf(false) }
    var gestureIcon by remember { mutableStateOf(Icons.Filled.Settings) }

    LaunchedEffect(areControlsVisible, playerState.isPlaying, isLocked) {
        if (areControlsVisible && playerState.isPlaying && !isLocked) {
            delay(4000)
            areControlsVisible = false
        }
    }

    LaunchedEffect(showGestureStatus) {
        if (showGestureStatus) {
            delay(1200)
            showGestureStatus = false
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, it.decorView) }

        insetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            val layoutParams = activity?.window?.attributes
            layoutParams?.screenBrightness = -1.0f
            activity?.window?.attributes = layoutParams
        }
    }

    BackHandler {
        if (isLocked) {
            isLocked = false
            areControlsVisible = true
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked, playerState.currentPosition, playerState.duration) {
                if (isLocked) {
                    detectTapGestures(onTap = { areControlsVisible = !areControlsVisible })
                } else {
                    detectTapGestures(
                        onTap = { areControlsVisible = !areControlsVisible },
                        onDoubleTap = { offset ->
                            val isForward = offset.x > size.width / 2
                            val targetPosition = if (isForward) {
                                (playerState.currentPosition + 10_000L).coerceAtMost(playerState.duration)
                            } else {
                                (playerState.currentPosition - 10_000L).coerceAtLeast(0L)
                            }
                            actions.onSeekTo(targetPosition)
                            gestureStatusText = if (isForward) "+10s" else "-10s"
                            gestureIcon = if (isForward) Icons.Filled.Forward10 else Icons.Filled.Replay10
                            scope.launch {
                                if (isForward) {
                                    showForwardIndicator = true
                                    delay(600)
                                    showForwardIndicator = false
                                } else {
                                    showRewindIndicator = true
                                    delay(600)
                                    showRewindIndicator = false
                                }
                            }
                            showGestureStatus = true
                        }
                    )
                }
            }
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectVerticalDragGestures { change, dragAmount ->
                        val isVolume = change.position.x > size.width / 2
                        if (isVolume) {
                            volumeLevel = (volumeLevel - dragAmount / size.height).coerceIn(0f, 1f)
                            gestureStatusText = "${(volumeLevel * 100).toInt()}%"
                            gestureIcon = if (volumeLevel > 0.6f) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown

                            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                            audioManager?.let {
                                val max = it.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                                val newVolume = (volumeLevel * max).toInt()
                                it.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0)
                            }
                        } else {
                            val activity = context as? Activity
                            if (brightness < 0f) {
                                val currentBrightness = activity?.window?.attributes?.screenBrightness ?: 0.5f
                                brightness = if (currentBrightness < 0f) 0.5f else currentBrightness
                            }
                            brightness = (brightness - dragAmount / size.height).coerceIn(0f, 1f)
                            gestureStatusText = "${(brightness * 100).toInt()}%"
                            gestureIcon = if (brightness > 0.5f) Icons.Filled.BrightnessHigh else Icons.Filled.BrightnessLow

                            val layoutParams = activity?.window?.attributes
                            layoutParams?.screenBrightness = brightness
                            activity?.window?.attributes = layoutParams
                        }
                        showGestureStatus = true
                    }
                }
            }
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom > 1.1f && resizeMode != AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            gestureStatusText = "Fill"
                            gestureIcon = Icons.Filled.AspectRatio
                            showGestureStatus = true
                        } else if (zoom < 0.9f && resizeMode != AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            gestureStatusText = "Fit"
                            gestureIcon = Icons.Filled.AspectRatio
                            showGestureStatus = true
                        }
                    }
                }
            }
    ) {
        AndroidView<PlayerView>(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                if (view.player !== player) {
                    view.player = player
                }
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                FullScreenSeekIndicator(visible = showRewindIndicator, isForward = false)
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                FullScreenSeekIndicator(visible = showForwardIndicator, isForward = true)
            }
        }

        AnimatedVisibility(
            visible = showGestureStatus,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "gesture")
                    val pulse by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
                        label = "pulse"
                    )

                    Icon(
                        imageVector = gestureIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(gestureStatusText, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }
        }

        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isLocked) 0.15f else 0.5f))
                    .systemBarsPadding()
            ) {
                if (isLocked) {
                    BounceButton(
                        onClick = { isLocked = false; areControlsVisible = true },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(24.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.LockOpen, "Unlock", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BounceButton(onClick = onDismiss) {
                            Icon(Icons.Filled.KeyboardArrowDown, "Minimize", tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(
                                text = playerState.currentSong?.title.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = playerState.currentSong?.artist.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BounceButton(
                                onClick = {
                                    Toast.makeText(context, "Video quality follows the current stream", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = "${playerState.videoQuality.maxResolution}p",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            BounceButton(
                                onClick = {
                                    val activity = context as? Activity
                                    activity?.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.ScreenRotation, "Rotate", tint = Color.White, modifier = Modifier.size(22.dp).padding(8.dp))
                            }

                            BounceButton(
                                onClick = {
                                    isVideoDownloading = true
                                    actions.onDownload()
                                    scope.launch {
                                        delay(600)
                                        isVideoDownloading = false
                                    }
                                },
                                clickEnabled = !isVideoDownloading
                            ) {
                                if (isVideoDownloading) {
                                    LoadingIndicator(color = dominantColors.primary, modifier = Modifier.size(18.dp))
                                } else {
                                    Icon(Icons.Filled.SaveAlt, "Download", tint = Color.White, modifier = Modifier.size(24.dp).padding(8.dp))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 80.dp else 48.dp)
                    ) {
                        BounceButton(onClick = { actions.onSeekTo((playerState.currentPosition - 10_000L).coerceAtLeast(0L)) }) {
                            Icon(Icons.Filled.Replay10, "-10s", tint = Color.White, modifier = Modifier.size(48.dp))
                        }

                        BounceButton(onClick = actions.onPlayPause) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(96.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                if (playerState.isLoading) {
                                    LoadingIndicator(color = dominantColors.primary, modifier = Modifier.size(56.dp))
                                } else {
                                    Icon(
                                        if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }
                        }

                        BounceButton(onClick = { actions.onSeekTo((playerState.currentPosition + 10_000L).coerceAtMost(playerState.duration)) }) {
                            Icon(Icons.Filled.Forward10, "+10s", tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (isLandscape) 12.dp else 32.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                formatDurationMs(playerState.currentPosition),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BounceButton(
                                    onClick = {
                                        resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        } else {
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.AspectRatio,
                                        "Resize",
                                        tint = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) dominantColors.primary else Color.White,
                                        modifier = Modifier.size(24.dp).padding(8.dp)
                                    )
                                }
                                BounceButton(onClick = { isLocked = true }) {
                                    Icon(Icons.Filled.Lock, "Lock", tint = Color.White, modifier = Modifier.size(24.dp).padding(8.dp))
                                }
                            }

                            Text(
                                formatDurationMs(playerState.duration),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Slider(
                            value = if (playerState.duration > 0) playerState.currentPosition.toFloat() else 0f,
                            onValueChange = { actions.onSeekTo(it.toLong()) },
                            valueRange = 0f..playerState.duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = dominantColors.primary,
                                activeTrackColor = dominantColors.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().height(32.dp)
                        )

                        LinearProgressIndicator(
                            progress = { playerState.bufferedPercentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = Color.White.copy(alpha = 0.35f),
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenSeekIndicator(
    visible: Boolean,
    isForward: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.2f),
            shape = CircleShape,
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isForward) Icons.Default.Forward10 else Icons.Default.Replay10,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
