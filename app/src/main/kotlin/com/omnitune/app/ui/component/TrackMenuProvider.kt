package com.omnitune.app.ui.component

import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.playback.queues.YouTubeQueue
import com.omnitune.app.ui.screens.DownloadsViewModel
import com.omnitune.app.ui.screens.LibraryViewModel
import com.omnitune.app.ui.theme.OmniColors

@Composable
fun TrackMenuProvider(
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    mediaMetadata: MediaMetadata,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onMoreLikeThis: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onViewArtist: (() -> Unit)? = null,
    onViewAlbum: (() -> Unit)? = null,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    downloadsViewModel: DownloadsViewModel = hiltViewModel(),
) {
    if (!showMenu) return

    val context = androidx.compose.ui.platform.LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val dbSong by libraryViewModel.song(mediaMetadata.id).collectAsState(initial = null)
    val isLiked = dbSong?.song?.liked == true
    val isInLibrary = dbSong?.song?.inLibrary != null
    val downloadsState by downloadsViewModel.uiState.collectAsState()
    val download = downloadsState.downloads.firstOrNull { it.request.id == mediaMetadata.id }
    val downloadLabel = when (download?.state) {
        Download.STATE_COMPLETED -> "Remove download"
        Download.STATE_QUEUED, Download.STATE_DOWNLOADING, Download.STATE_RESTARTING -> "Cancel download"
        else -> "Download"
    }
    
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    val playlists by libraryViewModel.editablePlaylists.collectAsState(initial = emptyList())

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Song details") },
            text = {
                Text(
                    listOf(
                        "Title: ${mediaMetadata.title}",
                        "Artist: ${mediaMetadata.artists.joinToString { it.name }.ifBlank { "Unknown" }}",
                        "Album: ${mediaMetadata.album?.title ?: "Unknown"}",
                        "Duration: ${mediaMetadata.duration.takeIf { it > 0 }?.let { "${it / 60}:${(it % 60).toString().padStart(2, '0')}" } ?: "Unknown"}",
                        "ID: ${mediaMetadata.id}",
                    ).joinToString("\n"),
                )
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("Done", color = OmniColors.Hot)
                }
            },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
            textContentColor = OmniColors.TextSecondary,
        )
    }

    if (showMenu && !showPlaylistDialog) {
        TrackOptionsBottomSheet(
            title = mediaMetadata.title,
            subtitle = mediaMetadata.artists.joinToString { it.name }.ifBlank { "Song" },
            thumbnailUrl = mediaMetadata.thumbnailUrl,
            isLiked = isLiked,
            onDismissRequest = onDismissMenu,
            onToggleLike = {
                // Ensure the song is in the DB before toggling like
                libraryViewModel.ensureSongExists(mediaMetadata)
                libraryViewModel.toggleLike(mediaMetadata.id)
            },
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onMoreLikeThis = onMoreLikeThis,
            onRemoveFromPlaylist = onRemoveFromPlaylist,
            onAddToPlaylist = { showPlaylistDialog = true },
            onStartRadio = {
                playerConnection?.playQueue(YouTubeQueue.radio(mediaMetadata))
            },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaMetadata.id}")
                }
                context.startActivity(Intent.createChooser(intent, "Share via"))
            },
            onDownload = {
                when (download?.state) {
                    Download.STATE_COMPLETED,
                    Download.STATE_QUEUED,
                    Download.STATE_DOWNLOADING,
                    Download.STATE_RESTARTING -> downloadsViewModel.removeDownload(mediaMetadata.id)
                    else -> downloadsViewModel.startDownload(mediaMetadata.id, mediaMetadata.title)
                }
            },
            downloadLabel = downloadLabel,
            onToggleLibrary = {
                libraryViewModel.ensureSongExists(mediaMetadata)
                libraryViewModel.toggleLibrary(mediaMetadata.id)
            },
            libraryLabel = if (isInLibrary) "Remove from library" else "Add to library",
            onViewArtist = onViewArtist,
            onViewAlbum = onViewAlbum,
            onDetails = { showDetails = true },
        )
    }

    if (showPlaylistDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val scope = rememberCoroutineScope()
        AddToPlaylistDialog(
            playlists = playlists,
            onDismissRequest = { 
                showPlaylistDialog = false
                onDismissMenu()
            },
            onPlaylistSelected = { playlist ->
                libraryViewModel.ensureSongExists(mediaMetadata)
                scope.launch {
                    val added = libraryViewModel.addToPlaylist(playlist, mediaMetadata.id)
                    if (!added) {
                        android.widget.Toast.makeText(context, "Already in playlist", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "Added to playlist", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCreatePlaylist = { name ->
                libraryViewModel.ensureSongExists(mediaMetadata)
                libraryViewModel.createPlaylist(name, mediaMetadata.id)
                android.widget.Toast.makeText(context, "Playlist created", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
}
