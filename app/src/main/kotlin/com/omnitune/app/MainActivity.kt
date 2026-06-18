/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.omnitune.app.ui.screens.SearchScreen
import com.omnitune.app.ui.screens.StatsScreen
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.playback.MusicService
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.innertube.models.SongItem
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
import com.omnitune.app.ui.theme.OmniTuneTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var database: MusicDatabase
    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    private fun startMusicServiceSafely() { try { startService(Intent(this, MusicService::class.java)) } catch (e: Exception) { reportException(e) } }
    private fun bindToMusicService() {
        val service = MusicService.instance ?: return
        if (playerConnection == null) playerConnection = PlayerConnection(this@MainActivity, service.binder(), database, lifecycleScope)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OmniTuneTheme {
                CompositionLocalProvider(LocalPlayerConnection provides playerConnection) {
                    Box(modifier = Modifier.fillMaxSize().background(OmniColors.Background)) { OmniTuneMainScreen() }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startMusicServiceSafely()
        lifecycleScope.launch { while (MusicService.instance == null) delay(100); bindToMusicService() }
    }
}

private fun playSongFromSearch(context: android.content.Context, songItem: SongItem) {
    val service = MusicService.instance ?: run { Toast.makeText(context, "MusicService not ready", Toast.LENGTH_SHORT).show(); return }
    service.playQueue(ListQueue(items = listOf(songItem.toMediaItem())))
}

@Composable
fun OmniTuneMainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val topLevelScreens = Screens.MainScreens.map { it.route }
    val context = LocalContext.current
    val localPlayerConnection = LocalPlayerConnection.current
    val showBottomBar = currentRoute in topLevelScreens && currentRoute != "player" && currentRoute != "queue" && currentRoute != "settings"

    Box(modifier = Modifier.fillMaxSize().background(OmniColors.Background)) {
        NavHost(navController = navController, startDestination = Screens.Home.route, modifier = Modifier.fillMaxSize()) {
            composable(Screens.Home.route) {
                HomeScreen(onNavigateToSearch = { navController.navigate(Screens.Search.route) }, onNavigateToLibrary = { navController.navigate(Screens.Library.route) },
                    onResumePlayback = { navController.navigate("player") }, onPlaySong = { song -> MusicService.instance?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) })
            }
            composable(Screens.Stats.route) { StatsScreen() }
            composable(Screens.History.route) { HistoryScreen() }
            composable(Screens.Library.route) {
                LibraryScreen(onNavigateToSearch = { navController.navigate(Screens.Search.route) }, onNavigateToLiked = { navController.navigate("liked_songs") },
                    onNavigateToRecentlyPlayed = { navController.navigate("recently_played") },
                    onNavigateToArtists = { Toast.makeText(context, "Artists — coming soon", Toast.LENGTH_SHORT).show() },
                    onNavigateToAlbums = { Toast.makeText(context, "Albums — coming soon", Toast.LENGTH_SHORT).show() },
                    onNavigateToPlaylists = { Toast.makeText(context, "Playlists — coming soon", Toast.LENGTH_SHORT).show() })
            }
            composable("liked_songs") {
                LikedSongsScreen(onBack = { navController.popBackStack() }, onPlaySong = { song ->
                    MusicService.instance?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) })
            }
            composable("recently_played") {
                RecentlyPlayedScreen(onBack = { navController.popBackStack() }, onPlaySong = { song ->
                    MusicService.instance?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) })
            }
            composable(Screens.Search.route) {
                SearchScreen(onBack = { navController.popBackStack() }, onNavigateToAlbum = { navController.navigate("album/$it") },
                    onNavigateToArtist = { navController.navigate("artist/$it") }, onPlaySong = { playSongFromSearch(context, it) })
            }
            composable("album/{albumId}") {
                AlbumScreen(albumId = it.arguments?.getString("albumId") ?: "", onBack = { navController.popBackStack() },
                    onPlaySong = { song -> MusicService.instance?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) })
            }
            composable("artist/{artistId}") {
                ArtistScreen(artistId = it.arguments?.getString("artistId") ?: "", onBack = { navController.popBackStack() },
                    onPlaySong = { song -> MusicService.instance?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) },
                    onNavigateToAlbum = { navController.navigate("album/$it") })
            }
            composable("player") { PlayerScreen(playerConnection = LocalPlayerConnection.current, onDismiss = { navController.popBackStack() }, onOpenQueue = { navController.navigate("queue") }) }
            composable("queue") { QueueScreen(playerConnection = LocalPlayerConnection.current, onBack = { navController.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
        // MiniPlayer + Glass Bottom Dock
        Column(modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))) {
            if (currentRoute != "player" && currentRoute != "queue") {
                Box(modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { if (localPlayerConnection != null) navController.navigate("player") }) {
                    MiniPlayer(pureBlack = false, playerConnection = localPlayerConnection)
                }
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

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
        .shadow(16.dp, OmniShapes.Dock, ambientColor = OmniColors.Primary.copy(alpha = 0.1f), spotColor = OmniColors.Primary.copy(alpha = 0.08f))
        .clip(OmniShapes.Dock).border(1.dp, OmniColors.GlassBorder, OmniShapes.Dock)
        .background(Brush.verticalGradient(listOf(OmniColors.GlassSurfaceStrong, OmniColors.GlassSurface)))
        .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        navItems.forEach { item ->
            val selected = currentRoute == item.route
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                .clickable(remember { MutableInteractionSource() }, indication = null) { onNavigate(item.route) }
                .then(if (selected) Modifier.background(Brush.horizontalGradient(listOf(OmniColors.Primary.copy(alpha = 0.2f), OmniColors.Secondary.copy(alpha = 0.15f)))) else Modifier)
                .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(painterResource(item.resId), contentDescription = item.label, tint = if (selected) OmniColors.Secondary else OmniColors.TextMuted, modifier = Modifier.size(22.dp))
                    if (selected) Text(item.label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = OmniColors.Secondary)
                }
            }
        }
    }
}

val LocalPlayerConnection = compositionLocalOf<PlayerConnection?> { null }
