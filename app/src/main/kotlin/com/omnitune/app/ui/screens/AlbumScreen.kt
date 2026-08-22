package com.omnitune.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.pages.AlbumPage
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.component.OmniTuneLoader
import com.omnitune.app.utils.formatDurationSeconds
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.component.TrackMenuProvider
import com.omnitune.app.ui.component.SongListItem
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.R
import com.omnitune.app.db.entities.Album
import com.omnitune.app.db.entities.Song
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.playback.queues.ListQueue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    albumId: String,
    onBack: () -> Unit = {},
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    var albumPage by remember { mutableStateOf<AlbumPage?>(null) }
    var songs by remember { mutableStateOf<List<SongItem>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val localAlbum by viewModel.album.collectAsState()
    val localSongs by viewModel.songs.collectAsState()
    val playerConnection = LocalPlayerConnection.current

    LaunchedEffect(albumId) {
        isLoading = true
        error = null
        viewModel.loadAlbum(albumId)
        val pageResult = YouTube.album(albumId)
        pageResult.fold(
            onSuccess = { page ->
                albumPage = page
                val songResult = YouTube.albumSongs(page.album.playlistId, page.album)
                songResult.fold(
                    onSuccess = { songList -> songs = songList },
                    onFailure = { e -> error = e.message }
                )
            },
            onFailure = { e -> error = e.message }
        )
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(albumPage?.album?.title ?: localAlbum?.title ?: "Album") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(com.omnitune.app.R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.toggleBookmark(albumId) }) {
                    Icon(
                        painter = painterResource(
                            if (isBookmarked) R.drawable.ic_favorite
                            else R.drawable.ic_favorite_border
                        ),
                        contentDescription = if (isBookmarked) "Unlike album" else "Like album",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        when {
            isLoading && localAlbum == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    OmniTuneLoader(size = 48.dp)
                }
            }
            localAlbum != null && (albumPage == null || songs == null) -> {
                LocalAlbumContent(localAlbum ?: return, localSongs, playerConnection)
            }
            error != null && albumPage == null -> {
                EmptyPlaceholder(
                    icon = com.omnitune.app.R.drawable.ic_album,
                    text = error ?: "Failed to load album",
                )
            }
            albumPage != null -> {
                val album = albumPage?.album ?: return
                val songList = songs ?: emptyList()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            AsyncImage(
                                model = album.thumbnail,
                                contentDescription = album.title,
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .aspectRatio(1f),
                                contentScale = ContentScale.Crop,
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = album.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                album.artists?.forEach { artist ->
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                album.year?.let { year ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = year.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${songList.size} tracks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Tracks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (songList.isEmpty()) {
                        item {
                            EmptyPlaceholder(
                                icon = com.omnitune.app.R.drawable.ic_album,
                                text = "No tracks available",
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = songList,
                            key = { index, song -> "album_song_${song.id.ifBlank { index.toString() }}" },
                            contentType = { _, _ -> "album-song" },
                        ) { index, song ->
                            SongRow(
                                index = index + 1,
                                song = song,
                                onClick = {
                                    playerConnection?.playQueue(
                                        ListQueue(
                                            title = album.title,
                                            items = songList.map { it.toMediaItem() },
                                            startIndex = index,
                                        ),
                                    )
                                },
                            )
                            if (index < songList.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LocalAlbumContent(
    album: Album,
    songs: List<Song>,
    playerConnection: PlayerConnection?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = album.thumbnailUrl,
                    contentDescription = album.title,
                    modifier = Modifier.size(144.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(album.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        album.artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("${songs.size} tracks", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (songs.isNotEmpty()) playerConnection?.playQueue(
                                ListQueue(title = album.title, items = songs.map { it.toMediaItem() }),
                            )
                        },
                    ) { Text("Play") }
                }
            }
        }
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongListItem(
                song = song,
                modifier = Modifier.clickable {
                    playerConnection?.playQueue(
                        ListQueue(title = album.title, items = songs.map { it.toMediaItem() }, startIndex = index),
                    )
                },
            )
        }
    }
}

@Composable
private fun SongRow(
    index: Int,
    song: SongItem,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        song.duration?.let { duration ->
            Text(
                text = formatDurationSeconds(duration.toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        var menuExpanded by remember { mutableStateOf(false) }
        val playerConnection = LocalPlayerConnection.current
        
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(com.omnitune.app.R.drawable.ic_more_vert),
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            
            TrackMenuProvider(
                showMenu = menuExpanded,
                onDismissMenu = { menuExpanded = false },
                mediaMetadata = song.toMediaMetadata(),
                onPlayNext = { playerConnection?.playNext(song.toMediaItem()) },
                onAddToQueue = { playerConnection?.addToQueue(song.toMediaItem()) }
            )
        }
    }
}


