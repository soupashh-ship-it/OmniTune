/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player

import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.omnitune.app.R
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.ui.component.OmniTuneLoader
import com.omnitune.app.ui.screens.DownloadsViewModel
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniMotion
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.ui.theme.omniPressScale
import com.omnitune.app.ui.theme.omniSoftBorder
import com.omnitune.app.utils.formatDurationMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber

private const val ARTWORK_REQUEST_SIZE = 800

@Composable
fun PlayerScreen(
    playerConnection: PlayerConnection?,
    onDismiss: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val mediaMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsState(initial = null)
    val isPlaying by (playerConnection?.isPlaying ?: flowOf(false)).collectAsState(initial = false)
    val playbackState by (playerConnection?.playbackState ?: flowOf(Player.STATE_IDLE)).collectAsState(initial = Player.STATE_IDLE)
    val shuffleEnabled by (playerConnection?.shuffleModeEnabled ?: flowOf(false)).collectAsState(initial = false)
    val repeatMode by (playerConnection?.repeatMode ?: flowOf(REPEAT_MODE_OFF)).collectAsState(initial = REPEAT_MODE_OFF)
    val isSeeking = remember { mutableFloatStateOf(-1f) }

    val gradientState = rememberPlayerGradient(
        thumbnailUrl = mediaMetadata?.thumbnailUrl,
        videoId = mediaMetadata?.id,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientState.backgroundBrush),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            gradientState.accentGlow,
                            Color.Transparent,
                        )
                    )
                ),
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            val compactPlayer = maxHeight < 900.dp
            val verticalPadding = if (compactPlayer) OmniSpacing.compact else OmniSpacing.medium
            val mediumGap = if (compactPlayer) OmniSpacing.compact else OmniSpacing.medium
            val largeGap = if (compactPlayer) OmniSpacing.small else OmniSpacing.large
            val artworkHeight = if (compactPlayer) 305.dp else 330.dp
            val artworkWidthFraction = if (compactPlayer) 0.84f else 0.86f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OmniSpacing.section)
                        .padding(top = verticalPadding, bottom = verticalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PlayerTopBar(
                        onDismiss = onDismiss,
                        onOpenQueue = onOpenQueue,
                        hasQueue = playerConnection != null,
                    )
                    Spacer(modifier = Modifier.height(mediumGap))
                    ArtworkHero(
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        height = artworkHeight,
                        widthFraction = artworkWidthFraction,
                    )
                    Spacer(modifier = Modifier.height(largeGap))
                    MetadataBlock(mediaMetadata = mediaMetadata)
                    Spacer(modifier = Modifier.height(largeGap))
                    PlayerSeekBar(
                        playerConnection = playerConnection,
                        isSeeking = isSeeking,
                    )
                    Spacer(modifier = Modifier.height(largeGap))
                    PlayerControlRow(
                        isPlaying = isPlaying,
                        playbackState = playbackState,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        playerConnection = playerConnection,
                    )
                    Spacer(modifier = Modifier.height(mediumGap))
                    PlayerActionsRow(
                        playerConnection = playerConnection,
                        onOpenQueue = onOpenQueue,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    hasQueue: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(
            icon = R.drawable.ic_arrow_back,
            contentDescription = "Navigate up",
            onClick = onDismiss,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = OmniSpacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Now playing",
                style = OmniTextStyles.caption,
                color = OmniColors.TextTertiary,
                maxLines = 1,
            )
            Text(
                text = "OmniTune",
                style = MaterialTheme.typography.labelLarge,
                color = OmniColors.TextSecondary,
                maxLines = 1,
            )
        }
        GlassIconButton(
            icon = R.drawable.ic_list,
            contentDescription = "Queue",
            onClick = onOpenQueue,
            enabled = hasQueue,
        )
    }
}

private fun buildArtworkCandidates(videoId: String?, thumbnailUrl: String?): List<String> {
    val candidates = mutableListOf<String>()
    if (videoId != null && videoId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
        candidates.add("https://i.ytimg.com/vi/$videoId/maxresdefault.jpg")
        candidates.add("https://i.ytimg.com/vi/$videoId/sddefault.jpg")
    }
    if (thumbnailUrl != null) candidates.add(thumbnailUrl)
    return candidates
}

