/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 * Licensed Under GPL-3.0
 */

package com.omnitune.app.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.omnitune.app.MainActivity
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.constants.MediaSessionConstants.CommandToggleLike
import com.omnitune.app.db.entities.LyricsEntity
import com.omnitune.app.lyrics.LyricsHelper
import com.omnitune.app.constants.MediaSessionConstants.CommandToggleLibrary
import com.omnitune.app.constants.MediaSessionConstants.CommandToggleStartRadio
import com.omnitune.app.constants.MediaSessionConstants.CommandToggleShuffle
import com.omnitune.app.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.omnitune.app.extensions.currentMetadata
import com.omnitune.app.extensions.metadata
import com.omnitune.app.extensions.setOffloadEnabled
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.playback.queues.EmptyQueue
import com.omnitune.app.playback.queues.ListQueue
import com.omnitune.app.playback.queues.Queue
import com.omnitune.app.utils.NetworkConnectivityObserver
import com.omnitune.app.utils.reportException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaLibraryService(), Player.Listener {

    @Inject lateinit var sessionCallback: MusicSessionCallback
    @Inject lateinit var database: MusicDatabase
    @Inject lateinit var lyricsHelper: LyricsHelper
    @Inject lateinit var streamExtractor: com.omnitune.app.data.StreamExtractor

    inner class MusicBinder : Binder() {
        val service: MusicService get() = this@MusicService
    }

    companion object {
        const val CHANNEL_ID = "music_player"
        const val NOTIFICATION_ID = 1

        @Volatile
        var instance: MusicService? = null
            private set
    }

    private var mediaSession: MediaLibrarySession? = null
    private var scopeJob = Job()
    var scope = CoroutineScope(Dispatchers.Main + scopeJob)
    private val binder = MusicBinder()


    lateinit var player: ExoPlayer
        private set

    fun binder(): MusicBinder = binder

    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)

    val currentMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
    var queueTitle: String? = null
    val queueRestoreCompleted = MutableStateFlow(false)

    private var currentQueue: Queue = EmptyQueue

    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.tag("MusicService").i("MusicService created")

        createNotificationChannel()
        startForegroundNotification()

        initializePlayer()

        connectivityObserver = NetworkConnectivityObserver(this)

        sessionCallback.onToggleLike = { toggleLike() }
        sessionCallback.onToggleLibrary = { toggleLibrary() }
        sessionCallback.onStartRadio = { toggleStartRadio() }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("OmniTune")
            .setContentText("Music Player")
            .setContentIntent(contentIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            reportException(e)
        }
    }

    private fun initializePlayer() {
        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().apply {
                setMaxVideoSizeSd()
                setPreferredAudioLanguages("en")
            })
        }

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(5_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
            .also { exoPlayer ->
                exoPlayer.setOffloadEnabled(true)
                exoPlayer.playWhenReady = false
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.addListener(this)
            }

        sessionCallback.onPlayerReady(player)

        mediaSession = MediaLibrarySession.Builder(this, player, sessionCallback)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setId("OmniTune")
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(android.R.drawable.ic_media_play)
            }
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    fun playQueue(queue: Queue, playWhenReady: Boolean = true) {
        currentQueue = queue
        queueTitle = null

        if (queue.preloadItem != null) {
            player.setMediaItem(queue.preloadItem!!.toMediaItem())
            player.prepare()
            player.playWhenReady = playWhenReady
        }

        scope.launch {
            val initialStatus = queue.getInitialStatus()
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            if (initialStatus.items.isEmpty()) {
                Timber.w("Queue items empty")
                return@launch
            }

            // Resolve YouTube video IDs to playable stream URLs
            val resolvedItems = withContext(Dispatchers.IO) {
                StreamUrlResolver.resolveMediaItems(
                    initialStatus.items,
                    streamExtractor
                )
            }
            if (resolvedItems.isEmpty()) {
                Timber.e("All stream resolutions failed")
                return@launch
            }

            val resolvedIndex = initialStatus.mediaItemIndex.coerceIn(0, resolvedItems.size - 1)

            if (queue.preloadItem != null) {
                player.addMediaItems(0, resolvedItems.subList(0, resolvedIndex))
                player.addMediaItems(resolvedItems.subList(resolvedIndex + 1, resolvedItems.size))
            } else {
                player.setMediaItems(resolvedItems, resolvedIndex, initialStatus.position)
                player.prepare()
                player.playWhenReady = playWhenReady
            }
        }
    }

    fun startRadioSeamlessly() {
        val currentMeta = player.currentMetadata ?: return
        val currentIndex = player.currentMediaItemIndex
        val currentMediaId = currentMeta.id

        scope.launch {
            val radioQueue = com.omnitune.app.playback.queues.YouTubeQueue(
                endpoint = com.omnitune.app.innertube.models.WatchEndpoint(videoId = currentMediaId)
            )
            val initialStatus = radioQueue.getInitialStatus()

            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }

            val radioItems = initialStatus.items.filter { item ->
                item.mediaId != currentMediaId
            }

            if (radioItems.isNotEmpty()) {
                val itemCount = player.mediaItemCount
                if (itemCount > currentIndex + 1) {
                    player.removeMediaItems(currentIndex + 1, itemCount)
                }
                player.addMediaItems(currentIndex + 1, radioItems)
            }

            currentQueue = radioQueue
        }
    }

    fun playNext(items: List<MediaItem>) {
        player.addMediaItems(
            if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1,
            items
        )
        player.prepare()
    }

    fun addToQueue(items: List<MediaItem>) {
        player.addMediaItems(items)
        player.prepare()
    }

    fun toggleLike() {
        Timber.tag("MusicService").i("Toggle like")
        val meta = currentMediaMetadata.value ?: return
        scope.launch(Dispatchers.IO) {
            val song = database.getSongById(meta.id)
            if (song != null) {
                database.upsert(song.song.localToggleLike())
            } else {
                database.upsert(meta.toSongEntity().copy(liked = !meta.liked, likedDate = if (!meta.liked) java.time.LocalDateTime.now() else null))
            }
            com.omnitune.app.innertube.YouTube.likeVideo(meta.id, !meta.liked)
        }
        currentMediaMetadata.value = meta.copy(liked = !meta.liked, likedDate = if (!meta.liked) java.time.LocalDateTime.now() else null)
    }

    fun toggleLibrary() {
        Timber.tag("MusicService").i("Toggle library")
        val meta = currentMediaMetadata.value ?: return
        scope.launch(Dispatchers.IO) {
            val song = database.getSongById(meta.id)
            val inLibrary = song?.song?.inLibrary
            val updated = if (song != null) {
                song.song.copy(inLibrary = if (inLibrary == null) java.time.LocalDateTime.now() else null)
            } else {
                meta.toSongEntity().copy(inLibrary = java.time.LocalDateTime.now())
            }
            database.upsert(updated)
        }
        val newInLibrary = if (meta.inLibrary == null) java.time.LocalDateTime.now() else null
        currentMediaMetadata.value = meta.copy(inLibrary = newInLibrary, liked = if (newInLibrary != null) meta.liked else false)
    }

    fun toggleStartRadio() {
        if (player.currentMediaItem != null) {
            startRadioSeamlessly()
        }
    }

    fun stopAndClearPlayback() {
        currentQueue = EmptyQueue
        queueTitle = null
        waitingForNetworkConnection.value = false
        currentMediaMetadata.value = null
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
    }

    override fun onDestroy() {
        Timber.tag("MusicService").i("MusicService destroyed")
        instance = null
        mediaSession?.run {
            sessionCallback.onDestroy()
            release()
            mediaSession = null
        }
        scopeJob.cancel()
        player.release()
        super.onDestroy()
    }

    // --- Player.Listener ---

    override fun onPlaybackStateChanged(state: Int) {
        Timber.tag("MusicService").v("Playback state: $state")
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val meta = mediaItem?.metadata ?: currentMediaMetadata.value
        currentMediaMetadata.value = meta
        updateNotification()

        // Fetch lyrics for the current song in the background
        meta?.let { metadata ->
            scope.launch(Dispatchers.IO) {
                try {
                    val existing = database.lyrics(metadata.id).first()
                    if (existing != null && existing.lyrics != LyricsEntity.LYRICS_NOT_FOUND) {
                        Timber.tag("MusicService").d("Lyrics already cached for: ${metadata.title}")
                        return@launch
                    }

                    val lyrics = lyricsHelper.getLyrics(metadata)
                    if (lyrics != LyricsEntity.LYRICS_NOT_FOUND && lyrics.isNotBlank()) {
                        database.upsert(LyricsEntity(id = metadata.id, lyrics = lyrics))
                        Timber.tag("MusicService").d("Fetched and cached lyrics for: ${metadata.title}")
                    } else {
                        // Mark as not found so we don't keep retrying
                        database.upsert(LyricsEntity(id = metadata.id, lyrics = LyricsEntity.LYRICS_NOT_FOUND))
                    }
                } catch (e: Exception) {
                    Timber.tag("MusicService").w(e, "Failed to fetch lyrics for: ${metadata.title}")
                    reportException(e)
                }
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        Timber.tag("MusicService").e(error, "Player error")
        reportException(error)
    }

    @Suppress("DEPRECATION")
    private fun updateNotification() {
        try {
            val customLayout = listOf(
                CommandButton.Builder()
                    .setDisplayName("Like")
                    .setIconResId(android.R.drawable.ic_input_add)
                    .setSessionCommand(CommandToggleLike)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName("Repeat")
                    .setIconResId(android.R.drawable.ic_menu_recent_history)
                    .setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName("Shuffle")
                    .setIconResId(android.R.drawable.ic_menu_sort_by_size)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName("Radio")
                    .setIconResId(android.R.drawable.ic_menu_share)
                    .setSessionCommand(CommandToggleStartRadio)
                    .build(),
            )
            mediaSession?.setCustomLayout(customLayout)
        } catch (e: Exception) {
            reportException(e)
        }
    }


}
