package com.omnitune.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.omnitune.app.models.Album
import com.omnitune.app.models.Song
import com.omnitune.app.models.toSuvSong
import com.omnitune.app.playback.EqualizerBand
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.screens.AlbumScreen
import com.omnitune.app.ui.screens.ArtistScreen
import com.omnitune.app.ui.screens.ArtistDiscographyScreen
import com.omnitune.app.ui.screens.BrowseDetailScreen
import com.omnitune.app.ui.screens.DownloadsScreen
import com.omnitune.app.ui.screens.EqualizerScreen
import com.omnitune.app.ui.screens.HistoryScreen
import com.omnitune.app.ui.screens.HomeScreen
import com.omnitune.app.ui.screens.ImportPlaylistScreen
import com.omnitune.app.ui.screens.LibraryScreen
import com.omnitune.app.ui.screens.LoginScreen
import com.omnitune.app.ui.screens.MoodAndGenresScreen
import com.omnitune.app.ui.screens.PickMusicScreen
import com.omnitune.app.ui.screens.PlaylistScreen
import com.omnitune.app.ui.screens.SearchScreen
import com.omnitune.app.ui.screens.TabletHomeScreen
import com.omnitune.app.ui.screens.TvHomeScreen
import com.omnitune.app.ui.screens.settings.*
import com.omnitune.app.ui.screens.wrapped.WrappedScreen
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.app.ui.utils.DeviceFormFactor
import com.omnitune.app.ui.utils.LocalDeviceFormFactor

