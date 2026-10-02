package com.omnitune.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.omnitune.shared.domain.models.AlbumItem
import com.omnitune.shared.domain.models.ArtistItem
import com.omnitune.shared.domain.models.PlaylistItem
import com.omnitune.shared.domain.models.SongItem
import com.omnitune.shared.playback.PlaybackController
import com.omnitune.shared.playback.RepeatMode
import com.omnitune.shared.ui.player.ExpandablePlayerSheet
import com.omnitune.shared.ui.player.MiniPlayer
import com.omnitune.shared.ui.screens.AlbumScreen
import com.omnitune.shared.ui.screens.ArtistScreen
import com.omnitune.shared.ui.screens.HomeScreen
import com.omnitune.shared.ui.screens.LibraryScreen
import com.omnitune.shared.ui.screens.PlaylistScreen
import com.omnitune.shared.ui.screens.SearchScreen
import com.omnitune.shared.ui.screens.SettingsScreen
import com.omnitune.shared.ui.theme.DominantColors
import com.omnitune.shared.ui.theme.SuvMusicPalette
import com.omnitune.shared.ui.theme.SuvMusicTheme
import com.omnitune.shared.ui.theme.getPaletteColors

val LocalPlaybackController = staticCompositionLocalOf<PlaybackController> {
    error("No PlaybackController provided")
}

enum class NavigationTab(val label: String) {
    HOME("Home"),
    SEARCH("Search"),
    LIBRARY("Library"),
    SETTINGS("Settings")
}

sealed class NavigationRoute {
    data object TabView : NavigationRoute()
    data class AlbumDetail(val album: AlbumItem) : NavigationRoute()
    data class ArtistDetail(val artist: ArtistItem) : NavigationRoute()
    data class PlaylistDetail(val playlist: PlaylistItem) : NavigationRoute()
}

/**
 * Root Compose Multiplatform Application Composable.
 * Features bottom navigation bar, persistent mini player docked above navigation,
 * full-screen expandable player sheet, detail screen routing, and LocalPlaybackController.
 */
