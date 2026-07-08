package com.omnitune.app.ui.screens.search

import android.widget.Toast
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.omnitune.app.LocalDatabase
import com.omnitune.app.R
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.playback.ExoDownloadService
import com.omnitune.app.ui.component.OmniTuneLoader
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.AlbumItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun LazyListScope.albumSearchResults(
    albums: List<AlbumItem>,
    onNavigateToAlbum: (String) -> Unit
) {
    if (albums.isNotEmpty()) {
        item(contentType = "section-albums") {
            SectionLabel(title = "Albums", count = albums.size)
        }
        items(
            items = albums,
            key = { "album-${it.browseId}" },
            contentType = { "album" },
        ) { album ->
            val context = LocalContext.current
            val database = LocalDatabase.current
            val scope = rememberCoroutineScope()
            val isDownloading = remember(album.browseId) { mutableStateOf(false) }
            SearchResultRow(
                title = album.title,
                subtitle = album.artists?.joinToString(", ") { it.name }.orEmpty().ifBlank { "Album" },
                thumbnailUrl = album.thumbnail,
                fallbackRes = R.drawable.ic_album,
                onClick = { onNavigateToAlbum(album.browseId) },
                trailingContent = {
                    IconButton(
                        enabled = !isDownloading.value,
                        onClick = {
                            scope.launch {
                                isDownloading.value = true
                                val result = withContext(Dispatchers.IO) {
                                    YouTube.album(album.browseId).mapCatching { albumPage ->
                                        database.withTransaction {
                                            insert(albumPage)
                                        }
                                        albumPage.songs
                                    }
                                }
                                val songs = result.getOrNull().orEmpty()
                                if (songs.isNotEmpty()) {
                                    songs.forEach { song ->
                                        val request = DownloadRequest.Builder(song.id, song.id.toUri())
                                            .setCustomCacheKey(song.id)
                                            .setData(song.title.toByteArray())
                                            .build()
                                        DownloadService.sendAddDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            request,
                                            false,
                                        )
                                    }
                                    Toast.makeText(context, "Queued ${songs.size} album downloads", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        result.exceptionOrNull()?.message ?: "Could not load album songs",
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
                                contentDescription = "Download album",
                                tint = OmniColors.TextSecondary,
                            )
                        }
                    }
                },
            )
        }
    }
}
