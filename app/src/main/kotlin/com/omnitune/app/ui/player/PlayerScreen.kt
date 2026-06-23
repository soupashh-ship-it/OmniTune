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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import timber.log.Timber

@Composable
fun PlayerScreen(
    playerConnection: PlayerConnection?,
    onDismiss: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val mediaMetadataFlow = playerConnection?.mediaMetadata ?: kotlinx.coroutines.flow.flowOf(null)
    val mediaMetadata by mediaMetadataFlow.collectAsState(initial = null)
    val isPlayingFlow = playerConnection?.isPlaying ?: kotlinx.coroutines.flow.flowOf(false)
    val isPlaying by isPlayingFlow.collectAsState(initial = false)
    val playbackStateFlow = playerConnection?.playbackState ?: kotlinx.coroutines.flow.flowOf(Player.STATE_IDLE)
    val playbackState by playbackStateFlow.collectAsState(initial = Player.STATE_IDLE)
    val shuffleEnabledFlow = playerConnection?.shuffleModeEnabled ?: kotlinx.coroutines.flow.flowOf(false)
    val shuffleEnabled by shuffleEnabledFlow.collectAsState(initial = false)
    val repeatModeFlow = playerConnection?.repeatMode ?: kotlinx.coroutines.flow.flowOf(REPEAT_MODE_OFF)
    val repeatMode by repeatModeFlow.collectAsState(initial = REPEAT_MODE_OFF)
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
            colors = listOf(animatedColor.copy(alpha = 0.45f), Color(0xFF0C0E16)),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )
    )) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Metro Edge-to-Edge Album Art
            Box(modifier = Modifier.fillMaxWidth().weight(0.55f)) {
                AnimatedContent(
                    targetState = thumbnailUrl,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label = "album_art",
                    modifier = Modifier.fillMaxSize()
                ) { url ->
                    if (!url.isNullOrBlank()) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Dark gradient overlay at the bottom of the album art so text is readable if it overlaps
                        Box(modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF0C0E16).copy(alpha = 0.6f), Color(0xFF0C0E16)),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        ))
                    }
                }
                
                // Top bar over album art
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Navigate up", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onOpenQueue) {
                        Icon(painterResource(R.drawable.ic_list), contentDescription = "Queue", tint = Color.White)
                    }
                }
            }

            // Metro Typography and Controls
            Column(modifier = Modifier.weight(0.45f).fillMaxWidth().padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Left-aligned bold typography
                AnimatedContent(targetState = mediaMetadata?.title ?: "No track", transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "title") { title ->
                    Text(
                        text = title.uppercase(),
                        style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                AnimatedContent(targetState = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown artist", transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "artist") { artists ->
                    Text(
                        text = artists,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = animatedColor, // Vibrant accent
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                PlayerSeekBar(playerConnection = playerConnection, isSeeking = isSeeking)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                PlayerControlRow(isPlaying, playbackState, shuffleEnabled, repeatMode, playerConnection)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                PlayerExtrasRow(playerConnection, onOpenQueue)
            }
        }
    }
}

@Composable
private fun PlayerSeekBar(playerConnection: PlayerConnection?, isSeeking: androidx.compose.runtime.MutableFloatState) {
    val playbackStateFlow = playerConnection?.playbackState ?: kotlinx.coroutines.flow.flowOf(Player.STATE_IDLE)
    val playbackState by playbackStateFlow.collectAsState(initial = Player.STATE_IDLE)
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playerConnection) {
        while (true) {
            val pc = playerConnection
            if (pc != null) {
                val dur = pc.duration
                if (playbackState == Player.STATE_ENDED) { if (dur > 0) { currentPosition = dur; duration = dur } }
                else { duration = if (dur > 0) dur else 0L; currentPosition = pc.currentPosition }
            }
            delay(200)
        }
    }
    val progress by animateFloatAsState(targetValue = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f, label = "progress")
    Column(modifier = Modifier.fillMaxWidth().semantics { 
        contentDescription = "Playback progress"
    }) {
        Slider(value = if (isSeeking.floatValue >= 0f) isSeeking.floatValue else progress,
            onValueChange = { isSeeking.floatValue = it },
            onValueChangeFinished = { if (playerConnection != null && duration > 0) playerConnection.seekTo((isSeeking.floatValue * duration).toLong()); isSeeking.floatValue = -1f },
            colors = SliderDefaults.colors(thumbColor = OmniColors.Primary, activeTrackColor = OmniColors.Primary, inactiveTrackColor = OmniColors.GlassSurfaceStrong),
            modifier = Modifier.fillMaxWidth().semantics { stateDescription = "At ${formatDurationMs(currentPosition)} of ${formatDurationMs(duration)}" }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDurationMs(if (isSeeking.floatValue >= 0f) (isSeeking.floatValue * duration).toLong() else currentPosition),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = OmniColors.TextMuted, modifier = Modifier.clearAndSetSemantics {})
            Text(formatDurationMs(duration), style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = OmniColors.TextMuted, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}

@Composable
private fun PlayerControlRow(isPlaying: Boolean, playbackState: Int, shuffleEnabled: Boolean, repeatMode: Int, playerConnection: PlayerConnection?) {
    val sleepTimerFlow = playerConnection?.sleepTimerRunning ?: kotlinx.coroutines.flow.flowOf(false)
    val sleepTimerRunning by sleepTimerFlow.collectAsState(initial = false)
    val canSkipPreviousFlow = playerConnection?.canSkipPrevious ?: kotlinx.coroutines.flow.flowOf(false)
    val canSkipNextFlow = playerConnection?.canSkipNext ?: kotlinx.coroutines.flow.flowOf(false)
    val canSkipPrevious by canSkipPreviousFlow.collectAsState(initial = false)
    val canSkipNext by canSkipNextFlow.collectAsState(initial = false)
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        MetroIconButton(painterResource(R.drawable.ic_shuffle), if (shuffleEnabled) "Shuffle on" else "Shuffle off",
            tint = if (shuffleEnabled) OmniColors.Primary else OmniColors.TextMuted,
            onClick = { 
                val pc = playerConnection
                if (pc != null) {
                    if (pc.mediaItemCount <= 1) {
                        android.widget.Toast.makeText(context, "Shuffle requires more than one track", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        pc.setShuffleModeEnabled(!shuffleEnabled)
                    }
                }
            })
        MetroIconButton(painterResource(R.drawable.ic_skip_previous), "Previous",
            tint = OmniColors.TextPrimary, size = 48.dp, iconSize = 28.dp,
            enabled = canSkipPrevious || repeatMode == REPEAT_MODE_ALL,
            onClick = { playerConnection?.seekToPrevious() })
        // Play/Pause gradient circle
        Box(modifier = Modifier.size(64.dp).shadow(16.dp, CircleShape, ambientColor = OmniColors.Primary.copy(alpha = 0.3f))
            .clip(CircleShape).background(Brush.linearGradient(listOf(OmniColors.Primary, OmniColors.Secondary))), contentAlignment = Alignment.Center) {
            IconButton(onClick = {
                val pc = playerConnection ?: return@IconButton
                if (isPlaying) pc.pause() else pc.playOrResolveCurrent()
            }, modifier = Modifier.size(64.dp)) {
                Icon(painter = painterResource(if (playbackState == Player.STATE_ENDED || !isPlaying) R.drawable.ic_play_arrow else R.drawable.ic_pause),
                    contentDescription = if (isPlaying) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
        MetroIconButton(painterResource(R.drawable.ic_skip_next), "Next",
            tint = OmniColors.TextPrimary, size = 48.dp, iconSize = 28.dp,
            enabled = canSkipNext || repeatMode == REPEAT_MODE_ALL,
            onClick = { playerConnection?.seekToNext() })
            
        val repeatIcon = when (repeatMode) {
            REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
            else -> R.drawable.ic_repeat
        }
        val repeatContentDesc = when (repeatMode) {
            REPEAT_MODE_OFF -> "Repeat off"
            REPEAT_MODE_ALL -> "Repeat all"
            REPEAT_MODE_ONE -> "Repeat one"
            else -> "Repeat"
        }
        MetroIconButton(painterResource(repeatIcon), repeatContentDesc,
            tint = if (repeatMode != REPEAT_MODE_OFF) OmniColors.Primary else OmniColors.TextMuted,
            onClick = { playerConnection?.toggleRepeatMode() })
            
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
private fun MetroIconButton(
    painter: androidx.compose.ui.graphics.painter.Painter, 
    contentDescription: String, 
    tint: Color, 
    onClick: () -> Unit, 
    size: Dp = 44.dp, 
    iconSize: Dp = 22.dp,
    enabled: Boolean = true
) {
    IconButton(onClick = onClick, modifier = Modifier.size(size), enabled = enabled) {
        Icon(painter, contentDescription = contentDescription, tint = if (enabled) tint else tint.copy(alpha = 0.38f), modifier = Modifier.size(iconSize))
    }
}

@androidx.media3.common.util.UnstableApi
@Composable
private fun PlayerExtrasRow(playerConnection: PlayerConnection?, onOpenQueue: () -> Unit) {
    val currentSongFlow = playerConnection?.currentSong ?: kotlinx.coroutines.flow.flowOf(null)
    val currentSongState = currentSongFlow.collectAsState(initial = null)
    val currentMetadataFlow = playerConnection?.mediaMetadata ?: kotlinx.coroutines.flow.flowOf(null)
    val currentMetadataState = currentMetadataFlow.collectAsState(initial = null)
    val liked = currentSongState.value?.song?.liked == true || currentMetadataState.value?.liked == true
    var showEffectsDialog by remember { mutableStateOf(false) }
    val downloadsViewModel: com.omnitune.app.ui.screens.DownloadsViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
    val context = androidx.compose.ui.platform.LocalContext.current

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { playerConnection?.toggleLike() }, modifier = Modifier.size(40.dp).clip(CircleShape).background(OmniColors.GlassSurface)) {
            Icon(painterResource(if (liked) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                contentDescription = if (liked) "Unlike" else "Like", tint = if (liked) OmniColors.Hot else OmniColors.TextMuted, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = {
            val song = currentSongState.value?.song
            val activeMetadata = currentMetadataState.value
            val videoId = song?.id ?: activeMetadata?.id
            val title = song?.title ?: activeMetadata?.title
            if (!videoId.isNullOrBlank() && !title.isNullOrBlank()) {
                val activeUri = playerConnection?.activeUri
                val resolvedStreamUrl = activeUri?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                Timber.d("Download button clicked for %s", videoId)
                downloadsViewModel.startDownload(videoId, title, resolvedStreamUrl) { _, message ->
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                Timber.w("Download button clicked without active song")
                android.widget.Toast.makeText(context, "No active song to download", android.widget.Toast.LENGTH_SHORT).show()
            }
        }, modifier = Modifier.size(40.dp).clip(CircleShape).background(OmniColors.GlassSurface)) {
            Icon(painterResource(com.omnitune.app.R.drawable.ic_download), contentDescription = "Download", tint = OmniColors.TextMuted, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = { showEffectsDialog = true }, modifier = Modifier.size(40.dp).clip(CircleShape).background(OmniColors.GlassSurface)) {
            Icon(painterResource(R.drawable.ic_settings), contentDescription = "Audio Effects", tint = OmniColors.TextMuted, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onOpenQueue, modifier = Modifier.size(40.dp).clip(CircleShape).background(OmniColors.GlassSurface)) {
            Icon(painterResource(R.drawable.ic_list), contentDescription = "Queue", tint = OmniColors.TextMuted, modifier = Modifier.size(20.dp))
        }
    }

    if (showEffectsDialog) {
        AudioEffectsDialog(playerConnection = playerConnection, onDismiss = { showEffectsDialog = false })
    }
}

@Composable
private fun AudioEffectsDialog(playerConnection: PlayerConnection?, onDismiss: () -> Unit) {
    var tempo by remember { mutableFloatStateOf(playerConnection?.playbackSpeed ?: 1f) }
    var pitch by remember { mutableFloatStateOf(playerConnection?.playbackPitch ?: 1f) }
    var skipSilence by remember { mutableStateOf(playerConnection?.skipSilenceEnabled ?: false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(OmniColors.GlassSurface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Audio Effects", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OmniColors.TextPrimary)

            // Tempo Slider
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tempo", color = OmniColors.TextPrimary, fontSize = 14.sp)
                    Text(String.format("%.2fx", tempo), color = OmniColors.TextMuted, fontSize = 14.sp)
                }
                Slider(
                    value = tempo,
                    onValueChange = { tempo = it },
                    onValueChangeFinished = { playerConnection?.setPlaybackParameters(tempo, pitch) },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = OmniColors.Primary, activeTrackColor = OmniColors.Primary, inactiveTrackColor = OmniColors.GlassSurfaceStrong)
                )
            }

            // Pitch Slider
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pitch", color = OmniColors.TextPrimary, fontSize = 14.sp)
                    Text(String.format("%.2fx", pitch), color = OmniColors.TextMuted, fontSize = 14.sp)
                }
                Slider(
                    value = pitch,
                    onValueChange = { pitch = it },
                    onValueChangeFinished = { playerConnection?.setPlaybackParameters(tempo, pitch) },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = OmniColors.Primary, activeTrackColor = OmniColors.Primary, inactiveTrackColor = OmniColors.GlassSurfaceStrong)
                )
            }

            // Skip Silence
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Skip Silence", color = OmniColors.TextPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = skipSilence,
                    onCheckedChange = { 
                        skipSilence = it
                        playerConnection?.setSkipSilenceEnabled(it) 
                    }
                )
            }

            // System Equalizer
            Button(
                onClick = {
                    val audioSessionId = playerConnection?.audioSessionId ?: 0
                    if (audioSessionId != 0) {
                        try {
                            val intent = android.content.Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                            intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                            intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "No system equalizer found", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Audio session not ready", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OmniColors.Primary)
            ) {
                Text("Open System Equalizer")
            }
        }
    }
}

@Composable
private fun LyricsGlassPanel(playerConnection: PlayerConnection?, modifier: Modifier = Modifier) {
    val lyricsFlow = playerConnection?.currentLyrics ?: kotlinx.coroutines.flow.flowOf(null)
    val lyricsEntity by lyricsFlow.collectAsState(initial = null)
    val currentMediaId = playerConnection?.currentMediaId
    var position by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playerConnection) { while (true) { position = playerConnection?.currentPosition ?: 0L; delay(50) } }
    val parsedLines = remember(lyricsEntity?.id, currentMediaId) { 
        lyricsEntity?.lyrics?.let { 
            if (LyricsUtils.isTtml(it)) LyricsUtils.parseTtml(it) else LyricsUtils.parseLyrics(it) 
        } ?: emptyList() 
    }
    val currentLineIndex = remember(position, parsedLines.size) { if (parsedLines.isEmpty()) -1 else LyricsUtils.findCurrentLineIndex(parsedLines, position) }
    val listState = rememberLazyListState()
    
    if (parsedLines.isEmpty()) return
    
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
