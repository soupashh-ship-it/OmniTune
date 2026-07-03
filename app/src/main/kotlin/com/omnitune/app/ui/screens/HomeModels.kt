/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.omnitune.app.db.entities.Song
import com.omnitune.innertube.models.SongItem

enum class HomeFeedAction {
    PLAY_TRACK,
    OPEN_COLLECTION,
    OPEN_ARTIST,
    OPEN_ALBUM,
    OPEN_PLAYLIST,
    OPEN_BROWSE,
    OPEN_SEARCH_ONLY_WHEN_EXPLICIT,
}

typealias HomeActionType = HomeFeedAction

enum class HomeCatalogSource {
    UserData,
    CuratedDefault,
    ProviderBrowse,
    UserHistory,
    Recommended,
}

enum class HomeCollectionType {
    ArtistMix,
    Mood,
    Genre,
    Playlist,
    TrendingSearch,
    QuickPick,
    NewReleases,
    ForYou,
    Related,
}

enum class HomeHydrationState {
    None,
    Loading,
    Loaded,
    Failed,
}

enum class HomeSectionType {
    Hero,
    QuickPicks,
    Shelf,
    MoodGrid,
}

@Immutable
data class HomeThumbnailRequest(
    val id: String,
    val query: String,
    val collage: Boolean = false,
)

@Immutable
data class MoodChip(
    val id: String,
    val label: String,
    val query: String,
    val artworkKey: String = id,
    val collectionType: HomeCollectionType = HomeCollectionType.Mood,
    val maxItems: Int = 50,
    val actionType: HomeActionType = HomeActionType.OPEN_COLLECTION,
    val source: HomeCatalogSource = HomeCatalogSource.CuratedDefault,
)

object GenreChipsHolder {
    var chips: List<MoodChip> by mutableStateOf(emptyList())
}

@Immutable
data class HomeCarouselItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val thumbnailUrls: List<String> = emptyList(),
    val song: Song? = null,
    val providerSong: SongItem? = null,
    val query: String? = null,
    val artworkKey: String? = null,
    val collectionType: HomeCollectionType = HomeCollectionType.Playlist,
    val maxItems: Int = 50,
    val hydrationState: HomeHydrationState = HomeHydrationState.None,
    val actionType: HomeActionType = if (song != null || providerSong != null) HomeActionType.PLAY_TRACK else HomeActionType.OPEN_COLLECTION,
    val source: HomeCatalogSource = if (song != null) HomeCatalogSource.UserData else HomeCatalogSource.CuratedDefault,
)

@Immutable
data class QuickPickItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val thumbnailUrls: List<String> = emptyList(),
    val song: Song? = null,
    val providerSong: SongItem? = null,
    val query: String? = null,
    val artworkKey: String? = null,
    val collectionType: HomeCollectionType = HomeCollectionType.QuickPick,
    val maxItems: Int = 50,
    val hydrationState: HomeHydrationState = HomeHydrationState.None,
    val actionType: HomeActionType = if (song != null || providerSong != null) HomeActionType.PLAY_TRACK else HomeActionType.OPEN_COLLECTION,
    val source: HomeCatalogSource = if (song != null) HomeCatalogSource.UserData else HomeCatalogSource.CuratedDefault,
)

@Immutable
data class PlaylistShelfItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val thumbnailUrls: List<String> = emptyList(),
    val song: Song? = null,
    val providerSong: SongItem? = null,
    val query: String? = null,
    val artworkKey: String? = null,
    val collectionType: HomeCollectionType = HomeCollectionType.Playlist,
    val maxItems: Int = 50,
    val hydrationState: HomeHydrationState = HomeHydrationState.None,
    val actionType: HomeActionType = if (song != null || providerSong != null) HomeActionType.PLAY_TRACK else HomeActionType.OPEN_COLLECTION,
    val source: HomeCatalogSource = if (song != null) HomeCatalogSource.UserData else HomeCatalogSource.CuratedDefault,
)

@Immutable
data class HomeSection(
    val id: String,
    val title: String,
    val actionLabel: String? = null,
    val items: List<PlaylistShelfItem> = emptyList(),
    val sectionType: HomeSectionType = HomeSectionType.Shelf,
)

@Immutable
data class HomeCollectionMetadata(
    val id: String,
    val title: String,
    val subtitle: String,
    val query: String,
    val collectionType: HomeCollectionType,
    val artworkKey: String,
    val maxItems: Int = 50,
    val source: HomeCatalogSource = HomeCatalogSource.CuratedDefault,
    val actionType: HomeActionType = HomeActionType.OPEN_COLLECTION,
    val providerId: String? = null,
    val browseParams: String? = null,
)
