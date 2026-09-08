package com.omnitune.app.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.omnitune.app.LocalDownloadUtil
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.models.Album
import com.omnitune.app.models.Playlist
import com.omnitune.app.models.Song
import com.omnitune.app.models.toMediaItem
import com.omnitune.app.ui.component.AddToPlaylistSheet
import com.omnitune.app.ui.component.CreatePlaylistDialog
import com.omnitune.app.ui.component.SongMenuBottomSheet
import com.omnitune.app.ui.component.shimmer.ShimmerHost
import com.omnitune.app.ui.component.shimmer.ShimmerShape
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.app.ui.utils.dpadFocusable

@Composable
fun BrowseDetailScreen(
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlaylistClick: (String) -> Unit,
    currentSong: Song? = null,
    viewModel: BrowseDetailViewModel = hiltViewModel(),
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlistMgmtState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val downloadUtil = LocalDownloadUtil.current

    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showSongMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(contentType = "header") {
                BrowseDetailHeader(
                    title = uiState.title,
                    onBackClick = onBackClick,
                )
            }

            when {
                uiState.isLoading -> item(contentType = "loading") {
                    BrowseDetailLoading()
                }

                uiState.error != null && uiState.sections.isEmpty() -> item(contentType = "error") {
                    BrowseDetailError(
                        message = uiState.error.orEmpty(),
                        onRetry = viewModel::retry,
                    )
                }

                uiState.sections.isEmpty() -> item(contentType = "empty") {
                    BrowseDetailEmpty()
                }

                else -> uiState.sections.forEachIndexed { sectionIndex, section ->
                    item(
                        key = "section_${sectionIndex}_${section.title}",
                        contentType = "section_header",
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    itemsIndexed(
                        items = section.items,
                        key = { index, item -> "${sectionIndex}_${index}_${item.id}" },
                        contentType = { _, item -> item::class },
                    ) { _, item ->
                        when (item) {
                            is BrowseDetailItem.SongEntry -> {
                                val songs = section.songs
                                BrowseSongRow(
                                    song = item.song,
                                    isPlaying = currentSong?.id == item.song.id,
                                    onClick = {
                                        val songIndex = songs.indexOf(item.song)
                                        if (songIndex != -1) {
                                            onSongClick(songs, songIndex)
                                        }
                                    },
                                    onMoreClick = {
                                        selectedSong = item.song
                                        showSongMenu = true
                                    },
                                )
                            }

                            is BrowseDetailItem.AlbumEntry -> {
                                AlbumSearchListItem(
                                    album = item.album,
                                    onClick = { onAlbumClick(item.album) },
                                )
                            }

                            is BrowseDetailItem.ArtistEntry -> {
                                ArtistSearchListItem(
                                    artist = item.artist,
                                    onClick = { onArtistClick(item.artist.id) },
                                )
                            }

                            is BrowseDetailItem.PlaylistEntry -> {
                                PlaylistSearchListItem(
                                    playlist = item.playlist,
                                    onClick = { onPlaylistClick(item.playlist.id) },
                                )
                            }
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
                onShare = { shareBrowseSong(context, song) },
                isCurrentlyPlaying = currentSong?.id == song.id,
            )
        }

        AddToPlaylistSheet(
            songs = playlistMgmtState.selectedSongs,
            isVisible = playlistMgmtState.showAddToPlaylistSheet,
            playlists = playlistMgmtState.userPlaylists,
            isLoading = playlistMgmtState.isLoadingPlaylists,
            onDismiss = playlistViewModel::hideAddToPlaylistSheet,
            onAddToPlaylist = playlistViewModel::addSongsToPlaylist,
            onCreateNewPlaylist = {
                playlistViewModel.hideAddToPlaylistSheet()
                playlistViewModel.showCreatePlaylistDialog()
            },
        )

        CreatePlaylistDialog(
            isVisible = playlistMgmtState.showCreatePlaylistDialog,
            isCreating = playlistMgmtState.isCreatingPlaylist,
            onDismiss = playlistViewModel::hideCreatePlaylistDialog,
            onCreate = { title, description, isPrivate, syncWithYt ->
                playlistViewModel.createPlaylist(title, description, isPrivate, syncWithYt)
            },
        )
    }
}

@Composable
private fun BrowseDetailHeader(
    title: String,
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Browse",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BrowseSongRow(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dpadFocusable(onClick = onClick, shape = SquircleShape)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(SquircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (song.artist.isBlank()) "Song" else "Song • ${song.artist}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onMoreClick)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BrowseDetailLoading() {
    ShimmerHost {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(7) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerShape(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(SquircleShape),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerShape(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .height(14.dp),
                        )
                        ShimmerShape(
                            modifier = Modifier
                                .fillMaxWidth(0.44f)
                                .height(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseDetailError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(44.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Couldn't load this collection",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(onClick = onRetry, shape = SquircleShape) {
            Text("Retry")
        }
    }
}

@Composable
private fun BrowseDetailEmpty() {
    Text(
        text = "No playable items found in this collection.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(24.dp),
    )
}

private fun shareBrowseSong(context: Context, song: Song) {
    val shareText = buildString {
        append(song.title)
        if (song.artist.isNotBlank()) {
            append('\n')
            append(song.artist)
        }
        if (song.id.isNotBlank()) {
            append("\n\nhttps://music.youtube.com/watch?v=")
            append(song.id)
        }
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "${song.title} - ${song.artist}")
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Song"))
}
