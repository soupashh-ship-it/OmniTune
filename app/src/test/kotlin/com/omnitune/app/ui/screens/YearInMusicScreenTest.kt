package com.omnitune.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class YearInMusicScreenTest {
    @Test
    fun `formats listening time from milliseconds`() {
        assertEquals("0 minutes", formatListeningTime(0L))
        assertEquals("1 minute", formatListeningTime(60_000L))
        assertEquals("1h", formatListeningTime(3_600_000L))
        assertEquals("1h 5m", formatListeningTime(3_900_000L))
    }

    @Test
    fun `does not treat milliseconds as seconds`() {
        assertEquals("1 minute", formatListeningTime(60_000L))
    }
}
