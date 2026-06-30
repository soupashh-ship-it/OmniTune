/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.runtime.Immutable
import com.omnitune.app.db.entities.Song

enum class HomeActionType {
    Search,
    PlaySong,
    OpenCollection,
    OpenArtist,
}

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
    val actionType: HomeActionType = HomeActionType.OpenCollection,
    val source: HomeCatalogSource = HomeCatalogSource.CuratedDefault,
)

@Immutable
data class HomeCarouselItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val thumbnailUrls: List<String> = emptyList(),
    val song: Song? = null,
    val query: String? = null,
    val artworkKey: String? = null,
    val collectionType: HomeCollectionType = HomeCollectionType.Playlist,
    val maxItems: Int = 50,
    val hydrationState: HomeHydrationState = HomeHydrationState.None,
    val actionType: HomeActionType = if (song != null) HomeActionType.PlaySong else HomeActionType.OpenCollection,
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
    val query: String? = null,
    val artworkKey: String? = null,
    val collectionType: HomeCollectionType = HomeCollectionType.QuickPick,
    val maxItems: Int = 50,
    val hydrationState: HomeHydrationState = HomeHydrationState.None,
    val actionType: HomeActionType = if (song != null) HomeActionType.PlaySong else HomeActionType.OpenCollection,
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
    val query: String? = null,
    val artworkKey: String? = null,
    val collectionType: HomeCollectionType = HomeCollectionType.Playlist,
    val maxItems: Int = 50,
    val hydrationState: HomeHydrationState = HomeHydrationState.None,
    val actionType: HomeActionType = if (song != null) HomeActionType.PlaySong else HomeActionType.OpenCollection,
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
    val actionType: HomeActionType = HomeActionType.OpenCollection,
)
