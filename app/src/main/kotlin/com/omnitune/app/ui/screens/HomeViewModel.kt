package com.omnitune.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.constants.AccountChannelHandleKey
import com.omnitune.app.constants.AccountEmailKey
import com.omnitune.app.constants.AccountNameKey
import com.omnitune.app.constants.InnerTubeCookieKey
import com.omnitune.app.content.HomeContentRequestGate
import com.omnitune.app.content.LanguageScopedHomeContinuationStore
import com.omnitune.app.content.LanguageScopedHomeSectionCache
import com.omnitune.app.content.MusicContentDiscoveryPolicy
import com.omnitune.app.content.MusicContentLanguage
import com.omnitune.app.content.MusicContentPreferenceRepository
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.models.*
import com.omnitune.app.models.HomeSection
import com.omnitune.app.models.HomeSectionType
import com.omnitune.app.models.HomeItem
import com.omnitune.app.utils.SecurePreferenceCipher
import com.omnitune.app.utils.dataStore
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.pages.HomePage
import com.omnitune.innertube.models.AlbumItem as InnerAlbumItem
import com.omnitune.innertube.models.ArtistItem as InnerArtistItem
import com.omnitune.innertube.models.PlaylistItem as InnerPlaylistItem
import com.omnitune.innertube.models.SongItem as InnerSongItem
import com.omnitune.innertube.models.YTItem as InnerYTItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.ZoneOffset
import java.util.Locale
import javax.inject.Inject

sealed class HomeEvent {
    data class ShowAddToPlaylistSheet(val song: Song) : HomeEvent()
    data object ScrollToTop : HomeEvent()
    data object Refresh : HomeEvent()
}

data class HomeUiState(
    val homeSections: List<HomeSection> = emptyList(),
    val filteredSections: List<HomeSection> = emptyList(),
    val recommendations: List<Song> = emptyList(),
    val personalizedSections: List<HomeSection> = emptyList(),
    val genreSections: List<HomeSection> = emptyList(),
    val contextSections: List<HomeSection> = emptyList(),
    val moreSections: List<HomeSection> = emptyList(),
    val recommendedArtists: List<RecommendedArtist> = emptyList(),
    val recommendedTracks: List<RecommendedTrack> = emptyList(),
    val userAvatarUrl: String? = null,
    val userName: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val paginationError: String? = null,
    val currentSource: SongSource = SongSource.YOUTUBE,
    val selectedMood: String? = null,
    val isLoggedIn: Boolean = false,
    val loadMorePage: Int = 0,
    val hasReachedEnd: Boolean = false,
    val detectedMood: String? = null,
    val isForYouBannerVisible: Boolean = true,
    val homeSectionsVisibility: Set<String> = setOf(
        "greeting", "mood_chips", "for_you_banner", "recommendations",
        "quick_picks", "youtube_sections", "personalized", "genres", "charts"
    )
)

object HomeLocalRecommendationMapper {
    fun recommendations(
        songs: List<com.omnitune.app.db.entities.Song>,
        limit: Int = 12,
    ): List<Song> = songs
        .filter { it.song.title.isNotBlank() }
        .sortedWith(
            compareByDescending<com.omnitune.app.db.entities.Song> { score(it) }
                .thenByDescending { latestKnownActivityEpochSecond(it) }
                .thenBy { it.song.title.lowercase(Locale.ROOT) }
                .thenBy { it.song.id }
        )
        .take(limit)
        .map { it.toPresentationSong() }

    internal fun score(song: com.omnitune.app.db.entities.Song): Long {
        val entity = song.song
        val playMinutes = (entity.totalPlayTime / 60_000L).coerceAtMost(1_000L)
        return (playMinutes * 20L) +
            if (entity.liked) 10_000L else 0L +
            if (entity.dateDownload != null || entity.downloadState == 2) 2_500L else 0L +
            if (entity.inLibrary != null) 100L else 0L
    }

    internal fun latestKnownActivityEpochSecond(song: com.omnitune.app.db.entities.Song): Long =
        listOfNotNull(
            song.song.likedDate,
            song.song.dateDownload,
            song.song.inLibrary,
            song.song.dateModified,
            song.song.date,
        )
            .maxOrNull()
            ?.toEpochSecond(ZoneOffset.UTC)
            ?: 0L
}

