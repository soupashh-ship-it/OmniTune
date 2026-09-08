/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.omnitune.app.constants.DarkModeKey
import com.omnitune.app.constants.DynamicSongColorsKey
import com.omnitune.app.constants.DynamicThemeKey
import com.omnitune.app.constants.MiniPlayerStyleKey
import com.omnitune.app.constants.PureBlackKey
import com.omnitune.app.constants.SwipeDownToDismissPlayerKey
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.models.AppTheme
import com.omnitune.app.models.MiniPlayerStyle
import com.omnitune.app.models.Song
import com.omnitune.app.models.toDomainSong
import com.omnitune.app.models.toMediaItem
import com.omnitune.app.pip.PipHelper
import com.omnitune.app.playback.DownloadUtil
import com.omnitune.app.playback.MusicService
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.playback.queues.ListQueue
import com.omnitune.app.ui.component.*
import com.omnitune.app.ui.navigation.Destination
import com.omnitune.app.ui.navigation.NavGraph
import com.omnitune.app.ui.player.ExpandablePlayerSheet
import com.omnitune.app.ui.player.PlayerScreen
import com.omnitune.app.ui.theme.OmniTuneTheme
import com.omnitune.app.ui.utils.DeviceFormFactor
import com.omnitune.app.ui.utils.LocalDeviceFormFactor
import com.omnitune.app.ui.utils.rememberDeviceFormFactor
import com.omnitune.app.utils.NetworkMonitor
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.reportException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.omnitune.app.constants.AppThemeKey

private val OnboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
private val WhatsNewSeenVersionKey = intPreferencesKey("whats_new_seen_version")

