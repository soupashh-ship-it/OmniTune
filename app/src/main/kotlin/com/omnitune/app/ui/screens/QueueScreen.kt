package com.omnitune.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.omnitune.app.R
import com.omnitune.app.db.entities.FormatEntity
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val QUEUE_ARTWORK_SIZE = 160

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    playerConnection: PlayerConnection?,
    onBack: () -> Unit = {},
    downloadsViewModel: com.omnitune.app.ui.screens.DownloadsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    libraryViewModel: com.omnitune.app.ui.screens.LibraryViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val queueTitle by playerConnection?.queueTitle?.collectAsState() ?: remember { mutableStateOf(null) }
    val currentIndex by playerConnection?.currentMediaItemIndex?.collectAsState() ?: remember { mutableStateOf(-1) }
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
    val queueIndices by playerConnection?.queueIndices?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val currentFormat by playerConnection?.currentFormat?.collectAsState(initial = null) ?: remember { mutableStateOf<FormatEntity?>(null) }
    val sleepTimerRunning by playerConnection?.sleepTimerRunning?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }
    val sleepTimerRemaining by playerConnection?.sleepTimerRemaining?.collectAsState(initial = 0L) ?: remember { mutableStateOf(0L) }

    val itemCount = queueIndices.size
    val currentIndexInQueue = queueIndices.indexOf(currentIndex).coerceAtLeast(0)
    val upcomingIndices = queueIndices.drop(currentIndexInQueue + 1)
    val upcomingCount = upcomingIndices.size

    // Multi-select state
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // Drag-to-reorder state
    val lazyListState = rememberLazyListState()
    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var itemHeights by remember { mutableStateOf(listOf<Int>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OmniColors.OmniBackgroundGradientTop.copy(alpha = 0.82f),
                        OmniColors.OmniBackgroundBase,
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = OmniSpacing.section),
        ) {
            QueueHeader(
                title = queueTitle ?: "Queue",
                itemCount = itemCount,
                upcomingCount = upcomingCount,
                onBack = onBack,
            )

            // Sleep timer banner
            AnimatedVisibility(
                visible = sleepTimerRunning && sleepTimerRemaining > 0,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                SleepTimerBanner(remainingMs = sleepTimerRemaining)
            }

            // Selection mode toolbar
            AnimatedVisibility(
                visible = selectionMode,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                BulkActionBar(
                    selectedCount = selectedIndices.size,
                    onClearSelection = {
                        selectionMode = false
                        selectedIndices.clear()
                    },
                    onRemoveSelected = {
                        playerConnection?.let { pc ->
                            selectedIndices.sortedDescending().forEach { idx ->
                                val windowIndex = upcomingIndices.getOrNull(idx) ?: return@forEach
                                pc.removeMediaItem(windowIndex)
                            }
                            selectedIndices.clear()
                            selectionMode = false
                        }
                    },
                    onAddSelectedToPlaylist = {
                        showAddToPlaylistDialog = true
                    },
                    onDownloadSelected = {
                        playerConnection?.let { pc ->
                            selectedIndices.forEach { idx ->
                                val windowIndex = upcomingIndices.getOrNull(idx) ?: return@forEach
                                val mediaItem = pc.getMediaItemAt(windowIndex)
                                val meta = mediaItem.localConfiguration?.tag as? MediaMetadata
                                val videoId = meta?.id ?: return@forEach
                                val title = meta.title ?: "Unknown"
                                downloadsViewModel.startDownload(videoId, title, null) { _, _ -> }
                            }
                            android.widget.Toast.makeText(context, "Started ${selectedIndices.size} downloads", android.widget.Toast.LENGTH_SHORT).show()
                            selectedIndices.clear()
                            selectionMode = false
                        }
                    },
                )
            }

            if (playerConnection == null || mediaMetadata == null) {
                QueueEmptyState(text = "No items in queue")
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
                ) {
                    item(contentType = "current") {
                        SectionLabel(
                            title = "Now playing",
                            subtitle = "Current track",
                        )
                        NowPlayingCard(
                            mediaMetadata = mediaMetadata!!,
                            format = currentFormat,
                        )
                    }

                    item(contentType = "upNextHeader") {
                        SectionLabel(
                            title = "Up next",
                            subtitle = if (upcomingCount == 1) "1 track queued" else "$upcomingCount tracks queued",
                        )
                    }

                    if (upcomingCount == 0) {
                        item(contentType = "emptyUpcoming") {
                            QueueEmptyState(
                                text = "No upcoming items",
                                compact = true,
                            )
                        }
                    } else {
                        // Track item heights for drag reorder
                        itemsIndexed(
                            items = upcomingIndices,
                            key = { idx, windowIndex -> queueItemKey(playerConnection, windowIndex) },
                            contentType = { _, _ -> "queueItem" },
                        ) { idx, windowIndex ->
                            val mediaItem = playerConnection.getMediaItemAt(windowIndex)
                            val meta = mediaItem.localConfiguration?.tag as? MediaMetadata
                            val title = meta?.title
                                ?: mediaItem.mediaMetadata.title?.toString()
                                ?: "Unknown title"
                            val artists = meta?.artists?.joinToString(", ") { it.name }
                                ?.takeIf { it.isNotBlank() }
                                ?: mediaItem.mediaMetadata.artist?.toString()
                                ?: "Unknown artist"

                            val isSelected = windowIndex in selectedIndices
                            val isDragging = draggedItemIndex == idx && dragOffset != 0f

                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (
                                        dismissValue == SwipeToDismissBoxValue.EndToStart ||
                                        dismissValue == SwipeToDismissBoxValue.StartToEnd
                                    ) {
                                        if (!selectionMode) {
                                            playerConnection.removeMediaItem(windowIndex)
                                            scope.launch {
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Removed \"$title\"",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short,
                                                )
                                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                    playerConnection.addMediaItem(mediaItem)
                                                }
                                            }
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .offset { IntOffset(0, if (isDragging) dragOffset.roundToInt() else 0) }
                                    .onGloballyPositioned { coords ->
                                        val h = coords.size.height
                                        itemHeights = itemHeights.toMutableList().apply {
                                            while (size <= idx) add(0)
                                            set(idx, h)
                                        }
                                    }
                                    .pointerInput(selectionMode) {
                                        if (!selectionMode) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    draggedItemIndex = idx
                                                    dragOffset = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount.y
                                                },
                                                onDragEnd = {
                                                    if (draggedItemIndex >= 0) {
                                                        val fromIdx = draggedItemIndex
                                                        val totalDrag = dragOffset
                                                        val itemH = itemHeights.getOrElse(fromIdx) { 1 }.coerceAtLeast(1)
                                                        val shiftCount = (totalDrag / itemH).roundToInt()
                                                        val toIdx = (fromIdx + shiftCount)
                                                            .coerceIn(0, upcomingIndices.lastIndex)

                                                        if (toIdx != fromIdx && abs(shiftCount) > 0) {
                                                            playerConnection.moveMediaItem(
                                                                upcomingIndices[fromIdx],
                                                                upcomingIndices[toIdx]
                                                            )
                                                        }
                                                    }
                                                    draggedItemIndex = -1
                                                    dragOffset = 0f
                                                },
                                                onDragCancel = {
                                                    draggedItemIndex = -1
                                                    dragOffset = 0f
                                                },
                                            )
                                        }
                                    }
                            ) {
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = !selectionMode,
                                    enableDismissFromEndToStart = !selectionMode,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            targetValue = when (dismissState.targetValue) {
                                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                                                else -> OmniColors.Error.copy(alpha = 0.28f)
                                            },
                                            label = "queueDismissColor",
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(OmniShapes.Large)
                                                .background(color)
                                                .padding(horizontal = OmniSpacing.large),
                                            contentAlignment = Alignment.CenterEnd,
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_close),
                                                contentDescription = "Remove from queue",
                                                tint = OmniColors.Error,
                                            )
                                        }
                                    },
                                    content = {
                                        QueueItemRow(
                                            title = title,
                                            artists = artists,
                                            thumbnail = meta?.thumbnailUrl,
                                            isCurrent = false,
                                            isSelected = isSelected,
                                            showDragHandle = !selectionMode,
                                            onClick = {
                                                if (selectionMode) {
                                                    if (windowIndex in selectedIndices) {
                                                        selectedIndices.remove(windowIndex)
                                                        if (selectedIndices.isEmpty()) selectionMode = false
                                                    } else {
                                                        selectedIndices.add(windowIndex)
                                                    }
                                                } else {
                                                    playerConnection.seekTo(windowIndex, 0)
                                                    playerConnection.prepare()
                                                }
                                            },
                                            onLongClick = {
                                                if (!selectionMode) {
                                                    selectionMode = true
                                                    selectedIndices.add(windowIndex)
                                                }
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }

                    item(contentType = "bottomSpace") {
                        Spacer(modifier = Modifier.height(OmniSpacing.section))
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OmniSpacing.section)
                .navigationBarsPadding(),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = OmniColors.OmniBackgroundElevated,
                contentColor = OmniColors.TextPrimary,
                actionColor = OmniColors.OmniAccentPrimary,
                shape = OmniShapes.Large,
            )
        }
        
        if (showAddToPlaylistDialog) {
            val playlists by libraryViewModel.playlists.collectAsState()
            com.omnitune.app.ui.component.AddToPlaylistDialog(
                playlists = playlists,
                onDismissRequest = { showAddToPlaylistDialog = false },
                onPlaylistSelected = { playlist ->
                    playerConnection?.let { pc ->
                        scope.launch {
                            var addedCount = 0
                            selectedIndices.forEach { idx ->
                                val windowIndex = upcomingIndices.getOrNull(idx) ?: return@forEach
                                val mediaItem = pc.getMediaItemAt(windowIndex)
                                val meta = mediaItem.localConfiguration?.tag as? MediaMetadata
                                if (meta != null) {
                                    libraryViewModel.ensureSongExists(meta)
                                    val added = libraryViewModel.addToPlaylist(playlist, meta.id)
                                    if (added) addedCount++
                                }
                            }
                            android.widget.Toast.makeText(context, "Added $addedCount songs to playlist", android.widget.Toast.LENGTH_SHORT).show()
                            selectedIndices.clear()
                            selectionMode = false
                        }
                    }
                    showAddToPlaylistDialog = false
                },
                onCreatePlaylist = { name ->
                    playerConnection?.let { pc ->
                        scope.launch {
                            val firstMeta = selectedIndices.firstOrNull()?.let { idx ->
                                val windowIndex = upcomingIndices.getOrNull(idx)
                                windowIndex?.let { pc.getMediaItemAt(it).localConfiguration?.tag as? MediaMetadata }
                            }
                            if (firstMeta != null) {
                                libraryViewModel.ensureSongExists(firstMeta)
                                libraryViewModel.createPlaylist(name, firstMeta.id)
                                
                                // Wait briefly for playlist to be created (hacky but works for bulk create)
                                kotlinx.coroutines.delay(200)
                                
                                val playlists = libraryViewModel.playlists.value
                                val newPlaylist = playlists.find { it.playlist.name == name }
                                
                                if (newPlaylist != null) {
                                    selectedIndices.drop(1).forEach { idx ->
                                        val windowIndex = upcomingIndices.getOrNull(idx) ?: return@forEach
                                        val mediaItem = pc.getMediaItemAt(windowIndex)
                                        val meta = mediaItem.localConfiguration?.tag as? MediaMetadata
                                        if (meta != null) {
                                            libraryViewModel.ensureSongExists(meta)
                                            libraryViewModel.addToPlaylist(newPlaylist, meta.id)
                                        }
                                    }
                                }
                            }
                            android.widget.Toast.makeText(context, "Added ${selectedIndices.size} songs to $name", android.widget.Toast.LENGTH_SHORT).show()
                            selectedIndices.clear()
                            selectionMode = false
                        }
                    }
                    showAddToPlaylistDialog = false
                },
            )
        }
    }
}

