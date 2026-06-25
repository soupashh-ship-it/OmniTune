/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.omnitune.app.ui.player.MiniPlayer
import com.omnitune.app.ui.screens.AlbumScreen
import com.omnitune.app.ui.screens.ArtistScreen
import com.omnitune.app.ui.screens.HistoryScreen
import com.omnitune.app.ui.screens.HomeScreen
import com.omnitune.app.ui.screens.LibraryScreen
import com.omnitune.app.ui.screens.Screens
import com.omnitune.app.ui.screens.Screens.Companion.ROUTE_EQUALIZER
import com.omnitune.app.ui.screens.Screens.Companion.ROUTE_DOWNLOADS
import com.omnitune.app.ui.screens.SearchScreen
import com.omnitune.app.ui.screens.StatsScreen
import com.omnitune.app.ui.screens.EqualizerScreen
import com.omnitune.app.ui.screens.DownloadsScreen
import com.omnitune.app.ui.screens.LibraryArtistsScreen
import com.omnitune.app.ui.screens.LibraryAlbumsScreen
import com.omnitune.app.ui.screens.LibraryPlaylistsScreen
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.content.Context
import androidx.media3.exoplayer.offline.Download
import com.omnitune.app.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.playback.MusicService
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.constants.DynamicThemeKey
import com.omnitune.app.utils.dataStore
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.map
import com.omnitune.innertube.models.SongItem
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.playback.queues.ListQueue
import com.omnitune.app.ui.player.PlayerScreen
import com.omnitune.app.ui.screens.QueueScreen
import com.omnitune.app.ui.screens.LikedSongsScreen
import com.omnitune.app.ui.screens.RecentlyPlayedScreen
import com.omnitune.app.ui.screens.SettingsScreen
import com.omnitune.app.utils.reportException
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTuneTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var database: MusicDatabase
    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    private val serviceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
            val binder = service as? MusicService.MusicBinder ?: return
            if (playerConnection == null) {
                playerConnection = PlayerConnection(this@MainActivity, binder, database, lifecycleScope)
            }
        }
        override fun onServiceDisconnected(name: android.content.ComponentName?) {
        }
    }

    private fun startMusicServiceSafely() { try { startService(Intent(this, MusicService::class.java)) } catch (e: Exception) { reportException(e) } }
    private fun bindToMusicService() {
        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            android.widget.Toast.makeText(this, "Notifications disabled. Media controls won't show in status bar.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
        val lastCrash = prefs.getString("last_crash", null)
        if (BuildConfig.DEBUG && lastCrash != null) {
            prefs.edit().remove("last_crash").commit()
            val scrollView = android.widget.ScrollView(this).apply {
                addView(android.widget.TextView(this@MainActivity).apply {
                    text = "CRASH OCCURRED:\n\n$lastCrash"
                    textSize = 12f
                    setPadding(32, 100, 32, 32)
                    setTextColor(android.graphics.Color.RED)
                    setTextIsSelectable(true)
                })
            }
            setContentView(scrollView)
            return
        } else if (lastCrash != null) {
            prefs.edit().remove("last_crash").apply()
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val context = LocalContext.current
            val dynamicThemeFlow = remember(context) { context.dataStore.data.map { it[DynamicThemeKey] ?: false } }
            val dynamicTheme by dynamicThemeFlow.collectAsState(initial = false)
                
            OmniTuneTheme(dynamicColor = dynamicTheme) {
                CompositionLocalProvider(LocalPlayerConnection provides playerConnection) {
                    OmniShellBackground {
                        OmniTuneMainScreen(database = database)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startMusicServiceSafely()
        bindToMusicService()
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }
}

@Composable
private fun OmniShellBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OmniColors.BackgroundGradient)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            OmniColors.OmniAccentPrimary.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                        radius = 980f,
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            OmniColors.OmniBackgroundBase.copy(alpha = 0.72f),
                        )
                    )
                )
        )
        content()
    }
}

private fun playSongFromSearch(
    context: android.content.Context,
    songs: List<SongItem>,
    index: Int,
    playerConnection: com.omnitune.app.playback.PlayerConnection?,
    onPlayerNotReady: (List<SongItem>, Int) -> Unit,
) {
    val songItem = songs[index]
    Timber.tag("OmniTunePlaybackTrace").i("Search play requested: ${songItem.title} (${songItem.id})")
    val connection = playerConnection ?: run {
        Timber.tag("OmniTunePlaybackTrace").w("Search play queued: player connection not ready")
        onPlayerNotReady(songs, index)
        Toast.makeText(context, "Starting player...", Toast.LENGTH_SHORT).show()
        return
    }
    val mediaItems = songs.map { it.toMediaItem() }
    connection.playQueue(ListQueue(title = "Search Results", items = mediaItems, startIndex = index))
}

