package com.omnitune.app.ui.player

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.constants.EnableBetterLyricsKey
import com.omnitune.app.constants.EnableKugouKey
import com.omnitune.app.constants.EnableLrcLibKey
import com.omnitune.app.constants.EnableSimpMusicLyricsKey
import com.omnitune.app.constants.LyricsBlurKey
import com.omnitune.app.constants.LyricsAnimationTypeKey
import com.omnitune.app.constants.LyricsLineSpacingKey
import com.omnitune.app.constants.LyricsTextPositionKey
import com.omnitune.app.constants.LyricsTextSizeKey
import com.omnitune.app.constants.PreferredLyricsProvider
import com.omnitune.app.constants.PreferredLyricsProviderKey
import com.omnitune.app.models.LyricsAnimationType
import com.omnitune.app.models.LyricsLine
import com.omnitune.app.models.LyricsTextPosition
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.playback.PlayerProgressState
import com.omnitune.app.ui.component.BounceButton
import com.omnitune.app.ui.component.DynamicLyricsBackground
import com.omnitune.app.ui.component.LoadingIndicator
import com.omnitune.app.ui.utils.LyricsShareExporter
import com.omnitune.app.ui.utils.MoodDetector
import com.omnitune.app.ui.utils.displayLabel
import com.omnitune.app.utils.TimeUtil
import com.omnitune.app.utils.rememberEnumPreference
import com.omnitune.app.utils.rememberPreference
import java.util.Locale
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsBottomSheet(
    playerConnection: PlayerConnection?,
    onDismissRequest: () -> Unit,
    viewModel: LyricsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsStateWithLifecycle(initialValue = null)
    val isPlaying by (playerConnection?.isPlaying ?: flowOf(false)).collectAsStateWithLifecycle(initialValue = false)
    val metadataDurationMs = remember(mediaMetadata?.id, mediaMetadata?.duration) {
        mediaMetadata?.duration?.toLong()?.takeIf { it > 0L }?.times(1000L) ?: 0L
    }
    val initialProgress = remember(metadataDurationMs) {
        PlayerProgressState(durationMs = metadataDurationMs)
    }
    val progressState by remember(playerConnection, mediaMetadata?.id, mediaMetadata?.duration) {
        playerConnection?.progressState ?: flowOf(initialProgress)
    }.collectAsStateWithLifecycle(initialValue = initialProgress)

    var lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsTextPosition.CENTER)
    val lyricsAnimationType by rememberEnumPreference(LyricsAnimationTypeKey, LyricsAnimationType.WORD)
    var lyricsFontSize by rememberPreference(LyricsTextSizeKey, 26f)
    var lyricsLineSpacing by rememberPreference(LyricsLineSpacingKey, 1.5f)
    var lyricsBlur by rememberPreference(LyricsBlurKey, 2.5f)
    var preferredProvider by rememberPreference(PreferredLyricsProviderKey, PreferredLyricsProvider.BETTER_LYRICS.name)

    val lrcLibEnabled by rememberPreference(EnableLrcLibKey, true)
    val betterLyricsEnabled by rememberPreference(EnableBetterLyricsKey, true)
    val kugouEnabled by rememberPreference(EnableKugouKey, true)
    val simpMusicEnabled by rememberPreference(EnableSimpMusicLyricsKey, true)

    LaunchedEffect(
        mediaMetadata?.id,
        mediaMetadata?.title,
        mediaMetadata?.artists,
        mediaMetadata?.duration,
    ) {
        val metadata = mediaMetadata
        if (metadata != null && metadata.title.isNotBlank()) {
            viewModel.loadLyrics(
                songId = metadata.id,
                title = metadata.title,
                artist = metadata.artists.joinToString(", ") { it.name },
                duration = metadata.duration.toLong(),
            )
        }
    }

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var syncOffset by remember { mutableLongStateOf(0L) }

    val songTitle = mediaMetadata?.title.orEmpty()
    val artistName = mediaMetadata?.artists?.joinToString(", ") { it.name }.orEmpty()
    val artworkUrl = mediaMetadata?.thumbnailUrl
    val currentPosition = progressState.positionMs
    val duration = progressState.durationMs.takeIf { it > 0L } ?: metadataDurationMs
    val loadedLines = (uiState as? LyricsUiState.Success)?.lines.orEmpty()
    val donorLyrics = remember(loadedLines) { loadedLines.toDonorLyrics() }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val providerOptions = remember {
        listOf(
            LyricsProviderOption(PreferredLyricsProvider.BETTER_LYRICS.name, "BetterLyrics"),
            LyricsProviderOption(PreferredLyricsProvider.LRCLIB.name, "LRCLIB"),
            LyricsProviderOption(PreferredLyricsProvider.KUGOU.name, "KuGou"),
            LyricsProviderOption(PreferredLyricsProvider.SIMPMUSIC.name, "SimpMusic"),
        )
    }
    val providerEnabled = remember(lrcLibEnabled, betterLyricsEnabled, kugouEnabled, simpMusicEnabled) {
        mapOf(
            PreferredLyricsProvider.BETTER_LYRICS.name to betterLyricsEnabled,
            PreferredLyricsProvider.LRCLIB.name to lrcLibEnabled,
            PreferredLyricsProvider.KUGOU.name to kugouEnabled,
            PreferredLyricsProvider.SIMPMUSIC.name to simpMusicEnabled,
        )
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val songId = mediaMetadata?.id
        if (songId.isNullOrBlank()) {
            Toast.makeText(context, "No current song selected", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        val content = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()

        if (content.isNullOrBlank()) {
            Toast.makeText(context, "Selected lyrics file is empty", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.importLocalLyrics(songId, content)
            Toast.makeText(context, "Lyrics imported", Toast.LENGTH_SHORT).show()
        }
    }

    LyricsScreenSurface(
        lyrics = donorLyrics,
        rawLines = loadedLines,
        isFetching = uiState is LyricsUiState.Idle || uiState is LyricsUiState.Loading,
        errorMessage = (uiState as? LyricsUiState.Error)?.message,
        currentTimeProvider = { currentPosition + syncOffset },
        artworkUrl = artworkUrl,
        onClose = onDismissRequest,
        isDarkTheme = isDarkTheme,
        onSeekTo = { playerConnection?.seekTo(it.coerceAtLeast(0L)) },
        songTitle = songTitle,
        artistName = artistName,
        duration = duration,
        isPlaying = isPlaying,
        onPlayPause = {
            if (isPlaying) {
                playerConnection?.pause()
            } else {
                playerConnection?.playOrResolveCurrent()
            }
        },
        onNext = { playerConnection?.seekToNext() },
        onPrevious = { playerConnection?.seekToPrevious() },
        selectedProvider = preferredProvider,
        providerOptions = providerOptions,
        enabledProviders = providerEnabled,
        onProviderChange = { preferredProvider = it },
        onImportLyrics = { filePicker.launch("*/*") },
        onRetry = {
            val metadata = mediaMetadata
            if (metadata != null) {
                viewModel.loadLyrics(
                    songId = metadata.id,
                    title = metadata.title,
                    artist = metadata.artists.joinToString(", ") { it.name },
                    duration = metadata.duration.toLong(),
                )
            }
        },
        lyricsTextPosition = lyricsTextPosition,
        lyricsAnimationType = lyricsAnimationType,
        lyricsLineSpacing = lyricsLineSpacing,
        lyricsFontSize = lyricsFontSize,
        lyricsBlur = lyricsBlur,
        onLineSpacingChange = { lyricsLineSpacing = it },
        onFontSizeChange = { lyricsFontSize = it },
        onBlurChange = { lyricsBlur = it },
        onTextPositionChange = { lyricsTextPosition = it },
        syncOffset = syncOffset,
        onSyncOffsetChange = { syncOffset = it },
        showSettingsSheet = showSettingsSheet,
        showShareSheet = showShareSheet,
        onShowSettingsSheet = { showSettingsSheet = true },
        onDismissSettingsSheet = { showSettingsSheet = false },
        onShowShareSheet = {
            if (loadedLines.isEmpty()) {
                Toast.makeText(context, "No lyrics to share", Toast.LENGTH_SHORT).show()
            } else {
                showShareSheet = true
            }
        },
        onDismissShareSheet = { showShareSheet = false },
        sheetState = sheetState,
        scope = scope,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsScreenSurface(
    lyrics: DonorLyrics?,
    rawLines: List<LyricsLine>,
    isFetching: Boolean,
    errorMessage: String?,
    currentTimeProvider: () -> Long,
    artworkUrl: String?,
    onClose: () -> Unit,
    isDarkTheme: Boolean,
    onSeekTo: (Long) -> Unit,
    songTitle: String,
    artistName: String,
    duration: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    selectedProvider: String,
    providerOptions: List<LyricsProviderOption>,
    enabledProviders: Map<String, Boolean>,
    onProviderChange: (String) -> Unit,
    onImportLyrics: () -> Unit,
    onRetry: () -> Unit,
    lyricsTextPosition: LyricsTextPosition,
    lyricsAnimationType: LyricsAnimationType,
    lyricsLineSpacing: Float,
    lyricsFontSize: Float,
    lyricsBlur: Float,
    onLineSpacingChange: (Float) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onTextPositionChange: (LyricsTextPosition) -> Unit,
    syncOffset: Long,
    onSyncOffsetChange: (Long) -> Unit,
    showSettingsSheet: Boolean,
    showShareSheet: Boolean,
    onShowSettingsSheet: () -> Unit,
    onDismissSettingsSheet: () -> Unit,
    onShowShareSheet: () -> Unit,
    onDismissShareSheet: () -> Unit,
    sheetState: androidx.compose.material3.SheetState,
    scope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val animatedBgColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "bgColor",
    )
    val animatedTextColor by animateColorAsState(
        targetValue = textColor,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "textColor",
    )
    val currentStyle = remember(songTitle, artistName, lyrics) {
        MoodDetector.detectStyle(
            title = songTitle,
            artist = artistName,
            lyricsText = lyrics?.lines?.joinToString(" ") { it.text }.orEmpty(),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(animatedBgColor)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, _ -> }
            },
    ) {
        DynamicLyricsBackground(
            artworkUrl = artworkUrl,
            style = currentStyle,
            isDarkTheme = isDarkTheme,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            LyricsHeader(
                songTitle = songTitle,
                artistName = artistName,
                textColor = animatedTextColor,
                onSettingsClick = onShowSettingsSheet,
                onShareClick = onShowShareSheet,
                onClose = onClose,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isFetching -> LyricsLoadingState(textColor = animatedTextColor)
                    errorMessage != null -> LyricsUnavailableState(
                        title = "Couldn't load lyrics",
                        message = errorMessage,
                        textColor = animatedTextColor,
                        onImportLyrics = onImportLyrics,
                        onRetry = onRetry,
                    )
                    lyrics == null || lyrics.lines.isEmpty() -> LyricsUnavailableState(
                        title = "Lyrics not available",
                        message = "Import a local .lrc or .txt file for this song.",
                        textColor = animatedTextColor,
                        onImportLyrics = onImportLyrics,
                        onRetry = onRetry,
                    )
                    else -> LyricsList(
                        lyrics = lyrics,
                        rawLines = rawLines,
                        currentTimeProvider = currentTimeProvider,
                        isDarkTheme = isDarkTheme,
                        onSeekTo = { position -> onSeekTo(position - syncOffset) },
                        songTitle = songTitle,
                        artistName = artistName,
                        artworkUrl = artworkUrl,
                        textPosition = lyricsTextPosition,
                        animationType = lyricsAnimationType,
                        fontSize = lyricsFontSize,
                        lineSpacingMultiplier = lyricsLineSpacing,
                        blurIntensity = lyricsBlur,
                    )
                }
            }

            if (lyrics?.sourceCredit != null) {
                Surface(
                    color = textColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = lyrics.sourceCredit,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (duration > 0L) {
                LyricsPlaybackFooter(
                    currentTimeProvider = currentTimeProvider,
                    duration = duration,
                    textColor = animatedTextColor,
                    backgroundColor = animatedBgColor,
                    isPlaying = isPlaying,
                    onSeekTo = onSeekTo,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                )
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showSettingsSheet) {
            LyricsSettingsSheet(
                sheetState = sheetState,
                selectedProvider = selectedProvider,
                providerOptions = providerOptions,
                enabledProviders = enabledProviders,
                lyricsTextPosition = lyricsTextPosition,
                lyricsFontSize = lyricsFontSize,
                lyricsLineSpacing = lyricsLineSpacing,
                lyricsBlur = lyricsBlur,
                syncOffset = syncOffset,
                onDismiss = onDismissSettingsSheet,
                onProviderChange = onProviderChange,
                onTextPositionChange = onTextPositionChange,
                onFontSizeChange = onFontSizeChange,
                onLineSpacingChange = onLineSpacingChange,
                onBlurChange = onBlurChange,
                onSyncOffsetChange = onSyncOffsetChange,
                onImportLyrics = onImportLyrics,
            )
        }

        if (showShareSheet) {
            LyricsShareSheet(
                sheetState = sheetState,
                rawLines = rawLines,
                songTitle = songTitle,
                artistName = artistName,
                onDismiss = onDismissShareSheet,
                scope = scope,
            )
        }
    }
}

@Composable
private fun LyricsHeader(
    songTitle: String,
    artistName: String,
    textColor: Color,
    onSettingsClick: () -> Unit,
    onShareClick: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LyricsHeaderButton(onClick = onSettingsClick, textColor = textColor) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "Settings",
                    tint = textColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            LyricsHeaderButton(onClick = onShareClick, textColor = textColor) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = textColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = songTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                ),
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artistName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = textColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        LyricsHeaderButton(onClick = onClose, textColor = textColor) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = textColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun LyricsHeaderButton(
    onClick: () -> Unit,
    textColor: Color,
    content: @Composable () -> Unit,
) {
    BounceButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
    ) { isPressed ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(textColor.copy(alpha = if (isPressed) 0.15f else 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun LyricsLoadingState(textColor: Color) {
    val loadingMessages = remember {
        listOf(
            "Polishing the lyrics...",
            "Preparing your concert experience...",
            "Tuning the vocal cords...",
            "Finding the rhythm...",
            "Teaching the app how to sing...",
            "Gathering the words...",
            "Fetching the soul of the song...",
            "The lyrics are taking a scenic route...",
        )
    }
    val currentMessage = remember { loadingMessages.random() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp),
    ) {
        LoadingIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(52.dp),
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = currentMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )
    }
}

@Composable
private fun LyricsUnavailableState(
    title: String,
    message: String,
    textColor: Color,
    onImportLyrics: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = textColor.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LyricsPillButton(
                label = "Retry",
                icon = Icons.Default.Tune,
                textColor = textColor,
                onClick = onRetry,
            )
            LyricsPillButton(
                label = "Import Local Lyrics",
                icon = Icons.Default.Description,
                textColor = textColor,
                onClick = onImportLyrics,
            )
        }
    }
}

@Composable
private fun LyricsPillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    textColor: Color,
    onClick: () -> Unit,
) {
    BounceButton(
        onClick = onClick,
        modifier = Modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
    ) { isPressed ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(textColor.copy(alpha = if (isPressed) 0.15f else 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = textColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = textColor,
                )
            }
        }
    }
}

@Composable
private fun LyricsPlaybackFooter(
    currentTimeProvider: () -> Long,
    duration: Long,
    textColor: Color,
    backgroundColor: Color,
    isPlaying: Boolean,
    onSeekTo: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .background(textColor.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        var sliderPosition by remember { mutableStateOf<Float?>(null) }
        val progress = sliderPosition ?: (currentTimeProvider().toFloat() / duration.toFloat()).coerceIn(0f, 1f)

        Slider(
            value = progress,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                sliderPosition?.let {
                    onSeekTo((it * duration).toLong())
                    sliderPosition = null
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            colors = SliderDefaults.colors(
                thumbColor = textColor,
                activeTrackColor = textColor.copy(alpha = 0.8f),
                inactiveTrackColor = textColor.copy(alpha = 0.15f),
            ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = TimeUtil.formatPosition(
                    if (sliderPosition != null) (sliderPosition!! * duration).toLong() else currentTimeProvider(),
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = textColor.copy(alpha = 0.5f),
            )
            Text(
                text = TimeUtil.formatPosition(duration),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = textColor.copy(alpha = 0.5f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            BounceButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) { isPressed ->
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = textColor.copy(alpha = if (isPressed) 0.6f else 0.9f),
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(modifier = Modifier.width(28.dp))
            BounceButton(
                onClick = onPlayPause,
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(textColor, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = backgroundColor,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(28.dp))
            BounceButton(onClick = onNext, modifier = Modifier.size(48.dp)) { isPressed ->
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = textColor.copy(alpha = if (isPressed) 0.6f else 0.9f),
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsSettingsSheet(
    sheetState: androidx.compose.material3.SheetState,
    selectedProvider: String,
    providerOptions: List<LyricsProviderOption>,
    enabledProviders: Map<String, Boolean>,
    lyricsTextPosition: LyricsTextPosition,
    lyricsFontSize: Float,
    lyricsLineSpacing: Float,
    lyricsBlur: Float,
    syncOffset: Long,
    onDismiss: () -> Unit,
    onProviderChange: (String) -> Unit,
    onTextPositionChange: (LyricsTextPosition) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onSyncOffsetChange: (Long) -> Unit,
    onImportLyrics: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Black.copy(alpha = 0.92f),
        contentColor = Color.White,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f))
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Customize Lyrics",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Tune)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(20.dp),
            ) {
                SettingsSlider(
                    label = "Font Size",
                    value = lyricsFontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 16f..50f,
                    icon = Icons.Default.FormatSize,
                )
                Spacer(modifier = Modifier.height(24.dp))
                SettingsSlider(
                    label = "Line Spacing",
                    value = lyricsLineSpacing,
                    onValueChange = onLineSpacingChange,
                    valueRange = 1.0f..2.5f,
                    icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                )
                Spacer(modifier = Modifier.height(24.dp))
                SettingsSlider(
                    label = "Blur Intensity",
                    value = lyricsBlur,
                    onValueChange = onBlurChange,
                    valueRange = 0f..12f,
                    icon = Icons.Default.BlurOn,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            LyricsAlignmentPicker(
                lyricsTextPosition = lyricsTextPosition,
                onTextPositionChange = onTextPositionChange,
            )

            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionHeader(title = "Sync Correction", icon = Icons.Default.Tune)
            LyricsSyncOffsetControl(
                syncOffset = syncOffset,
                onSyncOffsetChange = onSyncOffsetChange,
            )

            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionHeader(title = "Local Lyrics", icon = Icons.Default.LibraryMusic)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(20.dp),
            ) {
                Text(
                    text = "Add your own .lrc or .txt file for this song.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                BounceButton(
                    onClick = onImportLyrics,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { isPressed ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = if (isPressed) 0.15f else 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Import .lrc / .txt",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            LyricsSourcePicker(
                selectedProvider = selectedProvider,
                providerOptions = providerOptions,
                enabledProviders = enabledProviders,
                onProviderChange = onProviderChange,
            )
        }
    }
}

@Composable
private fun LyricsAlignmentPicker(
    lyricsTextPosition: LyricsTextPosition,
    onTextPositionChange: (LyricsTextPosition) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp),
    ) {
        Text(
            text = "Alignment",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LyricsTextPosition.entries.forEach { position ->
                val isSelected = lyricsTextPosition == position
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onTextPositionChange(position) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = position.displayLabel(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsSyncOffsetControl(
    syncOffset: Long,
    onSyncOffsetChange: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LyricsOffsetButton(label = "-0.5s", onClick = { onSyncOffsetChange(syncOffset - 500L) })
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${if (syncOffset > 0) "+" else ""}${syncOffset}ms",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = if (syncOffset != 0L) MaterialTheme.colorScheme.primary else Color.White,
            )
            if (syncOffset != 0L) {
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSyncOffsetChange(0L) }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        LyricsOffsetButton(label = "+0.5s", onClick = { onSyncOffsetChange(syncOffset + 500L) })
    }
}

@Composable
private fun LyricsOffsetButton(label: String, onClick: () -> Unit) {
    BounceButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(14.dp),
    ) { isPressed ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = if (isPressed) 0.15f else 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun LyricsSourcePicker(
    selectedProvider: String,
    providerOptions: List<LyricsProviderOption>,
    enabledProviders: Map<String, Boolean>,
    onProviderChange: (String) -> Unit,
) {
    SettingsSectionHeader(title = "Lyrics Source", icon = Icons.Default.LibraryMusic)
    var expandedProvider by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { expandedProvider = !expandedProvider }
            .padding(20.dp)
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.LibraryMusic, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current Provider",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
                Text(
                    text = providerOptions.firstOrNull { it.id == selectedProvider }?.displayName ?: selectedProvider,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
            Icon(
                imageVector = if (expandedProvider) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
            )
        }

        if (expandedProvider) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            providerOptions.forEach { provider ->
                val isEnabled = enabledProviders[provider.id] ?: true
                val isSelected = provider.id == selectedProvider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = isEnabled) {
                            onProviderChange(provider.id)
                            expandedProvider = false
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .alpha(if (isEnabled) 1f else 0.4f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        enabled = isEnabled,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color.White,
                            unselectedColor = Color.White.copy(alpha = 0.3f),
                        ),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = provider.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsShareSheet(
    sheetState: androidx.compose.material3.SheetState,
    rawLines: List<LyricsLine>,
    songTitle: String,
    artistName: String,
    onDismiss: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val context = LocalContext.current
    val lines = remember(rawLines) { rawLines.filter { it.text.isNotBlank() }.map { it.text } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Share Lyrics",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                fontWeight = FontWeight.Bold,
            )

            ListItem(
                headlineContent = { Text("Share as Text") },
                leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
                modifier = Modifier.clickable {
                    if (!LyricsShareExporter.shareText(context, lines, songTitle, artistName)) {
                        Toast.makeText(context, "Unable to open share sheet", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
            )

            ListItem(
                headlineContent = { Text("Share as .lrc") },
                leadingContent = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                modifier = Modifier.clickable {
                    scope.launch {
                        val uri = LyricsShareExporter.exportLrc(context, rawLines, songTitle, artistName)
                        if (uri != null) {
                            LyricsShareExporter.shareUri(context, uri, "text/plain", "Share Lyrics (.lrc)")
                        } else {
                            Toast.makeText(context, "Could not export lyrics", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                },
            )

            ListItem(
                headlineContent = { Text("Export as PDF") },
                leadingContent = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                modifier = Modifier.clickable {
                    scope.launch {
                        val uri = LyricsShareExporter.exportPdf(context, lines, songTitle, artistName)
                        if (uri != null) {
                            LyricsShareExporter.shareUri(context, uri, "application/pdf", "Share Lyrics PDF")
                        } else {
                            Toast.makeText(context, "Could not export lyrics", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            ),
            color = Color.White.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.getDefault(), "%.1f", value),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White.copy(alpha = 0.5f),
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(top = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.8f),
                inactiveTrackColor = Color.White.copy(alpha = 0.1f),
            ),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun LyricsList(
    lyrics: DonorLyrics,
    rawLines: List<LyricsLine>,
    currentTimeProvider: () -> Long,
    isDarkTheme: Boolean,
    onSeekTo: (Long) -> Unit,
    songTitle: String,
    artistName: String,
    artworkUrl: String?,
    textPosition: LyricsTextPosition,
    animationType: LyricsAnimationType,
    fontSize: Float,
    lineSpacingMultiplier: Float,
    blurIntensity: Float,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val animatedTextColor by animateColorAsState(
        targetValue = textColor,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "listTextColor",
    )
    val currentTime = currentTimeProvider()

    val textAlign = when (textPosition) {
        LyricsTextPosition.CENTER -> TextAlign.Center
        LyricsTextPosition.LEFT -> TextAlign.Start
        LyricsTextPosition.RIGHT -> TextAlign.End
    }
    val flowArrangement = when (textPosition) {
        LyricsTextPosition.CENTER -> Arrangement.Center
        LyricsTextPosition.LEFT -> Arrangement.Start
        LyricsTextPosition.RIGHT -> Arrangement.End
    }

    var activeLineIndex by remember { mutableIntStateOf(-1) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
    var isSharing by remember { mutableStateOf(false) }

    LaunchedEffect(lyrics) {
        isSelectionMode = false
        selectedIndices = emptySet()
    }

    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var lastUserInteractionTime by remember { mutableLongStateOf(0L) }
    var isAutoScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(isDragged, listState.isScrollInProgress) {
        if (isDragged || (listState.isScrollInProgress && !isAutoScrolling)) {
            lastUserInteractionTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(currentTime, lyrics, isSelectionMode) {
        if (lyrics.isSynced && !isSelectionMode) {
            val index = lyrics.lines.indexOfLast { it.startTimeMs <= currentTime }
            if (index != activeLineIndex && index >= 0) {
                activeLineIndex = index
                val shouldAutoScroll = System.currentTimeMillis() - lastUserInteractionTime > 4000

                if (shouldAutoScroll) {
                    try {
                        isAutoScrolling = true
                        val topOffset = listState.layoutInfo.viewportSize.height / 3
                        listState.animateScrollToItem(index = index, scrollOffset = -topOffset)
                    } finally {
                        isAutoScrolling = false
                    }
                }
            }
        }
    }

    val isActiveLineVisible by remember {
        derivedStateOf {
            activeLineIndex < 0 || listState.layoutInfo.visibleItemsInfo.any { it.index == activeLineIndex }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 120.dp, bottom = 450.dp),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.15f to Color.Black,
                            0.85f to Color.Black,
                            1f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            itemsIndexed(lyrics.lines, key = { index, _ -> index }) { index, line ->
                val isActive = if (lyrics.isSynced) index == activeLineIndex else true
                val isSelected = selectedIndices.contains(index)
                val targetAlpha = if (isSelectionMode) {
                    if (isSelected) 1f else 0.3f
                } else {
                    if (isActive) 1f else 0.25f
                }
                val alpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                    label = "alpha",
                )
                val scale by animateFloatAsState(
                    targetValue = if (isActive && lyrics.isSynced && !isSelectionMode) 1.08f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                    label = "scale",
                )
                val blurRadius by animateFloatAsState(
                    targetValue = if (isActive || isSelectionMode || !lyrics.isSynced || activeLineIndex == -1) 0f else blurIntensity,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                    label = "blur",
                )

                if (isSelectionMode) {
                    LyricsSelectionLine(
                        line = line,
                        isSelected = isSelected,
                        alpha = alpha,
                        animatedTextColor = animatedTextColor,
                        fontSize = fontSize,
                        lineSpacingMultiplier = lineSpacingMultiplier,
                        onToggle = {
                            val newSelection = selectedIndices.toMutableSet()
                            if (isSelected) {
                                newSelection.remove(index)
                                if (newSelection.isEmpty()) isSelectionMode = false
                            } else if (newSelection.size < 5) {
                                newSelection.add(index)
                            }
                            selectedIndices = newSelection
                        },
                    )
                } else {
                    LyricsDisplayLine(
                        line = line,
                        currentTime = currentTime,
                        isActive = isActive,
                        isSynced = lyrics.isSynced,
                        scale = scale,
                        blurRadius = blurRadius,
                        animatedTextColor = animatedTextColor,
                        textAlign = textAlign,
                        flowArrangement = flowArrangement,
                        animationType = animationType,
                        fontSize = fontSize,
                        lineSpacingMultiplier = lineSpacingMultiplier,
                        onSeekTo = onSeekTo,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isSelectionMode = true
                            selectedIndices = setOf(index)
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = lyrics.isSynced && !isSelectionMode && activeLineIndex >= 0 && !isActiveLineVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        ) {
            ResumePill {
                coroutineScope.launch {
                    try {
                        isAutoScrolling = true
                        val topOffset = listState.layoutInfo.viewportSize.height / 3
                        listState.animateScrollToItem(
                            index = activeLineIndex.coerceAtLeast(0),
                            scrollOffset = -topOffset,
                        )
                    } finally {
                        isAutoScrolling = false
                        lastUserInteractionTime = 0L
                    }
                }
            }
        }

        if (isSelectionMode) {
            LyricsSelectionShareBar(
                isSharing = isSharing,
                selectedCount = selectedIndices.size,
                onShare = {
                    if (!isSharing && selectedIndices.isNotEmpty()) {
                        isSharing = true
                        coroutineScope.launch {
                            val selectedLines = selectedIndices.sorted().map { lyrics.lines[it].text }
                            val uri = LyricsShareExporter.generateImage(
                                context = context,
                                lyricsLines = selectedLines,
                                songTitle = songTitle,
                                artistName = artistName,
                                artworkUrl = artworkUrl,
                            )
                            isSharing = false
                            if (uri != null) {
                                LyricsShareExporter.shareUri(context, uri, "image/png", "Share Lyrics")
                            } else {
                                val fallbackLines = selectedIndices.sorted().mapNotNull { rawLines.getOrNull(it)?.text }
                                LyricsShareExporter.shareText(context, fallbackLines, songTitle, artistName)
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun LyricsSelectionLine(
    line: DonorLyricsLine,
    isSelected: Boolean,
    alpha: Float,
    animatedTextColor: Color,
    fontSize: Float,
    lineSpacingMultiplier: Float,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 32.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (isSelected) "Selected" else "Unselected",
            tint = if (isSelected) MaterialTheme.colorScheme.primary else animatedTextColor.copy(alpha = 0.3f),
            modifier = Modifier
                .size(24.dp)
                .padding(end = 16.dp),
        )
        Text(
            text = line.text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (fontSize * (lineSpacingMultiplier * 1.1f)).sp,
            ),
            color = animatedTextColor.copy(alpha = alpha),
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LyricsDisplayLine(
    line: DonorLyricsLine,
    currentTime: Long,
    isActive: Boolean,
    isSynced: Boolean,
    scale: Float,
    blurRadius: Float,
    animatedTextColor: Color,
    textAlign: TextAlign,
    flowArrangement: Arrangement.Horizontal,
    animationType: LyricsAnimationType,
    fontSize: Float,
    lineSpacingMultiplier: Float,
    onSeekTo: (Long) -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .blur(blurRadius.dp)
            .combinedClickable(
                onClick = {
                    if (isSynced && line.startTimeMs > 0) {
                        onSeekTo(line.startTimeMs)
                    }
                },
                onLongClick = onLongClick,
            )
            .padding(horizontal = 32.dp, vertical = 10.dp),
    ) {
        val words = line.words
        if (words != null && words.isNotEmpty() && isSynced && animationType == LyricsAnimationType.WORD) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = flowArrangement,
                verticalArrangement = Arrangement.Center,
            ) {
                words.forEach { word ->
                    val wordAlpha by animateFloatAsState(
                        targetValue = if (isActive) {
                            if (currentTime >= word.startTimeMs) 1f else 0.4f
                        } else {
                            0.3f
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "wordAlpha",
                    )
                    Text(
                        text = word.text + " ",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = (if (isActive) fontSize * 1.05f else fontSize).sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = (fontSize * (lineSpacingMultiplier * 1.1f)).sp,
                        ),
                        color = animatedTextColor.copy(alpha = wordAlpha),
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        } else {
            val lineAlpha by animateFloatAsState(
                targetValue = if (isActive || !isSynced) 1f else 0.25f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "lineAlpha",
            )
            Text(
                text = line.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = (if (isActive && isSynced) fontSize * 1.05f else fontSize).sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = (fontSize * (lineSpacingMultiplier * 1.1f)).sp,
                ),
                color = animatedTextColor.copy(alpha = lineAlpha),
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val translation = line.translation
        if (!translation.isNullOrBlank()) {
            val translationAlpha by animateFloatAsState(
                targetValue = if (isActive || !isSynced) 0.7f else 0.2f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "translationAlpha",
            )
            Text(
                text = translation,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (fontSize * 0.75f).sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = (fontSize * lineSpacingMultiplier * 0.85f).sp,
                ),
                color = animatedTextColor.copy(alpha = translationAlpha),
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun ResumePill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Resume",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun LyricsSelectionShareBar(
    isSharing: Boolean,
    selectedCount: Int,
    onShare: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onShare)
            .padding(vertical = 16.dp, horizontal = 24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSharing) {
                LoadingIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Generating...",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Share Lyrics ($selectedCount/5)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private data class LyricsProviderOption(
    val id: String,
    val displayName: String,
)

private data class DonorLyrics(
    val lines: List<DonorLyricsLine>,
    val isSynced: Boolean,
    val sourceCredit: String?,
)

private data class DonorLyricsLine(
    val startTimeMs: Long,
    val text: String,
    val words: List<DonorLyricsWord>? = null,
    val translation: String? = null,
)

private data class DonorLyricsWord(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
)

private fun List<LyricsLine>.toDonorLyrics(): DonorLyrics? {
    if (isEmpty()) return null

    val displayLines = mutableListOf<DonorLyricsLine>()
    forEach { line ->
        if (line.text.isBlank()) return@forEach
        if (line.isTranslated && displayLines.isNotEmpty()) {
            val previous = displayLines.last()
            displayLines[displayLines.lastIndex] = previous.copy(translation = line.text)
        } else {
            displayLines += DonorLyricsLine(
                startTimeMs = line.timestamp,
                text = line.text,
            )
        }
    }

    return DonorLyrics(
        lines = displayLines,
        isSynced = displayLines.any { it.startTimeMs >= 0L },
        sourceCredit = "OmniTune Lyrics",
    )
}
