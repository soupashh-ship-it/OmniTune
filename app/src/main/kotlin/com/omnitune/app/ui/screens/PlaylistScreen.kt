package com.omnitune.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.omnitune.app.LocalDownloadUtil
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.models.Playlist
import com.omnitune.app.models.Song
import com.omnitune.app.models.SortOrder
import com.omnitune.app.models.SortType
import com.omnitune.app.models.toMediaItem
import com.omnitune.app.ui.component.*
import com.omnitune.app.ui.navigation.LocalRouteChromeInsets
import com.omnitune.app.ui.theme.SquircleShape
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit = {},
    onShufflePlay: (List<Song>) -> Unit = {},
    onAddSongsClick: () -> Unit = {},
    currentSong: Song? = null,
    viewModel: PlaylistViewModel = hiltViewModel(),
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlist = uiState.playlist
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val downloadUtil = LocalDownloadUtil.current
    val chromeInsets = LocalRouteChromeInsets.current

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDarkTheme) Color(0xFF0D0D0D) else Color.White

    val dominantColors = rememberDominantColors(playlist?.thumbnailUrl)
    val listState = rememberLazyListState()

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSongMenu by remember { mutableStateOf(false) }
    var selectedSong: Song? by remember { mutableStateOf(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSongs = remember(playlist?.songs, searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        if (q.isBlank()) {
            playlist?.songs.orEmpty()
        } else {
            playlist?.songs.orEmpty().filter {
                it.title.lowercase(Locale.getDefault()).contains(q) ||
                    it.artist.lowercase(Locale.getDefault()).contains(q) ||
                    it.album.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            onBackClick()
        }
    }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        val message = uiState.successMessage ?: uiState.errorMessage
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    val sharePlaylist: (Playlist) -> Unit = { playlistToShare ->
        val shareText = "Check out this playlist: ${playlistToShare.title} by ${playlistToShare.author}\n\nhttps://music.youtube.com/playlist?list=${playlistToShare.id}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Playlist"))
    }

    val shareSong: (Song) -> Unit = { song ->
        val shareText = "Check out this song: ${song.title} by ${song.artist}\n\nhttps://music.youtube.com/watch?v=${song.id}"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "${song.title} - ${song.artist}")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Song"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (playlist?.thumbnailUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(playlist.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp),
                contentScale = ContentScale.Crop,
                alpha = if (isDarkTheme) 0.4f else 0.3f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color.Transparent, Color(0xFF0D0D0D).copy(alpha = 0.8f), Color(0xFF0D0D0D))
                        } else {
                            listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.8f), Color.White)
                        }
                    )
                )
        )

        when {
            uiState.isLoading && playlist == null -> {
                PremiumLoadingScreen(
                    thumbnailUrl = playlist?.thumbnailUrl,
                    onBackClick = onBackClick
                )
            }
            uiState.error != null && playlist == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Failed to load playlist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = uiState.error ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = viewModel::refreshPlaylist, shape = SquircleShape) {
                        Text("Retry")
                    }
                }
            }
            playlist != null -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refreshPlaylist,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = chromeInsets.contentBottomPadding)
                    ) {
                        item {
                            PlaylistHeader(
                                playlist = playlist,
                                isSaved = uiState.isSaved,
                                isEditable = uiState.isEditable,
                                onBackClick = onBackClick,
                                onPlayAll = { onPlayAll(filteredSongs) },
                                onShufflePlay = { onShufflePlay(filteredSongs) },
                                onToggleSave = viewModel::toggleSaveToLibrary,
                                onDownload = { viewModel.downloadPlaylist(playlist) },
                                onShare = { sharePlaylist(playlist) },
                                onEditClick = { showRenameDialog = true },
                                onDeleteClick = { showDeleteDialog = true }
                            )
                        }

                        // Search Bar
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search in playlist...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = SquircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        // Sort Chips
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = uiState.sortType == SortType.CUSTOM,
                                    onClick = { viewModel.setSortType(SortType.CUSTOM) },
                                    label = { Text("Default") },
                                    shape = SquircleShape
                                )
                                FilterChip(
                                    selected = uiState.sortType == SortType.TITLE,
                                    onClick = { viewModel.setSortType(SortType.TITLE) },
                                    label = { Text("Title") },
                                    shape = SquircleShape
                                )
                                FilterChip(
                                    selected = uiState.sortType == SortType.ARTIST,
                                    onClick = { viewModel.setSortType(SortType.ARTIST) },
                                    label = { Text("Artist") },
                                    shape = SquircleShape
                                )
                            }
                        }

                        // Track list
                        itemsIndexed(filteredSongs) { index, song ->
                            val isPlaying = currentSong?.id == song.id
                            PlaylistSongRow(
                                index = index + 1,
                                song = song,
                                isPlaying = isPlaying,
                                onClick = { onSongClick(filteredSongs, index) },
                                onMoreClick = {
                                    selectedSong = song
                                    showSongMenu = true
                                }
                            )
                        }
                    }
                }
            }
        }

        selectedSong?.let { song ->
            SongMenuBottomSheet(
                isVisible = showSongMenu,
                onDismiss = {
                    showSongMenu = false
                    selectedSong = null
                },
                song = song,
                onPlayNext = playerConnection?.let { connection -> { connection.playNext(song.toMediaItem()) } },
                onAddToQueue = playerConnection?.let { connection -> { connection.addToQueue(song.toMediaItem()) } },
                onAddToPlaylist = { playlistViewModel.showAddToPlaylistSheet(song) },
                onDownload = {
                    downloadUtil.enqueue(song.id, song.title) { _, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = { shareSong(song) },
                onRemoveFromPlaylist = if (uiState.isEditable) { { viewModel.removeSong(song) } } else null,
                isCurrentlyPlaying = currentSong?.id == song.id
            )
        }

        RenamePlaylistDialog(
            isVisible = showRenameDialog,
            currentName = playlist?.title ?: "",
            isRenaming = false,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                viewModel.renamePlaylist(newName)
                showRenameDialog = false
            }
        )

        DeletePlaylistDialog(
            isVisible = showDeleteDialog,
            playlistTitle = playlist?.title ?: "",
            isDeleting = false,
            onDismiss = { showDeleteDialog = false },
            onDelete = {
                viewModel.deletePlaylist()
                showDeleteDialog = false
            }
        )
    }
}

@Composable
private fun PlaylistHeader(
    playlist: Playlist,
    isSaved: Boolean,
    isEditable: Boolean,
    onBackClick: () -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onToggleSave: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEditable) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(240.dp)
                .shadow(16.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (!playlist.thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = playlist.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp).align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = playlist.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = playlist.author,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${playlist.totalSongCount ?: playlist.songs.size} songs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )


        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPlayAll,
                shape = SquircleShape,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play", fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = onShufflePlay,
                shape = SquircleShape,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shuffle", fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = onToggleSave,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Save",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onDownload,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download")
            }
        }
    }
}

@Composable
private fun PlaylistSongRow(
    index: Int,
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(28.dp)
        )

        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier.size(48.dp).clip(SquircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onMoreClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
