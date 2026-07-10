package com.omnitune.app.ui.screens.search

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.omnitune.app.LocalDatabase
import com.omnitune.app.LocalDownloadUtil
import com.omnitune.app.R
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.db.entities.PlaylistSongMap
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.component.OmniTuneLoader
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.utils.completed
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun LazyListScope.playlistSearchResults(
    playlists: List<PlaylistItem>,
    onNavigateToPlaylist: (PlaylistItem) -> Unit,
) {
    if (playlists.isNotEmpty()) {
        item(contentType = "section-playlists") {
            SectionLabel(title = "Playlists", count = playlists.size)
        }
        items(
            items = playlists,
            key = { "playlist-${it.id}" },
            contentType = { "playlist" },
        ) { playlist ->
            val context = LocalContext.current
            val database = LocalDatabase.current
            val downloadUtil = LocalDownloadUtil.current
            val scope = rememberCoroutineScope()
            val existingPlaylist by database.playlistByBrowseId(playlist.id).collectAsState(initial = null)
            val isSaving = remember(playlist.id) { mutableStateOf(false) }
            val isDownloading = remember(playlist.id) { mutableStateOf(false) }
            SearchResultRow(
                title = playlist.title,
                subtitle = playlist.author?.name ?: "Playlist",
                thumbnailUrl = playlist.thumbnail,
                fallbackRes = R.drawable.ic_list,
                onClick = { onNavigateToPlaylist(playlist) },
                statusText = "Open collection",
                trailingContent = {
                    Row {
                        IconButton(
                            enabled = !isSaving.value,
                            onClick = {
                                scope.launch {
                                    if (existingPlaylist?.playlist?.bookmarkedAt != null) {
                                        Toast.makeText(context, "Already saved to Library playlists", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    isSaving.value = true
                                    val result = withContext(Dispatchers.IO) {
                                        YouTube.playlist(playlist.id).completed().mapCatching { page ->
                                            val songs = page.songs.map { it.toMediaMetadata() }
                                            database.withTransaction {
                                                val entity = existingPlaylist?.playlist?.copy(
                                                    bookmarkedAt = LocalDateTime.now(),
                                                    name = playlist.title,
                                                    thumbnailUrl = playlist.thumbnail,
                                                    remoteSongCount = playlist.songCountText?.let {
                                                        Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                                    },
                                                    playEndpointParams = playlist.playEndpoint?.params,
                                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                                    radioEndpointParams = playlist.radioEndpoint?.params,
                                                ) ?: PlaylistEntity(
                                                    name = playlist.title,
                                                    browseId = playlist.id,
                                                    thumbnailUrl = playlist.thumbnail,
                                                    isEditable = false,
                                                    bookmarkedAt = LocalDateTime.now(),
                                                    remoteSongCount = playlist.songCountText?.let {
                                                        Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                                    },
                                                    playEndpointParams = playlist.playEndpoint?.params,
                                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                                    radioEndpointParams = playlist.radioEndpoint?.params,
                                                )
                                                if (existingPlaylist == null) {
                                                    insert(entity)
                                                } else {
                                                    update(entity)
                                                }
                                                songs.forEach(::insert)
                                                songs.mapIndexed { index, song ->
                                                    PlaylistSongMap(
                                                        playlistId = entity.id,
                                                        songId = song.id,
                                                        position = index,
                                                        setVideoId = song.setVideoId,
                                                    )
                                                }.forEach { map ->
                                                    if (checkInPlaylist(entity.id, map.songId) == 0) {
                                                        insert(map)
                                                    }
                                                }
                                            }
                                            songs.size
                                        }
                                    }
                                    result.fold(
                                        onSuccess = { count ->
                                            Toast.makeText(context, "Saved ${playlist.title} with $count songs", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = {
                                            Toast.makeText(context, it.message ?: "Could not save playlist", Toast.LENGTH_SHORT).show()
                                        },
                                    )
                                    isSaving.value = false
                                }
                            },
                        ) {
                            if (isSaving.value) {
                                OmniTuneLoader(size = 18.dp)
                            } else {
                                Icon(
                                    painter = painterResource(
                                        if (existingPlaylist?.playlist?.bookmarkedAt != null) {
                                            R.drawable.ic_favorite
                                        } else {
                                            R.drawable.ic_add
                                        },
                                    ),
                                    contentDescription = "Save playlist",
                                    tint = if (existingPlaylist?.playlist?.bookmarkedAt != null) {
                                        OmniColors.Hot
                                    } else {
                                        OmniColors.TextSecondary
                                    },
                                )
                            }
                        }
                        IconButton(
                            enabled = !isDownloading.value,
                            onClick = {
                                scope.launch {
                                    isDownloading.value = true
                                    val result = withContext(Dispatchers.IO) {
                                        YouTube.playlist(playlist.id).completed().mapCatching { page ->
                                            val songs = page.songs.map { it.toMediaMetadata() }
                                            database.withTransaction {
                                                songs.forEach(::insert)
                                            }
                                            page.songs
                                        }
                                    }
                                    val songs = result.getOrNull().orEmpty()
                                    if (songs.isNotEmpty()) {
                                        songs.forEach { song ->
                                            downloadUtil.enqueue(song.id, song.title)
                                        }
                                        Toast.makeText(context, "Queued ${songs.size} playlist downloads", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            result.exceptionOrNull()?.message ?: "Could not load playlist songs",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    isDownloading.value = false
                                }
                            },
                        ) {
                            if (isDownloading.value) {
                                OmniTuneLoader(size = 18.dp)
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_download),
                                    contentDescription = "Download playlist",
                                    tint = OmniColors.TextSecondary,
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}
