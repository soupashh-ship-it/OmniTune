/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.Player
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaLibraryService
import com.omnitune.app.BuildConfig
import com.omnitune.app.MainActivity
import com.omnitune.app.R
import com.omnitune.app.utils.reportException
import com.omnitune.app.widget.updateWidgetState
import timber.log.Timber

internal data class PlaybackNotificationPresentation(
    val title: String,
    val artist: String,
)

internal data class PlaybackNotificationActionSpec(
    val iconRes: Int,
    val title: String,
    val action: String,
    val requestCode: Int,
)

/** Pure notification contract shared by the platform builder and JVM regression tests. */
internal object PlaybackNotificationContract {
    fun presentation(
        title: CharSequence?,
        artist: CharSequence?,
        albumArtist: CharSequence?,
        albumTitle: CharSequence?,
        fallbackTitle: String,
    ): PlaybackNotificationPresentation = PlaybackNotificationPresentation(
        title = title?.toString()?.takeIf(String::isNotBlank) ?: fallbackTitle,
        artist = artist?.toString()?.takeIf(String::isNotBlank)
            ?: albumArtist?.toString()?.takeIf(String::isNotBlank)
            ?: albumTitle?.toString()?.takeIf(String::isNotBlank)
            ?: "Playing",
    )

    fun actions(isPlaying: Boolean, isLiked: Boolean): List<PlaybackNotificationActionSpec> = listOf(
        PlaybackNotificationActionSpec(
            R.drawable.ic_notification_previous,
            "Previous",
            PlaybackNotificationManager.ACTION_PREVIOUS,
            1,
        ),
        PlaybackNotificationActionSpec(
            if (isPlaying) R.drawable.ic_notification_pause else R.drawable.ic_notification_play,
            if (isPlaying) "Pause" else "Play",
            if (isPlaying) PlaybackNotificationManager.ACTION_PAUSE else PlaybackNotificationManager.ACTION_PLAY,
            2,
        ),
        PlaybackNotificationActionSpec(
            R.drawable.ic_notification_next,
            "Next",
            PlaybackNotificationManager.ACTION_NEXT,
            3,
        ),
        PlaybackNotificationActionSpec(
            if (isLiked) R.drawable.ic_notification_favorite else R.drawable.ic_notification_favorite_border,
            "Like",
            PlaybackNotificationManager.ACTION_LIKE,
            4,
        ),
        PlaybackNotificationActionSpec(
            R.drawable.ic_notification_repeat,
            "Repeat",
            PlaybackNotificationManager.ACTION_REPEAT,
            5,
        ),
        PlaybackNotificationActionSpec(
            R.drawable.ic_close,
            "Stop",
            PlaybackNotificationManager.ACTION_STOP,
            6,
        ),
    )
}

