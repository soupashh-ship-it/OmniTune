/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.discovery

import com.omnitune.app.BuildConfig
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.SongItem
import timber.log.Timber

interface MoodGenreSearchProvider {
    suspend fun searchSongs(query: String): Result<List<SongItem>>
}

class YouTubeMoodGenreSearchProvider : MoodGenreSearchProvider {
    override suspend fun searchSongs(query: String): Result<List<SongItem>> =
        YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
            .map { result -> result.items.filterIsInstance<SongItem>() }
}

data class MoodGenreResult(
    val category: MoodGenreCategory,
    val songs: List<SongItem>,
    val primaryQueriesUsed: List<String>,
    val fallbackQueriesUsed: List<String>,
    val filteredCount: Int,
)

class MoodGenreResolver(
    private val searchProvider: MoodGenreSearchProvider = YouTubeMoodGenreSearchProvider(),
) {
    suspend fun loadCategorySongs(
        category: MoodGenreCategory,
        limit: Int,
    ): Result<MoodGenreResult> = runCatching {
        val primaryCandidates = searchQueries(
            category = category,
            queries = category.primaryQueries,
            source = CandidateQuerySource.PRIMARY,
        )
        var scored = rankCandidates(category, primaryCandidates)
        var totalCandidates = primaryCandidates.size
        var fallbackQueriesUsed = emptyList<String>()

        if (scored.size < category.minResultCount) {
            val fallbackCandidates = searchQueries(
                category = category,
                queries = category.fallbackQueries,
                source = CandidateQuerySource.FALLBACK,
            )
            totalCandidates += fallbackCandidates.size
            fallbackQueriesUsed = category.fallbackQueries
            scored = rankCandidates(category, primaryCandidates + fallbackCandidates)
        }

        val songs = scored.map { it.song }.take(limit)
        val filteredCount = totalCandidates - songs.size

        if (BuildConfig.DEBUG) {
            Timber.tag("MoodGenreResolver").d(
                "Category ${category.title} loaded ${songs.size} results; primary=${category.primaryQueries.firstOrNull().orEmpty()}; filtered=$filteredCount",
            )
        }

        MoodGenreResult(
            category = category,
            songs = songs,
            primaryQueriesUsed = category.primaryQueries,
            fallbackQueriesUsed = fallbackQueriesUsed,
            filteredCount = filteredCount.coerceAtLeast(0),
        )
    }

    private suspend fun searchQueries(
        category: MoodGenreCategory,
        queries: List<String>,
        source: CandidateQuerySource,
    ): List<CandidateSong> {
        val songs = mutableListOf<CandidateSong>()
        queries.forEach { query ->
            val result = searchProvider.searchSongs(query).getOrElse { emptyList() }
            result.forEach { song ->
                songs += CandidateSong(
                    song = song,
                    query = query,
                    source = source,
                    category = category,
                )
            }
        }
        return songs
    }

    internal fun rankCandidates(
        category: MoodGenreCategory,
        candidates: List<CandidateSong>,
    ): List<ScoredSong> {
        val bestByKey = linkedMapOf<String, ScoredSong>()

        candidates.forEach { candidate ->
            val score = scoreCandidate(candidate)
            if (score < MIN_ACCEPTED_SCORE) return@forEach
            val key = candidate.song.stableKey()
            val existing = bestByKey[key]
            if (existing == null || score > existing.score) {
                bestByKey[key] = ScoredSong(candidate.song, score, candidate.query)
            }
        }

        return bestByKey.values
            .sortedWith(
                compareByDescending<ScoredSong> { it.score }
                    .thenBy { it.song.title.lowercase() },
            )
    }

    internal fun scoreCandidate(candidate: CandidateSong): Int {
        val song = candidate.song
        val category = candidate.category
        val text = song.searchText()
        val queryText = candidate.query.lowercase()
        var score = when (candidate.source) {
            CandidateQuerySource.PRIMARY -> 2
            CandidateQuerySource.FALLBACK -> 1
        }

        if (text.contains(category.title.lowercase())) score += 4

        category.preferredTerms.forEach { term ->
            if (text.contains(term.lowercase())) score += 3
            if (queryText.contains(term.lowercase())) score += 1
        }

        category.includeKeywords.forEach { keyword ->
            if (text.contains(keyword.lowercase())) score += 2
        }

        category.excludeKeywords.forEach { keyword ->
            if (text.contains(keyword.lowercase())) score -= 5
        }

        if (looksUnrelated(text)) score -= 5
        if (song.title.isBlank() || song.artists.isEmpty()) score -= 2

        return score
    }

    private fun looksUnrelated(text: String): Boolean =
        unrelatedMarkers.any { text.contains(it) }

    companion object {
        private const val MIN_ACCEPTED_SCORE = 1

        private val unrelatedMarkers = listOf(
            "official trailer",
            "full movie",
            "reaction",
            "tutorial",
            "podcast",
            "ringtone",
            "news",
            "nursery rhyme",
            "kids song",
        )
    }
}

internal enum class CandidateQuerySource {
    PRIMARY,
    FALLBACK,
}

internal data class CandidateSong(
    val song: SongItem,
    val query: String,
    val source: CandidateQuerySource,
    val category: MoodGenreCategory,
)

internal data class ScoredSong(
    val song: SongItem,
    val score: Int,
    val query: String,
)

private fun SongItem.stableKey(): String =
    id.ifBlank {
        val artist = artists.firstOrNull()?.name.orEmpty()
        "${title.lowercase()}::$artist::${duration ?: -1}"
    }

private fun SongItem.searchText(): String =
    buildString {
        append(title)
        append(' ')
        append(artists.joinToString(" ") { it.name })
        append(' ')
        append(album?.name.orEmpty())
    }.lowercase()
