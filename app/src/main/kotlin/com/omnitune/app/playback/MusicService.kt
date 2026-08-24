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
import androidx.media3.exoplayer.offline.Download
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
import com.omnitune.app.sync.YouTubeLibrarySync
import com.omnitune.app.utils.isInternetAvailable
import com.omnitune.app.utils.reportException
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.PreferenceStore
import com.omnitune.app.constants.RepeatModeKey
import com.omnitune.app.constants.RestrictExplicitContentKey
import com.omnitune.app.constants.ShuffleEnabledKey
import com.omnitune.app.constants.HistoryDuration
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.omnitune.app.constants.ScrobbleDelayPercentKey
import com.omnitune.app.constants.ScrobbleDelaySecondsKey
import com.omnitune.app.constants.ScrobbleMinSongDurationKey
import com.omnitune.app.constants.StopMusicOnTaskClearKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlin.math.min
import kotlin.math.pow
import timber.log.Timber
import javax.inject.Inject

import com.omnitune.app.constants.EqualizerBandLevelsMbKey
import com.omnitune.app.constants.EqualizerEnabledKey
import com.omnitune.app.playback.continuation.AutoplayCandidate
import com.omnitune.app.playback.continuation.AutoplayContinuationManager
import com.omnitune.app.playback.continuation.AutoplayRecommendationResolver
import com.omnitune.app.playback.continuation.AutoplayRetryPolicy
import com.omnitune.app.playback.continuation.LikedSongsPlaybackPlanner
import com.omnitune.app.playback.continuation.OmniAutoplayRecommendationProvider
import com.omnitune.app.playback.continuation.PlaybackContinuationPolicy
import com.omnitune.app.playback.continuation.PlaybackContext
import com.omnitune.app.playback.continuation.PlaybackSourceType
import com.omnitune.app.playback.continuation.QueuePlaybackContextMapper
import com.omnitune.app.playback.continuation.TasteSignalClassifier
import com.omnitune.app.playback.continuation.TasteSignal
import com.omnitune.app.playback.continuation.withSeedItem
import com.omnitune.app.playback.ScrobblingManager

@AndroidEntryPoint
class MusicService : MediaLibraryService(), Player.Listener {

    @Inject lateinit var sessionCallback: MusicSessionCallback
    @Inject lateinit var database: MusicDatabase
    @Inject lateinit var lyricsHelper: LyricsHelper
    @Inject lateinit var streamExtractor: com.omnitune.app.data.StreamExtractor
    @Inject lateinit var downloadUtil: DownloadUtil
    @Inject lateinit var okHttpClient: okhttp3.OkHttpClient

    private lateinit var scrobblingManager: ScrobblingManager

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
        private const val ACTION_LIKE = PlaybackNotificationManager.ACTION_LIKE
        private const val ACTION_REPEAT = PlaybackNotificationManager.ACTION_REPEAT
        private const val ACTION_STOP = PlaybackNotificationManager.ACTION_STOP
        private const val PREVIOUS_RESTART_THRESHOLD_MS = 3_000L
        // A process can be killed without an orderly service callback. Keep the persisted
        // queue position close enough to resume the listening session safely.
        private const val QUEUE_POSITION_CHECKPOINT_MS = 5_000L
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
    private var autoDownloadOnLikeCoordinator: AutoDownloadOnLikeCoordinator? = null
    private lateinit var equalizerEffectObserver: EqualizerEffectObserver
    private var radioQueueManager: RadioQueueManager? = null
    private val equalizerController by lazy { EqualizerController(this) }
    private val audioEffectController by lazy { AudioEffectController(this) }
    private val _volumeNormalizationFactor = MutableStateFlow(1f)

    // OMNITUNE: Sleep timer
    lateinit var sleepTimer: SleepTimer

    fun binder(): MusicBinder = binder

    private val _waitingForNetworkConnection = MutableStateFlow(false)
    val waitingForNetworkConnection = _waitingForNetworkConnection.asStateFlow()

