package com.omnitune.app.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.models.*
import com.omnitune.app.ui.component.*
import com.omnitune.app.ui.navigation.LocalRouteChromeInsets
import com.omnitune.app.ui.theme.SquircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onHistoryClick: () -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onImportPlaylist: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlistMgmtState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val chromeInsets = LocalRouteChromeInsets.current

    var selectedPlaylist: PlaylistDisplayItem? by remember { mutableStateOf(null) }
    var showPlaylistMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val exportM3ULauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        val playlist = selectedPlaylist
        if (uri != null && playlist != null) {
            viewModel.exportPlaylist(
                context = context,
                playlistId = playlist.getPlaylistId(),
                playlistName = playlist.name,
                uri = uri,
                format = PlaylistExportFormat.M3U
            ) { _, message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val exportOmniLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val playlist = selectedPlaylist
        if (uri != null && playlist != null) {
            viewModel.exportPlaylist(
                context = context,
                playlistId = playlist.getPlaylistId(),
                playlistName = playlist.name,
                uri = uri,
                format = PlaylistExportFormat.OMNI
            ) { _, message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { playlistViewModel.showCreatePlaylistDialog() },
                shape = SquircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = chromeInsets.contentBottomPadding)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Playlist")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar
                    LibraryTopBar(
                        onHistoryClick = onHistoryClick,
                        onSyncClick = { viewModel.refresh() }
                    )

                    // Filter Chips
                    LibraryFilterChips(
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = { viewModel.setFilter(it) }
                    )

                    // Control Bar
                    LibraryControlBar(
                        sortOption = uiState.sortOption,
                        viewMode = uiState.viewMode,
                        onSortClick = {
                            viewModel.setSortOption(
                                if (uiState.sortOption == LibrarySortOption.DATE_ADDED) LibrarySortOption.NAME else LibrarySortOption.DATE_ADDED
                            )
                        },
                        onViewModeClick = {
                            viewModel.setViewMode(
                                if (uiState.viewMode == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
                            )
                        },
                        itemCount = when (uiState.selectedFilter) {
                            LibraryFilter.PLAYLISTS -> uiState.playlists.size + 5
                            LibraryFilter.SONGS -> uiState.librarySongs.size
                            LibraryFilter.ALBUMS -> uiState.libraryAlbums.size
                            LibraryFilter.ARTISTS -> uiState.libraryArtists.size
                            LibraryFilter.FOLDERS -> uiState.localFolders.size
                        },
                        searchQuery = uiState.librarySearchQuery,
                        onSearchQueryChange = { viewModel.setLibrarySearchQuery(it) }
                    )

                    // Content based on filter
                    when (uiState.selectedFilter) {
                        LibraryFilter.PLAYLISTS -> {
                            if (uiState.viewMode == LibraryViewMode.GRID) {
                                PlaylistsGrid(
                                    uiState = uiState,
                                    onPlaylistClick = onPlaylistClick,
                                    onSmartPlaylistClick = { type ->
                                        handleSmartPlaylistClick(type, onPlaylistClick, onDownloadsClick, viewModel)
                                    },
                                    onMoreClick = { playlist ->
                                        selectedPlaylist = playlist
                                        showPlaylistMenu = true
                                    }
                                )
                            } else {
                                PlaylistsList(
                                    uiState = uiState,
                                    onPlaylistClick = onPlaylistClick,
                                    onSmartPlaylistClick = { type ->
                                        handleSmartPlaylistClick(type, onPlaylistClick, onDownloadsClick, viewModel)
                                    },
                                    onMoreClick = { playlist ->
                                        selectedPlaylist = playlist
                                        showPlaylistMenu = true
                                    }
                                )
                            }
                        }
                        LibraryFilter.SONGS -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = chromeInsets.contentBottomPadding,
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(uiState.librarySongs) { index, song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(SquircleShape)
                                            .clickable { onSongClick(uiState.librarySongs, index) }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = song.thumbnailUrl,
                                            contentDescription = song.title,
                                            modifier = Modifier.size(52.dp).clip(SquircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
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
                                    }
                                }
                            }
                        }
                        LibraryFilter.ALBUMS -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(150.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = chromeInsets.contentBottomPadding,
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.libraryAlbums) { album ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(SquircleShape)
                                            .clickable { onAlbumClick(album) }
                                    ) {
                                        AsyncImage(
                                            model = album.thumbnailUrl,
                                            contentDescription = album.title,
                                            modifier = Modifier.aspectRatio(1f).fillMaxWidth().clip(SquircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = album.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = album.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        LibraryFilter.ARTISTS -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(130.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = chromeInsets.contentBottomPadding,
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.libraryArtists) { artist ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onArtistClick(artist.id) },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AsyncImage(
                                            model = artist.thumbnailUrl,
                                            contentDescription = artist.name,
                                            modifier = Modifier.size(110.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = artist.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        LibraryFilter.FOLDERS -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = chromeInsets.contentBottomPadding,
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.localFolders.entries.toList()) { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(SquircleShape)
                                            .clickable {
                                                onSongClick(entry.value, 0)
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.key,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${entry.value.size} songs",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        selectedPlaylist?.let { playlist ->
            val playlistId = playlist.getPlaylistId()
            MediaMenuBottomSheet(
                isVisible = showPlaylistMenu,
                onDismiss = { showPlaylistMenu = false },
                title = playlist.name,
                subtitle = "${playlist.songCount} songs",
                thumbnailUrl = playlist.thumbnailUrl,
                onShuffle = {
                    viewModel.withPlaylistSongs(playlistId) { songs ->
                        if (songs.isNotEmpty()) {
                            onSongClick(songs.shuffled(), 0)
                        }
                    }
                },
                onStartRadio = {
                    viewModel.withPlaylistSongs(playlistId) { songs ->
                        if (songs.isNotEmpty()) {
                            onSongClick(songs.shuffled(), 0)
                        }
                    }
                },
                onPlayNext = playerConnection?.let { connection ->
                    { viewModel.playPlaylistNext(playlistId, connection) }
                },
                onAddToQueue = playerConnection?.let { connection ->
                    { viewModel.addPlaylistToQueue(playlistId, connection) }
                },
                onAddToPlaylist = {
                    viewModel.withPlaylistSongs(playlistId) { songs ->
                        playlistViewModel.showAddToPlaylistSheet(songs)
                    }
                },
                onDownload = { viewModel.downloadPlaylist(playlistId) },
                onShare = { sharePlaylist(context, playlist) },
                onExport = { showExportDialog = true },
                onRename = { showRenameDialog = true },
                onDelete = { showDeleteDialog = true }
            )

            RenamePlaylistDialog(
                isVisible = showRenameDialog,
                currentName = playlist.name,
                isRenaming = false,
                onDismiss = { showRenameDialog = false },
                onRename = { newName ->
                    viewModel.renamePlaylist(playlistId, newName)
                    showRenameDialog = false
                }
            )

            DeletePlaylistDialog(
                isVisible = showDeleteDialog,
                playlistTitle = playlist.name,
                isDeleting = false,
                onDismiss = { showDeleteDialog = false },
                onDelete = {
                    viewModel.deletePlaylist(playlistId)
                    showDeleteDialog = false
                }
            )

            ExportPlaylistDialog(
                isVisible = showExportDialog,
                onDismiss = { showExportDialog = false },
                onExportM3U = { exportM3ULauncher.launch(exportFileName(playlist.name, "m3u")) },
                onExportOmni = { exportOmniLauncher.launch(exportFileName(playlist.name, "omni")) }
            )
        }

        CreatePlaylistDialog(
            isVisible = playlistMgmtState.showCreatePlaylistDialog,
            isCreating = playlistMgmtState.isCreatingPlaylist,
            onDismiss = playlistViewModel::hideCreatePlaylistDialog,
            onCreate = { title, description, isPrivate, syncWithYt ->
                playlistViewModel.createPlaylist(title, description, isPrivate, syncWithYt)
            }
        )
    }
}

private fun sharePlaylist(context: Context, playlist: PlaylistDisplayItem) {
    val url = playlist.url.ifBlank { "omnitune://playlist/${playlist.getPlaylistId()}" }
    val shareText = buildString {
        append(playlist.name)
        if (playlist.uploaderName.isNotBlank()) {
            append('\n')
            append(playlist.uploaderName)
        }
        append("\n\n")
        append(url)
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, playlist.name)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Playlist"))
}

private fun exportFileName(name: String, extension: String): String {
    val safeName = name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "playlist" }
    return "$safeName.$extension"
}

private fun handleSmartPlaylistClick(
    type: SmartPlaylistType,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onDownloadsClick: () -> Unit,
    viewModel: LibraryViewModel
) {
    when (type) {
        SmartPlaylistType.LIKED -> {
            viewModel.syncLikedSongs()
            onPlaylistClick(PlaylistDisplayItem(id = "LM", name = "Liked Songs", url = "", uploaderName = "You", thumbnailUrl = null, songCount = 0))
        }
        SmartPlaylistType.DOWNLOADED -> onDownloadsClick()
        SmartPlaylistType.DEVICE_SONGS -> {
            onPlaylistClick(PlaylistDisplayItem(id = "DEVICE_SONGS", name = "Device Files", url = "", uploaderName = "You", thumbnailUrl = null, songCount = 0))
        }
        SmartPlaylistType.TOP_50 -> {
            onPlaylistClick(PlaylistDisplayItem(id = "TOP_50", name = "My Top 50", url = "", uploaderName = "You", thumbnailUrl = null, songCount = 0))
        }
        SmartPlaylistType.CACHED -> {
            onPlaylistClick(PlaylistDisplayItem(id = "CACHED_ALL", name = "Cached Songs", url = "", uploaderName = "You", thumbnailUrl = null, songCount = 0))
        }
    }
}

@Composable
private fun LibraryTopBar(
    onHistoryClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSyncClick) {
                Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LibraryFilterChips(
    selectedFilter: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(LibraryFilter.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.title) },
                shape = SquircleShape
            )
        }
    }
}

@Composable
private fun LibraryControlBar(
    sortOption: LibrarySortOption,
    viewMode: LibraryViewMode,
    onSortClick: () -> Unit,
    onViewModeClick: () -> Unit,
    itemCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onSortClick) {
            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (sortOption == LibrarySortOption.DATE_ADDED) "Recently Added" else "Alphabetical")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onViewModeClick) {
                Icon(
                    if (viewMode == LibraryViewMode.GRID) Icons.Default.GridView else Icons.AutoMirrored.Filled.List,
                    contentDescription = "View Mode"
                )
            }
        }
    }
}

