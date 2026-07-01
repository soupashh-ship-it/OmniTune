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
import com.omnitune.app.db.entities.SongSkipEntity
import com.omnitune.app.playback.DownloadUtil
import com.omnitune.app.ui.utils.resize
import com.omnitune.app.utils.reportException
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.SongItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class HomeDiscoveryUiState(
    val recentSongs: List<EventWithSong> = emptyList(),
    val carouselItems: List<HomeCarouselItem> = emptyList(),
    val quickPicks: List<QuickPickItem> = emptyList(),
    val searchSection: HomeSection = HomeDefaultCatalog.freshDiscovery,
    val downloadSection: HomeSection = HomeSection(id = "downloads", title = "Offline Songs"),
    val librarySection: HomeSection = HomeSection(id = "library", title = "Library and Favorites"),
    val personalizedSections: List<HomeSection> = emptyList(),
    val providerSections: List<HomeSection> = emptyList(),
    val communitySections: List<HomeSection> = emptyList(),
    val exploreSections: List<HomeSection> = emptyList(),
    val shelfSections: List<HomeSection> = HomeDefaultCatalog.shelves,
    val moodChips: List<MoodChip> = HomeDefaultCatalog.moodChips,
    val genreChips: List<MoodChip> = HomeDefaultCatalog.genreGrid,
    val playAllSongs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val isProviderLoading: Boolean = true,
)

private const val HOME_THUMBNAIL_WORKERS = 2

private data class HomeSignalBundle(
    val events: List<EventWithSong>,
    val quickPickSongs: List<Song>,
    val likedSongs: List<Song>,
    val librarySongs: List<Song>,
    val mostPlayedSongs: List<Song>,
    val searchHistory: List<SearchHistory>,
    val forgottenFavorites: List<Song>,
    val skips: List<SongSkipEntity>,
)

private data class HomeThumbnailPreview(
    val thumbnailUrls: List<String> = emptyList(),
    val state: HomeHydrationState = HomeHydrationState.None,
)

