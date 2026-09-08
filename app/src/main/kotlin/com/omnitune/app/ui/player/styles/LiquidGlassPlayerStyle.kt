/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player.styles

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import com.omnitune.app.models.ArtworkShape
import com.omnitune.app.models.ArtworkSize
import com.omnitune.app.models.SeekbarStyle
import com.omnitune.app.models.SleepTimerOption
import com.omnitune.app.models.Song
import com.omnitune.app.models.SponsorSegment
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.player.PlayerScreenActions
import com.omnitune.app.ui.player.PlayerState

/**
 * iOS-style Liquid Glass player.
 * Layered structure:
 *   1. Heavily blurred album artwork filling the entire screen (the "glass backdrop")
 *   2. Scrim gradient for text legibility
 *   3. Subtle dominant-color wash
 *   4. Full YTMusicPlayerStyle on top with transparent background.
 */
@Composable
fun LiquidGlassPlayerStyle(
    song: Song?,
    playerState: PlayerState,
    playbackInfo: PlayerState,
    dominantColors: DominantColors,
    currentArtworkShape: ArtworkShape,
    currentArtworkSize: ArtworkSize,
    currentSeekbarStyle: SeekbarStyle,
    sponsorSegments: List<SponsorSegment>,
    audioArEnabled: Boolean,
    isRotatingEnabled: Boolean,
    player: Player?,
    isFullScreen: Boolean,
    isCompactHeight: Boolean,
    useWideLayout: Boolean,
    actions: PlayerScreenActions,
    onShowActions: () -> Unit,
    onShowQueue: () -> Unit,
    onShowLyrics: () -> Unit,
    onShowRelated: () -> Unit,
    onShowDevices: () -> Unit,
    onShowSleepTimer: () -> Unit,
    onShowPlaybackSpeed: () -> Unit,
    onShowEqualizer: () -> Unit,
    handleDoubleTapSeek: (Boolean) -> Unit,
    onShapeChange: (ArtworkShape) -> Unit,
    onSeekbarStyleChange: (SeekbarStyle) -> Unit,
    onRecenterAr: () -> Unit,
    onSetFullScreen: (Boolean) -> Unit,
    isSwitchingMode: Boolean,
    sleepTimerOption: SleepTimerOption,
    sleepTimerRemainingMs: Long?,
    progressProvider: () -> Float,
    positionProvider: () -> Long,
    durationProvider: () -> Long,
    isAIEnabled: Boolean = false,
    aiStatus: String? = null,
    windowSizeClass: WindowSizeClass? = null,
    blurRadius: Float = 60f,
    intensity: Float = 1f,
    backgroundArtworkUrl: String? = null
) {
    val isDarkTheme = isSystemInDarkTheme()
    val thumbnailUrl = backgroundArtworkUrl ?: song?.thumbnailUrl
    val scrimAlpha = if (isDarkTheme) 0.55f else 0.40f
    val i = intensity.coerceIn(0.3f, 1.5f)

    Box(modifier = Modifier.fillMaxSize()) {
        if (!thumbnailUrl.isNullOrBlank() && !playerState.isVideoMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.graphicsLayer {
                                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                    blurRadius * 1.8f,
                                    blurRadius * 1.8f,
                                    android.graphics.Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        } else {
                            Modifier.blur((blurRadius * 0.9f).dp)
                        }
                    )
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDarkTheme) {
                                listOf(
                                    Color.Black.copy(alpha = scrimAlpha * i * 0.7f),
                                    Color.Black.copy(alpha = scrimAlpha * i),
                                    Color.Black.copy(alpha = scrimAlpha * i * 1.2f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = scrimAlpha * i * 0.5f),
                                    Color.White.copy(alpha = scrimAlpha * i * 0.8f),
                                    Color.White.copy(alpha = scrimAlpha * i * 1.1f)
                                )
                            }
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                (if (isDarkTheme) dominantColors.primary else dominantColors.primary.copy(alpha = 0.5f)).copy(alpha = 0.18f * i),
                                Color.Transparent
                            ),
                            radius = 1200f
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDarkTheme) Color(0xFF0B0B0F) else Color(0xFFF2F2F6)
                    )
            )
        }

        YTMusicPlayerStyle(
            song = song,
            playerState = playerState,
            playbackInfo = playbackInfo,
            dominantColors = dominantColors,
            currentArtworkShape = currentArtworkShape,
            currentArtworkSize = currentArtworkSize,
            currentSeekbarStyle = currentSeekbarStyle,
            sponsorSegments = sponsorSegments,
            audioArEnabled = audioArEnabled,
            isRotatingEnabled = isRotatingEnabled,
            player = player,
            isFullScreen = isFullScreen,
            isCompactHeight = isCompactHeight,
            useWideLayout = useWideLayout,
            actions = actions,
            onShowActions = onShowActions,
            onShowQueue = onShowQueue,
            onShowLyrics = onShowLyrics,
            onShowRelated = onShowRelated,
            onShowDevices = onShowDevices,
            onShowSleepTimer = onShowSleepTimer,
            onShowPlaybackSpeed = onShowPlaybackSpeed,
            onShowEqualizer = onShowEqualizer,
            handleDoubleTapSeek = handleDoubleTapSeek,
            onShapeChange = onShapeChange,
            onSeekbarStyleChange = onSeekbarStyleChange,
            onRecenterAr = onRecenterAr,
            onSetFullScreen = onSetFullScreen,
            isSwitchingMode = isSwitchingMode,
            sleepTimerOption = sleepTimerOption,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            progressProvider = progressProvider,
            positionProvider = positionProvider,
            durationProvider = durationProvider,
            isAIEnabled = isAIEnabled,
            aiStatus = aiStatus,
            windowSizeClass = windowSizeClass
        )
    }
}