@Composable
private fun PlaylistsGrid(
    uiState: LibraryUiState,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onSmartPlaylistClick: (SmartPlaylistType) -> Unit,
    onMoreClick: (PlaylistDisplayItem) -> Unit
) {
    val gridState = rememberLazyGridState()
    val chromeInsets = LocalRouteChromeInsets.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        state = gridState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = chromeInsets.contentBottomPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().stylishScrollbar(gridState, MaterialTheme.colorScheme.primary)
    ) {
        // Smart Playlists
        item {
            SmartPlaylistCard(
                title = "Liked Songs",
                subtitle = "${uiState.likedSongsCount} songs",
                icon = Icons.Default.Favorite,
                gradientColors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
                onClick = { onSmartPlaylistClick(SmartPlaylistType.LIKED) }
            )
        }
        item {
            SmartPlaylistCard(
                title = "Downloads",
                subtitle = "Offline tracks",
                icon = Icons.Outlined.FileDownload,
                gradientColors = listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
                onClick = { onSmartPlaylistClick(SmartPlaylistType.DOWNLOADED) }
            )
        }
        item {
            SmartPlaylistCard(
                title = "Device Files",
                subtitle = "${uiState.deviceSongsCount} songs",
                icon = Icons.Default.Folder,
                gradientColors = listOf(Color(0xFFFF8008), Color(0xFFFFC837)),
                onClick = { onSmartPlaylistClick(SmartPlaylistType.DEVICE_SONGS) }
            )
        }
        item {
            SmartPlaylistCard(
                title = "My Top 50",
                subtitle = "${uiState.top50SongCount} songs",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                gradientColors = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
                onClick = { onSmartPlaylistClick(SmartPlaylistType.TOP_50) }
            )
        }
        item {
            SmartPlaylistCard(
                title = "Cached Songs",
                subtitle = "${uiState.cachedSongCount} songs",
                icon = Icons.Default.Cached,
                gradientColors = listOf(Color(0xFF3A7BD5), Color(0xFF3A6073)),
                onClick = { onSmartPlaylistClick(SmartPlaylistType.CACHED) }
            )
        }

        // User playlists
        items(uiState.playlists) { playlist ->
            PlaylistGridItem(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) },
                onMoreClick = { onMoreClick(playlist) }
            )
        }
    }
}

