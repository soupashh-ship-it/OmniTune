/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player.styles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.omnitune.app.models.ArtworkShape
import com.omnitune.app.models.ArtworkSize
import com.omnitune.app.models.SeekbarStyle
import com.omnitune.app.models.SleepTimerOption
import com.omnitune.app.models.Song
import com.omnitune.app.models.SongSource
import com.omnitune.app.models.SponsorSegment
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.player.PlayerScreenActions
import com.omnitune.app.ui.player.PlayerState
import com.omnitune.app.ui.player.WaveformSeeker
import com.omnitune.app.ui.player.components.AlbumArtwork
import com.omnitune.app.ui.player.components.M3ELoadingOverlay
import com.omnitune.app.ui.player.components.PlaybackControls
import com.omnitune.app.ui.player.components.PlayerActionChips
import com.omnitune.app.ui.player.components.PlayerTopBar
import com.omnitune.app.ui.player.components.QueueHandle
import com.omnitune.app.ui.player.components.SongInfoSection
import com.omnitune.app.ui.player.components.TimeLabelsWithQuality

@Composable
fun YTMusicPlayerStyle(
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
    isSwitchingMode: Boolean = false,
    sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    sleepTimerRemainingMs: Long? = null,
    progressProvider: () -> Float = { 0f },
    positionProvider: () -> Long = { 0L },
    durationProvider: () -> Long = { 0L },
    windowSizeClass: WindowSizeClass? = null
) {
    if (useWideLayout) {
        YTMusicLandscapeContent(
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
            actions = actions,
            onShowActions = onShowActions,
            onShowLyrics = onShowLyrics,
            onShowQueue = onShowQueue,
            onShowRelated = onShowRelated,
            onShowDevices = onShowDevices,
            onShowSleepTimer = onShowSleepTimer,
            onShowPlaybackSpeed = onShowPlaybackSpeed,
            onShowEqualizer = onShowEqualizer,
            isVideoMode = playerState.isVideoMode,
            onToggleVideoMode = actions.onToggleVideoMode,
            handleDoubleTapSeek = handleDoubleTapSeek,
            onShapeChange = onShapeChange,
            onSeekbarStyleChange = onSeekbarStyleChange,
            onRecenterAr = onRecenterAr,
            player = player,
            isFullScreen = isFullScreen,
            onSetFullScreen = onSetFullScreen,
            isSwitchingMode = isSwitchingMode,
            sleepTimerOption = sleepTimerOption,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            progressProvider = progressProvider,
            positionProvider = positionProvider,
            durationProvider = durationProvider,
            windowSizeClass = windowSizeClass
        )
    } else {
        YTMusicPortraitContent(
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
            windowSizeClass = windowSizeClass
        )
    }
}

