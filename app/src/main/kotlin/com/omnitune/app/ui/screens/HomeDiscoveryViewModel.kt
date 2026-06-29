/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import com.omnitune.app.constants.SongSortType
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.EventWithSong
import com.omnitune.app.db.entities.SearchHistory
import com.omnitune.app.db.entities.Song
import com.omnitune.app.playback.DownloadUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HomeDiscoveryUiState(
    val recentSongs: List<EventWithSong> = emptyList(),
    val carouselItems: List<HomeCarouselItem> = emptyList(),
    val quickPicks: List<QuickPickItem> = emptyList(),
    val quickPicksExploreQuery: String = HomeDefaultCatalog.quickPicks.first().query.orEmpty(),
    val searchSection: HomeSection = HomeSection(id = "searches", title = "New or Trending Searches"),
    val downloadSection: HomeSection = HomeSection(id = "downloads", title = "Offline Songs"),
    val librarySection: HomeSection = HomeSection(id = "library", title = "Library and Favorites"),
    val shelfSections: List<HomeSection> = HomeDefaultCatalog.shelves,
    val moodChips: List<MoodChip> = HomeDefaultCatalog.moodChips,
    val genreChips: List<MoodChip> = HomeDefaultCatalog.genreGrid,
    val playAllSongs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
)

private val homeFallbackSearches = listOf(
    "new music",
    "trending songs",
    "fresh pop",
    "indie hits",
    "lofi focus",
    "acoustic covers",
)

private data class HomeSignalBundle(
    val events: List<EventWithSong>,
    val quickPickSongs: List<Song>,
    val likedSongs: List<Song>,
    val librarySongs: List<Song>,
    val searchHistory: List<SearchHistory>,
)