@Composable
private fun SleepTimerBanner(remainingMs: Long) {
    val minutes = (remainingMs / 60_000).toInt()
    val seconds = ((remainingMs % 60_000) / 1000).toInt()
    val display = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Large)
            .background(OmniColors.OmniAccentPrimary.copy(alpha = 0.12f))
            .border(
                BorderStroke(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.3f)),
                OmniShapes.Large,
            )
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bedtime),
            contentDescription = null,
            tint = OmniColors.OmniAccentPrimary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Text(
            text = "Sleep timer: $display remaining",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.OmniAccentPrimary,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(OmniSpacing.small))
}

@Composable
private fun BulkActionBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onRemoveSelected: () -> Unit,
    onAddSelectedToPlaylist: () -> Unit,
    onDownloadSelected: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Large)
            .background(OmniColors.OmniGlassMedium)
            .border(
                BorderStroke(1.dp, OmniColors.OmniGlassBorderStrong),
                OmniShapes.Large,
            )
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClearSelection) {
            Icon(painterResource(R.drawable.ic_close), "Clear selection", tint = OmniColors.TextSecondary)
        }
        Text(
            text = "$selectedCount",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OmniColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onDownloadSelected) {
            Icon(painterResource(R.drawable.ic_download), "Download", tint = OmniColors.TextPrimary)
        }
        IconButton(onClick = onAddSelectedToPlaylist) {
            Icon(painterResource(R.drawable.ic_list), "Add to playlist", tint = OmniColors.TextPrimary)
        }
        TextButton(onClick = onRemoveSelected) {
            Text("Remove", color = OmniColors.Error)
        }
    }
    Spacer(modifier = Modifier.height(OmniSpacing.small))
}