class PlaybackNotificationManager(
    private val service: MediaLibraryService,
    private val player: Player,
    private val sessionProvider: () -> MediaLibraryService.MediaLibrarySession?,
) {
    fun createChannelIfNeeded() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music Player",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Playback controls for OmniTune"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        val notificationManager = service.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        if (BuildConfig.DEBUG) {
            val postedChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            Timber.tag("MediaControls").d(
                "Playback notification channel ready: id=%s importance=%s visibility=%s notificationsEnabled=%s",
                CHANNEL_ID,
                postedChannel?.importance,
                postedChannel?.lockscreenVisibility,
                NotificationManagerCompat.from(service).areNotificationsEnabled()
            )
        }
    }

    fun createProvider(): MediaNotification.Provider {
        return DefaultMediaNotificationProvider.Builder(service)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.music_player_channel_name)
            .setNotificationId(NOTIFICATION_ID)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_stat_omnitune)
            }
    }

    fun updateWidget() {
        try {
            val isPlaying = player.playWhenReady &&
                player.playbackState != Player.STATE_ENDED &&
                player.playbackState != Player.STATE_IDLE
            val metadata = player.currentMediaItem?.mediaMetadata
            val title = metadata?.title?.toString() ?: "OmniTune"
            val artist = metadata?.artist?.toString() ?: "Ready to play"
            updateWidgetState(service, title, artist, isPlaying)
        } catch (e: Exception) {
            reportException(e)
        }
    }

    fun logState(event: String) {
        if (!BuildConfig.DEBUG) return
        try {
            val notificationManager = service.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            val channelImportance = notificationManager.getNotificationChannel(CHANNEL_ID)?.importance
            Timber.tag("MediaControls").d(
                "event=%s notificationsEnabled=%s channel=%s session=%s playerReady=%s state=%s playWhenReady=%s isPlaying=%s title=%s mediaId=%s count=%s",
                event,
                NotificationManagerCompat.from(service).areNotificationsEnabled(),
                channelImportance,
                sessionProvider() != null,
                true,
                player.playbackState,
                player.playWhenReady,
                player.isPlaying,
                player.currentMediaItem?.mediaMetadata?.title,
                player.currentMediaItem?.mediaId,
                player.mediaItemCount
            )
        } catch (e: Exception) {
            Timber.tag("MediaControls").w(e, "Failed to log media control state")
        }
    }

    fun postFallback(reason: String, force: Boolean = false) {
        if (sessionProvider() == null || player.currentMediaItem == null) return
        if (!force && hasActivePlaybackNotification()) return

        try {
            val notification = buildPlatformMediaNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                service.startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                service.startForeground(NOTIFICATION_ID, notification)
            }
            if (BuildConfig.DEBUG) {
                Timber.tag("MediaControls").d(
                    "Posted platform media notification fallback: reason=%s activeBefore=%s",
                    reason,
                    hasActivePlaybackNotification()
                )
            }
        } catch (e: Exception) {
            Timber.tag("MediaControls").w(e, "Failed to post platform media notification fallback")
        }
    }

    fun release() = Unit

    private fun hasActivePlaybackNotification(): Boolean {
        return try {
            val notificationManager = service.getSystemService(NotificationManager::class.java)
            notificationManager.activeNotifications.any { notification ->
                notification.id == NOTIFICATION_ID && notification.packageName == service.packageName
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun buildPlatformMediaNotification(): Notification {
        val metadata = player.currentMediaItem?.mediaMetadata
        val presentation = PlaybackNotificationContract.presentation(
            title = metadata?.title,
            artist = metadata?.artist,
            albumArtist = metadata?.albumArtist,
            albumTitle = metadata?.albumTitle,
            fallbackTitle = service.getString(R.string.app_name),
        )
        val actions = PlaybackNotificationContract.actions(
            isPlaying = player.isPlaying,
            isLiked = (service as? MusicService)?.currentMediaMetadata?.value?.liked == true,
        )

        return Notification.Builder(service, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_omnitune)
            .setContentTitle(presentation.title)
            .setContentText(presentation.artist)
            .setTicker(presentation.title)
            .setLargeIcon(
                metadata?.artworkData?.let { data ->
                    try { android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size) } catch (_: Exception) { null }
                } ?: defaultNotificationArtwork()
            )
            .setContentIntent(
                PendingIntent.getActivity(
                    service,
                    0,
                    Intent(service, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setOngoing(player.isPlaying)
            .apply { actions.forEach { addAction(notificationAction(it)) } }
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(sessionProvider()?.platformToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun defaultNotificationArtwork(): Bitmap {
        val d = androidx.core.content.ContextCompat.getDrawable(service, R.drawable.ic_omnitune_logo)
        if (d != null) {
            val bitmap = Bitmap.createBitmap(d.intrinsicWidth.coerceAtLeast(1), d.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            d.setBounds(0, 0, canvas.width, canvas.height)
            d.draw(canvas)
            return bitmap
        }
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    private fun notificationAction(spec: PlaybackNotificationActionSpec): Notification.Action {
        val intent = Intent(service, MusicService::class.java).setAction(spec.action)
        val pendingIntent = PendingIntent.getService(
            service,
            spec.requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Action.Builder(
            Icon.createWithResource(service, spec.iconRes),
            spec.title,
            pendingIntent,
        ).build()
    }

    companion object {
        const val CHANNEL_ID = "music_player"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "com.omnitune.app.playback.action.PLAY"
        const val ACTION_PAUSE = "com.omnitune.app.playback.action.PAUSE"
        const val ACTION_NEXT = "com.omnitune.app.playback.action.NEXT"
        const val ACTION_PREVIOUS = "com.omnitune.app.playback.action.PREVIOUS"
        const val ACTION_LIKE = "com.omnitune.app.playback.action.LIKE"
        const val ACTION_REPEAT = "com.omnitune.app.playback.action.REPEAT"
        const val ACTION_STOP = "com.omnitune.app.playback.action.STOP"
    }
}
