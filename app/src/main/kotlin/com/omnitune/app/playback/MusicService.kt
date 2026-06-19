/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import com.omnitune.app.R
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
import com.omnitune.app.utils.dataStore
import com.omnitune.app.constants.SkipSilenceKey
import com.omnitune.app.constants.AudioOffload
import com.omnitune.app.constants.PlayerVolumeKey
import com.omnitune.app.constants.RepeatModeKey
import com.omnitune.app.constants.AutoSkipNextOnErrorKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.omnitune.app.constants.AudioCrossfadeDurationKey
import com.omnitune.app.constants.AudioNormalizationKey
import com.omnitune.app.constants.ScrobbleDelayPercentKey
import com.omnitune.app.constants.ScrobbleDelaySecondsKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import com.omnitune.app.playback.EqualizerBand

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

    // OMNITUNE: Crossfade state
    var playerVolume = MutableStateFlow(1f)
    private val audioFocusVolumeFactor = MutableStateFlow(1f)
    private val playbackFadeFactor = MutableStateFlow(1f)
    private val crossfadeDurationMs = MutableStateFlow(0)
    private val audioNormalizationEnabled = MutableStateFlow(true)
    private var crossfadeAudio: CrossfadeAudio? = null

    // OMNITUNE: Sleep timer
    lateinit var sleepTimer: SleepTimer
        private set

    fun binder(): MusicBinder = binder

    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)

    // OMNITUNE: Playback tracking
    private var lastRecordedMediaId: String? = null
    private var playbackTrackerJob: Job? = null

    val currentMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
    var queueTitle: String? = null
    val queueRestoreCompleted = MutableStateFlow(false)

    private var currentQueue: Queue = EmptyQueue

    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.tag("MusicService").i("MusicService created")

        createNotificationChannel()

        initializePlayer()
        sleepTimer = SleepTimer(player, scope)
        observePreferences()

        // Mark queue restore as done (no persistent queue implementation yet)
        queueRestoreCompleted.value = true

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

        crossfadeAudio = CrossfadeAudio(
            player = player,
            database = database,
            crossfadeDurationMs = crossfadeDurationMs,
            playbackFadeFactor = playbackFadeFactor,
            playerVolume = playerVolume,
            audioFocusVolumeFactor = audioFocusVolumeFactor,
            audioNormalizationEnabled = audioNormalizationEnabled,
            maxSafeGainFactor = 3.16f,
            overlapPlayerFactory = {
                ExoPlayer.Builder(this)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(this))
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_MEDIA)
                            .build(),
                        true
                    )
                    .setHandleAudioBecomingNoisy(false)
                    .build()
            }
        )
        crossfadeAudio?.start(scope)

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
                setSmallIcon(R.drawable.ic_launcher_foreground)
            }
        )
    }

    // OMNITUNE: System equalizer integration
    private var systemEqualizer: android.media.audiofx.Equalizer? = null

    private fun setupEqualizer() {
        try {
            systemEqualizer?.release()
            systemEqualizer = android.media.audiofx.Equalizer(0, player.audioSessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to initialize equalizer")
        }
    }

    fun applyEqualizerBands(bands: List<com.omnitune.app.playback.EqualizerBand>) {
        val eq = systemEqualizer ?: return
        try {
            bands.forEachIndexed { index, band ->
                if (index < eq.numberOfBands) {
                    val gainMillibels = (band.gainDb * 100).toInt().toShort()
                    eq.setBandLevel(index.toShort(), gainMillibels)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to apply EQ bands")
        }
    }

    private var autoSkipNextOnError = true

    /**
     * Observes user preferences from DataStore and applies them to the player in real-time.
     * This ensures Settings toggles actually affect playback behavior.
     */
    private fun observePreferences() {
        val ds = applicationContext.dataStore

        // Skip Silence
        scope.launch {
            ds.data.map { it[SkipSilenceKey] ?: false }.distinctUntilChanged().collect { skipSilence ->
                player.skipSilenceEnabled = skipSilence
                Timber.tag("MusicService").d("Skip silence: $skipSilence")
            }
        }

        // Audio Offload
        scope.launch {
            ds.data.map { it[AudioOffload] ?: true }.distinctUntilChanged().collect { offload ->
                player.setOffloadEnabled(offload)
                Timber.tag("MusicService").d("Audio offload: $offload")
            }
        }

        // Player Volume
        scope.launch {
            ds.data.map { it[PlayerVolumeKey] ?: 1f }.distinctUntilChanged().collect { volume ->
                playerVolume.value = volume.coerceIn(0f, 1f)
            }
        }

        // Combine volumes for crossfade
        scope.launch {
            combine(playerVolume, playbackFadeFactor) { vol, fade ->
                (vol * fade).coerceIn(0f, 1f)
            }.collectLatest { finalVolume ->
                player.volume = finalVolume
            }
        }

        // Repeat Mode
        scope.launch {
            ds.data.map { it[RepeatModeKey] ?: Player.REPEAT_MODE_OFF }.distinctUntilChanged().collect { mode ->
                player.repeatMode = mode
                Timber.tag("MusicService").d("Repeat mode: $mode")
            }
        }

        // Crossfade
        scope.launch {
            ds.data.map { (it[AudioCrossfadeDurationKey] ?: 0) * 1000 }.distinctUntilChanged().collectLatest { durationMs ->
                crossfadeDurationMs.value = durationMs
            }
        }

        // Audio Normalization
        scope.launch {
            ds.data.map { it[AudioNormalizationKey] ?: true }.distinctUntilChanged().collect { enabled ->
                audioNormalizationEnabled.value = enabled
            }
        }

        // Auto skip on error
        scope.launch {
            ds.data.map { it[AutoSkipNextOnErrorKey] ?: true }.distinctUntilChanged().collect { autoSkip ->
                autoSkipNextOnError = autoSkip
            }
        }
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
                endpoint = com.omnitune.innertube.models.WatchEndpoint(videoId = currentMediaId)
            )
            val initialStatus = radioQueue.getInitialStatus()

            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }

            val radioItems = initialStatus.items.filter { item ->
                item.mediaId != currentMediaId
            }

            if (radioItems.isNotEmpty()) {
                // Resolve YouTube video IDs to playable stream URLs before adding to player
                val resolvedRadioItems = withContext(Dispatchers.IO) {
                    StreamUrlResolver.resolveMediaItems(radioItems, streamExtractor)
                }
                if (resolvedRadioItems.isNotEmpty()) {
                    val itemCount = player.mediaItemCount
                    if (itemCount > currentIndex + 1) {
                        player.removeMediaItems(currentIndex + 1, itemCount)
                    }
                    player.addMediaItems(currentIndex + 1, resolvedRadioItems)
                    Timber.tag("MusicService").i("Radio: added ${resolvedRadioItems.size} resolved tracks")
                } else {
                    Timber.tag("MusicService").w("Radio: all stream resolutions failed")
                }
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
            try {
                com.omnitune.innertube.YouTube.likeVideo(meta.id, !meta.liked)
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync like with YouTube")
            }
            withContext(Dispatchers.Main) {
                currentMediaMetadata.value = meta.copy(liked = !meta.liked, likedDate = if (!meta.liked) java.time.LocalDateTime.now() else null)
            }
        }
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
            withContext(Dispatchers.Main) {
                val newInLibrary = if (meta.inLibrary == null) java.time.LocalDateTime.now() else null
                currentMediaMetadata.value = meta.copy(inLibrary = newInLibrary, liked = if (newInLibrary != null) meta.liked else false)
            }
        }
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
        try {
            crossfadeAudio?.release()
            crossfadeAudio = null
        } catch (_: Exception) {}
        systemEqualizer?.release()
        systemEqualizer = null
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
        crossfadeAudio?.onPlaybackStateChanged(state)
        if (state == Player.STATE_READY && systemEqualizer == null) {
            setupEqualizer()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        crossfadeAudio?.onMediaItemTransition(mediaItem, reason)
        val meta = mediaItem?.metadata ?: currentMediaMetadata.value
        currentMediaMetadata.value = meta
        updateNotification()
        startPlaybackTracker(mediaItem)

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

        if (autoSkipNextOnError && player.hasNextMediaItem()) {
            Timber.tag("MusicService").i("Auto-skipping to next track after error")
            player.seekToNextMediaItem()
            player.prepare()
        }
    }

    @Suppress("DEPRECATION")
    private fun updateNotification() {
        try {
            val customLayout = listOf(
                CommandButton.Builder(CommandButton.ICON_PLAY)
                    .setSessionCommand(CommandToggleLike)
                    .setDisplayName("Like")
                    .setIconResId(android.R.drawable.ic_input_add)
                    .build(),
                CommandButton.Builder(CommandButton.ICON_PLAY)
                    .setSessionCommand(CommandToggleRepeatMode)
                    .setDisplayName("Repeat")
                    .setIconResId(android.R.drawable.ic_menu_recent_history)
                    .build(),
                CommandButton.Builder(CommandButton.ICON_PLAY)
                    .setSessionCommand(CommandToggleShuffle)
                    .setDisplayName("Shuffle")
                    .setIconResId(android.R.drawable.ic_menu_sort_by_size)
                    .build(),
                CommandButton.Builder(CommandButton.ICON_PLAY)
                    .setSessionCommand(CommandToggleStartRadio)
                    .setDisplayName("Radio")
                    .setIconResId(android.R.drawable.ic_menu_share)
                    .build(),
            )
            mediaSession?.setCustomLayout(customLayout)
        } catch (e: Exception) {
            reportException(e)
        }
    }
    private fun startPlaybackTracker(mediaItem: MediaItem?) {
        playbackTrackerJob?.cancel()
        val mediaId = mediaItem?.mediaId ?: return
        if (mediaId == lastRecordedMediaId) return

        playbackTrackerJob = scope.launch(Dispatchers.IO) {
            var durationMs = player.duration
            while (durationMs == C.TIME_UNSET || durationMs <= 0L) {
                delay(1000)
                if (!isActive) return@launch
                // Safe way to get duration on main thread
                durationMs = withContext(Dispatchers.Main) { player.duration }
            }

            if (durationMs < 30_000L) return@launch // Skip very short tracks

            val ds = applicationContext.dataStore
            val delayPercent = ds.data.map { it[ScrobbleDelayPercentKey] ?: 50f }.first()
            val delaySeconds = ds.data.map { it[ScrobbleDelaySecondsKey] ?: 30 }.first()
            
            // Threshold is the minimum of (delayPercent %) or (delaySeconds), clamped to 10s min
            val thresholdMs = minOf((durationMs * delayPercent / 100f).toLong(), delaySeconds * 1000L).coerceAtLeast(10_000L)

            while (isActive) {
                val (currentPos, currentId) = withContext(Dispatchers.Main) { 
                    Pair(player.currentPosition, player.currentMediaItem?.mediaId) 
                }
                
                if (currentId == mediaId) {
                    if (currentPos >= thresholdMs) {
                        Timber.tag("MusicService").d("Recording play count for $mediaId")
                        database.incrementPlayCount(mediaId)
                        lastRecordedMediaId = mediaId
                        break
                    }
                } else {
                    break
                }
                delay(1000)
            }
        }
    }


}
