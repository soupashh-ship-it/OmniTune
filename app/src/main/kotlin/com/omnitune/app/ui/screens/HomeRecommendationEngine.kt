/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import com.omnitune.app.db.entities.EventWithSong
import com.omnitune.app.db.entities.Song
import java.time.Duration
import java.time.LocalDateTime

object HomeRecommendationEngine {
    private const val MIN_HISTORY_EVENTS = 1
    private const val MIN_LIBRARY_SIGNAL = 2

    fun build(input: HomeRecommendationInput, now: LocalDateTime = LocalDateTime.now()): HomeRecommendationResult {
        val distinctRecentSongs = input.events.map { it.song }.distinctBy { it.id }
        val hasEnoughSignal = distinctRecentSongs.size >= MIN_HISTORY_EVENTS ||
            (input.likedSongs.size + input.downloadedSongs.size) >= MIN_LIBRARY_SIGNAL
        if (!hasEnoughSignal) {
            return HomeRecommendationResult(sections = emptyList(), topSongs = emptyList(), topArtists = emptyList())
        }

        val skipCounts = input.skips.associate { it.songId to it.skipCount }
        val artistAffinity = buildArtistAffinity(input.events, input.likedSongs, input.downloadedSongs)
        val candidates = mergeSongs(
            distinctRecentSongs,
            input.quickPickSongs,
            input.likedSongs,
            input.downloadedSongs,
            input.forgottenFavorites,
            input.librarySongs,
        )

        val scoredSongs = candidates
            .map { song ->
                val event = input.events.firstOrNull { it.song.id == song.id }
                song to scoreSong(
                    song = song,
                    event = event,
                    artistAffinity = artistAffinity,
                    skipCount = skipCounts[song.id] ?: 0,
                    downloaded = input.downloadedSongs.any { it.id == song.id },
                    now = now,
                )
            }
            .filter { (_, score) -> score > 0.0 }
            .sortedByDescending { (_, score) -> score }
            .map { it.first }
            .distinctBy { it.id }

        val topArtists = artistAffinity.entries
            .sortedByDescending { it.value.score }
            .take(6)
            .map { (artist, seed) ->
                HomeArtistSeed(
                    name = artist,
                    query = "$artist songs",
                    score = seed.score,
                    thumbnailUrl = seed.thumbnailUrl,
                )
            }

        val sections = buildList {
            scoredSongs.take(8).takeIf { it.size >= 3 }?.let { songs ->
                add(
                    HomeSection(
                        id = "personal_for_you",
                        title = "For You",
                        items = songs.map { it.toRecommendedShelfItem("for_you", "Based on your listening") },
                    ),
                )
            }

            distinctRecentSongs.firstOrNull()?.let { song ->
                val artist = song.artists.firstOrNull()?.name.orEmpty()
                val query = listOf(song.title, artist, "similar songs")
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                if (query.isNotBlank()) {
                    add(
                        HomeSection(
                            id = "more_like_recent",
                            title = "More like ${song.title.take(32)}",
                            items = listOf(
                                PlaylistShelfItem(
                                    id = HomeDefaultCatalog.queryCollectionId(query),
                                    title = song.title.ifBlank { "Recent song mix" },
                                    subtitle = artist.ifBlank { "Related to your recent plays" },
                                    query = query,
                                    thumbnailUrl = song.thumbnailUrl,
                                    artworkKey = "related_${song.id}",
                                    collectionType = HomeCollectionType.Related,
                                    source = HomeCatalogSource.Recommended,
                                    actionType = HomeActionType.OPEN_COLLECTION,
                                ),
                            ) + topArtists.take(5).map { it.toRecommendedArtistItem() },
                        ),
                    )
                }
            }

            topArtists.takeIf { it.size >= 2 }?.let { artists ->
                add(
                    HomeSection(
                        id = "top_artists",
                        title = "Your top artists",
                        items = artists.map { it.toRecommendedArtistItem() },
                    ),
                )
            }

            input.forgottenFavorites
                .distinctBy { it.id }
                .take(8)
                .takeIf { it.size >= 2 }
                ?.let { songs ->
                    add(
                        HomeSection(
                            id = "forgotten_favorites",
                            title = "Forgotten favorites",
                            items = songs.map { it.toRecommendedShelfItem("forgotten", "Worth revisiting") },
                        ),
                    )
                }
        }

        return HomeRecommendationResult(
            sections = sections,
            topSongs = scoredSongs.take(12),
            topArtists = topArtists,
        )
    }