@Composable
private fun YTMusicPortraitContent(
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
    isSwitchingMode: Boolean = false,
    sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    sleepTimerRemainingMs: Long? = null,
    progressProvider: () -> Float = { 0f },
    positionProvider: () -> Long = { 0L },
    durationProvider: () -> Long = { 0L },
    windowSizeClass: WindowSizeClass? = null
) {
    val combinedLoading = playerState.isLoading || isSwitchingMode
    val controlsAlpha by animateFloatAsState(
        targetValue = if (combinedLoading) 0.45f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label = "controlsDimOnLoad"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val heightSizeClass = windowSizeClass?.windowHeightSizeClass ?: WindowHeightSizeClass.MEDIUM
        val isVeryShort = heightSizeClass == WindowHeightSizeClass.COMPACT || screenHeight < 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = if (isVeryShort) 16.dp else 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            PlayerTopBar(
                onBack = actions.onBack,
                dominantColors = dominantColors,
                isVideoMode = playerState.isVideoMode,
                isYouTubeSong = song?.source == SongSource.YOUTUBE,
                onVideoToggle = actions.onToggleVideoMode,
                onMoreClick = onShowActions,
                onCastClick = onShowDevices,
                audioArEnabled = audioArEnabled,
                onRecenter = onRecenterAr
            )

            Spacer(modifier = Modifier.weight(if (isVeryShort) 0.2f else 0.18f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isVeryShort) 1.5f else 8f, fill = false)
                    .then(if (!isVeryShort) Modifier.aspectRatio(1f) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = playerState.isVideoMode && player != null && !isFullScreen,
                    transitionSpec = { fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500)) },
                    label = "video_artwork_transition"
                ) { isVideo ->
                    if (isVideo) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxHeight(if (isVeryShort) 0.9f else 1f)
                                .aspectRatio(1f)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize(currentArtworkSize.fraction / ArtworkSize.MAX_FRACTION)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                                    .clickable { onSetFullScreen(true) },
                                tonalElevation = 16.dp,
                                shadowElevation = 16.dp
                            ) {
                                AndroidView(
                                    factory = { context ->
                                        PlayerView(context).apply {
                                            this.player = player
                                            useController = false
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            setBackgroundColor(android.graphics.Color.BLACK)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopEnd) {
                                    Icon(
                                        imageVector = Icons.Filled.Fullscreen,
                                        contentDescription = "Full Screen",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        AlbumArtwork(
                            imageUrl = song?.thumbnailUrl,
                            title = song?.title,
                            dominantColors = dominantColors,
                            isLoading = playerState.isLoading,
                            isPlaying = playerState.isPlaying,
                            isRotatingEnabled = isRotatingEnabled,
                            onSwipeLeft = actions.onNext,
                            onSwipeRight = actions.onPrevious,
                            initialShape = currentArtworkShape,
                            artworkSize = currentArtworkSize,
                            onShapeChange = onShapeChange,
                            onDoubleTapLeft = { handleDoubleTapSeek(false) },
                            onDoubleTapRight = { handleDoubleTapSeek(true) },
                            songId = song?.id,
                            modifier = Modifier.fillMaxHeight(if (isVeryShort) 0.9f else 1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(if (isVeryShort) 0.15f else 0.3f))

            SongInfoSection(
                song = song,
                isFavorite = playbackInfo.isLiked,
                onFavoriteClick = actions.onToggleLike,
                isDisliked = playbackInfo.isDisliked,
                onDislikeClick = actions.onToggleDislike,
                onMoreClick = onShowActions,
                onArtistClick = actions.onArtistClick,
                onAlbumClick = { actions.onAlbumClickWithSong(it, song?.id) },
                dominantColors = dominantColors,
                isLoading = playerState.isLoading,
                compact = isVeryShort,
                sleepTimerOption = sleepTimerOption,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                showMoreButton = false,
                isClassic = false,
                activeAudioSource = playerState.activeAudioSource,
                isSwitchingSource = playerState.isSwitchingSource,
                onSwitchAudioSource = actions.onSwitchAudioSource
            )


            Spacer(modifier = Modifier.height(if (isVeryShort) 2.dp else 4.dp))

            PlayerActionChips(
                isFavorite = playbackInfo.isLiked,
                isDisliked = playbackInfo.isDisliked,
                onToggleLike = actions.onToggleLike,
                onToggleDislike = actions.onToggleDislike,
                onLyricsClick = onShowLyrics,
                onRelatedClick = onShowRelated,
                onDownloadClick = actions.onDownload,
                downloadState = playbackInfo.downloadState,
                dominantColors = dominantColors,
                onSleepTimerClick = onShowSleepTimer,
                onSpeedClick = onShowPlaybackSpeed,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                playbackSpeed = playbackInfo.playbackSpeed,
                activeAudioSource = playerState.activeAudioSource,
                isSwitchingSource = playerState.isSwitchingSource,
                onSwitchAudioSource = actions.onSwitchAudioSource,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(if (isVeryShort) 0.1f else 0.2f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = controlsAlpha }
            ) {
                WaveformSeeker(
                    progressProvider = progressProvider,
                    isPlaying = playerState.isPlaying,
                    onSeek = { fraction ->
                        val target = (fraction * durationProvider()).toLong()
                        actions.onSeekTo(target)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    activeColor = dominantColors.accent,
                    inactiveColor = dominantColors.onBackground.copy(alpha = 0.2f),
                    initialStyle = currentSeekbarStyle,
                    onStyleChange = onSeekbarStyleChange,
                    duration = durationProvider(),
                    sponsorSegments = sponsorSegments
                )
            }

            TimeLabelsWithQuality(
                currentPositionProvider = positionProvider,
                durationProvider = durationProvider,
                dominantColors = dominantColors
            )

            Spacer(modifier = Modifier.weight(if (isVeryShort) 0.1f else 0.25f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = controlsAlpha }
            ) {
                PlaybackControls(
                    isPlaying = playerState.isPlaying,
                    shuffleEnabled = playerState.shuffleEnabled,
                    repeatMode = playerState.repeatMode.ordinal,
                    onPlayPause = actions.onPlayPause,
                    onNext = actions.onNext,
                    onPrevious = actions.onPrevious,
                    onShuffleToggle = actions.onShuffleToggle,
                    onRepeatToggle = actions.onRepeatToggle,
                    dominantColors = dominantColors,
                    compact = isVeryShort
                )
            }

            Spacer(modifier = Modifier.weight(if (isVeryShort) 0.15f else 0.35f))

            QueueHandle(
                onClick = onShowQueue,
                dominantColors = dominantColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(if (isVeryShort) 2.dp else 4.dp))
        }

        M3ELoadingOverlay(
            isLoading = combinedLoading,
            dominantColors = dominantColors,
            modifier = Modifier.fillMaxSize()
        )

    }
}

@Composable
private fun YTMusicLandscapeContent(
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
    actions: PlayerScreenActions,
    onShowActions: () -> Unit,
    onShowLyrics: () -> Unit,
    onShowQueue: () -> Unit,
    onShowRelated: () -> Unit,
    onShowDevices: () -> Unit,
    onShowSleepTimer: () -> Unit,
    onShowPlaybackSpeed: () -> Unit,
    onShowEqualizer: () -> Unit,
    isVideoMode: Boolean,
    onToggleVideoMode: () -> Unit,
    handleDoubleTapSeek: (Boolean) -> Unit,
    onShapeChange: (ArtworkShape) -> Unit,
    onSeekbarStyleChange: (SeekbarStyle) -> Unit,
    onRecenterAr: () -> Unit,
    player: Player?,
    isFullScreen: Boolean,
    onSetFullScreen: (Boolean) -> Unit,
    isSwitchingMode: Boolean = false,
    sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    sleepTimerRemainingMs: Long? = null,
    progressProvider: () -> Float = { 0f },
    positionProvider: () -> Long = { 0L },
    durationProvider: () -> Long = { 0L },
    windowSizeClass: WindowSizeClass? = null
) {
    val combinedLoading = playerState.isLoading || isSwitchingMode
    val controlsAlpha by animateFloatAsState(
        targetValue = if (combinedLoading) 0.45f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label = "controlsDimOnLoad"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (isVideoMode && player != null && !isFullScreen) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize(0.85f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .clickable { onSetFullScreen(true) },
                        tonalElevation = 16.dp,
                        shadowElevation = 16.dp
                    ) {
                        AndroidView(
                            factory = { context ->
                                PlayerView(context).apply {
                                    this.player = player
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    AlbumArtwork(
                        imageUrl = song?.thumbnailUrl,
                        title = song?.title,
                        dominantColors = dominantColors,
                        isLoading = playerState.isLoading,
                        isPlaying = playerState.isPlaying,
                        isRotatingEnabled = isRotatingEnabled,
                        onSwipeLeft = actions.onNext,
                        onSwipeRight = actions.onPrevious,
                        initialShape = currentArtworkShape,
                        artworkSize = currentArtworkSize,
                        onShapeChange = onShapeChange,
                        onDoubleTapLeft = { handleDoubleTapSeek(false) },
                        onDoubleTapRight = { handleDoubleTapSeek(true) },
                        songId = song?.id,
                        modifier = Modifier.fillMaxSize(0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                PlayerTopBar(
                    onBack = actions.onBack,
                    dominantColors = dominantColors,
                    isVideoMode = isVideoMode,
                    isYouTubeSong = song?.source == SongSource.YOUTUBE,
                    onVideoToggle = onToggleVideoMode,
                    onMoreClick = onShowActions,
                    onCastClick = onShowDevices,
                    audioArEnabled = audioArEnabled,
                    onRecenter = onRecenterAr
                )

                SongInfoSection(
                    song = song,
                    isFavorite = playbackInfo.isLiked,
                    onFavoriteClick = actions.onToggleLike,
                    isDisliked = playbackInfo.isDisliked,
                    onDislikeClick = actions.onToggleDislike,
                    onMoreClick = onShowActions,
                    onArtistClick = actions.onArtistClick,
                    onAlbumClick = { actions.onAlbumClickWithSong(it, song?.id) },
                    dominantColors = dominantColors,
                    isLoading = playerState.isLoading,
                    sleepTimerOption = sleepTimerOption,
                    sleepTimerRemainingMs = sleepTimerRemainingMs,
                    showMoreButton = false,
                    isClassic = false,
                    activeAudioSource = playerState.activeAudioSource,
                    isSwitchingSource = playerState.isSwitchingSource,
                    onSwitchAudioSource = actions.onSwitchAudioSource
                )

                PlayerActionChips(
                    isFavorite = playbackInfo.isLiked,
                    isDisliked = playbackInfo.isDisliked,
                    onToggleLike = actions.onToggleLike,
                    onToggleDislike = actions.onToggleDislike,
                    onLyricsClick = onShowLyrics,
                    onRelatedClick = onShowRelated,
                    onDownloadClick = actions.onDownload,
                    downloadState = playbackInfo.downloadState,
                    dominantColors = dominantColors,
                    onSleepTimerClick = onShowSleepTimer,
                    onSpeedClick = onShowPlaybackSpeed,
                    sleepTimerRemainingMs = sleepTimerRemainingMs,
                    playbackSpeed = playbackInfo.playbackSpeed,
                    activeAudioSource = playerState.activeAudioSource,
                    isSwitchingSource = playerState.isSwitchingSource,
                    onSwitchAudioSource = actions.onSwitchAudioSource,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = controlsAlpha }) {
                    WaveformSeeker(
                        progressProvider = progressProvider,
                        isPlaying = playerState.isPlaying,
                        onSeek = { fraction ->
                            val target = (fraction * durationProvider()).toLong()
                            actions.onSeekTo(target)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        activeColor = dominantColors.accent,
                        inactiveColor = dominantColors.onBackground.copy(alpha = 0.2f),
                        initialStyle = currentSeekbarStyle,
                        onStyleChange = onSeekbarStyleChange,
                        duration = durationProvider(),
                        sponsorSegments = sponsorSegments
                    )
                }

                TimeLabelsWithQuality(
                    currentPositionProvider = positionProvider,
                    durationProvider = durationProvider,
                    dominantColors = dominantColors
                )

                Box(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = controlsAlpha }) {
                    PlaybackControls(
                        isPlaying = playerState.isPlaying,
                        shuffleEnabled = playerState.shuffleEnabled,
                        repeatMode = playerState.repeatMode.ordinal,
                        onPlayPause = actions.onPlayPause,
                        onNext = actions.onNext,
                        onPrevious = actions.onPrevious,
                        onShuffleToggle = actions.onShuffleToggle,
                        onRepeatToggle = actions.onRepeatToggle,
                        dominantColors = dominantColors
                    )
                }

                QueueHandle(
                    onClick = onShowQueue,
                    dominantColors = dominantColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        M3ELoadingOverlay(
            isLoading = combinedLoading,
            dominantColors = dominantColors,
            modifier = Modifier.fillMaxSize()
        )

    }
}
