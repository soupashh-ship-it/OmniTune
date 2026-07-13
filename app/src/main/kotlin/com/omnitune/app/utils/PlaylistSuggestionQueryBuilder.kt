/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.utils

import com.omnitune.app.db.entities.PlaylistSong
import com.omnitune.app.models.PlaylistSuggestionQuery
import java.time.Year

object PlaylistSuggestionQueryBuilder {
    private val genreKeywords = listOf(
        "pop", "rock", "hip hop", "rap", "electronic", "dance", "jazz", "blues", "country",
        "folk", "classical", "metal", "punk", "indie", "alternative", "r&b", "soul",
        "reggae", "latin", "k-pop", "j-pop", "house", "techno", "ambient",
    )
    private val moodKeywords = listOf(
        "chill", "upbeat", "energetic", "relaxing", "romantic", "sad", "happy",
        "calm", "dreamy", "nostalgic", "focus", "workout",
    )
    private val fallbackQueries = listOf(
        "new music ${Year.now().value}",
        "trending songs",
        "popular music",
        "fresh music",
        "music discovery",
    )

    fun buildSuggestionQueries(
        playlistName: String,
        playlistSongs: List<PlaylistSong>,
    ): List<PlaylistSuggestionQuery> {
        val queries = mutableListOf<PlaylistSuggestionQuery>()
        val cleanName = playlistName.trim()

        if (cleanName.isNotBlank()) {
            queries += PlaylistSuggestionQuery(cleanName, 1)
        }

        val genres = extractMatches(cleanName, genreKeywords)
        val moods = extractMatches(cleanName, moodKeywords)

        genres.forEach { queries += PlaylistSuggestionQuery("$it music", 2) }
        moods.forEach { queries += PlaylistSuggestionQuery("$it music", 3) }

        val artists = topArtists(playlistSongs)
        artists.take(4).forEachIndexed { index, artist ->
            queries += PlaylistSuggestionQuery("songs like $artist", 4 + index)
        }

        if (genres.isNotEmpty()) {
            artists.take(2).forEach { artist ->
                queries += PlaylistSuggestionQuery("${genres.first()} music like $artist", 10)
            }
        }

        fallbackQueries.shuffled().forEachIndexed { index, query ->
            queries += PlaylistSuggestionQuery(query, 20 + index)
        }

        return queries
            .distinctBy { it.query.lowercase() }
            .sortedBy { it.priority }
    }

    fun shouldRefreshSuggestions(lastTimestamp: Long, cacheExpiryHours: Long = 12): Boolean =
        System.currentTimeMillis() - lastTimestamp > cacheExpiryHours * 60 * 60 * 1000

    private fun extractMatches(text: String, candidates: List<String>): List<String> {
        val lower = text.lowercase()
        return candidates.filter { lower.contains(it) }
    }

    private fun topArtists(songs: List<PlaylistSong>): List<String> =
        songs
            .flatMap { it.song.artists }
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
}
