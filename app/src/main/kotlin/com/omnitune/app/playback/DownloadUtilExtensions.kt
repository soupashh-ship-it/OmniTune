package com.omnitune.app.playback

import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.db.entities.PlaylistSongMap
import com.omnitune.app.models.MediaMetadata
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime

val DownloadUtil.downloads: Flow<Map<String, Download>>
    get() = callbackFlow {
        val sendDownloads = {
            val cursor = downloadManager.downloadIndex.getDownloads()
            val map = mutableMapOf<String, Download>()
            try {
                while (cursor.moveToNext()) {
                    val download = cursor.download
                    map[download.request.id] = download
                }
            } finally {
                cursor.close()
            }
            trySend(map)
        }
        
        sendDownloads()

        val poller = launch {
            while (isActive) {
                delay(500)
                sendDownloads()
            }
        }
        
        val listener = object : DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?
            ) {
                sendDownloads()
            }
            override fun onDownloadRemoved(
                downloadManager: DownloadManager,
                download: Download
            ) {
                sendDownloads()
            }
        }
        
        downloadManager.addListener(listener)
        awaitClose {
            poller.cancel()
            downloadManager.removeListener(listener)
        }
    }

fun DownloadUtil.getDownload(id: String): Flow<Download?> {
    return downloads.map { it[id] }
}

suspend fun DownloadUtil.enqueueCollection(
    database: MusicDatabase,
    playlistId: String,
    name: String,
    thumbnailUrl: String?,
    songs: List<MediaMetadata>,
) {
    if (songs.isEmpty()) return

    database.withTransaction {
        val now = LocalDateTime.now()
        val existing = getPlaylistByIdBlocking(playlistId)?.playlist
        val playlist = existing?.copy(
            name = name,
            thumbnailUrl = thumbnailUrl ?: existing.thumbnailUrl,
            bookmarkedAt = existing.bookmarkedAt ?: now,
            lastUpdateTime = now,
            isDownloaded = false,
        ) ?: PlaylistEntity(
            id = playlistId,
            name = name,
            thumbnailUrl = thumbnailUrl,
            bookmarkedAt = now,
            isDownloaded = false,
        )

        if (existing == null) insert(playlist) else update(playlist)
        clearPlaylist(playlistId)
        songs.forEachIndexed { index, song ->
            insert(song)
            insert(PlaylistSongMap(playlistId = playlistId, songId = song.id, position = index))
        }
    }

    songs.forEach { song -> enqueue(song.id, song.title) }
}

fun downloadCollectionId(type: String, sourceId: String): String = "DL_${type}_$sourceId"
