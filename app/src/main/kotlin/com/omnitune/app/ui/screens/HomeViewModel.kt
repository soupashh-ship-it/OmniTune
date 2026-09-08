package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.models.*
import com.omnitune.app.models.HomeSection
import com.omnitune.app.models.HomeSectionType
import com.omnitune.app.models.HomeItem

import com.omnitune.innertube.YouTube
import com.omnitune.innertube.pages.HomePage
import com.omnitune.innertube.models.AlbumItem as InnerAlbumItem
import com.omnitune.innertube.models.ArtistItem as InnerArtistItem
import com.omnitune.innertube.models.PlaylistItem as InnerPlaylistItem
import com.omnitune.innertube.models.SongItem as InnerSongItem
import com.omnitune.innertube.models.YTItem as InnerYTItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val userName: String? = "Music Lover",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentSource: SongSource = SongSource.YOUTUBE,
    val selectedMood: String? = null,
    val isLoggedIn: Boolean = true,
    val loadMorePage: Int = 0,
    val hasReachedEnd: Boolean = false,
    val detectedMood: String? = null,
    val isForYouBannerVisible: Boolean = true,
    val homeSectionsVisibility: Set<String> = setOf(
        "greeting", "mood_chips", "for_you_banner", "recommendations",
        "quick_picks", "youtube_sections", "personalized", "genres", "charts"
    )
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val database: MusicDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private var homeContinuation: String? = null

    init {
        loadHomeContent()
    }

    private fun loadHomeContent(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            loadData(forceRefresh)
            loadLocalRecommendations()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadData(forceRefresh = true)
            loadLocalRecommendations()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadLocalRecommendations() {
        try {
            val localSongs = withContext(Dispatchers.IO) {
                database.songsByRowIdAsc().first()
            }

            if (localSongs.isNotEmpty()) {
                val recs = localSongs.take(12).map { it.toSuvSong() }
                _uiState.update { it.copy(recommendations = recs) }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun loadData(forceRefresh: Boolean = false) {
        if (_uiState.value.homeSections.isEmpty() || forceRefresh) {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }

        try {
            val homeResult = withContext(Dispatchers.IO) {
                YouTube.home()
            }

            homeResult.onSuccess { homePage ->
                homeContinuation = homePage.continuation
                val parsedSections = homePage.sections.toHomeSections()

                val quickPicks = parsedSections.firstOrNull { it.type == HomeSectionType.QuickPicks }
                    ?.items?.mapNotNull { (it as? HomeItem.SongItem)?.song }
                    ?: emptyList()

                val personalized = parsedSections.filter { it.type == HomeSectionType.PersonalizedMix }
                val genres = parsedSections.filter { it.type == HomeSectionType.GenreCarousel }

                _uiState.update {
                    it.copy(
                        homeSections = parsedSections,
                        filteredSections = parsedSections,
                        recommendations = if (quickPicks.isNotEmpty()) quickPicks else it.recommendations,
                        personalizedSections = personalized,
                        genreSections = genres,
                        isLoading = false,
                        hasReachedEnd = homePage.continuation == null,
                        loadMorePage = 0,
                        error = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (it.homeSections.isEmpty()) error.message ?: "Network error" else null
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = if (it.homeSections.isEmpty()) e.message ?: "Failed to load home" else null
                )
            }
        }
    }

    fun loadMore() {
        val continuation = homeContinuation ?: return
        if (_uiState.value.isLoadingMore || _uiState.value.hasReachedEnd) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val homeResult = withContext(Dispatchers.IO) {
                YouTube.home(continuation = continuation)
            }
            homeResult.onSuccess { page ->
                homeContinuation = page.continuation
                _uiState.update { current ->
                    val nextSections = page.sections.toHomeSections(startIndex = current.homeSections.size)
                    val combinedSections = (current.homeSections + nextSections).distinctBy { it.title to it.items.map(HomeItem::id) }
                    val filteredSections = current.selectedMood
                        ?.let { mood -> combinedSections.filteredForMood(mood) }
                        ?.takeIf { it.isNotEmpty() }
                        ?: combinedSections
                    current.copy(
                        homeSections = combinedSections,
                        filteredSections = filteredSections,
                        personalizedSections = combinedSections.filter { it.type == HomeSectionType.PersonalizedMix },
                        genreSections = combinedSections.filter { it.type == HomeSectionType.GenreCarousel },
                        loadMorePage = current.loadMorePage + 1,
                        hasReachedEnd = page.continuation == null || nextSections.isEmpty(),
                        isLoadingMore = false,
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMore = false) }
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
            is InnerSongItem -> HomeItem.SongItem(toSuvSong())
            is InnerAlbumItem -> HomeItem.AlbumItem(toSuvAlbum())
            is InnerArtistItem -> HomeItem.ArtistItem(toSuvArtist())
            is InnerPlaylistItem -> HomeItem.PlaylistItem(toSuvPlaylistDisplayItem())
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
}