object HomePaginationStateReducer {
    fun success(
        current: HomeUiState,
        nextSections: List<HomeSection>,
        nextContinuation: String?,
    ): HomeUiState {
        val combinedSections = (current.homeSections + nextSections)
            .distinctBy { it.title to it.items.map(HomeItem::id) }
        val filteredSections = current.selectedMood
            ?.let { mood -> combinedSections.filteredForMood(mood) }
            ?.takeIf { it.isNotEmpty() }
            ?: combinedSections

        return current.copy(
            homeSections = combinedSections,
            filteredSections = filteredSections,
            personalizedSections = combinedSections.filter { it.type == HomeSectionType.PersonalizedMix },
            genreSections = combinedSections.filter { it.type == HomeSectionType.GenreCarousel },
            loadMorePage = current.loadMorePage + 1,
            hasReachedEnd = nextContinuation == null || nextSections.isEmpty(),
            isLoadingMore = false,
            paginationError = null,
        )
    }

    fun failure(current: HomeUiState, message: String?): HomeUiState =
        current.copy(
            isLoadingMore = false,
            paginationError = message?.takeIf { it.isNotBlank() } ?: "Couldn't load more music",
        )
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val musicContentPreferenceRepository: MusicContentPreferenceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private val homeRequestGate = HomeContentRequestGate()
    private val homeContinuations = LanguageScopedHomeContinuationStore()
    private val homeSectionCache = LanguageScopedHomeSectionCache()
    private var activeMusicLanguage: MusicContentLanguage = MusicContentLanguage.Default

    init {
        observeAccountState()
        observeMusicContentLanguage()
    }

    private fun observeAccountState() {
        viewModelScope.launch {
            context.dataStore.data
                .map { prefs ->
                    AccountSessionStateMapper.fromStoredAccount(
                        plainCookie = SecurePreferenceCipher.decryptOrPlain(prefs[InnerTubeCookieKey]),
                        accountName = prefs[AccountNameKey],
                        accountEmail = prefs[AccountEmailKey],
                        channelHandle = prefs[AccountChannelHandleKey],
                    )
                }
                .distinctUntilChanged()
                .collect { account ->
                    _uiState.update {
                        it.copy(
                            isLoggedIn = account.isLoggedIn,
                            userName = account.userName,
                            userAvatarUrl = account.userAvatarUrl,
                        )
                    }
                }
        }
    }

