package com.omnitune.app.playback

import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
