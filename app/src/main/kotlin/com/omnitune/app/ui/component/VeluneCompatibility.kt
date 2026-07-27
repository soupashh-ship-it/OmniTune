package com.omnitune.app.ui.component

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.omnitune.app.LocalDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.omnitune.app.constants.ListThumbnailSize
import com.omnitune.app.db.entities.Album
import com.omnitune.app.db.entities.Artist
import com.omnitune.app.db.entities.Playlist
import com.omnitune.innertube.models.YTItem

@Composable
fun AlbumListItem(
    modifier: Modifier = Modifier,
    album: Album,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    showLikedIcon: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {},
    trailingContent: @Composable () -> Unit = {}
) {
    androidx.compose.material3.ListItem(
        modifier = modifier,
        headlineContent = { Text(album.album.title) },
        supportingContent = { Text(album.artists.joinToString { it.name }) },
        leadingContent = {
            AsyncImage(
                model = album.album.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(6.dp))
            )
        },
        trailingContent = trailingContent
    )
}

@Composable
fun ArtistListItem(
    modifier: Modifier = Modifier,
    artist: Artist,
    showLikedIcon: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {},
    trailingContent: @Composable () -> Unit = {}
) {
    androidx.compose.material3.ListItem(
        modifier = modifier,
        headlineContent = { Text(artist.artist.name) },
        leadingContent = {
            AsyncImage(
                model = artist.artist.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(androidx.compose.foundation.shape.CircleShape)
            )
        },
        trailingContent = trailingContent
    )
}

@Composable
fun PlaylistListItem(
    modifier: Modifier = Modifier,
    playlist: Playlist,
    showLikedIcon: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {},
    trailingContent: @Composable () -> Unit = {}
) {
    androidx.compose.material3.ListItem(
        modifier = modifier,
        headlineContent = { Text(playlist.playlist.name) },
        supportingContent = { Text("${playlist.songCount} songs") },
        leadingContent = {
            AsyncImage(
                model = playlist.playlist.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(6.dp))
            )
        },
        trailingContent = trailingContent
    )
}

@Composable
fun AddToPlaylistDialog(
    isVisible: Boolean,
    onGetSong: suspend (Playlist) -> List<String>,
    onDismiss: () -> Unit,
    onAddComplete: (Int, List<String>) -> Unit
) {
    if (!isVisible) return
    val database = LocalDatabase.current
    val playlists by database.playlists(com.omnitune.app.constants.PlaylistSortType.CREATE_DATE, false).collectAsState(initial = emptyList())
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    com.omnitune.app.ui.component.AddToPlaylistDialog(
        playlists = playlists,
        onDismissRequest = onDismiss,
        onPlaylistSelected = { playlist ->
            coroutineScope.launch {
                val songIds = onGetSong(playlist)
                if (songIds.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        database.addSongToPlaylist(playlist, songIds)
                    }
                    onAddComplete(songIds.size, listOf(playlist.playlist.name))
                }
            }
            onDismiss()
        },
        onCreatePlaylist = { name ->
            coroutineScope.launch {
                val newPlaylist = com.omnitune.app.db.entities.PlaylistEntity(name = name)
                val playlist = Playlist(
                    playlist = newPlaylist,
                    songCount = 0,
                    songThumbnails = emptyList(),
                )
                withContext(Dispatchers.IO) {
                    database.insert(newPlaylist)
                }
                val songIds = onGetSong(playlist)
                if (songIds.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        database.addSongToPlaylist(playlist, songIds)
                    }
                }
                onAddComplete(songIds.size, listOf(name))
                onDismiss()
            }
        }
    )
}


@Composable
fun YouTubeListItem(
    item: YTItem,
    showLikedIcon: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {},
    trailingContent: @Composable () -> Unit = {}
) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(item.title) },
        leadingContent = {
            AsyncImage(
                model = item.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(6.dp))
            )
        },
        trailingContent = trailingContent
    )
}

@Composable
fun EditPlaylistDialog(
    initialName: String,
    initialThumbnailUrl: String?,
    fallbackThumbnails: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    TextFieldDialog(
        title = { Text("Edit Playlist") },
        placeholder = { Text("Playlist Name") },
        initialValue = initialName,
        onDismiss = onDismiss,
        onDone = {
            onSave(it, initialThumbnailUrl)
        }
    )
}