    private fun observeMusicContentLanguage() {
        viewModelScope.launch {
            musicContentPreferenceRepository.selectedLanguage.collectLatest { language ->
                val changed = activeMusicLanguage != language
                activeMusicLanguage = language
                if (changed) {
                    homeRequestGate.invalidate()
                    homeContinuations.resetAll()
                    homeSectionCache.resetAll()
                    resetRemoteHomeState()
                }
                loadData(language = language, forceRefresh = changed)
                loadLocalRecommendations()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val language = musicContentPreferenceRepository.currentLanguage()
            activeMusicLanguage = language
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                loadData(language = language, forceRefresh = true)
                loadLocalRecommendations()
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun resetRemoteHomeState() {
        _uiState.update {
            it.copy(
                homeSections = emptyList(),
                filteredSections = emptyList(),
                personalizedSections = emptyList(),
                genreSections = emptyList(),
                contextSections = emptyList(),
                moreSections = emptyList(),
                selectedMood = null,
                isLoading = true,
                error = null,
                paginationError = null,
                loadMorePage = 0,
                hasReachedEnd = false,
            )
        }
    }

    private suspend fun loadLocalRecommendations() {
        try {
            val localSongs = withContext(Dispatchers.IO) {
                database.songsByRowIdAsc().first()
            }

            if (localSongs.isNotEmpty()) {
                val recs = HomeLocalRecommendationMapper.recommendations(localSongs)
                _uiState.update {
                    if (it.recommendations.isEmpty()) it.copy(recommendations = recs) else it
                }
            }
        } catch (e: Exception) {
            logFailure(e, "Failed to load local home recommendations")
        }
    }

    private suspend fun loadData(
        language: MusicContentLanguage,
        forceRefresh: Boolean = false,
    ) {
        val cachedSections = if (forceRefresh) emptyList() else homeSectionCache.sectionsFor(language)
        if (cachedSections.isNotEmpty()) {
            applyHomeSections(
                sections = cachedSections,
                continuation = homeContinuations.continuationFor(language),
            )
            return
        }

        val request = homeRequestGate.begin(language)
        if (_uiState.value.homeSections.isEmpty() || forceRefresh) {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }

        try {
            val homePayload = withContext(Dispatchers.IO) {
                musicContentPreferenceRepository.applyLanguage(language)
                val seededSections = loadLanguageSeedSections(language)
                val homePage = YouTube.home().getOrThrow()
                seededSections to homePage
            }

            if (!homeRequestGate.accepts(request)) return

            val (seededSections, homePage) = homePayload
            homeContinuations.setContinuation(language, homePage.continuation)
            val parsedSections = MusicContentDiscoveryPolicy.mergeHomeSections(
                language = language,
                providerSections = homePage.sections.toHomeSections(startIndex = seededSections.size),
                seededSections = seededSections,
            )
            homeSectionCache.put(language, parsedSections)

            applyHomeSections(
                sections = parsedSections,
                continuation = homePage.continuation,
            )
        } catch (e: Exception) {
            logFailure(e, "Failed to load home")
            if (!homeRequestGate.accepts(request)) return
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = if (it.homeSections.isEmpty()) e.message ?: "Failed to load home" else null
                )
            }
        }
    }

    private fun applyHomeSections(
        sections: List<HomeSection>,
        continuation: String?,
    ) {
        val quickPicks = sections.firstOrNull { it.type == HomeSectionType.QuickPicks }
            ?.items?.mapNotNull { (it as? HomeItem.SongItem)?.song }
            ?: emptyList()

        val personalized = sections.filter { it.type == HomeSectionType.PersonalizedMix }
        val genres = sections.filter { it.type == HomeSectionType.GenreCarousel }

        _uiState.update {
            it.copy(
                homeSections = sections,
                filteredSections = sections,
                recommendations = if (quickPicks.isNotEmpty()) quickPicks else it.recommendations,
                personalizedSections = personalized,
                genreSections = genres,
                isLoading = false,
                hasReachedEnd = continuation == null,
                loadMorePage = 0,
                error = null,
                paginationError = null,
            )
        }
    }

    private suspend fun loadLanguageSeedSections(language: MusicContentLanguage): List<HomeSection> {
        if (language == MusicContentLanguage.AUTOMATIC) return emptyList()

        val songItems = searchHomeItems(
            queries = MusicContentDiscoveryPolicy.songDiscoveryQueries(language),
            filter = YouTube.SearchFilter.FILTER_SONG,
        )
            .filterIsInstance<InnerSongItem>()
            .map { item -> HomeItem.SongItem(item.toPresentationSong()) }

        val playlistItems = searchHomeItems(
            queries = MusicContentDiscoveryPolicy.playlistDiscoveryQueries(language),
            filter = YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST,
        )
            .filterIsInstance<InnerPlaylistItem>()
            .map { item -> HomeItem.PlaylistItem(item.toPresentationPlaylistDisplayItem()) }

        val artistItems = searchHomeItems(
            queries = MusicContentDiscoveryPolicy.artistDiscoveryQueries(language),
            filter = YouTube.SearchFilter.FILTER_ARTIST,
        )
            .filterIsInstance<InnerArtistItem>()
            .map { item -> HomeItem.ArtistItem(item.toPresentationArtist()) }

        return buildList {
            if (songItems.isNotEmpty()) {
                add(
                    HomeSection(
                        title = MusicContentDiscoveryPolicy.songSectionTitle(language),
                        items = songItems,
                        type = HomeSectionType.QuickPicks,
                        id = "music_language_${language.name.lowercase(Locale.ROOT)}_songs",
                    )
                )
            }
            if (playlistItems.isNotEmpty()) {
                add(
                    HomeSection(
                        title = MusicContentDiscoveryPolicy.playlistSectionTitle(language),
                        items = playlistItems,
                        type = HomeSectionType.CommunityCarousel,
                        id = "music_language_${language.name.lowercase(Locale.ROOT)}_playlists",
                    )
                )
            }
            if (artistItems.isNotEmpty()) {
                add(
                    HomeSection(
                        title = MusicContentDiscoveryPolicy.artistSectionTitle(language),
                        items = artistItems,
                        type = HomeSectionType.GenreCarousel,
                        id = "music_language_${language.name.lowercase(Locale.ROOT)}_artists",
                    )
                )
            }
        }
    }

    private suspend fun searchHomeItems(
        queries: List<String>,
        filter: YouTube.SearchFilter,
    ): List<InnerYTItem> {
        val items = mutableListOf<InnerYTItem>()
        queries.take(2).forEach { query ->
            val result = YouTube.search(query, filter)
            result.onSuccess { page ->
                items += page.items
            }.onFailure { error ->
                logFailure(error, "Failed to load language seed content")
            }
        }
        return items
            .distinctBy { item -> item.id }
            .take(12)
    }

    fun loadMore() {
        val language = activeMusicLanguage
        val continuation = homeContinuations.continuationFor(language) ?: return
        if (_uiState.value.isLoadingMore || _uiState.value.hasReachedEnd) return

        val request = homeRequestGate.begin(language)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, paginationError = null) }
            try {
                val homeResult = withContext(Dispatchers.IO) {
                    musicContentPreferenceRepository.applyLanguage(language)
                    YouTube.home(continuation = continuation)
                }
                if (!homeRequestGate.accepts(request)) return@launch
                homeResult.onSuccess { page ->
                    homeContinuations.setContinuation(language, page.continuation)
                    _uiState.update { current ->
                        val nextSections = page.sections.toHomeSections(startIndex = current.homeSections.size)
                        val nextState = HomePaginationStateReducer.success(
                            current = current,
                            nextSections = nextSections,
                            nextContinuation = page.continuation,
                        )
                        homeSectionCache.put(language, nextState.homeSections)
                        nextState
                    }
                }.onFailure { error ->
                    if (!homeRequestGate.accepts(request)) return@onFailure
                    _uiState.update { current ->
                        HomePaginationStateReducer.failure(current, error.message)
                    }
                }
            } catch (e: Exception) {
                logFailure(e, "Failed to load more home content")
                if (!homeRequestGate.accepts(request)) return@launch
                _uiState.update { current ->
                    HomePaginationStateReducer.failure(current, e.message)
                }
            } finally {
                if (homeRequestGate.accepts(request)) {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }

    fun onMoodSelected(mood: String) {
        val currentMood = _uiState.value.selectedMood
        if (currentMood == mood) {
            _uiState.update {
                it.copy(
                    selectedMood = null,
                    filteredSections = it.homeSections
                )
            }
        } else {
            _uiState.update { it.copy(selectedMood = mood) }
            fetchMoodContent(mood)
        }
    }

    private fun fetchMoodContent(mood: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val moodSections = _uiState.value.homeSections.filteredForMood(mood)
                _uiState.update {
                    it.copy(
                        filteredSections = if (moodSections.isNotEmpty()) moodSections else it.homeSections,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                logFailure(e, "Failed to filter home mood content")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onDismissForYouBanner() {
        _uiState.update { it.copy(isForYouBannerVisible = false) }
    }

    fun addToPlaylist(song: Song) {
        viewModelScope.launch {
            _events.emit(HomeEvent.ShowAddToPlaylistSheet(song))
        }
    }

    private fun logFailure(error: Throwable, message: String) {
        if (error is CancellationException) throw error
        Timber.w(error, message)
    }

    private fun List<HomePage.Section>.toHomeSections(startIndex: Int = 0): List<HomeSection> =
        mapIndexedNotNull { index, section ->
            val absoluteIndex = startIndex + index
            val sectionItems = section.items.mapNotNull { it.toHomeItem() }
            if (sectionItems.isEmpty()) {
                null
            } else {
                HomeSection(
                    title = section.title,
                    items = sectionItems,
                    type = section.toHomeSectionType(absoluteIndex, sectionItems),
                    actionLabel = section.label
                )
            }
        }

    private fun InnerYTItem.toHomeItem(): HomeItem? =
        when (this) {
            is InnerSongItem -> HomeItem.SongItem(toPresentationSong())
            is InnerAlbumItem -> HomeItem.AlbumItem(toPresentationAlbum())
            is InnerArtistItem -> HomeItem.ArtistItem(toPresentationArtist())
            is InnerPlaylistItem -> HomeItem.PlaylistItem(toPresentationPlaylistDisplayItem())
        }

    private fun HomePage.Section.toHomeSectionType(index: Int, sectionItems: List<HomeItem>): HomeSectionType =
        when {
            index == 0 && sectionItems.any { it is HomeItem.SongItem } -> HomeSectionType.QuickPicks
            title.contains("chart", ignoreCase = true) || title.contains("top", ignoreCase = true) -> HomeSectionType.ChartPodium
            title.contains("community", ignoreCase = true) || title.contains("playlist", ignoreCase = true) -> HomeSectionType.CommunityCarousel
            title.contains("mix", ignoreCase = true) || title.contains("radio", ignoreCase = true) -> HomeSectionType.PersonalizedMix
            index % 4 == 1 -> HomeSectionType.Grid
            index % 4 == 2 -> HomeSectionType.LargeCardWithList
            index % 4 == 3 -> HomeSectionType.VerticalList
            else -> HomeSectionType.HorizontalCarousel
        }

}

private fun List<HomeSection>.filteredForMood(mood: String): List<HomeSection> =
    filter { section ->
        section.title.contains(mood, ignoreCase = true) ||
            section.items.any { item ->
                when (item) {
                    is HomeItem.SongItem -> item.song.title.contains(mood, ignoreCase = true)
                    is HomeItem.PlaylistItem -> item.playlist.name.contains(mood, ignoreCase = true)
                    is HomeItem.AlbumItem -> item.album.title.contains(mood, ignoreCase = true)
                    is HomeItem.ArtistItem -> item.artist.name.contains(mood, ignoreCase = true)
                    is HomeItem.ExploreItem -> item.title.contains(mood, ignoreCase = true)
                }
            }
    }