private fun NavDestination?.topLevelDestination(): Destination = when {
    this?.hasRoute<Destination.Search>() == true -> Destination.Search
    this?.hasRoute<Destination.Library>() == true -> Destination.Library
    this?.hasRoute<Destination.Downloads>() == true -> Destination.Library
    this?.hasRoute<Destination.Settings>() == true -> Destination.Settings
    this?.hasRoute<Destination.PlaybackSettings>() == true -> Destination.Settings
    this?.hasRoute<Destination.AppearanceSettings>() == true -> Destination.Settings
    this?.hasRoute<Destination.CustomizationSettings>() == true -> Destination.Settings
    this?.hasRoute<Destination.ArtworkShapeSettings>() == true -> Destination.Settings
    this?.hasRoute<Destination.SeekbarStyleSettings>() == true -> Destination.Settings
    this?.hasRoute<Destination.ArtworkSizeSettings>() == true -> Destination.Settings
    this?.hasRoute<Destination.About>() == true -> Destination.Settings
    this?.hasRoute<Destination.Storage>() == true -> Destination.Settings
    this?.hasRoute<Destination.ListeningStats>() == true -> Destination.Settings
    this?.hasRoute<Destination.Wrapped>() == true -> Destination.Settings
    this?.hasRoute<Destination.PlayerCache>() == true -> Destination.Settings
    this?.hasRoute<Destination.HowItWorks>() == true -> Destination.Settings
    this?.hasRoute<Destination.Support>() == true -> Destination.Settings
    this?.hasRoute<Destination.Misc>() == true -> Destination.Settings
    this?.hasRoute<Destination.LyricsProviders>() == true -> Destination.Settings
    this?.hasRoute<Destination.SponsorBlockSettings>() == true -> Destination.Settings
    this?.hasRoute<Destination.DiscordSettings>() == true -> Destination.Settings
    this?.hasRoute<Destination.PoToken>() == true -> Destination.Settings
    this?.hasRoute<Destination.Equalizer>() == true -> Destination.Settings
    this?.hasRoute<Destination.AIEqualizer>() == true -> Destination.Settings
    this?.hasRoute<Destination.AISettings>() == true -> Destination.Settings
    this?.hasRoute<Destination.Credits>() == true -> Destination.Settings
    this?.hasRoute<Destination.Updater>() == true -> Destination.Settings
    this?.hasRoute<Destination.LastFmLogin>() == true -> Destination.Settings
    else -> Destination.Home
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var database: MusicDatabase
    @Inject lateinit var downloadUtil: DownloadUtil
    @Inject lateinit var networkMonitor: NetworkMonitor
    @Inject lateinit var pipHelper: PipHelper

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private lateinit var audioManager: AudioManager

    private var isSongPlaying: Boolean = false
    private var isVolumeSliderEnabled: Boolean = true
    private var isPipEnabled: Boolean = true

    private val _volumeKeyEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicService.MusicBinder ?: return
            if (playerConnection == null) {
                playerConnection = PlayerConnection(this@MainActivity, binder, database, lifecycleScope)
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    private fun startMusicServiceSafely() {
        try {
            startService(Intent(this, MusicService::class.java))
        } catch (e: Exception) {
            reportException(e)
        }
    }

    private fun bindToMusicService() {
        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Notifications disabled. Playback controls won't show in status bar.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onStart() {
        super.onStart()
        startMusicServiceSafely()
        bindToMusicService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            val dynamicColor by remember {
                context.dataStore.data.map { it[DynamicThemeKey] ?: true }
            }.collectAsStateWithLifecycle(initialValue = true)

            val dynamicSongColors by remember {
                context.dataStore.data.map { it[DynamicSongColorsKey] ?: true }
            }.collectAsStateWithLifecycle(initialValue = true)

            val pureBlack by remember {
                context.dataStore.data.map { it[PureBlackKey] ?: false }
            }.collectAsStateWithLifecycle(initialValue = false)

            val appThemeStr by remember {
                context.dataStore.data.map { it[AppThemeKey] ?: AppTheme.DEFAULT.name }
            }.collectAsStateWithLifecycle(initialValue = AppTheme.DEFAULT.name)

            val appTheme = remember(appThemeStr) {
                try { AppTheme.valueOf(appThemeStr) } catch (_: Exception) { AppTheme.DEFAULT }
            }

            val darkModePref by remember {
                context.dataStore.data.map { it[DarkModeKey] ?: "ON" }
            }.collectAsStateWithLifecycle(initialValue = "ON")

            val isDark = isSystemInDarkTheme()
            val darkTheme = remember(darkModePref, isDark) {
                when (darkModePref) {
                    "OFF" -> false
                    "AUTO" -> isDark
                    else -> true
                }
            }

            val currentMetadata by remember(playerConnection) {
                playerConnection?.mediaMetadata ?: flowOf(null)
            }.collectAsStateWithLifecycle(initialValue = null)

            val isPlayingState by remember(playerConnection) {
                playerConnection?.isPlaying ?: flowOf(false)
            }.collectAsStateWithLifecycle(initialValue = false)

            val currentThumbnailUrl = currentMetadata?.thumbnailUrl
            val currentDomainSong = remember(currentMetadata) {
                currentMetadata?.toDomainSong()
            }

            LaunchedEffect(isPlayingState) {
                isSongPlaying = isPlayingState
            }

            val albumArtColors = rememberDominantColors(
                imageUrl = currentThumbnailUrl,
                isDarkTheme = darkTheme
            )

            OmniTuneTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
                pureBlack = pureBlack,
                appTheme = appTheme,
                albumArtColors = if (dynamicSongColors && currentThumbnailUrl != null) albumArtColors else null
            ) {
                val foldingFeature by produceState<FoldingFeature?>(null) {
                    WindowInfoTracker.getOrCreate(this@MainActivity)
                        .windowLayoutInfo(this@MainActivity)
                        .collect { info ->
                            value = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                        }
                }

                val formFactor = rememberDeviceFormFactor(foldingFeature = foldingFeature)

                CompositionLocalProvider(
                    LocalDeviceFormFactor provides formFactor,
                    LocalPlayerConnection provides playerConnection,
                    LocalDatabase provides database,
                    LocalDownloadUtil provides downloadUtil,
                ) {
                    OmniTuneAppRoot(
                        networkMonitor = networkMonitor,
                        playerConnection = playerConnection,
                        currentSong = currentDomainSong,
                        isPlaying = isPlayingState,
                        volumeKeyEvents = _volumeKeyEvents,
                        dominantColors = albumArtColors,
                        formFactor = formFactor
                    )
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!isSongPlaying || !isVolumeSliderEnabled) {
            return super.dispatchKeyEvent(event)
        }

        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                    _volumeKeyEvents.tryEmit(Unit)
                }
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                    _volumeKeyEvents.tryEmit(Unit)
                }
                true
            }
            else -> super.dispatchKeyEvent(event)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        playerConnection?.let { conn ->
            val hasSong = conn.player.currentMediaItem != null
            pipHelper.enterPipIfEligible(
                activity = this,
                hasSong = hasSong,
                isPlaying = isSongPlaying,
                isPipEnabled = isPipEnabled
            )
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onStop() {
        super.onStop()
        try { unbindService(serviceConnection) } catch (_: Exception) {}
    }

    override fun onDestroy() {
        playerConnection?.dispose()
        playerConnection = null
        super.onDestroy()
    }
}

