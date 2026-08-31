/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.omnitune.app.models.MiniPlayerStyle
import com.omnitune.app.models.Song
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.player.miniplayer.LiquidGlassMiniPlayer
import com.omnitune.app.ui.player.miniplayer.PillMiniPlayer
import com.omnitune.app.ui.player.miniplayer.StandardMiniPlayer
import com.omnitune.app.ui.player.miniplayer.YTMusicMiniPlayer
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val MiniPlayerHeight = 64.dp

@Composable
fun ExpandablePlayerSheet(
    currentSong: Song?,
    isPlaying: Boolean,
    isLoading: Boolean = false,
    progressProvider: () -> Float,
    dominantColors: DominantColors,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    bottomPadding: Float,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    userAlpha: Float = 0f,
    swipeDownToDismissEnabled: Boolean = true,
    style: MiniPlayerStyle = MiniPlayerStyle.YT_MUSIC,
    artworkShape: String = "ROUNDED_SQUARE",
    glassBlurAmount: Float = 50f,
    expandedContent: @Composable (onCollapse: () -> Unit) -> Unit
) {
    val song = currentSong ?: return
    val coroutineScope = rememberCoroutineScope()

    val expansion = remember { Animatable(if (isExpanded) 1f else 0f) }

    LaunchedEffect(isExpanded) {
        val target = if (isExpanded) 1f else 0f
        if (expansion.value != target) {
            expansion.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = 350,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    val density = LocalDensity.current
    val view = LocalView.current
    val screenHeightPx = view.height.toFloat()
    val miniPlayerHeightPx = with(density) { MiniPlayerHeight.toPx() }

    val stylePaddingOffset = when (style) {
        MiniPlayerStyle.YT_MUSIC -> with(density) { 14.dp.toPx() }
        MiniPlayerStyle.LIQUID_GLASS -> with(density) { 6.dp.toPx() }
        else -> with(density) { 2.dp.toPx() }
    }
    val adjustedBottomPadding = (bottomPadding - stylePaddingOffset).coerceAtLeast(0f)

    val dragRange = (screenHeightPx - (miniPlayerHeightPx + bottomPadding)).coerceAtLeast(1f)

    BackHandler(enabled = isExpanded) {
        onExpandChange(false)
        coroutineScope.launch {
            expansion.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val h = ((miniPlayerHeightPx + bottomPadding) +
                    dragRange * expansion.value.coerceAtLeast(0f)).roundToInt()
                val placeable = measurable.measure(constraints.copy(minHeight = h, maxHeight = h))
                layout(placeable.width, h) { placeable.place(0, 0) }
            }
    ) {
        val showMiniPlayer by remember { derivedStateOf { expansion.value < 0.4f } }
        val horizontalDrag = remember { Animatable(0f) }
        val skipThresholdPx = with(density) { 96.dp.toPx() }
        if (showMiniPlayer) {
            CollapsedMiniPlayer(
                song = song,
                isPlaying = isPlaying,
                isLoading = isLoading,
                dominantColors = dominantColors,
                progressProvider = progressProvider,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onClose = onClose,
                userAlpha = userAlpha,
                style = style,
                artworkShape = artworkShape,
                glassBlurAmount = glassBlurAmount,
                onTap = {
                    coroutineScope.launch {
                        expansion.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 350,
                                easing = FastOutSlowInEasing
                            )
                        )
                        onExpandChange(true)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MiniPlayerHeight)
                    .align(Alignment.TopCenter)
                    .offset {
                        val e = expansion.value
                        val px = if (e >= 0f) (bottomPadding - adjustedBottomPadding) * (1f - e) else 0f
                        IntOffset(0, px.roundToInt())
                    }
                    .graphicsLayer {
                        translationX = horizontalDrag.value
                        alpha = (1f - expansion.value * 2.5f).coerceIn(0f, 1f)
                        if (expansion.value < 0f && swipeDownToDismissEnabled) {
                            translationY = -expansion.value * dragRange
                            alpha = (1f + expansion.value * 1.5f).coerceIn(0.25f, 1f)
                        }
                    }
                    .zIndex(if (isExpanded) 0f else 1f)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    val total = horizontalDrag.value
                                    when {
                                        total <= -skipThresholdPx -> onNext()
                                        total >= skipThresholdPx -> onPrevious()
                                    }
                                    horizontalDrag.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    horizontalDrag.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    horizontalDrag.snapTo(horizontalDrag.value + dragAmount)
                                }
                            }
                        )
                    }
                    .pointerInput(swipeDownToDismissEnabled) {
                        val dismissThreshold = -0.05f
                        detectVerticalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (expansion.value <= dismissThreshold && swipeDownToDismissEnabled) {
                                        expansion.animateTo(
                                            targetValue = -0.6f,
                                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                        )
                                        onClose()
                                        expansion.snapTo(0f)
                                    } else {
                                        val targetValue = if (expansion.value > 0.4f) 1f else 0f
                                        expansion.animateTo(
                                            targetValue = targetValue,
                                            animationSpec = tween(
                                                durationMillis = 250,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                        onExpandChange(targetValue == 1f)
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    val targetValue = if (expansion.value > 0.4f) 1f else 0f
                                    expansion.animateTo(
                                        targetValue = targetValue,
                                        animationSpec = tween(
                                            durationMillis = 250,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                    onExpandChange(targetValue == 1f)
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val delta = -dragAmount / dragRange
                                coroutineScope.launch {
                                    val minExpansion = if (swipeDownToDismissEnabled) -0.7f else 0f
                                    expansion.snapTo(
                                        (expansion.value + delta).coerceIn(minExpansion, 1f)
                                    )
                                }
                            }
                        )
                    }
            )
        }

        val showFullPlayer by remember { derivedStateOf { expansion.value > 0.3f } }
        if (showFullPlayer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = ((expansion.value - 0.3f) / 0.7f).coerceIn(0f, 1f) }
                    .zIndex(if (isExpanded) 1f else 0f)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                val targetValue = if (expansion.value > 0.8f) 1f else 0f
                                coroutineScope.launch {
                                    expansion.animateTo(
                                        targetValue = targetValue,
                                        animationSpec = tween(
                                            durationMillis = 250,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                    onExpandChange(targetValue == 1f)
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    val targetValue = if (expansion.value > 0.8f) 1f else 0f
                                    expansion.animateTo(
                                        targetValue = targetValue,
                                        animationSpec = tween(
                                            durationMillis = 250,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                    onExpandChange(targetValue == 1f)
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val delta = -dragAmount / dragRange
                                coroutineScope.launch {
                                    expansion.snapTo(
                                        (expansion.value + delta).coerceIn(0f, 1f)
                                    )
                                }
                            }
                        )
                    }
            ) {
                expandedContent {
                    coroutineScope.launch {
                        expansion.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = FastOutSlowInEasing
                            )
                        )
                        onExpandChange(false)
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsedMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean = false,
    dominantColors: DominantColors,
    progressProvider: () -> Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onTap: () -> Unit,
    userAlpha: Float = 0f,
    style: MiniPlayerStyle = MiniPlayerStyle.YT_MUSIC,
    artworkShape: String = "ROUNDED_SQUARE",
    glassBlurAmount: Float = 50f,
    modifier: Modifier = Modifier
) {
    when (style) {
        MiniPlayerStyle.LIQUID_GLASS -> {
            LiquidGlassMiniPlayer(
                song = song,
                isPlaying = isPlaying,
                isLoading = isLoading,
                dominantColors = dominantColors,
                progressProvider = progressProvider,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onClose = onClose,
                onTap = onTap,
                userAlpha = userAlpha,
                artworkShape = artworkShape,
                blurAmount = glassBlurAmount,
                modifier = modifier
            )
        }
        MiniPlayerStyle.FLOATING_PILL -> {
            PillMiniPlayer(
                song = song,
                isPlaying = isPlaying,
                isLoading = isLoading,
                dominantColors = dominantColors,
                progressProvider = progressProvider,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onClose = onClose,
                onTap = onTap,
                userAlpha = userAlpha,
                artworkShape = artworkShape,
                modifier = modifier
            )
        }
        MiniPlayerStyle.YT_MUSIC -> {
            YTMusicMiniPlayer(
                song = song,
                isPlaying = isPlaying,
                isLoading = isLoading,
                dominantColors = dominantColors,
                progressProvider = progressProvider,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onClose = onClose,
                onTap = onTap,
                userAlpha = userAlpha,
                artworkShape = artworkShape,
                modifier = modifier
            )
        }
        else -> {
            StandardMiniPlayer(
                song = song,
                isPlaying = isPlaying,
                isLoading = isLoading,
                dominantColors = dominantColors,
                progressProvider = progressProvider,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onClose = onClose,
                onTap = onTap,
                userAlpha = userAlpha,
                artworkShape = artworkShape,
                modifier = modifier
            )
        }
    }
}