@Composable
private fun ArtworkHero(
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    height: Dp,
    widthFraction: Float,
) {
    val context = LocalContext.current
    val videoId = mediaMetadata?.id
    val thumbnailUrl = mediaMetadata?.thumbnailUrl
    val candidates = remember(videoId, thumbnailUrl) { buildArtworkCandidates(videoId, thumbnailUrl) }
    var candidateIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(videoId, thumbnailUrl) {
        candidateIndex = 0
    }

    val currentUrl = candidates.getOrNull(candidateIndex)
    val imageRequest = remember(currentUrl, candidateIndex) {
        currentUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .size(ARTWORK_REQUEST_SIZE, ARTWORK_REQUEST_SIZE)
                .memoryCacheKey("full-artwork:${mediaMetadata?.id}:$url")
                .build()
        }
    }

    val glowAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.22f else 0.10f,
        animationSpec = OmniMotion.gentleSpring(),
        label = "artwork_glow",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .shadow(
                elevation = if (isPlaying) 26.dp else 18.dp,
                shape = OmniShapes.ArtworkLarge,
                ambientColor = Color.Black.copy(alpha = 0.42f),
                spotColor = OmniColors.OmniAccentGlow.copy(alpha = glowAlpha),
            )
            .clip(OmniShapes.ArtworkLarge)
            .omniSoftBorder(
                shape = OmniShapes.ArtworkLarge,
                color = OmniColors.OmniGlassBorderStrong.copy(alpha = 0.18f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            OmniColors.OmniAccentPrimary.copy(alpha = 0.10f),
                            OmniColors.OmniGlassStrong,
                            OmniColors.OmniBackgroundElevated,
                        )
                    )
                )
        )

        // Foreground artwork fills the 16:9 card
        if (imageRequest != null) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    OmniTuneLoader(
                        modifier = Modifier.size(48.dp),
                        color = OmniColors.ActivePlayback,
                        size = 48.dp,
                    )
                },
                error = {
                    LaunchedEffect(candidateIndex) {
                        if (candidateIndex < candidates.size - 1) {
                            candidateIndex += 1
                            Timber.tag("OmniTuneArtwork").w(
                                "Full player artwork candidate %d/%d failed, trying next",
                                candidateIndex + 1, candidates.size
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        OmniTuneLoader(
                            modifier = Modifier.size(32.dp),
                            color = OmniColors.ActivePlayback,
                            size = 32.dp,
                        )
                    }
                },
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_album),
                    contentDescription = null,
                    tint = OmniColors.TextTertiary,
                    modifier = Modifier.size(58.dp),
                )
                Text(
                    text = "No artwork",
                    style = OmniTextStyles.caption,
                    color = OmniColors.TextTertiary,
                )
            }
        }
    }
}
@Composable
private fun MetadataBlock(mediaMetadata: MediaMetadata?) {
    val title = mediaMetadata?.title?.takeIf { it.isNotBlank() } ?: "No track"
    val artist = mediaMetadata
        ?.artists
        ?.joinToString(", ") { it.name }
        ?.takeIf { it.isNotBlank() }
        ?: "Unknown artist"
    val album = mediaMetadata?.album?.title?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
    ) {
        AnimatedContent(
            targetState = title,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "player_title",
        ) { currentTitle ->
            Text(
                text = currentTitle,
                style = OmniTextStyles.screenTitle,
                color = OmniColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AnimatedContent(
            targetState = artist,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "player_artist",
        ) { currentArtist ->
            Text(
                text = currentArtist,
                style = MaterialTheme.typography.titleMedium,
                color = OmniColors.OmniAccentSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (album != null) {
            Text(
                text = album,
                style = OmniTextStyles.caption,
                color = OmniColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlayerSeekBar(
    playerConnection: PlayerConnection?,
    isSeeking: androidx.compose.runtime.MutableFloatState,
) {
    val playbackState by (playerConnection?.playbackState ?: flowOf(Player.STATE_IDLE)).collectAsState(initial = Player.STATE_IDLE)
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(playerConnection, playbackState) {
        while (true) {
            val pc = playerConnection
            if (pc != null) {
                val dur = pc.duration
                if (playbackState == Player.STATE_ENDED && dur > 0) {
                    currentPosition = dur
                    duration = dur
                } else {
                    duration = if (dur > 0) dur else 0L
                    if (isSeeking.floatValue < 0f) {
                        currentPosition = pc.currentPosition
                    }
                }
            } else {
                currentPosition = 0L
                duration = 0L
            }
            delay(250)
        }
    }

    val progressTarget = if (duration > 0L) {
        (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "player_progress",
    )
    val displayPosition = if (isSeeking.floatValue >= 0f && duration > 0L) {
        (isSeeking.floatValue * duration).toLong()
    } else {
        currentPosition
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Large)
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.small)
            .semantics { contentDescription = "Playback progress" },
    ) {
        Slider(
            value = if (isSeeking.floatValue >= 0f) isSeeking.floatValue else progress,
            onValueChange = { isSeeking.floatValue = it },
            onValueChangeFinished = {
                if (playerConnection != null && duration > 0L) {
                    playerConnection.seekTo((isSeeking.floatValue * duration).toLong())
                }
                isSeeking.floatValue = -1f
            },
            colors = SliderDefaults.colors(
                thumbColor = OmniColors.ActivePlayback,
                activeTrackColor = OmniColors.ActivePlayback,
                inactiveTrackColor = OmniColors.OmniGlassStrong,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    stateDescription = "At ${formatDurationMs(displayPosition)} of ${formatDurationMs(duration)}"
                },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDurationMs(displayPosition),
                style = OmniTextStyles.caption,
                color = OmniColors.TextTertiary,
                modifier = Modifier.clearAndSetSemantics {},
            )
            Text(
                text = formatDurationMs(duration),
                style = OmniTextStyles.caption,
                color = OmniColors.TextTertiary,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun PlayerControlRow(
    isPlaying: Boolean,
    playbackState: Int,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    playerConnection: PlayerConnection?,
) {
    val canSkipPrevious by (playerConnection?.canSkipPrevious ?: flowOf(false)).collectAsState(initial = false)
    val canSkipNext by (playerConnection?.canSkipNext ?: flowOf(false)).collectAsState(initial = false)
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerIconButton(
            icon = R.drawable.ic_shuffle,
            contentDescription = if (shuffleEnabled) "Shuffle on" else "Shuffle off",
            active = shuffleEnabled,
            onClick = {
                val pc = playerConnection
                if (pc != null) {
                    if (pc.mediaItemCount <= 1) {
                        Toast.makeText(context, "Shuffle requires more than one track", Toast.LENGTH_SHORT).show()
                    } else {
                        pc.setShuffleModeEnabled(!shuffleEnabled)
                    }
                }
            },
        )
        PlayerIconButton(
            icon = R.drawable.ic_skip_previous,
            contentDescription = "Previous",
            enabled = canSkipPrevious,
            size = 52.dp,
            iconSize = 28.dp,
            onClick = { playerConnection?.seekToPrevious() },
        )
        PlayPauseButton(
            isPlaying = isPlaying,
            playbackState = playbackState,
            onClick = {
                val pc = playerConnection ?: return@PlayPauseButton
                if (playbackState == Player.STATE_ENDED || !isPlaying) {
                    pc.playOrResolveCurrent()
                } else {
                    pc.pause()
                }
            },
        )
        PlayerIconButton(
            icon = R.drawable.ic_skip_next,
            contentDescription = "Next",
            enabled = canSkipNext,
            size = 52.dp,
            iconSize = 28.dp,
            onClick = { playerConnection?.seekToNext() },
        )
        PlayerIconButton(
            icon = if (repeatMode == REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
            contentDescription = when (repeatMode) {
                REPEAT_MODE_OFF -> "Repeat off"
                REPEAT_MODE_ALL -> "Repeat all"
                REPEAT_MODE_ONE -> "Repeat one"
                else -> "Repeat"
            },
            active = repeatMode != REPEAT_MODE_OFF,
            onClick = { playerConnection?.toggleRepeatMode() },
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    playbackState: Int,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(76.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = OmniColors.OmniAccentGlow.copy(alpha = 0.20f),
                spotColor = OmniColors.OmniAccentGlow.copy(alpha = 0.20f),
            )
            .clip(CircleShape)
            .background(Brush.linearGradient(OmniColors.PrimaryGradientColors))
            .omniPressScale(interactionSource),
    ) {
        Icon(
            painter = painterResource(
                if (playbackState == Player.STATE_BUFFERING) {
                    R.drawable.ic_pause
                } else if (playbackState == Player.STATE_ENDED || !isPlaying) {
                    R.drawable.ic_play_arrow
                } else {
                    R.drawable.ic_pause
                }
            ),
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = OmniColors.TextOnAccent,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun PlayerIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (active) {
                    OmniColors.OmniAccentPrimary.copy(alpha = 0.18f)
                } else {
                    OmniColors.OmniGlassSubtle
                }
            )
            .omniPressScale(interactionSource),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = when {
                !enabled -> OmniColors.TextDisabled
                active -> OmniColors.OmniAccentSecondary
                else -> OmniColors.TextSecondary
            },
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun GlassIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(46.dp)
            .clip(OmniShapes.Medium)
            .background(OmniColors.OmniGlassMedium)
            .omniSoftBorder(OmniShapes.Medium, OmniColors.OmniGlassBorderSubtle)
            .omniPressScale(interactionSource),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (enabled) OmniColors.TextPrimary else OmniColors.TextDisabled,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun PlayerActionsRow(
    playerConnection: PlayerConnection?,
    onOpenQueue: () -> Unit,
) {
    val currentSong by (playerConnection?.currentSong ?: flowOf(null)).collectAsState(initial = null)
    val currentMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsState(initial = null)
    val sleepTimerRunning by (playerConnection?.sleepTimerRunning ?: flowOf(false)).collectAsState(initial = false)
    val liked = currentSong?.song?.liked == true || currentMetadata?.liked == true
    val downloadsViewModel: DownloadsViewModel = hiltViewModel()
    val context = LocalContext.current
    var showEffectsDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OmniSpacing.small, vertical = OmniSpacing.compact),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionButton(
            icon = if (liked) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
            contentDescription = if (liked) "Unlike" else "Like",
            active = liked,
            activeTint = OmniColors.Hot,
            onClick = { playerConnection?.toggleLike() },
        )
        ActionButton(
            icon = R.drawable.ic_download,
            contentDescription = "Download",
            onClick = {
                val song = currentSong?.song
                val metadata = currentMetadata
                val videoId = song?.id ?: metadata?.id
                val title = song?.title ?: metadata?.title
                if (!videoId.isNullOrBlank() && !title.isNullOrBlank()) {
                    val activeUri = playerConnection?.activeUri
                    val resolvedStreamUrl = activeUri?.takeIf {
                        it.startsWith("http://") || it.startsWith("https://")
                    }
                    Timber.d("Download button clicked for %s", videoId)
                    downloadsViewModel.startDownload(videoId, title, resolvedStreamUrl) { _, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Timber.w("Download button clicked without active song")
                    Toast.makeText(context, "No active song to download", Toast.LENGTH_SHORT).show()
                }
            },
        )
        ActionButton(
            icon = R.drawable.ic_settings,
            contentDescription = "Audio Effects",
            onClick = { showEffectsDialog = true },
        )
        ActionButton(
            icon = R.drawable.ic_lyrics,
            contentDescription = "Lyrics",
            onClick = { showLyricsSheet = true },
        )
        ActionButton(
            icon = R.drawable.ic_bedtime,
            contentDescription = if (sleepTimerRunning) "Cancel sleep timer" else "Set sleep timer",
            active = sleepTimerRunning,
            onClick = { showSleepTimerDialog = true },
        )
        ActionButton(
            icon = R.drawable.ic_list,
            contentDescription = "Queue",
            onClick = onOpenQueue,
        )
    }

    if (showEffectsDialog) {
        AudioEffectsDialog(playerConnection = playerConnection, onDismiss = { showEffectsDialog = false })
    }
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false },
            onSet = { minutes, endOfSong ->
                playerConnection?.service?.sleepTimer?.start(
                    durationMs = minutes * 60_000L,
                    stopAtEndOfSong = endOfSong,
                )
                showSleepTimerDialog = false
            },
            onCancel = {
                playerConnection?.service?.sleepTimer?.cancel()
                showSleepTimerDialog = false
            },
            isRunning = sleepTimerRunning,
        )
    }
    if (showLyricsSheet) {
        LyricsBottomSheet(
            playerConnection = playerConnection,
            onDismissRequest = { showLyricsSheet = false }
        )
    }
}

@Composable
private fun ActionButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
    activeTint: Color = OmniColors.OmniAccentSecondary,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (active) {
                    activeTint.copy(alpha = 0.18f)
                } else {
                    OmniColors.OmniBackgroundElevated.copy(alpha = 0.88f)
                }
            ),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (active) activeTint else OmniColors.TextSecondary.copy(alpha = 0.92f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun AudioEffectsDialog(
    playerConnection: PlayerConnection?,
    onDismiss: () -> Unit,
) {
    var tempo by remember { mutableFloatStateOf(playerConnection?.playbackSpeed ?: 1f) }
    var pitch by remember { mutableFloatStateOf(playerConnection?.playbackPitch ?: 1f) }
    var skipSilence by remember { mutableStateOf(playerConnection?.skipSilenceEnabled ?: false) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(OmniShapes.ExtraLarge)
                .background(OmniColors.OmniBackgroundElevated)
                .omniSoftBorder(OmniShapes.ExtraLarge, OmniColors.OmniGlassBorderStrong)
                .padding(OmniSpacing.section),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
        ) {
            Text(
                text = "Audio Effects",
                style = OmniTextStyles.sectionTitle,
                color = OmniColors.TextPrimary,
            )

            EffectSlider(
                label = "Tempo",
                valueText = String.format("%.2fx", tempo),
                value = tempo,
                onValueChange = { tempo = it },
                onValueChangeFinished = { playerConnection?.setPlaybackParameters(tempo, pitch) },
                valueRange = 0.5f..2.0f,
            )

            EffectSlider(
                label = "Pitch",
                valueText = String.format("%.2fx", pitch),
                value = pitch,
                onValueChange = { pitch = it },
                onValueChangeFinished = { playerConnection?.setPlaybackParameters(tempo, pitch) },
                valueRange = 0.5f..2.0f,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Skip Silence",
                    color = OmniColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = skipSilence,
                    onCheckedChange = {
                        skipSilence = it
                        playerConnection?.setSkipSilenceEnabled(it)
                    },
                )
            }

            Button(
                onClick = {
                    val audioSessionId = playerConnection?.audioSessionId ?: 0
                    if (audioSessionId != 0) {
                        try {
                            val intent = android.content.Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No system equalizer found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Audio session not ready", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OmniColors.OmniAccentPrimary),
            ) {
                Text("Open System Equalizer", color = OmniColors.TextOnAccent)
            }
        }
    }
}

@Composable
private fun EffectSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = OmniColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, color = OmniColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = OmniColors.ActivePlayback,
                activeTrackColor = OmniColors.ActivePlayback,
                inactiveTrackColor = OmniColors.OmniGlassStrong,
            ),
        )
    }
}

@Composable
private fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSet: (minutes: Int, endOfSong: Boolean) -> Unit,
    onCancel: () -> Unit,
    isRunning: Boolean,
) {
    var selectedMinutes by remember { mutableStateOf(30) }
    var endOfSong by remember { mutableStateOf(false) }
    val options = listOf(15, 30, 45, 60, 90, 120)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(OmniShapes.ExtraLarge)
                .background(OmniColors.OmniBackgroundElevated)
                .omniSoftBorder(OmniShapes.ExtraLarge, OmniColors.OmniGlassBorderStrong)
                .padding(OmniSpacing.section),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
        ) {
            Text(
                text = "Sleep Timer",
                style = OmniTextStyles.sectionTitle,
                color = OmniColors.TextPrimary,
            )

            options.forEach { minutes ->
                val selected = selectedMinutes == minutes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(OmniShapes.Small)
                        .background(
                            if (selected) {
                                OmniColors.OmniAccentPrimary.copy(alpha = 0.18f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { selectedMinutes = minutes }
                        .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$minutes minutes",
                        color = OmniColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Stop at end of song",
                    color = OmniColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = endOfSong, onCheckedChange = { endOfSong = it })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
                if (isRunning) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OmniColors.Hot),
                    ) {
                        Text("Cancel Timer")
                    }
                }
                Button(
                    onClick = { onSet(selectedMinutes, endOfSong) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = OmniColors.OmniAccentPrimary),
                ) {
                    Text(
                        text = if (isRunning) "Restart" else "Set Timer",
                        color = OmniColors.TextOnAccent,
                    )
                }
            }
        }
    }
}
