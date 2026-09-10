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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.view.Display
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import com.omnitune.app.constants.ArtworkShapeKey
import com.omnitune.app.constants.ForceMaxRefreshRateKey
import com.omnitune.app.constants.KeepScreenOnKey
import com.omnitune.app.constants.LiquidGlassEnabledKey
import com.omnitune.app.constants.MiniPlayerAlphaKey
import com.omnitune.app.constants.MiniPlayerBlurKey
import com.omnitune.app.constants.MiniPlayerStyleKey
import com.omnitune.app.constants.NavBarAlphaKey
import com.omnitune.app.constants.NavBarBlurKey
import com.omnitune.app.constants.PictureInPictureEnabledKey
import com.omnitune.app.constants.PureBlackKey
import com.omnitune.app.constants.SwipeDownToDismissPlayerKey
import com.omnitune.app.constants.ThemeModeKey
import com.omnitune.app.constants.VolumeSliderEnabledKey
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.extensions.ExtraIsMusicVideo
import com.omnitune.app.models.AppTheme
import com.omnitune.app.models.ArtworkShape
import com.omnitune.app.models.MiniPlayerStyle
import com.omnitune.app.models.PlayerPresentationPreferenceMapper
import com.omnitune.app.models.Song
import com.omnitune.app.models.SongSource
import com.omnitune.app.models.ThemeModePreferenceMapper
import com.omnitune.app.models.toDomainSong
import com.omnitune.app.models.toMediaItem
import com.omnitune.app.pip.PipHelper
import com.omnitune.app.playback.DownloadUtil
import com.omnitune.app.playback.MusicService
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.playback.PlayerProgressState
import com.omnitune.app.playback.VolumeKeyRoutingPolicy
import com.omnitune.app.playback.queues.ListQueue
import com.omnitune.app.ui.component.*
import com.omnitune.app.ui.navigation.Destination
import com.omnitune.app.ui.navigation.LocalRouteChromeInsets
import com.omnitune.app.ui.navigation.NavGraph
import com.omnitune.app.ui.navigation.RouteChromeInsets
import com.omnitune.app.ui.navigation.RouteChromeMode
import com.omnitune.app.ui.navigation.RouteChromePolicy
import com.omnitune.app.ui.player.ExpandablePlayerSheet
import com.omnitune.app.ui.player.PlayerScreen
import com.omnitune.app.ui.theme.OmniTuneTheme
import com.omnitune.app.ui.utils.DeviceFormFactor
import com.omnitune.app.ui.utils.LocalDeviceFormFactor
import com.omnitune.app.ui.utils.rememberDeviceFormFactor
import com.omnitune.app.utils.NetworkMonitor
import com.omnitune.app.utils.DisplayModeCandidate
import com.omnitune.app.utils.DisplayModeSelector
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.reportException
import com.omnitune.app.viewmodels.MainEvent
import com.omnitune.app.viewmodels.MainViewModel
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
    this?.hasRoute<Destination.PoToken>() == true -> Destination.Settings
    this?.hasRoute<Destination.Equalizer>() == true -> Destination.Settings
    this?.hasRoute<Destination.AIEqualizer>() == true -> Destination.Settings
    this?.hasRoute<Destination.Credits>() == true -> Destination.Settings
    this?.hasRoute<Destination.Updater>() == true -> Destination.Settings
    this?.hasRoute<Destination.LastFmLogin>() == true -> Destination.Settings
    else -> Destination.Home
}

private fun NavDestination?.routeChromeMode(): RouteChromeMode = when {
    this?.hasRoute<Destination.YouTubeLogin>() == true -> RouteChromeMode.Immersive
    this?.hasRoute<Destination.LastFmLogin>() == true -> RouteChromeMode.Immersive
    this?.hasRoute<Destination.ImportPlaylist>() == true -> RouteChromeMode.Immersive
    this?.hasRoute<Destination.PickMusic>() == true -> RouteChromeMode.Immersive
    else -> RouteChromeMode.Standard
}

private fun Context.displayNameFor(uri: Uri): String {
    if (uri.scheme.equals("content", ignoreCase = true)) {
        runCatching {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) {
                    cursor.getString(index)?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            }
        }.getOrNull()?.let { return it }
    }

    return uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.takeIf { it.isNotBlank() }
        ?: "Local audio"
}

