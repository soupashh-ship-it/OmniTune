package com.omnitune.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.omnitune.app.models.Song
import com.omnitune.app.ui.component.*
import com.omnitune.app.ui.navigation.LocalRouteChromeInsets
import com.omnitune.app.ui.theme.SquircleShape
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit = {},
    onShufflePlay: (List<Song>) -> Unit = {},
    viewModel: DownloadsViewModel = hiltViewModel(),
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel()
) {
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val downloadedVideos by viewModel.downloadedVideos.collectAsStateWithLifecycle()
    val downloadItems by viewModel.downloadItems.collectAsStateWithLifecycle()
    val storageInfo by viewModel.storageInfo.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedSongIds.collectAsStateWithLifecycle()
    val chromeInsets = LocalRouteChromeInsets.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val songItems = remember(downloadItems) {
        downloadItems.filterIsInstance<DownloadItem.SongItem>()
    }
    val currentItems = remember(songItems, selectedTab) {
        songItems.filter { item -> if (selectedTab == 0) !item.song.isVideo else item.song.isVideo }
    }
    val currentPlayableList = remember(currentItems) {
        currentItems
            .filter { it.status == DownloadUiStatus.COMPLETED }
            .map { it.song }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var showSongMenu by remember { mutableStateOf(false) }
    var selectedSongForMenu by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isSelectionMode) "${selectedIds.size} selected" else "Downloads",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) viewModel.clearSelection() else onBackClick()
                    }) {
                        Icon(
                            if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.selectAll(currentItems.map { it.song.id }) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = viewModel::refreshDownloads) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Downloads")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Delete All")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Songs (${currentSongCount(songItems)})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Videos (${currentVideoCount(songItems)})") }
                    )
                }

                DownloadStorageStrip(
                    isRefreshing = isRefreshing,
                    storageInfo = storageInfo,
                    visible = songItems.isNotEmpty() || isRefreshing || storageInfo != null,
                    onRefresh = viewModel::refreshDownloads,
                )

                if (currentItems.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onPlayAll(currentPlayableList) },
                            enabled = currentPlayableList.isNotEmpty(),
                            shape = SquircleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play All", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { onShufflePlay(currentPlayableList) },
                            enabled = currentPlayableList.isNotEmpty(),
                            shape = SquircleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Shuffle", fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = chromeInsets.contentBottomPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentItems, key = { it.song.id }) { item ->
                            DownloadSongRow(
                                item = item,
                                selected = selectedIds.contains(item.song.id),
                                selectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(item.song.id)
                                    } else if (item.status == DownloadUiStatus.COMPLETED) {
                                        val playableIndex = currentPlayableList.indexOfFirst { it.id == item.song.id }
                                        if (playableIndex >= 0) onSongClick(currentPlayableList, playableIndex)
                                    }
                                },
                                onSelectionChange = { viewModel.toggleSelection(item.song.id) },
                                onRetry = { viewModel.retryDownload(item.song.id) },
                                onCancel = { viewModel.deleteDownload(item.song.id) },
                                onDelete = {
                                    songToDelete = item.song
                                    showDeleteConfirm = true
                                },
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.DownloadDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedTab == 0) "No downloaded songs" else "No downloaded videos",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirm = false
                    songToDelete = null
                },
                title = { Text(if (songToDelete != null) "Delete Download?" else "Delete Downloads?") },
                text = {
                    Text(
                        if (songToDelete != null) {
                            "Remove \"${songToDelete?.title}\" from your downloaded songs?"
                        } else if (isSelectionMode) {
                            "Remove ${selectedIds.size} selected songs from your downloads?"
                        } else {
                            "Remove all downloaded songs from your device?"
                        }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (songToDelete != null) {
                                viewModel.deleteDownload(songToDelete!!.id)
                            } else if (isSelectionMode) {
                                viewModel.deleteSelected()
                            } else {
                                viewModel.deleteAll()
                            }
                            showDeleteConfirm = false
                            songToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        songToDelete = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun DownloadStorageStrip(
    isRefreshing: Boolean,
    storageInfo: com.omnitune.app.playback.DownloadUtil.StorageInfo?,
    visible: Boolean,
    onRefresh: () -> Unit,
) {
    AnimatedVisibility(visible = visible) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = storageInfo?.let {
                    "${formatBytes(it.downloadCacheBytes)} downloaded • ${formatBytes(it.availableBytes)} free"
                } ?: "Refreshing downloads",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Downloads")
            }
        }
    }
}

@Composable
private fun DownloadSongRow(
    item: DownloadItem.SongItem,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onSelectionChange: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val isPlayable = item.status == DownloadUiStatus.COMPLETED
    val statusSubtitle = item.statusSubtitle()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(
                enabled = selectionMode || isPlayable,
                role = if (selectionMode) Role.Checkbox else Role.Button,
                onClick = onClick,
            )
            .semantics {
                contentDescription = listOf(item.song.title, item.song.artist, statusSubtitle)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                role = if (selectionMode) Role.Checkbox else Role.Button
                if (selectionMode) {
                    this.selected = selected
                    stateDescription = if (selected) "Selected" else "Not selected"
                } else {
                    stateDescription = statusSubtitle
                }
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onSelectionChange() }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        AsyncImage(
            model = item.song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(52.dp).clip(SquircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.status in setOf(
                    DownloadUiStatus.RESOLVING,
                    DownloadUiStatus.QUEUED,
                    DownloadUiStatus.WAITING_FOR_NETWORK,
                    DownloadUiStatus.DOWNLOADING,
                )
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { item.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (item.failureReason != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.failureReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        when (item.status) {
            DownloadUiStatus.FAILED -> {
                IconButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry download ${item.song.title}")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete download ${item.song.title}",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
            DownloadUiStatus.RESOLVING,
            DownloadUiStatus.QUEUED,
            DownloadUiStatus.WAITING_FOR_NETWORK,
            DownloadUiStatus.DOWNLOADING,
            DownloadUiStatus.PAUSED -> {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel download ${item.song.title}")
                }
            }
            DownloadUiStatus.COMPLETED,
            DownloadUiStatus.REMOVING -> {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete download ${item.song.title}",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun DownloadItem.SongItem.statusSubtitle(): String {
    val artist = song.artist.takeIf { it.isNotBlank() }
    val status = when (status) {
        DownloadUiStatus.RESOLVING -> "Preparing download"
        DownloadUiStatus.QUEUED -> "Queued"
        DownloadUiStatus.WAITING_FOR_NETWORK -> "Waiting for network"
        DownloadUiStatus.DOWNLOADING -> "Downloading ${progressPercentText()}"
        DownloadUiStatus.COMPLETED -> "Downloaded"
        DownloadUiStatus.FAILED -> "Failed"
        DownloadUiStatus.PAUSED -> "Paused"
        DownloadUiStatus.REMOVING -> "Removing"
    }
    return listOfNotNull(artist, status).joinToString(" • ")
}

private fun DownloadItem.SongItem.progressPercentText(): String =
    "${(progress.coerceIn(0f, 1f) * 100f).toInt()}%"

private fun currentSongCount(items: List<DownloadItem.SongItem>): Int =
    items.count { !it.song.isVideo && it.status == DownloadUiStatus.COMPLETED }

private fun currentVideoCount(items: List<DownloadItem.SongItem>): Int =
    items.count { it.song.isVideo && it.status == DownloadUiStatus.COMPLETED }

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit += 1
    }
    return if (unit == 0) {
        "${bytes} B"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[unit])
    }
}
