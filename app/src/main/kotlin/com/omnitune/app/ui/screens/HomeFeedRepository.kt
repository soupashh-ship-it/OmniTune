/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.omnitune.app.constants.HomeProviderFeedCacheAtKey
import com.omnitune.app.constants.HomeProviderFeedCacheKey
import com.omnitune.app.utils.dataStore
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.models.YTItem
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class HomeProviderFeed(
    val chips: List<MoodChip> = emptyList(),
    val providerSections: List<HomeSection> = emptyList(),
    val communitySections: List<HomeSection> = emptyList(),
    val exploreSections: List<HomeSection> = emptyList(),
    val moodSections: List<HomeSection> = emptyList(),
    val fetchedAtEpochMillis: Long = 0L,
    val isCached: Boolean = false,
    val failures: List<String> = emptyList(),
)

@Singleton
class HomeFeedRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var cachedFeed: HomeProviderFeed? = null
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadProviderFeed(): HomeProviderFeed {
        val persistedCache = cachedFeed ?: readCachedFeed().also { cachedFeed = it }
        val failures = mutableListOf<String>()
        val (home, explore, moodGroups) = supervisorScope {
            val homeRequest = async { YouTube.home() }
            val exploreRequest = async { YouTube.explore() }
            val moodsRequest = async { YouTube.moodAndGenres() }
            Triple(
                homeRequest.await().getOrElse {
                    failures += "Home suggestions are unavailable"
                    null
                },
                exploreRequest.await().getOrElse {
                    failures += "Explore suggestions are unavailable"
                    null
                },
                moodsRequest.await().getOrElse {
                    failures += "Mood suggestions are unavailable"
                    emptyList()
                },
            )
        }

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

        val liveFeed = HomeProviderFeed(
            chips = chips,
            providerSections = provider,
            communitySections = community,
            exploreSections = listOfNotNull(newReleases, exploreMoods),
            moodSections = moodSections,
            fetchedAtEpochMillis = System.currentTimeMillis(),
            failures = failures.distinct(),
        )
        val cached = persistedCache
        val resolved = if (cached == null) {
            liveFeed
        } else {
            liveFeed.copy(
                chips = if (home == null) cached.chips else liveFeed.chips,
                providerSections = if (home == null) cached.providerSections else liveFeed.providerSections,
                communitySections = if (home == null) cached.communitySections else liveFeed.communitySections,
                exploreSections = if (explore == null) cached.exploreSections else liveFeed.exploreSections,
                moodSections = if (moodGroups.isEmpty() && failures.any { it.startsWith("Mood") }) cached.moodSections else liveFeed.moodSections,
                fetchedAtEpochMillis = if (failures.isEmpty()) liveFeed.fetchedAtEpochMillis else cached.fetchedAtEpochMillis,
                isCached = failures.isNotEmpty(),
            )
        }.deduplicated()

        if (resolved.providerSections.isNotEmpty() || resolved.communitySections.isNotEmpty() ||
            resolved.exploreSections.isNotEmpty() || resolved.moodSections.isNotEmpty()
        ) {
            cachedFeed = resolved.copy(isCached = false, failures = emptyList())
            if (failures.isEmpty()) persistCache(requireNotNull(cachedFeed))
        }
        return resolved
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

    private suspend fun readCachedFeed(): HomeProviderFeed? = runCatching {
        val preferences = context.dataStore.data.first()
        val encoded = preferences[HomeProviderFeedCacheKey].orEmpty()
        if (encoded.isBlank()) return@runCatching null
        json.decodeFromString<CachedHomeProviderFeed>(encoded).toFeed(
            preferences[HomeProviderFeedCacheAtKey] ?: 0L,
        )
    }.getOrNull()

    private suspend fun persistCache(feed: HomeProviderFeed) {
        runCatching {
            val cached = CachedHomeProviderFeed.from(feed)
            context.dataStore.edit { preferences ->
                preferences[HomeProviderFeedCacheKey] = json.encodeToString(cached)
                preferences[HomeProviderFeedCacheAtKey] = feed.fetchedAtEpochMillis
            }
        }
    }
}

