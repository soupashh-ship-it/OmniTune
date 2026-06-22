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
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
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
import kotlinx.coroutines.flow.asStateFlow
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
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject
import com.omnitune.app.playback.EqualizerBand

@AndroidEntryPoint
class MusicService : MediaLibraryService(), Player.Listener {

    @Inject lateinit var sessionCallback: MusicSessionCallback
    @Inject lateinit var database: MusicDatabase
    @Inject lateinit var lyricsHelper: LyricsHelper
    @Inject lateinit var streamExtractor: com.omnitune.app.data.StreamExtractor
    @Inject lateinit var downloadUtil: DownloadUtil

    inner class MusicBinder : Binder() {
        val service: MusicService get() = this@MusicService
    }

    companion object {
        const val CHANNEL_ID = "music_player"
        const val NOTIFICATION_ID = 1
    }

    private var mediaSession: MediaLibrarySession? = null
    private var scopeJob = kotlinx.coroutines.SupervisorJob()
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, exception ->
        timber.log.Timber.tag("MusicService").e(exception, "Uncaught exception in MusicService scope")
    }
    var scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + scopeJob + exceptionHandler)
    private val binder = MusicBinder()
    private val playbackRecoveryPolicy = com.omnitune.app.playback.recovery.PlaybackRecoveryPolicy()


    val exoPlayer: ExoPlayer get() = player
    internal lateinit var player: ExoPlayer
    private val _playerVolume = MutableStateFlow(1f)
    val playerVolume = _playerVolume.asStateFlow()
    private val audioFocusVolumeFactor = MutableStateFlow(1f)
    private val playbackFadeFactor = MutableStateFlow(1f)
    private val crossfadeDurationMs = MutableStateFlow(0)
    private val audioNormalizationEnabled = MutableStateFlow(true)
    private var crossfadeAudio: CrossfadeAudio? = null

    // OMNITUNE: Sleep timer
    lateinit var sleepTimer: SleepTimer

    fun binder(): MusicBinder = binder

    lateinit var connectivityObserver: NetworkConnectivityObserver
    private val _waitingForNetworkConnection = MutableStateFlow(false)
    val waitingForNetworkConnection = _waitingForNetworkConnection.asStateFlow()

    // OMNITUNE: Playback tracking
    private var lastRecordedMediaId: String? = null
    private var playbackTrackerJob: Job? = null

    private val _currentMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
    val currentMediaMetadata = _currentMediaMetadata.asStateFlow()
    var queueTitle: String? = null
        private set
    private val _queueRestoreCompleted = MutableStateFlow(false)
    val queueRestoreCompleted = _queueRestoreCompleted.asStateFlow()
    private var saveQueueJob: Job? = null

    private var currentQueue: Queue = EmptyQueue

    override fun onBind(intent: Intent?): IBinder? {
        return if (intent?.action == null) {
            Timber.tag("OmniTunePlaybackTrace").i("Returning local MusicBinder")
            binder
        } else {
            super.onBind(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.tag("MusicService").i("MusicService created")

        createNotificationChannel()

        initializePlayer()
        sleepTimer = SleepTimer(player, scope)
        observePreferences()
        StreamUrlResolver.clearMemoryCache("service startup")

        // Restore persistent queue
        scope.launch(Dispatchers.IO) {
            try {
                val savedQueue = database.getQueue()
                if (savedQueue != null && savedQueue.mediaIdList.isNotBlank()) {
                    val mediaIds = savedQueue.mediaIdList.split(",")
                    val songs = database.getSongsByIds(mediaIds)
                    // Ensure the order is preserved
                    val mediaItems = mediaIds.mapNotNull { id -> songs.find { it.id == id }?.toMediaItem() }
                    
                    if (mediaItems.isNotEmpty()) {
                        val queue = ListQueue(
                            title = savedQueue.title,
                            items = mediaItems,
                            startIndex = savedQueue.startIndex.coerceIn(0, mediaItems.size - 1),
                            position = savedQueue.position
                        )
                        withContext(Dispatchers.Main) {
                            restoreQueueMetadataOnly(queue)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag("MusicService").e(e, "Failed to restore persistent queue")
            } finally {
                _queueRestoreCompleted.value = true
            }
        }

        connectivityObserver = NetworkConnectivityObserver(this)

        sessionCallback.onToggleLike = { toggleLike() }
        sessionCallback.onToggleLibrary = { toggleLibrary() }
        sessionCallback.onStartRadio = { toggleStartRadio() }
    }

    private suspend fun restoreQueueMetadataOnly(queue: Queue) {
        val initialStatus = queue.getInitialStatus()
        if (initialStatus.items.isEmpty()) {
            Timber.tag("OmniTunePlaybackTrace").w("Restore skipped: saved queue is empty")
            return
        }

        val restoredIndex = initialStatus.mediaItemIndex.coerceIn(0, initialStatus.items.size - 1)
        currentQueue = queue
        queueTitle = initialStatus.title
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        player.setMediaItems(
            initialStatus.items.map { it.withOriginalVideoIdUri() },
            restoredIndex,
            initialStatus.position.coerceAtLeast(0L)
        )
        _currentMediaMetadata.value = player.currentMediaItem?.metadata
        updateNotification()
        Timber.tag("OmniTunePlaybackTrace").i(
            "Restored queue metadata only: items=${initialStatus.items.size}, index=$restoredIndex, current=${player.currentMediaItem?.mediaId}"
        )
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

        val dataSourceFactory = DefaultDataSource.Factory(this, DefaultHttpDataSource.Factory())
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(downloadUtil.downloadCache)
            .setUpstreamDataSourceFactory(dataSourceFactory)
            .setCacheWriteDataSinkFactory(null)

        player = ExoPlayer.Builder(this)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(cacheDataSourceFactory))
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
            playerVolume = _playerVolume,
            audioFocusVolumeFactor = audioFocusVolumeFactor,
            audioNormalizationEnabled = audioNormalizationEnabled,
            maxSafeGainFactor = 3.16f,
            overlapPlayerFactory = {
                val overlapDataSourceFactory = DefaultDataSource.Factory(this, DefaultHttpDataSource.Factory())
                val overlapCacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(downloadUtil.downloadCache)
                    .setUpstreamDataSourceFactory(overlapDataSourceFactory)
                    .setCacheWriteDataSinkFactory(null)
                    
                ExoPlayer.Builder(this)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(overlapCacheDataSourceFactory))
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_MEDIA)
                            .build(),
                        true
                    )
                    .setHandleAudioBecomingNoisy(true)
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
                setPlayerVolume(volume.coerceIn(0f, 1f))
            }
        }

        // Combine volumes for crossfade
        scope.launch {
            combine(_playerVolume, playbackFadeFactor) { vol, fade ->
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

    private fun setPlayerVolume(volume: Float) {
        _playerVolume.value = volume
        player.volume = volume
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
        Timber.tag("OmniTunePlaybackTrace").i("playQueue requested: playWhenReady=$playWhenReady")

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
            Timber.tag("OmniTunePlaybackTrace").i(
                "playQueue initial status: items=${initialStatus.items.size}, index=${initialStatus.mediaItemIndex}, first=${initialStatus.items.firstOrNull()?.mediaId}"
            )

            val requestedIndex = initialStatus.mediaItemIndex.coerceIn(0, initialStatus.items.size - 1)
            val currentItem = initialStatus.items[requestedIndex]
            val resolvedCurrent = withContext(Dispatchers.IO) {
                if (StreamUrlResolver.isYouTubeVideoId(currentItem.localConfiguration?.uri)) {
                    StreamUrlResolver.resolveMediaItem(currentItem, streamExtractor, downloadUtil)
                } else {
                    currentItem
                }
            }
            if (resolvedCurrent == null) {
                Timber.e("Current stream resolution failed for ${currentItem.mediaId}")
                Toast.makeText(this@MusicService, "Could not resolve stream for ${currentItem.mediaMetadata.title ?: "track"}", Toast.LENGTH_SHORT).show()
                return@launch
            }
            Timber.tag("OmniTunePlaybackTrace").i("Current item resolved: ${currentItem.mediaId}")

            val resolvedItems = withContext(Dispatchers.IO) {
                StreamUrlResolver.resolveMediaItems(
                    initialStatus.items,
                    streamExtractor,
                    downloadUtil,
                    priorityIndex = requestedIndex
                )
            }
            val resolvedIndex = requestedIndex

            if (queue.preloadItem != null) {
                player.addMediaItems(0, resolvedItems.subList(0, resolvedIndex))
                player.addMediaItems(resolvedItems.subList(resolvedIndex + 1, resolvedItems.size))
            } else {
                Timber.tag("OmniTunePlaybackTrace").i(
                    "Player setMediaItems: count=${resolvedItems.size}, index=$resolvedIndex, position=${initialStatus.position}"
                )
                player.setMediaItems(resolvedItems, resolvedIndex, initialStatus.position)
                player.prepare()
                player.playWhenReady = playWhenReady
                Timber.tag("OmniTunePlaybackTrace").i(
                    "Player prepared: count=${player.mediaItemCount}, current=${player.currentMediaItem?.mediaId}, state=${player.playbackState}, playWhenReady=${player.playWhenReady}"
                )
            }
        }
    }

    fun playOrResolveCurrent() {
        val currentItem = player.currentMediaItem
        if (currentItem == null) {
            Timber.tag("OmniTunePlaybackTrace").w("Play requested with no current media item")
            return
        }

        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0, 0)
            player.playWhenReady = true
            return
        }

        if (currentItem.needsFreshResolution() || player.playbackState == Player.STATE_IDLE) {
            val items = (0 until player.mediaItemCount).map { index ->
                player.getMediaItemAt(index).withOriginalVideoIdUri()
            }
            val index = player.currentMediaItemIndex.coerceAtLeast(0)
            val position = player.currentPosition.coerceAtLeast(0L)
            Timber.tag("OmniTunePlaybackTrace").i(
                "Play requested for recoverable/restored item: current=${currentItem.mediaId}, index=$index, state=${player.playbackState}"
            )
            playQueue(
                ListQueue(
                    title = queueTitle,
                    items = items,
                    startIndex = index.coerceIn(0, items.lastIndex.coerceAtLeast(0)),
                    position = position
                ),
                playWhenReady = true
            )
            return
        }

        Timber.tag("OmniTunePlaybackTrace").i("Player play called: current=${currentItem.mediaId}, state=${player.playbackState}")
        player.play()
    }

    fun pausePlayback() {
        player.pause()
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
                    StreamUrlResolver.resolveMediaItems(radioItems, streamExtractor, downloadUtil)
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
        scope.launch {
            val resolvedItems = withContext(Dispatchers.IO) {
                StreamUrlResolver.resolveMediaItems(items, streamExtractor, downloadUtil)
            }
            player.addMediaItems(
                if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1,
                resolvedItems
            )
            player.prepare()
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        scope.launch {
            val resolvedItems = withContext(Dispatchers.IO) {
                StreamUrlResolver.resolveMediaItems(items, streamExtractor, downloadUtil)
            }
            player.addMediaItems(resolvedItems)
            player.prepare()
        }
    }

    fun toggleLike() {
        Timber.tag("MusicService").i("Toggle like")
        val meta = _currentMediaMetadata.value ?: return
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
                _currentMediaMetadata.value = meta.copy(liked = !meta.liked, likedDate = if (!meta.liked) java.time.LocalDateTime.now() else null)
            }
        }
    }

    fun toggleLibrary() {
        Timber.tag("MusicService").i("Toggle library")
        val meta = _currentMediaMetadata.value ?: return
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
                _currentMediaMetadata.value = meta.copy(inLibrary = newInLibrary, liked = if (newInLibrary != null) meta.liked else false)
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
        _waitingForNetworkConnection.value = false
        _currentMediaMetadata.value = null
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
        if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
            saveQueueState()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        playbackRecoveryPolicy.resetRetry(mediaItem?.mediaId ?: "")
        crossfadeAudio?.onMediaItemTransition(mediaItem, reason)
        val meta = mediaItem?.metadata ?: _currentMediaMetadata.value
        _currentMediaMetadata.value = meta
        updateNotification()
        startPlaybackTracker(mediaItem)
        saveQueueState()

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

        val errorType = com.omnitune.app.playback.recovery.PlaybackErrorClassifier.classify(error)
        val currentMediaItem = player.currentMediaItem
        val mediaId = currentMediaItem?.mediaId
        
        if (mediaId != null && playbackRecoveryPolicy.canRetry(mediaId, errorType)) {
            Timber.tag("MusicService").w("Recovering from $errorType for mediaId: $mediaId")
            playbackRecoveryPolicy.incrementRetry(mediaId)
            
            // Invalidate caches
            StreamUrlResolver.invalidate(mediaId)
            streamExtractor.invalidate(mediaId)

            scope.launch(Dispatchers.Main) {
                try {
                    // Start from the original item, not the resolved one, so the resolver sees the yt ID
                    val originalItem = currentMediaItem.buildUpon().setUri(mediaId).build()
                    val resolved = withContext(Dispatchers.IO) {
                        StreamUrlResolver.resolveMediaItem(originalItem, streamExtractor, downloadUtil)
                    }
                    if (resolved != null) {
                        val pos = player.currentPosition
                        val index = player.currentMediaItemIndex
                        player.replaceMediaItem(index, resolved)
                        player.seekTo(index, pos)
                        player.prepare()
                        player.play()
                        return@launch
                    }
                } catch (e: Exception) {
                    Timber.tag("MusicService").e(e, "Failed to resolve during recovery")
                }
                fallbackSkip()
            }
        } else {
            Timber.tag("MusicService").w("Recovery policy denied retry for $mediaId, error: $errorType")
            fallbackSkip()
        }
    }

    private fun fallbackSkip() {
        if (autoSkipNextOnError && player.hasNextMediaItem()) {
            Timber.tag("MusicService").i("Auto-skipping to next track after error")
            player.seekToNextMediaItem()
            player.prepare()
            player.play()
        }
    }

    @Suppress("DEPRECATION")
    private fun updateNotification() {
        try {
            val customLayout = listOf(
                CommandButton.Builder(CommandButton.ICON_PLAY)
                    .setSessionCommand(CommandToggleLike)
                    .setDisplayName("Like")
                    .setIconResId(com.omnitune.app.R.drawable.ic_add)
                    .build(),
                CommandButton.Builder(CommandButton.ICON_PLAY)
                    .setSessionCommand(CommandToggleRepeatMode)
                    .setDisplayName("Repeat")
                    .setIconResId(com.omnitune.app.R.drawable.ic_history)
                    .build(),
                CommandButton.Builder(CommandButton.ICON_PLAY)
                    .setSessionCommand(CommandToggleShuffle)
                    .setDisplayName("Shuffle")
                    .setIconResId(com.omnitune.app.R.drawable.ic_sort)
                    .build(),
                CommandButton.Builder(CommandButton.ICON_PLAY)
                    .setSessionCommand(CommandToggleStartRadio)
                    .setDisplayName("Radio")
                    .setIconResId(com.omnitune.app.R.drawable.ic_share)
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
            var durationMs = withContext(Dispatchers.Main) { player.duration }
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

    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
        saveQueueState()
    }

    private fun saveQueueState() {
        saveQueueJob?.cancel()
        saveQueueJob = scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                kotlinx.coroutines.delay(1000) // Debounce
                
                val (count, currentIndex, currentPos) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { 
                    Triple(player.mediaItemCount, player.currentMediaItemIndex, player.currentPosition) 
                }
                
                if (count == 0) {
                    database.clearQueue()
                    return@launch
                }
                
                val mediaIds = mutableListOf<String>()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    for (i in 0 until count) {
                        mediaIds.add(player.getMediaItemAt(i).mediaId)
                    }
                }
                
                val entity = com.omnitune.app.db.entities.QueueEntity(
                    id = 1,
                    title = queueTitle,
                    mediaIdList = mediaIds.joinToString(","),
                    startIndex = currentIndex,
                    position = currentPos.coerceAtLeast(0L)
                )
                database.saveQueue(entity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                timber.log.Timber.tag("MusicService").e(e, "Error saving queue state")
            }
        }
    }

    private fun MediaItem.withOriginalVideoIdUri(): MediaItem {
        return if (StreamUrlResolver.isYouTubeVideoId(Uri.parse(mediaId))) {
            buildUpon()
                .setUri(mediaId)
                .setCustomCacheKey(mediaId)
                .build()
        } else {
            this
        }
    }

    private fun MediaItem.needsFreshResolution(): Boolean {
        return StreamUrlResolver.isYouTubeVideoId(localConfiguration?.uri) ||
            localConfiguration?.uri?.toString()?.startsWith("file:///offline/") == true
    }

}
