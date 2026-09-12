/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.window.core.layout.WindowWidthSizeClass
import com.omnitune.app.LocalDownloadUtil
import com.omnitune.app.constants.EqualizerBandLevelsMbKey
import com.omnitune.app.constants.EqualizerBassBoostEnabledKey
import com.omnitune.app.constants.EqualizerBassBoostStrengthKey
import com.omnitune.app.constants.DoubleTapSeekSecondsKey
import com.omnitune.app.constants.EqualizerEnabledKey
import com.omnitune.app.constants.EqualizerPreampLevelMbKey
import com.omnitune.app.constants.EqualizerVirtualizerEnabledKey
import com.omnitune.app.constants.EqualizerVirtualizerStrengthKey
import com.omnitune.app.extensions.mediaItems
import com.omnitune.app.extensions.metadata
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.ArtworkShape
import com.omnitune.app.models.ArtworkSize
import com.omnitune.app.models.DownloadState
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.models.PlayerStyle
import com.omnitune.app.models.RepeatMode
import com.omnitune.app.models.SeekbarStyle
import com.omnitune.app.models.SleepTimerOption
import com.omnitune.app.models.Song
import com.omnitune.app.models.SongSource
import com.omnitune.app.models.SponsorSegment
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.playback.EqualizerBand
import com.omnitune.app.playback.EqualizerPresets
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.playback.PlayerProgressState
import com.omnitune.app.playback.decodeEqualizerBands
import com.omnitune.app.playback.encodeEqualizerBands
import com.omnitune.app.playback.withPreamp
import com.omnitune.app.ui.component.AddToPlaylistSheet
import com.omnitune.app.ui.component.CreatePlaylistDialog
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.component.SongInfoScreen
import com.omnitune.app.ui.component.VideoErrorDialog
import com.omnitune.app.ui.component.glass.GlassArtwork
import com.omnitune.app.ui.component.glass.LocalGlassArtwork
import com.omnitune.app.ui.component.rememberDominantColors
import com.omnitune.app.ui.player.components.EqualizerSheet
import com.omnitune.app.ui.player.components.GlassArtBackground
import com.omnitune.app.ui.player.components.ModernQueueView
import com.omnitune.app.ui.player.components.OutputDeviceSheet
import com.omnitune.app.ui.player.components.PlaybackSpeedSheet
import com.omnitune.app.ui.player.components.QueuePlayerHeader
import com.omnitune.app.ui.player.components.RelatedSheet
import com.omnitune.app.ui.player.components.SleepTimerSheet
import com.omnitune.app.ui.player.components.SongActionsSheet
import com.omnitune.app.ui.player.components.VolumeControl
import com.omnitune.app.ui.player.styles.ClassicPlayerStyle
import com.omnitune.app.ui.player.styles.LiquidGlassPlayerStyle
import com.omnitune.app.ui.player.styles.YTMusicPlayerStyle
import com.omnitune.app.ui.screens.PlaylistManagementViewModel
import com.omnitune.app.ui.theme.YtFlatBackground
import com.omnitune.app.utils.rememberPreference
import com.omnitune.app.viewmodels.PlayerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf

/**
 * Top-level PlayerScreen bound to PlayerConnection and PlayerViewModel.
 */
