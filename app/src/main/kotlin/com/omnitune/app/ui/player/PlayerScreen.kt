/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import coil3.BitmapImage
import coil3.compose.AsyncImage
import coil3.request.SuccessResult
import com.omnitune.app.R
import com.omnitune.app.lyrics.LyricsUtils
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.utils.formatDurationMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(
    playerConnection: PlayerConnection?,
    onDismiss: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
    val isPlaying by playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val playbackState by playerConnection?.playbackState?.collectAsState() ?: remember { mutableStateOf(Player.STATE_IDLE) }
    val shuffleEnabled by playerConnection?.shuffleModeEnabled?.collectAsState() ?: remember { mutableStateOf(false) }
    val repeatMode by playerConnection?.repeatMode?.collectAsState() ?: remember { mutableStateOf(REPEAT_MODE_OFF) }
    val thumbnailUrl = mediaMetadata?.thumbnailUrl
    val isSeeking = remember { mutableFloatStateOf(-1f) }

    // OMNITUNE: Dynamic palette color from album art
    var dominantColor by remember { mutableStateOf(Color(0xFF1A1035)) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(mediaMetadata?.thumbnailUrl) {
        val url = mediaMetadata?.thumbnailUrl ?: return@LaunchedEffect
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val request = coil3.request.ImageRequest.Builder(context)
                    .data(url)
                    .build()
                val result = coil3.SingletonImageLoader.get(context).execute(request)
                val bitmap = (result as? coil3.request.SuccessResult)?.image
                    ?.let { (it as? coil3.BitmapImage)?.bitmap }
                bitmap?.let {
                    val palette = androidx.palette.graphics.Palette.from(it).generate()
                    val swatch = palette.darkMutedSwatch ?: palette.dominantSwatch
                    swatch?.let { s ->
                        dominantColor = Color(s.rgb).copy(alpha = 1f)
                    }
                }
            } catch (e: Exception) {
                // Keep default color on failure
            }
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800),
        label = "animatedColor"
    )

    Box(modifier = modifier.fillMaxSize().background(
        Brush.verticalGradient(
            colors = listOf(animatedColor.copy(alpha = 0.8f), OmniColors.Background),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )
    )) {
        thumbnailUrl?.let { url ->
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(60.dp), alpha = 0.25f)
        }
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(OmniColors.Background.copy(alpha = 0.7f), OmniColors.Background.copy(alpha = 0.5f), OmniColors.Background.copy(alpha = 0.85f)))
        ))
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            // Top bar
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp).clip(OmniShapes.SM).border(1.dp, OmniColors.GlassBorderLight, OmniShapes.SM).background(OmniColors.GlassSurface)) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back", tint = OmniColors.TextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("Now Playing", style = androidx.compose.material3.MaterialTheme.typography.labelLarge, color = OmniColors.TextMuted)
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Album Art
            AnimatedContent(targetState = mediaMetadata?.id ?: "", transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "album_art", modifier = Modifier.weight(0.45f)) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.fillMaxWidth(0.72f).aspectRatio(1f).shadow(24.dp, OmniShapes.XL, ambientColor = OmniColors.Primary.copy(alpha = 0.15f))
                        .clip(OmniShapes.XL).border(2.dp, OmniColors.GlassBorder, OmniShapes.XL).background(OmniColors.GlassSurfaceStrong)) {
                        if (!thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(model = thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
            com.omnitune.app.ui.component.AudioVisualizer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                isPlaying = isPlaying,
                color = OmniColors.Primary.copy(alpha = 0.4f),
                activeColor = OmniColors.Primary,
            )
            // Lyrics
            LyricsGlassPanel(playerConnection = playerConnection, modifier = Modifier.weight(0.25f))
            // Song Info
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(targetState = mediaMetadata?.title ?: "No track", transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "title") { title ->
                    Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedContent(targetState = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown artist", transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "artist") { artists ->
                    Text(artists, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = OmniColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(16.dp))
                PlayerSeekBar(playerConnection = playerConnection, isSeeking = isSeeking)
                Spacer(modifier = Modifier.height(12.dp))
                PlayerControlRow(isPlaying, playbackState, shuffleEnabled, repeatMode, playerConnection)
                Spacer(modifier = Modifier.height(8.dp))
                PlayerExtrasRow(playerConnection, onOpenQueue)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlayerSeekBar(playerConnection: PlayerConnection?, isSeeking: androidx.compose.runtime.MutableFloatState) {
    val player = playerConnection?.player
    val playbackState by playerConnection?.playbackState?.collectAsState() ?: remember { mutableStateOf(Player.STATE_IDLE) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playerConnection?.player) {
        while (true) {
            val p = playerConnection?.player
            if (p != null) {
                val dur = p.duration; val pos = p.currentPosition
                if (playbackState == Player.STATE_ENDED) { if (dur > 0) { currentPosition = dur; duration = dur } }
                else { duration = if (dur > 0) dur else 0L; currentPosition = if (pos in 0..dur) pos else 0L }
            } else { currentPosition = 0L; duration = 0L }
            delay(200)
        }
    }
    val progress by animateFloatAsState(targetValue = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f, label = "progress")
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(value = if (isSeeking.floatValue >= 0f) isSeeking.floatValue else progress,
            onValueChange = { isSeeking.floatValue = it },
            onValueChangeFinished = { if (player != null && duration > 0) player.seekTo((isSeeking.floatValue * duration).toLong()); isSeeking.floatValue = -1f },
            colors = SliderDefaults.colors(thumbColor = OmniColors.Primary, activeTrackColor = OmniColors.Primary, inactiveTrackColor = OmniColors.GlassSurfaceStrong),
            modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDurationMs(if (isSeeking.floatValue >= 0f) (isSeeking.floatValue * duration).toLong() else currentPosition),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = OmniColors.TextMuted)
            Text(formatDurationMs(duration), style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = OmniColors.TextMuted)
        }
    }
}

@Composable
private fun PlayerControlRow(isPlaying: Boolean, playbackState: Int, shuffleEnabled: Boolean, repeatMode: Int, playerConnection: PlayerConnection?) {
    val sleepTimerRunning by playerConnection?.sleepTimerRunning?.collectAsState() ?: remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        GlassCircleButton(painterResource(R.drawable.ic_shuffle), "Shuffle",
            tint = if (shuffleEnabled) OmniColors.Primary else OmniColors.TextMuted,
            onClick = { playerConnection?.player?.shuffleModeEnabled = !shuffleEnabled })
        GlassCircleButton(painterResource(R.drawable.ic_skip_previous), "Previous",
            tint = OmniColors.TextPrimary, size = 48.dp, iconSize = 28.dp,
            onClick = { playerConnection?.seekToPrevious() })
        // Play/Pause gradient circle
        Box(modifier = Modifier.size(64.dp).shadow(16.dp, CircleShape, ambientColor = OmniColors.Primary.copy(alpha = 0.3f))
            .clip(CircleShape).background(Brush.linearGradient(listOf(OmniColors.Primary, OmniColors.Secondary))), contentAlignment = Alignment.Center) {
            IconButton(onClick = {
                val p = playerConnection?.player ?: return@IconButton
                if (playbackState == Player.STATE_ENDED) { p.seekTo(0, 0); p.playWhenReady = true }
                else { if (isPlaying) p.pause() else p.play() }
            }, modifier = Modifier.size(64.dp)) {
                Icon(painter = painterResource(if (playbackState == Player.STATE_ENDED || !isPlaying) R.drawable.ic_play_arrow else R.drawable.ic_pause),
                    contentDescription = if (isPlaying) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
        GlassCircleButton(painterResource(R.drawable.ic_skip_next), "Next",
            tint = OmniColors.TextPrimary, size = 48.dp, iconSize = 28.dp,
            onClick = { playerConnection?.seekToNext() })
        GlassCircleButton(painterResource(R.drawable.ic_repeat), "Repeat",
            tint = if (repeatMode != REPEAT_MODE_OFF) OmniColors.Primary else OmniColors.TextMuted,
            onClick = { playerConnection?.player?.let { p -> p.repeatMode = when (p.repeatMode) { REPEAT_MODE_OFF -> REPEAT_MODE_ALL; REPEAT_MODE_ALL -> REPEAT_MODE_ONE; else -> REPEAT_MODE_OFF } } })
            
        // OMNITUNE: Sleep timer button
        IconButton(onClick = { showSleepTimerDialog = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_bedtime),
                contentDescription = if (sleepTimerRunning) "Cancel sleep timer" else "Set sleep timer",
                tint = if (sleepTimerRunning) OmniColors.Primary else OmniColors.TextMuted,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false },
            onSet = { minutes, endOfSong ->
                playerConnection?.service?.sleepTimer?.start(
                    durationMs = minutes * 60_000L,
                    stopAtEndOfSong = endOfSong
                )
                showSleepTimerDialog = false
            },
            onCancel = {
                playerConnection?.service?.sleepTimer?.cancel()
                showSleepTimerDialog = false
            },
            isRunning = sleepTimerRunning
        )
    }
}

@Composable
private fun GlassCircleButton(painter: androidx.compose.ui.graphics.painter.Painter, contentDescription: String, tint: Color, onClick: () -> Unit, size: Dp = 44.dp, iconSize: Dp = 22.dp) {
    IconButton(onClick = onClick, modifier = Modifier.size(size).clip(CircleShape).border(1.dp, OmniColors.GlassBorderLight, CircleShape).background(OmniColors.GlassSurface)) {
        Icon(painter, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun PlayerExtrasRow(playerConnection: PlayerConnection?, onOpenQueue: () -> Unit) {
    val currentSongState = playerConnection?.currentSong?.collectAsState(initial = null)
    val liked = currentSongState?.value?.song?.liked == true
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { playerConnection?.toggleLike() }, modifier = Modifier.size(40.dp).clip(CircleShape).background(OmniColors.GlassSurface)) {
            Icon(painterResource(if (liked) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                contentDescription = if (liked) "Unlike" else "Like", tint = if (liked) OmniColors.Hot else OmniColors.TextMuted, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onOpenQueue, modifier = Modifier.size(40.dp).clip(CircleShape).background(OmniColors.GlassSurface)) {
            Icon(painterResource(R.drawable.ic_list), contentDescription = "Queue", tint = OmniColors.TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun LyricsGlassPanel(playerConnection: PlayerConnection?, modifier: Modifier = Modifier) {
    val lyricsEntity by playerConnection?.currentLyrics?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
    val player = playerConnection?.player
    val currentMediaId = player?.currentMediaItem?.mediaId
    var position by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playerConnection?.player) { while (true) { position = player?.currentPosition ?: 0L; delay(50) } }
    val parsedLines = remember(lyricsEntity?.id, currentMediaId) { 
        lyricsEntity?.lyrics?.let { 
            if (LyricsUtils.isTtml(it)) LyricsUtils.parseTtml(it) else LyricsUtils.parseLyrics(it) 
        } ?: emptyList() 
    }
    if (parsedLines.isEmpty()) return
    val currentLineIndex = remember(position, parsedLines.size) { if (parsedLines.isEmpty()) -1 else LyricsUtils.findCurrentLineIndex(parsedLines, position) }
    val listState = rememberLazyListState()
    LaunchedEffect(currentLineIndex) { if (currentLineIndex >= 0) listState.animateScrollToItem(index = currentLineIndex, scrollOffset = -200) }
    Box(modifier = modifier.fillMaxWidth().clip(OmniShapes.LG).background(OmniColors.GlassSurface).border(1.dp, OmniColors.GlassBorderLight, OmniShapes.LG).padding(vertical = 8.dp)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, userScrollEnabled = true) {
            itemsIndexed(parsedLines) { index, line ->
                val isCurrent = index == currentLineIndex
                val alpha by animateFloatAsState(targetValue = when { isCurrent -> 1f; kotlin.math.abs(index - currentLineIndex) <= 2 -> 0.5f; else -> 0.2f }, label = "lyrics_alpha")
                
                val textStyle = if (isCurrent) androidx.compose.material3.MaterialTheme.typography.titleMedium else androidx.compose.material3.MaterialTheme.typography.bodyMedium
                val fontSize = if (isCurrent) 16.sp else 14.sp
                val fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal

                if (line.words != null && line.words.isNotEmpty()) {
                    val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                        line.words.forEach { word ->
                            val wordStartMs = (word.startTime * 1000).toLong()
                            val wordColor = if (isCurrent) {
                                if (position >= wordStartMs) OmniColors.Secondary else OmniColors.TextPrimary.copy(alpha = 0.5f)
                            } else {
                                if (index < currentLineIndex) OmniColors.Secondary.copy(alpha = 0.5f) else OmniColors.TextSecondary
                            }
                            
                            withStyle(androidx.compose.ui.text.SpanStyle(color = wordColor)) {
                                append(word.text)
                                append(" ")
                            }
                        }
                    }
                    Text(
                        text = annotatedString,
                        style = textStyle,
                        fontWeight = fontWeight,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp).alpha(alpha),
                        fontSize = fontSize
                    )
                } else {
                    Text(line.text, style = textStyle,
                        color = if (isCurrent) OmniColors.Secondary else OmniColors.TextSecondary, fontWeight = fontWeight,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp).alpha(alpha),
                        fontSize = fontSize)
                }
            }
        }
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
                .clip(RoundedCornerShape(20.dp))
                .background(OmniColors.GlassSurface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Sleep Timer", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OmniColors.TextPrimary)

            options.forEach { mins ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedMinutes == mins) OmniColors.Primary.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { selectedMinutes = mins }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$mins minutes", color = OmniColors.TextPrimary, fontSize = 15.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Stop at end of song", color = OmniColors.TextPrimary, modifier = Modifier.weight(1f))
                Switch(checked = endOfSong, onCheckedChange = { endOfSong = it })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isRunning) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OmniColors.Hot)
                    ) { Text("Cancel Timer") }
                }
                Button(
                    onClick = { onSet(selectedMinutes, endOfSong) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = OmniColors.Primary)
                ) { Text(if (isRunning) "Restart" else "Set Timer") }
            }
        }
    }
}