@Composable
private fun OmniTuneAppRoot(
    networkMonitor: NetworkMonitor,
    playerConnection: PlayerConnection?,
    currentSong: Song?,
    isPlaying: Boolean,
    volumeKeyEvents: SharedFlow<Unit>,
    dominantColors: DominantColors,
    formFactor: DeviceFormFactor
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = remember(navBackStackEntry) {
        navBackStackEntry?.destination.topLevelDestination()
    }

    val isOnline by networkMonitor.isConnected.collectAsStateWithLifecycle(initialValue = true)

    var isPlayerExpanded by remember { mutableStateOf(false) }
    var isMiniPlayerDismissed by remember { mutableStateOf(false) }
    var currentPosition by remember(playerConnection) { mutableLongStateOf(0L) }
    var currentDuration by remember(playerConnection, currentSong?.id) {
        mutableLongStateOf(currentSong?.duration ?: 0L)
    }

    val onboardingCompleted by remember {
        context.dataStore.data.map { it[OnboardingCompletedKey] ?: false }
    }.collectAsStateWithLifecycle(initialValue = true)

    var showWelcomeDialog by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }

    val miniPlayerStyleName by remember {
        context.dataStore.data.map { it[MiniPlayerStyleKey] ?: MiniPlayerStyle.YT_MUSIC.name }
    }.collectAsStateWithLifecycle(initialValue = MiniPlayerStyle.YT_MUSIC.name)

    val swipeDownToDismissPlayer by remember {
        context.dataStore.data.map { it[SwipeDownToDismissPlayerKey] ?: true }
    }.collectAsStateWithLifecycle(initialValue = true)

    val miniPlayerStyle = remember(miniPlayerStyleName) {
        try { MiniPlayerStyle.valueOf(miniPlayerStyleName) } catch (_: Exception) { MiniPlayerStyle.YT_MUSIC }
    }

    LaunchedEffect(Unit) {
        val currentVersion = BuildConfig.VERSION_CODE
        val isCompleted = context.dataStore.data.map { it[OnboardingCompletedKey] ?: false }.first()
        val seenVersion = context.dataStore.data.map { it[WhatsNewSeenVersionKey] ?: 0 }.first()

        if (!isCompleted) {
            showWelcomeDialog = true
            context.dataStore.edit { it[WhatsNewSeenVersionKey] = currentVersion }
        } else if (seenVersion < currentVersion) {
            showWhatsNew = true
        }
    }

    if (showWelcomeDialog) {
        WelcomeOnboardingDialog(
            onLoginClick = {
                showWelcomeDialog = false
                navController.navigate(Destination.YouTubeLogin)
            },
            onContinueAsGuest = {
                showWelcomeDialog = false
                scope.launch {
                    context.dataStore.edit { it[OnboardingCompletedKey] = true }
                }
            }
        )
    }

    if (showWhatsNew) {
        WhatsNewDialog(
            versionLabel = "Version ${BuildConfig.VERSION_NAME}",
            onDismiss = {
                showWhatsNew = false
                scope.launch {
                    context.dataStore.edit { it[WhatsNewSeenVersionKey] = BuildConfig.VERSION_CODE }
                }
            }
        )
    }

    val hasSong = currentSong != null
    val showMiniPlayer = !isMiniPlayerDismissed && hasSong
    val showBottomNav = !isPlayerExpanded
    val miniPlayerProgressProvider: () -> Float = {
        if (currentDuration > 0L) {
            (currentPosition.toFloat() / currentDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    LaunchedEffect(playerConnection, currentSong?.id, isPlaying) {
        isMiniPlayerDismissed = false
        while (true) {
            val connection = playerConnection
            if (connection == null) {
                currentPosition = 0L
                currentDuration = currentSong?.duration ?: 0L
            } else {
                currentPosition = connection.currentPosition.coerceAtLeast(0L)
                currentDuration = connection.duration
                    .takeIf { it > 0L }
                    ?: currentSong?.duration
                    ?: 0L
            }
            kotlinx.coroutines.delay(500L)
        }
    }

    val density = LocalDensity.current
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBarHeight = if (showBottomNav && formFactor != DeviceFormFactor.TV) 80.dp else 0.dp
    val bottomPaddingPx = with(density) { navBarPadding.toPx() + navBarHeight.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomNav && formFactor.isPhoneLike) {
                    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val navBarColor = if (showMiniPlayer) {
                        if (miniPlayerStyle == MiniPlayerStyle.YT_MUSIC && isDarkTheme) {
                            lerp(dominantColors.primary, Color.Black, 0.45f)
                        } else {
                            dominantColors.primary
                        }
                    } else {
                        MaterialTheme.colorScheme.surface
                    }

                    ExpressiveBottomNav(
                        currentDestination = currentDestination,
                        onDestinationChange = { dest ->
                            navController.navigate(dest) {
                                popUpTo<Destination.Home> { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        backgroundColor = navBarColor
                    )
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize()) {
                if (showBottomNav && !formFactor.isPhoneLike) {
                    when {
                        formFactor == DeviceFormFactor.TV -> {
                            TvNavigationRail(
                                currentDestination = currentDestination,
                                onDestinationChange = { dest ->
                                    navController.navigate(dest) {
                                        popUpTo<Destination.Home> { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        formFactor.isTabletLike -> {
                            AdaptiveNavigationRail(
                                currentDestination = currentDestination,
                                onDestinationChange = { dest ->
                                    navController.navigate(dest) {
                                        popUpTo<Destination.Home> { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        else -> {}
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(bottom = 0.dp)
                ) {
                    NavGraph(
                        navController = navController,
                        onPlaySong = { songs: List<Song>, index: Int ->
                            playerConnection?.let { conn ->
                                val mediaItems = songs.map { it.toMediaItem() }
                                conn.playQueue(
                                    ListQueue(
                                        title = songs.getOrNull(index)?.title ?: "Playing",
                                        items = mediaItems,
                                        startIndex = index
                                    )
                                )
                            }
                        },
                        onPlayPause = {
                            playerConnection?.let { conn ->
                                if (isPlaying) conn.player.pause() else conn.player.play()
                            }
                        },
                        onSeekTo = { pos: Long ->
                            playerConnection?.player?.seekTo(pos)
                        },
                        onNext = {
                            playerConnection?.player?.seekToNextMediaItem()
                        },
                        onPrevious = {
                            playerConnection?.player?.seekToPreviousMediaItem()
                        },
                        onStartRadio = {
                            playerConnection?.startRadioSeamlessly()
                        },
                        onApplyEqualizerBands = { bands ->
                            playerConnection?.applyEqualizerBands(bands)
                        },
                        onSetEqualizerEnabled = { enabled ->
                            playerConnection?.setEqualizerEnabled(enabled)
                        },
                        currentSong = currentSong,
                        dominantColors = dominantColors
                    )
                }
            }
        }

        // Offline banner
        AnimatedVisibility(
            visible = !isOnline,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.msg_offline_banner),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        }

        // Expandable Player Sheet overlay
        if (showMiniPlayer) {
            ExpandablePlayerSheet(
                currentSong = currentSong,
                isPlaying = isPlaying,
                isLoading = false,
                progressProvider = miniPlayerProgressProvider,
                dominantColors = dominantColors,
                onPlayPause = {
                    playerConnection?.let { conn ->
                        if (isPlaying) conn.player.pause() else conn.player.play()
                    }
                },
                onNext = { playerConnection?.player?.seekToNextMediaItem() },
                onPrevious = { playerConnection?.player?.seekToPreviousMediaItem() },
                onClose = {
                    playerConnection?.player?.stop()
                    isMiniPlayerDismissed = true
                },
                bottomPadding = bottomPaddingPx,
                isExpanded = isPlayerExpanded,
                onExpandChange = { isPlayerExpanded = it },
                style = miniPlayerStyle,
                swipeDownToDismissEnabled = swipeDownToDismissPlayer,
                modifier = Modifier.align(Alignment.BottomCenter),
                expandedContent = { onCollapse ->
                    PlayerScreen(
                        playerConnection = playerConnection,
                        onDismiss = onCollapse,
                        volumeKeyEvents = volumeKeyEvents
                    )
                }
            )
        }
    }
}