@Composable
fun PlayerScreen(
    playerConnection: PlayerConnection?,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onOpenAIEqualizer: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
    volumeKeyEvents: SharedFlow<Unit>? = null,
    volumeSliderEnabled: Boolean = true
) {
    val context = LocalContext.current
    val meta by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsStateWithLifecycle(initialValue = null)
    val isPlaying by (playerConnection?.isPlaying ?: flowOf(false)).collectAsStateWithLifecycle(initialValue = false)
    val playbackStateInt by (playerConnection?.playbackState ?: flowOf(Player.STATE_IDLE)).collectAsStateWithLifecycle(initialValue = Player.STATE_IDLE)
    val sleepTimerRemaining by (playerConnection?.sleepTimerRemaining ?: flowOf(0L)).collectAsStateWithLifecycle(initialValue = 0L)
    val repeatModeInt by (playerConnection?.repeatMode ?: flowOf(Player.REPEAT_MODE_OFF)).collectAsStateWithLifecycle(initialValue = Player.REPEAT_MODE_OFF)
    val shuffleEnabled by (playerConnection?.shuffleModeEnabled ?: flowOf(false)).collectAsStateWithLifecycle(initialValue = false)
    val currentMediaItemIndex by (playerConnection?.currentMediaItemIndex ?: flowOf(-1)).collectAsStateWithLifecycle(initialValue = -1)
    val queueIndices by (playerConnection?.queueIndices ?: flowOf(emptyList())).collectAsStateWithLifecycle(initialValue = emptyList())
    val playbackParameters by (playerConnection?.playbackParameters ?: flowOf(PlaybackParameters.DEFAULT)).collectAsStateWithLifecycle(initialValue = PlaybackParameters.DEFAULT)
    val initialProgress = remember(meta?.id, meta?.duration) {
        PlayerProgressState(durationMs = meta?.duration?.toLong()?.takeIf { it > 0L }?.times(1000L) ?: 0L)
    }
    val progressState by remember(playerConnection, meta?.id, meta?.duration) {
        playerConnection?.progressState ?: flowOf(initialProgress)
    }.collectAsStateWithLifecycle(initialValue = initialProgress)

    val currentSongEntity by (playerConnection?.currentSong ?: flowOf(null)).collectAsStateWithLifecycle(initialValue = null)

    var showVideoErrorDialog by remember { mutableStateOf(false) }

    val song: Song? = remember(meta, currentSongEntity) {
        val metadata = meta
        if (metadata != null) {
            Song(
                id = metadata.id,
                title = metadata.title,
                artist = metadata.artists.joinToString(", ") { it.name },
                album = metadata.album?.title.orEmpty(),
                artistId = metadata.artists.firstOrNull()?.id,
                thumbnailUrl = metadata.thumbnailUrl,
                duration = metadata.duration.toLong() * 1000L,
                source = if (currentSongEntity?.song?.isLocal == true) SongSource.LOCAL else SongSource.YOUTUBE
            )
        } else {
            null
        }
    }

    val repeatMode = when (repeatModeInt) {
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        else -> RepeatMode.OFF
    }

    val queueSongs = remember(playerConnection, queueIndices, currentMediaItemIndex, meta) {
        playerConnection?.player?.mediaItems
            ?.map { item -> item.toQueueSong() }
            .orEmpty()
    }
    val isVideoMode by viewModel.isVideoMode.collectAsStateWithLifecycle()
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()

    val pState = PlayerState(
        currentSong = song,
        isPlaying = isPlaying,
        currentPosition = progressState.positionMs,
        duration = progressState.durationMs,
        isLoading = playbackStateInt == STATE_BUFFERING,
        queue = queueSongs,
        currentIndex = currentMediaItemIndex,
        repeatMode = repeatMode,
        shuffleEnabled = shuffleEnabled,
        isLiked = currentSongEntity?.song?.liked ?: (meta?.liked == true),
        playbackSpeed = playbackParameters.speed,
        pitch = playbackParameters.pitch,
        isVideoMode = isVideoMode,
        availableDevices = availableDevices,
        selectedDevice = availableDevices.firstOrNull { it.isSelected }
    )


    val activeOverlay by viewModel.activeOverlay.collectAsStateWithLifecycle()
    val relatedSongs by viewModel.relatedSongs.collectAsStateWithLifecycle()
    val isFetchingRelated by viewModel.isFetchingRelated.collectAsStateWithLifecycle()
    val relatedError by viewModel.relatedError.collectAsStateWithLifecycle()
    val selectedRelatedIndices by viewModel.selectedRelatedIndices.collectAsStateWithLifecycle()
    val selectedQueueIndices by viewModel.selectedQueueIndices.collectAsStateWithLifecycle()
    val playerStyle by viewModel.playerStyle.collectAsStateWithLifecycle()
    val seekbarStyle by viewModel.seekbarStyle.collectAsStateWithLifecycle()
    val artworkShape by viewModel.artworkShape.collectAsStateWithLifecycle()
    val artworkSize by viewModel.artworkSize.collectAsStateWithLifecycle()
    val sponsorSegments by viewModel.sponsorSegments.collectAsStateWithLifecycle()
    val isFullScreen by viewModel.isFullScreen.collectAsStateWithLifecycle()
    val audioArEnabled by viewModel.audioArEnabled.collectAsStateWithLifecycle()
    val isRotatingEnabled by viewModel.isRotatingEnabled.collectAsStateWithLifecycle()

    val screenState = PlayerScreenState(
        playbackInfo = pState,
        playerState = pState,
        relatedSongs = relatedSongs,
        isFetchingRelated = isFetchingRelated,
        relatedError = relatedError,
        selectedRelatedIndices = selectedRelatedIndices,
        sleepTimerOption = if (sleepTimerRemaining > 0) SleepTimerOption.CUSTOM else SleepTimerOption.OFF,
        sleepTimerRemainingMs = if (sleepTimerRemaining > 0) sleepTimerRemaining else null
    )

    LaunchedEffect(queueSongs.size) {
        viewModel.pruneQueueSelection(queueSongs.size)
    }

    LaunchedEffect(song?.id) {
        viewModel.onMediaItemChanged(song?.id)
    }

    val actions = PlayerScreenActions(
        onBack = onDismiss,
        onPlayPause = {
            if (isPlaying) {
                playerConnection?.pause()
            } else {
                playerConnection?.playOrResolveCurrent()
            }
        },
        onNext = { playerConnection?.seekToNext() },
        onPrevious = { playerConnection?.seekToPrevious() },
        onSeekTo = { pos -> playerConnection?.player?.seekTo(pos) },
        onToggleLike = { playerConnection?.toggleLike() },
        onShuffleToggle = {
            val p = playerConnection?.player
            if (p != null) {
                p.shuffleModeEnabled = !p.shuffleModeEnabled
                playerConnection.shuffleModeEnabled.value = p.shuffleModeEnabled
            }
        },
        onRepeatToggle = {
            val p = playerConnection?.player
            if (p != null) {
                val nextMode = when (p.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                p.repeatMode = nextMode
                playerConnection.repeatMode.value = nextMode
            }
        },
        onArtistClick = { artistId -> onNavigateToArtist(artistId) },
        onAlbumClick = { albumId -> onNavigateToAlbum(albumId) },
        onAlbumClickWithSong = { albumId, _ -> onNavigateToAlbum(albumId) },
        onDownload = {
            song?.let { current ->
                playerConnection?.service?.downloadUtil?.enqueue(current.id, current.title) { _, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        },
        onToggleVideoMode = {
            val p = playerConnection?.player
            val hasVideoTrack = p?.currentTracks?.isTypeSupported(C.TRACK_TYPE_VIDEO) == true ||
                ((p?.videoSize?.width ?: 0) > 0 && (p?.videoSize?.height ?: 0) > 0)
            when {
                song?.source != SongSource.YOUTUBE -> {
                    showVideoErrorDialog = true
                    viewModel.setVideoMode(false)
                }
                p == null -> Toast.makeText(context, "Player is not ready yet", Toast.LENGTH_SHORT).show()
                hasVideoTrack -> viewModel.toggleVideoMode(song.id)
                else -> {
                    showVideoErrorDialog = true
                    viewModel.setVideoMode(false)
                }
            }
        },
        onPlayFromQueue = { index ->
            playerConnection?.seekTo(index, 0)
        },
        onSetSleepTimer = { option, minutes ->
            playerConnection?.applySleepTimer(option, minutes)
        },
        onSwitchDevice = { device ->
            viewModel.switchOutputDevice(device, playerConnection)
        },
        onRefreshDevices = viewModel::refreshDevices,
        onSetPlaybackParameters = { speed, pitch ->
            playerConnection?.setPlaybackParameters(speed, pitch)
        },
        onToggleRelatedSelection = { viewModel.toggleRelatedSelection(it) },
        onSelectAllRelated = { viewModel.selectAllRelated() },
        onClearRelatedSelection = { viewModel.clearRelatedSelection() },
        onAddRelatedToQueue = { songsToAdd ->
            playerConnection?.addToQueue(songsToAdd.toPlaybackMediaItems())
        },
        onPlayRelated = { related ->
            playerConnection?.playNext(related.toPlaybackMediaItem())
            playerConnection?.seekToNext()
        },
        onShowAIEqualizer = onOpenAIEqualizer,
        onClearQueue = { playerConnection?.clearQueue() }
    )

    PlayerScreen(
        state = screenState,
        originalActions = actions,
        player = playerConnection?.player,
        playerViewModel = viewModel,
        volumeKeyEvents = volumeKeyEvents,
        volumeSliderEnabled = volumeSliderEnabled,
        playerConnection = playerConnection
    )

    if (showVideoErrorDialog) {
        VideoErrorDialog(
            onDismiss = { showVideoErrorDialog = false },
            onSwitchToAudio = {
                showVideoErrorDialog = false
                viewModel.setVideoMode(false)
            },
            dominantColors = rememberDominantColors(song?.thumbnailUrl, isSystemInDarkTheme())
        )
    }
}

/**
 * Core PlayerScreen implementation matching the ported layout and overlays.
 */
@Composable
fun PlayerScreen(
    state: PlayerScreenState,
    originalActions: PlayerScreenActions,
    player: Player? = null,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    volumeKeyEvents: SharedFlow<Unit>? = null,
    volumeSliderEnabled: Boolean = true,
    playerConnection: PlayerConnection? = null
) {
    val playbackInfo = state.playbackInfo
    val playerState = state.playerState
    val song = playbackInfo.currentSong
    val context = LocalContext.current

    val sponsorSegments by playerViewModel.sponsorSegments.collectAsStateWithLifecycle()
    val isFullScreen by playerViewModel.isFullScreen.collectAsStateWithLifecycle()
    val playerStyle by playerViewModel.playerStyle.collectAsStateWithLifecycle()
    val seekbarStyle by playerViewModel.seekbarStyle.collectAsStateWithLifecycle()
    val artworkShape by playerViewModel.artworkShape.collectAsStateWithLifecycle()
    val artworkSize by viewModelArtworkSize(playerViewModel)
    val animatedBackgroundEnabled by playerViewModel.animatedBackgroundEnabled.collectAsStateWithLifecycle()
    val albumArtDynamicColorsEnabled by playerViewModel.albumArtDynamicColorsEnabled.collectAsStateWithLifecycle()
    val audioArEnabled by playerViewModel.audioArEnabled.collectAsStateWithLifecycle()
    val isRotatingEnabled by playerViewModel.isRotatingEnabled.collectAsStateWithLifecycle()
    val activeOverlay by playerViewModel.activeOverlay.collectAsStateWithLifecycle()

    val formFactor = com.omnitune.app.ui.utils.LocalDeviceFormFactor.current
    val isExpanded = formFactor.isTabletLike

    val isDarkTheme = isSystemInDarkTheme()
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val extractedColors = rememberDominantColors(song?.thumbnailUrl, isDarkTheme)
    val finalColors = if (albumArtDynamicColorsEnabled) {
        extractedColors
    } else if (isDarkTheme) {
        DominantColors()
    } else {
        DominantColors(
            primary = Color(0xFFF5F5F5),
            secondary = Color(0xFFE8E8E8),
            accent = Color(0xFF666666),
            onBackground = Color(0xFF1A1A1A)
        )
    }
    val animatedPrimary by animateColorAsState(targetValue = finalColors.primary, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow), label = "primary")
    val animatedSecondary by animateColorAsState(targetValue = finalColors.secondary, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow), label = "secondary")
    val animatedAccent by animateColorAsState(targetValue = finalColors.accent, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow), label = "accent")
    val animatedOnBg by animateColorAsState(targetValue = finalColors.onBackground, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow), label = "onBg")

    val dominantColors = DominantColors(primary = animatedPrimary, secondary = animatedSecondary, accent = animatedAccent, onBackground = animatedOnBg)

    val bgLoadingAlpha by animateFloatAsState(
        targetValue = if (playerState.isLoading) 0.85f else 1f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow),
        label = "bgLoadingDim"
    )

    DisposableEffect(isDarkTheme) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            val previousLightStatusBars = insetsController.isAppearanceLightStatusBars
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            onDispose { insetsController.isAppearanceLightStatusBars = previousLightStatusBars }
        } else {
            onDispose { }
        }
    }


    BackHandler {
        if (activeOverlay != PlayerOverlay.None) {
            val overlay = activeOverlay
            when {
                overlay is PlayerOverlay.Actions && overlay.fromQueue -> playerViewModel.setActiveOverlay(PlayerOverlay.Queue)
                overlay is PlayerOverlay.Actions && overlay.fromRelated -> playerViewModel.setActiveOverlay(PlayerOverlay.Related)
                else -> playerViewModel.dismissOverlay()
            }
        } else {
            originalActions.onBack()
        }
    }

    var pendingSeekPosition by remember { mutableStateOf<Long?>(null) }
    var seekDebounceJob by remember { mutableStateOf<Job?>(null) }
    val playerStateProvider by rememberUpdatedState(playerState)

    val progressProvider = remember { { playerStateProvider.progress } }
    val positionProvider = remember { { playerStateProvider.currentPosition } }
    val durationProvider = remember { { playerStateProvider.duration } }
    val doubleTapSeekSeconds by rememberPreference(DoubleTapSeekSecondsKey, 10)

    val handleDoubleTapSeek: (Boolean) -> Unit = remember(doubleTapSeekSeconds) {
        { forward ->
            val currentPos = playerStateProvider.currentPosition
            val durationMs = playerStateProvider.duration
            val current = pendingSeekPosition ?: currentPos
            val seekAmount = doubleTapSeekSeconds.coerceAtLeast(1) * 1000L
            val newPos = if (forward) (current + seekAmount).coerceAtMost(durationMs) else (current - seekAmount).coerceAtLeast(0)
            pendingSeekPosition = newPos
            seekDebounceJob?.cancel()
            seekDebounceJob = coroutineScope.launch {
                delay(400)
                originalActions.onSeekTo(newPos)
                delay(600)
                pendingSeekPosition = null
            }
        }
    }

    val playerBackgroundColor = if (isDarkTheme) Color.Black else Color.White

    androidx.compose.runtime.CompositionLocalProvider(
        LocalGlassArtwork provides GlassArtwork(
            artworkUrl = song?.thumbnailUrl?.takeIf { !playerState.isVideoMode },
            colors = dominantColors,
            isDarkTheme = isDarkTheme
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(playerBackgroundColor)
                .graphicsLayer { alpha = bgLoadingAlpha }
        ) {
            if (animatedBackgroundEnabled && playerStyle != PlayerStyle.LIQUID_GLASS) {
                GlassArtBackground(
                    thumbnailUrl = song?.thumbnailUrl,
                    isDarkTheme = isDarkTheme,
                    isVideoMode = playerState.isVideoMode,
                    dominantColors = dominantColors,
                    blurRadius = 60f,
                    intensity = 1f
                )
            }

            val playerMainContent: @Composable () -> Unit = {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useWideLayout = maxWidth > 520.dp && maxWidth > maxHeight
                    val isCompactHeight = maxHeight < 600.dp

                    when (playerStyle) {
                        PlayerStyle.YT_MUSIC -> {
                            YTMusicPlayerStyle(
                                song = song,
                                playerState = playerState,
                                playbackInfo = playbackInfo,
                                dominantColors = dominantColors,
                                currentArtworkShape = artworkShape,
                                currentArtworkSize = artworkSize,
                                currentSeekbarStyle = seekbarStyle,
                                sponsorSegments = sponsorSegments,
                                audioArEnabled = audioArEnabled,
                                isRotatingEnabled = isRotatingEnabled,
                                player = player,
                                isFullScreen = isFullScreen,
                                isCompactHeight = isCompactHeight,
                                useWideLayout = useWideLayout,
                                actions = originalActions,
                                onShowActions = { playerViewModel.setActiveOverlay(PlayerOverlay.Actions(song)) },
                                onShowQueue = { playerViewModel.setActiveOverlay(PlayerOverlay.Queue) },
                                onShowLyrics = { playerViewModel.setActiveOverlay(PlayerOverlay.Lyrics) },
                                onShowRelated = {
                                    playerViewModel.refreshRelatedSongs(song)
                                    playerViewModel.setActiveOverlay(PlayerOverlay.Related)
                                },
                                onShowDevices = {
                                    playerViewModel.refreshDevices()
                                    playerViewModel.setActiveOverlay(PlayerOverlay.OutputDevice)
                                },
                                onShowSleepTimer = { playerViewModel.setActiveOverlay(PlayerOverlay.SleepTimer) },
                                onShowPlaybackSpeed = { playerViewModel.setActiveOverlay(PlayerOverlay.PlaybackSpeed) },
                                onShowEqualizer = { playerViewModel.setActiveOverlay(PlayerOverlay.Equalizer) },
                                handleDoubleTapSeek = handleDoubleTapSeek,
                                onShapeChange = { shape -> playerViewModel.setArtworkShape(shape) },
                                onSeekbarStyleChange = { style -> playerViewModel.setSeekbarStyle(style) },
                                onRecenterAr = playerViewModel::refreshDevices,
                                onSetFullScreen = { playerViewModel.setFullScreen(it) },
                                 isSwitchingMode = false,
                                sleepTimerOption = state.sleepTimerOption,
                                sleepTimerRemainingMs = state.sleepTimerRemainingMs,
                                progressProvider = progressProvider,
                                positionProvider = positionProvider,
                                durationProvider = durationProvider
                            )
                        }
                        PlayerStyle.LIQUID_GLASS -> {
                            LiquidGlassPlayerStyle(
                                song = song,
                                playerState = playerState,
                                playbackInfo = playbackInfo,
                                dominantColors = dominantColors,
                                currentArtworkShape = artworkShape,
                                currentArtworkSize = artworkSize,
                                currentSeekbarStyle = seekbarStyle,
                                sponsorSegments = sponsorSegments,
                                audioArEnabled = audioArEnabled,
                                isRotatingEnabled = isRotatingEnabled,
                                player = player,
                                isFullScreen = isFullScreen,
                                isCompactHeight = isCompactHeight,
                                useWideLayout = useWideLayout,
                                actions = originalActions,
                                onShowActions = { playerViewModel.setActiveOverlay(PlayerOverlay.Actions(song)) },
                                onShowQueue = { playerViewModel.setActiveOverlay(PlayerOverlay.Queue) },
                                onShowLyrics = { playerViewModel.setActiveOverlay(PlayerOverlay.Lyrics) },
                                onShowRelated = {
                                    playerViewModel.refreshRelatedSongs(song)
                                    playerViewModel.setActiveOverlay(PlayerOverlay.Related)
                                },
                                onShowDevices = {
                                    playerViewModel.refreshDevices()
                                    playerViewModel.setActiveOverlay(PlayerOverlay.OutputDevice)
                                },
                                onShowSleepTimer = { playerViewModel.setActiveOverlay(PlayerOverlay.SleepTimer) },
                                onShowPlaybackSpeed = { playerViewModel.setActiveOverlay(PlayerOverlay.PlaybackSpeed) },
                                onShowEqualizer = { playerViewModel.setActiveOverlay(PlayerOverlay.Equalizer) },
                                handleDoubleTapSeek = handleDoubleTapSeek,
                                onShapeChange = { shape -> playerViewModel.setArtworkShape(shape) },
                                onSeekbarStyleChange = { style -> playerViewModel.setSeekbarStyle(style) },
                                onRecenterAr = playerViewModel::refreshDevices,
                                onSetFullScreen = { playerViewModel.setFullScreen(it) },
                                isSwitchingMode = false,
                                sleepTimerOption = state.sleepTimerOption,
                                sleepTimerRemainingMs = state.sleepTimerRemainingMs,
                                progressProvider = progressProvider,
                                positionProvider = positionProvider,
                                durationProvider = durationProvider,
                                backgroundArtworkUrl = if (animatedBackgroundEnabled) song?.thumbnailUrl else ""
                            )
                        }
                        PlayerStyle.CLASSIC -> {
                            ClassicPlayerStyle(
                                song = song,
                                playerState = playerState,
                                playbackInfo = playbackInfo,
                                dominantColors = dominantColors,
                                currentArtworkShape = artworkShape,
                                currentArtworkSize = artworkSize,
                                currentSeekbarStyle = seekbarStyle,
                                sponsorSegments = sponsorSegments,
                                audioArEnabled = audioArEnabled,
                                isRotatingEnabled = isRotatingEnabled,
                                player = player,
                                isFullScreen = isFullScreen,
                                isCompactHeight = isCompactHeight,
                                useWideLayout = useWideLayout,
                                actions = originalActions,
                                onShowActions = { playerViewModel.setActiveOverlay(PlayerOverlay.Actions(song)) },
                                onShowQueue = { playerViewModel.setActiveOverlay(PlayerOverlay.Queue) },
                                onShowLyrics = { playerViewModel.setActiveOverlay(PlayerOverlay.Lyrics) },
                                onShowRelated = {
                                    playerViewModel.refreshRelatedSongs(song)
                                    playerViewModel.setActiveOverlay(PlayerOverlay.Related)
                                },
                                onShowDevices = {
                                    playerViewModel.refreshDevices()
                                    playerViewModel.setActiveOverlay(PlayerOverlay.OutputDevice)
                                },
                                onShowSleepTimer = { playerViewModel.setActiveOverlay(PlayerOverlay.SleepTimer) },
                                onShowPlaybackSpeed = { playerViewModel.setActiveOverlay(PlayerOverlay.PlaybackSpeed) },
                                onShowEqualizer = { playerViewModel.setActiveOverlay(PlayerOverlay.Equalizer) },
                                handleDoubleTapSeek = handleDoubleTapSeek,
                                onShapeChange = { shape -> playerViewModel.setArtworkShape(shape) },
                                onSeekbarStyleChange = { style -> playerViewModel.setSeekbarStyle(style) },
                                onRecenterAr = playerViewModel::refreshDevices,
                                onSetFullScreen = { playerViewModel.setFullScreen(it) },
                                isSwitchingMode = false,
                                sleepTimerOption = state.sleepTimerOption,
                                sleepTimerRemainingMs = state.sleepTimerRemainingMs,
                                progressProvider = progressProvider,
                                positionProvider = positionProvider,
                                durationProvider = durationProvider
                            )
                        }
                    }
                }
            }

            playerMainContent()

            Box(modifier = Modifier.fillMaxSize()) {
                OverlaysContent(
                    state = state,
                    actions = originalActions,
                    activeOverlay = activeOverlay,
                    onOverlayChange = { playerViewModel.setActiveOverlay(it) },
                    dominantColors = dominantColors,
                    playerViewModel = playerViewModel,
                    isAppInDarkTheme = isDarkTheme,
                    volumeSliderEnabled = volumeSliderEnabled,
                    volumeKeyEvents = volumeKeyEvents,
                    isFullScreen = isFullScreen,
                    isExpanded = isExpanded,
                    animatedBackgroundEnabled = animatedBackgroundEnabled,
                    progressProvider = progressProvider,
                    positionProvider = positionProvider,
                    durationProvider = durationProvider,
                    playerConnection = playerConnection
                )
            }

            AnimatedVisibility(
                visible = isFullScreen && playerState.isVideoMode && player != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                if (player != null) {
                    FullScreenVideoPlayer(
                        player = player,
                        playerState = playerState,
                        actions = originalActions,
                        dominantColors = dominantColors,
                        onDismiss = { playerViewModel.setFullScreen(false) }
                    )
                }
            }
        }
    }
}