@Serializable
private data class CachedHomeProviderFeed(
    val chips: List<CachedMoodChip> = emptyList(),
    val providerSections: List<CachedHomeSection> = emptyList(),
    val communitySections: List<CachedHomeSection> = emptyList(),
    val exploreSections: List<CachedHomeSection> = emptyList(),
    val moodSections: List<CachedHomeSection> = emptyList(),
) {
    fun toFeed(fetchedAtEpochMillis: Long): HomeProviderFeed = HomeProviderFeed(
        chips = chips.map { it.toMoodChip() },
        providerSections = providerSections.map { it.toSection() },
        communitySections = communitySections.map { it.toSection() },
        exploreSections = exploreSections.map { it.toSection() },
        moodSections = moodSections.map { it.toSection() },
        fetchedAtEpochMillis = fetchedAtEpochMillis,
        isCached = true,
    ).deduplicated()

    companion object {
        fun from(feed: HomeProviderFeed) = CachedHomeProviderFeed(
            chips = feed.chips.map(CachedMoodChip::from),
            providerSections = feed.providerSections.map(CachedHomeSection::from),
            communitySections = feed.communitySections.map(CachedHomeSection::from),
            exploreSections = feed.exploreSections.map(CachedHomeSection::from),
            moodSections = feed.moodSections.map(CachedHomeSection::from),
        )
    }
}

@Serializable
private data class CachedMoodChip(
    val id: String,
    val label: String,
    val query: String,
    val artworkKey: String,
    val collectionType: String,
    val actionType: String,
) {
    fun toMoodChip() = MoodChip(
        id = id,
        label = label,
        query = query,
        artworkKey = artworkKey,
        collectionType = enumValueOrDefault(collectionType, HomeCollectionType.Mood),
        actionType = enumValueOrDefault(actionType, HomeActionType.OPEN_BROWSE),
        source = HomeCatalogSource.ProviderBrowse,
    )

    companion object {
        fun from(chip: MoodChip) = CachedMoodChip(
            chip.id, chip.label, chip.query, chip.artworkKey, chip.collectionType.name, chip.actionType.name,
        )
    }
}

@Serializable
private data class CachedHomeSection(
    val id: String,
    val title: String,
    val actionLabel: String? = null,
    val items: List<CachedShelfItem> = emptyList(),
) {
    fun toSection() = HomeSection(
        id = id,
        title = "$title (cached)",
        actionLabel = actionLabel,
        items = items.map { it.toShelfItem() },
    )

    companion object {
        fun from(section: HomeSection) = CachedHomeSection(
            section.id, section.title, section.actionLabel, section.items.map(CachedShelfItem::from),
        )
    }
}

@Serializable
private data class CachedShelfItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val query: String? = null,
    val artworkKey: String? = null,
    val collectionType: String,
    val actionType: String,
) {
    fun toShelfItem(): PlaylistShelfItem {
        val cachedAction = enumValueOrDefault(actionType, HomeActionType.OPEN_BROWSE)
        val action = if (cachedAction == HomeActionType.PLAY_TRACK) HomeActionType.OPEN_SEARCH_ONLY_WHEN_EXPLICIT else cachedAction
        return PlaylistShelfItem(
            id = id,
            title = title,
            subtitle = subtitle,
            thumbnailUrl = thumbnailUrl,
            query = query ?: "$title $subtitle".trim(),
            artworkKey = artworkKey,
            collectionType = enumValueOrDefault(collectionType, HomeCollectionType.Playlist),
            source = HomeCatalogSource.ProviderBrowse,
            actionType = action,
        )
    }

    companion object {
        fun from(item: PlaylistShelfItem) = CachedShelfItem(
            item.id, item.title, item.subtitle, item.thumbnailUrl, item.query, item.artworkKey,
            item.collectionType.name, item.actionType.name,
        )
    }
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default

internal fun HomeProviderFeed.deduplicated(): HomeProviderFeed {
    val seen = mutableSetOf<String>()
    fun dedupeSections(sections: List<HomeSection>) = sections.mapNotNull { section ->
        val uniqueItems = section.items.filter { item ->
            val key = item.providerSong?.id ?: item.id
            seen.add(key)
        }
        section.takeIf { uniqueItems.isNotEmpty() }?.copy(items = uniqueItems)
    }
    return copy(
        providerSections = dedupeSections(providerSections),
        communitySections = dedupeSections(communitySections),
        exploreSections = dedupeSections(exploreSections),
        moodSections = dedupeSections(moodSections),
    )
}
