package com.omnitune.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeProviderFeedTest {
    @Test
    fun providerDeduplicationKeepsTheFirstSuccessfulSection() {
        val duplicate = PlaylistShelfItem(id = "provider_song_one", title = "One", subtitle = "Artist")
        val feed = HomeProviderFeed(
            providerSections = listOf(HomeSection(id = "home", title = "Home", items = listOf(duplicate))),
            communitySections = listOf(HomeSection(id = "community", title = "Community", items = listOf(duplicate))),
        )

        val resolved = feed.deduplicated()

        assertEquals(1, resolved.providerSections.single().items.size)
        assertTrue(resolved.communitySections.isEmpty())
    }
}
