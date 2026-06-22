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
import dagger.hilt.android.AndroidEntryPoint
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

    override fun onCreate() {
        super.onCreate()
        notificationHelper = DownloadNotificationHelper(this, DOWNLOAD_CHANNEL_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun getDownloadManager(): DownloadManager {
        val downloadManager = downloadUtil.downloadManager
        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: java.lang.Exception?
            ) {
                if (download.state == Download.STATE_FAILED && finalException is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException && finalException.responseCode == 403) {
                    val videoId = download.request.id
                    serviceScope.launch {
                        val result = streamExtractor.extractWithFallback(videoId, com.omnitune.app.models.StreamQuality.HIGH)
                        if (result != null) {
                            val newRequest = androidx.media3.exoplayer.offline.DownloadRequest.Builder(download.request.id, android.net.Uri.parse(result.url))
                                .setCustomCacheKey(download.request.customCacheKey)
                                .setData(download.request.data)
                                .build()
                            DownloadService.sendAddDownload(
                                this@ExoDownloadService,
                                ExoDownloadService::class.java,
                                newRequest,
                                false
                            )
                        }
                    }
                }
            }
        })
        return downloadManager
    }

    override fun getScheduler(): Scheduler =
        androidx.media3.exoplayer.workmanager.WorkManagerScheduler(this, "OmniTuneDownloadWorker")

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        return notificationHelper.buildProgressNotification(
            this,
            android.R.drawable.stat_sys_download,
            null,
            null,
            downloads,
            notMetRequirements
        )
    }
}
