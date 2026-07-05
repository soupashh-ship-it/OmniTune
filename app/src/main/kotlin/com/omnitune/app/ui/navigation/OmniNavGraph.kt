package com.omnitune.app.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.playback.queues.ListQueue
import com.omnitune.app.ui.player.MiniPlayer
import com.omnitune.app.ui.player.PlayerScreen
import com.omnitune.app.ui.screens.AlbumScreen
import com.omnitune.app.ui.screens.ArtistScreen
import com.omnitune.app.ui.screens.DownloadsScreen
import com.omnitune.app.ui.screens.EqualizerScreen
import com.omnitune.app.ui.screens.HistoryScreen
import com.omnitune.app.ui.screens.HomeDefaultCatalog
import com.omnitune.app.ui.screens.HomeAllGenresScreen
import com.omnitune.app.ui.screens.HomeCollectionRoute
import com.omnitune.app.ui.screens.HomeDiscoveryRoute
import com.omnitune.app.ui.screens.LibraryAlbumsScreen
import com.omnitune.app.ui.screens.LibraryArtistsScreen
import com.omnitune.app.ui.screens.LibraryPlaylistsScreen
import com.omnitune.app.ui.screens.LibraryScreen
import com.omnitune.app.ui.screens.LibrarySongsScreen
import com.omnitune.app.ui.screens.LikedSongsScreen
import com.omnitune.app.ui.screens.PlaylistDetailScreen
import com.omnitune.app.ui.screens.QueueScreen
import com.omnitune.app.ui.screens.RecentlyPlayedScreen
import com.omnitune.app.ui.screens.Screens
import com.omnitune.app.ui.screens.Screens.Companion.ROUTE_DOWNLOADS
import com.omnitune.app.ui.screens.Screens.Companion.ROUTE_EQUALIZER
import com.omnitune.app.ui.screens.search.SearchScreen
import com.omnitune.app.ui.screens.settings.DiscordSettingsScreen
import com.omnitune.app.ui.screens.settings.DiscordLoginScreen
import com.omnitune.app.ui.screens.settings.BackupRestoreScreen
import com.omnitune.app.ui.screens.settings.AboutSettings
import com.omnitune.app.ui.screens.settings.AppearanceSettings
import com.omnitune.app.ui.screens.settings.ContentSettings
import com.omnitune.app.ui.screens.settings.DiagnosticsSettings
import com.omnitune.app.ui.screens.settings.LyricsSettings
import com.omnitune.app.ui.screens.settings.MediaControlsHelp
import com.omnitune.app.ui.screens.settings.PlaybackSettings
import com.omnitune.app.ui.screens.settings.ScrobblingSettings
import com.omnitune.app.ui.screens.settings.SettingsScreen
import com.omnitune.app.ui.screens.settings.SettingsSubScreenScaffold
import com.omnitune.app.ui.screens.settings.StorageSettings
import com.omnitune.app.ui.screens.settings.UpdatesSettings
import com.omnitune.app.ui.screens.StatsScreen
import com.omnitune.app.ui.shell.GlassBottomDock
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.theme.OmniMotion
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.constants.PureBlackKey
import com.omnitune.app.utils.rememberPreference
import com.omnitune.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private data class PendingSongQueue(
    val title: String,
    val songs: List<SongItem>,
    val index: Int,
)

private fun homeCollectionRoute(collectionId: String, artworkUrl: String?): String {
    val base = "homeCollection/${Uri.encode(collectionId)}"
    return artworkUrl
        ?.takeIf { it.isNotBlank() }
        ?.let { "$base?artworkUrl=${Uri.encode(it)}" }
        ?: base
}