    // OMNITUNE: Playback tracking
    private lateinit var playCountTracker: PlayCountTracker
    private val _currentMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
    val currentMediaMetadata = _currentMediaMetadata.asStateFlow()
    var queueTitle: String? = null
        private set
    private val _queueRestoreCompleted = MutableStateFlow(false)
    val queueRestoreCompleted = _queueRestoreCompleted.asStateFlow()
    private var saveQueueJob: Job? = null
    private var queuePositionCheckpointJob: Job? = null
    private var playQueueJob: Job? = null
    private lateinit var bluetoothAudioHandler: BluetoothAudioHandler
    private var pausedByDeviceMute = false


    private var currentQueue: Queue = EmptyQueue
    private lateinit var autoplayResolver: AutoplayRecommendationResolver
    private var currentPlaybackContext: PlaybackContext = PlaybackContext.Unknown
    private var autoplayContinuationJob: Job? = null
    private lateinit var autoplayContinuation: AutoplayContinuationManager
    private lateinit var tasteSignalRecorder: TasteSignalRecorder
    private lateinit var historyTracker: PlaybackHistoryTracker
    private var userNavigationJob: Job? = null

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
                seekToNext()
                postMediaNotificationFallback("action-next", force = true)
                return START_STICKY
            }
            ACTION_PREVIOUS -> {
                seekToPrevious()
                postMediaNotificationFallback("action-previous", force = true)
                return START_STICKY
            }
            ACTION_LIKE -> {
                toggleLike()
                postMediaNotificationFallback("action-like", force = true)
                return START_STICKY
            }
            ACTION_REPEAT -> {
                if (::player.isInitialized) {
                    player.repeatMode = when (player.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                        else -> Player.REPEAT_MODE_OFF
                    }
                }
                postMediaNotificationFallback("action-repeat", force = true)
                return START_STICKY
            }
            ACTION_STOP -> {
                if (::player.isInitialized) {
                    player.pause()
                    player.stop()
                    player.clearMediaItems()
                }
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        Timber.tag("MusicService").i("MusicService created")

        lyricsPrefetcher = LyricsPrefetcher(database, lyricsHelper, scope)
        autoplayResolver = AutoplayRecommendationResolver(
            OmniAutoplayRecommendationProvider(database),
        )
        initializePlayer()
        sleepTimer = SleepTimer(player, scope)
        StreamUrlResolver.clearMemoryCache("service startup")

        networkPlaybackMonitor = NetworkPlaybackMonitor(
            context = this,
            waitingForNetworkConnection = _waitingForNetworkConnection,
            isDownloadCompleted = ::isDownloadCompleted,
            onNetworkRestored = {
                if (::playbackRecoveryCoordinator.isInitialized) {
                    playbackRecoveryCoordinator.retryAfterNetworkRestored()
                }
            },
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
            normalizationFactor = _volumeNormalizationFactor,
            crossfadeDurationMs = crossfadeDurationMs,
            audioNormalizationEnabled = audioNormalizationEnabled,
            onAutoSkipNextOnErrorChanged = { playbackRecoveryCoordinator.setAutoSkipNextOnError(it) },
        ).also { it.start() }
        startAutoDownloadOnLikeObserver()
        equalizerEffectObserver = EqualizerEffectObserver(
            preferences = dataStore.data,
            equalizerController = equalizerController,
            audioEffectController = audioEffectController,
            scope = scope,
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
                val prefs = this@MusicService.dataStore.data.first()
                val persistentQueue = prefs[com.omnitune.app.constants.PersistentQueueKey] ?: true
                if (!persistentQueue) {
                    database.clearQueue()
                } else {
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
                                position = savedQueue.position,
                                playbackContext = QueuePlaybackContextMapper.fromEntity(savedQueue, mediaItems),
                            )
                            withContext(Dispatchers.Main) {
                                restoreQueueMetadataOnly(queue)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag("MusicService").e(e, "Failed to restore persistent queue")
            } finally {
                _queueRestoreCompleted.value = true
            }
        }

        bluetoothAudioHandler = BluetoothAudioHandler(
            context = this,
            preferences = dataStore.data,
            scope = scope,
            isPlayingProvider = { ::player.isInitialized && player.isPlaying },
            onAutoStartPlayback = {
                if (!::player.isInitialized || player.mediaItemCount == 0 || player.playWhenReady) {
                    false
                } else {
                    playOrResolveCurrent()
                    true
                }
            },
            onPauseForDisconnect = { pausePlayback() },
            onNotificationFallback = { reason, force -> postMediaNotificationFallback(reason, force) },
        ).also { it.start() }

        scrobblingManager = ScrobblingManager(this, scope)

        tasteSignalRecorder = TasteSignalRecorder(
            preferences = dataStore.data,
            database = database,
            scope = scope,
        )
        historyTracker = PlaybackHistoryTracker()
        autoplayContinuation = AutoplayContinuationManager(
            database = database,
            preferences = dataStore.data,
            autoplayResolver = autoplayResolver,
            scope = scope,
        )
        playCountTracker = PlayCountTracker(
            player = player,
            database = database,
            scrobblingManager = scrobblingManager,
            currentMetadataProvider = { _currentMediaMetadata.value },
            preferences = dataStore.data,
            scope = scope,
        )

        sessionCallback.onToggleLike = { toggleLike() }
        sessionCallback.onToggleLibrary = { toggleLibrary() }
        sessionCallback.onStartRadio = { toggleStartRadio() }

    }

    private suspend fun restoreQueueMetadataOnly(queue: Queue) {
        val unfilteredStatus = queue.getInitialStatus()
        val restrictExplicit = dataStore.data.first()[RestrictExplicitContentKey] ?: false
        val selectedId = unfilteredStatus.items
            .getOrNull(unfilteredStatus.mediaItemIndex.coerceIn(0, unfilteredStatus.items.lastIndex.coerceAtLeast(0)))
            ?.mediaId
        val visibleItems = if (restrictExplicit) {
            unfilteredStatus.items.filterNot { it.metadata?.explicit == true }
        } else {
            unfilteredStatus.items
        }
        val initialStatus = unfilteredStatus.copy(
            items = visibleItems,
            mediaItemIndex = selectedId
                ?.let { id -> visibleItems.indexOfFirst { it.mediaId == id } }
                ?.takeIf { it >= 0 }
                ?: 0,
        )
        if (initialStatus.items.isEmpty()) {
            Timber.tag("OmniTunePlaybackTrace").w("Restore skipped: saved queue is empty")
            return
        }

        val restoredIndex = initialStatus.mediaItemIndex.coerceIn(0, initialStatus.items.size - 1)
        currentQueue = queue
        currentPlaybackContext = queue.playbackContext
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
                exoPlayer.playWhenReady = false
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.addListener(this)
            }

        sessionCallback.onPlayerReady(player)

        crossfadePlaybackCoordinator = CrossfadePlaybackCoordinator(
            context = this,
            player = player,
            database = database,
            streamExtractor = streamExtractor,
            okHttpClient = okHttpClient,
            downloadUtil = downloadUtil,
            playbackQualityModeProvider = ::getPlaybackQualityMode,
            crossfadeDurationMs = crossfadeDurationMs,
            playbackFadeFactor = playbackFadeFactor,
            playerVolume = _playerVolume,
            audioFocusVolumeFactor = audioFocusVolumeFactor,
            audioNormalizationEnabled = audioNormalizationEnabled,
        ).also { it.start(scope) }

        sessionManager = SessionManager(this, player, sessionCallback, scope)

        playbackNotificationManager = PlaybackNotificationManager(this, player) { mediaSession }
        playbackNotificationManager.createChannelIfNeeded()
        setMediaNotificationProvider(playbackNotificationManager.createProvider())
        logMediaControlState("provider-ready")
    }

    fun applyEqualizerBands(bands: List<com.omnitune.app.playback.EqualizerBand>) {
        equalizerController.applyBands(bands)
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        equalizerController.setEnabled(enabled)
    }

    private fun startAutoDownloadOnLikeObserver() {
        autoDownloadOnLikeCoordinator?.stop()
        autoDownloadOnLikeCoordinator = AutoDownloadOnLikeCoordinator(
            preferences = dataStore.data,
            database = database,
            downloadUtil = downloadUtil,
            scope = scope,
        ).also { it.start() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scope.launch {
            val stopOnClear = dataStore.data.first()[StopMusicOnTaskClearKey] ?: false
            if (stopOnClear) {
                player.pause()
                player.stop()
                stopSelf()
            } else if (!player.playWhenReady || player.mediaItemCount == 0) {
                stopSelf()
            }
        }
    }

    fun playQueue(queue: Queue, playWhenReady: Boolean = true) {
        val restrictExplicit = PreferenceStore.get(RestrictExplicitContentKey) ?: false
        if (restrictExplicit && queue.preloadItem?.explicit == true) {
            Toast.makeText(this, "Explicit content is restricted on this device.", Toast.LENGTH_SHORT).show()
            return
        }

        playQueueJob?.cancel()
        preResolveJob?.cancel()
        StartupTracker.reset()
        Timber.tag("OmniTunePlaybackTrace").i("playQueue requested: playWhenReady=$playWhenReady")

        playQueueJob = scope.launch {
            val unfilteredStatus = queue.getInitialStatus()
            val originalIndex = unfilteredStatus.mediaItemIndex.coerceIn(
                0,
                unfilteredStatus.items.lastIndex.coerceAtLeast(0),
            )
            val requestedItem = unfilteredStatus.items.getOrNull(originalIndex)
            if (restrictExplicit && requestedItem?.metadata?.explicit == true) {
                Toast.makeText(
                    this@MusicService,
                    "Explicit content is restricted on this device.",
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }

            val visibleItems = if (restrictExplicit) {
                unfilteredStatus.items.filterNot { it.metadata?.explicit == true }
            } else {
                unfilteredStatus.items
            }
            val visibleIndex = requestedItem?.let { selected ->
                visibleItems.indexOfFirst { it.mediaId == selected.mediaId }
            }?.takeIf { it >= 0 } ?: 0
            val initialStatus = unfilteredStatus.copy(
                items = visibleItems,
                mediaItemIndex = visibleIndex,
            )

            currentQueue = queue
            currentPlaybackContext = queue.playbackContext
            autoplayContinuation.clearFailedCandidates()
            resetPlaybackHistory()
            queueTitle = null
            currentPlaybackContext = queue.playbackContext.withSeedItem(
                initialStatus.items.getOrNull(initialStatus.mediaItemIndex.coerceIn(0, initialStatus.items.lastIndex.coerceAtLeast(0))),
            )
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
            val queueItems = initialStatus.items.map { it.withOriginalVideoIdUri() }
            val currentItem = queueItems[requestedIndex]
            Timber.tag("OmniTunePlaybackTrace").i(
                "Player set unresolved queue: count=${queueItems.size}, index=$requestedIndex, position=${initialStatus.position}"
            )
            // Queue entries intentionally start with a bare YouTube ID so the
            // resolver can replace it with a signed stream URL. This applies to
            // preload queues too: preparing a preload item's raw ID lets ExoPlayer
            // try it as a local source, which results in an immediate pause/error.
            player.playWhenReady = false
            player.setMediaItems(queueItems, requestedIndex, initialStatus.position)
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

            Timber.tag("OmniTunePlaybackTrace").i(
                "Player current item resolved: index=$requestedIndex, position=${initialStatus.position}"
            )
            if (requestedIndex in 0 until player.mediaItemCount &&
                player.getMediaItemAt(requestedIndex).mediaId == currentItem.mediaId
            ) {
                player.replaceMediaItem(requestedIndex, resolvedCurrent)
                player.seekTo(requestedIndex, initialStatus.position)
            } else {
                Timber.tag("OmniTunePlaybackTrace").w("Queue changed before current stream resolved")
                return@launch
            }
            StartupTracker.logPlayerPrepare()
            player.prepare()
            player.playWhenReady = playWhenReady
            Timber.tag("OmniTunePlaybackTrace").i(
                "Player prepared: count=${player.mediaItemCount}, current=${player.currentMediaItem?.mediaId}, state=${player.playbackState}, playWhenReady=${player.playWhenReady}"
            )
        }
    }

    private var preResolveJob: kotlinx.coroutines.Job? = null

    private fun preResolveNextTrack() {
        preResolveJob?.cancel()
        preResolveJob = scope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                val index = player.nextMediaItemIndex
                if (index == androidx.media3.common.C.INDEX_UNSET) return@launch
                val item = player.getMediaItemAt(index)
                if (!StreamUrlResolver.isYouTubeVideoId(item.localConfiguration?.uri)) return@launch

                val resolved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    StreamUrlResolver.resolveMediaItem(item, streamExtractor, downloadUtil, getPlaybackQualityMode())
                } ?: return@launch

                if (index < player.mediaItemCount) {
                    val queuedItem = player.getMediaItemAt(index)
                    if (queuedItem.mediaId == item.mediaId &&
                        StreamUrlResolver.isYouTubeVideoId(queuedItem.localConfiguration?.uri)
                    ) {
                        player.replaceMediaItem(index, resolved)
                        Timber.tag("OmniTunePlaybackTrace").d("Pre-resolved queue index $index")
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Pre-resolve failed")
            }
        }
    }

    private suspend fun seekToResolvedMediaItem(index: Int, positionMs: Long): Boolean {
        if (index !in 0 until player.mediaItemCount) return false

        val target = player.getMediaItemAt(index)
        val targetNeedsResolution = target.needsFreshResolution()
        val playableTarget = if (targetNeedsResolution) {
            val originalItem = target.withOriginalVideoIdUri()
            withContext(Dispatchers.IO) {
                if (StreamUrlResolver.isYouTubeVideoId(originalItem.localConfiguration?.uri)) {
                    StreamUrlResolver.resolveMediaItem(
                        originalItem,
                        streamExtractor,
                        downloadUtil,
                        getPlaybackQualityMode(),
                    )
                } else {
                    originalItem
                }
            }
        } else {
            target
        }

        if (playableTarget == null) {
            Timber.tag("OmniTunePlaybackTrace").w("Could not resolve navigation target ${target.mediaId}")
            Toast.makeText(
                this,
                "Could not resolve stream for ${target.mediaMetadata.title ?: "track"}",
                Toast.LENGTH_SHORT,
            ).show()
            return false
        }

        if (index !in 0 until player.mediaItemCount) return false
        val queuedItem = player.getMediaItemAt(index)
        if (queuedItem.mediaId == target.mediaId && targetNeedsResolution) {
            player.replaceMediaItem(index, playableTarget)
        }

        player.seekTo(index, positionMs.coerceAtLeast(0L))
        if (targetNeedsResolution || player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
        return true
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
                    position = position,
                    playbackContext = currentPlaybackContext,
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

    fun seekToNext() {
        userNavigationJob?.cancel()
        userNavigationJob = scope.launch {
            if (!::player.isInitialized || player.mediaItemCount == 0) return@launch

            val nextIndex = player.nextMediaItemIndex
            if (nextIndex == C.INDEX_UNSET) {
                player.seekToNext()
                player.playWhenReady = true
                return@launch
            }

            if (seekToResolvedMediaItem(nextIndex, 0L)) {
                player.playWhenReady = true
            }
        }
    }

    fun seekToPrevious() {
        userNavigationJob?.cancel()
        userNavigationJob = scope.launch {
            if (!::player.isInitialized || player.mediaItemCount == 0) return@launch

            if (player.playbackState != Player.STATE_ENDED &&
                player.currentPosition > PREVIOUS_RESTART_THRESHOLD_MS
            ) {
                player.seekTo(0L)
                player.playWhenReady = true
                return@launch
            }

            while (historyTracker.hasPrevious()) {
                val previous = historyTracker.popPrevious() ?: continue
                val historyIndex = findHistoryIndex(previous)
                if (historyIndex == C.INDEX_UNSET || historyIndex == player.currentMediaItemIndex) {
                    continue
                }

                historyTracker.suppressNextRecord()
                if (seekToResolvedMediaItem(historyIndex, 0L)) {
                    player.playWhenReady = true
                    return@launch
                }
                historyTracker.clearSuppressNextRecord()
            }

            val previousIndex = player.previousMediaItemIndex
            if (previousIndex != C.INDEX_UNSET) {
                historyTracker.suppressNextRecord()
                if (seekToResolvedMediaItem(previousIndex, 0L)) {
                    player.playWhenReady = true
                    return@launch
                }
                historyTracker.clearSuppressNextRecord()
            }

            player.seekTo(0L)
            player.playWhenReady = true
        }
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
            val updatedSong = if (song != null) {
                song.song.localToggleLike()
            } else {
                meta.toSongEntity().copy(
                    liked = !meta.liked,
                    likedDate = if (!meta.liked) java.time.LocalDateTime.now() else null
                )
            }
            database.upsert(updatedSong)
            YouTubeLibrarySync.syncSongLike(updatedSong, liked = updatedSong.liked)
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
        currentPlaybackContext = PlaybackContext.Unknown
        autoplayContinuation.clearTransientState()
        queueTitle = null
        _waitingForNetworkConnection.value = false
        _currentMediaMetadata.value = null
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
    }

    fun prefetchLyrics(metadata: MediaMetadata?) {
        if (::lyricsPrefetcher.isInitialized) {
            lyricsPrefetcher.prefetch(metadata)
        }
    }

    override fun onDestroy() {
        Timber.tag("MusicService").i("MusicService destroyed")
        queuePositionCheckpointJob?.cancel()
        queuePositionCheckpointJob = null
        try {
            crossfadePlaybackCoordinator?.release()
            crossfadePlaybackCoordinator = null
        } catch (_: Exception) {}
        playbackPreferenceObserver?.stop()
        playbackPreferenceObserver = null
        autoDownloadOnLikeCoordinator?.stop()
        autoDownloadOnLikeCoordinator = null
        if (::equalizerEffectObserver.isInitialized) {
            equalizerEffectObserver.stop()
        }
        radioQueueManager = null
        equalizerController.release()
        audioEffectController.release()
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
        if (::bluetoothAudioHandler.isInitialized) {
            bluetoothAudioHandler.stop()
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
            audioEffectController.setupIfNeeded(player.audioSessionId)
        }
        if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
            saveQueueState()
        }
        playbackRecoveryCoordinator.onPlaybackStateChanged(state)
        if (state == Player.STATE_ENDED) {
            handlePlaybackEnded()
        }
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
        updatePlaybackHistory(mediaItem)
        recordTasteSignalForPreviousTransition(
            completed = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
        )
        playbackRecoveryCoordinator.resetRetry(mediaItem?.mediaId ?: "")
        crossfadePlaybackCoordinator?.onMediaItemTransition(mediaItem, reason)
        val meta = mediaItem?.metadata ?: _currentMediaMetadata.value
        _currentMediaMetadata.value = meta
        updateNotification()
        logMediaControlState("item-transition")
        postMediaNotificationFallback("item-transition")
        playCountTracker.startFor(mediaItem)
        beginTasteWindow(mediaItem)
        saveQueueState()
        updateVolumeNormalizationFactor(mediaItem?.mediaId)

        // Send a now-playing update when the user configured ListenBrainz.
        meta?.let { m ->
            scrobblingManager.onTrackChanged(
                title = m.title,
                artist = m.artists.firstOrNull()?.name ?: "",
                album = m.album?.title,
            )
            lyricsPrefetcher.prefetch(m)
        }

    }

    private fun updatePlaybackHistory(mediaItem: MediaItem?) {
        historyTracker.onTransition(mediaItem, player.currentMediaItemIndex)
    }

    private fun resetPlaybackHistory() {
        historyTracker.reset()
    }

    private fun findHistoryIndex(entry: PlaybackHistoryTracker.Entry): Int {
        if (entry.index in 0 until player.mediaItemCount &&
            player.getMediaItemAt(entry.index).mediaId == entry.mediaId
        ) {
            return entry.index
        }

        for (index in 0 until player.mediaItemCount) {
            if (player.getMediaItemAt(index).mediaId == entry.mediaId) {
                return index
            }
        }
        return C.INDEX_UNSET
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
            startQueuePositionCheckpoint()
            preResolveNextTrack()
            val mediaItem = player.currentMediaItem
            val mediaId = mediaItem?.mediaId
            if (mediaId != null) {
                val meta = mediaItem.metadata
                if (meta != null) {
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        database.insert(meta)
                        timber.log.Timber.tag("OmniTuneRecent").d("Ensured song metadata for active play: ${meta.title}")
                    }
                    lyricsPrefetcher.prefetch(meta)
                } else {
                    timber.log.Timber.tag("OmniTuneRecent").w("Metadata is null for $mediaId, skipping recent play record")
                }
            }
            if (!tasteSignalRecorder.hasOpenWindow()) {
                beginTasteWindow(mediaItem)
            }
        } else if (
            player.playbackState == Player.STATE_READY &&
            player.currentPosition >= TasteSignalRecorder.MIN_LISTEN_HISTORY_MS
        ) {
            stopQueuePositionCheckpoint(saveImmediately = true)
            recordTasteSignalForPreviousTransition(completed = false)
        } else {
            stopQueuePositionCheckpoint(saveImmediately = true)
        }
    }

    override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
        updateNotification()
        logMediaControlState("metadata")
        postMediaNotificationFallback("metadata", force = true)
    }

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        scope.launch {
            val pauseOnMute = dataStore.data.first()[com.omnitune.app.constants.PauseOnDeviceMuteKey] ?: false
            if (!pauseOnMute) {
                pausedByDeviceMute = false
                return@launch
            }

            if (volume == 0 || muted) {
                if (player.playWhenReady) {
                    pausedByDeviceMute = true
                    pausePlayback()
                }
            } else if (pausedByDeviceMute) {
                pausedByDeviceMute = false
                playOrResolveCurrent()
            }
        }
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
            val meta = _currentMediaMetadata.value
            sessionManager?.updateCustomLayout(
                isLiked = meta?.liked == true,
                repeatMode = player.repeatMode,
            )

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

    private fun handlePlaybackEnded() {
        recordTasteSignalForPreviousTransition(completed = true)
        if (autoplayContinuationJob?.isActive == true) return
        autoplayContinuationJob = scope.launch {
            if (!::player.isInitialized || player.hasNextMediaItem()) return@launch
            if (loopLikedSongsIfNeeded()) return@launch
            continueWithAutoplayIfAllowed()
        }
    }

    private fun loopLikedSongsIfNeeded(): Boolean {
        if (currentPlaybackContext.sourceType != PlaybackSourceType.LIKED_SONGS) return false
        if (player.mediaItemCount <= 0) return false
        if (player.repeatMode == Player.REPEAT_MODE_ONE) return true

        val count = player.mediaItemCount
        val currentItems = (0 until count).map { index ->
            player.getMediaItemAt(index).withOriginalVideoIdUri()
        }
        val loopItems = if (currentPlaybackContext.shuffledCollection && currentItems.size > 1) {
            currentItems.shuffled()
        } else {
            currentItems
        }
        val loopIndex = LikedSongsPlaybackPlanner.nextLoopIndex(loopItems.size) ?: return false

        player.setMediaItems(loopItems, loopIndex, 0L)
        player.prepare()
        player.playWhenReady = true
        Timber.tag("OmniTuneContinuation").i(
            "Looped Liked Songs queue: count=${loopItems.size}, shuffled=${currentPlaybackContext.shuffledCollection}",
        )
        return true
    }

    private fun beginTasteWindow(mediaItem: MediaItem?) {
        val meta = mediaItem?.metadata
        val durationMs = meta?.duration
            ?.takeIf { it > 0 }
            ?.let { it * 1000L }
            ?: player.duration.takeIf { it != C.TIME_UNSET && it > 0L }
        tasteSignalRecorder.beginWindow(
            mediaItem = mediaItem,
            sourceType = currentPlaybackContext.sourceType,
            playerDurationMs = durationMs,
        )
    }

    private fun recordTasteSignalForPreviousTransition(completed: Boolean) {
        val result = tasteSignalRecorder.endWindow(
            completed = completed,
            currentPlayerMediaId = player.currentMediaItem?.mediaId,
            playerPositionMs = player.currentPosition,
        ) ?: return
        autoplayContinuation.onTasteWindowResult(result)
    }

    private suspend fun resolveAutoplayCandidate(candidate: AutoplayCandidate): MediaItem? =
        withContext(Dispatchers.IO) {
            val item = candidate.mediaItem.withOriginalVideoIdUri()
            if (StreamUrlResolver.isYouTubeVideoId(item.localConfiguration?.uri)) {
                StreamUrlResolver.resolveMediaItem(item, streamExtractor, downloadUtil, getPlaybackQualityMode())
            } else {
                item
            }
        }

    private suspend fun continueWithAutoplayIfAllowed() {
        autoplayContinuation.continueWithAutoplay(
            playbackContext = currentPlaybackContext,
            hasNextItem = player.hasNextMediaItem(),
            seedFallbacks = listOf(_currentMediaMetadata.value, player.currentMediaItem?.metadata),
            resolve = ::resolveAutoplayCandidate,
            startRadio = { seedTrack, resolved ->
                val meta = resolved.metadata
                playQueue(
                    ListQueue(
                        title = "Autoplay Radio",
                        items = listOf(resolved),
                        playbackContext = PlaybackContext(
                            sourceType = PlaybackSourceType.AUTOPLAY_RADIO,
                            sourceTitle = "Autoplay Radio",
                            seedSongId = seedTrack.id,
                            artist = meta?.artists?.firstOrNull()?.name ?: seedTrack.artists.firstOrNull()?.name,
                            allowAutoplay = true,
                        ),
                    ),
                    playWhenReady = true,
                )
            },
        )
    }

    private fun updateVolumeNormalizationFactor(mediaId: String?) {
        if (mediaId.isNullOrBlank()) {
            _volumeNormalizationFactor.value = 1f
            return
        }
        scope.launch {
            val factor = withContext(Dispatchers.IO) {
                if (!audioNormalizationEnabled.value) return@withContext 1f
                val format = database.format(mediaId).first()
                val loudness = format?.loudnessDb ?: format?.perceptualLoudnessDb ?: return@withContext 1f
                var f = 10f.pow((-loudness.toFloat()) / 20f)
                if (f > 1f) f = min(f, 3.16f)
                f
            }
            _volumeNormalizationFactor.value = factor
        }
    }


    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
        saveQueueState()
    }

    private fun startQueuePositionCheckpoint() {
        if (queuePositionCheckpointJob?.isActive == true) return
        queuePositionCheckpointJob = scope.launch {
            while (isActive) {
                delay(QUEUE_POSITION_CHECKPOINT_MS)
                if (player.isPlaying) saveQueueState()
            }
        }
    }

    private fun stopQueuePositionCheckpoint(saveImmediately: Boolean) {
        queuePositionCheckpointJob?.cancel()
        queuePositionCheckpointJob = null
        if (saveImmediately) saveQueueState(debounceMillis = 0L)
    }

    private fun saveQueueState(debounceMillis: Long = 1_000L) {
        saveQueueJob?.cancel()
        saveQueueJob = scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val prefs = this@MusicService.dataStore.data.first()
                val persistentQueue = prefs[com.omnitune.app.constants.PersistentQueueKey] ?: true
                if (!persistentQueue) return@launch

                kotlinx.coroutines.delay(debounceMillis)

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
                    position = currentPos.coerceAtLeast(0L),
                    playbackSourceType = currentPlaybackContext.sourceType.name,
                    playbackSourceId = currentPlaybackContext.sourceId,
                    playbackSourceTitle = currentPlaybackContext.sourceTitle,
                    playbackSeedSongId = currentPlaybackContext.seedSongId,
                    playbackGenre = currentPlaybackContext.genre,
                    playbackMood = currentPlaybackContext.mood,
                    playbackArtist = currentPlaybackContext.artist,
                    playbackAllowAutoplay = currentPlaybackContext.allowAutoplay,
                    playbackShuffledCollection = currentPlaybackContext.shuffledCollection,
                )
                database.saveQueue(entity)
                Timber.tag("OmniTuneQueue").i("Queue saved: count=$count, index=$currentIndex, pos=$currentPos")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag("MusicService").e(e, "Error saving queue state")
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
