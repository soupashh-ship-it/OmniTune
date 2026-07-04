/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.lyrics.LyricsHelper
import com.omnitune.app.extensions.metadata
import com.omnitune.app.extensions.setOffloadEnabled
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.playback.queues.EmptyQueue
import com.omnitune.app.playback.queues.ListQueue
import com.omnitune.app.playback.queues.Queue
import com.omnitune.app.utils.NetworkConnectivityObserver
import com.omnitune.app.utils.isInternetAvailable
import com.omnitune.app.utils.reportException
import com.omnitune.app.utils.dataStore
import com.omnitune.app.constants.RepeatModeKey
import com.omnitune.app.constants.ShuffleEnabledKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.omnitune.app.constants.ScrobbleDelayPercentKey
import com.omnitune.app.constants.ScrobbleDelaySecondsKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject

import com.omnitune.app.constants.DiscordTokenKey
import com.omnitune.app.constants.EnableDiscordRPCKey
import com.omnitune.app.discord.DiscordPresenceManager
import com.omnitune.app.discord.SongPresenceData
import com.omnitune.app.discord.getPresenceIntervalMillis
import com.omnitune.app.utils.dataStore
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MusicService : MediaLibraryService(), Player.Listener {

    @Inject lateinit var sessionCallback: MusicSessionCallback
    @Inject lateinit var database: MusicDatabase
    @Inject lateinit var lyricsHelper: LyricsHelper
    @Inject lateinit var streamExtractor: com.omnitune.app.data.StreamExtractor
    @Inject lateinit var downloadUtil: DownloadUtil
    @Inject lateinit var okHttpClient: okhttp3.OkHttpClient
    @Inject lateinit var discordPresenceManager: DiscordPresenceManager

    private lateinit var networkPlaybackMonitor: NetworkPlaybackMonitor
    private lateinit var playbackRecoveryCoordinator: PlaybackRecoveryCoordinator

    private suspend fun getPlaybackQualityMode(): com.omnitune.app.models.PlaybackQualityMode {
        return try {
            val prefs = this.dataStore.data.first()
            val modeName = prefs[com.omnitune.app.constants.PlaybackQualityModeKey]
            if (modeName != null) {
                com.omnitune.app.models.PlaybackQualityMode.valueOf(modeName)
            } else {
                com.omnitune.app.models.PlaybackQualityMode.AUTO
            }
        } catch (e: Exception) {
            com.omnitune.app.models.PlaybackQualityMode.AUTO
        }
    }

    inner class MusicBinder : Binder() {
        val service: MusicService get() = this@MusicService
    }

    companion object {
        const val CHANNEL_ID = PlaybackNotificationManager.CHANNEL_ID
        const val NOTIFICATION_ID = PlaybackNotificationManager.NOTIFICATION_ID
        private const val ACTION_PLAY = PlaybackNotificationManager.ACTION_PLAY
        private const val ACTION_PAUSE = PlaybackNotificationManager.ACTION_PAUSE
        private const val ACTION_NEXT = PlaybackNotificationManager.ACTION_NEXT
        private const val ACTION_PREVIOUS = PlaybackNotificationManager.ACTION_PREVIOUS
    }

    private var sessionManager: SessionManager? = null
    private val mediaSession: MediaLibrarySession?
        get() = sessionManager?.session
    private lateinit var playbackNotificationManager: PlaybackNotificationManager
    private lateinit var lyricsPrefetcher: LyricsPrefetcher
    private var scopeJob = kotlinx.coroutines.SupervisorJob()
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, exception ->
        timber.log.Timber.tag("MusicService").e(exception, "Uncaught exception in MusicService scope")
    }
    var scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + scopeJob + exceptionHandler)
    private val binder = MusicBinder()
    val exoPlayer: ExoPlayer get() = player
    internal lateinit var player: ExoPlayer
    private val _playerVolume = MutableStateFlow(1f)
    val playerVolume = _playerVolume.asStateFlow()
    private val audioFocusVolumeFactor = MutableStateFlow(1f)
    private val playbackFadeFactor = MutableStateFlow(1f)
    private val crossfadeDurationMs = MutableStateFlow(0)
    private val audioNormalizationEnabled = MutableStateFlow(true)
    private var crossfadePlaybackCoordinator: CrossfadePlaybackCoordinator? = null
    private var playbackPreferenceObserver: PlaybackPreferenceObserver? = null
    private var radioQueueManager: RadioQueueManager? = null
    private val equalizerController by lazy { EqualizerController(this) }

    // OMNITUNE: Sleep timer
    lateinit var sleepTimer: SleepTimer

    fun binder(): MusicBinder = binder

    lateinit var connectivityObserver: NetworkConnectivityObserver
    private val _waitingForNetworkConnection = MutableStateFlow(false)
    val waitingForNetworkConnection = _waitingForNetworkConnection.asStateFlow()

    // OMNITUNE: Playback tracking
    private var lastRecordedMediaId: String? = null
    private var lastRecordedRecentMediaId: String? = null
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                playOrResolveCurrent()
                postMediaNotificationFallback("action-play", force = true)
                return START_STICKY
            }
            ACTION_PAUSE -> {
                pausePlayback()
                postMediaNotificationFallback("action-pause", force = true)
                return START_STICKY
            }
            ACTION_NEXT -> {
                if (::player.isInitialized && player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.play()
                }
                postMediaNotificationFallback("action-next", force = true)
                return START_STICKY
            }
            ACTION_PREVIOUS -> {
                if (::player.isInitialized) {
                    player.seekToPreviousMediaItem()
                    player.play()
                }
                postMediaNotificationFallback("action-previous", force = true)
                return START_STICKY
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        Timber.tag("MusicService").i("MusicService created")

        lyricsPrefetcher = LyricsPrefetcher(database, lyricsHelper, scope)
        initializePlayer()
        sleepTimer = SleepTimer(player, scope)
        StreamUrlResolver.clearMemoryCache("service startup")

        networkPlaybackMonitor = NetworkPlaybackMonitor(
            context = this,
            player = player,
            scope = scope,
            streamExtractor = streamExtractor,
            downloadUtil = downloadUtil,
            waitingForNetworkConnection = _waitingForNetworkConnection,
            playbackQualityModeProvider = ::getPlaybackQualityMode,
            isDownloadCompleted = ::isDownloadCompleted,
        )
        networkPlaybackMonitor.register()
        playbackRecoveryCoordinator = PlaybackRecoveryCoordinator(
            context = this,
            player = player,
            scope = scope,
            streamExtractor = streamExtractor,
            downloadUtil = downloadUtil,
            networkPlaybackMonitor = networkPlaybackMonitor,
            playbackQualityModeProvider = ::getPlaybackQualityMode,
        )
        playbackPreferenceObserver = PlaybackPreferenceObserver(
            context = this,
            player = player,
            scope = scope,
            playerVolume = _playerVolume,
            playbackFadeFactor = playbackFadeFactor,
            crossfadeDurationMs = crossfadeDurationMs,
            audioNormalizationEnabled = audioNormalizationEnabled,
            onAutoSkipNextOnErrorChanged = { playbackRecoveryCoordinator.setAutoSkipNextOnError(it) },
        ).also { it.start() }
        radioQueueManager = RadioQueueManager(
            player = player,
            scope = scope,
            streamExtractor = streamExtractor,
            downloadUtil = downloadUtil,
            playbackQualityModeProvider = ::getPlaybackQualityMode,
            setQueueTitle = { queueTitle = it },
            setCurrentQueue = { currentQueue = it },
        )

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

        // Start Discord Rich Presence if configured
        scope.launch {
            try {
                val prefs = this@MusicService.dataStore.data.first()
                val enabled = prefs[EnableDiscordRPCKey] ?: false
                val token = prefs[DiscordTokenKey] ?: ""
                if (enabled && token.isNotBlank()) {
                    discordPresenceManager.start(
                        context = this@MusicService,
                        token = token,
                        songProvider = {
                            val meta = _currentMediaMetadata.value
                            if (meta != null) {
                                SongPresenceData(
                                    title = meta.title,
                                    videoId = meta.id,
                                    artist = meta.artists.firstOrNull()?.name ?: "",
                                    album = meta.album?.title ?: "",
                                    thumbnail = meta.thumbnailUrl ?: "",
                                    artistImage = meta.artists.firstOrNull()?.thumbnailUrl ?: "",
                                    position = player.currentPosition,
                                    duration = meta.duration.toLong(),
                                    isPaused = !player.playWhenReady,
                                )
                            } else {
                                SongPresenceData()
                            }
                        },
                        positionProvider = { player.currentPosition },
                        pauseProvider = { !player.playWhenReady },
                        intervalProvider = { getPresenceIntervalMillis() },
                    )
                }
            } catch (e: Exception) {
                Timber.tag("MusicService").e(e, "Failed to start Discord presence")
            }
        }
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

    private fun initializePlayer() {
        player = PlayerFactory.createPlayer(this, okHttpClient, downloadUtil)
            .also { exoPlayer ->
                exoPlayer.setOffloadEnabled(true)
                exoPlayer.playWhenReady = false
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.addListener(this)
            }

        sessionCallback.onPlayerReady(player)

        crossfadePlaybackCoordinator = CrossfadePlaybackCoordinator(
            context = this,
            player = player,
            database = database,
            okHttpClient = okHttpClient,
            downloadUtil = downloadUtil,
            crossfadeDurationMs = crossfadeDurationMs,
            playbackFadeFactor = playbackFadeFactor,
            playerVolume = _playerVolume,
            audioFocusVolumeFactor = audioFocusVolumeFactor,
            audioNormalizationEnabled = audioNormalizationEnabled,
        ).also { it.start(scope) }

        sessionManager = SessionManager(this, player, sessionCallback)

        playbackNotificationManager = PlaybackNotificationManager(this, player) { mediaSession }
        playbackNotificationManager.createChannelIfNeeded()
        setMediaNotificationProvider(playbackNotificationManager.createProvider())
        logMediaControlState("provider-ready")
    }

    fun applyEqualizerBands(bands: List<com.omnitune.app.playback.EqualizerBand>) {
        equalizerController.applyBands(bands)
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
        StartupTracker.reset()
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
                    StreamUrlResolver.resolveMediaItem(currentItem, streamExtractor, downloadUtil, getPlaybackQualityMode())
                } else {
                    currentItem
                }
            }
            if (resolvedCurrent == null) {
                Timber.e("Current stream resolution failed for ${currentItem.mediaId}")
                val message = if (!isInternetAvailable(this@MusicService) && !isDownloadCompleted(currentItem.mediaId)) {
                    _waitingForNetworkConnection.value = true
                    "This song is not downloaded and cannot play offline."
                } else {
                    "Could not resolve stream for ${currentItem.mediaMetadata.title ?: "track"}"
                }
                Toast.makeText(this@MusicService, message, Toast.LENGTH_LONG).show()
                return@launch
            }
            Timber.tag("OmniTunePlaybackTrace").i("Current item resolved: ${currentItem.mediaId}")

            val resolvedItems = withContext(Dispatchers.IO) {
                StreamUrlResolver.resolveMediaItems(
                    initialStatus.items,
                    streamExtractor,
                    downloadUtil,
                    getPlaybackQualityMode(),
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
                StartupTracker.logPlayerPrepare()
                player.prepare()
                player.playWhenReady = playWhenReady
                Timber.tag("OmniTunePlaybackTrace").i(
                    "Player prepared: count=${player.mediaItemCount}, current=${player.currentMediaItem?.mediaId}, state=${player.playbackState}, playWhenReady=${player.playWhenReady}"
                )
                preResolveNextTracks()
            }
        }
    }

    private var preResolveJob: kotlinx.coroutines.Job? = null

    private fun preResolveNextTracks() {
        preResolveJob?.cancel()
        preResolveJob = scope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                if (player.mediaItemCount == 0) return@launch
                
                // Get the actual next index considering shuffle and repeat modes
                val nextIndex1 = player.nextMediaItemIndex
                if (nextIndex1 == androidx.media3.common.C.INDEX_UNSET || nextIndex1 == player.currentMediaItemIndex) return@launch
                
                val item1 = player.getMediaItemAt(nextIndex1)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (StreamUrlResolver.isYouTubeVideoId(item1.localConfiguration?.uri)) {
                        val resolved = StreamUrlResolver.resolveMediaItem(item1, streamExtractor, downloadUtil, getPlaybackQualityMode())
                        if (resolved != null) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (StreamUrlResolver.isYouTubeVideoId(player.getMediaItemAt(nextIndex1).localConfiguration?.uri)) {
                                    player.replaceMediaItem(nextIndex1, resolved)
                                    Timber.tag("OmniTunePlaybackTrace").d("Pre-resolved queue index $nextIndex1")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Pre-resolve failed")
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
        radioQueueManager?.startRadioSeamlessly()
    }

    fun playNext(items: List<MediaItem>) {
        scope.launch {
            val resolvedItems = withContext(Dispatchers.IO) {
                StreamUrlResolver.resolveMediaItems(items, streamExtractor, downloadUtil, getPlaybackQualityMode())
            }
            val insertIndex = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
            player.addMediaItems(
                insertIndex,
                resolvedItems
            )
            Timber.tag("OmniTuneQueue").i("Play Next: inserted ${resolvedItems.size} items at index $insertIndex")
            player.prepare()
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        scope.launch {
            val resolvedItems = withContext(Dispatchers.IO) {
                StreamUrlResolver.resolveMediaItems(items, streamExtractor, downloadUtil, getPlaybackQualityMode())
            }
            player.addMediaItems(resolvedItems)
            Timber.tag("OmniTuneQueue").i("Add to Queue: added ${resolvedItems.size} items, new queue size: ${player.mediaItemCount}")
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

    fun restartDiscordPresence() {
        scope.launch {
            try {
                val prefs = this@MusicService.dataStore.data.first()
                val enabled = prefs[EnableDiscordRPCKey] ?: false
                val token = prefs[DiscordTokenKey] ?: ""
                if (enabled && token.isNotBlank()) {
                    discordPresenceManager.stop()
                    discordPresenceManager.start(
                        context = this@MusicService,
                        token = token,
                        songProvider = {
                            val meta = _currentMediaMetadata.value
                            if (meta != null) {
                                SongPresenceData(
                                    title = meta.title,
                                    videoId = meta.id,
                                    artist = meta.artists.firstOrNull()?.name ?: "",
                                    album = meta.album?.title ?: "",
                                    thumbnail = meta.thumbnailUrl ?: "",
                                    artistImage = meta.artists.firstOrNull()?.thumbnailUrl ?: "",
                                    position = player.currentPosition,
                                    duration = meta.duration.toLong(),
                                    isPaused = !player.playWhenReady,
                                )
                            } else {
                                SongPresenceData()
                            }
                        },
                        positionProvider = { player.currentPosition },
                        pauseProvider = { !player.playWhenReady },
                        intervalProvider = { getPresenceIntervalMillis() },
                    )
                } else {
                    discordPresenceManager.stop()
                }
            } catch (e: Exception) {
                Timber.tag("MusicService").e(e, "Failed to restart Discord presence")
            }
        }
    }

    override fun onDestroy() {
        discordPresenceManager.destroy()
        Timber.tag("MusicService").i("MusicService destroyed")
        try {
            crossfadePlaybackCoordinator?.release()
            crossfadePlaybackCoordinator = null
        } catch (_: Exception) {}
        playbackPreferenceObserver?.stop()
        playbackPreferenceObserver = null
        radioQueueManager = null
        equalizerController.release()
        if (::playbackRecoveryCoordinator.isInitialized) {
            playbackRecoveryCoordinator.release()
        }
        sessionManager?.release()
        sessionManager = null
        if (::networkPlaybackMonitor.isInitialized) {
            networkPlaybackMonitor.release()
        }
        if (::playbackNotificationManager.isInitialized) {
            playbackNotificationManager.release()
        }
        scopeJob.cancel()
        player.release()
        super.onDestroy()
    }

    // --- Player.Listener ---

    override fun onPlaybackStateChanged(state: Int) {
        Timber.tag("MusicService").v("Playback state: $state")
        StartupTracker.logState(state)
        crossfadePlaybackCoordinator?.onPlaybackStateChanged(state)
        updateNotification()
        logMediaControlState("state-$state")
        postMediaNotificationFallback("state-$state")
        if (state == Player.STATE_READY) {
            equalizerController.setupIfNeeded(player.audioSessionId)
        }
        if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
            saveQueueState()
        }
        playbackRecoveryCoordinator.onPlaybackStateChanged(state)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            try {
                applicationContext.dataStore.edit { it[ShuffleEnabledKey] = shuffleModeEnabled }
                Timber.tag("OmniTuneQueue").d("Shuffle mode persisted: $shuffleModeEnabled")
            } catch (e: Exception) {
                Timber.e(e, "Failed to save shuffle mode")
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                applicationContext.dataStore.edit { it[RepeatModeKey] = repeatMode }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save repeat mode")
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        playbackRecoveryCoordinator.resetRetry(mediaItem?.mediaId ?: "")
        crossfadePlaybackCoordinator?.onMediaItemTransition(mediaItem, reason)
        val meta = mediaItem?.metadata ?: _currentMediaMetadata.value
        _currentMediaMetadata.value = meta
        updateNotification()
        logMediaControlState("item-transition")
        postMediaNotificationFallback("item-transition")
        startPlaybackTracker(mediaItem)
        saveQueueState()
        preResolveNextTracks()

        lyricsPrefetcher.prefetch(meta)
    }

    override fun onPlayerError(error: PlaybackException) {
        Timber.tag("MusicService").e(error, "Player error")
        logMediaControlState("player-error")
        reportException(error)
        playbackRecoveryCoordinator.onPlayerError(error)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        logMediaControlState("is-playing-$isPlaying")
        postMediaNotificationFallback("is-playing-$isPlaying", force = true)

        if (isPlaying) {
            val mediaItem = player.currentMediaItem
            val mediaId = mediaItem?.mediaId
            if (mediaId != null && mediaId != lastRecordedRecentMediaId) {
                lastRecordedRecentMediaId = mediaId
                val meta = mediaItem.metadata
                if (meta != null) {
                    val durationMs = if (player.duration == androidx.media3.common.C.TIME_UNSET) 0L else player.duration
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        database.insert(meta)
                        database.insertRecentEvent(mediaId, durationMs)
                        timber.log.Timber.tag("OmniTuneRecent").d("Recorded recent play: ${meta.title}")
                    }
                } else {
                    timber.log.Timber.tag("OmniTuneRecent").w("Metadata is null for $mediaId, skipping recent play record")
                }
            }
        }
    }

    override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
        updateNotification()
        logMediaControlState("metadata")
        postMediaNotificationFallback("metadata", force = true)
    }

    private fun isDownloadCompleted(mediaId: String?): Boolean {
        val id = mediaId?.takeIf { it.isNotBlank() } ?: return false
        return try {
            downloadUtil.downloadManager.downloadIndex.getDownload(id)?.state ==
                androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
        } catch (e: Exception) {
            Timber.tag("MusicService").w(e, "Failed to inspect download state for $id")
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun updateNotification() {
        try {
            sessionManager?.updateCustomLayout()

            if (::player.isInitialized) {
                playbackNotificationManager.updateWidget()
            }
        } catch (e: Exception) {
            reportException(e)
        }
    }

    private fun logMediaControlState(event: String) {
        if (::playbackNotificationManager.isInitialized) {
            playbackNotificationManager.logState(event)
        }
    }

    private fun postMediaNotificationFallback(reason: String, force: Boolean = false) {
        if (::playbackNotificationManager.isInitialized) {
            playbackNotificationManager.postFallback(reason, force)
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
                Timber.tag("OmniTuneQueue").i("Queue saved: count=$count, index=$currentIndex, pos=$currentPos")
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
        val uriStr = localConfiguration?.uri?.toString() ?: ""
        return StreamUrlResolver.isYouTubeVideoId(localConfiguration?.uri) ||
            uriStr.startsWith("file:///offline/") ||
            uriStr.startsWith("omnitune-unresolved://")
    }

}
