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
}

enum class HomeCatalogSource {
    UserData,
    CuratedDefault,
}

enum class HomeSectionType {
    Hero,
    QuickPicks,
    Shelf,
    MoodGrid,
}

@Immutable
data class MoodChip(
    val id: String,
    val label: String,
    val query: String,
    val artworkKey: String = id,
)

@Immutable
data class HomeCarouselItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val song: Song? = null,
    val query: String? = null,
    val artworkKey: String? = null,
    val actionType: HomeActionType = if (song != null) HomeActionType.PlaySong else HomeActionType.Search,
    val source: HomeCatalogSource = if (song != null) HomeCatalogSource.UserData else HomeCatalogSource.CuratedDefault,
)

@Immutable
data class QuickPickItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val song: Song? = null,
    val query: String? = null,
    val artworkKey: String? = null,
    val actionType: HomeActionType = if (song != null) HomeActionType.PlaySong else HomeActionType.Search,
    val source: HomeCatalogSource = if (song != null) HomeCatalogSource.UserData else HomeCatalogSource.CuratedDefault,
)

@Immutable
data class PlaylistShelfItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val song: Song? = null,
    val query: String? = null,
    val artworkKey: String? = null,
    val actionType: HomeActionType = if (song != null) HomeActionType.PlaySong else HomeActionType.Search,
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
