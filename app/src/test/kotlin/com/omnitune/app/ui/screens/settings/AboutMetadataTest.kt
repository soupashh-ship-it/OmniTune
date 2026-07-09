/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens.settings

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutMetadataTest {
    @Test
    fun repositoryAndDeveloperUrlsUseVerifiedOwner() {
        assertEquals("https://github.com/soupashh-ship-it/OmniTune", OmniTuneRepositoryUrl)
        assertEquals("https://github.com/soupashh-ship-it", AboutDestinations.developer.url)
    }

    @Test
    fun discordAndSupportRowsStayHiddenWithoutRealDestinations() {
        assertNull(AboutDestinations.discordUrl)
        assertNull(AboutDestinations.supportUrl)
    }

    @Test
    fun inspirationContainsOnlyVerifiedProjectUrls() {
        val urls = AboutDestinations.inspiration.map { it.url }

        assertTrue(urls.contains(VeluneRepositoryUrl))
        assertTrue(urls.contains(ArchiveTuneRepositoryUrl))
        assertFalse(urls.any { it.isBlank() || it == "#" })
    }

    @Test
    fun installedDateFormattingUsesLocaleDateInsteadOfTimestamp() {
        val formatted = formatInstallDate(1_783_536_000_000L, Locale.US)

        assertEquals("Jul 9, 2026", formatted)
    }

    @Test
    fun invalidInstallDateReturnsUnknown() {
        assertEquals("Unknown", formatInstallDate(0L, Locale.US))
    }
}
