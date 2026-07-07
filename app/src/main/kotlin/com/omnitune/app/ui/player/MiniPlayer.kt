/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_BUFFERING
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.omnitune.app.R
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.component.OmniTuneLoader
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniMotion
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.ui.theme.OmniGlassDefaults
import com.omnitune.app.ui.theme.OmniGlassSurface
import com.omnitune.app.ui.theme.omniPressScale
import com.omnitune.app.ui.theme.omniPressScaleBounce
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.roundToInt

private const val ARTWORK_REQUEST_SIZE = 112

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    playerConnection: PlayerConnection? = null,
    onClick: () -> Unit = {},
    onNavigateToAlbum: ((String) -> Unit)? = null,
    onNavigateToArtist: ((String) -> Unit)? = null,
    onShare: ((String, String?) -> Unit)? = null,
    onOpenQueue: (() -> Unit)? = null,
) {
    val isPlaying by (playerConnection?.isPlaying ?: flowOf(false)).collectAsState(initial = false)
    val playbackState by (playerConnection?.playbackState ?: flowOf(Player.STATE_IDLE)).collectAsState(initial = Player.STATE_IDLE)
    val mediaMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsState(initial = null)
    val canSkipNext by (playerConnection?.canSkipNext ?: flowOf(false)).collectAsState(initial = false)
    val canSkipPrevious by (playerConnection?.canSkipPrevious ?: flowOf(false)).collectAsState(initial = false)
    val isLoading = playbackState == STATE_BUFFERING
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    var position by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    val currentMediaId = mediaMetadata?.id

    LaunchedEffect(playerConnection, currentMediaId, isPlaying, playbackState) {
        while (true) {
            val pc = playerConnection
            if (pc != null) {
                val dur = pc.duration
                val pos = pc.currentPosition
                if (playbackState == Player.STATE_ENDED && dur > 0) {
                    duration = dur.toFloat()
                    position = dur.toFloat()
                } else {
                    duration = if (dur > 0) dur.toFloat() else 0f
                    position = if (pos in 0..dur) pos.toFloat() else 0f
                }
            } else {
                duration = 0f
                position = 0f
            }
            delay(if (isPlaying) 250 else 750)
        }
    }

    val progressTarget = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "mini_progress",
    )

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    )
    val swipeSensitivity = 0.73f
    val autoSwipeThreshold = (600 / (1f + exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    val bodyInteraction = remember { MutableInteractionSource() }

    val miniGradient = rememberPlayerGradient(
        thumbnailUrl = mediaMetadata?.thumbnailUrl,
        videoId = mediaMetadata?.id,
    )
    val songPalette = miniGradient.palette
    val miniGlassStyle = OmniGlassDefaults.miniPlayerStyle(
        isDark = true,
        isPureBlack = pureBlack,
    ).copy(
        surfaceTint = songPalette.miniPlayerSurface,
        surfaceAlpha = if (pureBlack) 0.88f else 0.96f,
        overlayColor = songPalette.backgroundSecondary,
        overlayAlpha = if (pureBlack) 0.18f else 0.42f,
        borderColor = songPalette.accent,
        borderAlpha = if (miniGradient.isFromArtwork) 0.16f else 0.05f,
        shadowSpot = songPalette.accent.copy(alpha = if (miniGradient.isFromArtwork) 0.10f else 0.04f),
    )

    OmniGlassSurface(
        shape = OmniShapes.Dock,
        style = miniGlassStyle,
        modifier = modifier
            .fillMaxWidth()
            .height(OmniChrome.MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = OmniSpacing.small),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(playerConnection, canSkipNext, canSkipPrevious, layoutDirection) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragStartTime = System.currentTimeMillis()
                            totalDragDistance = 0f
                        },
                        onDragCancel = {
                            coroutineScope.launch { offsetXAnimatable.animateTo(0f, animationSpec) }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val adjustedDrag = if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                            val canSwipe = (adjustedDrag < 0 && canSkipNext) || (adjustedDrag > 0 && canSkipPrevious)
                            if (canSwipe) {
                                totalDragDistance += kotlin.math.abs(adjustedDrag)
                                coroutineScope.launch {
                                    offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDrag)
                                }
                            }
                        },
                        onDragEnd = {
                            val pc = playerConnection ?: return@detectHorizontalDragGestures
                            val dragDuration = System.currentTimeMillis() - dragStartTime
                            val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                            val offset = offsetXAnimatable.value
                            val shouldSkip = (
                                kotlin.math.abs(offset) > 50f &&
                                    velocity > (swipeSensitivity * -8.25f + 8.5f)
                                ) || kotlin.math.abs(offset) > autoSwipeThreshold

                            if (shouldSkip) {
                                if (offset > 0 && canSkipPrevious) {
                                    pc.seekToPrevious()
                                } else if (offset < 0 && canSkipNext) {
                                    pc.seekToNext()
                                }
                            }
                            coroutineScope.launch { offsetXAnimatable.animateTo(0f, animationSpec) }
                        },
                    )
                },
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val showPreviousControl = canSkipPrevious && maxWidth >= 390.dp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                    .clip(OmniShapes.Dock)
                    .omniPressScale(bodyInteraction)
                    .clickable(
                        interactionSource = bodyInteraction,
                        indication = androidx.compose.material3.ripple(
                            bounded = true,
                            color = songPalette.accent.copy(alpha = 0.12f),
                        ),
                        onClick = onClick,
                    )
                    .padding(horizontal = OmniSpacing.compact, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(OmniChrome.MiniPlayerContentHeight)
                        .padding(horizontal = OmniSpacing.micro),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniArtwork(
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        accentColor = songPalette.accent,
                    )
                    Spacer(modifier = Modifier.width(OmniSpacing.small))
                    MiniMediaInfo(
                        mediaMetadata = mediaMetadata,
                        modifier = Modifier.weight(1f),
                    )
                }

                playerConnection?.let { pc ->
                    Spacer(modifier = Modifier.width(OmniSpacing.micro))
                    if (showPreviousControl) {
                        MiniControlButton(
                            icon = R.drawable.ic_skip_previous,
                            contentDescription = "Previous",
                            onClick = { pc.seekToPrevious() },
                        )
                    }
                    MiniPlayPauseButton(
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        playbackState = playbackState,
                        accentColor = songPalette.accent,
                        onAccent = songPalette.onAccent,
                        onClick = {
                            if (playbackState == Player.STATE_ENDED || !isPlaying) {
                                pc.playOrResolveCurrent()
                            } else {
                                pc.pause()
                            }
                        },
                    )
                    if (canSkipNext) {
                        MiniControlButton(
                            icon = R.drawable.ic_skip_next,
                            contentDescription = "Next",
                            onClick = { pc.seekToNext() },
                        )
                    }
                    Spacer(modifier = Modifier.width(OmniSpacing.micro))
                    Box {
                        MiniControlButton(
                            icon = R.drawable.ic_more_vert,
                            contentDescription = "More options",
                            onClick = { showMenu = true },
                        )
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            mediaMetadata?.let { meta ->
                                DropdownMenuItem(
                                    text = { Text(if (meta.liked) "Unlike" else "Like") },
                                    onClick = {
                                        showMenu = false
                                        pc.toggleLike()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(
                                                if (meta.liked) R.drawable.ic_favorite
                                                else R.drawable.ic_favorite_border
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    },
                                )
                                if (!showPreviousControl && canSkipPrevious) {
                                    DropdownMenuItem(
                                        text = { Text("Previous") },
                                        onClick = {
                                            showMenu = false
                                            pc.seekToPrevious()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_skip_previous),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        },
                                    )
                                }
                                if (meta.album != null) {
                                    DropdownMenuItem(
                                        text = { Text("Go to album") },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToAlbum?.invoke(meta.album.id)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_album),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        },
                                    )
                                }
                                if (meta.artists.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Go to artist") },
                                        onClick = {
                                            showMenu = false
                                            meta.artists.firstOrNull()?.id?.let { onNavigateToArtist?.invoke(it) }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_artist),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    onClick = {
                                        showMenu = false
                                        onShare?.invoke(meta.id, meta.title)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_share),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("View queue") },
                                    onClick = {
                                        showMenu = false
                                        onOpenQueue?.invoke()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_list),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter),
                color = songPalette.accent,
                trackColor = songPalette.playerControlSurface.copy(alpha = 0.42f),
                strokeCap = StrokeCap.Square,
            )
        }
    }
}

