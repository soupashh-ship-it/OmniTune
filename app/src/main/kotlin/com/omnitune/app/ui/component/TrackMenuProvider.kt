package com.omnitune.app.ui.component

import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.ui.screens.LibraryViewModel

@Composable
fun TrackMenuProvider(
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    mediaMetadata: MediaMetadata,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    if (!showMenu) return

    val dbSong by libraryViewModel.song(mediaMetadata.id).collectAsState(initial = null)
    val isLiked = dbSong?.song?.liked == true
    
    var showPlaylistDialog by remember { mutableStateOf(false) }
    val playlists by libraryViewModel.editablePlaylists.collectAsState(initial = emptyList())

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
            onRemoveFromPlaylist = onRemoveFromPlaylist,
            onAddToPlaylist = { showPlaylistDialog = true }
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