@Composable
fun OmniTuneApp(
    controller: PlaybackController = remember { createPlaybackController() },
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(NavigationTab.HOME) }
    var activeRoute by remember { mutableStateOf<NavigationRoute>(NavigationRoute.TabView) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    var currentPalette by remember { mutableStateOf(SuvMusicPalette.DEFAULT) }
    var isPureBlack by remember { mutableStateOf(false) }

    // Playback state observations
    val currentSong by controller.currentItem.collectAsState()
    val isPlaying by controller.isPlaying.collectAsState()
    val currentPositionMs by controller.currentPositionMs.collectAsState()
    val durationMs by controller.durationMs.collectAsState()
    val queue by controller.queue.collectAsState()
    val currentIndex by controller.currentIndex.collectAsState()
    val shuffleMode by controller.shuffleMode.collectAsState()
    val repeatMode by controller.repeatMode.collectAsState()

    // Sample data for initial discovery
    val sampleSongs = remember {
        listOf(
            SongItem(id = "s1", title = "Blinding Lights", artist = "The Weeknd", duration = 200, thumbnailUrl = "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg"),
            SongItem(id = "s2", title = "Save Your Tears", artist = "The Weeknd", duration = 215, thumbnailUrl = "https://i.ytimg.com/vi/XXYlFuWEuKI/hqdefault.jpg"),
            SongItem(id = "s3", title = "Starboy", artist = "The Weeknd", duration = 230, thumbnailUrl = "https://i.ytimg.com/vi/34Na4j8AVgA/hqdefault.jpg"),
            SongItem(id = "s4", title = "Get Lucky", artist = "Daft Punk", duration = 248, thumbnailUrl = "https://i.ytimg.com/vi/5NV6Rdv1a3I/hqdefault.jpg")
        )
    }

    val samplePlaylists = remember {
        listOf(
            PlaylistItem(id = "p1", title = "Today's Top Hits", author = "OmniTune Curated", songCount = 4, songs = sampleSongs)
        )
    }

    SuvMusicTheme(
        palette = currentPalette,
        darkTheme = true,
        pureBlack = isPureBlack
    ) {
        val accentColor = MaterialTheme.colorScheme.primary

        CompositionLocalProvider(
            LocalPlaybackController provides controller
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Main Content Area
                when (val route = activeRoute) {
                    is NavigationRoute.AlbumDetail -> {
                        AlbumScreen(
                            album = route.album,
                            onBackClick = { activeRoute = NavigationRoute.TabView },
                            onSongClick = { songs, idx -> controller.playQueue(songs, idx) },
                            accentColor = accentColor
                        )
                    }
                    is NavigationRoute.ArtistDetail -> {
                        ArtistScreen(
                            artist = route.artist,
                            onBackClick = { activeRoute = NavigationRoute.TabView },
                            onSongClick = { songs, idx -> controller.playQueue(songs, idx) },
                            accentColor = accentColor
                        )
                    }
                    is NavigationRoute.PlaylistDetail -> {
                        PlaylistScreen(
                            playlist = route.playlist,
                            onBackClick = { activeRoute = NavigationRoute.TabView },
                            onSongClick = { songs, idx -> controller.playQueue(songs, idx) },
                            accentColor = accentColor
                        )
                    }
                    NavigationRoute.TabView -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                when (activeTab) {
                                    NavigationTab.HOME -> {
                                        HomeScreen(
                                            onSongClick = { song -> controller.play(song) },
                                            onHistoryClick = {},
                                            quickAccessItems = sampleSongs,
                                            quickPicks = sampleSongs,
                                            accentColor = accentColor
                                        )
                                    }
                                    NavigationTab.SEARCH -> {
                                        SearchScreen(
                                            onSongClick = { song -> controller.play(song) },
                                            searchResults = sampleSongs,
                                            accentColor = accentColor
                                        )
                                    }
                                    NavigationTab.LIBRARY -> {
                                        LibraryScreen(
                                            onSongClick = { song -> controller.play(song) },
                                            onPlaylistClick = { pl -> activeRoute = NavigationRoute.PlaylistDetail(pl) },
                                            songs = sampleSongs,
                                            playlists = samplePlaylists,
                                            onExploreMusicClick = { activeTab = NavigationTab.HOME },
                                            accentColor = accentColor
                                        )
                                    }
                                    NavigationTab.SETTINGS -> {
                                        SettingsScreen(
                                            currentPalette = currentPalette,
                                            isPureBlack = isPureBlack,
                                            onPaletteChange = { currentPalette = it },
                                            onPureBlackChange = { isPureBlack = it },
                                            accentColor = accentColor
                                        )
                                    }
                                }
                            }

                            // Persistent Docked MiniPlayer (directly above bottom nav)
                            if (currentSong != null && !isPlayerExpanded) {
                                MiniPlayer(
                                    song = currentSong!!,
                                    isPlaying = isPlaying,
                                    currentPositionMs = currentPositionMs,
                                    durationMs = durationMs,
                                    onPlayPauseClick = {
                                        if (isPlaying) controller.pause() else controller.resume()
                                    },
                                    onSkipNextClick = { controller.skipNext() },
                                    onClick = { isPlayerExpanded = true },
                                    accentColor = accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Bottom Navigation Bar
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.height(72.dp)
                            ) {
                                NavigationBarItem(
                                    selected = activeTab == NavigationTab.HOME,
                                    onClick = { activeTab = NavigationTab.HOME },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = accentColor,
                                        selectedTextColor = accentColor,
                                        indicatorColor = accentColor.copy(alpha = 0.2f)
                                    )
                                )
                                NavigationBarItem(
                                    selected = activeTab == NavigationTab.SEARCH,
                                    onClick = { activeTab = NavigationTab.SEARCH },
                                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                    label = { Text("Search") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = accentColor,
                                        selectedTextColor = accentColor,
                                        indicatorColor = accentColor.copy(alpha = 0.2f)
                                    )
                                )
                                NavigationBarItem(
                                    selected = activeTab == NavigationTab.LIBRARY,
                                    onClick = { activeTab = NavigationTab.LIBRARY },
                                    icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Library") },
                                    label = { Text("Library") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = accentColor,
                                        selectedTextColor = accentColor,
                                        indicatorColor = accentColor.copy(alpha = 0.2f)
                                    )
                                )
                                NavigationBarItem(
                                    selected = activeTab == NavigationTab.SETTINGS,
                                    onClick = { activeTab = NavigationTab.SETTINGS },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = accentColor,
                                        selectedTextColor = accentColor,
                                        indicatorColor = accentColor.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }
                }

                // Full-Screen Expandable Player Sheet
                AnimatedVisibility(
                    visible = isPlayerExpanded && currentSong != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    currentSong?.let { song ->
                        ExpandablePlayerSheet(
                            song = song,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            shuffleMode = shuffleMode,
                            repeatMode = repeatMode,
                            onPlayPause = {
                                if (isPlaying) controller.pause() else controller.resume()
                            },
                            onSkipNext = { controller.skipNext() },
                            onSkipPrevious = { controller.skipPrevious() },
                            onSeekTo = { pos -> controller.seekTo(pos) },
                            onToggleShuffle = { controller.setShuffle(!shuffleMode) },
                            onToggleRepeat = {
                                val nextMode = when (repeatMode) {
                                    RepeatMode.OFF -> RepeatMode.ALL
                                    RepeatMode.ALL -> RepeatMode.ONE
                                    RepeatMode.ONE -> RepeatMode.OFF
                                }
                                controller.setRepeat(nextMode)
                            },
                            onCollapse = { isPlayerExpanded = false },
                            queue = queue,
                            currentIndex = currentIndex,
                            onTrackSelect = { idx -> controller.playQueue(queue, idx) },
                            onRemoveFromQueue = { idx -> controller.removeFromQueue(idx) },
                            onReorderQueue = { from, to -> controller.reorderQueue(from, to) },
                            onClearQueue = { controller.playQueue(emptyList(), 0) },
                            accentColor = accentColor
                        )
                    }
                }
            }
        }
    }
}