@Composable
private fun viewModelArtworkSize(viewModel: PlayerViewModel) =
    viewModel.artworkSize.collectAsStateWithLifecycle()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.OverlaysContent(
    state: PlayerScreenState,
    actions: PlayerScreenActions,
    activeOverlay: PlayerOverlay,
    onOverlayChange: (PlayerOverlay) -> Unit,
    dominantColors: DominantColors,
    playerViewModel: PlayerViewModel,
    isAppInDarkTheme: Boolean,
    volumeSliderEnabled: Boolean,
    volumeKeyEvents: SharedFlow<Unit>?,
    isFullScreen: Boolean,
    isExpanded: Boolean = false,
    animatedBackgroundEnabled: Boolean = true,
    progressProvider: () -> Float,
    positionProvider: () -> Long,
    durationProvider: () -> Long,
    playerConnection: PlayerConnection? = null
) {
    val song = state.playbackInfo.currentSong
    val playerState = state.playerState
    val currentOverlay by rememberUpdatedState(activeOverlay)
    val context = LocalContext.current
    val downloadUtil = LocalDownloadUtil.current
    val playlistViewModel: PlaylistManagementViewModel = hiltViewModel()
    val playlistMgmtState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val selectedQueueIndices by playerViewModel.selectedQueueIndices.collectAsStateWithLifecycle()

    var storedEqualizerBands by rememberPreference(EqualizerBandLevelsMbKey, "")
    var equalizerEnabled by rememberPreference(EqualizerEnabledKey, false)
    var equalizerPreampLevelMb by rememberPreference(EqualizerPreampLevelMbKey, 0)
    var bassBoostEnabled by rememberPreference(EqualizerBassBoostEnabledKey, false)
    var bassBoostStrength by rememberPreference(EqualizerBassBoostStrengthKey, 0)
    var virtualizerEnabled by rememberPreference(EqualizerVirtualizerEnabledKey, false)
    var virtualizerStrength by rememberPreference(EqualizerVirtualizerStrengthKey, 0)

    val equalizerBands = remember(storedEqualizerBands) {
        decodeEqualizerBands(storedEqualizerBands) ?: EqualizerPresets.FLAT.bands
    }
    val equalizerBandLevels = remember(equalizerBands) {
        equalizerBands.map { it.gainDb }.toFloatArray()
    }

    if (volumeSliderEnabled) {
        VolumeControl(
            dominantColors = dominantColors,
            volumeKeyEvents = volumeKeyEvents,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(0.3f).padding(end = 16.dp)
        )
    }

    if (!isExpanded) {
        val actionsOverlay = activeOverlay as? PlayerOverlay.Actions
        val queueVisible = activeOverlay is PlayerOverlay.Queue || actionsOverlay?.fromQueue == true
        if (queueVisible) {
            val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
            val isQueueExpanded by remember {
                derivedStateOf { queueSheetState.targetValue == SheetValue.Expanded }
            }
            ModalBottomSheet(
                onDismissRequest = { if (currentOverlay is PlayerOverlay.Queue) onOverlayChange(PlayerOverlay.None) },
                sheetState = queueSheetState,
                containerColor = if (isAppInDarkTheme) YtFlatBackground else MaterialTheme.colorScheme.surface,
                contentWindowInsets = { WindowInsets(0) },
                dragHandle = if (isQueueExpanded) null else ({ BottomSheetDefaults.DragHandle() }),
                scrimColor = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxHeight()
            ) {
                ModernQueueView(
                    currentSong = song,
                    queue = playerState.queue,
                    upNextSongs = emptyList(),
                    selectedQueueIndices = selectedQueueIndices,
                    onToggleSelection = playerViewModel::toggleQueueSelection,
                    onSelectAll = { playerViewModel.selectAllQueue(playerState.queue.size) },
                    onClearSelection = playerViewModel::clearQueueSelection,
                    currentIndex = playerState.currentIndex,
                    isPlaying = playerState.isPlaying,
                    shuffleEnabled = playerState.shuffleEnabled,
                    repeatMode = playerState.repeatMode.ordinal,
                    isAutoplayEnabled = playerState.isAutoplayEnabled,
                    isFavorite = playerState.isLiked,
                    isRadioMode = state.isRadioMode,
                    isLoadingMore = state.isLoadingMoreSongs,
                    onBack = { if (currentOverlay is PlayerOverlay.Queue) onOverlayChange(PlayerOverlay.None) },
                    onSongClick = actions.onPlayFromQueue,
                    onPlayPause = actions.onPlayPause,
                    onToggleShuffle = actions.onShuffleToggle,
                    onToggleRepeat = actions.onRepeatToggle,
                    onToggleAutoplay = actions.onToggleAutoplay,
                    onToggleLike = actions.onToggleLike,
                    onMoreClick = { onOverlayChange(PlayerOverlay.Actions(it, fromQueue = true)) },
                    onLoadMore = actions.onLoadMoreRadioSongs,
                    onMoveItem = { from, to ->
                        val count = playerConnection?.mediaItemCount ?: 0
                        if (from in 0 until count && to in 0 until count && from != to) {
                            playerConnection?.moveMediaItem(from, to)
                        }
                    },
                    onRemoveItems = { indices ->
                        val count = playerConnection?.mediaItemCount ?: 0
                        indices
                            .filter { it in 0 until count }
                            .sortedDescending()
                            .forEach { index -> playerConnection?.removeMediaItem(index) }
                        playerViewModel.clearQueueSelection()
                    },
                    onSaveAsPlaylist = { _, _, _, _ -> },
                    onAddToPlaylistClick = { songs ->
                        playlistViewModel.showAddToPlaylistSheet(songs)
                        playerViewModel.clearQueueSelection()
                    },
                    onPlayNext = { songs ->
                        playerConnection?.playNext(songs.toPlaybackMediaItems())
                        playerViewModel.clearQueueSelection()
                    },
                    onAddToQueue = { songs ->
                        playerConnection?.addToQueue(songs.toPlaybackMediaItems())
                        playerViewModel.clearQueueSelection()
                    },
                    onClearQueue = {
                        actions.onClearQueue()
                        playerViewModel.clearQueueSelection()
                    },
                    dominantColors = dominantColors,
                    animatedBackgroundEnabled = animatedBackgroundEnabled,
                    isDarkTheme = isAppInDarkTheme,
                    modalMode = true,
                    nowPlayingHeaderOverride = if (isQueueExpanded) {
                        {
                            QueuePlayerHeader(
                                song = song,
                                isPlaying = playerState.isPlaying,
                                dominantColors = dominantColors,
                                progressProvider = progressProvider,
                                onPlayPause = actions.onPlayPause,
                                onNext = actions.onNext,
                                onPrevious = actions.onPrevious,
                                isDarkTheme = isAppInDarkTheme
                            )
                        }
                    } else null
                )
            }
        }

        if (activeOverlay is PlayerOverlay.Lyrics) {
            LyricsBottomSheet(
                playerConnection = playerConnection,
                onDismissRequest = { if (currentOverlay is PlayerOverlay.Lyrics) onOverlayChange(PlayerOverlay.None) }
            )
        }

        val songInfoOverlay = activeOverlay as? PlayerOverlay.SongInfo
        AnimatedVisibility(
            visible = songInfoOverlay != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            val infoSong = songInfoOverlay?.song
            if (infoSong != null) {
                SongInfoScreen(
                    song = infoSong,
                    onBack = { if (currentOverlay is PlayerOverlay.SongInfo) onOverlayChange(PlayerOverlay.None) },
                    onArtistClick = actions.onArtistClick,
                    onAlbumClick = actions.onAlbumClickWithSong,
                    audioCodec = playerState.audioCodec,
                    audioBitrate = playerState.audioBitrate,
                    dominantColors = dominantColors,
                    isDarkTheme = isAppInDarkTheme
                )
            }
        }

        AnimatedVisibility(
            visible = activeOverlay is PlayerOverlay.Related || actionsOverlay?.fromRelated == true,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            RelatedSheet(
                isVisible = true,
                relatedSongs = state.relatedSongs,
                isLoading = state.isFetchingRelated,
                errorMessage = state.relatedError,
                onRetry = { playerViewModel.refreshRelatedSongs(song) },
                selectedIndices = state.selectedRelatedIndices,
                onToggleSelection = actions.onToggleRelatedSelection,
                onSelectAll = actions.onSelectAllRelated,
                onClearSelection = actions.onClearRelatedSelection,
                onAddSelectedToQueue = {
                    val selectedSongs = state.selectedRelatedSongs()
                    if (selectedSongs.isNotEmpty()) {
                        actions.onAddRelatedToQueue(selectedSongs)
                        actions.onClearRelatedSelection()
                    }
                },
                onAddSelectedToPlaylist = {
                    val selectedSongs = state.selectedRelatedSongs()
                    if (selectedSongs.isNotEmpty()) {
                        playlistViewModel.showAddToPlaylistSheet(selectedSongs)
                        actions.onClearRelatedSelection()
                    }
                },
                onSongClick = { actions.onPlayRelated(it); onOverlayChange(PlayerOverlay.None) },
                onMoreClick = { onOverlayChange(PlayerOverlay.Actions(it, fromRelated = true)) },
                onClose = { if (currentOverlay is PlayerOverlay.Related) onOverlayChange(PlayerOverlay.None) },
                dominantColors = dominantColors,
                isDarkTheme = isAppInDarkTheme
            )
        }
    }

    val menuSong = (activeOverlay as? PlayerOverlay.Actions)?.targetSong ?: song
    if (menuSong != null) {
        SongActionsSheet(
            song = menuSong,
            isVisible = activeOverlay is PlayerOverlay.Actions,
            onDismiss = {
                val overlay = currentOverlay
                if (overlay is PlayerOverlay.Actions) {
                    when {
                        overlay.fromQueue -> onOverlayChange(PlayerOverlay.Queue)
                        overlay.fromRelated -> onOverlayChange(PlayerOverlay.Related)
                        else -> onOverlayChange(PlayerOverlay.None)
                    }
                }
            },
            dominantColors = dominantColors,
            onDownload = {
                downloadUtil.enqueue(menuSong.id, menuSong.title) { _, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            onToggleFavorite = { actions.onToggleLike() },
            onToggleDislike = { actions.onToggleDislike() },
            isFavorite = playerState.isLiked,
            isDisliked = playerState.isDisliked,
            onPlayNext = { playerConnection?.playNext(menuSong.toPlaybackMediaItem()) },
            onAddToQueue = { playerConnection?.addToQueue(menuSong.toPlaybackMediaItem()) },
            onViewInfo = { onOverlayChange(PlayerOverlay.SongInfo(menuSong)) },
            onAddToPlaylist = {
                playlistViewModel.showAddToPlaylistSheet(menuSong)
                onOverlayChange(PlayerOverlay.None)
            },
            onSleepTimer = { onOverlayChange(PlayerOverlay.SleepTimer) },
            onStartRadio = { actions.onStartRadio() },
            onPlaybackSpeed = { onOverlayChange(PlayerOverlay.PlaybackSpeed) },
            onEqualizerClick = { onOverlayChange(PlayerOverlay.Equalizer) },
            currentSpeed = playerState.playbackSpeed,
            isFromQueue = (activeOverlay as? PlayerOverlay.Actions)?.fromQueue ?: false,
            isCurrentlyPlaying = menuSong.id == song?.id,
            isDarkTheme = isAppInDarkTheme
        )
    }

    SleepTimerSheet(
        isVisible = activeOverlay is PlayerOverlay.SleepTimer,
        currentOption = state.sleepTimerOption,
        remainingTimeFormatted = state.sleepTimerRemainingMs?.let { String.format(java.util.Locale.US, "%d:%02d", it / 60000, (it / 1000) % 60) },
        onSelectOption = actions.onSetSleepTimer,
        onDismiss = { if (currentOverlay is PlayerOverlay.SleepTimer) onOverlayChange(PlayerOverlay.None) },
        accentColor = dominantColors.accent,
        dominantColors = dominantColors,
        isDarkTheme = isAppInDarkTheme
    )

    if (activeOverlay is PlayerOverlay.Equalizer) {
        EqualizerSheet(
            isVisible = true,
            onDismiss = { onOverlayChange(PlayerOverlay.None) },
            dominantColor = dominantColors.accent,
            initialEnabled = equalizerEnabled,
            initialBands = equalizerBandLevels,
            initialPreamp = equalizerPreampLevelMb / 100f,
            initialBassBoost = if (bassBoostEnabled) bassBoostStrength / 1000f else 0f,
            initialVirtualizer = if (virtualizerEnabled) virtualizerStrength / 1000f else 0f,
            onEnabledChange = { enabled ->
                equalizerEnabled = enabled
                playerConnection?.setEqualizerEnabled(enabled)
            },
            onBandChange = { index, gain ->
                val nextLevels = equalizerBandLevels.copyOf()
                if (index in nextLevels.indices) {
                    nextLevels[index] = gain
                    val nextBands = nextLevels.toEqualizerBands()
                    storedEqualizerBands = encodeEqualizerBands(nextBands)
                    playerConnection?.applyEqualizerBands(nextBands.withPreamp(equalizerPreampLevelMb / 100f))
                }
            },
            onBandsChange = { levels ->
                val nextBands = levels.toEqualizerBands()
                storedEqualizerBands = encodeEqualizerBands(nextBands)
                playerConnection?.applyEqualizerBands(nextBands.withPreamp(equalizerPreampLevelMb / 100f))
            },
            onPreampChange = { preamp ->
                equalizerPreampLevelMb = (preamp * 100).toInt()
                playerConnection?.applyEqualizerBands(equalizerBands.withPreamp(preamp))
            },
            onBassBoostChange = { value ->
                bassBoostStrength = (value.coerceIn(0f, 1f) * 1000).toInt()
                bassBoostEnabled = value > 0f
            },
            onVirtualizerChange = { value ->
                virtualizerStrength = (value.coerceIn(0f, 1f) * 1000).toInt()
                virtualizerEnabled = value > 0f
            },
            onReset = {
                storedEqualizerBands = encodeEqualizerBands(EqualizerPresets.FLAT.bands)
                equalizerPreampLevelMb = 0
                bassBoostStrength = 0
                bassBoostEnabled = false
                virtualizerStrength = 0
                virtualizerEnabled = false
                playerConnection?.applyEqualizerBands(EqualizerPresets.FLAT.bands)
            },
            onAIEqualizerClick = actions.onShowAIEqualizer,
            dominantColors = dominantColors,
            isDarkTheme = isAppInDarkTheme
        )
    }

    PlaybackSpeedSheet(
        isVisible = activeOverlay is PlayerOverlay.PlaybackSpeed,
        currentSpeed = playerState.playbackSpeed,
        currentPitch = playerState.pitch,
        onDismiss = { if (currentOverlay is PlayerOverlay.PlaybackSpeed) onOverlayChange(PlayerOverlay.None) },
        onApply = { speed, pitch ->
            playerConnection?.player?.playbackParameters = androidx.media3.common.PlaybackParameters(speed, pitch)
            actions.onSetPlaybackParameters(speed, pitch)
        },
        dominantColors = dominantColors,
        isDarkTheme = isAppInDarkTheme
    )

    OutputDeviceSheet(
        isVisible = activeOverlay is PlayerOverlay.OutputDevice,
        devices = playerState.availableDevices,
        onDeviceSelected = { actions.onSwitchDevice(it) },
        onDismiss = { if (currentOverlay is PlayerOverlay.OutputDevice) onOverlayChange(PlayerOverlay.None) },
        onRefreshDevices = { actions.onRefreshDevices() },
        accentColor = dominantColors.accent,
        dominantColors = dominantColors,
        isDarkTheme = isAppInDarkTheme
    )

    AddToPlaylistSheet(
        songs = playlistMgmtState.selectedSongs,
        isVisible = playlistMgmtState.showAddToPlaylistSheet,
        playlists = playlistMgmtState.userPlaylists,
        isLoading = playlistMgmtState.isLoadingPlaylists,
        onDismiss = playlistViewModel::hideAddToPlaylistSheet,
        onAddToPlaylist = playlistViewModel::addSongsToPlaylist,
        onCreateNewPlaylist = {
            playlistViewModel.hideAddToPlaylistSheet()
            playlistViewModel.showCreatePlaylistDialog()
        }
    )

    CreatePlaylistDialog(
        isVisible = playlistMgmtState.showCreatePlaylistDialog,
        isCreating = playlistMgmtState.isCreatingPlaylist,
        onDismiss = playlistViewModel::hideCreatePlaylistDialog,
        onCreate = playlistViewModel::createPlaylist
    )

    LaunchedEffect(playlistMgmtState.successMessage, playlistMgmtState.errorMessage) {
        val message = playlistMgmtState.successMessage ?: playlistMgmtState.errorMessage
        if (!message.isNullOrBlank()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            playlistViewModel.clearMessages()
        }
    }
}

private fun MediaItem.toQueueSong(): Song {
    val omniMetadata = metadata
    if (omniMetadata != null) {
        return omniMetadata.toPresentationSong()
    }

    val fallbackTitle = mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: mediaId
    val fallbackArtist = mediaMetadata.artist?.toString()
        ?: mediaMetadata.subtitle?.toString()
        ?: ""
    return Song(
        id = mediaId,
        title = fallbackTitle,
        artist = fallbackArtist,
        album = mediaMetadata.albumTitle?.toString().orEmpty(),
        thumbnailUrl = mediaMetadata.artworkUri?.toString(),
        isVideo = mediaMetadata.extras?.getBoolean(com.omnitune.app.extensions.ExtraIsMusicVideo, false) == true,
    )
}

private fun MediaMetadata.toPresentationSong(): Song =
    Song(
        id = id,
        title = title,
        artist = artists.joinToString(", ") { it.name },
        album = album?.title.orEmpty(),
        duration = duration.takeIf { it > 0 }?.toLong()?.times(1000L) ?: 0L,
        thumbnailUrl = thumbnailUrl,
        artistId = artists.firstOrNull()?.id,
        setVideoId = setVideoId,
        isVideo = isVideo,
    )

private fun Song.toPlaybackMediaItem(): MediaItem =
    toMediaMetadata().toMediaItem()

private fun List<Song>.toPlaybackMediaItems(): List<MediaItem> =
    map { it.toPlaybackMediaItem() }

private fun PlayerScreenState.selectedRelatedSongs(): List<Song> =
    selectedRelatedIndices
        .sorted()
        .mapNotNull { index -> relatedSongs.getOrNull(index) }

private fun FloatArray.toEqualizerBands(): List<EqualizerBand> =
    EqualizerPresets.FREQUENCIES.mapIndexed { index, frequency ->
        EqualizerBand(
            centerFrequencyHz = frequency,
            gainDb = getOrNull(index)?.coerceIn(-15f, 15f) ?: 0f
        )
    }

private fun PlayerConnection.applySleepTimer(option: SleepTimerOption, customMinutes: Int?) {
    when (option) {
        SleepTimerOption.OFF -> cancelSleepTimer()
        SleepTimerOption.CUSTOM -> customMinutes
            ?.takeIf { it > 0 }
            ?.let { minutes -> startSleepTimer(minutes * 60_000L) }
        SleepTimerOption.END_OF_SONG -> startSleepTimer(1_000L, stopAtEndOfSong = true)
        SleepTimerOption.FADE_OUT_GENTLE -> startFadeOutSleepTimer(120_000L)
        SleepTimerOption.FADE_OUT_FAST -> startFadeOutSleepTimer(60_000L)
        else -> if (option.minutes > 0) {
            startSleepTimer(option.minutes * 60_000L)
        }
    }
}
