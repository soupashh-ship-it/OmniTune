package com.omnitune.app.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.Player
import com.omnitune.app.R
import com.omnitune.app.models.LyricsLine
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.ui.component.OmniLoadingPulse
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsBottomSheet(
    playerConnection: PlayerConnection?,
    onDismissRequest: () -> Unit,
    viewModel: LyricsViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsState()
    val mediaMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsState(initial = null)
    
    LaunchedEffect(mediaMetadata) {
        val metadata = mediaMetadata
        if (metadata != null && metadata.title.isNotBlank()) {
            val songId = metadata.id
            val title = metadata.title
            val artist = metadata.artists.joinToString(", ") { it.name }
            val duration = metadata.duration.toLong()
            viewModel.loadLyrics(songId, title, artist, duration)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.88f),
        containerColor = OmniColors.OmniBackgroundElevated,
        shape = OmniShapes.ExtraLarge,
    ) {
        val trackTitle = mediaMetadata?.title?.takeIf { it.isNotBlank() }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OmniSpacing.section),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Lyrics",
                style = OmniTextStyles.sectionTitle,
                color = OmniColors.TextPrimary,
                modifier = Modifier.padding(bottom = OmniSpacing.medium)
            )

            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "lyrics_content"
            ) { state ->
                when (state) {
                    is LyricsUiState.Idle, is LyricsUiState.Loading -> {
                        LyricsLoadingState(trackTitle = trackTitle)
                    }
                    is LyricsUiState.Error -> {
                        ErrorState(
                            message = state.message,
                            onRetry = {
                                val metadata = mediaMetadata
                                if (metadata != null) {
                                    viewModel.loadLyrics(
                                        metadata.id,
                                        metadata.title,
                                        metadata.artists.joinToString(", ") { it.name },
                                        metadata.duration.toLong()
                                    )
                                }
                            }
                        )
                    }
                    is LyricsUiState.NoLyrics -> {
                        EmptyState(message = "No lyrics found for this track.")
                    }
                    is LyricsUiState.Success -> {
                        LyricsContent(
                            lines = state.lines,
                            playerConnection = playerConnection
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsLoadingState(trackTitle: String?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OmniLoadingPulse(size = 44.dp, color = OmniColors.OmniAccentSecondary)
        Spacer(modifier = Modifier.height(OmniSpacing.large))
        Text(
            text = "Finding lyrics",
            style = OmniTextStyles.sectionTitle,
            color = OmniColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(OmniSpacing.compact))
        Text(
            text = trackTitle ?: "Checking available lyrics providers",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = OmniSpacing.section),
        )
    }
}

@Composable
private fun LyricsContent(
    lines: List<LyricsLine>,
    playerConnection: PlayerConnection?
) {
    val isSynced = lines.any { it.timestamp > 0L }
    val playbackState by (playerConnection?.playbackState ?: flowOf(Player.STATE_IDLE)).collectAsState(initial = Player.STATE_IDLE)
    var currentPosition by remember { mutableLongStateOf(0L) }
    val listState = rememberLazyListState()

    var isManualScrolling by remember { mutableStateOf(false) }
    var forceReturn by remember { mutableStateOf(false) }

    val isDragged by listState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isDragged, forceReturn) {
        if (forceReturn) {
            isManualScrolling = false
            forceReturn = false
        } else if (isDragged) {
            isManualScrolling = true
        } else if (isManualScrolling) {
            delay(3000)
            isManualScrolling = false
        }
    }

    if (isSynced) {
        LaunchedEffect(playerConnection, playbackState) {
            while (true) {
                if (playerConnection != null && playbackState != Player.STATE_ENDED) {
                    currentPosition = playerConnection.currentPosition
                }
                delay(200)
            }
        }
    }

    val activeIndex = if (isSynced) {
        lines.indexOfFirst { line ->
            val index = lines.indexOf(line)
            val nextTimestamp = lines.getOrNull(index + 1)?.timestamp ?: Long.MAX_VALUE
            currentPosition in line.timestamp until nextTimestamp
        }
    } else -1

    LaunchedEffect(activeIndex, isManualScrolling) {
        if (activeIndex >= 0 && !isManualScrolling) {
            val centerOffset = listState.layoutInfo.viewportSize.height / 2
            listState.animateScrollToItem(activeIndex, scrollOffset = -centerOffset / 2)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp, top = OmniSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)
        ) {
            itemsIndexed(lines) { index, line ->
                val isActive = index == activeIndex
                
                val textColor = if (isActive) OmniColors.TextPrimary else OmniColors.TextTertiary
                val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                val textStyle = if (isActive) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium

                Text(
                    text = line.text,
                    style = textStyle,
                    color = textColor,
                    fontWeight = fontWeight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OmniSpacing.medium)
                )
            }
        }

        if (isManualScrolling && isSynced) {
            Button(
                onClick = { forceReturn = true },
                colors = ButtonDefaults.buttonColors(containerColor = OmniColors.OmniAccentSecondary),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text("Return to current lyric", color = OmniColors.TextOnAccent)
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = OmniColors.TextTertiary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(OmniSpacing.medium))
        Text(
            text = "Couldn't load lyrics",
            style = OmniTextStyles.screenTitle,
            color = OmniColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(OmniSpacing.compact))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = OmniSpacing.section)
        )
        Spacer(modifier = Modifier.height(OmniSpacing.large))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = OmniColors.OmniAccentSecondary)
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = OmniColors.TextTertiary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(OmniSpacing.medium))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = OmniSpacing.section)
        )
    }
}
