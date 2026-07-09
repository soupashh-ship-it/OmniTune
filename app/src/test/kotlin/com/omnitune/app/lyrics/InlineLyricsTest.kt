/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineLyricsTest {
    @Test
    fun syncedLyricsReturnCurrentAndNextLineForPosition() {
        val entries = InlineLyrics.parseSyncedEntries(
            """
            [00:00.00]First line
            [00:10.00]Second line
            [00:20.00]Third line
            """.trimIndent()
        )

        val state = InlineLyrics.stateFor(entries, positionMs = 10_100L)

        assertTrue(state.hasLyrics)
        assertTrue(state.isSynced)
        assertEquals("Second line", state.currentLine)
        assertEquals("Third line", state.nextLine)
    }

    @Test
    fun unsyncedLyricsDoNotCreateFakeSubtitles() {
        val entries = InlineLyrics.parseSyncedEntries(
            """
            First plain line
            Second plain line
            """.trimIndent()
        )

        val state = InlineLyrics.stateFor(entries, positionMs = 5_000L)

        assertFalse(state.hasLyrics)
        assertFalse(state.isSynced)
        assertNull(state.currentLine)
    }
}
