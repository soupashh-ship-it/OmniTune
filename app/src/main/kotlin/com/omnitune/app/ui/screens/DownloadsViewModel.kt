package com.omnitune.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import com.omnitune.app.playback.DownloadUtil
import com.omnitune.app.playback.ExoDownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.playback.PlayerConnection
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class DownloadsUiState(
    val downloads: List<Download> = emptyList(),
)

@HiltViewModel
@androidx.media3.common.util.UnstableApi
class DownloadsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadUtil: DownloadUtil,
    private val database: MusicDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val listener = object : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            refreshDownloads()
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            refreshDownloads()
        }
    }

    init {
        downloadUtil.downloadManager.addListener(listener)
        refreshDownloads()
        
        viewModelScope.launch {
            while (true) {
                if (_uiState.value.downloads.any { it.state == Download.STATE_DOWNLOADING }) {
                    refreshDownloads()
                }
                kotlinx.coroutines.delay(300)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        downloadUtil.downloadManager.removeListener(listener)
    }

    private fun refreshDownloads() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                val cursor = downloadUtil.downloadManager.downloadIndex.getDownloads()
                val result = mutableListOf<Download>()
                try {
                    while (cursor.moveToNext()) {
                        result.add(cursor.download)
                    }
                } finally {
                    cursor.close()
                }
                result
            }
            _uiState.value = DownloadsUiState(downloads = list)
        }
    }

    fun isPlayable(download: Download): Boolean {
        return downloadUtil.isPlayable(download)
    }

    fun playDownload(download: Download, playerConnection: PlayerConnection?, context: Context) {
        if (!downloadUtil.isPlayable(download)) {
            android.widget.Toast.makeText(context, "Playback rejected: Download is not ready or missing.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            val playableDownloads = _uiState.value.downloads.filter { downloadUtil.isPlayable(it) }
            val mediaItems = withContext(Dispatchers.IO) {
                playableDownloads.map { dl ->
                    val dbSong = database.getSongById(dl.request.id)
                    if (dbSong != null) {
                        dbSong.toMediaItem()
                    } else {
                        Timber.i("Diagnostics: Metadata fallback for non-DB-backed download: ${dl.request.id}")
                        val title = String(dl.request.data, Charsets.UTF_8).ifBlank { dl.request.id }
                        val downloadMeta = com.omnitune.app.models.MediaMetadata(
                            id = dl.request.id,
                            title = title,
                            artists = emptyList(),
                            duration = 0,
                        )
                        androidx.media3.common.MediaItem.Builder()
                            .setMediaId(dl.request.id)
                            .setUri(dl.request.id)
                            .setCustomCacheKey(dl.request.id)
                            .setTag(downloadMeta)
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(title)
                                    .build()
                            )
                            .build()
                    }
                }
            }
            val startIndex = playableDownloads.indexOfFirst { it.request.id == download.request.id }.coerceAtLeast(0)
            playerConnection?.playQueue(com.omnitune.app.playback.queues.ListQueue(items = mediaItems, startIndex = startIndex))
        }
    }

    fun startDownload(
        videoId: String,
        title: String,
        resolvedStreamUrl: String? = null,
        onResult: (success: Boolean, message: String) -> Unit = { _, _ -> }
    ) {
        downloadUtil.enqueue(videoId, title, resolvedStreamUrl) { success, message ->
            if (success) {
                refreshDownloads()
            }
            onResult(success, message)
        }
    }

    fun retryDownload(videoId: String) {
        val download = _uiState.value.downloads.find { it.request.id == videoId }
        if (download != null) {
            val title = String(download.request.data, Charsets.UTF_8)
            startDownload(videoId, title)
        }
    }

    fun removeDownload(videoId: String) {
        DownloadService.sendRemoveDownload(
            context,
            ExoDownloadService::class.java,
            videoId,
            false
        )
    }

    fun clearFailedDownloads() {
        _uiState.value.downloads
            .filter { it.state == Download.STATE_FAILED }
            .forEach { removeDownload(it.request.id) }
    }
}
