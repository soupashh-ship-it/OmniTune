/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.content

import com.omnitune.app.models.HomeItem
import com.omnitune.app.models.HomeSection
import com.omnitune.app.models.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeContentRequestGateTest {
    @Test
    fun staleRequestFromPreviousLanguageIsRejected() {
        val gate = HomeContentRequestGate()
        val hindiRequest = gate.begin(MusicContentLanguage.HINDI)
        val englishRequest = gate.begin(MusicContentLanguage.ENGLISH)

        assertFalse(gate.accepts(hindiRequest))
        assertTrue(gate.accepts(englishRequest))
    }

    @Test
    fun continuationResetPreventsCrossLanguagePagination() {
        val store = LanguageScopedHomeContinuationStore()
        store.setContinuation(MusicContentLanguage.HINDI, "hindi-page-2")

        store.resetAll()

        assertNull(store.continuationFor(MusicContentLanguage.HINDI))
        assertNull(store.continuationFor(MusicContentLanguage.ENGLISH))
    }

    @Test
    fun sectionCacheIsScopedByLanguage() {
        val cache = LanguageScopedHomeSectionCache()
        val hindiSection = section("Hindi Essentials", "hindi-song")
        val englishSection = section("English Essentials", "english-song")

        cache.put(MusicContentLanguage.HINDI, listOf(hindiSection))
        cache.put(MusicContentLanguage.ENGLISH, listOf(englishSection))

        assertEquals(listOf(hindiSection), cache.sectionsFor(MusicContentLanguage.HINDI))
        assertEquals(listOf(englishSection), cache.sectionsFor(MusicContentLanguage.ENGLISH))
    }

    @Test
    fun languageChangeInvalidatesOnlyRemoteDiscoverySurfaces() {
        val plan = MusicContentDiscoveryPolicy.invalidationPlan(
            previous = MusicContentLanguage.ENGLISH,
            next = MusicContentLanguage.TAMIL,
        )

        assertTrue(plan.invalidates(MusicContentSurface.HOME))
        assertTrue(plan.invalidates(MusicContentSurface.DISCOVERY))
        assertTrue(plan.invalidates(MusicContentSurface.CHARTS))
        assertTrue(plan.invalidates(MusicContentSurface.SEARCH_SUGGESTIONS))
        assertTrue(plan.invalidates(MusicContentSurface.RELATED))
        assertTrue(plan.preserves(MusicContentSurface.LIBRARY))
        assertTrue(plan.preserves(MusicContentSurface.DOWNLOADS))
        assertTrue(plan.preserves(MusicContentSurface.LIKED_SONGS))
        assertTrue(plan.preserves(MusicContentSurface.HISTORY))
        assertTrue(plan.preserves(MusicContentSurface.AUTH))
        assertTrue(plan.preserves(MusicContentSurface.SETTINGS))
    }

    private fun section(title: String, songId: String): HomeSection =
        HomeSection(
            title = title,
            items = listOf(HomeItem.SongItem(Song(id = songId, title = songId, artist = "Artist"))),
        )
}