    private fun scoreSong(
        song: Song,
        event: EventWithSong?,
        artistAffinity: Map<String, ArtistScore>,
        skipCount: Int,
        downloaded: Boolean,
        now: LocalDateTime,
    ): Double {
        val playTimeScore = (song.song.totalPlayTime / 60_000.0).coerceAtMost(90.0)
        val eventScore = ((event?.event?.playTime ?: 0L) / 60_000.0).coerceAtMost(45.0)
        val recencyScore = event?.event?.timestamp?.let { timestamp ->
            val days = Duration.between(timestamp, now).toDays().coerceAtLeast(0)
            when {
                days <= 1 -> 34.0
                days <= 7 -> 22.0
                days <= 30 -> 12.0
                else -> 3.0
            }
        } ?: 0.0
        val likedScore = if (song.song.liked) 70.0 else 0.0
        val downloadScore = if (downloaded || song.song.downloadState == 2) 14.0 else 0.0
        val artistScore = song.artists.sumOf { artist -> artistAffinity[artist.name]?.score ?: 0.0 }.coerceAtMost(36.0)
        val timeOfDayScore = event?.event?.timestamp?.let { timestamp ->
            if (sameDayPart(timestamp, now)) 8.0 else 0.0
        } ?: 0.0
        val skipPenalty = skipCount * 28.0

        return playTimeScore + eventScore + recencyScore + likedScore + downloadScore + artistScore + timeOfDayScore - skipPenalty
    }

    private fun buildArtistAffinity(
        events: List<EventWithSong>,
        likedSongs: List<Song>,
        downloadedSongs: List<Song>,
    ): Map<String, ArtistScore> {
        val scores = linkedMapOf<String, ArtistScore>()

        fun add(song: Song, score: Double) {
            song.artists.forEach { artist ->
                if (artist.name.isBlank()) return@forEach
                val existing = scores[artist.name] ?: ArtistScore()
                scores[artist.name] = existing.copy(
                    score = existing.score + score,
                    thumbnailUrl = existing.thumbnailUrl ?: song.thumbnailUrl,
                )
            }
        }

        events.take(30).forEachIndexed { index, event ->
            add(event.song, 8.0 + (30 - index).coerceAtLeast(0) * 0.4)
        }
        likedSongs.take(30).forEach { add(it, 12.0) }
        downloadedSongs.take(30).forEach { add(it, 5.0) }

        return scores
    }

    private fun sameDayPart(a: LocalDateTime, b: LocalDateTime): Boolean = dayPart(a.hour) == dayPart(b.hour)

    private fun dayPart(hour: Int): Int = when (hour) {
        in 5..10 -> 0
        in 11..16 -> 1
        in 17..22 -> 2
        else -> 3
    }

    private fun mergeSongs(vararg lists: List<Song>): List<Song> = lists.flatMap { it }.distinctBy { it.id }

    private data class ArtistScore(
        val score: Double = 0.0,
        val thumbnailUrl: String? = null,
    )
}

private fun Song.toRecommendedShelfItem(prefix: String, subtitle: String): PlaylistShelfItem =
    PlaylistShelfItem(
        id = "${prefix}_$id",
        title = title.ifBlank { "Unknown track" },
        subtitle = artists.joinToString(", ") { it.name }.ifBlank { subtitle },
        thumbnailUrl = thumbnailUrl,
        song = this,
        artworkKey = "${prefix}_$id",
        source = HomeCatalogSource.Recommended,
        actionType = HomeActionType.PLAY_TRACK,
    )

private fun HomeArtistSeed.toRecommendedArtistItem(): PlaylistShelfItem =
    PlaylistShelfItem(
        id = HomeDefaultCatalog.queryCollectionId(query),
        title = name,
        subtitle = "Artist mix from your listening",
        query = query,
        thumbnailUrl = thumbnailUrl,
        artworkKey = "artist_${name.lowercase().replace(' ', '_')}",
        collectionType = HomeCollectionType.ArtistMix,
        source = HomeCatalogSource.Recommended,
        actionType = HomeActionType.OPEN_COLLECTION,
    )