@Composable
private fun PlaylistsList(
    uiState: LibraryUiState,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onSmartPlaylistClick: (SmartPlaylistType) -> Unit,
    onMoreClick: (PlaylistDisplayItem) -> Unit
) {
    val listState = rememberLazyListState()
    val chromeInsets = LocalRouteChromeInsets.current
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = chromeInsets.contentBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().stylishScrollbar(listState, MaterialTheme.colorScheme.primary)
    ) {
        item {
            SmartPlaylistListItem(
                title = "Liked Songs",
                subtitle = "${uiState.likedSongsCount} songs",
                icon = Icons.Default.Favorite,
                iconColor = Color(0xFF8E2DE2),
                onClick = { onSmartPlaylistClick(SmartPlaylistType.LIKED) }
            )
        }
        item {
            SmartPlaylistListItem(
                title = "Downloads",
                subtitle = "Offline tracks",
                icon = Icons.Outlined.FileDownload,
                iconColor = Color(0xFF11998E),
                onClick = { onSmartPlaylistClick(SmartPlaylistType.DOWNLOADED) }
            )
        }
        item {
            SmartPlaylistListItem(
                title = "Device Files",
                subtitle = "${uiState.deviceSongsCount} songs",
                icon = Icons.Default.Folder,
                iconColor = Color(0xFFFF8008),
                onClick = { onSmartPlaylistClick(SmartPlaylistType.DEVICE_SONGS) }
            )
        }
        item {
            SmartPlaylistListItem(
                title = "My Top 50",
                subtitle = "${uiState.top50SongCount} songs",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconColor = Color(0xFFFF416C),
                onClick = { onSmartPlaylistClick(SmartPlaylistType.TOP_50) }
            )
        }
        item {
            SmartPlaylistListItem(
                title = "Cached Songs",
                subtitle = "${uiState.cachedSongCount} songs",
                icon = Icons.Default.Cached,
                iconColor = Color(0xFF3A7BD5),
                onClick = { onSmartPlaylistClick(SmartPlaylistType.CACHED) }
            )
        }

        items(uiState.playlists) { playlist ->
            PlaylistListItem(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) },
                onMoreClick = { onMoreClick(playlist) }
            )
        }
    }
}

@Composable
private fun SmartPlaylistCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = SquircleShape,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp).align(Alignment.TopStart)
            )

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun SmartPlaylistListItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(SquircleShape)
                .background(iconColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlaylistGridItem(
    playlist: PlaylistDisplayItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(SquircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (!playlist.thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp).align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onMoreClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PlaylistListItem(
    playlist: PlaylistDisplayItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(SquircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (!playlist.thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp).align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.uploaderName} • ${playlist.songCount} songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onMoreClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
        }
    }
}
