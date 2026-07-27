/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.app.Notification
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.media3.exoplayer.scheduler.Scheduler
import com.omnitune.app.R
import com.omnitune.app.constants.RetryFailedDownloadsKey
import com.omnitune.app.utils.PreferenceStore
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

private const val JOB_ID = 1001
private const val DOWNLOAD_NOTIFICATION_ID = 2001
const val DOWNLOAD_CHANNEL_ID = "omnitune_downloads"

@UnstableApi
@AndroidEntryPoint
class ExoDownloadService : DownloadService(
    DOWNLOAD_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DOWNLOAD_CHANNEL_ID,
    R.string.download_channel_name, // Add this string resource (see Task 3.4a)
    0
) {
    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var streamExtractor: com.omnitune.app.data.StreamExtractor

    private lateinit var notificationHelper: DownloadNotificationHelper
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private var retryListenerAttached = false
    private val resolveRetryCounts = ConcurrentHashMap<String, Int>()
    private val retryListener = object : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?,
        ) {
            if (download.state == Download.STATE_COMPLETED) {
                resolveRetryCounts.remove(download.request.id)
            }
            if (download.state == Download.STATE_FAILED &&
                (PreferenceStore.get(RetryFailedDownloadsKey) ?: true)
            ) {
                retryWithResolvedStream(download)
            }
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            resolveRetryCounts.remove(download.request.id)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = DownloadNotificationHelper(this, DOWNLOAD_CHANNEL_ID)
    }

    override fun onDestroy() {
        if (retryListenerAttached) {
            downloadUtil.downloadManager.removeListener(retryListener)
            retryListenerAttached = false
        }
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun getDownloadManager(): DownloadManager {
        val downloadManager = downloadUtil.downloadManager
        if (!retryListenerAttached) {
            retryListenerAttached = true
            downloadManager.addListener(retryListener)
        }
        return downloadManager
    }

    private fun retryWithResolvedStream(download: Download) {
        val videoId = download.request.id.takeIf { YOUTUBE_ID_REGEX.matches(it) } ?: return
        val attempts = resolveRetryCounts.merge(videoId, 1, Int::plus) ?: 1
        if (attempts > 2) return

        serviceScope.launch {
            streamExtractor.invalidate(videoId)
            val result = streamExtractor.extractWithFallback(videoId, preferredDownloadStreamQuality())
            if (result != null) {
                val newRequest = androidx.media3.exoplayer.offline.DownloadRequest.Builder(videoId, android.net.Uri.parse(result.url))
                    .setCustomCacheKey(download.request.customCacheKey ?: videoId)
                    .setData(download.request.data)
                    .build()
                DownloadService.sendAddDownload(
                    this@ExoDownloadService,
                    ExoDownloadService::class.java,
                    newRequest,
                    false
                )
                DownloadService.sendResumeDownloads(
                    this@ExoDownloadService,
                    ExoDownloadService::class.java,
                    false,
                )
            }
        }
    }

    override fun getScheduler(): Scheduler =
        androidx.media3.exoplayer.workmanager.WorkManagerScheduler(this, "OmniTuneDownloadWorker")

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        return notificationHelper.buildProgressNotification(
            this,
            com.omnitune.app.R.drawable.ic_download,
            null,
            null,
            downloads,
            notMetRequirements
        )
    }

    companion object {
        private val YOUTUBE_ID_REGEX = Regex("^[a-zA-Z0-9_-]{11}$")
    }
}
