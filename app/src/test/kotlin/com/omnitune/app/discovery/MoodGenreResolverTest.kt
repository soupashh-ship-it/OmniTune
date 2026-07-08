/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.discovery

import com.omnitune.app.ui.screens.HomeCatalogSource
import com.omnitune.app.ui.screens.HomeCollectionMetadata
import com.omnitune.app.ui.screens.HomeCollectionType
import com.omnitune.innertube.models.Artist
import com.omnitune.innertube.models.SongItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodGenreResolverTest {
    @Test
    fun chillCategoryDoesNotUseGenericSongsQuery() = runBlocking {
        val provider = FakeSearchProvider()
        val category = requireNotNull(MoodGenreCategories.requireProfile("chill"))

        MoodGenreResolver(provider).loadCategorySongs(category, limit = 20).getOrThrow()

        assertTrue(provider.queries.isNotEmpty())
        assertFalse(provider.queries.any { it == "songs" || it == "music" })
        assertTrue(provider.queries.any { it.contains("chill", ignoreCase = true) })
    }

    @Test
    fun gamingCategoryUsesGamingSpecificQueries() = runBlocking {
        val provider = FakeSearchProvider()
        val category = requireNotNull(MoodGenreCategories.requireProfile("gaming"))

        MoodGenreResolver(provider).loadCategorySongs(category, limit = 20).getOrThrow()

        assertTrue(provider.queries.any { it.contains("gaming", ignoreCase = true) })
        assertTrue(provider.queries.any { it.contains("edm", ignoreCase = true) || it.contains("phonk", ignoreCase = true) })
    }

    @Test
    fun unknownSingleWordCategoryHasNoProfile() {
        val metadata = HomeCollectionMetadata(
            id = "unknown_category",
            title = "Unknown",
            subtitle = "Made for exploring",
            query = "music",
            collectionType = HomeCollectionType.Mood,
            artworkKey = "unknown",
            source = HomeCatalogSource.CuratedDefault,
        )

        assertNull(MoodGenreCategories.forCollection(metadata))
    }

    @Test
    fun duplicateResultsAreRemovedByStableId() = runBlocking {
        val duplicate = song("same", "Chill Night", "Soft Artist")
        val provider = FakeSearchProvider(
            defaultSongs = listOf(duplicate, duplicate.copy(title = "Chill Night Duplicate")),
        )
        val category = requireNotNull(MoodGenreCategories.requireProfile("chill"))

        val result = MoodGenreResolver(provider).loadCategorySongs(category, limit = 20).getOrThrow()

        assertEquals(1, result.songs.size)
        assertEquals("same", result.songs.single().id)
    }

    @Test
    fun excludeKeywordsFilterIrrelevantResults() = runBlocking {
        val provider = FakeSearchProvider(
            defaultSongs = listOf(
                song("good", "Chill Acoustic Evening", "Soft Artist"),
                song("bad", "Bass Boosted Hardstyle Gym Remix", "Loud Artist"),
            ),
        )
        val category = requireNotNull(MoodGenreCategories.requireProfile("chill"))

        val result = MoodGenreResolver(provider).loadCategorySongs(category, limit = 20).getOrThrow()

        assertEquals(listOf("good"), result.songs.map { it.id })
    }

    @Test
    fun preferredKeywordsIncreaseRelevance() {
        val resolver = MoodGenreResolver(FakeSearchProvider())
        val category = requireNotNull(MoodGenreCategories.requireProfile("focus"))
        val focused = CandidateSong(
            song = song("focused", "Deep Focus Lofi Beats", "Study Artist"),
            query = "focus music playlist",
            source = CandidateQuerySource.PRIMARY,
            category = category,
        )
        val unrelated = CandidateSong(
            song = song("unrelated", "Random Song", "Artist"),
            query = "focus music playlist",
            source = CandidateQuerySource.PRIMARY,
            category = category,
        )

        assertTrue(resolver.scoreCandidate(focused) > resolver.scoreCandidate(unrelated))
    }

    @Test
    fun hindiQueryIsOnlyUsedForHindiProfile() = runBlocking {
        val chillProvider = FakeSearchProvider()
        val hindiProvider = FakeSearchProvider()
        val chill = requireNotNull(MoodGenreCategories.requireProfile("chill"))
        val hindi = requireNotNull(MoodGenreCategories.requireProfile("bollywood_hindi"))

        MoodGenreResolver(chillProvider).loadCategorySongs(chill, limit = 20).getOrThrow()
        MoodGenreResolver(hindiProvider).loadCategorySongs(hindi, limit = 20).getOrThrow()

        assertFalse(chillProvider.queries.any { it.contains("hindi", ignoreCase = true) || it.contains("bollywood", ignoreCase = true) })
        assertTrue(hindiProvider.queries.any { it.contains("hindi", ignoreCase = true) || it.contains("bollywood", ignoreCase = true) })
    }

    @Test
    fun fallbackQueriesAreUsedWhenPrimaryResultCountIsLow() = runBlocking {
        val provider = FakeSearchProvider(defaultSongs = listOf(song("one", "Gaming Hype", "EDM Artist")))
        val category = requireNotNull(MoodGenreCategories.requireProfile("gaming"))

        val result = MoodGenreResolver(provider).loadCategorySongs(category, limit = 20).getOrThrow()

        assertTrue(result.fallbackQueriesUsed.isNotEmpty())
        assertTrue(provider.queries.any { it.contains("montage", ignoreCase = true) || it.contains("no copyright", ignoreCase = true) })
    }

    @Test
    fun emptyRelevantResultDoesNotReturnUnrelatedTrendingSongs() = runBlocking {
        val provider = FakeSearchProvider(
            defaultSongs = listOf(
                song("news", "Daily News Podcast", "Talk Channel"),
                song("movie", "Full Movie Trailer Reaction", "Video Channel"),
            ),
        )
        val category = requireNotNull(MoodGenreCategories.requireProfile("sleep"))

        val result = MoodGenreResolver(provider).loadCategorySongs(category, limit = 20).getOrThrow()

        assertTrue(result.songs.isEmpty())
    }

    @Test
    fun visibleGridAliasResolvesToCategoryProfile() {
        val metadata = HomeCollectionMetadata(
            id = "grid_gaming",
            title = "Gaming",
            subtitle = "Made for exploring",
            query = "gaming music",
            collectionType = HomeCollectionType.Genre,
            artworkKey = "grid_gaming",
        )

        val category = MoodGenreCategories.forCollection(metadata)

        assertNotNull(category)
        assertEquals("gaming", category?.id)
    }

    private class FakeSearchProvider(
        private val defaultSongs: List<SongItem> = emptyList(),
    ) : MoodGenreSearchProvider {
        val queries = mutableListOf<String>()

        override suspend fun searchSongs(query: String): Result<List<SongItem>> {
            queries += query
            return Result.success(defaultSongs)
        }
    }

    private fun song(
        id: String,
        title: String,
        artist: String,
    ) = SongItem(
        id = id,
        title = title,
        artists = listOf(Artist(name = artist, id = null)),
        thumbnail = "https://example.com/$id.jpg",
    )
}