@Composable
fun NavGraph(
    navController: NavHostController,
    onPlaySong: (List<Song>, Int) -> Unit,
    onPlayPause: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onStartRadio: () -> Unit = {},
    onApplyEqualizerBands: (List<EqualizerBand>) -> Unit = {},
    onSetEqualizerEnabled: (Boolean) -> Unit = {},
    currentSong: Song? = null,
    dominantColors: DominantColors? = null,
    modifier: Modifier = Modifier,
    startDestination: Any = Destination.Home
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        composable<Destination.Home> {
            val formFactor = LocalDeviceFormFactor.current
            when {
                formFactor == DeviceFormFactor.TV -> {
                    TvHomeScreen(
                        onSongClick = { songs, index -> onPlaySong(songs, index) },
                        onPlaylistClick = { playlist ->
                            navController.navigate(
                                Destination.Playlist(
                                    playlistId = playlist.id,
                                    name = playlist.name,
                                    thumbnailUrl = playlist.thumbnailUrl
                                )
                            )
                        },
                        onAlbumClick = { album ->
                            navController.navigate(
                                Destination.Album(
                                    albumId = album.id,
                                    name = album.title,
                                    thumbnailUrl = album.thumbnailUrl
                                )
                            )
                        },
                        onArtistClick = { artistId ->
                            navController.navigate(Destination.Artist(artistId))
                        },
                        onExploreClick = { browseId, title ->
                            if (browseId == "FEmusic_moods_and_genres") {
                                navController.navigate(Destination.MoodAndGenres)
                            } else {
                                navController.navigate(Destination.Explore(browseId = browseId, title = title))
                            }
                        }
                    )
                }
                formFactor.isTabletLike -> {
                    TabletHomeScreen(
                        onSongClick = { songs, index -> onPlaySong(songs, index) },
                        onPlaylistClick = { playlist ->
                            navController.navigate(
                                Destination.Playlist(
                                    playlistId = playlist.id,
                                    name = playlist.name,
                                    thumbnailUrl = playlist.thumbnailUrl
                                )
                            )
                        },
                        onAlbumClick = { album ->
                            navController.navigate(
                                Destination.Album(
                                    albumId = album.id,
                                    name = album.title,
                                    thumbnailUrl = album.thumbnailUrl
                                )
                            )
                        },
                        onArtistClick = { artistId ->
                            navController.navigate(Destination.Artist(artistId))
                        },
                        onHistoryClick = {
                            navController.navigate(Destination.Recents)
                        },
                        onExploreClick = { browseId, title ->
                            if (browseId == "FEmusic_moods_and_genres") {
                                navController.navigate(Destination.MoodAndGenres)
                            } else {
                                navController.navigate(Destination.Explore(browseId = browseId, title = title))
                            }
                        },
                        onStartRadio = onStartRadio,
                        currentSong = currentSong
                    )
                }
                else -> {
                    HomeScreen(
                        onSongClick = { songs, index -> onPlaySong(songs, index) },
                        onPlaylistClick = { playlist ->
                            navController.navigate(
                                Destination.Playlist(
                                    playlistId = playlist.id,
                                    name = playlist.name,
                                    thumbnailUrl = playlist.thumbnailUrl
                                )
                            )
                        },
                        onAlbumClick = { album ->
                            navController.navigate(
                                Destination.Album(
                                    albumId = album.id,
                                    name = album.title,
                                    thumbnailUrl = album.thumbnailUrl
                                )
                            )
                        },
                        onArtistClick = { artistId ->
                            navController.navigate(Destination.Artist(artistId))
                        },
                        onHistoryClick = {
                            navController.navigate(Destination.Recents)
                        },
                        onStartRadio = onStartRadio,
                        onCreateMixClick = {
                            navController.navigate(Destination.PickMusic)
                        },
                        currentSong = currentSong
                    )
                }
            }
        }

        composable<Destination.Search> {
            SearchScreen(
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onArtistClick = { artistId ->
                    navController.navigate(Destination.Artist(artistId))
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Destination.Playlist(playlistId = playlistId))
                },
                onAlbumClick = { album ->
                    navController.navigate(
                        Destination.Album(
                            albumId = album.id,
                            name = album.title,
                            thumbnailUrl = album.thumbnailUrl
                        )
                    )
                },
                onBrowseClick = { browseId, params, title ->
                    navController.navigate(Destination.Explore(browseId = browseId, title = title, params = params))
                },
                currentSong = currentSong
            )
        }

        composable<Destination.Library> {
            LibraryScreen(
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onPlaylistClick = { playlist ->
                    navController.navigate(
                        Destination.Playlist(
                            playlistId = playlist.id,
                            name = playlist.name,
                            thumbnailUrl = playlist.thumbnailUrl
                        )
                    )
                },
                onHistoryClick = {
                    navController.navigate(Destination.Recents)
                },
                onArtistClick = { artistId ->
                    navController.navigate(Destination.Artist(artistId))
                },
                onAlbumClick = { album ->
                    navController.navigate(
                        Destination.Album(
                            albumId = album.id,
                            name = album.title,
                            thumbnailUrl = album.thumbnailUrl
                        )
                    )
                },
                onDownloadsClick = {
                    navController.navigate(Destination.Downloads)
                },
                onImportPlaylist = {
                    navController.navigate(Destination.ImportPlaylist)
                }
            )
        }

        composable<Destination.Settings> {
            SettingsScreen(
                onAppearanceClick = { navController.navigate(Destination.AppearanceSettings) },
                onPlaybackClick = { navController.navigate(Destination.PlaybackSettings) },
                onCustomizationClick = { navController.navigate(Destination.CustomizationSettings) },
                onAboutClick = { navController.navigate(Destination.About) },
                onStorageClick = { navController.navigate(Destination.Storage) },
                onStatsClick = { navController.navigate(Destination.ListeningStats) },
                onSupportClick = { navController.navigate(Destination.Support) },
                onMiscClick = { navController.navigate(Destination.Misc) },
                onSponsorBlockClick = { navController.navigate(Destination.SponsorBlockSettings) },
                onDiscordClick = { navController.navigate(Destination.DiscordSettings) },
                onAISettingsClick = { navController.navigate(Destination.AISettings) },
                onCreditsClick = { navController.navigate(Destination.Credits) },
                onUpdaterClick = { navController.navigate(Destination.Updater) },
                onLastFmClick = { navController.navigate(Destination.LastFmLogin) },
                onLoginClick = { navController.navigate(Destination.YouTubeLogin) }
            )
        }

        composable<Destination.AppearanceSettings> {
            AppearanceSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.PlaybackSettings> {
            PlaybackSettingsScreen(
                onBack = { navController.popBackStack() },
                onEqualizerClick = { navController.navigate(Destination.Equalizer) }
            )
        }

        composable<Destination.Equalizer> {
            EqualizerScreen(
                onBack = { navController.popBackStack() },
                onApplyBands = onApplyEqualizerBands,
                onSetEnabled = onSetEqualizerEnabled
            )
        }

        composable<Destination.CustomizationSettings> {
            CustomizationScreen(
                onBack = { navController.popBackStack() },
                onArtworkShapeClick = { navController.navigate(Destination.ArtworkShapeSettings) },
                onSeekbarStyleClick = { navController.navigate(Destination.SeekbarStyleSettings) },
                onArtworkSizeClick = { navController.navigate(Destination.ArtworkSizeSettings) }
            )
        }

        composable<Destination.ArtworkShapeSettings> {
            ArtworkShapeScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.SeekbarStyleSettings> {
            SeekbarStyleScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.ArtworkSizeSettings> {
            ArtworkSizeScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.About> {
            AboutScreen(
                onBackClick = { navController.popBackStack() },
                onChangelogClick = { navController.navigate(Destination.Changelog) }
            )
        }

        composable<Destination.Storage> {
            StorageScreen(
                onBackClick = { navController.popBackStack() },
                onPlayerCacheClick = { navController.navigate(Destination.PlayerCache) }
            )
        }

        composable<Destination.ListeningStats> {
            ListeningStatsScreen(
                onBackClick = { navController.popBackStack() },
                onWrappedClick = { navController.navigate(Destination.Wrapped) }
            )
        }

        composable<Destination.Wrapped> {
            WrappedScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.HowItWorks> {
            HowItWorksScreen(onBackClick = { navController.popBackStack() })
        }

        composable<Destination.Support> {
            SupportScreen(onBackClick = { navController.popBackStack() })
        }

        composable<Destination.Misc> {
            MiscScreen(
                onBackClick = { navController.popBackStack() },
                onLyricsProvidersClick = { navController.navigate(Destination.LyricsProviders) },
                onPoTokenClick = { navController.navigate(Destination.PoToken) }
            )
        }

        composable<Destination.PoToken> {
            PoTokenScreen(navController = navController)
        }

        composable<Destination.LyricsProviders> {
            LyricsProvidersScreen(onBackClick = { navController.popBackStack() })
        }

        composable<Destination.SponsorBlockSettings> {
            SponsorBlockSettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable<Destination.DiscordSettings> {
            DiscordSettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable<Destination.AIEqualizer> {
            AIEqualizerScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.AISettings> {
            AISettingsScreen(onBack = { navController.popBackStack() })
        }

        composable<Destination.Credits> {
            CreditsScreen(onBackClick = { navController.popBackStack() })
        }

        composable<Destination.Changelog> {
            ChangelogScreen(navController = navController)
        }

        composable<Destination.Downloads> {
            DownloadsScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songs: List<Song>, index: Int -> onPlaySong(songs, index) }
            )
        }

        composable<Destination.Recents> {
            HistoryScreen(
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Destination.Playlist> {
            PlaylistScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songs: List<Song>, index: Int -> onPlaySong(songs, index) }
            )
        }

        composable<Destination.Album> {
            AlbumScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songs: List<Song>, index: Int -> onPlaySong(songs, index) }
            )
        }

        composable<Destination.Artist> {
            ArtistScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songs: List<Song>, index: Int -> onPlaySong(songs, index) },
                onAlbumClick = { album ->
                    navController.navigate(
                        Destination.Album(
                            albumId = album.id,
                            name = album.title,
                            thumbnailUrl = album.thumbnailUrl
                        )
                    )
                }
            )
        }

        composable<Destination.ArtistDiscography> { backStackEntry ->
            val dest = backStackEntry.toRoute<Destination.ArtistDiscography>()
            ArtistDiscographyScreen(
                artistId = dest.artistId,
                type = dest.type,
                onBackClick = { navController.popBackStack() },
                onAlbumClick = { album ->
                    navController.navigate(
                        Destination.Album(
                            albumId = album.id,
                            name = album.title,
                            thumbnailUrl = album.thumbnailUrl
                        )
                    )
                }
            )
        }

        composable<Destination.Explore> {
            BrowseDetailScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songs, index -> onPlaySong(songs, index) },
                onArtistClick = { artistId -> navController.navigate(Destination.Artist(artistId)) },
                onAlbumClick = { album ->
                    navController.navigate(
                        Destination.Album(
                            albumId = album.id,
                            name = album.title,
                            thumbnailUrl = album.thumbnailUrl
                        )
                    )
                },
                onPlaylistClick = { playlistId -> navController.navigate(Destination.Playlist(playlistId = playlistId)) },
                currentSong = currentSong,
            )
        }

        composable<Destination.MoodAndGenres> {
            MoodAndGenresScreen(
                onBack = { navController.popBackStack() },
                onBrowse = { browseId, params, title ->
                    navController.navigate(Destination.Explore(browseId = browseId, title = title, params = params))
                }
            )
        }

        composable<Destination.YouTubeLogin> {
            LoginScreen(navController = navController)
        }

        composable<Destination.PickMusic> {
            PickMusicScreen(
                onBackClick = { navController.popBackStack() },
                onMixCreated = { songs ->
                    if (songs.isNotEmpty()) {
                        onPlaySong(songs, 0)
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable<Destination.ImportPlaylist> {
            ImportPlaylistScreen(onBackClick = { navController.popBackStack() })
        }

        composable<Destination.Updater> {
            UpdaterScreen(onBackClick = { navController.popBackStack() })
        }

        composable<Destination.LastFmLogin> {
            LastFmSettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable<Destination.PlayerCache> {
            PlayerCacheScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
