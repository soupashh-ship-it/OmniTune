/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.omnitune.app.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.omnitune.app.LocalDatabase
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.R
import com.omnitune.app.constants.AlbumViewTypeKey
import com.omnitune.app.constants.CONTENT_TYPE_HEADER
import com.omnitune.app.constants.CONTENT_TYPE_PLAYLIST
import com.omnitune.app.constants.GridItemSize
import com.omnitune.app.constants.GridThumbnailHeight
import com.omnitune.app.constants.LibraryViewType
import com.omnitune.app.constants.MixSortDescendingKey
import com.omnitune.app.constants.MixSortType
import com.omnitune.app.constants.MixSortTypeKey
import com.omnitune.app.constants.PlaylistSortType
import com.omnitune.app.constants.PlaylistSortTypeKey
import com.omnitune.app.constants.PlaylistTagsFilterKey
import com.omnitune.app.constants.ShowLikedPlaylistKey
import com.omnitune.app.constants.ShowDownloadedPlaylistKey
import com.omnitune.app.constants.ShowTopPlaylistKey
import com.omnitune.app.constants.ShowCachedPlaylistKey
import com.omnitune.app.constants.UseNewLibraryDesignKey
import com.omnitune.app.constants.YtmSyncKey
import com.omnitune.app.db.entities.Album
import com.omnitune.app.db.entities.Artist
import com.omnitune.app.db.entities.Playlist
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.extensions.move
import com.omnitune.app.ui.component.AlbumListItem
import com.omnitune.app.ui.component.ArtistListItem
import com.omnitune.app.ui.component.LocalMenuState
import com.omnitune.app.ui.component.PlaylistListItem
import com.omnitune.app.ui.menu.AlbumMenu
import com.omnitune.app.ui.menu.ArtistMenu
import com.omnitune.app.ui.menu.PlaylistMenu
import com.omnitune.app.utils.rememberEnumPreference
import com.omnitune.app.utils.rememberPreference
import com.omnitune.app.ui.screens.LibraryViewModel
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import com.omnitune.app.ui.component.SortHeader
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