@UnstableApi
@HiltViewModel
class HomeDiscoveryViewModel @Inject constructor(
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil,
) : ViewModel() {

    private val downloadSongs = MutableStateFlow<List<Song>>(emptyList())

    private val downloadListener = object : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?,
        ) {
            refreshDownloads()
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            refreshDownloads()
        }
    }

    private val homeSignals = combine(
        database.events(),
        database.quickPicks(),
        database.likedSongs(SongSortType.CREATE_DATE, descending = true),
        database.songsByCreateDateAsc(),
        database.searchHistory(),
    ) { events, quickPickSongs, likedSongs, librarySongs, searchHistory ->
        HomeSignalBundle(
            events = events,
            quickPickSongs = quickPickSongs,
            likedSongs = likedSongs,
            librarySongs = librarySongs,
            searchHistory = searchHistory,
        )
    }

    val uiState: StateFlow<HomeDiscoveryUiState> = combine(homeSignals, downloadSongs) { bundle, offlineSongs ->
        buildState(
            events = bundle.events,
            quickPickSongs = bundle.quickPickSongs,
            likedSongs = bundle.likedSongs,
            librarySongs = bundle.librarySongs.reversed(),
            searchHistory = bundle.searchHistory,
            offlineSongs = offlineSongs,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeDiscoveryUiState())

    init {
        downloadUtil.downloadManager.addListener(downloadListener)
        refreshDownloads()
    }

    override fun onCleared() {
        super.onCleared()
        downloadUtil.downloadManager.removeListener(downloadListener)
    }

    private fun refreshDownloads() {
        viewModelScope.launch {
            downloadSongs.value = withContext(Dispatchers.IO) {
                val cursor = downloadUtil.downloadManager.downloadIndex.getDownloads(Download.STATE_COMPLETED)
                val ids = mutableListOf<String>()
                try {
                    while (cursor.moveToNext()) {
                        val download = cursor.download
                        if (downloadUtil.isPlayable(download)) ids.add(download.request.id)
                    }
                } finally {
                    cursor.close()
                }
                if (ids.isEmpty()) emptyList() else database.getSongsByIds(ids).sortedBy { ids.indexOf(it.id) }
            }
        }
    }

    private fun buildState(
        events: List<EventWithSong>,
        quickPickSongs: List<Song>,
        likedSongs: List<Song>,
        librarySongs: List<Song>,
        searchHistory: List<SearchHistory>,
        offlineSongs: List<Song>,
    ): HomeDiscoveryUiState {
        val recentSongs = events.take(20)
        val recentSongItems = recentSongs.map { it.song }.distinctBy { it.id }
        val quickSongs = mergeSongs(quickPickSongs, recentSongItems, likedSongs, offlineSongs, librarySongs).take(18)
        val searchItems = buildSearchItems(searchHistory)
        val realQuickPicks = quickSongs.take(12).map { it.toQuickPickItem() }
        val curatedQuickPicks = HomeDefaultCatalog.quickPicks
            .filterNot { curated -> realQuickPicks.any { it.title.equals(curated.title, ignoreCase = true) } }
        val playAllSongs = mergeSongs(recentSongItems, quickSongs, likedSongs, offlineSongs).take(50)

        return HomeDiscoveryUiState(
            recentSongs = recentSongs,
            carouselItems = buildCarouselItems(recentSongItems, likedSongs, offlineSongs),
            quickPicks = (realQuickPicks + curatedQuickPicks).take(12),
            quickPicksExploreQuery = curatedQuickPicks.firstOrNull()?.query
                ?: HomeDefaultCatalog.quickPicks.first().query.orEmpty(),
            searchSection = HomeSection(
                id = "searches",
                title = "New or Trending Searches",
                actionLabel = "Search",
                items = searchItems,
            ),
            downloadSection = HomeSection(
                id = "downloads",
                title = "Offline Songs",
                actionLabel = if (offlineSongs.isNotEmpty()) "Downloads" else null,
                items = offlineSongs.take(10).map { it.toShelfItem("download") },
            ),
            librarySection = HomeSection(
                id = "library",
                title = "Library and Favorites",
                actionLabel = if (likedSongs.isNotEmpty() || librarySongs.isNotEmpty()) "Library" else null,
                items = mergeSongs(likedSongs, librarySongs).take(12).map { it.toShelfItem("library") },
            ),
            shelfSections = HomeDefaultCatalog.shelves,
            playAllSongs = playAllSongs,
            isLoading = false,
        )
    }

    private fun buildCarouselItems(
        recentSongs: List<Song>,
        likedSongs: List<Song>,
        offlineSongs: List<Song>,
    ): List<HomeCarouselItem> {
        val songItems = listOfNotNull(
            recentSongs.firstOrNull()?.toCarouselItem("Pick up where you left off", "Recently played"),
            likedSongs.firstOrNull()?.toCarouselItem("From your favorites", "Saved in your library"),
            offlineSongs.firstOrNull()?.toCarouselItem("Ready offline", "Downloaded song"),
        )
        return (songItems + HomeDefaultCatalog.heroItems).distinctBy { it.id }.take(8)
    }

    private fun buildSearchItems(searchHistory: List<SearchHistory>): List<PlaylistShelfItem> {
        val history = searchHistory
            .map { it.query.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(6)
        val queries = (history + homeFallbackSearches).distinct().take(8)
        return queries.mapIndexed { index, query ->
            PlaylistShelfItem(
                id = "query_${query.hashCode()}_$index",
                title = query.replaceFirstChar { it.titlecase() },
                subtitle = if (query in history) "From search history" else "Seed search",
                query = query,
            )
        }
    }

    private fun mergeSongs(vararg lists: List<Song>): List<Song> = lists.flatMap { it }.distinctBy { it.id }

    private fun Song.toQuickPickItem() = QuickPickItem(
        id = id,
        title = song.title.ifBlank { "Unknown track" },
        subtitle = artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
        thumbnailUrl = song.thumbnailUrl,
        song = this,
        artworkKey = "song_$id",
        source = HomeCatalogSource.UserData,
    )

    private fun Song.toShelfItem(prefix: String) = PlaylistShelfItem(
        id = "${prefix}_$id",
        title = song.title.ifBlank { "Unknown track" },
        subtitle = artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
        thumbnailUrl = song.thumbnailUrl,
        song = this,
        artworkKey = "${prefix}_$id",
        source = HomeCatalogSource.UserData,
    )

    private fun Song.toCarouselItem(titlePrefix: String, source: String) = HomeCarouselItem(
        id = "hero_$id",
        title = titlePrefix,
        subtitle = "$source - ${song.title.ifBlank { "Unknown track" }}",
        thumbnailUrl = song.thumbnailUrl,
        song = this,
        artworkKey = "hero_$id",
        source = HomeCatalogSource.UserData,
    )
}
