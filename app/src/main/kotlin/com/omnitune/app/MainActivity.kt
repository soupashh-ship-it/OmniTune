package com.omnitune.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
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
import com.omnitune.app.ui.screens.SearchScreen
import com.omnitune.app.ui.screens.StatsScreen
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.playback.MusicService
import com.omnitune.app.db.entities.Song
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.innertube.models.SongItem
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.playback.queues.ListQueue
import com.omnitune.app.ui.player.PlayerScreen
import com.omnitune.app.ui.screens.QueueScreen
import com.omnitune.app.utils.reportException
import javax.inject.Inject
import com.omnitune.app.ui.theme.OmniTuneTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    private fun startMusicServiceSafely() {
        try {
            startService(Intent(this, MusicService::class.java))
        } catch (e: Exception) {
            reportException(e)
        }
    }

    private fun bindToMusicService() {
        val service = MusicService.instance ?: return
        if (playerConnection == null) {
            playerConnection =
                PlayerConnection(this@MainActivity, service.binder(), database, lifecycleScope)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OmniTuneTheme {
                CompositionLocalProvider(LocalPlayerConnection provides playerConnection) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        OmniTuneMainScreen()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startMusicServiceSafely()
        lifecycleScope.launch {
            while (MusicService.instance == null) {
                delay(100)
            }
            bindToMusicService()
        }
    }

    override fun onStop() {
        super.onStop()
        // Don't dispose playerConnection — music must keep playing when app backgrounds
    }
}

private fun playSongFromSearch(context: android.content.Context, songItem: SongItem) {
    val service = MusicService.instance
    if (service == null) {
        Toast.makeText(context, "MusicService not ready", Toast.LENGTH_SHORT).show()
        return
    }
    // Use the existing extension which properly sets tag (MediaMetadata) + artwork URI
    service.playQueue(ListQueue(items = listOf(songItem.toMediaItem())))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniTuneMainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val topLevelScreens = Screens.MainScreens.map { it.route }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            if (currentRoute in topLevelScreens) {
                TopAppBar(
                    title = { Text("OmniTune") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    actions = {
                        IconButton(onClick = {
                            navController.navigate(Screens.Search.route)
                        }) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_search),
                                contentDescription = "Search",
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            Column {
                val localPlayerConnection = LocalPlayerConnection.current

                // MiniPlayer above nav bar — show on ALL screens when music is playing
                if (currentRoute != "player" && currentRoute != "queue") {
                    Box(
                        modifier = Modifier.clickable(
                            onClick = {
                                if (localPlayerConnection != null) {
                                    navController.navigate("player")
                                }
                            }
                        )
                    ) {
                        MiniPlayer(
                            pureBlack = false,
                            playerConnection = localPlayerConnection,
                        )
                    }
                }

                // Navigation bar only on top-level screens
                if (currentRoute in topLevelScreens) {
                    NavigationBar {
                        Screens.MainScreens.forEach { screen ->
                            val selected = navBackStackEntry?.destination?.hierarchy?.any {
                                it.route == screen.route
                            } == true

                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(
                                            when (screen.route) {
                                                "home" -> android.R.drawable.ic_menu_myplaces
                                                "stats" -> android.R.drawable.ic_menu_sort_by_size
                                                "history" -> android.R.drawable.ic_menu_recent_history
                                                "library" -> android.R.drawable.ic_menu_gallery
                                                else -> android.R.drawable.ic_menu_myplaces
                                            }
                                        ),
                                        contentDescription = screen.route.replaceFirstChar { it.uppercase() },
                                    )
                                },
                                label = { Text(screen.route.replaceFirstChar { it.uppercase() }) },
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screens.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screens.Home.route) {
                HomeScreen(
                    onNavigateToSearch = {
                        navController.navigate(Screens.Search.route)
                    },
                    onNavigateToLibrary = {
                        navController.navigate(Screens.Library.route)
                    },
                    onResumePlayback = {
                        navController.navigate("player")
                    },
                    onPlaySong = { song ->
                        val service = MusicService.instance ?: return@HomeScreen
                        service.playQueue(
                            ListQueue(items = listOf(song.toMediaItem()))
                        )
                    },
                )
            }
            composable(Screens.Stats.route) {
                StatsScreen()
            }
            composable(Screens.History.route) {
                HistoryScreen()
            }
            composable(Screens.Library.route) {
                LibraryScreen(
                    onNavigateToSearch = {
                        navController.navigate(Screens.Search.route)
                    },
                    onNavigateToArtists = {
                        navController.navigate(Screens.Search.route)
                    },
                    onNavigateToAlbums = {
                        navController.navigate(Screens.Search.route)
                    },
                    onNavigateToPlaylists = {
                        navController.navigate(Screens.Search.route)
                    },
                )
            }
            composable(Screens.Search.route) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToAlbum = { albumId ->
                        navController.navigate("album/$albumId")
                    },
                    onNavigateToArtist = { artistId ->
                        navController.navigate("artist/$artistId")
                    },
                    onPlaySong = { songItem ->
                        playSongFromSearch(context, songItem)
                    },
                )
            }
            composable("album/{albumId}") { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                val pc = LocalPlayerConnection.current
                AlbumScreen(
                    albumId = albumId,
                    onBack = { navController.popBackStack() },
                    onPlaySong = { song ->
                        val connection = pc ?: return@AlbumScreen
                        val service = MusicService.instance
                        if (service != null) {
                            service.playQueue(ListQueue(items = listOf(song.toMediaItem())))
                        }
                    },
                )
            }
            composable("artist/{artistId}") { backStackEntry ->
                val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                val pc = LocalPlayerConnection.current
                ArtistScreen(
                    artistId = artistId,
                    onBack = { navController.popBackStack() },
                    onPlaySong = { song ->
                        val service = MusicService.instance
                        if (service != null) {
                            service.playQueue(ListQueue(items = listOf(song.toMediaItem())))
                        }
                    },
                    onNavigateToAlbum = { browseId ->
                        navController.navigate("album/$browseId")
                    },
                )
            }
            composable("player") {
                PlayerScreen(
                    playerConnection = LocalPlayerConnection.current,
                    onDismiss = { navController.popBackStack() },
                    onOpenQueue = { navController.navigate("queue") },
                )
            }
            composable("queue") {
                QueueScreen(
                    playerConnection = LocalPlayerConnection.current,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

val LocalPlayerConnection = compositionLocalOf<PlayerConnection?> { null }
