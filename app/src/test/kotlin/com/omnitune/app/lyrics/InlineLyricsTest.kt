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
import com.omnitune.app.models.LyricsLine

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

    @Test
    fun lrcMetadataBeforeTimestampsStillParsesAsSynced() {
        val entries = InlineLyrics.parseSyncedEntries(
            """
            [ar:Artist]
            [ti:Song]
            [00:05.00]First line
            [00:10.00]Second line
            """.trimIndent()
        )

        assertEquals(listOf("First line", "Second line"), entries.map { it.text })
        assertTrue(InlineLyrics.stateFor(entries, positionMs = 5_100L).isSynced)
    }

    @Test
    fun ttmlLyricsKeepTheirTiming() {
        val entries = InlineLyrics.parseSyncedEntries(
            """
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body><div>
                <p begin="00:00:05.000" end="00:00:10.000"><span>First line</span></p>
                <p begin="00:00:10.000" end="00:00:15.000"><span>Second line</span></p>
              </div></body>
            </tt>
            """.trimIndent()
        )

        assertEquals(listOf(5_000L, 10_000L), entries.map { it.time })
        assertEquals("Second line", InlineLyrics.stateFor(entries, positionMs = 10_100L).currentLine)
    }

    @Test
    fun loadedLyricsLinesCanDriveInlineStateBeforeDatabaseFlowUpdates() {
        val entries = InlineLyrics.syncedEntriesFromLines(
            listOf(
                LyricsLine(timestamp = 1_000L, text = "First loaded line"),
                LyricsLine(timestamp = 3_000L, text = "Second loaded line"),
            )
        )

        val state = InlineLyrics.stateFor(entries, positionMs = 3_100L)

        assertTrue(state.isSynced)
        assertEquals("Second loaded line", state.currentLine)
    }
}
