/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.models.YTItem
import javax.inject.Inject
import javax.inject.Singleton

data class HomeProviderFeed(
    val chips: List<MoodChip> = emptyList(),
    val providerSections: List<HomeSection> = emptyList(),
    val communitySections: List<HomeSection> = emptyList(),
    val exploreSections: List<HomeSection> = emptyList(),
    val moodSections: List<HomeSection> = emptyList(),
)

@Singleton
class HomeFeedRepository @Inject constructor() {
    suspend fun loadProviderFeed(): HomeProviderFeed {
        val home = YouTube.home().getOrNull()
        val explore = YouTube.explore().getOrNull()
        val moodGroups = YouTube.moodAndGenres().getOrNull().orEmpty()

        val homeSections = home?.sections.orEmpty()
            .mapIndexedNotNull { index, section ->
                val items = section.items.mapNotNull { it.toShelfItem(section.title) }
                if (items.isEmpty()) return@mapIndexedNotNull null
                HomeSection(
                    id = "provider_home_${index}_${section.title.stableId()}",
                    title = section.title,
                    actionLabel = section.label,
                    items = items,
                )
            }

        val community = homeSections.filter { section ->
            section.title.contains("community", ignoreCase = true) ||
                section.title.contains("playlist", ignoreCase = true) ||
                section.items.any { it.actionType == HomeActionType.OPEN_PLAYLIST }
        }

        val provider = homeSections.filterNot { it.id in community.map(HomeSection::id) }

        val newReleases = explore?.newReleaseAlbums.orEmpty()
            .mapNotNull { it.toShelfItem("New releases") }
            .takeIf { it.isNotEmpty() }
            ?.let { items ->
                HomeSection(
                    id = "provider_new_releases",
                    title = "New releases",
                    items = items,
                )
            }

        val exploreMoods = explore?.moodAndGenres.orEmpty()
            .map { item ->
                PlaylistShelfItem(
                    id = HomeDefaultCatalog.providerCollectionId(
                        kind = "browse",
                        providerId = item.endpoint.browseId,
                        title = item.title,
                        subtitle = "Browse",
                        params = item.endpoint.params,
                    ),
                    title = item.title,
                    subtitle = "Browse",
                    query = item.title,
                    artworkKey = "browse_${item.endpoint.browseId}",
                    collectionType = HomeCollectionType.Mood,
                    source = HomeCatalogSource.ProviderBrowse,
                    actionType = HomeActionType.OPEN_BROWSE,
                )
            }
            .takeIf { it.isNotEmpty() }
            ?.let { items ->
                HomeSection(
                    id = "provider_explore_moods",
                    title = "Explore moods",
                    items = items,
                )
            }

        val moodSections = moodGroups.take(3).mapIndexedNotNull { index, group ->
            val items = group.items.map { item ->
                PlaylistShelfItem(
                    id = HomeDefaultCatalog.providerCollectionId(
                        kind = "browse",
                        providerId = item.endpoint.browseId,
                        title = item.title,
                        subtitle = group.title,
                        params = item.endpoint.params,
                    ),
                    title = item.title,
                    subtitle = group.title,
                    query = item.title,
                    artworkKey = "mood_${item.endpoint.browseId}",
                    collectionType = HomeCollectionType.Mood,
                    source = HomeCatalogSource.ProviderBrowse,
                    actionType = HomeActionType.OPEN_BROWSE,
                )
            }
            if (items.isEmpty()) return@mapIndexedNotNull null
            HomeSection(
                id = "provider_moods_${index}_${group.title.stableId()}",
                title = group.title,
                items = items,
            )
        }

        val chips = home?.chips.orEmpty().mapNotNull { chip ->
            val endpoint = chip.endpoint ?: return@mapNotNull null
            MoodChip(
                id = HomeDefaultCatalog.providerCollectionId(
                    kind = "browse",
                    providerId = endpoint.browseId,
                    title = chip.title,
                    subtitle = "Home",
                    params = endpoint.params,
                ),
                label = chip.title,
                query = chip.title,
                artworkKey = "chip_${endpoint.browseId}",
                source = HomeCatalogSource.ProviderBrowse,
                actionType = HomeActionType.OPEN_BROWSE,
            )
        }

        return HomeProviderFeed(
            chips = chips,
            providerSections = provider,
            communitySections = community,
            exploreSections = listOfNotNull(newReleases, exploreMoods),
            moodSections = moodSections,
        )
    }

    private fun YTItem.toShelfItem(sectionTitle: String): PlaylistShelfItem? = when (this) {
        is SongItem -> PlaylistShelfItem(
            id = "provider_song_$id",
            title = title,
            subtitle = artists.joinToString(", ") { it.name }.ifBlank { "Song" },
            thumbnailUrl = thumbnail,
            providerSong = this,
            artworkKey = "provider_song_$id",
            source = HomeCatalogSource.ProviderBrowse,
            actionType = HomeActionType.PLAY_TRACK,
        )

        is AlbumItem -> PlaylistShelfItem(
            id = HomeDefaultCatalog.providerCollectionId("album", browseId, title, "Album"),
            title = title,
            subtitle = artists?.joinToString(", ") { it.name }.orEmpty().ifBlank { "Album" },
            thumbnailUrl = thumbnail,
            artworkKey = "provider_album_$browseId",
            collectionType = HomeCollectionType.NewReleases,
            source = HomeCatalogSource.ProviderBrowse,
            actionType = HomeActionType.OPEN_ALBUM,
        )

        is ArtistItem -> PlaylistShelfItem(
            id = HomeDefaultCatalog.providerCollectionId("artist", id, title, "Artist"),
            title = title,
            subtitle = "Artist",
            thumbnailUrl = thumbnail,
            artworkKey = "provider_artist_$id",
            collectionType = HomeCollectionType.ArtistMix,
            source = HomeCatalogSource.ProviderBrowse,
            actionType = HomeActionType.OPEN_ARTIST,
        )

        is PlaylistItem -> PlaylistShelfItem(
            id = HomeDefaultCatalog.providerCollectionId("playlist", id, title, sectionTitle),
            title = title,
            subtitle = author?.name ?: songCountText ?: "Playlist",
            thumbnailUrl = thumbnail,
            artworkKey = "provider_playlist_$id",
            collectionType = HomeCollectionType.Playlist,
            source = HomeCatalogSource.ProviderBrowse,
            actionType = HomeActionType.OPEN_PLAYLIST,
        )
    }

    private fun String.stableId(): String =
        lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { hashCode().toString() }
}
