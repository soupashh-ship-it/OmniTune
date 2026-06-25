package com.omnitune.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.omnitune.app.data.StreamExtractor
import com.omnitune.app.models.StreamQuality
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
    private val streamExtractor: StreamExtractor,
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
    }

    override fun onCleared() {
        super.onCleared()
        downloadUtil.downloadManager.removeListener(listener)
    }

    private fun refreshDownloads() {
        val cursor = downloadUtil.downloadManager.downloadIndex.getDownloads()
        val list = mutableListOf<Download>()
        try {
            while (cursor.moveToNext()) {
                list.add(cursor.download)
            }
        } finally {
            cursor.close()
        }
        _uiState.value = DownloadsUiState(downloads = list)
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
            val dbSong = withContext(Dispatchers.IO) { database.getSongById(download.request.id) }
            val mediaItem = if (dbSong != null) {
                dbSong.toMediaItem()
            } else {
                Timber.i("Diagnostics: Metadata fallback for non-DB-backed download")
                val title = String(download.request.data, Charsets.UTF_8)
                androidx.media3.common.MediaItem.Builder()
                    .setMediaId(download.request.id)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(title)
                            .build()
                    )
                    .build()
            }
            playerConnection?.playQueue(com.omnitune.app.playback.queues.ListQueue(items = listOf(mediaItem)))
        }
    }

    fun startDownload(
        videoId: String,
        title: String,
        resolvedStreamUrl: String? = null,
        onResult: (success: Boolean, message: String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                Timber.d("Starting download resolve for %s", videoId)
                val streamUrl = resolvedStreamUrl ?: streamExtractor.extractWithFallback(videoId, StreamQuality.HIGH)?.url
                if (streamUrl != null) {
                    val request = DownloadRequest.Builder(videoId, android.net.Uri.parse(streamUrl))
                        .setCustomCacheKey(videoId)
                        .setData(title.toByteArray(Charsets.UTF_8))
                        .build()

                    DownloadService.sendAddDownload(
                        context,
                        ExoDownloadService::class.java,
                        request,
                        false
                    )
                    Timber.i("Download request queued for %s", videoId)
                    refreshDownloads()
                    onResult(true, "Download queued")
                } else {
                    Timber.w("Download failed to resolve stream for %s", videoId)
                    onResult(false, "Download failed: stream unavailable")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start download for $videoId")
                onResult(false, "Download failed")
            }
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
}
