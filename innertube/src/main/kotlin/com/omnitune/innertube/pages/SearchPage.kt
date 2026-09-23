/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.omnitune.innertube.pages

import com.omnitune.innertube.models.Album
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.Artist
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.MusicResponsiveListItemRenderer
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.models.YTItem
import com.omnitune.innertube.models.oddElements
import com.omnitune.innertube.models.splitBySeparator
import com.omnitune.innertube.utils.parseTime

import com.omnitune.innertube.models.response.SearchResponse
import com.omnitune.innertube.models.getContinuation
import com.omnitune.innertube.models.getItems
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.ArrayDeque

data class SearchResult(
    val items: List<YTItem>,
    val continuation: String? = null,
)

object SearchPage {
    fun parseVideoSearchResults(response: JsonElement): List<SongItem> {
        val pending = ArrayDeque<JsonElement>()
        val videos = LinkedHashMap<String, SongItem>()
        pending.addLast(response)

        var visitedNodes = 0
        fun enqueue(element: JsonElement) {
            if (pending.size + visitedNodes < MAX_VIDEO_SEARCH_NODES) pending.addLast(element)
        }

        while (pending.isNotEmpty() && visitedNodes < MAX_VIDEO_SEARCH_NODES && videos.size < MAX_VIDEO_SEARCH_RESULTS) {
            val element = pending.removeFirst()
            visitedNodes++

            when (element) {
                is JsonObject -> {
                    runCatching { (element["videoRenderer"] as? JsonObject)?.toVideoSearchItem() }
                        .getOrNull()
                        ?.let { videos.putIfAbsent(it.id, it) }
                    element.forEach { (key, value) ->
                        if (key != "videoRenderer") enqueue(value)
                    }
                }
                is JsonArray -> element.forEach(::enqueue)
                else -> Unit
            }
        }

        return videos.values.toList()
    }

    private const val MAX_VIDEO_SEARCH_NODES = 20_000
    private const val MAX_VIDEO_SEARCH_RESULTS = 120

    private fun JsonObject.toVideoSearchItem(): SongItem? {
        val id = (this["videoId"] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) ?: return null
        val title = text("title")?.takeIf(String::isNotBlank) ?: return null
        val thumbnail = ((this["thumbnail"] as? JsonObject)?.get("thumbnails") as? JsonArray)
            ?.mapNotNull { item ->
                ((item as? JsonObject)?.get("url") as? JsonPrimitive)?.contentOrNull
            }
            ?.lastOrNull()
            ?.takeIf(String::isNotBlank)
            ?: return null
        val owner = text("ownerText") ?: text("shortBylineText")
        val channelId = (((this["ownerText"] as? JsonObject)?.get("runs") as? JsonArray)
            ?.firstOrNull() as? JsonObject)
            ?.get("navigationEndpoint")
            ?.let { it as? JsonObject }
            ?.get("browseEndpoint")
            ?.let { it as? JsonObject }
            ?.get("browseId")
            .let { it as? JsonPrimitive }
            ?.contentOrNull

        return SongItem(
            id = id,
            title = title,
            artists = owner?.let { listOf(Artist(name = it, id = channelId)) }.orEmpty(),
            duration = text("lengthText")?.parseTime(),
            thumbnail = thumbnail,
        )
    }

    private fun JsonObject.text(key: String): String? {
        val value = this[key] as? JsonObject ?: return null
        (value["simpleText"] as? JsonPrimitive)?.contentOrNull?.let { return it }
        return (value["runs"] as? JsonArray)
            ?.mapNotNull { run ->
                ((run as? JsonObject)?.get("text") as? JsonPrimitive)?.contentOrNull
            }
            ?.joinToString(separator = "")
            ?.takeIf(String::isNotBlank)
    }

    fun parseSearchResult(response: SearchResponse): SearchResult? {
        val contents = response.contents ?: return null

        val sectionLists = listOfNotNull(
            contents.tabbedSearchResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer,
            contents.twoColumnSearchResultsRenderer?.primaryContents?.sectionListRenderer,
            contents.sectionListRenderer,
        )

        sectionLists.forEach { sectionList ->
            val shelves = sectionList.contents.orEmpty().mapNotNull { it.musicShelfRenderer }
            val shelf = shelves.lastOrNull { it.contents?.getItems()?.isNotEmpty() == true }
                ?: shelves.lastOrNull()
                ?: return@forEach

            return SearchResult(
                items = shelf.contents?.getItems().orEmpty().mapNotNull { renderer ->
                    runCatching { toYTItem(renderer) }.getOrNull()
                },
                continuation = shelf.continuations?.getContinuation(),
            )
        }

        return null
    }

    fun toYTItem(renderer: MusicResponsiveListItemRenderer): YTItem? {
        val menuItems = renderer.menu?.menuRenderer?.items.orEmpty()
        val secondaryLine =
            renderer.flexColumns
                .getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.splitBySeparator()
                ?: return null
        return when {
            renderer.isSong -> {
                SongItem(
                    id = renderer.playlistItemData?.videoId ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    artists =
                        secondaryLine.firstOrNull()?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        } ?: return null,
                    album =
                        secondaryLine.getOrNull(1)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                            val browseId = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null
                            Album(
                                name = it.text,
                                id = browseId,
                            )
                        },
                    duration =
                        secondaryLine
                            .lastOrNull()
                            ?.firstOrNull()
                            ?.text
                            ?.parseTime(),
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    explicit =
                        renderer.badges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null,
                    endpoint = renderer.watchEndpoint,
                )
            }
            renderer.isArtist -> {
                ArtistItem(
                    id = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text
                            ?: return null,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    shuffleEndpoint =
                        menuItems
                            .find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint ?: return null,
                    radioEndpoint =
                        menuItems
                            .find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint ?: return null,
                )
            }
            renderer.isAlbum -> {
                AlbumItem(
                    browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                    playlistId =
                        renderer.overlay
                            ?.musicItemThumbnailOverlayRenderer
                            ?.content
                            ?.musicPlayButtonRenderer
                            ?.playNavigationEndpoint
                            ?.anyWatchEndpoint
                            ?.playlistId
                            ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    artists =
                        secondaryLine.getOrNull(1)?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        } ?: return null,
                    year =
                        secondaryLine
                            .getOrNull(2)
                            ?.firstOrNull()
                            ?.text
                            ?.toIntOrNull(),
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    explicit =
                        renderer.badges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null,
                )
            }
            renderer.isPlaylist -> {
                PlaylistItem(
                    id =
                        renderer.navigationEndpoint
                            ?.browseEndpoint
                            ?.browseId
                            ?.removePrefix("VL") ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    author =
                        secondaryLine.firstOrNull()?.firstOrNull()?.let {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        } ?: return null,
                    songCountText =
                        renderer.flexColumns
                            .getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.lastOrNull()
                            ?.text ?: return null,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    playEndpoint =
                        renderer.overlay
                            ?.musicItemThumbnailOverlayRenderer
                            ?.content
                            ?.musicPlayButtonRenderer
                            ?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint ?: return null,
                    shuffleEndpoint =
                        menuItems
                            .find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint ?: return null,
                    radioEndpoint =
                        menuItems
                            .find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint ?: return null,
                )
            }
            else -> null
        }
    }
}
