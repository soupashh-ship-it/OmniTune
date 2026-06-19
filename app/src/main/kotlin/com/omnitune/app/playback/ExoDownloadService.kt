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

    private lateinit var notificationHelper: DownloadNotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper = DownloadNotificationHelper(this, DOWNLOAD_CHANNEL_ID)
    }

    override fun getDownloadManager(): DownloadManager {
        // Use a simple DownloadManager with the download cache
        val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
        val executor = java.util.concurrent.Executors.newFixedThreadPool(4)
        return DownloadManager(
            this,
            downloadUtil.databaseProvider,
            downloadUtil.downloadCache,
            dataSourceFactory,
            executor
        ).apply {
            maxParallelDownloads = 3
        }
    }

    override fun getScheduler(): Scheduler =
        PlatformScheduler(this, JOB_ID)

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