private fun playSongList(
    context: android.content.Context,
    queueTitle: String,
    songs: List<SongItem>,
    index: Int,
    playerConnection: PlayerConnection?,
    onPlayerNotReady: (PendingSongQueue) -> Unit,
) {
    val songItem = songs[index]
    Timber.tag("OmniTunePlaybackTrace").i("$queueTitle play requested: ${songItem.title} (${songItem.id})")
    val connection = playerConnection ?: run {
        Timber.tag("OmniTunePlaybackTrace").w("$queueTitle play queued: player connection not ready")
        onPlayerNotReady(PendingSongQueue(queueTitle, songs, index))
        Toast.makeText(context, "Starting player...", Toast.LENGTH_SHORT).show()
        return
    }
    val mediaItems = songs.map { it.toMediaItem() }
    connection.playQueue(ListQueue(title = queueTitle, items = mediaItems, startIndex = index))
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
    var pendingSongQueue by remember { mutableStateOf<PendingSongQueue?>(null) }
    val isTopLevelRoute = topLevelScreens.any { route -> currentRoute == route || currentRoute?.startsWith("$route?") == true }
    val showBottomBar = isTopLevelRoute && currentRoute != "player" && currentRoute != "queue" && currentRoute != "settings" && !currentRoute.orEmpty().startsWith("settings/")

    LaunchedEffect(localPlayerConnection, pendingSongQueue) {
        val queueData = pendingSongQueue
        val connection = localPlayerConnection
        if (queueData != null && connection != null) {
            val (title, songs, index) = queueData
            Timber.tag("OmniTunePlaybackTrace").i("Playing queued search: ${songs[index].title}")
            pendingSongQueue = null
            val mediaItems = songs.map { it.toMediaItem() }
            connection.playQueue(ListQueue(title = title, items = mediaItems, startIndex = index))
        }
    }

    val showMiniPlayer = currentRoute != "player" && currentRoute != "queue" && currentMediaMetadata != null

    val mpHeight = OmniChrome.MiniPlayerHeight
    val dockHeight = OmniChrome.BottomDockHeight
    val chromeSpacing = OmniSpacing.compact
    val chromeBottomMargin = OmniChrome.BottomDockBottomMargin

    val shellBottomPaddingTarget = when {
        currentRoute == "player" || currentRoute == "queue" -> 0.dp
        showMiniPlayer && showBottomBar -> mpHeight + chromeSpacing + dockHeight + chromeBottomMargin
        showBottomBar -> dockHeight + chromeBottomMargin
        showMiniPlayer -> mpHeight + chromeSpacing
        else -> 0.dp
    }
    val isPlayerRoute = currentRoute == "player" || currentRoute == "queue"
    val shellBottomPadding by animateDpAsState(
        targetValue = shellBottomPaddingTarget,
        animationSpec = if (isPlayerRoute) androidx.compose.animation.core.snap() else OmniMotion.gentleSpring(),
        label = "shell_bottom_padding",
    )

    val pureBlack by rememberPreference(PureBlackKey, false)

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screens.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = shellBottomPadding),
            enterTransition = { OmniMotion.screenEnter() },
            exitTransition = { OmniMotion.screenExit() },
            popEnterTransition = { OmniMotion.screenPopEnter() },
            popExitTransition = { OmniMotion.screenPopExit() },
        ) {
            composable(Screens.Home.route) {
                HomeDiscoveryRoute(
                    onNavigateToSearch = { navController.navigate(Screens.Search.route) },
                    onNavigateToCollection = { collectionId, artworkUrl -> navController.navigate(homeCollectionRoute(collectionId, artworkUrl)) },
                    onNavigateToLibrary = { navController.navigate(Screens.Library.route) },
                    onNavigateToDownloads = { navController.navigate(ROUTE_DOWNLOADS) },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToAllGenres = { navController.navigate("all_genres") },
                    onResumePlayback = { navController.navigate("player") },
                    onNavigateToExplore = { route -> navController.navigate(route) },
                    onPlaySong = { song -> localPlayerConnection?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) },
                    onPlayProviderSong = { song ->
                        playSongList(
                            context = context,
                            queueTitle = "Home",
                            songs = listOf(song),
                            index = 0,
                            playerConnection = localPlayerConnection,
                            onPlayerNotReady = { pendingSongQueue = it },
                        )
                    },
                    onPlayProviderSongs = { songs ->
                        if (songs.isNotEmpty()) {
                            playSongList(
                                context = context,
                                queueTitle = "Quick Picks",
                                songs = songs,
                                index = 0,
                                playerConnection = localPlayerConnection,
                                onPlayerNotReady = { pendingSongQueue = it },
                            )
                        }
                    },
                    onPlaySongs = { songs ->
                        if (songs.isNotEmpty()) localPlayerConnection?.playQueue(ListQueue(title = "Home", items = songs.map { it.toMediaItem() }))
                    },
                )
            }
            composable(Screens.Stats.route) { 
                StatsScreen(onNavigateToYearInMusic = { navController.navigate(Screens.YearInMusic.route) }) 
            }
            composable(Screens.History.route) {
                HistoryScreen(onPlaySong = { song ->
                    localPlayerConnection?.playQueue(ListQueue(title = "History", items = listOf(song.toMediaItem())))
                })
            }
            composable(Screens.YearInMusic.route) {
                com.omnitune.app.ui.screens.YearInMusicScreen(navController = navController)
            }
            composable(Screens.ThemeCreator.route) {
                com.omnitune.app.ui.screens.settings.ThemeCreatorScreen(navController = navController)
            }
            composable(Screens.PalettePicker.route) {
                com.omnitune.app.ui.screens.settings.PalettePickerScreen(navController = navController)
            }
            composable(Screens.CustomizeBackground.route) {
                com.omnitune.app.ui.screens.settings.CustomizeBackground(navController = navController)
            }
            composable(Screens.PoToken.route) {
                com.omnitune.app.ui.screens.settings.PoTokenScreen(navController = navController)
            }
            composable(Screens.Changelog.route) {
                com.omnitune.app.ui.screens.settings.ChangelogScreen(navController = navController)
            }
            composable(Screens.MusicTogether.route) {
                com.omnitune.app.ui.screens.settings.MusicTogetherScreen(navController = navController)
            }
            composable(Screens.Charts.route) {
                com.omnitune.app.ui.screens.ChartsScreen(navController = navController)
            }
            composable(Screens.Explore.route) {
                com.omnitune.app.ui.screens.ExploreScreen(navController = navController)
            }
            composable(Screens.NewRelease.route) {
                com.omnitune.app.ui.screens.NewReleaseScreen(navController = navController)
            }
            composable(Screens.MoodAndGenres.route) {
                com.omnitune.app.ui.screens.MoodAndGenresScreen(navController = navController)
            }
            composable(Screens.YouTubeBrowse.route) {
                com.omnitune.app.ui.screens.YouTubeBrowseScreen(navController = navController)
            }
            composable(Screens.Account.route) {
                com.omnitune.app.ui.screens.AccountScreen(navController = navController)
            }
            composable(Screens.Login.route) {
                com.omnitune.app.ui.screens.LoginScreen(navController = navController)
            }
            composable(Screens.AccountSettings.route) {
                com.omnitune.app.ui.screens.settings.OmniTuneAccountSettingsScreen(navController = navController)
            }
            composable(
                route = Screens.AutoPlaylist.route + "/{playlistType}",
                arguments = listOf(androidx.navigation.navArgument("playlistType") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val playlistType = backStackEntry.arguments?.getString("playlistType") ?: ""
                com.omnitune.app.ui.screens.AutoPlaylistScreen(
                    navController = navController,
                    playlistType = playlistType
                )
            }
            composable(Screens.Library.route) {
                LibraryScreen(onNavigateToSearch = { navController.navigate(Screens.Search.route) }, onNavigateToLiked = { navController.navigate("liked_songs") },
                    onNavigateToSongs = { navController.navigate("library_songs") },
                    onNavigateToDownloads = { navController.navigate(ROUTE_DOWNLOADS) },
                    onNavigateToRecentlyPlayed = { navController.navigate("recently_played") },
                    onNavigateToArtists = { navController.navigate("library_artists") },
                    onNavigateToAlbums = { navController.navigate("library_albums") },
                    onNavigateToPlaylists = { navController.navigate("library_playlists") })
            }
            composable("library_songs") {
                LibrarySongsScreen(onBack = { navController.popBackStack() }, onPlaySong = { song ->
                    localPlayerConnection?.playQueue(ListQueue(title = "Songs", items = listOf(song.toMediaItem())))
                })
            }
            composable("library_artists") {
                LibraryArtistsScreen(onBack = { navController.popBackStack() }, onNavigateToArtist = { navController.navigate("artist/$it") })
            }
            composable("library_albums") {
                LibraryAlbumsScreen(onBack = { navController.popBackStack() }, onNavigateToAlbum = { navController.navigate("album/$it") })
            }
            composable("library_playlists") {
                LibraryPlaylistsScreen(onBack = { navController.popBackStack() }, onNavigateToPlaylist = { navController.navigate("playlist/$it") })
            }
            composable("playlist/{playlistId}") {
                PlaylistDetailScreen(onBack = { navController.popBackStack() }, onPlaySong = { song ->
                    localPlayerConnection?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) })
            }
            composable("liked_songs") {
                LikedSongsScreen(onBack = { navController.popBackStack() }, onPlaySong = { song ->
                    localPlayerConnection?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) })
            }
            composable("recently_played") {
                RecentlyPlayedScreen(onBack = { navController.popBackStack() }, onPlaySong = { song ->
                    localPlayerConnection?.playQueue(ListQueue(items = listOf(song.toMediaItem()))) })
            }
            composable(
                route = "${Screens.Search.route}?query={query}",
                arguments = listOf(navArgument("query") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                SearchScreen(initialQuery = it.arguments?.getString("query"), onBack = { navController.popBackStack() }, onNavigateToAlbum = { navController.navigate("album/$it") },
                    onNavigateToArtist = { navController.navigate("artist/$it") },
                    onNavigateToPlaylist = { playlist ->
                        navController.navigate(
                            homeCollectionRoute(
                                collectionId = HomeDefaultCatalog.queryCollectionId(playlist.title),
                                artworkUrl = playlist.thumbnail,
                            ),
                        )
                    },
                    onPlaySong = { songs, index ->
                        playSongList(
                            context = context,
                            queueTitle = "Search Results",
                            songs = songs,
                            index = index,
                            playerConnection = localPlayerConnection,
                            onPlayerNotReady = { pendingSongQueue = it }
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
            composable(
                route = "homeCollection/{collectionId}?artworkUrl={artworkUrl}",
                arguments = listOf(
                    navArgument("collectionId") { type = NavType.StringType },
                    navArgument("artworkUrl") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                HomeCollectionRoute(
                    onBack = { navController.popBackStack() },
                    onSearch = { query -> navController.navigate("${Screens.Search.route}?query=${Uri.encode(query)}") },
                    onOpenRelated = { song ->
                        val artist = song.artists.firstOrNull()?.name.orEmpty()
                        val query = listOf(song.title, artist, "similar songs")
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                        navController.navigate(
                            homeCollectionRoute(
                                collectionId = HomeDefaultCatalog.queryCollectionId(query),
                                artworkUrl = song.thumbnail,
                            ),
                        )
                    },
                    onPlaySongs = { songs, index ->
                        playSongList(
                            context = context,
                            queueTitle = "Home Collection",
                            songs = songs,
                            index = index,
                            playerConnection = localPlayerConnection,
                            onPlayerNotReady = { pendingSongQueue = it },
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
                    },
                )
            }
            composable("all_genres",
                enterTransition = { OmniMotion.screenEnter() },
                exitTransition = { OmniMotion.screenExit() },
                popEnterTransition = { OmniMotion.screenPopEnter() },
                popExitTransition = { OmniMotion.screenPopExit() },
            ) {
                HomeAllGenresScreen(
                    onBack = { navController.popBackStack() },
                    onChipClick = { chip -> navController.navigate(homeCollectionRoute(chip.id, null)) }
                )
            }
            composable("settings/content") { 
                SettingsSubScreenScaffold(title = "Content", onBack = { navController.popBackStack() }) { 
                    ContentSettings(
                        onNavigateToPoToken = { navController.navigate(Screens.PoToken.route) },
                        onNavigateToMusicTogether = { navController.navigate(Screens.MusicTogether.route) }
                    ) 
                } 
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
            composable("player") { 
                PlayerScreen(
                    playerConnection = localPlayerConnection, 
                    onDismiss = { navController.popBackStack() }, 
                    onOpenQueue = { navController.navigate("queue") },
                    onNavigateToListenTogether = { navController.navigate(Screens.MusicTogether.route) }
                ) 
            }
            composable("queue") { QueueScreen(playerConnection = localPlayerConnection, onBack = { navController.popBackStack() }) }
            composable("settings/appearance") { 
                SettingsSubScreenScaffold(title = "Appearance", onBack = { navController.popBackStack() }) { 
                    AppearanceSettings(
                        onNavigateToThemeCreator = { navController.navigate(Screens.ThemeCreator.route) },
                        onNavigateToCustomizeBackground = { navController.navigate(Screens.CustomizeBackground.route) }
                    ) 
                } 
            }
            composable("settings/updates") { 
                SettingsSubScreenScaffold(title = "Updates", onBack = { navController.popBackStack() }) { 
                    UpdatesSettings(onNavigateToChangelog = { navController.navigate(Screens.Changelog.route) }) 
                } 
            }
            composable("settings") { 
                SettingsScreen(
                    onBack = { navController.popBackStack() }, 
                    onNavigateToEqualizer = { navController.navigate(ROUTE_EQUALIZER) }, 
                    onNavigateToCategory = { cat -> 
                        if (cat == "account_settings") {
                            navController.navigate(Screens.AccountSettings.route)
                        } else if (cat == "music_together") {
                            navController.navigate(Screens.MusicTogether.route)
                        } else {
                            navController.navigate("settings/$cat") 
                        }
                    }
                ) 
            }
            composable("settings/playback") { SettingsSubScreenScaffold(title = "Playback & Audio", onBack = { navController.popBackStack() }) { PlaybackSettings(onNavigateToEqualizer = { navController.navigate(ROUTE_EQUALIZER) }) } }
            composable("settings/content") { 
                SettingsSubScreenScaffold(title = "Content", onBack = { navController.popBackStack() }) { 
                    ContentSettings(onNavigateToPoToken = { navController.navigate(Screens.PoToken.route) }) 
                } 
            }
            composable("settings/storage") { SettingsSubScreenScaffold(title = "Storage", onBack = { navController.popBackStack() }) { StorageSettings() } }
            composable("settings/lyrics") { SettingsSubScreenScaffold(title = "Lyrics", onBack = { navController.popBackStack() }) { LyricsSettings() } }
            composable("settings/scrobbling") { SettingsSubScreenScaffold(title = "Scrobbling", onBack = { navController.popBackStack() }) { ScrobblingSettings() } }
            composable("settings/updates") { 
                SettingsSubScreenScaffold(title = "Updates", onBack = { navController.popBackStack() }) { 
                    UpdatesSettings(onNavigateToChangelog = { navController.navigate(Screens.Changelog.route) }) 
                } 
            }
            composable("settings/diagnostics") { SettingsSubScreenScaffold(title = "Diagnostics", onBack = { navController.popBackStack() }) { DiagnosticsSettings() } }
            composable("settings/about") { SettingsSubScreenScaffold(title = "About", onBack = { navController.popBackStack() }) { AboutSettings() } }
            composable("settings/discord") { SettingsSubScreenScaffold(title = "Discord RPC", onBack = { navController.popBackStack() }) { DiscordSettingsScreen(onNavigateToLogin = { navController.navigate("settings/discord_login") }) } }
            composable("settings/discord_login") { DiscordLoginScreen(onBack = { navController.popBackStack() }, onLoggedIn = { navController.popBackStack() }) }
            composable("settings/backup_restore") { SettingsSubScreenScaffold(title = "Backup & Restore", onBack = { navController.popBackStack() }) { BackupRestoreScreen() } }
            composable("settings/notifications") { SettingsSubScreenScaffold(title = "Notifications", onBack = { navController.popBackStack() }) { MediaControlsHelp() } }
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
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = OmniMotion.miniPlayerEnter(),
                    exit = OmniMotion.miniPlayerExit(),
                ) {
                    MiniPlayer(
                        pureBlack = pureBlack,
                        playerConnection = localPlayerConnection,
                        onClick = { if (localPlayerConnection != null) navController.navigate("player") }
                    )
                }
            }
            if (showBottomBar) GlassBottomDock(currentRoute = currentRoute, onNavigate = { route -> navController.navigate(route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }, pureBlack = pureBlack)
        }
    }
}
