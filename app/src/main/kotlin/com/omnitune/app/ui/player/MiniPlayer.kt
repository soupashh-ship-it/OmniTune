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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.omnitune.app.constants.OmniMiniPlayerDesign
import com.omnitune.app.constants.OmniMiniPlayerDesignKey
import com.omnitune.app.constants.SwipeSensitivityKey
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
import com.omnitune.app.utils.rememberEnumPreference
import com.omnitune.app.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    val isPlaying by (playerConnection?.isPlaying ?: flowOf(false)).collectAsStateWithLifecycle(initialValue = false)
    val playbackState by (playerConnection?.playbackState ?: flowOf(Player.STATE_IDLE)).collectAsStateWithLifecycle(initialValue = Player.STATE_IDLE)
    val mediaMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsStateWithLifecycle(initialValue = null)
    val canSkipNext by (playerConnection?.canSkipNext ?: flowOf(false)).collectAsStateWithLifecycle(initialValue = false)
    val canSkipPrevious by (playerConnection?.canSkipPrevious ?: flowOf(false)).collectAsStateWithLifecycle(initialValue = false)
    val isLoading = playbackState == STATE_BUFFERING
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    val miniPlayerDesign by rememberEnumPreference(
        OmniMiniPlayerDesignKey,
        OmniMiniPlayerDesign.DEFAULT,
    )
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    )
    val autoSwipeThreshold = (600 / (1f + exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    val bodyInteraction = remember { MutableInteractionSource() }

    val compactMiniPlayer = miniPlayerDesign == OmniMiniPlayerDesign.COMPACT
    val miniArtworkSize = if (compactMiniPlayer) 42.dp else OmniChrome.MiniPlayerArtwork
    val miniContentHeight = if (compactMiniPlayer) 50.dp else OmniChrome.MiniPlayerContentHeight
    val miniButtonSize = if (compactMiniPlayer) 36.dp else OmniChrome.MiniPlayerButton
    val miniIconSize = if (compactMiniPlayer) 21.dp else 24.dp
    val miniGlassStyle = OmniGlassDefaults.miniPlayerStyle(
        isDark = true,
        isPureBlack = pureBlack,
    ).copy(
        surfaceTint = if (pureBlack) Color(0xFF0C0C10) else Color(0xFF181822),
        surfaceAlpha = 1f,
        borderColor = if (pureBlack) Color(0xFF1E1E26) else Color(0xFF2E2E3E),
        borderAlpha = 1f,
        borderWidth = 1.dp,
        shadowElevation = 10.dp,
        shadowAmbient = Color.Black.copy(alpha = 0.5f),
        shadowSpot = Color.Black.copy(alpha = 0.4f),
    )
    OmniGlassSurface(
        shape = OmniShapes.Dock,
        style = miniGlassStyle,
        modifier = modifier
            .fillMaxWidth()
            .height(OmniChrome.MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = OmniChrome.BottomDockHorizontalPadding),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(playerConnection, canSkipNext, canSkipPrevious, layoutDirection, swipeSensitivity) {
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
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                        .clip(OmniShapes.Dock)
                        .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(miniContentHeight)
                        .clip(OmniShapes.Medium)
                        .omniPressScale(bodyInteraction)
                        .clickable(
                            interactionSource = bodyInteraction,
                            indication = androidx.compose.material3.ripple(
                                bounded = true,
                                color = OmniColors.OmniAccentPrimary.copy(alpha = 0.12f),
                            ),
                            onClick = onClick,
                        )
                        .padding(horizontal = OmniSpacing.micro),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniArtwork(
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        accentColor = OmniColors.OmniAccentPrimary,
                        artworkSize = miniArtworkSize,
                    )
                    Spacer(modifier = Modifier.width(if (compactMiniPlayer) OmniSpacing.compact else OmniSpacing.small))
                    MiniMediaInfo(
                        mediaMetadata = mediaMetadata,
                        modifier = Modifier.weight(1f),
                    )
                }

                playerConnection?.let { pc ->
                    MiniPlaybackBars(
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .width(if (compactMiniPlayer) 28.dp else 36.dp)
                            .height(28.dp),
                    )
                    Spacer(modifier = Modifier.width(OmniSpacing.small))
                    MiniPlayPauseButton(
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        playbackState = playbackState,
                        accentColor = OmniColors.OmniAccentPrimary,
                        onAccent = Color.White,
                        buttonSize = miniButtonSize,
                        iconSize = miniIconSize,
                        onClick = {
                            if (playbackState == Player.STATE_ENDED || !isPlaying) {
                                pc.playOrResolveCurrent()
                            } else {
                                pc.pause()
                            }
                        },
                    )
                    Box {
                        MiniControlButton(
                            icon = R.drawable.ic_more_vert,
                            contentDescription = "More options",
                            onClick = { showMenu = true },
                            buttonSize = miniButtonSize,
                            iconSize = miniIconSize,
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
                                if (canSkipPrevious) {
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
                                if (canSkipNext) {
                                    DropdownMenuItem(
                                        text = { Text("Next") },
                                        onClick = {
                                            showMenu = false
                                            pc.seekToNext()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_skip_next),
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
        }
    }
}

@Composable
private fun MiniPlaybackBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val bars = if (isPlaying) intArrayOf(11, 20, 15, 25, 17, 23, 12) else intArrayOf(7, 10, 8, 12, 8, 10, 7)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bars.forEach { height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .clip(OmniShapes.Pill)
                    .background(OmniColors.OmniAccentSecondary.copy(alpha = if (isPlaying) 0.95f else 0.48f)),
            )
        }
    }
}

@Composable
private fun MiniArtwork(
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    accentColor: Color,
    artworkSize: androidx.compose.ui.unit.Dp = OmniChrome.MiniPlayerArtwork,
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
            .size(artworkSize)
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
    val title = mediaMetadata?.title?.takeIf { it.isNotBlank() }.orEmpty()
    val artist = mediaMetadata
        ?.artists
        ?.joinToString(", ") { it.name }
        ?.takeIf { it.isNotBlank() }
        .orEmpty()

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

        if (artist.isNotBlank()) {
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
}

@Composable
private fun MiniPlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    playbackState: Int,
    accentColor: Color,
    onAccent: Color,
    buttonSize: androidx.compose.ui.unit.Dp = OmniChrome.MiniPlayerButton,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(buttonSize)
            .clip(OmniShapes.Pill)
            .background(accentColor.copy(alpha = 0.88f))
            .omniPressScaleBounce(interactionSource),
    ) {
        if (isLoading) {
            OmniTuneLoader(
                modifier = Modifier.size(22.dp),
                color = onAccent,
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
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun MiniControlButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    buttonSize: androidx.compose.ui.unit.Dp = OmniChrome.MiniPlayerButton,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(buttonSize),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = OmniColors.TextSecondary,
            modifier = Modifier.size(iconSize),
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