@Composable
fun OmniTuneMainScreen(database: MusicDatabase) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val topLevelScreens = Screens.MainScreens.map { it.route }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val localPlayerConnection = LocalPlayerConnection.current
    val currentMediaMetadata by (localPlayerConnection?.mediaMetadata ?: kotlinx.coroutines.flow.flowOf(null))
        .collectAsState(initial = null)
    var pendingSearchQueue by remember { mutableStateOf<Pair<List<SongItem>, Int>?>(null) }
    val showBottomBar = currentRoute in topLevelScreens && currentRoute != "player" && currentRoute != "queue" && currentRoute != "settings"

    LaunchedEffect(localPlayerConnection, pendingSearchQueue) {
        val queueData = pendingSearchQueue
        val connection = localPlayerConnection
        if (queueData != null && connection != null) {
            val (songs, index) = queueData
            Timber.tag("OmniTunePlaybackTrace").i("Playing queued search: ${songs[index].title}")
            pendingSearchQueue = null
            val mediaItems = songs.map { it.toMediaItem() }
            connection.playQueue(ListQueue(title = "Search Results", items = mediaItems, startIndex = index))
        }
    }

    val showMiniPlayer = currentRoute != "player" && currentRoute != "queue" && currentMediaMetadata != null
    val shellBottomPadding = when {
        currentRoute == "player" || currentRoute == "queue" -> 0.dp
        showMiniPlayer && showBottomBar -> 196.dp
        showBottomBar -> 112.dp
        showMiniPlayer -> 104.dp
        else -> 0.dp
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screens.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = shellBottomPadding),
            enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)) },
            exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)) },
            popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)) },
            popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)) }
        ) {
            composable(Screens.Home.route) {
                HomeScreen(onNavigateToSearch = { navController.navigate(Screens.Search.route) }, onNavigateToLibrary = { navController.navigate(Screens.Library.route) },
                    onResumePlayback = { navController.navigate("player") }, onPlaySong = { song -> localPlayerConnection?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) })
            }
            composable(Screens.Stats.route) { StatsScreen() }
            composable(Screens.History.route) { HistoryScreen() }
            composable(Screens.Library.route) {
                LibraryScreen(onNavigateToSearch = { navController.navigate(Screens.Search.route) }, onNavigateToLiked = { navController.navigate("liked_songs") },
                    onNavigateToDownloads = { navController.navigate(ROUTE_DOWNLOADS) },
                    onNavigateToRecentlyPlayed = { navController.navigate("recently_played") },
                    onNavigateToArtists = { navController.navigate("library_artists") },
                    onNavigateToAlbums = { navController.navigate("library_albums") },
                    onNavigateToPlaylists = { navController.navigate("library_playlists") })
            }
            composable("library_artists") {
                LibraryArtistsScreen(onBack = { navController.popBackStack() }, onNavigateToArtist = { navController.navigate("artist/$it") })
            }
            composable("library_albums") {
                LibraryAlbumsScreen(onBack = { navController.popBackStack() }, onNavigateToAlbum = { navController.navigate("album/$it") })
            }
            composable("library_playlists") {
                LibraryPlaylistsScreen(onBack = { navController.popBackStack() }, onNavigateToPlaylist = { Toast.makeText(context, "Playlist details coming soon", Toast.LENGTH_SHORT).show() })
            }
            composable("liked_songs") {
                LikedSongsScreen(onBack = { navController.popBackStack() }, onPlaySong = { song ->
                    localPlayerConnection?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) })
            }
            composable("recently_played") {
                RecentlyPlayedScreen(onBack = { navController.popBackStack() }, onPlaySong = { song ->
                    localPlayerConnection?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) })
            }
            composable(Screens.Search.route) {
                SearchScreen(onBack = { navController.popBackStack() }, onNavigateToAlbum = { navController.navigate("album/$it") },
                    onNavigateToArtist = { navController.navigate("artist/$it") },
                    onPlaySong = { songs, index ->
                        playSongFromSearch(
                            context = context,
                            songs = songs,
                            index = index,
                            playerConnection = localPlayerConnection,
                            onPlayerNotReady = { s, i -> pendingSearchQueue = Pair(s, i) }
                        )
                    },
                    onPlayNext = { song ->
                        val connection = localPlayerConnection
                        if (connection != null) {
                            connection.playNext(song.toMediaItem())
                            Toast.makeText(context, "Added to play next", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Starting player...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onAddToQueue = { song ->
                        val connection = localPlayerConnection
                        if (connection != null) {
                            connection.addToQueue(song.toMediaItem())
                            Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Starting player...", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            composable("album/{albumId}") {
                AlbumScreen(albumId = it.arguments?.getString("albumId") ?: "", onBack = { navController.popBackStack() },
                    onPlaySong = { song -> localPlayerConnection?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) })
            }
            composable("artist/{artistId}") {
                ArtistScreen(artistId = it.arguments?.getString("artistId") ?: "", onBack = { navController.popBackStack() },
                    onPlaySong = { song -> localPlayerConnection?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) },
                    onNavigateToAlbum = { navController.navigate("album/$it") })
            }
            composable("player") { PlayerScreen(playerConnection = LocalPlayerConnection.current, onDismiss = { navController.popBackStack() }, onOpenQueue = { navController.navigate("queue") }) }
            composable("queue") { QueueScreen(playerConnection = LocalPlayerConnection.current, onBack = { navController.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }, onNavigateToEqualizer = { navController.navigate(ROUTE_EQUALIZER) }) }
            composable(ROUTE_DOWNLOADS) {
                DownloadsScreen(
                    onBack = { navController.popBackStack() },
                    onPlayDownload = { download ->
                        val connection = localPlayerConnection
                        if (connection != null) {
                            if (download.state != Download.STATE_COMPLETED) {
                                Toast.makeText(context, "Download is not ready to play.", Toast.LENGTH_SHORT).show()
                            } else {
                                coroutineScope.launch {
                                    val mediaItem = withContext(Dispatchers.IO) {
                                        database.getSongById(download.request.id)?.toMediaItem()
                                            ?: MediaMetadata(
                                                id = download.request.id,
                                                title = String(download.request.data, Charsets.UTF_8)
                                                    .ifBlank { download.request.id },
                                                artists = emptyList(),
                                                duration = -1,
                                            ).toMediaItem()
                                    }
                                    connection.playQueue(ListQueue(title = "Downloads", items = listOf(mediaItem)))
                                    navController.navigate("player")
                                }
                            }
                        } else {
                            Toast.makeText(context, "Starting player...", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            composable(ROUTE_EQUALIZER) {
                EqualizerScreen(
                    onBack = { navController.popBackStack() },
                    onApplyBands = { bands ->
                        localPlayerConnection?.applyEqualizerBands(bands)
                    }
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                .padding(bottom = OmniSpacing.compact),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
        ) {
            if (showMiniPlayer) {
                MiniPlayer(
                    pureBlack = false,
                    playerConnection = localPlayerConnection,
                    onClick = { if (localPlayerConnection != null) navController.navigate("player") }
                )
            }
            if (showBottomBar) GlassBottomDock(currentRoute = currentRoute, onNavigate = { route -> navController.navigate(route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } })
        }
    }
}

@Composable
private fun GlassBottomDock(currentRoute: String?, onNavigate: (String) -> Unit) {
    data class NavItem(val resId: Int, val label: String, val route: String)
    val navItems = listOf(
        NavItem(R.drawable.ic_home, "Home", "home"),
        NavItem(R.drawable.ic_search, "Search", "search"),
        NavItem(R.drawable.ic_list, "Library", "library"),
        NavItem(R.drawable.ic_settings, "Settings", "settings")
    )

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
        .height(76.dp)
        .shadow(18.dp, OmniShapes.Dock, ambientColor = OmniColors.OmniAccentGlow.copy(alpha = 0.28f), spotColor = OmniColors.OmniAccentGlow.copy(alpha = 0.18f))
        .clip(OmniShapes.Dock).border(1.dp, OmniColors.OmniGlassBorderStrong, OmniShapes.Dock)
        .background(Brush.verticalGradient(listOf(OmniColors.OmniGlassDock, OmniColors.OmniBackgroundBase.copy(alpha = 0.9f))))
        .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        navItems.forEach { item ->
            val selected = currentRoute == item.route
            
            val tint by androidx.compose.animation.animateColorAsState(
                targetValue = if (selected) OmniColors.Secondary else OmniColors.TextMuted,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 250),
                label = "color"
            )
            val iconSize by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (selected) 24.dp else 22.dp,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 250),
                label = "size"
            )
            val backgroundAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 250),
                label = "bg_alpha"
            )
            
            Box(modifier = Modifier.weight(if (selected) 1.2f else 1f).clip(RoundedCornerShape(18.dp))
                .clickable(remember { MutableInteractionSource() }, indication = androidx.compose.material3.ripple(bounded = true, color = OmniColors.OmniAccentSecondary.copy(alpha = 0.15f))) { onNavigate(item.route) }
                .then(if (backgroundAlpha > 0f) Modifier.background(Brush.horizontalGradient(listOf(OmniColors.OmniAccentPrimary.copy(alpha = 0.24f * backgroundAlpha), OmniColors.OmniAccentSecondary.copy(alpha = 0.16f * backgroundAlpha)))) else Modifier)
                .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(painterResource(item.resId), contentDescription = item.label, tint = tint, modifier = Modifier.size(iconSize))
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selected,
                        enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(250)) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(250)),
                        exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(250)) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250))
                    ) {
                        Text(item.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tint, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

val LocalPlayerConnection = compositionLocalOf<PlayerConnection?> { null }
