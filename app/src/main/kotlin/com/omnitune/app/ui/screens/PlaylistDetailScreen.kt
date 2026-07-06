package com.omnitune.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.R
import com.omnitune.app.db.entities.PlaylistSong
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.playback.queues.ListQueue
import com.omnitune.app.ui.component.AssignTagsDialog
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.component.PlaylistTagChips
import com.omnitune.app.ui.component.TrackMenuProvider
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import kotlin.math.abs
import kotlin.math.roundToInt
import com.omnitune.app.ui.screens.LibraryViewModel

@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit = {},
    onPlaySong: (com.omnitune.app.db.entities.Song) -> Unit = {},
    viewModel: PlaylistDetailViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(),
    libraryViewModel: LibraryViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(),
) {
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    var showAssignTagsDialog by remember { mutableStateOf(false) }
    val playerConnection = LocalPlayerConnection.current

    // Selection state
    var selectionMode by remember { mutableStateOf(false) }
    var selectedSongs by remember { mutableStateOf(setOf<String>()) }

    // Drag reorder state
    val lazyListState = rememberLazyListState()
    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var itemHeights by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .padding(horizontal = OmniSpacing.section),
    ) {
        // Header
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
                    .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), CircleShape),
            ) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = OmniColors.TextPrimary,
                )
            }
            Spacer(modifier = Modifier.width(OmniSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist?.playlist?.name ?: "Playlist",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmniColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${songs.size} songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmniColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                val pid = playlist?.playlist?.id
                if (pid != null) {
                    val tags by libraryViewModel.playlistTags(pid).collectAsState(initial = emptyList())
                    PlaylistTagChips(
                        tags = tags,
                        editable = playlist?.playlist?.isEditable == true,
                        onRemoveTag = { tag -> libraryViewModel.removePlaylistTag(pid, tag.id) }
                    )
                }
            }

            var showPlaylistMenu by remember { mutableStateOf(false) }
            var showRenameDialog by remember { mutableStateOf(false) }
            var showDeleteDialog by remember { mutableStateOf(false) }

            if (playlist?.playlist?.isEditable == true) {
                Box {
                    IconButton(
                        onClick = { showPlaylistMenu = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(OmniColors.OmniGlassMedium),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_more_vert),
                            contentDescription = "More options",
                            tint = OmniColors.TextSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showPlaylistMenu,
                        onDismissRequest = { showPlaylistMenu = false },
                        modifier = Modifier.background(OmniColors.OmniBackgroundElevated),
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Rename playlist") },
                            onClick = {
                                showPlaylistMenu = false
                                showRenameDialog = true
                            },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Assign tags") },
                            onClick = {
                                showPlaylistMenu = false
                                showAssignTagsDialog = true
                            },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Delete playlist", color = OmniColors.Error) },
                            onClick = {
                                showPlaylistMenu = false
                                showDeleteDialog = true
                            },
                        )
                    }
                }

                if (showRenameDialog) {
                    var newName by remember { mutableStateOf(playlist?.playlist?.name ?: "") }
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = { Text("Rename Playlist", fontWeight = FontWeight.Bold) },
                        text = {
                            androidx.compose.material3.OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                singleLine = true,
                                placeholder = { Text("Playlist name") },
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OmniColors.OmniAccentPrimary,
                                    unfocusedBorderColor = OmniColors.OmniGlassBorderSubtle,
                                    focusedTextColor = OmniColors.TextPrimary,
                                    unfocusedTextColor = OmniColors.TextPrimary
                                )
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    if (newName.isNotBlank()) {
                                        viewModel.renamePlaylist(newName.trim())
                                        showRenameDialog = false
                                    }
                                },
                                enabled = newName.isNotBlank()
                            ) {
                                Text("Rename", color = if (newName.isNotBlank()) OmniColors.Hot else OmniColors.TextSecondary)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { showRenameDialog = false }) {
                                Text("Cancel", color = OmniColors.TextPrimary)
                            }
                        },
                        containerColor = OmniColors.OmniBackgroundElevated,
                        titleContentColor = OmniColors.TextPrimary,
                    )
                }

                if (showDeleteDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Playlist?", fontWeight = FontWeight.Bold) },
                        text = { Text("This removes the playlist only. Songs and downloads will not be deleted.") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    viewModel.deletePlaylist()
                                    showDeleteDialog = false
                                    onBack()
                                }
                            ) {
                                Text("Delete", color = OmniColors.Error)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel", color = OmniColors.TextPrimary)
                            }
                        },
                        containerColor = OmniColors.OmniBackgroundElevated,
                        titleContentColor = OmniColors.TextPrimary,
                        textContentColor = OmniColors.TextSecondary,
                    )
                }

            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(OmniColors.Hot.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_list),
                        contentDescription = null,
                        tint = OmniColors.Hot,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // Artwork header
        val thumbs = playlist?.thumbnails
        if (!thumbs.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(bottom = OmniSpacing.large),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbs.size == 1) {
                    AsyncImage(
                        model = thumbs[0],
                        contentDescription = null,
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(OmniSpacing.small))
                            .background(OmniColors.GlassSurface),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(OmniSpacing.small))
                            .background(OmniColors.GlassSurface),
                    ) {
                        val positions = listOf(
                            Alignment.TopStart, Alignment.TopEnd,
                            Alignment.BottomStart, Alignment.BottomEnd,
                        )
                        thumbs.take(4).forEachIndexed { i, url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .align(positions.getOrElse(i) { Alignment.Center })
                                    .size(90.dp),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }
        }

        // Play All / Shuffle buttons
        if (songs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = OmniSpacing.medium),
                horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                Button(
                    onClick = {
                        playerConnection?.playQueue(
                            ListQueue(
                                title = playlist?.playlist?.name,
                                items = songs.map { it.song.toMediaItem() },
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = OmniShapes.Medium,
                    colors = ButtonDefaults.buttonColors(containerColor = OmniColors.OmniAccentPrimary),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_play_arrow),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(OmniSpacing.compact))
                    Text("Play All")
                }
                Button(
                    onClick = {
                        playerConnection?.playQueue(
                            ListQueue(
                                title = playlist?.playlist?.name,
                                items = songs.shuffled().map { it.song.toMediaItem() },
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = OmniShapes.Medium,
                    colors = ButtonDefaults.buttonColors(containerColor = OmniColors.GlassSurface),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_shuffle),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(OmniSpacing.compact))
                    Text("Shuffle")
                }
            }
        }

        if (showAssignTagsDialog) {
            val pid = playlist?.playlist?.id
            if (pid != null) {
                AssignTagsDialog(
                    database = viewModel.db,
                    playlistId = pid,
                    onDismiss = { showAssignTagsDialog = false },
                )
            }
        }

        if (songs.isEmpty()) {
            EmptyPlaceholder(
                icon = R.drawable.ic_list,
                text = "No songs in this playlist yet",
            )
        } else {
            val isEditable = playlist?.playlist?.isEditable == true

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(
                    items = songs,
                    key = { _, item -> item.map.songId },
                    contentType = { _, _ -> "playlist_song" },
                ) { index, playlistSong ->
                    val isDragging = draggedItemIndex == index && dragOffset != 0f
                    val density = LocalDensity.current

                    Box(
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffset else 0f
                                alpha = if (isDragging) 0.85f else 1f
                            }
                            .onSizeChanged {
                                itemHeights = itemHeights + (index to it.height)
                            }
                            .then(
                                if (isEditable && !selectionMode) {
                                    Modifier.pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedItemIndex = index
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
                                                    val itemH = itemHeights[fromIdx]?.coerceAtLeast(1) ?: 1
                                                    val shiftCount = (totalDrag / itemH).roundToInt()
                                                    val toIdx = (fromIdx + shiftCount).coerceIn(0, songs.lastIndex)
                                                    if (toIdx != fromIdx) {
                                                        viewModel.moveSong(fromIdx, toIdx)
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
                                } else Modifier
                            ),
                    ) {
                        PlaylistSongRow(
                            index = index,
                            playlistSong = playlistSong,
                            isSelected = selectedSongs.contains(playlistSong.song.id),
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) {
                                    val id = playlistSong.song.id
                                    if (id in selectedSongs) {
                                        selectedSongs = selectedSongs - id
                                        if (selectedSongs.isEmpty()) selectionMode = false
                                    } else {
                                        selectedSongs = selectedSongs + id
                                    }
                                } else {
                                    onPlaySong(playlistSong.song)
                                }
                            },
                            onLongClick = {
                                if (isEditable) {
                                    val id = playlistSong.song.id
                                    if (!selectionMode) {
                                        selectionMode = true
                                        selectedSongs = setOf(id)
                                    } else {
                                        if (id in selectedSongs) {
                                            selectedSongs = selectedSongs - id
                                            if (selectedSongs.isEmpty()) selectionMode = false
                                        } else {
                                            selectedSongs = selectedSongs + id
                                        }
                                    }
                                }
                            },
                            onRemove = if (isEditable) { { viewModel.removeSong(playlistSong.song.id) } } else null,
                            showDragHandle = isEditable && !selectionMode,
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(88.dp)) }
            }

            // Selection action bar
            AnimatedVisibility(
                visible = selectionMode,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OmniColors.OmniBackgroundElevated)
                        .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.compact),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${selectedSongs.size} selected",
                        color = OmniColors.TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        selectedSongs = if (selectedSongs.size == songs.size) emptySet()
                        else songs.map { it.song.id }.toSet()
                    }) {
                        Text(
                            if (selectedSongs.size == songs.size) "Deselect All" else "Select All",
                            color = OmniColors.OmniAccentPrimary,
                        )
                    }
                    if (selectedSongs.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(OmniSpacing.small))
                        TextButton(onClick = {
                            viewModel.removeSongs(selectedSongs.toList())
                            selectedSongs = emptySet()
                            selectionMode = false
                        }) {
                            Text("Remove", color = OmniColors.Error)
                        }
                        Spacer(modifier = Modifier.width(OmniSpacing.small))
                        TextButton(onClick = {
                            selectionMode = false
                            selectedSongs = emptySet()
                        }) {
                            Text("Cancel", color = OmniColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistSongRow(
    index: Int,
    playlistSong: PlaylistSong,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onRemove: (() -> Unit)? = null,
    showDragHandle: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.SM)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .background(
                if (isSelected) OmniColors.OmniAccentPrimary.copy(alpha = 0.12f)
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = OmniColors.OmniAccentPrimary,
                    uncheckedColor = OmniColors.TextSecondary,
                ),
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextMuted,
            modifier = Modifier.width(28.dp),
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(OmniShapes.SM)
                .background(OmniColors.GlassSurface),
        ) {
            val thumbnailUrl = playlistSong.song.song.thumbnailUrl
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlistSong.song.song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = playlistSong.song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showDragHandle) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = "Drag to reorder",
                tint = OmniColors.TextMuted,
                modifier = Modifier.size(24.dp),
            )
        }

        var menuExpanded by remember { mutableStateOf(false) }
        val playerConnection = LocalPlayerConnection.current

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "More options",
                    tint = OmniColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }

            TrackMenuProvider(
                showMenu = menuExpanded,
                onDismissMenu = { menuExpanded = false },
                mediaMetadata = playlistSong.song.toMediaMetadata(),
                onPlayNext = { playerConnection?.playNext(playlistSong.song.toMediaItem()) },
                onAddToQueue = { playerConnection?.addToQueue(playlistSong.song.toMediaItem()) },
                onRemoveFromPlaylist = onRemove,
            )
        }
    }
    HorizontalDivider(
        color = OmniColors.OmniGlassBorderSubtle,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 48.dp),
    )
}
