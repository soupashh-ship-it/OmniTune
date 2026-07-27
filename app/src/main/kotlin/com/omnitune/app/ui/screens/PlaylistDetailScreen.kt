package com.omnitune.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.exoplayer.offline.Download
import coil3.compose.AsyncImage
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.R
import com.omnitune.app.db.entities.PlaylistSong
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.playback.PlaylistPlaybackPlanner
import com.omnitune.app.ui.component.AssignTagsDialog
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.component.PlaylistTagChips
import com.omnitune.app.ui.component.TrackMenuProvider
import com.omnitune.app.ui.screens.playlist.PlaylistSuggestionsSection
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import kotlin.math.roundToInt
import com.omnitune.app.ui.screens.LibraryViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit = {},
    onAddSongs: () -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    onPlaySong: (com.omnitune.app.db.entities.Song) -> Unit = {},
    viewModel: PlaylistDetailViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(),
    libraryViewModel: LibraryViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(),
    downloadsViewModel: DownloadsViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val downloadsState by downloadsViewModel.uiState.collectAsState()
    val playerConnection = LocalPlayerConnection.current
    val lazyListState = rememberLazyListState()

    var showAssignTagsDialog by remember { mutableStateOf(false) }
    var showPlaylistMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedSongs by remember { mutableStateOf(setOf<String>()) }
    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var itemHeights by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    val songDownloadStates = remember(downloadsState.downloads) {
        downloadsState.downloads.associateBy { it.request.id }
    }
    val downloadableSongs = songs.filter { it.song.id.isNotBlank() }
    val completedDownloadCount = downloadableSongs.count {
        songDownloadStates[it.song.id]?.state == Download.STATE_COMPLETED
    }
    val allDownloadsComplete = downloadableSongs.isNotEmpty() && completedDownloadCount == downloadableSongs.size
    val totalDurationSeconds = songs.sumOf { it.song.song.duration.coerceAtLeast(0) }
    val durationLabel = formatPlaylistDuration(totalDurationSeconds)
    val ownerLabel = when {
        playlist?.playlist?.isEditable == true -> "Your playlist"
        playlist?.playlist?.browseId != null -> "Saved playlist"
        else -> "Playlist"
    }
    val toolbarTitleVisible by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 420
        }
    }
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 112.dp
    val isEditable = playlist?.playlist?.isEditable == true

    fun queueMissingDownloads() {
        val missing = downloadableSongs.filter {
            songDownloadStates[it.song.id]?.state != Download.STATE_COMPLETED &&
                songDownloadStates[it.song.id]?.state != Download.STATE_DOWNLOADING &&
                songDownloadStates[it.song.id]?.state != Download.STATE_QUEUED &&
                songDownloadStates[it.song.id]?.state != Download.STATE_RESTARTING
        }
        if (missing.isEmpty()) {
            Toast.makeText(context, "Playlist is already downloaded or queued", Toast.LENGTH_SHORT).show()
        } else {
            missing.forEach { playlistSong ->
                downloadsViewModel.startDownload(
                    videoId = playlistSong.song.id,
                    title = playlistSong.song.song.title,
                )
            }
            Toast.makeText(context, "Queued ${missing.size} playlist downloads", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient),
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = OmniSpacing.section,
                top = 92.dp,
                end = OmniSpacing.section,
                bottom = bottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "playlist_hero", contentType = "playlist_hero") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    horizontalAlignment = Alignment.Start,
                ) {
                    val thumbs = playlist?.thumbnails.orEmpty()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2.15f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(OmniColors.GlassSurface)
                                .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                thumbs.isNotEmpty() -> {
                                    AsyncImage(
                                        model = thumbs[0],
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                else -> {
                                    Icon(
                                        painterResource(R.drawable.ic_list),
                                        contentDescription = null,
                                        tint = OmniColors.TextMuted,
                                        modifier = Modifier.size(72.dp),
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = playlist?.playlist?.name ?: "Playlist",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = OmniColors.TextPrimary,
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_list),
                            contentDescription = null,
                            tint = OmniColors.OmniAccentPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(OmniSpacing.compact))
                        Text(
                            text = ownerLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmniColors.TextSecondary,
                            maxLines = 1,
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmniColors.TextMuted,
                        )
                        Text(
                            text = "${songs.size} ${if (songs.size == 1) "song" else "songs"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmniColors.TextSecondary,
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmniColors.TextMuted,
                        )
                        Text(
                            text = durationLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmniColors.TextSecondary,
                        )
                    }

                    val pid = playlist?.playlist?.id
                    if (pid != null) {
                        val tags by libraryViewModel.playlistTags(pid).collectAsState(initial = emptyList())
                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            PlaylistTagChips(
                                tags = tags,
                                editable = isEditable,
                                onRemoveTag = { tag -> libraryViewModel.removePlaylistTag(pid, tag.id) },
                            )
                        }
                    }

                    if (songs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(22.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = {
                                    val p = playlist ?: return@Button
                                    playerConnection?.playQueue(
                                        PlaylistPlaybackPlanner
                                            .ordered(p.id, p.playlist.name, songs)
                                            .toQueue(),
                                    )
                                },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(56.dp),
                                shape = OmniShapes.Pill,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OmniColors.OmniAccentPrimary,
                                    contentColor = OmniColors.TextOnAccent,
                                ),
                            ) {
                                Icon(painterResource(R.drawable.ic_play_arrow), contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = {
                                    val p = playlist ?: return@Button
                                    playerConnection?.playQueue(
                                        PlaylistPlaybackPlanner
                                            .shuffled(p.id, p.playlist.name, songs)
                                            .toQueue(),
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(56.dp),
                                shape = OmniShapes.Pill,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OmniColors.SurfaceRaised,
                                    contentColor = OmniColors.TextPrimary,
                                ),
                            ) {
                                Icon(painterResource(R.drawable.ic_shuffle), contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            PlaylistRoundAction(
                                icon = if (playlist?.playlist?.bookmarkedAt != null) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                                contentDescription = if (playlist?.playlist?.bookmarkedAt != null) "Unsave playlist" else "Save playlist",
                                onClick = { viewModel.toggleBookmark() },
                                tint = if (playlist?.playlist?.bookmarkedAt != null) OmniColors.Hot else OmniColors.TextPrimary,
                            )
                            PlaylistRoundAction(
                                icon = R.drawable.ic_download,
                                contentDescription = "Download playlist",
                                onClick = ::queueMissingDownloads,
                                enabled = downloadableSongs.isNotEmpty() && !allDownloadsComplete,
                            )
                        }
                    }
                }
            }

            if (songs.isEmpty()) {
                item(key = "empty_playlist", contentType = "empty_playlist") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .padding(top = OmniSpacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        EmptyPlaceholder(
                            icon = R.drawable.ic_list,
                            text = "No songs in this playlist yet",
                        )
                        if (isEditable) {
                            Button(
                                onClick = onAddSongs,
                                shape = OmniShapes.Medium,
                                colors = ButtonDefaults.buttonColors(containerColor = OmniColors.OmniAccentPrimary),
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_add),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(OmniSpacing.compact))
                                Text("Add songs")
                            }
                        }
                    }
                }
            } else {
                item(key = "custom_order_header", contentType = "custom_order_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .padding(top = OmniSpacing.large, bottom = OmniSpacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Custom order",
                            style = MaterialTheme.typography.titleMedium,
                            color = OmniColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (isEditable) {
                            Icon(
                                painter = painterResource(R.drawable.ic_drag_handle),
                                contentDescription = "Drag songs to reorder",
                                tint = OmniColors.TextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                itemsIndexed(
                    items = songs,
                    key = { _, item -> item.map.songId },
                    contentType = { _, _ -> "playlist_song" },
                ) { index, playlistSong ->
                    val isDragging = draggedItemIndex == index && dragOffset != 0f

                    Box(
                        modifier = Modifier
                            .animateItem()
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
                                                    val itemH = itemHeights[fromIdx]?.coerceAtLeast(1) ?: 1
                                                    val shiftCount = (dragOffset / itemH).roundToInt()
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
                                } else {
                                    Modifier
                                },
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
                                    val p = playlist
                                    if (p != null) {
                                        playerConnection?.playQueue(
                                            PlaylistPlaybackPlanner
                                                .ordered(
                                                    playlistId = p.id,
                                                    playlistName = p.playlist.name,
                                                    songs = songs,
                                                    selectedMapId = playlistSong.map.id,
                                                )
                                                .toQueue(),
                                        )
                                    }
                                }
                            },
                            onLongClick = {
                                if (isEditable) {
                                    val id = playlistSong.song.id
                                    if (!selectionMode) {
                                        selectionMode = true
                                        selectedSongs = setOf(id)
                                    } else {
                                        selectedSongs = if (id in selectedSongs) {
                                            (selectedSongs - id).also {
                                                if (it.isEmpty()) selectionMode = false
                                            }
                                        } else {
                                            selectedSongs + id
                                        }
                                    }
                                }
                            },
                            onRemove = if (isEditable) { { viewModel.removeSong(playlistSong.song.id) } } else null,
                            onViewArtist = playlistSong.song.artists.firstOrNull { it.id.isNotBlank() }?.let { artist ->
                                { onNavigateToArtist(artist.id) }
                            },
                            onViewAlbum = playlistSong.song.song.albumId?.let { albumId ->
                                { onNavigateToAlbum(albumId) }
                            },
                            showDragHandle = isEditable && !selectionMode,
                        )
                    }
                }
                item(key = "playlist_suggestions", contentType = "playlist_suggestions") {
                    PlaylistSuggestionsSection(
                        modifier = Modifier
                            .animateItem()
                            .padding(top = OmniSpacing.large),
                        viewModel = viewModel,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = OmniSpacing.section, vertical = OmniSpacing.compact)
                .zIndex(2f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(46.dp)
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
            Text(
                text = playlist?.playlist?.name ?: "Playlist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = OmniSpacing.medium)
                    .graphicsLayer { alpha = if (toolbarTitleVisible) 1f else 0f },
            )
            if (isEditable) {
                Box {
                    IconButton(
                        onClick = { showPlaylistMenu = true },
                        modifier = Modifier
                            .size(46.dp)
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
                            text = { Text("Add songs") },
                            onClick = {
                                showPlaylistMenu = false
                                onAddSongs()
                            },
                        )
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
            } else {
                Icon(
                    painterResource(R.drawable.ic_list),
                    contentDescription = null,
                    tint = OmniColors.Hot,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(OmniColors.Hot.copy(alpha = 0.16f))
                        .padding(12.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = selectionMode,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(3f),
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
                    selectedSongs = if (selectedSongs.size == songs.size) emptySet() else songs.map { it.song.id }.toSet()
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

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(playlist?.playlist?.name ?: "") }
        val trimmedName = newName.trim()
        val nameError = when {
            newName.isNotBlank() && trimmedName.isBlank() -> "Playlist name cannot be empty"
            trimmedName.length > 80 -> "Playlist name must be 80 characters or fewer"
            else -> null
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        isError = nameError != null,
                        placeholder = { Text("Playlist name") },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OmniColors.OmniAccentPrimary,
                            unfocusedBorderColor = OmniColors.OmniGlassBorderSubtle,
                            focusedTextColor = OmniColors.TextPrimary,
                            unfocusedTextColor = OmniColors.TextPrimary,
                        ),
                    )
                    if (nameError != null) {
                        Text(
                            text = nameError,
                            style = MaterialTheme.typography.bodySmall,
                            color = OmniColors.Error,
                            modifier = Modifier.padding(top = OmniSpacing.compact),
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (trimmedName.isNotBlank() && trimmedName.length <= 80) {
                            viewModel.renamePlaylist(trimmedName)
                            showRenameDialog = false
                        }
                    },
                    enabled = trimmedName.isNotBlank() && trimmedName.length <= 80,
                ) {
                    Text(
                        "Rename",
                        color = if (trimmedName.isNotBlank() && trimmedName.length <= 80) {
                            OmniColors.Hot
                        } else {
                            OmniColors.TextSecondary
                        },
                    )
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
            title = { Text("Delete playlist?", fontWeight = FontWeight.Bold) },
            text = { Text("This removes the playlist, not the songs from your library.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.deletePlaylist()
                        showDeleteDialog = false
                        onBack()
                    },
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
}

@Composable
private fun PlaylistRoundAction(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prominent: Boolean = false,
    tint: androidx.compose.ui.graphics.Color = if (prominent) OmniColors.OmniBackgroundBase else OmniColors.TextPrimary,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(if (prominent) 72.dp else 48.dp)
            .clip(CircleShape)
            .background(
                when {
                    !enabled -> OmniColors.OmniGlassMedium.copy(alpha = 0.45f)
                    prominent -> OmniColors.OmniAccentPrimary
                    else -> OmniColors.OmniGlassMedium
                },
            )
            .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), CircleShape),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (enabled) tint else OmniColors.TextMuted,
            modifier = Modifier.size(if (prominent) 28.dp else 24.dp),
        )
    }
}

@Composable
private fun PlaylistInfoPill(
    icon: Int,
    label: String,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(OmniColors.OmniGlassMedium)
            .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), CircleShape)
            .padding(horizontal = OmniSpacing.small, vertical = OmniSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = OmniColors.TextSecondary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(OmniSpacing.compact))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = OmniColors.TextSecondary,
        )
    }
}

private fun formatPlaylistDuration(totalSeconds: Int): String {
    if (totalSeconds <= 0) return "0:00"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
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
    onViewArtist: (() -> Unit)? = null,
    onViewAlbum: (() -> Unit)? = null,
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
                onViewArtist = onViewArtist,
                onViewAlbum = onViewAlbum,
            )
        }
    }
    HorizontalDivider(
        color = OmniColors.OmniGlassBorderSubtle,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 48.dp),
    )
}
