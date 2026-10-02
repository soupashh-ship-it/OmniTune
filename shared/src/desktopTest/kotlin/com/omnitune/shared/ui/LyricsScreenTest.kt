package com.omnitune.shared.ui

import com.omnitune.shared.data.lyrics.LrcParser
import com.omnitune.shared.domain.models.LyricLine
import com.omnitune.shared.domain.models.Lyrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LyricsScreenTest {

    @Test
    fun testLrcParserFindsActiveLyricLineAtExactTimestamp() {
        val lrc = "[00:10.00]Line 1\n[00:20.00]Line 2\n[00:30.00]Line 3"
        val parsed = LrcParser.parse(lrc)
        assertEquals(0, LrcParser.findActiveLineIndex(parsed.lines, 10000L))
        assertEquals(1, LrcParser.findActiveLineIndex(parsed.lines, 20000L))
        assertEquals(2, LrcParser.findActiveLineIndex(parsed.lines, 35000L))
    }

    @Test
    fun testLrcParserFindsActiveLyricLineBetweenTimestamps() {
        val lrc = "[00:10.00]Line 1\n[00:20.00]Line 2\n[00:30.00]Line 3"
        val parsed = LrcParser.parse(lrc)
        assertEquals(0, LrcParser.findActiveLineIndex(parsed.lines, 15000L))
        assertEquals(1, LrcParser.findActiveLineIndex(parsed.lines, 25000L))
    }

    @Test
    fun testLrcParserReturnsMinusOneBeforeFirstLine() {
        val lrc = "[00:10.00]Line 1\n[00:20.00]Line 2"
        val parsed = LrcParser.parse(lrc)
        assertEquals(-1, LrcParser.findActiveLineIndex(parsed.lines, 5000L))
    }

    @Test
    fun testSeekPastLastLyricTimestampKeepsLastLineActive() {
        val lrc = "[00:10.00]First\n[00:20.00]Second\n[00:30.00]Last"
        val parsed = LrcParser.parse(lrc)
        val idx = LrcParser.findActiveLineIndex(parsed.lines, 90000L)
        assertEquals(2, idx)
        assertEquals("Last", parsed.lines[idx].text)
    }

    @Test
    fun testSingleLineLyricLocatesLineAfterTimestamp() {
        val lrc = "[00:15.00]Only Line"
        val parsed = LrcParser.parse(lrc)
        assertEquals(-1, LrcParser.findActiveLineIndex(parsed.lines, 10000L))
        assertEquals(0, LrcParser.findActiveLineIndex(parsed.lines, 20000L))
    }

    @Test
    fun testUnsyncedLyricsFallbackRendersLinesWithoutMarkers() {
        val text = "Verse 1\nNo timecodes here\nJust plain text"
        val result = LrcParser.parse(text)
        assertFalse(result.isSynced)
        assertEquals(3, result.lines.size)
        assertEquals("No timecodes here", result.lines[1].text)
    }

    @Test
    fun testEmptyOrNullLyricsReturnsEmptySafely() {
        val resNull = LrcParser.parse(null)
        assertEquals(0, resNull.lines.size)
        assertFalse(resNull.isSynced)

        val resBlank = LrcParser.parse("   ")
        assertEquals(0, resBlank.lines.size)
        assertFalse(resBlank.isSynced)
    }
}
