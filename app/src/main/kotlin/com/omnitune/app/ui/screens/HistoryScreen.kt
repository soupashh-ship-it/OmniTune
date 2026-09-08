/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.db.entities.EventWithSong
import com.omnitune.app.db.entities.Song as DbSong
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.Song as SuvSong
import com.omnitune.app.models.toSuvSong
import com.omnitune.app.ui.component.AddToPlaylistSheet
import com.omnitune.app.ui.component.CreatePlaylistDialog
import com.omnitune.app.ui.component.SongMenuBottomSheet
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onPlaySong: (DbSong) -> Unit = {},
    onSongClick: ((List<SuvSong>, Int) -> Unit)? = null,
    onBack: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSongIds by viewModel.selectedSongIds.collectAsStateWithLifecycle()
    val incognitoModeEnabled by viewModel.incognitoModeEnabled.collectAsStateWithLifecycle()
    val playlistState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showSongMenu by remember { mutableStateOf(false) }
    var selectedRecent by remember { mutableStateOf<RecentHistoryItem?>(null) }

    val recentlyPlayed = remember(uiState.events) {
        uiState.events.map { event -> event.toRecentHistoryItem() }
    }
    val filteredHistory = remember(recentlyPlayed, searchQuery) {
        if (searchQuery.isBlank()) {
            recentlyPlayed
        } else {
            recentlyPlayed.filter { recent ->
                recent.song.title.contains(searchQuery, ignoreCase = true) ||
                    recent.song.artist.contains(searchQuery, ignoreCase = true) ||
                    recent.song.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    val isSelectionMode = selectedSongIds.isNotEmpty()

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear history?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "This will remove all songs from your listening history. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearListenHistory()
                        viewModel.clearSelection()
                        showClearConfirmDialog = false
                    },
                ) {
                    Text(
                        text = "Clear All",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedSongIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Selection",
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val selectedSongs = recentlyPlayed
                                    .filter { recent -> recent.song.id in selectedSongIds }
                                    .distinctBy { recent -> recent.song.id }
                                    .map { recent -> recent.song }
                                playlistViewModel.showAddToPlaylistSheet(selectedSongs)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            } else {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = "History",
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.setIncognitoMode(!incognitoModeEnabled) },
                        ) {
                            Icon(
                                imageVector = if (incognitoModeEnabled) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = "Incognito Mode",
                                tint = if (incognitoModeEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }

                        if (recentlyPlayed.isNotEmpty()) {
                            IconButton(onClick = { showClearConfirmDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Clear",
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AnimatedVisibility(
                visible = incognitoModeEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Incognito Mode is on. Your listening history is not being saved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search history") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                } else {
                    null
                },
                shape = CircleShape,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    EmptyHistoryState(
                        title = "History unavailable",
                        message = uiState.error ?: "Your listening history could not be loaded.",
                    )
                }

                filteredHistory.isEmpty() -> {
                    EmptyHistoryState(
                        title = if (searchQuery.isBlank()) "No history yet" else "No results found",
                        message = if (searchQuery.isBlank()) "Songs you play will appear here." else null,
                    )
                }

                else -> {
                    val groupedByDate = remember(filteredHistory) {
                        filteredHistory.groupBy { recent -> getDateLabel(recent.playedAt) }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp),
                    ) {
                        groupedByDate.forEach { (dateLabel, itemsForDate) ->
                            stickyHeader {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.background,
                                ) {
                                    Text(
                                        text = dateLabel,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    )
                                }
                            }

                            items(
                                items = itemsForDate,
                                key = { recent -> recent.key },
                            ) { recent ->
                                val isSelected = recent.song.id in selectedSongIds
                                val allSongsInHistory = recentlyPlayed.map { item -> item.song }
                                val indexInAll = recentlyPlayed
                                    .indexOfFirst { item -> item.key == recent.key }
                                    .coerceAtLeast(0)

                                RecentSongItem(
                                    recent = recent,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleSelection(recent.song.id)
                                        } else if (onSongClick != null) {
                                            onSongClick(allSongsInHistory, indexInAll)
                                        } else {
                                            onPlaySong(recent.dbSong)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.toggleSelection(recent.song.id)
                                    },
                                    onMoreClick = {
                                        selectedRecent = recent
                                        showSongMenu = true
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSongMenu && selectedRecent != null) {
        val recent = selectedRecent ?: return
        SongMenuBottomSheet(
            isVisible = true,
            onDismiss = { showSongMenu = false },
            song = recent.song,
            onPlayNext = { playerConnection?.playNext(recent.dbSong.toMediaItem()) },
            onAddToQueue = { playerConnection?.addToQueue(recent.dbSong.toMediaItem()) },
            onAddToPlaylist = { playlistViewModel.showAddToPlaylistSheet(recent.song) },
            onShare = { shareSong(context, recent.song) },
        )
    }

    if (playlistState.showAddToPlaylistSheet && playlistState.selectedSongs.isNotEmpty()) {
        AddToPlaylistSheet(
            songs = playlistState.selectedSongs,
            isVisible = true,
            playlists = playlistState.userPlaylists,
            isLoading = playlistState.isLoadingPlaylists || playlistState.isAddingSong,
            onDismiss = { playlistViewModel.hideAddToPlaylistSheet() },
            onAddToPlaylist = { playlistId ->
                playlistViewModel.addSongsToPlaylist(playlistId)
                viewModel.clearSelection()
            },
            onCreateNewPlaylist = { playlistViewModel.showCreatePlaylistDialog() },
        )
    }

    if (playlistState.showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            isVisible = true,
            isCreating = playlistState.isCreatingPlaylist,
            onDismiss = { playlistViewModel.hideCreatePlaylistDialog() },
            onCreate = { title, description, isPrivate, syncWithYt ->
                playlistViewModel.createPlaylist(title, description, isPrivate, syncWithYt)
            },
            isLoggedIn = true,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentSongItem(
    recent: RecentHistoryItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    Color.Transparent
                },
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = recent.song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
                alpha = if (isSelected) 0.5f else 1f,
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = recent.song.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${recent.song.artist.ifBlank { "Unknown artist" }} - ${getTimeLabel(recent.playedAt)}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!isSelectionMode) {
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(
    title: String,
    message: String? = null,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
    }
}

private data class RecentHistoryItem(
    val key: String,
    val song: SuvSong,
    val dbSong: DbSong,
    val playedAt: Long,
)

private fun EventWithSong.toRecentHistoryItem(): RecentHistoryItem {
    val timestampMs = event.timestamp
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    return RecentHistoryItem(
        key = "${event.id}_${song.song.id}_$timestampMs",
        song = song.toSuvSong(),
        dbSong = song,
        playedAt = timestampMs,
    )
}

private fun getDateLabel(timestamp: Long): String {
    val calendar = java.util.Calendar.getInstance()
    val year = calendar.get(java.util.Calendar.YEAR)
    val playedCalendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val playedYear = playedCalendar.get(java.util.Calendar.YEAR)
    return when {
        DateUtils.isToday(timestamp) -> "Today"
        DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
        year == playedYear -> SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun getTimeLabel(timestamp: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))

private fun shareSong(context: Context, song: SuvSong) {
    val artist = song.artist.ifBlank { "Unknown artist" }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Listen to ${song.title} by $artist on OmniTune")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share song"))
}