@Composable
private fun MiniArtwork(
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    accentColor: Color,
) {
    val context = LocalContext.current
    val thumbnailUrl = mediaMetadata?.thumbnailUrl
    val thumbnailModel = remember(thumbnailUrl) {
        thumbnailUrl?.let {
            ImageRequest.Builder(context)
                .data(it)
                .size(ARTWORK_REQUEST_SIZE, ARTWORK_REQUEST_SIZE)
                .memoryCacheKey(it)
                .build()
        }
    }
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.96f,
        animationSpec = OmniMotion.gentleSpring(),
        label = "mini_artwork_scale",
    )

    Box(
        modifier = Modifier
            .size(OmniChrome.MiniPlayerArtwork)
            .shadow(
                elevation = 6.dp,
                shape = OmniShapes.ArtworkSmall,
                ambientColor = Color.Black.copy(alpha = 0.32f),
                spotColor = accentColor.copy(alpha = 0.10f),
            )
            .clip(OmniShapes.ArtworkSmall)
            .background(
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.12f),
                        OmniColors.OmniGlassStrong,
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnailModel != null) {
            AsyncImage(
                model = thumbnailModel,
                contentDescription = "Album art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(OmniShapes.ArtworkSmall)
                    .graphicsLayerScale(artworkScale),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_play_arrow),
                contentDescription = null,
                tint = OmniColors.TextTertiary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun MiniMediaInfo(
    mediaMetadata: MediaMetadata?,
    modifier: Modifier = Modifier,
) {
    val title = mediaMetadata?.title?.takeIf { it.isNotBlank() } ?: "Unknown track"
    val artist = mediaMetadata
        ?.artists
        ?.joinToString(", ") { it.name }
        ?.takeIf { it.isNotBlank() }
        ?: "Unknown artist"

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        AnimatedContent(
            targetState = title,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "mini_title",
        ) { currentTitle ->
            Text(
                text = currentTitle,
                style = OmniTextStyles.songTitle,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        AnimatedContent(
            targetState = artist,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "mini_artist",
        ) { currentArtist ->
            Text(
                text = currentArtist,
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MiniPlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    playbackState: Int,
    accentColor: Color,
    onAccent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(OmniChrome.MiniPlayerButton)
            .clip(OmniShapes.Pill)
            .background(accentColor.copy(alpha = 0.88f))
            .omniPressScaleBounce(interactionSource),
    ) {
        if (isLoading) {
            OmniTuneLoader(
                modifier = Modifier.size(22.dp),
                color = accentColor,
                size = 22.dp,
            )
        } else {
            Icon(
                painter = painterResource(
                    if (playbackState == Player.STATE_ENDED || !isPlaying) {
                        R.drawable.ic_play_arrow
                    } else {
                        R.drawable.ic_pause
                    }
                ),
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = onAccent,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun MiniControlButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(OmniChrome.MiniPlayerButton),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = OmniColors.TextSecondary,
            modifier = Modifier.size(24.dp),
        )
    }
}

private fun Modifier.graphicsLayerScale(scale: Float): Modifier =
    this.then(
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    )