val GridItemsSizeKey = stringPreferencesKey("GridItemsSizeKey")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.LIST)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        MixSortTypeKey,
        MixSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val (playlistSortType) = rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CUSTOM)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val (selectedTagsFilter) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }
    val database = LocalDatabase.current
    val filteredPlaylistIds by database.playlistIdsByTags(
        if (selectedTagIds.isEmpty()) emptyList() else selectedTagIds.toList()
    ).collectAsState(initial = emptyList())

    val (topSizeStr) = rememberPreference(com.omnitune.app.constants.TopSize, "50")
    val topSize = topSizeStr.toIntOrNull() ?: 50
    val likedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = "Liked"
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val downloadPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = "Offline"
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val topPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = "My Top $topSize"
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val cachePlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = "Cached"
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)
    val (useNewLibraryDesign) = rememberPreference(UseNewLibraryDesignKey, false)

    val albums = viewModel.libraryAlbums.collectAsState()
    val artist = viewModel.libraryArtists.collectAsState()
    val playlist = viewModel.playlists.collectAsState()

    val collator = Collator.getInstance(Locale.getDefault())
    collator.strength = Collator.PRIMARY
    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val visiblePlaylists =
        playlist.value.let { playlists ->
            if (selectedTagIds.isEmpty()) playlists else playlists.filter { it.id in filteredPlaylistIds }
        }
    val otherItems =
        albums.value + artist.value
    val sortedOtherItems =
        when (sortType) {
            MixSortType.CREATE_DATE ->
                otherItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.bookmarkedAt
                        is Artist -> item.artist.bookmarkedAt
                        else -> null
                    }
                }

            MixSortType.NAME ->
                otherItems.sortedWith(
                    compareBy(collator) { item ->
                        when (item) {
                            is Album -> item.album.title
                            is Artist -> item.artist.name
                            else -> ""
                        }
                    },
                )

            MixSortType.LAST_UPDATED ->
                otherItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.lastUpdateTime
                        is Artist -> item.artist.lastUpdateTime
                        else -> null
                    }
                }
        }.let { list ->
            if (sortDescending) list.asReversed() else list
        }

    val customPlaylistMode = playlistSortType == PlaylistSortType.CUSTOM
    val canEnterReorderMode = customPlaylistMode && selectedTagIds.isEmpty()
    var reorderEnabled by rememberSaveable { mutableStateOf(false) }
    val canReorderPlaylists = canEnterReorderMode && reorderEnabled
    val listHeaderItems =
        2 +
            (if (showLiked) 1 else 0) +
            (if (showDownloaded) 1 else 0) +
            (if (showTop) 1 else 0) +
            (if (showCached) 1 else 0)
    val reorderableState = Unit

    LaunchedEffect(canEnterReorderMode) {
        if (!canEnterReorderMode) reorderEnabled = false
    }

    val allItems =
        if (customPlaylistMode) {
            (visiblePlaylists + sortedOtherItems).distinctBy { it.id }
        } else {
            val combinedItems = (albums.value + artist.value + visiblePlaylists).distinctBy { it.id }
            when (sortType) {
                MixSortType.CREATE_DATE ->
                    combinedItems.sortedBy { item ->
                        when (item) {
                            is Album -> item.album.bookmarkedAt
                            is Artist -> item.artist.bookmarkedAt
                            is Playlist -> item.playlist.createdAt
                            else -> null
                        }
                    }

                MixSortType.NAME ->
                    combinedItems.sortedWith(
                        compareBy(collator) { item ->
                            when (item) {
                                is Album -> item.album.title
                                is Artist -> item.artist.name
                                is Playlist -> item.playlist.name
                                else -> ""
                            }
                        },
                    )

                MixSortType.LAST_UPDATED ->
                    combinedItems.sortedBy { item ->
                        when (item) {
                            is Album -> item.album.lastUpdateTime
                            is Artist -> item.artist.lastUpdateTime
                            is Playlist -> item.playlist.lastUpdateTime
                            else -> null
                        }
                    }
            }.let { list ->
                if (sortDescending) list.asReversed() else list
            }
        }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
         if (ytmSync) {
             withContext(Dispatchers.IO) {
                 // viewModel.syncAllLibrary()
             }
         }
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp),
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { type ->
                    when (type) {
                        MixSortType.CREATE_DATE -> "Date Added"
                        MixSortType.LAST_UPDATED -> "Last Updated"
                        MixSortType.NAME -> "Name"
                    }
                },
            )
            if (canEnterReorderMode) {
                IconButton(
                    onClick = { reorderEnabled = !reorderEnabled },
                    modifier = Modifier.padding(start = 6.dp),
                ) {
                    Icon(
                        painter = painterResource(if (reorderEnabled) R.drawable.ic_volume_off else R.drawable.ic_volume_up), // Using volume off/up as lock replacement since lock doesn't exist
                        contentDescription = null,
                    )
                }
            }

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp),
            ) {
                Icon(
                    painter = painterResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.drawable.ic_list
                            LibraryViewType.GRID -> R.drawable.ic_album
                        },
                    ),
                    contentDescription = null,
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when (viewType) {
            LibraryViewType.LIST ->
                LazyColumn(
                    state = lazyListState,
                    contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    if (showLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            com.omnitune.app.ui.component.LibraryPlaylistListItem(
                                playlist = likedPlaylist,
                                modifier = Modifier.animateItem(),
                                onClick = { navController.navigate("auto_playlist/liked") }
                            )
                        }
                    }

                    if (showDownloaded) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            com.omnitune.app.ui.component.LibraryPlaylistListItem(
                                playlist = downloadPlaylist,
                                modifier = Modifier.animateItem(),
                                onClick = { navController.navigate("auto_playlist/downloaded") }
                            )
                        }
                    }

                    if (showTop) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            com.omnitune.app.ui.component.LibraryPlaylistListItem(
                                playlist = topPlaylist,
                                modifier = Modifier.animateItem(),
                                onClick = { navController.navigate("top_playlist/$topSize") }
                            )
                        }
                    }

                    if (showCached) {
                        item(
                            key = "cachePlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            com.omnitune.app.ui.component.LibraryPlaylistListItem(
                                playlist = cachePlaylist,
                                modifier = Modifier.animateItem(),
                                onClick = { navController.navigate("cache_playlist/cached") }
                            )
                        }
                    }

                    if (customPlaylistMode) {
                        if (canReorderPlaylists) {
                            items(
                                items = visiblePlaylists,
                                key = { it.id },
                                contentType = { CONTENT_TYPE_PLAYLIST },
                            ) { item ->
                                com.omnitune.app.ui.component.LibraryPlaylistListItem(
                                    playlist = item,
                                    modifier = Modifier.animateItem(),
                                    onClick = { navController.navigate("playlist/${item.id}") }
                                )
                            }
                        } else {
                            items(
                                items = visiblePlaylists,
                                key = { it.id },
                                contentType = { CONTENT_TYPE_PLAYLIST },
                            ) { item ->
                                com.omnitune.app.ui.component.LibraryPlaylistListItem(
                                    playlist = item,
                                    modifier = Modifier.animateItem(),
                                    onClick = { navController.navigate("playlist/${item.id}") }
                                )
                            }
                        }


                        items(
                            items = sortedOtherItems.distinctBy { it.id },
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { item ->
                            when (item) {
                                is Artist -> {
                                    ArtistListItem(
                                        artist = item,
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_more_vert),
                                                    contentDescription = null,
                                                )
                                            }
                                        },
                                        modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("artist/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                    )
                                }

                                is Album -> {
                                    AlbumListItem(
                                        album = item,
                                        isActive = item.id == mediaMetadata?.album?.id,
                                        isPlaying = isPlaying,
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_more_vert),
                                                    contentDescription = null,
                                                )
                                            }
                                        },
                                        modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("album/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                    )
                                }

                                else -> {}
                            }
                        }
                    } else {
                        items(
                            items = allItems,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { item ->
                            when (item) {
                                is Playlist -> {
                                    com.omnitune.app.ui.component.LibraryPlaylistListItem(
                                        playlist = item,
                                        modifier = Modifier.animateItem(),
                                        onClick = { navController.navigate("playlist/${item.id}") }
                                    )
                                }

                                is Artist -> {
                                    ArtistListItem(
                                        artist = item,
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_more_vert),
                                                    contentDescription = null,
                                                )
                                            }
                                        },
                                        modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("artist/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                    )
                                }

                                is Album -> {
                                    AlbumListItem(
                                        album = item,
                                        isActive = item.id == mediaMetadata?.album?.id,
                                        isPlaying = isPlaying,
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_more_vert),
                                                    contentDescription = null,
                                                )
                                            }
                                        },
                                        modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("album/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                    )
                                }

                                else -> {}
                            }
                        }
                    }
                }

            LibraryViewType.GRID ->
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                    GridCells.Adaptive(
                        minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                    ),
                    contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    if (showLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            com.omnitune.app.ui.component.LibraryPlaylistListItem(
                                playlist = likedPlaylist,
                                modifier = Modifier.animateItem(),
                                onClick = { navController.navigate("auto_playlist/liked") }
                            )
                        }
                    }

                    if (showDownloaded) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            com.omnitune.app.ui.component.LibraryPlaylistListItem(
                                playlist = downloadPlaylist,
                                modifier = Modifier.animateItem(),
                                onClick = { navController.navigate("auto_playlist/downloaded") }
                            )
                        }
                    }

                    if (showTop) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            com.omnitune.app.ui.component.LibraryPlaylistListItem(
                                playlist = topPlaylist,
                                modifier = Modifier.animateItem(),
                                onClick = { navController.navigate("top_playlist/$topSize") }
                            )
                        }
                    }

                    if (showCached) {
                        item(
                            key = "cachePlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            com.omnitune.app.ui.component.LibraryPlaylistListItem(
                                playlist = cachePlaylist,
                                modifier = Modifier.animateItem(),
                                onClick = { navController.navigate("cache_playlist/cached") }
                            )
                        }
                    }

                    items(
                        items = allItems,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                com.omnitune.app.ui.component.LibraryPlaylistListItem(
                                    playlist = item,
                                    modifier = Modifier.animateItem(),
                                    onClick = { navController.navigate("playlist/${item.id}") }
                                )
                            }

                            is Artist -> {
                                ArtistListItem(
                                    artist = item,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("artist/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            is Album -> {
                                AlbumListItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("album/${item.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }

                            else -> {}
                        }
                    }
                }
        }
    }
}