private fun PlayerConnection.playSingleSong(title: String, song: Song) {
    playQueue(
        ListQueue(
            title = title,
            items = listOf(song.toMediaItem()),
            startIndex = 0
        )
    )
}

private fun Display.Mode.toCandidate(): DisplayModeCandidate =
    DisplayModeCandidate(
        modeId = modeId,
        width = physicalWidth,
        height = physicalHeight,
        refreshRate = refreshRate,
    )

private fun ComponentActivity.applyRefreshRatePreference(forceMaxRefreshRate: Boolean) {
    val preferredModeId = if (forceMaxRefreshRate) {
        val display = window.decorView.display ?: return
        DisplayModeSelector.highestRefreshRateMode(
            currentMode = display.mode.toCandidate(),
            supportedModes = display.supportedModes.map { it.toCandidate() },
        )?.modeId ?: 0
    } else {
        0
    }

    val attributes = window.attributes
    if (attributes.preferredDisplayModeId != preferredModeId) {
        attributes.preferredDisplayModeId = preferredModeId
        window.attributes = attributes
    }
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
    private var musicServiceBound: Boolean = false
    private val mainViewModel: MainViewModel by viewModels()

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
            musicServiceBound = false
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
        try {
            musicServiceBound = bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            musicServiceBound = false
            reportException(e)
        }
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

            val themeMode by remember {
                context.dataStore.data.map { prefs ->
                    ThemeModePreferenceMapper.resolveMode(
                        themeModeValue = prefs[ThemeModeKey],
                        legacyDarkModeValue = prefs[DarkModeKey],
                    )
                }
            }.collectAsStateWithLifecycle(initialValue = ThemeModePreferenceMapper.DefaultMode)

            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = ThemeModePreferenceMapper.isDarkTheme(themeMode, isSystemDark)

            val currentMetadata by remember(playerConnection) {
                playerConnection?.mediaMetadata ?: flowOf(null)
            }.collectAsStateWithLifecycle(initialValue = null)

            val isPlayingState by remember(playerConnection) {
                playerConnection?.isPlaying ?: flowOf(false)
            }.collectAsStateWithLifecycle(initialValue = false)

            val pipEnabled by remember {
                context.dataStore.data.map { it[PictureInPictureEnabledKey] ?: true }
            }.collectAsStateWithLifecycle(initialValue = true)

            val volumeSliderEnabled by remember {
                context.dataStore.data.map { it[VolumeSliderEnabledKey] ?: true }
            }.collectAsStateWithLifecycle(initialValue = true)

            val forceMaxRefreshRate by remember {
                context.dataStore.data.map { it[ForceMaxRefreshRateKey] ?: false }
            }.collectAsStateWithLifecycle(initialValue = false)

            LaunchedEffect(forceMaxRefreshRate) {
                applyRefreshRatePreference(forceMaxRefreshRate)
            }

            DisposableEffect(Unit) {
                onDispose { applyRefreshRatePreference(false) }
            }

            val currentThumbnailUrl = currentMetadata?.thumbnailUrl
            val currentDomainSong = remember(currentMetadata) {
                currentMetadata?.toDomainSong()
            }

            LaunchedEffect(playerConnection, currentMetadata?.id, isPlayingState, pipEnabled, volumeSliderEnabled) {
                isSongPlaying = isPlayingState
                isPipEnabled = pipEnabled
                isVolumeSliderEnabled = volumeSliderEnabled
                pipHelper.updatePipParams(
                    activity = this@MainActivity,
                    isPlaying = isPlayingState,
                    isVideoMode = currentMediaItemIsVideo(),
                    isPipEnabled = pipEnabled,
                )
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
                        mainViewModel = mainViewModel,
                        networkMonitor = networkMonitor,
                        playerConnection = playerConnection,
                        currentSong = currentDomainSong,
                        initialIntent = this@MainActivity.intent,
                        isPlaying = isPlayingState,
                        volumeKeyEvents = _volumeKeyEvents,
                        volumeSliderEnabled = volumeSliderEnabled,
                        dominantColors = albumArtColors,
                        formFactor = formFactor
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainViewModel.handleIntent(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!VolumeKeyRoutingPolicy.shouldIntercept(isSongPlaying, isVolumeSliderEnabled, keyCode)) {
            return super.onKeyDown(keyCode, event)
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                _volumeKeyEvents.tryEmit(Unit)
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                _volumeKeyEvents.tryEmit(Unit)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        playerConnection?.let { conn ->
            val hasSong = conn.player.currentMediaItem != null
            val isVideoMode = currentMediaItemIsVideo()
            pipHelper.enterPipIfEligible(
                activity = this,
                hasSong = hasSong,
                isPlaying = isSongPlaying,
                isVideoMode = isVideoMode,
                isPipEnabled = isPipEnabled
            )
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        mainViewModel.setPictureInPictureMode(isInPictureInPictureMode)
    }

    private fun currentMediaItemIsVideo(): Boolean =
        playerConnection
            ?.player
            ?.currentMediaItem
            ?.mediaMetadata
            ?.extras
            ?.getBoolean(ExtraIsMusicVideo, false) == true

    override fun onStop() {
        super.onStop()
        if (musicServiceBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                reportException(e)
            } finally {
                musicServiceBound = false
            }
        }
    }

    override fun onDestroy() {
        playerConnection?.dispose()
        playerConnection = null
        super.onDestroy()
    }
}

@Composable
private fun OmniTuneAppRoot(
    mainViewModel: MainViewModel,
    networkMonitor: NetworkMonitor,
    playerConnection: PlayerConnection?,
    currentSong: Song?,
    initialIntent: Intent?,
    isPlaying: Boolean,
    volumeKeyEvents: SharedFlow<Unit>,
    volumeSliderEnabled: Boolean,
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
    val routeChromeMode = remember(navBackStackEntry) {
        navBackStackEntry?.destination.routeChromeMode()
    }

    val isOnline by networkMonitor.isConnected.collectAsStateWithLifecycle(initialValue = true)

    var isPlayerExpanded by remember { mutableStateOf(false) }
    var isMiniPlayerDismissed by remember { mutableStateOf(false) }
    var pendingPlaybackEvent by remember { mutableStateOf<MainEvent?>(null) }
    val latestPlayerConnection by rememberUpdatedState(playerConnection)

    val onboardingCompleted by remember {
        context.dataStore.data.map { it[OnboardingCompletedKey] ?: false }
    }.collectAsStateWithLifecycle(initialValue = true)

    var showWelcomeDialog by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }

    val miniPlayerStyleName by remember {
        context.dataStore.data.map {
            PlayerPresentationPreferenceMapper.resolveMiniPlayerStyle(it[MiniPlayerStyleKey]).name
        }
    }.collectAsStateWithLifecycle(
        initialValue = PlayerPresentationPreferenceMapper.DefaultMiniPlayerStyle.name
    )

    val miniPlayerAlpha by remember {
        context.dataStore.data.map { it[MiniPlayerAlphaKey] ?: 0f }
    }.collectAsStateWithLifecycle(initialValue = 0f)

    val miniPlayerBlur by remember {
        context.dataStore.data.map { it[MiniPlayerBlurKey] ?: 50f }
    }.collectAsStateWithLifecycle(initialValue = 50f)

    val miniPlayerArtworkShape by remember {
        context.dataStore.data.map { prefs ->
            val savedShape = prefs[ArtworkShapeKey] ?: ArtworkShape.ROUNDED_SQUARE.name
            PlayerPresentationPreferenceMapper.resolveArtworkShape(savedShape).name
        }
    }.collectAsStateWithLifecycle(
        initialValue = PlayerPresentationPreferenceMapper.DefaultArtworkShape.name
    )

    val iosLiquidGlassEnabled by remember {
        context.dataStore.data.map { it[LiquidGlassEnabledKey] ?: true }
    }.collectAsStateWithLifecycle(initialValue = true)

    val navBarAlpha by remember {
        context.dataStore.data.map { it[NavBarAlphaKey] ?: 1f }
    }.collectAsStateWithLifecycle(initialValue = 1f)

    val navBarBlur by remember {
        context.dataStore.data.map { it[NavBarBlurKey] ?: 60f }
    }.collectAsStateWithLifecycle(initialValue = 60f)

    val swipeDownToDismissPlayer by remember {
        context.dataStore.data.map { it[SwipeDownToDismissPlayerKey] ?: true }
    }.collectAsStateWithLifecycle(initialValue = true)

    val miniPlayerStyle = remember(miniPlayerStyleName) {
        PlayerPresentationPreferenceMapper.resolveMiniPlayerStyle(miniPlayerStyleName)
    }

    val keepScreenOnEnabled by remember {
        context.dataStore.data.map { it[KeepScreenOnKey] ?: false }
    }.collectAsStateWithLifecycle(initialValue = false)

    fun playIncomingEvent(event: MainEvent): Boolean {
        val connection = latestPlayerConnection ?: return false
        when (event) {
            is MainEvent.PlayFromDeepLink -> {
                val song = Song(
                    id = event.videoId,
                    title = "YouTube link",
                    artist = "YouTube Music",
                    source = SongSource.YOUTUBE
                )
                connection.playSingleSong("YouTube link", song)
            }
            is MainEvent.PlayFromLocalUri -> {
                val title = context.displayNameFor(event.uri)
                val song = Song(
                    id = event.uri.toString(),
                    title = title,
                    artist = "Local file",
                    source = SongSource.LOCAL,
                    localUri = event.uri.toString()
                )
                connection.playSingleSong(title, song)
            }
            else -> return true
        }
        isMiniPlayerDismissed = false
        return true
    }

    LaunchedEffect(mainViewModel) {
        mainViewModel.events.collect { event ->
            when (event) {
                is MainEvent.PlayFromDeepLink,
                is MainEvent.PlayFromLocalUri -> {
                    if (!playIncomingEvent(event)) {
                        pendingPlaybackEvent = event
                    }
                }
                is MainEvent.NavigateToPlaylist -> {
                    navController.navigate(Destination.Playlist(playlistId = event.playlistId))
                }
                is MainEvent.NavigateToAlbum -> {
                    navController.navigate(Destination.Album(albumId = event.browseId))
                }
                is MainEvent.NavigateToArtist -> {
                    navController.navigate(Destination.Artist(event.channelId))
                }
                is MainEvent.NavigateToSearch -> {
                    navController.navigate(Destination.Search) {
                        launchSingleTop = true
                    }
                }
                is MainEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(initialIntent) {
        mainViewModel.handleIntent(initialIntent)
    }

    LaunchedEffect(playerConnection, pendingPlaybackEvent) {
        val pending = pendingPlaybackEvent ?: return@LaunchedEffect
        if (playIncomingEvent(pending)) {
            pendingPlaybackEvent = null
        }
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

    LaunchedEffect(playerConnection, currentSong?.id, isPlaying) {
        isMiniPlayerDismissed = false
    }

    KeepScreenOnEffect(keepScreenOnEnabled && isPlayerExpanded && currentSong != null)

    val density = LocalDensity.current
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isImeVisible = imeBottomPadding > 0.dp
    val routeChromeLayout = RouteChromePolicy.resolve(
        routeMode = routeChromeMode,
        formFactor = formFactor,
        hasCurrentSong = currentSong != null,
        isMiniPlayerDismissed = isMiniPlayerDismissed,
        isPlayerExpanded = isPlayerExpanded,
        isBlockingOverlayVisible = showWelcomeDialog,
        isImeVisible = isImeVisible,
    )
    val showMiniPlayer = routeChromeLayout.showPlayerSheet
    val navBarHeight = routeChromeLayout.miniPlayerBottomPaddingDp.dp
    val bottomPaddingPx = with(density) { navBarPadding.toPx() + navBarHeight.toPx() }
    val snackbarBottomPadding = if (isImeVisible) {
        imeBottomPadding + RouteChromePolicy.SnackbarMarginDp.dp
    } else {
        navBarPadding + routeChromeLayout.snackbarBottomInsetDp.dp
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (routeChromeLayout.showBottomNavigation) {
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
                        alpha = navBarAlpha,
                        iosLiquidGlassEnabled = iosLiquidGlassEnabled,
                        backgroundColor = navBarColor,
                        iosNavBarBlur = navBarBlur
                    )
                }
            }
        ) { innerPadding ->
            val contentBottomPadding = if (innerPadding.calculateBottomPadding() > 0.dp) {
                routeChromeLayout.contentBottomInsetDp.dp
            } else {
                navBarPadding + routeChromeLayout.contentBottomInsetDp.dp
            }
            val routeChromeInsets = RouteChromeInsets(
                contentBottomPadding = contentBottomPadding,
                snackbarBottomPadding = snackbarBottomPadding,
                imeVisible = isImeVisible,
            )
            Row(modifier = Modifier.fillMaxSize()) {
                if (routeChromeLayout.showNavigationRail) {
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
                        .padding(innerPadding)
                ) {
                    CompositionLocalProvider(LocalRouteChromeInsets provides routeChromeInsets) {
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
        currentSong?.takeIf { showMiniPlayer }?.let { miniSong ->
            MiniPlayerSheetOverlay(
                currentSong = miniSong,
                isPlaying = isPlaying,
                playerConnection = playerConnection,
                volumeKeyEvents = volumeKeyEvents,
                volumeSliderEnabled = volumeSliderEnabled,
                dominantColors = dominantColors,
                bottomPadding = bottomPaddingPx,
                isExpanded = isPlayerExpanded,
                onExpandChange = { isPlayerExpanded = it },
                onClose = {
                    playerConnection?.player?.stop()
                    isMiniPlayerDismissed = true
                },
                style = miniPlayerStyle,
                userAlpha = miniPlayerAlpha,
                artworkShape = miniPlayerArtworkShape,
                glassBlurAmount = miniPlayerBlur,
                swipeDownToDismissEnabled = swipeDownToDismissPlayer,
                onOpenAIEqualizer = {
                    isPlayerExpanded = false
                    navController.navigate(Destination.AIEqualizer) {
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun KeepScreenOnEffect(enabled: Boolean) {
    val activity = LocalActivity.current
    var applied by remember(activity) { mutableStateOf(false) }

    LaunchedEffect(activity, enabled) {
        activity ?: return@LaunchedEffect
        when {
            enabled && !applied -> {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                applied = true
            }
            !enabled && applied -> {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                applied = false
            }
        }
    }

    DisposableEffect(activity) {
        onDispose {
            if (activity != null && applied) {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                applied = false
            }
        }
    }
}

@Composable
private fun MiniPlayerSheetOverlay(
    currentSong: Song,
    isPlaying: Boolean,
    playerConnection: PlayerConnection?,
    volumeKeyEvents: SharedFlow<Unit>,
    volumeSliderEnabled: Boolean,
    dominantColors: DominantColors,
    bottomPadding: Float,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    style: MiniPlayerStyle,
    userAlpha: Float,
    artworkShape: String,
    glassBlurAmount: Float,
    swipeDownToDismissEnabled: Boolean,
    onOpenAIEqualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialProgress = remember(currentSong.id, currentSong.duration) {
        PlayerProgressState(durationMs = currentSong.duration)
    }
    val progressState by remember(playerConnection, currentSong.id, currentSong.duration) {
        playerConnection?.progressState ?: flowOf(initialProgress)
    }.collectAsStateWithLifecycle(initialValue = initialProgress)
    val latestProgressState by rememberUpdatedState(progressState)
    val progressProvider = remember {
        { latestProgressState.progress }
    }

    ExpandablePlayerSheet(
        currentSong = currentSong,
        isPlaying = isPlaying,
        isLoading = false,
        progressProvider = progressProvider,
        dominantColors = dominantColors,
        onPlayPause = {
            playerConnection?.let { conn ->
                if (isPlaying) conn.player.pause() else conn.player.play()
            }
        },
        onNext = { playerConnection?.player?.seekToNextMediaItem() },
        onPrevious = { playerConnection?.player?.seekToPreviousMediaItem() },
        onClose = onClose,
        bottomPadding = bottomPadding,
        isExpanded = isExpanded,
        onExpandChange = onExpandChange,
        style = style,
        userAlpha = userAlpha,
        artworkShape = artworkShape,
        glassBlurAmount = glassBlurAmount,
        swipeDownToDismissEnabled = swipeDownToDismissEnabled,
        modifier = modifier,
        expandedContent = { onCollapse ->
            PlayerScreen(
                playerConnection = playerConnection,
                onDismiss = onCollapse,
                onOpenAIEqualizer = onOpenAIEqualizer,
                volumeKeyEvents = volumeKeyEvents,
                volumeSliderEnabled = volumeSliderEnabled
            )
        }
    )
}
