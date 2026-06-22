/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.omnitune.app.R
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.omnitune.app.lyrics.LyricsUtils
import androidx.compose.runtime.mutableLongStateOf

private const val MINI_PLAYER_HEIGHT = 76

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    playerConnection: PlayerConnection? = null,
    onClick: () -> Unit = {},
) {
    val isPlayingFlow = playerConnection?.isPlaying ?: kotlinx.coroutines.flow.flowOf(false)
    val isPlaying by isPlayingFlow.collectAsState(initial = false)
    
    val playbackStateFlow = playerConnection?.playbackState ?: kotlinx.coroutines.flow.flowOf(Player.STATE_IDLE)
    val playbackState by playbackStateFlow.collectAsState(initial = Player.STATE_IDLE)
    
    val mediaMetadataFlow = playerConnection?.mediaMetadata ?: kotlinx.coroutines.flow.flowOf(null)
    val mediaMetadata by mediaMetadataFlow.collectAsState(initial = null)
    
    val canSkipNextFlow = playerConnection?.canSkipNext ?: kotlinx.coroutines.flow.flowOf(false)
    val canSkipNext by canSkipNextFlow.collectAsState(initial = false)
    val isLoading = playbackState == STATE_BUFFERING
    val hasPlayer = playerConnection != null
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    var position by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    val currentMediaId = mediaMetadata?.id
    LaunchedEffect(playerConnection?.player, currentMediaId) {
        while (true) {
            val player = playerConnection?.player
            if (player != null) {
                val dur = player.duration
                val pos = player.currentPosition
                if (playbackState == Player.STATE_ENDED) {
                    if (dur > 0) { position = dur.toFloat(); duration = dur.toFloat() }
                } else {
                    duration = if (dur > 0) dur.toFloat() else 0f
                    position = if (pos in 0..dur) pos.toFloat() else 0f
                }
            } else { duration = 0f; position = 0f }
            delay(50) // Reduced delay for smoother lyrics
        }
    }
    
    // Lyrics fetching for miniplayer
    val currentLyricsFlow = playerConnection?.currentLyrics ?: kotlinx.coroutines.flow.flowOf(null)
    val lyricsEntity by currentLyricsFlow.collectAsState(initial = null)
    val parsedLines = remember(lyricsEntity?.id, currentMediaId) {
        lyricsEntity?.lyrics?.let {
            if (LyricsUtils.isTtml(it)) LyricsUtils.parseTtml(it) else LyricsUtils.parseLyrics(it)
        } ?: emptyList()
    }
    val currentLineIndex = remember(position, parsedLines.size) {
        if (parsedLines.isEmpty()) -1 else LyricsUtils.findCurrentLineIndex(parsedLines, position.toLong())
    }
    val currentLyricsText = if (currentLineIndex >= 0) parsedLines[currentLineIndex].text else null

    val progress = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    val animationSpec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    val swipeSensitivity = 0.73f
    val autoSwipeThreshold = (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()

    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "artworkScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MINI_PLAYER_HEIGHT.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 12.dp)
            .shadow(12.dp, OmniShapes.LG, ambientColor = OmniColors.Primary.copy(alpha = 0.15f), spotColor = OmniColors.Primary.copy(alpha = 0.1f))
            .clip(OmniShapes.LG)
            .border(1.dp, OmniColors.GlassBorder, OmniShapes.LG)
            .background(Brush.verticalGradient(listOf(Color(0xFF232736).copy(alpha = 0.5f), Color(0xFF0C0E16).copy(alpha = 0.9f))))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragStartTime = System.currentTimeMillis(); totalDragDistance = 0f },
                    onDragCancel = { coroutineScope.launch { offsetXAnimatable.animateTo(0f, animationSpec) } },
                    onHorizontalDrag = { _, dragAmount ->
                        val pc = playerConnection ?: return@detectHorizontalDragGestures
                        val p = pc.player
                        val adj = if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                        val allow = (adj < 0 && p.nextMediaItemIndex != -1) || (adj > 0 && p.previousMediaItemIndex != -1)
                        if (allow) {
                            totalDragDistance += kotlin.math.abs(adj)
                            coroutineScope.launch { offsetXAnimatable.snapTo(offsetXAnimatable.value + adj) }
                        }
                    },
                    onDragEnd = {
                        val pc = playerConnection ?: return@detectHorizontalDragGestures
                        val p = pc.player
                        val dur = System.currentTimeMillis() - dragStartTime
                        val vel = if (dur > 0) totalDragDistance / dur else 0f
                        val off = offsetXAnimatable.value
                        val should = (kotlin.math.abs(off) > 50f && vel > (swipeSensitivity * -8.25f + 8.5f)) || kotlin.math.abs(off) > autoSwipeThreshold
                        if (should) {
                            if (off > 0 && p.previousMediaItemIndex != -1) p.seekToPreviousMediaItem()
                            else if (off < 0 && canSkipNext) pc.seekToNext()
                        }
                        coroutineScope.launch { offsetXAnimatable.animateTo(0f, animationSpec) }
                    }
                )
            }
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter),
            color = OmniColors.Primary, 
            trackColor = OmniColors.Primary.copy(alpha = 0.15f), 
            strokeCap = StrokeCap.Square,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.material3.ripple(bounded = true),
                    onClick = onClick
                )
                .padding(horizontal = 14.dp),
        ) {
            // Album art with progress ring
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(OmniColors.GlassSurface), contentAlignment = Alignment.Center) {
                    mediaMetadata?.thumbnailUrl?.let { url ->
                        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(artworkScale))
                    } ?: Icon(painterResource(R.drawable.ic_play_arrow), contentDescription = null, tint = OmniColors.TextMuted, modifier = Modifier.size(20.dp).scale(artworkScale))
                }
            }
            // Title + Artist/Lyrics with better spacing
            Box(modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 4.dp)) {
                mediaMetadata?.let { MiniMediaInfo(mediaMetadata = it, lyricsText = currentLyricsText) }
            }

            // OMNITUNE: Scrobbling badge
            val context = androidx.compose.ui.platform.LocalContext.current
            val lastFmEnabled by com.omnitune.app.utils.rememberPreference(com.omnitune.app.constants.EnableLastFMScrobblingKey, false)

            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "scrobble_pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable<Float>(
                    animation = androidx.compose.animation.core.tween<Float>(800),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "scrobble_alpha"
            )

            if (lastFmEnabled && isPlaying) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(OmniColors.Hot.copy(alpha = pulseAlpha))
                )
            }
            // Playback controls
            if (hasPlayer) {
                val pc = playerConnection
                IconButton(
                    onClick = {
                        if (playbackState == Player.STATE_ENDED || !isPlaying) pc.playOrResolveCurrent()
                        else pc.pause()
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    if (isLoading) com.omnitune.app.ui.component.OmniTuneLoader(modifier = Modifier.size(20.dp), color = OmniColors.Primary, size = 20.dp)
                    else Icon(
                        painter = painterResource(if (playbackState == Player.STATE_ENDED || !isPlaying) R.drawable.ic_play_arrow else R.drawable.ic_pause),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = OmniColors.TextPrimary, modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(enabled = canSkipNext, onClick = { pc.seekToNext() }, modifier = Modifier.size(40.dp)) {
                    Icon(painterResource(R.drawable.ic_skip_next), contentDescription = "Next",
                        tint = if (canSkipNext) OmniColors.TextPrimary else OmniColors.TextMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniMediaInfo(mediaMetadata: MediaMetadata, lyricsText: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.Start) {
        AnimatedContent(targetState = mediaMetadata.title, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "title") { title ->
            Text(text = title, color = OmniColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee())
        }
        val secondaryText = lyricsText ?: mediaMetadata.artists.joinToString { it.name }
        val secondaryColor = if (lyricsText != null) OmniColors.Secondary else OmniColors.TextSecondary.copy(alpha = 0.8f)
        AnimatedContent(targetState = secondaryText, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "artist_or_lyrics") { text ->
            Text(text = text, color = secondaryColor, fontSize = 11.sp, fontWeight = FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee())
        }
    }
}
