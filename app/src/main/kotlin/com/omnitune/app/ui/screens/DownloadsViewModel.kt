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

data class DownloadsUiState(
    val downloads: List<Download> = emptyList(),
)

@HiltViewModel
@androidx.media3.common.util.UnstableApi
class DownloadsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadUtil: DownloadUtil,
    private val streamExtractor: StreamExtractor
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

    fun startDownload(videoId: String, title: String) {
        viewModelScope.launch {
            try {
                val result = streamExtractor.extractWithFallback(videoId, StreamQuality.HIGH)
                if (result != null) {
                    val request = DownloadRequest.Builder(videoId, android.net.Uri.parse(result.url))
                        .setCustomCacheKey(videoId)
                        .setData(title.toByteArray(Charsets.UTF_8))
                        .build()

                    DownloadService.sendAddDownload(
                        context,
                        ExoDownloadService::class.java,
                        request,
                        false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start download for $videoId")
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