@UnstableApi
@HiltViewModel
class HomeDiscoveryViewModel @Inject constructor(
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil,
    private val homeFeedRepository: HomeFeedRepository,
) : ViewModel() {

    private val downloadSongs = MutableStateFlow<List<Song>>(emptyList())
    private val providerFeed = MutableStateFlow(HomeProviderFeed())
    private val isProviderLoading = MutableStateFlow(true)
    private val thumbnailPreviews = MutableStateFlow<Map<String, HomeThumbnailPreview>>(emptyMap())
    private val hydrationRequests = Channel<HomeThumbnailRequest>(Channel.UNLIMITED)
    private val requestedThumbnailIds = ConcurrentHashMap.newKeySet<String>()

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

    private data class BaseHomeSignalBundle(
        val events: List<EventWithSong>,
        val quickPickSongs: List<Song>,
        val likedSongs: List<Song>,
        val librarySongs: List<Song>,
        val mostPlayedSongs: List<Song>,
        val searchHistory: List<SearchHistory>,
    )

    private val baseHomeSignals = combine(
        database.events(),
        database.quickPicks(),
        database.likedSongs(SongSortType.CREATE_DATE, descending = true),
        database.songsByCreateDateAsc(),
        database.searchHistory(),
    ) { events, quickPickSongs, likedSongs, librarySongs, searchHistory ->
        BaseHomeSignalBundle(
            events = events,
            quickPickSongs = quickPickSongs,
            likedSongs = likedSongs,
            librarySongs = librarySongs,
            mostPlayedSongs = emptyList(),
            searchHistory = searchHistory,
        )
    }

    private val homeSignals = combine(
        baseHomeSignals,
        database.mostPlayedSongs(fromTimeStamp = 0L, limit = 20),
        database.forgottenFavorites(),
        database.getAllSkips(),
    ) { base, mostPlayedSongs, forgottenFavorites, skips ->
        HomeSignalBundle(
            events = base.events,
            quickPickSongs = base.quickPickSongs,
            likedSongs = base.likedSongs,
            librarySongs = base.librarySongs,
            mostPlayedSongs = mostPlayedSongs,
            searchHistory = base.searchHistory,
            forgottenFavorites = forgottenFavorites,
            skips = skips,
        )
    }

    val uiState: StateFlow<HomeDiscoveryUiState> = combine(homeSignals, downloadSongs, providerFeed, isProviderLoading, thumbnailPreviews) { bundle, offlineSongs, providerFeed, providerLoading, previews ->
        buildState(
            events = bundle.events,
            quickPickSongs = bundle.quickPickSongs,
            likedSongs = bundle.likedSongs,
            librarySongs = bundle.librarySongs.reversed(),
            mostPlayedSongs = bundle.mostPlayedSongs,
            searchHistory = bundle.searchHistory,
            forgottenFavorites = bundle.forgottenFavorites,
            skips = bundle.skips,
            offlineSongs = offlineSongs,
            providerFeed = providerFeed,
            isProviderLoading = providerLoading,
            previews = previews,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeDiscoveryUiState())

    init {
        downloadUtil.downloadManager.addListener(downloadListener)
        refreshDownloads()
        loadProviderFeed()
        startThumbnailHydrationWorker()
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
        mostPlayedSongs: List<Song>,
        searchHistory: List<SearchHistory>,
        forgottenFavorites: List<Song>,
        skips: List<SongSkipEntity>,
        offlineSongs: List<Song>,
        providerFeed: HomeProviderFeed,
        isProviderLoading: Boolean,
        previews: Map<String, HomeThumbnailPreview>,
    ): HomeDiscoveryUiState {
        val recentSongs = events.take(20)
        val recentSongItems = recentSongs.map { it.song }.distinctBy { it.id }
        val quickSongs = mergeSongs(quickPickSongs, recentSongItems, likedSongs, offlineSongs, mostPlayedSongs, librarySongs).take(18)
        val hasSearchHistory = searchHistory.any { it.query.isNotBlank() }
        val searchItems = buildSearchItems(searchHistory).map { it.withHydration(previews) }
        val realQuickPicks = quickSongs.take(12).map { it.toQuickPickItem() }
        val playAllSongs = mergeSongs(recentSongItems, quickSongs, likedSongs, offlineSongs).take(50)
        val recommendations = HomeRecommendationEngine.build(
            HomeRecommendationInput(
                events = events,
                quickPickSongs = quickPickSongs,
                likedSongs = likedSongs,
                librarySongs = librarySongs,
                downloadedSongs = offlineSongs,
                forgottenFavorites = forgottenFavorites,
                skips = skips,
            ),
        )

        return HomeDiscoveryUiState(
            recentSongs = recentSongs,
            carouselItems = buildCarouselItems(quickSongs, likedSongs, offlineSongs, providerFeed, previews),
            quickPicks = realQuickPicks,
            searchSection = HomeSection(
                id = if (hasSearchHistory) "recent_searches" else HomeDefaultCatalog.freshDiscovery.id,
                title = if (hasSearchHistory) "Recent searches" else HomeDefaultCatalog.freshDiscovery.title,
                actionLabel = if (hasSearchHistory) "Search" else null,
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
            personalizedSections = recommendations.sections.map { section ->
                section.copy(items = section.items.map { it.withHydration(previews) })
            },
            providerSections = providerFeed.providerSections,
            communitySections = providerFeed.communitySections,
            exploreSections = providerFeed.exploreSections,
            shelfSections = HomeDefaultCatalog.shelves
                .map { section -> section.copy(items = section.items.map { it.withHydration(previews) }) },
            moodChips = (providerFeed.chips + HomeDefaultCatalog.moodChips).distinctBy { it.id },
            genreChips = (providerFeed.moodSections.flatMap { section ->
                section.items.map { item ->
                    MoodChip(
                        id = item.id,
                        label = item.title,
                        query = item.query ?: item.title,
                        artworkKey = item.artworkKey ?: item.id,
                        collectionType = item.collectionType,
                        actionType = item.actionType,
                        source = item.source,
                    )
                }
            } + HomeDefaultCatalog.genreGrid).distinctBy { it.id },
            playAllSongs = playAllSongs,
            isLoading = false,
            isProviderLoading = isProviderLoading,
        )
    }

    private fun buildCarouselItems(
        quickSongs: List<Song>,
        likedSongs: List<Song>,
        offlineSongs: List<Song>,
        providerFeed: HomeProviderFeed,
        previews: Map<String, HomeThumbnailPreview>,
    ): List<HomeCarouselItem> {
        val songItems = quickSongs.take(4).map { song ->
            song.toCarouselItem(song.title.ifBlank { "Quick pick" }, song.artists.joinToString(", ") { it.name }.ifBlank { "Ready to play" })
        } + listOfNotNull(
            likedSongs.firstOrNull()?.toCarouselItem("From your favorites", "Saved in your library"),
            offlineSongs.firstOrNull()?.toCarouselItem("Ready offline", "Downloaded song"),
        )
        val providerHeroItems = providerFeed.providerSections
            .asSequence()
            .flatMap { it.items.asSequence() }
            .mapNotNull { it.providerSong }
            .take(4)
            .map { it.toCarouselItem() }
            .toList()
        return (songItems + providerHeroItems + HomeDefaultCatalog.heroItems)
            .distinctBy { it.id }
            .take(8)
            .map { it.withHydration(previews) }
    }

    private fun buildSearchItems(searchHistory: List<SearchHistory>): List<PlaylistShelfItem> {
        val history = searchHistory
            .map { it.query.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(6)
        if (history.isEmpty()) return HomeDefaultCatalog.freshDiscovery.items

        return history.map { query ->
            PlaylistShelfItem(
                id = HomeDefaultCatalog.queryCollectionId(query),
                title = query.replaceFirstChar { it.titlecase() },
                subtitle = "From search history",
                query = query,
                collectionType = HomeCollectionType.TrendingSearch,
                actionType = HomeActionType.OPEN_COLLECTION,
                source = HomeCatalogSource.UserHistory,
            )
        }
    }

    private fun mergeSongs(vararg lists: List<Song>): List<Song> = lists.flatMap { it }.distinctBy { it.id }

    fun requestThumbnailHydration(request: HomeThumbnailRequest) {
        if (request.query.isBlank()) return
        val existing = thumbnailPreviews.value[request.id]?.state
        if (existing == HomeHydrationState.Loading ||
            existing == HomeHydrationState.Loaded ||
            existing == HomeHydrationState.Failed
        ) return
        if (!requestedThumbnailIds.add(request.id)) return
        hydrationRequests.trySend(request)
    }

    private fun startThumbnailHydrationWorker() {
        viewModelScope.launch {
            repeat(HOME_THUMBNAIL_WORKERS) {
                launch {
                    for (request in hydrationRequests) {
                        val existing = thumbnailPreviews.value[request.id]?.state
                        if (existing == HomeHydrationState.Loaded || existing == HomeHydrationState.Failed) continue
                        val preview = loadThumbnailPreview(request)
                        thumbnailPreviews.update { current -> current + (request.id to preview) }
                    }
                }
            }
        }
    }

    private fun loadProviderFeed() {
        viewModelScope.launch {
            isProviderLoading.value = true
            providerFeed.value = runCatching { homeFeedRepository.loadProviderFeed() }
                .onFailure(::reportException)
                .getOrDefault(HomeProviderFeed())
            isProviderLoading.value = false
        }
    }

    private suspend fun loadThumbnailPreview(request: HomeThumbnailRequest): HomeThumbnailPreview = withContext(Dispatchers.IO) {
        runCatching {
            YouTube.search(request.query, YouTube.SearchFilter.FILTER_SONG)
                .getOrThrow()
                .items
                .filterIsInstance<SongItem>()
                .mapNotNull { item -> item.thumbnail.takeIf { it.isNotBlank() }?.resize(544, 544) }
                .distinct()
                .take(if (request.collage) 4 else 1)
        }.fold(
            onSuccess = { thumbnails ->
                HomeThumbnailPreview(
                    thumbnailUrls = thumbnails,
                    state = if (thumbnails.isEmpty()) HomeHydrationState.Failed else HomeHydrationState.Loaded,
                )
            },
            onFailure = { error ->
                reportException(error)
                HomeThumbnailPreview(state = HomeHydrationState.Failed)
            },
        )
    }

    private fun HomeCarouselItem.withHydration(previews: Map<String, HomeThumbnailPreview>): HomeCarouselItem {
        if (source == HomeCatalogSource.UserData || source == HomeCatalogSource.ProviderBrowse || !thumbnailUrl.isNullOrBlank()) return this
        val preview = previews[id] ?: return this
        return copy(
            thumbnailUrl = preview.thumbnailUrls.firstOrNull(),
            thumbnailUrls = preview.thumbnailUrls,
            hydrationState = preview.state,
        )
    }

    private fun QuickPickItem.withHydration(previews: Map<String, HomeThumbnailPreview>): QuickPickItem {
        if (source == HomeCatalogSource.UserData || source == HomeCatalogSource.ProviderBrowse || !thumbnailUrl.isNullOrBlank()) return this
        val preview = previews[id] ?: return this
        return copy(
            thumbnailUrl = preview.thumbnailUrls.firstOrNull(),
            thumbnailUrls = preview.thumbnailUrls,
            hydrationState = preview.state,
        )
    }

    private fun PlaylistShelfItem.withHydration(previews: Map<String, HomeThumbnailPreview>): PlaylistShelfItem {
        if (source == HomeCatalogSource.UserData || source == HomeCatalogSource.ProviderBrowse || !thumbnailUrl.isNullOrBlank()) return this
        val preview = previews[id] ?: return this
        return copy(
            thumbnailUrl = preview.thumbnailUrls.firstOrNull(),
            thumbnailUrls = preview.thumbnailUrls,
            hydrationState = preview.state,
        )
    }

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

    private fun SongItem.toCarouselItem() = HomeCarouselItem(
        id = "provider_hero_$id",
        title = title.ifBlank { "Provider pick" },
        subtitle = artists.joinToString(", ") { it.name }.ifBlank { "Ready to play" },
        thumbnailUrl = thumbnail,
        providerSong = this,
        artworkKey = "provider_hero_$id",
        source = HomeCatalogSource.ProviderBrowse,
        actionType = HomeActionType.PLAY_TRACK,
    )
}
