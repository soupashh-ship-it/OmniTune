/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player

import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.toArgb
import coil3.imageLoader
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
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
import com.omnitune.app.ui.theme.LocalOmniAccents
import com.omnitune.app.ui.theme.OmniMotion
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.ui.theme.omniPressScale
import com.omnitune.app.ui.theme.omniSoftBorder
import com.omnitune.app.utils.formatDurationMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import timber.log.Timber

private const val ARTWORK_REQUEST_SIZE = 800

@Composable
fun PlayerScreen(
    playerConnection: PlayerConnection?,
    onDismiss: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onNavigateToListenTogether: () -> Unit = {},
    onNavigateToAlbum: ((String) -> Unit)? = null,
    onNavigateToArtist: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val mediaMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsState(initial = null)
    val isPlaying by (playerConnection?.isPlaying ?: flowOf(false)).collectAsState(initial = false)
    val playbackState by (playerConnection?.playbackState ?: flowOf(Player.STATE_IDLE)).collectAsState(initial = Player.STATE_IDLE)
    val shuffleEnabled by (playerConnection?.shuffleModeEnabled ?: flowOf(false)).collectAsState(initial = false)
    val repeatMode by (playerConnection?.repeatMode ?: flowOf(REPEAT_MODE_OFF)).collectAsState(initial = REPEAT_MODE_OFF)
    val isSeeking = remember { mutableFloatStateOf(-1f) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showArtistSelectionDialog by remember { mutableStateOf(false) }
    val sleepTimerRunning by (playerConnection?.sleepTimerRunning ?: flowOf(false)).collectAsState(initial = false)
    val libraryViewModel: com.omnitune.app.ui.screens.LibraryViewModel = hiltViewModel()
    val playlists by libraryViewModel.playlists.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val gradientState = com.omnitune.app.ui.player.rememberPlayerGradient(
        thumbnailUrl = mediaMetadata?.thumbnailUrl,
        videoId = mediaMetadata?.id,
    )
    val dynamicAccent = gradientState.accentGlow



    Box(modifier = modifier.fillMaxSize()) {
        // Background is now handled globally by OmniShell

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
                        .padding(top = verticalPadding, bottom = OmniSpacing.hero),
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
                        accentColor = dynamicAccent,
                    )
                    Spacer(modifier = Modifier.height(largeGap))
                    MetadataBlock(
                        playerConnection = playerConnection,
                        onShare = {
                            showOptionsSheet = false
                            val videoId = mediaMetadata?.id
                            val titleText = mediaMetadata?.title
                            if (!videoId.isNullOrBlank()) {
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=$videoId")
                                    if (titleText != null) putExtra(android.content.Intent.EXTRA_SUBJECT, titleText)
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share via"))
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(largeGap))
                    PlayerSeekBar(
                        playerConnection = playerConnection,
                        isSeeking = isSeeking,
                        accentColor = dynamicAccent,
                    )
                    Spacer(modifier = Modifier.height(largeGap))
                    PlayerControlRow(
                        isPlaying = isPlaying,
                        playbackState = playbackState,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        playerConnection = playerConnection,
                        accentColor = dynamicAccent,
                    )
                    Spacer(modifier = Modifier.height(largeGap))
                    PlayerActionsRow(
                        playerConnection = playerConnection,
                        onOpenQueue = onOpenQueue,
                        onShowSleepTimer = { showSleepTimerDialog = true },
                        onShowOptions = { showOptionsSheet = true },
                        accentColor = dynamicAccent,
                    )
                }
            }
        }
    val context = androidx.compose.ui.platform.LocalContext.current
    if (showOptionsSheet) {
        PlayerOptionsBottomSheet(
            playerConnection = playerConnection,
            onDismissRequest = { showOptionsSheet = false },
            onNavigateToRadio = {
                showOptionsSheet = false
                playerConnection?.startRadioSeamlessly()
                Toast.makeText(context, "Starting radio...", Toast.LENGTH_SHORT).show()
            },
            onAddToPlaylist = {
                showOptionsSheet = false
                showAddToPlaylistDialog = true
            },
            onCopyLink = {
                showOptionsSheet = false
                val videoId = mediaMetadata?.id
                if (!videoId.isNullOrBlank()) {
                    val clip = android.content.ClipData.newPlainText("OmniTune link", "https://music.youtube.com/watch?v=$videoId")
                    (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, "Link copied", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onListenTogether = {
                showOptionsSheet = false
                onNavigateToListenTogether()
            },
            onNavigateToAlbum = onNavigateToAlbum?.let { navigate -> { showOptionsSheet = false; val id = mediaMetadata?.album?.id; if (id != null) navigate(id) } },
            onNavigateToArtist = onNavigateToArtist?.let { navigate ->
                {
                    val artists = mediaMetadata?.artists
                    if (artists != null && artists.size > 1) {
                        showArtistSelectionDialog = true
                    } else {
                        showOptionsSheet = false
                        val id = artists?.firstOrNull()?.id
                        if (id != null) navigate(id)
                    }
                }
            },
            onShare = {
                showOptionsSheet = false
                val videoId = mediaMetadata?.id
                val title = mediaMetadata?.title
                if (!videoId.isNullOrBlank()) {
                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=$videoId")
                        if (title != null) putExtra(android.content.Intent.EXTRA_SUBJECT, title)
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share via"))
                }
            },
            onSleepTimer = {
                showOptionsSheet = false
                showSleepTimerDialog = true
            },
            onOpenQueue = {
                showOptionsSheet = false
                onOpenQueue()
            },
        )
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
    if (showAddToPlaylistDialog) {
        com.omnitune.app.ui.component.AddToPlaylistDialog(
            playlists = playlists,
            onDismissRequest = { showAddToPlaylistDialog = false },
            onPlaylistSelected = { playlist ->
                val meta = mediaMetadata
                if (meta != null) {
                    libraryViewModel.ensureSongExists(meta)
                    scope.launch {
                        val added = libraryViewModel.addToPlaylist(playlist, meta.id)
                        if (!added) {
                            Toast.makeText(context, "Already in playlist", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                showAddToPlaylistDialog = false
            },
            onCreatePlaylist = { name ->
                val meta = mediaMetadata
                if (meta != null) {
                    libraryViewModel.ensureSongExists(meta)
                    libraryViewModel.createPlaylist(name, meta.id)
                    Toast.makeText(context, "Playlist created", Toast.LENGTH_SHORT).show()
                }
                showAddToPlaylistDialog = false
            },
        )
    }

    if (showArtistSelectionDialog) {
        val artists = mediaMetadata?.artists ?: emptyList()
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showArtistSelectionDialog = false },
            containerColor = OmniColors.SurfaceRaised,
            titleContentColor = OmniColors.TextPrimary,
            title = { Text("Select Artist", fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn {
                    items(artists.size) { index ->
                        val artist = artists[index]
                        Text(
                            text = artist.name,
                            color = OmniColors.TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    onClick = {
                                        showArtistSelectionDialog = false
                                        showOptionsSheet = false
                                        artist.id?.let { onNavigateToArtist?.invoke(it) }
                                    }
                                )
                                .padding(vertical = OmniSpacing.medium)
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showArtistSelectionDialog = false }) {
                    Text("Cancel", color = OmniColors.TextSecondary)
                }
            }
        )
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
    accentColor: Color = LocalOmniAccents.current.primary,
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
                spotColor = accentColor.copy(alpha = glowAlpha),
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
                            accentColor.copy(alpha = 0.10f),
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
                        color = LocalOmniAccents.current.primary,
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
                            color = LocalOmniAccents.current.primary,
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
private fun MetadataBlock(
    playerConnection: PlayerConnection?,
    onShare: () -> Unit
) {
    val mediaMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsState(initial = null)
    val currentSong by (playerConnection?.currentSong ?: flowOf(null)).collectAsState(initial = null)
    val liked = currentSong?.song?.liked == true || mediaMetadata?.liked == true
    val title = mediaMetadata?.title?.takeIf { it.isNotBlank() } ?: "No track"
    val artist = mediaMetadata
        ?.artists
        ?.joinToString(", ") { it.name }
        ?.takeIf { it.isNotBlank() }
        ?: "Unknown artist"
    val album = mediaMetadata?.album?.title?.takeIf { it.isNotBlank() }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AnimatedContent(
                targetState = artist + (if (album != null) " • $album" else ""),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "player_artist",
            ) { currentArtist ->
                Text(
                    text = currentArtist,
                    style = MaterialTheme.typography.titleMedium,
                    color = OmniColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        IconButton(onClick = onShare) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = "Share",
                tint = OmniColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(onClick = { playerConnection?.toggleLike() }) {
            Icon(
                painter = painterResource(if (liked) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                contentDescription = if (liked) "Unlike" else "Like",
                tint = if (liked) OmniColors.Hot else OmniColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun PlayerSeekBar(
    playerConnection: PlayerConnection?,
    isSeeking: androidx.compose.runtime.MutableFloatState,
    accentColor: Color = LocalOmniAccents.current.primary,
) {
    val isPlaying by (playerConnection?.isPlaying ?: flowOf(false)).collectAsState(initial = false)
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
        me.saket.squiggles.SquigglySlider(
            value = if (isSeeking.floatValue >= 0f) isSeeking.floatValue else progress,
            onValueChange = { isSeeking.floatValue = it },
            onValueChangeFinished = {
                if (playerConnection != null && duration > 0L) {
                    playerConnection.seekTo((isSeeking.floatValue * duration).toLong())
                }
                isSeeking.floatValue = -1f
            },
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = OmniColors.OmniGlassStrong,
            ),
            squigglesSpec = me.saket.squiggles.SquigglySlider.SquigglesSpec(
                amplitude = if (isPlaying) 2.dp else 0.dp,
                strokeWidth = 6.dp
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
    accentColor: Color = LocalOmniAccents.current.primary,
) {
    val canSkipPrevious by (playerConnection?.canSkipPrevious ?: flowOf(false)).collectAsState(initial = false)
    val canSkipNext by (playerConnection?.canSkipNext ?: flowOf(false)).collectAsState(initial = false)
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Shuffle Button (Icon only)
        IconButton(
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
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shuffle),
                contentDescription = if (shuffleEnabled) "Shuffle on" else "Shuffle off",
                tint = OmniColors.TextPrimary.copy(alpha = if (shuffleEnabled) 1f else 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Previous Button (Boxed)
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(OmniColors.TextPrimary.copy(alpha = 0.08f))
                .clickable(enabled = canSkipPrevious) { playerConnection?.seekToPrevious() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_previous),
                contentDescription = "Previous",
                tint = OmniColors.TextPrimary.copy(alpha = if (canSkipPrevious) 0.9f else 0.4f),
                modifier = Modifier.size(26.dp)
            )
        }

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
        
        // Next Button (Boxed)
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(OmniColors.TextPrimary.copy(alpha = 0.08f))
                .clickable(enabled = canSkipNext) { playerConnection?.seekToNext() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_next),
                contentDescription = "Next",
                tint = OmniColors.TextPrimary.copy(alpha = if (canSkipNext) 0.9f else 0.4f),
                modifier = Modifier.size(26.dp)
            )
        }

        // Repeat Button (Icon only)
        IconButton(
            onClick = { playerConnection?.toggleRepeatMode() },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(
                    when (repeatMode) {
                        REPEAT_MODE_OFF, REPEAT_MODE_ALL -> R.drawable.ic_repeat
                        REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
                        else -> R.drawable.ic_repeat
                    }
                ),
                contentDescription = "Repeat",
                tint = OmniColors.TextPrimary.copy(alpha = if (repeatMode == REPEAT_MODE_OFF) 0.4f else 1f),
                modifier = Modifier.size(24.dp)
            )
        }
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
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f),
            )
            .clip(CircleShape)
            .background(Color.White)
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
            tint = Color.Black,
            modifier = Modifier.size(36.dp),
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
                    LocalOmniAccents.current.primary.copy(alpha = 0.18f)
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
                active -> LocalOmniAccents.current.secondary
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
    onShowSleepTimer: () -> Unit,
    onShowOptions: () -> Unit = {},
    accentColor: Color = LocalOmniAccents.current.primary,
) {
    var showLyricsSheet by remember { mutableStateOf(false) }

    val sleepTimerRunning by (playerConnection?.sleepTimerRunning ?: flowOf(false)).collectAsState(initial = false)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OmniSpacing.small, vertical = OmniSpacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenQueue)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_list), contentDescription = "Queue", tint = OmniColors.TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Queue", color = OmniColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
        }

        IconButton(onClick = onShowSleepTimer) {
            Icon(painterResource(R.drawable.ic_bedtime), contentDescription = "Sleep timer", tint = if (sleepTimerRunning) LocalOmniAccents.current.primary else OmniColors.TextSecondary)
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showLyricsSheet = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_lyrics), contentDescription = "Lyrics", tint = OmniColors.TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Lyrics", color = OmniColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
        }

        IconButton(onClick = onShowOptions) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "More options", tint = OmniColors.TextSecondary)
        }
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
    activeTint: Color = LocalOmniAccents.current.secondary,
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
                colors = ButtonDefaults.buttonColors(containerColor = LocalOmniAccents.current.primary),
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
                thumbColor = LocalOmniAccents.current.primary,
                activeTrackColor = LocalOmniAccents.current.primary,
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
                                LocalOmniAccents.current.primary.copy(alpha = 0.18f)
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
                    colors = ButtonDefaults.buttonColors(containerColor = LocalOmniAccents.current.primary),
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOptionsBottomSheet(
    playerConnection: PlayerConnection?,
    onDismissRequest: () -> Unit,
    onNavigateToRadio: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onCopyLink: () -> Unit = {},
    onListenTogether: () -> Unit = {},
    onNavigateToAlbum: (() -> Unit)? = null,
    onNavigateToArtist: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onSleepTimer: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
) {
    var showMediaInfoDialog by remember { mutableStateOf(false) }
    val currentFormat by (playerConnection?.currentFormat ?: kotlinx.coroutines.flow.flowOf(null)).collectAsState(initial = null)
    val currentMetadata by (playerConnection?.mediaMetadata ?: kotlinx.coroutines.flow.flowOf(null)).collectAsState(initial = null)
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = OmniColors.OmniBackgroundElevated,
        shape = OmniShapes.ExtraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OmniSpacing.section, vertical = OmniSpacing.medium)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentMetadata?.thumbnailUrl != null) {
                    coil3.compose.AsyncImage(
                        model = currentMetadata?.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(OmniShapes.Medium),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(OmniSpacing.medium))
                Column {
                    Text("Now Playing", color = OmniColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = currentMetadata?.title ?: "No track",
                        color = OmniColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentMetadata?.artists?.joinToString(", ") { it.name } ?: "Unknown artist",
                        color = OmniColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(OmniSpacing.large))

            // Volume slider
            var volume by remember(playerConnection) { mutableFloatStateOf(playerConnection?.player?.volume ?: 1f) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(
                        if (volume == 0f) R.drawable.ic_volume_off else R.drawable.ic_volume_up
                    ),
                    contentDescription = "Volume",
                    tint = OmniColors.TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(OmniSpacing.small))
                me.saket.squiggles.SquigglySlider(
                    value = volume,
                    onValueChange = {
                        volume = it
                        playerConnection?.player?.setVolume(it)
                    },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = LocalOmniAccents.current.primary,
                        activeTrackColor = LocalOmniAccents.current.primary,
                        inactiveTrackColor = OmniColors.SurfaceRaised,
                    ),
                    squigglesSpec = me.saket.squiggles.SquigglySlider.SquigglesSpec(
                        amplitude = 2.dp,
                        strokeWidth = 6.dp
                    ),
                )
                Spacer(modifier = Modifier.width(OmniSpacing.small))
                Text(
                    text = "${(volume * 100).toInt()}%",
                    color = OmniColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End,
                )
            }

            Spacer(modifier = Modifier.height(OmniSpacing.medium))

            // Speed slider
            var speed by remember(playerConnection) { mutableFloatStateOf(playerConnection?.player?.playbackParameters?.speed ?: 1f) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow),
                    contentDescription = "Speed",
                    tint = OmniColors.TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(OmniSpacing.small))
                Slider(
                    value = speed,
                    onValueChange = {
                        speed = it
                        playerConnection?.player?.let { player ->
                            player.playbackParameters = player.playbackParameters.withSpeed(it)
                        }
                    },
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = LocalOmniAccents.current.primary,
                        activeTrackColor = LocalOmniAccents.current.primary,
                        inactiveTrackColor = OmniColors.SurfaceRaised,
                    )
                )
                Spacer(modifier = Modifier.width(OmniSpacing.small))
                Text(
                    text = String.format("%.1fx", speed),
                    color = OmniColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End,
                )
            }

            // Pitch slider
            var pitch by remember(playerConnection) { mutableFloatStateOf(playerConnection?.player?.playbackParameters?.pitch ?: 1f) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings), // fallback icon for pitch
                    contentDescription = "Pitch",
                    tint = OmniColors.TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(OmniSpacing.small))
                Slider(
                    value = pitch,
                    onValueChange = {
                        pitch = it
                        playerConnection?.player?.let { player ->
                            // using an extension or recreating PlaybackParameters if withPitch doesn't exist
                            player.playbackParameters = androidx.media3.common.PlaybackParameters(player.playbackParameters.speed, it)
                        }
                    },
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = LocalOmniAccents.current.primary,
                        activeTrackColor = LocalOmniAccents.current.primary,
                        inactiveTrackColor = OmniColors.SurfaceRaised,
                    )
                )
                Spacer(modifier = Modifier.width(OmniSpacing.small))
                Text(
                    text = String.format("%.1fx", pitch),
                    color = OmniColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End,
                )
            }

            Spacer(modifier = Modifier.height(OmniSpacing.medium))

            val context = LocalContext.current
            val downloadsViewModel: DownloadsViewModel = hiltViewModel()
            val optionItems = buildList {
                add(PlayerOption(R.drawable.ic_insights, "Start radio", onNavigateToRadio))
                add(PlayerOption(R.drawable.ic_list, "Add to playlist", onAddToPlaylist))
                add(PlayerOption(R.drawable.ic_share, "Copy link", onCopyLink))
                add(PlayerOption(R.drawable.ic_info, "Media info") { showMediaInfoDialog = true })
                if (onNavigateToAlbum != null) add(PlayerOption(R.drawable.ic_album, "Go to album", onNavigateToAlbum))
                if (onNavigateToArtist != null) add(PlayerOption(R.drawable.ic_artist, "Go to artist", onNavigateToArtist))
                if (onShare != null) add(PlayerOption(R.drawable.ic_share, "Share", onShare))
                add(PlayerOption(R.drawable.ic_bedtime, "Sleep timer", onSleepTimer))
                add(PlayerOption(R.drawable.ic_list, "Queue", onOpenQueue))
                add(PlayerOption(R.drawable.ic_share, "Listen together", onListenTogether))
                add(
                    PlayerOption(R.drawable.ic_download, "Download") {
                        val videoId = currentMetadata?.id
                        val title = currentMetadata?.title
                        if (!videoId.isNullOrBlank() && !title.isNullOrBlank()) {
                            downloadsViewModel.startDownload(videoId, title, null) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            OptionGrid(options = optionItems)

            if (showMediaInfoDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showMediaInfoDialog = false },
                    containerColor = OmniColors.SurfaceRaised,
                    titleContentColor = OmniColors.TextPrimary,
                    textContentColor = OmniColors.TextSecondary,
                    title = { Text("Media Information", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Codec: ${currentFormat?.codecs ?: "Unknown"}")
                            Text("Bitrate: ${currentFormat?.bitrate?.let { "${it / 1000} kbps" } ?: "Unknown"}")
                            Text("Sample Rate: ${currentFormat?.sampleRate?.let { "${it / 1000} kHz" } ?: "Unknown"}")
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { showMediaInfoDialog = false }) {
                            Text("Close", color = LocalOmniAccents.current.primary)
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private data class PlayerOption(
    val icon: Int,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
private fun OptionGrid(options: List<PlayerOption>) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
        options.chunked(4).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                rowOptions.forEach { option ->
                    OptionButton(
                        icon = option.icon,
                        label = option.label,
                        onClick = option.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - rowOptions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OptionButton(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(OmniShapes.Medium)
            .clickable(onClick = onClick)
            .padding(vertical = OmniSpacing.compact, horizontal = OmniSpacing.micro)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(OmniShapes.Large)
                .background(OmniColors.SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = OmniColors.TextPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(OmniSpacing.compact))
        Text(
            text = label,
            color = OmniColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}



