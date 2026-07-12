package com.omnitune.app.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.R
import com.omnitune.app.constants.LyricsScrollKey
import com.omnitune.app.models.LyricsLine
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.ui.component.OmniLoadingPulse
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.utils.rememberPreference
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Drag handle indicator
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OmniColors.TextDisabled.copy(alpha = 0.30f)),
            )

            // Header with track info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Lyrics",
                    style = OmniTextStyles.sectionTitle,
                    color = OmniColors.TextPrimary,
                )
                if (trackTitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = trackTitle,
                        style = OmniTextStyles.metadata,
                        color = OmniColors.TextSecondary,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(OmniSpacing.medium))

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
                        val autoScrollLyrics by rememberPreference(LyricsScrollKey, true)
                        val useLyricsV2 by com.omnitune.app.utils.rememberPreference(com.omnitune.app.constants.UseLyricsV2Key, false)
                        if (useLyricsV2) {
                            com.omnitune.app.ui.component.LyricsV2(
                                sliderPositionProvider = { playerConnection?.player?.currentPosition },
                                fallbackLines = state.lines,
                            )
                        } else {
                            LyricsContent(
                                lines = state.lines,
                                playerConnection = playerConnection,
                                autoScrollEnabled = autoScrollLyrics,
                            )
                        }
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
    playerConnection: PlayerConnection?,
    autoScrollEnabled: Boolean,
) {
    val isSynced = lines.any { it.timestamp >= 0L }
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

    LaunchedEffect(playerConnection, isSynced) {
        if (isSynced) {
            while (true) {
                currentPosition = playerConnection?.currentPosition?.coerceAtLeast(0L) ?: 0L
                delay(100)
            }
        }
    }

    val activeIndex = if (isSynced) {
        lines.indexOfLast { it.timestamp <= currentPosition }
    } else -1

    val targetScrollIndex = if (
        autoScrollEnabled && activeIndex >= 0 && !isManualScrolling
    ) activeIndex else -1

    LaunchedEffect(lines, targetScrollIndex) {
        if (targetScrollIndex >= 0) {
            while (listState.layoutInfo.viewportSize.height == 0) delay(16)
            val firstItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            val itemHeight = firstItem?.size ?: 0
            val centerOffset = (listState.layoutInfo.viewportSize.height / 2) - (itemHeight / 2)
            listState.animateScrollToItem(targetScrollIndex, scrollOffset = -centerOffset.coerceAtLeast(0))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp, top = OmniSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
        ) {
            itemsIndexed(lines) { index, line ->
                val isActive = index == activeIndex

                LyricsLineItem(
                    text = line.text,
                    isActive = isActive,
                    isSynced = isSynced,
                )
            }
        }

        // Return-to-current button with fade animation
        AnimatedVisibility(
            visible = isManualScrolling && isSynced,
            enter = fadeIn(spring(stiffness = 300f)),
            exit = fadeOut(spring(stiffness = 300f)),
        ) {
            Button(
                onClick = { forceReturn = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = OmniColors.OmniAccentSecondary,
                    contentColor = OmniColors.TextOnAccent,
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            ) {
                Text("Return to current lyric", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LyricsLineItem(
    text: String,
    isActive: Boolean,
    isSynced: Boolean,
) {
    // Active-line glow animation
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive && isSynced) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "lyrics_line_glow",
    )

    // Breathing pulse on the active line background
    var pulsePhase by remember { mutableStateOf(false) }
    LaunchedEffect(isActive, isSynced) {
        if (isActive && isSynced) {
            while (true) {
                pulsePhase = !pulsePhase
                delay(600)
            }
        } else {
            pulsePhase = false
        }
    }
    val pulseAlpha by animateFloatAsState(
        targetValue = if (pulsePhase) 0.14f else 0.06f,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "lyrics_pulse_alpha",
    )

    val textColor = if (isActive && isSynced) {
        OmniColors.TextPrimary
    } else {
        OmniColors.TextTertiary
    }
    val textWeight = if (isActive && isSynced) FontWeight.Bold else FontWeight.Normal
    val textSize = if (isActive && isSynced) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.titleMedium
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OmniSpacing.medium)
            .clip(OmniShapes.Medium)
            .then(
                if (isActive && isSynced) {
                    Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(
                                OmniColors.OmniAccentPrimary.copy(alpha = glowAlpha * 0.10f),
                                OmniColors.OmniAccentSecondary.copy(alpha = pulseAlpha),
                            )
                        )
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (isActive && isSynced) {
                    Modifier.graphicsLayer {
                        scaleX = 1f + glowAlpha * 0.015f
                        scaleY = 1f + glowAlpha * 0.015f
                    }
                } else {
                    Modifier
                }
            )
            .padding(
                vertical = if (isActive && isSynced) 10.dp else 6.dp,
                horizontal = 12.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = textSize,
            color = textColor,
            fontWeight = textWeight,
            textAlign = TextAlign.Center,
        )
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
            colors = ButtonDefaults.buttonColors(
                containerColor = OmniColors.OmniAccentSecondary,
                contentColor = OmniColors.TextOnAccent,
            )
        ) {
            Text("Retry", fontWeight = FontWeight.Bold)
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
            painter = painterResource(R.drawable.ic_lyrics),
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
