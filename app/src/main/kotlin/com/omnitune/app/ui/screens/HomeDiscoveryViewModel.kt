/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import com.omnitune.app.constants.QuickPicks
import com.omnitune.app.constants.QuickPicksKey
import com.omnitune.app.constants.SongSortType
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.EventWithSong
import com.omnitune.app.db.entities.SearchHistory
import com.omnitune.app.db.entities.Song
import com.omnitune.app.db.entities.SongSkipEntity
import com.omnitune.app.playback.DownloadUtil
import com.omnitune.app.ui.utils.resize
import com.omnitune.app.utils.classifyProviderError
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.reportException
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.SongItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
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
    val playAllQuickPicks: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val isHydratingQuickPicks: Boolean = false,
    val isProviderLoading: Boolean = true,
    val providerError: String? = null,
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
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil,
    private val homeFeedRepository: HomeFeedRepository,
) : ViewModel() {

    private val downloadSongs = MutableStateFlow<List<Song>>(emptyList())
    private val providerFeed = MutableStateFlow(HomeProviderFeed())
    private val isProviderLoading = MutableStateFlow(true)
    private val thumbnailPreviews = MutableStateFlow<Map<String, HomeThumbnailPreview>>(emptyMap())
    private val _hydratedQuickPicks = MutableStateFlow<List<SongItem>>(emptyList())
    private val _isHydratingQuickPicks = MutableStateFlow(false)
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

    private val quickPicksMode: StateFlow<QuickPicks> = context.dataStore.data.map { prefs ->
        val value = prefs[QuickPicksKey] ?: QuickPicks.QUICK_PICKS.name
        try { QuickPicks.valueOf(value) } catch (_: Exception) { QuickPicks.QUICK_PICKS }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickPicks.QUICK_PICKS)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val effectiveQuickPicks: StateFlow<List<Song>> = quickPicksMode.flatMapLatest { mode ->
        when (mode) {
            QuickPicks.QUICK_PICKS -> database.quickPicks().map { it.shuffled() }
            QuickPicks.LAST_LISTEN -> database.events().flatMapLatest { events ->
                val lastSongId = events.firstOrNull()?.song?.id
                if (lastSongId != null) {
                    database.getRelatedSongs(lastSongId).flatMapLatest { related ->
                        if (related.isNotEmpty()) flowOf(related.shuffled()) else database.quickPicks().map { it.shuffled() }
                    }
                } else {
                    database.quickPicks().map { it.shuffled() }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val baseHomeSignals = combine(
        database.events(),
        effectiveQuickPicks,
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

    private val homeSignals: StateFlow<HomeSignalBundle> = combine(
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeSignalBundle(
        events = emptyList(),
        quickPickSongs = emptyList(),
        likedSongs = emptyList(),
        librarySongs = emptyList(),
        mostPlayedSongs = emptyList(),
        searchHistory = emptyList(),
        forgottenFavorites = emptyList(),
        skips = emptyList(),
    ))

    private val _uiState = MutableStateFlow(HomeDiscoveryUiState())
    val uiState: StateFlow<HomeDiscoveryUiState> = _uiState.asStateFlow()

    private fun rebuildUiState() {
        val bundle = homeSignals.value
        val offlineSongs = downloadSongs.value
        val feed = providerFeed.value
        val loading = isProviderLoading.value
        val previews = thumbnailPreviews.value
        val hydrated = _hydratedQuickPicks.value
        val isHydrating = _isHydratingQuickPicks.value
        _uiState.value = buildState(
            events = bundle.events,
            quickPickSongs = bundle.quickPickSongs,
            likedSongs = bundle.likedSongs,
            librarySongs = bundle.librarySongs.reversed(),
            mostPlayedSongs = bundle.mostPlayedSongs,
            searchHistory = bundle.searchHistory,
            forgottenFavorites = bundle.forgottenFavorites,
            skips = bundle.skips,
            offlineSongs = offlineSongs,
            providerFeed = feed,
            isProviderLoading = loading,
            previews = previews,
            hydratedQuickPicks = hydrated,
            isHydratingQuickPicks = isHydrating,
        )
    }

    init {
        viewModelScope.launch {
            combine(
                homeSignals, downloadSongs, providerFeed, isProviderLoading, thumbnailPreviews,
            ) { _, _, _, _, _ -> rebuildUiState() }.collect { }
        }
        viewModelScope.launch {
            combine(_hydratedQuickPicks, _isHydratingQuickPicks) { _, _ -> rebuildUiState() }.collect { }
        }
        downloadUtil.downloadManager.addListener(downloadListener)
        refreshDownloads()
        loadProviderFeed()
        startThumbnailHydrationWorker()
        startQuickPickHydration()
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
        hydratedQuickPicks: List<SongItem> = emptyList(),
        isHydratingQuickPicks: Boolean = false,
    ): HomeDiscoveryUiState {
        val recentSongs = events.take(20)
        val quickSongs = quickPickSongs.take(80)
        val hasSearchHistory = searchHistory.any { it.query.isNotBlank() }
        val searchItems = buildSearchItems(searchHistory).map { it.withHydration(previews) }
        val providerPlayableItems = providerFeed.providerSections.flatMap { section ->
            section.items.mapNotNull { item -> item.providerSong?.let { item to it } }
        } + providerFeed.exploreSections.flatMap { section ->
            section.items.mapNotNull { item -> item.providerSong?.let { item to it } }
        } + providerFeed.communitySections.flatMap { section ->
            section.items.mapNotNull { item -> item.providerSong?.let { item to it } }
        }

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

        fun QuickPickItem.dedupKey(): String {
            val cleanTitle = title.lowercase().trim()
                .replace(Regex("\\s*\\(.*?\\)\\s*$"), "")
                .replace(Regex("\\s*-\\s*(official|full|lyrical|video|audio|song).*$"), "")
                .trim()
            return "$cleanTitle|${subtitle.lowercase().trim().take(40)}"
        }

        val realQuickPicks = run {
            val recommendedItems = recommendations.topSongs
                .take(40)
                .map { it.toQuickPickItem() }
            val recommendedKeys = recommendedItems.map { it.dedupKey() }.toSet()
            if (recommendedItems.isNotEmpty()) {
                val remaining = (quickSongs.map { it.toQuickPickItem() } +
                    hydratedQuickPicks.map { it.toQuickPickItem() })
                    .filter { it.dedupKey() !in recommendedKeys }
                    .distinctBy { it.id }
                    .take(80 - recommendedItems.size)
                (recommendedItems + remaining).distinctBy { it.dedupKey() }
            } else if (quickSongs.isNotEmpty()) {
                quickSongs.map { it.toQuickPickItem() }
            } else if (providerPlayableItems.isNotEmpty()) {
                providerPlayableItems.take(80).map { (shelfItem, _) ->
                    shelfItem.toQuickPickItem()
                }
            } else if (hydratedQuickPicks.isNotEmpty()) {
                hydratedQuickPicks.map { it.toQuickPickItem() }
            } else {
                emptyList()
            }
        }
        val shuffledQuickPicks = run {
            val anchorCount = minOf(5, realQuickPicks.size)
            val anchors = realQuickPicks.take(anchorCount)
            val rest = realQuickPicks.drop(anchorCount)
            val seed = (System.currentTimeMillis() / (30 * 60 * 1000L)).toInt()
            val shuffledRest = if (rest.isNotEmpty()) rest.shuffled(kotlin.random.Random(seed)) else rest
            anchors + shuffledRest
        }
        val quickPickSongsForPlayAll = shuffledQuickPicks.mapNotNull { it.song }

        val providerMoodChips = providerFeed.chips
        val providerGenreChips = providerFeed.moodSections.flatMap { section ->
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
        }.distinctBy { it.id }

        return HomeDiscoveryUiState(
            recentSongs = recentSongs,
            carouselItems = buildCarouselItems(shuffledQuickPicks, likedSongs, offlineSongs, providerFeed, previews),
            quickPicks = shuffledQuickPicks,
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
            personalizedSections = recommendations.sections
                .map { section ->
                    section.copy(items = section.items.map { it.withHydration(previews) })
                },
            providerSections = providerFeed.providerSections.withHydration(previews),
            communitySections = providerFeed.communitySections.withHydration(previews),
            exploreSections = providerFeed.exploreSections.withHydration(previews),
            shelfSections = HomeDefaultCatalog.shelves
                .map { section -> section.copy(items = section.items.map { it.withHydration(previews) }) },
            // Keep the reference-derived mood entry points stable while retaining
            // provider categories after them.  Each chip still opens a real query or
            // provider browse route; this only controls discovery ordering.
            moodChips = (HomeDefaultCatalog.moodChips + providerMoodChips).distinctBy { it.id },
            genreChips = providerGenreChips,
            playAllQuickPicks = quickPickSongsForPlayAll,
            isLoading = false,
            isHydratingQuickPicks = isHydratingQuickPicks,
            isProviderLoading = isProviderLoading,
            providerError = providerFeed.failures.takeIf { it.isNotEmpty() }?.joinToString(". "),
        )
    }

    private fun List<HomeSection>.withHydration(previews: Map<String, HomeThumbnailPreview>): List<HomeSection> =
        map { section -> section.copy(items = section.items.map { it.withHydration(previews) }) }

    private fun buildCarouselItems(
        quickPicks: List<QuickPickItem>,
        likedSongs: List<Song>,
        offlineSongs: List<Song>,
        providerFeed: HomeProviderFeed,
        previews: Map<String, HomeThumbnailPreview>,
    ): List<HomeCarouselItem> {
        return quickPicks.take(20).map { it.toCarouselItem().withHydration(previews) }
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
            val result = runCatching { homeFeedRepository.loadProviderFeed() }
            result.onSuccess { feed ->
                providerFeed.value = feed
            }.onFailure { error ->
                reportException(error)
                val pe = classifyProviderError(error)
                providerFeed.value = HomeProviderFeed(failures = listOf(pe.message))
            }
            isProviderLoading.value = false
        }
    }

    private fun SongItem.toQuickPickItem() = QuickPickItem(
        id = "qp_hydrated_$id",
        title = title.ifBlank { "Quick pick" },
        subtitle = artists.joinToString(", ") { it.name }.ifBlank { "From provider" },
        thumbnailUrl = thumbnail,
        providerSong = this,
        artworkKey = "qp_hydrated_$id",
        source = HomeCatalogSource.ProviderBrowse,
        actionType = HomeActionType.PLAY_TRACK,
    )

    private fun startQuickPickHydration() {
        viewModelScope.launch {
            try {
                isProviderLoading.first { !it }
                _isHydratingQuickPicks.value = true

                val feed = providerFeed.value

                val hydratableItems = listOf(
                    feed.providerSections,
                    feed.communitySections,
                    feed.exploreSections,
                    feed.moodSections,
                ).flatMap { sections ->
                    sections.flatMap { section ->
                        section.items.filter { item ->
                            item.providerSong == null &&
                                (item.actionType == HomeActionType.OPEN_PLAYLIST ||
                                 item.actionType == HomeActionType.OPEN_BROWSE ||
                                 item.actionType == HomeActionType.OPEN_ALBUM)
                        }
                    }
                }

                val allSongs = LinkedHashSet<SongItem>()

                // Search YouTube for songs from your top artists + liked/recent tracks first
                val seedSongs = homeSignals.value.let { bundle ->
                    (bundle.events.map { it.song } + bundle.likedSongs)
                        .distinctBy { it.id }
                        .take(3)
                }
                val recommendationInput = HomeRecommendationInput(
                    events = homeSignals.value.events,
                    quickPickSongs = homeSignals.value.quickPickSongs,
                    likedSongs = homeSignals.value.likedSongs,
                    librarySongs = homeSignals.value.librarySongs,
                    downloadedSongs = downloadSongs.value,
                    forgottenFavorites = homeSignals.value.forgottenFavorites,
                    skips = homeSignals.value.skips,
                )
                val topArtists = HomeRecommendationEngine.build(recommendationInput).topArtists
                val searchResults = searchSongsForSeeds(seedSongs, topArtists, 80)
                allSongs.addAll(searchResults)

                // Fill remaining with provider collections
                if (allSongs.size < 80) {
                    val itemsToHydrate = hydratableItems.distinctBy { it.id }.take(12)
                    for (item in itemsToHydrate) {
                        if (allSongs.size >= 80) break
                        val songs = hydrateProviderCollection(item)
                        allSongs.addAll(songs.take(80 - allSongs.size))
                    }
                }

                _hydratedQuickPicks.value = allSongs.take(80).toList()
            } finally {
                _isHydratingQuickPicks.value = false
            }
        }
    }

    private suspend fun searchSongsForSeeds(seedSongs: List<Song>, topArtists: List<HomeArtistSeed>, maxResults: Int): List<SongItem> {
        if (maxResults <= 0) return emptyList()

        val results = LinkedHashSet<SongItem>()
        val queries = buildList {
            // Top artists from recommendation engine
            topArtists.forEach { artist ->
                if (artist.name.isNotBlank()) add("${artist.name} songs")
            }
            // Artist-based searches for each seed song
            seedSongs.forEach { song ->
                song.artists.firstOrNull()?.name?.takeIf { it.isNotBlank() }?.let { artist ->
                    add("$artist songs")
                }
            }
            // Song + artist queries
            seedSongs.forEach { song ->
                val artist = song.artists.firstOrNull()?.name.orEmpty()
                if (song.title.isNotBlank() && artist.isNotBlank()) {
                    add("${song.title} $artist")
                }
            }
        }.distinct().take(6)

        for (query in queries) {
            if (results.size >= maxResults) break
            val songs = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                .getOrNull()
                ?.items
                ?.filterIsInstance<SongItem>()
                .orEmpty()
            results.addAll(songs.take(maxResults - results.size))
        }

        return results.toList()
    }

    private suspend fun hydrateProviderCollection(item: PlaylistShelfItem): List<SongItem> {
        return when (item.actionType) {
            HomeActionType.OPEN_PLAYLIST -> {
                val parts = item.id.removePrefix("provider:")
                    .split("||")
                val playlistId = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return emptyList()
                YouTube.playlist(playlistId).getOrNull()?.songs.orEmpty()
            }
            HomeActionType.OPEN_BROWSE -> {
                val parts = item.id.removePrefix("provider:")
                    .split("||")
                val browseId = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return emptyList()
                val params = parts.getOrNull(4)?.takeIf { it.isNotBlank() }
                resolveBrowseSongs(browseId, params)
            }
            HomeActionType.OPEN_ALBUM -> {
                val parts = item.id.removePrefix("provider:")
                    .split("||")
                val albumId = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return emptyList()
                YouTube.album(albumId).getOrNull()?.songs.orEmpty()
            }
            else -> emptyList()
        }.distinctBy { it.id }.take(80)
    }

    private suspend fun resolveBrowseSongs(browseId: String, params: String?): List<SongItem> {
        val browseItems = runCatching {
            YouTube.browse(browseId, params).getOrThrow().items.flatMap { it.items }
        }.getOrDefault(emptyList())

        val songs = mutableListOf<SongItem>()
        songs.addAll(browseItems.filterIsInstance<SongItem>())

        for (ytItem in browseItems) {
            if (songs.size >= 20) break
            when (ytItem) {
                is com.omnitune.innertube.models.PlaylistItem -> {
                    YouTube.playlist(ytItem.id).getOrNull()?.songs?.let {
                        songs.addAll(it.filterNot { existing -> songs.any { s -> s.id == existing.id } })
                    }
                }
                is com.omnitune.innertube.models.AlbumItem -> {
                    YouTube.album(ytItem.browseId).getOrNull()?.songs?.let {
                        songs.addAll(it.filterNot { existing -> songs.any { s -> s.id == existing.id } })
                    }
                }
                else -> {}
            }
        }

        return songs.take(80)
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
        if (source == HomeCatalogSource.UserData || !thumbnailUrl.isNullOrBlank()) return this
        val preview = previews[id] ?: return this
        return copy(
            thumbnailUrl = preview.thumbnailUrls.firstOrNull(),
            thumbnailUrls = preview.thumbnailUrls,
            hydrationState = preview.state,
        )
    }

    private fun QuickPickItem.withHydration(previews: Map<String, HomeThumbnailPreview>): QuickPickItem {
        if (source == HomeCatalogSource.UserData || !thumbnailUrl.isNullOrBlank()) return this
        val preview = previews[id] ?: return this
        return copy(
            thumbnailUrl = preview.thumbnailUrls.firstOrNull(),
            thumbnailUrls = preview.thumbnailUrls,
            hydrationState = preview.state,
        )
    }

    private fun PlaylistShelfItem.withHydration(previews: Map<String, HomeThumbnailPreview>): PlaylistShelfItem {
        if (source == HomeCatalogSource.UserData || !thumbnailUrl.isNullOrBlank()) return this
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

    private fun PlaylistShelfItem.toQuickPickItem() = QuickPickItem(
        id = "qp_provider_${id}",
        title = title.ifBlank { "Quick pick" },
        subtitle = subtitle.ifBlank { "From provider" },
        thumbnailUrl = thumbnailUrl,
        providerSong = providerSong,
        query = query,
        artworkKey = artworkKey ?: "qp_provider_$id",
        source = HomeCatalogSource.ProviderBrowse,
        actionType = HomeActionType.PLAY_TRACK,
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

    private fun QuickPickItem.toCarouselItem() = HomeCarouselItem(
        id = "qp_carousel_${id}",
        title = title.ifBlank { "Quick pick" },
        subtitle = subtitle.ifBlank { "Recommended for you" },
        thumbnailUrl = thumbnailUrl,
        thumbnailUrls = thumbnailUrls,
        song = song,
        providerSong = providerSong,
        query = query,
        artworkKey = artworkKey ?: "qp_carousel_$id",
        collectionType = collectionType,
        maxItems = maxItems,
        hydrationState = hydrationState,
        actionType = actionType,
        source = source,
    )

    private fun PlaylistShelfItem.toCarouselItem() = HomeCarouselItem(
        id = id,
        title = title.ifBlank { "Featured pick" },
        subtitle = subtitle.ifBlank {
            if (providerSong != null || song != null) "Ready to play" else "Open in OmniTune"
        },
        thumbnailUrl = thumbnailUrl,
        thumbnailUrls = thumbnailUrls,
        song = song,
        providerSong = providerSong,
        query = query,
        artworkKey = artworkKey ?: id,
        collectionType = collectionType,
        maxItems = maxItems,
        hydrationState = hydrationState,
        actionType = actionType,
        source = source,
    )
}