@Composable
private fun QueueHeader(
    title: String,
    itemCount: Int,
    upcomingCount: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OmniSpacing.medium, bottom = OmniSpacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OmniColors.OmniGlassMedium)
                .border(
                    BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle),
                    CircleShape,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = OmniColors.TextPrimary,
            )
        }

        Spacer(modifier = Modifier.width(OmniSpacing.medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = queueCountLabel(itemCount, upcomingCount),
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionLabel(
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OmniSpacing.small, bottom = OmniSpacing.compact),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = OmniColors.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NowPlayingCard(
    mediaMetadata: MediaMetadata,
    format: FormatEntity?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.ExtraLarge)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        OmniColors.OmniGlassPlayer,
                        OmniColors.OmniGlassMedium,
                    )
                )
            )
            .border(
                BorderStroke(1.dp, OmniColors.OmniGlassBorderStrong),
                OmniShapes.ExtraLarge,
            )
            .padding(OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QueueArtwork(
            thumbnail = mediaMetadata.thumbnailUrl,
            contentDescription = "Current track artwork",
            size = 72.dp,
            shapeLarge = true,
        )

        Spacer(modifier = Modifier.width(OmniSpacing.medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Playing now",
                style = MaterialTheme.typography.labelMedium,
                color = OmniColors.ActivePlayback,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = mediaMetadata.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val artistText = mediaMetadata.artists.joinToString(", ") { it.name }
                .ifBlank { "Unknown artist" }
            Text(
                text = artistText,
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Codec/bitrate info
            if (format != null) {
                FormatInfoText(format = format)
            }
        }

        Spacer(modifier = Modifier.width(OmniSpacing.small))

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(OmniColors.OmniAccentPrimary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_play_arrow),
                contentDescription = "Now playing",
                tint = OmniColors.ActivePlayback,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun FormatInfoText(format: FormatEntity) {
    val parts = mutableListOf<String>()
    format.codecs?.let { if (it.isNotBlank()) parts.add(it) }
    format.bitrate?.let { if (it > 0) parts.add("${it / 1000}kbps") }
    format.sampleRate?.let { if (it > 0) parts.add("${it / 1000}kHz") }
    if (parts.isNotEmpty()) {
        Text(
            text = parts.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = OmniColors.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QueueItemRow(
    title: String,
    artists: String,
    thumbnail: String?,
    isCurrent: Boolean,
    isSelected: Boolean = false,
    showDragHandle: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Large)
            .clickable(onClick = onClick)
            .background(
                when {
                    isSelected -> OmniColors.OmniAccentPrimary.copy(alpha = 0.18f)
                    isCurrent -> OmniColors.OmniAccentPrimary.copy(alpha = 0.12f)
                    else -> OmniColors.OmniGlassSubtle
                }
            )
            .border(
                BorderStroke(
                    1.dp,
                    when {
                        isSelected -> OmniColors.OmniAccentPrimary
                        isCurrent -> OmniColors.OmniGlassBorderStrong
                        else -> OmniColors.OmniGlassBorderSubtle
                    },
                ),
                OmniShapes.Large,
            )
            .padding(OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(OmniColors.OmniAccentPrimary),
            )
            Spacer(modifier = Modifier.width(OmniSpacing.small))
        }

        QueueArtwork(
            thumbnail = thumbnail,
            contentDescription = null,
            size = if (isSelected) 50.dp else 58.dp,
        )

        Spacer(modifier = Modifier.width(OmniSpacing.small))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artists.ifBlank { "Unknown artist" },
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Drag handle
        if (showDragHandle) {
            Spacer(modifier = Modifier.width(OmniSpacing.small))
            Icon(
                painter = painterResource(R.drawable.ic_sort),
                contentDescription = "Drag to reorder",
                tint = OmniColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun QueueArtwork(
    thumbnail: String?,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp,
    shapeLarge: Boolean = false,
) {
    val context = LocalContext.current
    val shape = if (shapeLarge) OmniShapes.ArtworkMedium else OmniShapes.ArtworkSmall

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(OmniColors.OmniGlassStrong),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnail.isNullOrBlank()) {
            Icon(
                painter = painterResource(R.drawable.ic_album),
                contentDescription = contentDescription,
                tint = OmniColors.TextTertiary,
                modifier = Modifier.size(size * 0.48f),
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnail)
                    .size(Size(QUEUE_ARTWORK_SIZE, QUEUE_ARTWORK_SIZE))
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun QueueEmptyState(
    text: String,
    compact: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (compact) Modifier else Modifier.fillMaxSize())
            .clip(OmniShapes.ExtraLarge)
            .background(OmniColors.OmniGlassSubtle)
            .border(
                BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle),
                OmniShapes.ExtraLarge,
            )
            .padding(
                horizontal = OmniSpacing.section,
                vertical = if (compact) OmniSpacing.hero else OmniSpacing.screen,
            ),
        contentAlignment = Alignment.Center,
    ) {
        EmptyPlaceholder(
            icon = R.drawable.ic_sort,
            text = text,
        )
    }
}

private fun queueCountLabel(itemCount: Int, upcomingCount: Int): String {
    if (itemCount <= 0) return "Queue is empty"
    val total = if (itemCount == 1) "1 track" else "$itemCount tracks"
    val upcoming = if (upcomingCount == 1) "1 up next" else "$upcomingCount up next"
    return "$total · $upcoming"
}

private fun queueItemKey(
    playerConnection: PlayerConnection?,
    index: Int,
): String {
    val mediaItem = playerConnection?.getMediaItemAt(index) ?: return "unknown_$index"
    val mediaId = mediaItem.mediaId.takeIf { it.isNotBlank() }
    val title = mediaItem.mediaMetadata.title?.toString()
    return mediaId ?: "$index-${title.orEmpty()}"
}
