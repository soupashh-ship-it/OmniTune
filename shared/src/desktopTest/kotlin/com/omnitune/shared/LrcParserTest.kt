package com.omnitune.shared

import com.omnitune.shared.data.lyrics.LrcParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LrcParserTest {

    @Test
    fun testStandardTimestampCentiseconds() {
        val lrc = "[01:23.45]Hello world"
        val result = LrcParser.parse(lrc)

        assertTrue(result.isSynced)
        assertEquals(1, result.lines.size)
        assertEquals(83450L, result.lines[0].timestampMs)
        assertEquals("Hello world", result.lines[0].text)
    }

    @Test
    fun testExtendedTimestampMilliseconds() {
        val lrc = "[01:05.678]Precision timing lyric"
        val result = LrcParser.parse(lrc)

        assertTrue(result.isSynced)
        assertEquals(1, result.lines.size)
        assertEquals(65678L, result.lines[0].timestampMs)
        assertEquals("Precision timing lyric", result.lines[0].text)
    }

    @Test
    fun testMultipleTimestampsOnSingleLine() {
        val lrc = "[00:10.00][00:25.50]Repeated Chorus"
        val result = LrcParser.parse(lrc)

        assertTrue(result.isSynced)
        assertEquals(2, result.lines.size)
        assertEquals(10000L, result.lines[0].timestampMs)
        assertEquals("Repeated Chorus", result.lines[0].text)
        assertEquals(25500L, result.lines[1].timestampMs)
        assertEquals("Repeated Chorus", result.lines[1].text)
    }

    @Test
    fun testInstrumentalBreakEmptyText() {
        val lrc = "[00:15.00]"
        val result = LrcParser.parse(lrc)

        assertTrue(result.isSynced)
        assertEquals(1, result.lines.size)
        assertEquals(15000L, result.lines[0].timestampMs)
        assertEquals("", result.lines[0].text)
    }

    @Test
    fun testMetadataParsing() {
        val lrc = """
            [ti:Bohemian Rhapsody]
            [ar:Queen]
            [al:A Night at the Opera]
            [00:01.00]Is this the real life?
        """.trimIndent()

        val result = LrcParser.parse(lrc)
        assertTrue(result.isSynced)
        assertEquals("Bohemian Rhapsody", result.metadata["ti"])
        assertEquals("Queen", result.metadata["ar"])
        assertEquals("A Night at the Opera", result.metadata["al"])
        assertEquals(1, result.lines.size)
    }

    @Test
    fun testFindActiveLineIndexBinarySearch() {
        val lrc = """
            [00:05.00]Line 1
            [00:15.00]Line 2
            [00:30.00]Line 3
            [01:00.00]Line 4
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        val lines = parsed.lines

        // Before first line
        assertEquals(-1, LrcParser.findActiveLineIndex(lines, 2000L))

        // Exactly at first line
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 5000L))

        // Between line 1 and line 2
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 10000L))

        // Exactly at line 2
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 15000L))

        // After last line
        assertEquals(3, LrcParser.findActiveLineIndex(lines, 90000L))
    }

    @Test
    fun testSeekBackwardsSmoothUpdate() {
        val lrc = """
            [00:05.00]First
            [00:10.00]Second
            [00:20.00]Third
        """.trimIndent()
        val lines = LrcParser.parse(lrc).lines

        assertEquals(2, LrcParser.findActiveLineIndex(lines, 25000L))
        // Seek backward to 12s -> should be index 1
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 12000L))
        // Seek backward to 6s -> should be index 0
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 6000L))
    }

    @Test
    fun testUnsyncedAndNullInput() {
        val emptyResult = LrcParser.parse(null)
        assertFalse(emptyResult.isSynced)
        assertTrue(emptyResult.lines.isEmpty())

        val plainLrc = """
            Line one without timestamps
            Line two without timestamps
        """.trimIndent()
        val plainResult = LrcParser.parse(plainLrc)
        assertFalse(plainResult.isSynced)
        assertEquals(2, plainResult.lines.size)
    }
}
