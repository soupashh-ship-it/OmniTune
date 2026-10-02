package com.omnitune.shared.ui

import com.omnitune.shared.ui.player.WaveformStyle
import com.omnitune.shared.ui.player.formatDuration
import com.omnitune.shared.ui.theme.SuvMusicThemeEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaveformSeekerTest {

    @Test
    fun testWaveformSeekerSupportsAllNineStyles() {
        val styles = SuvMusicThemeEngine.WAVEFORM_STYLES
        assertEquals(9, styles.size)
        assertTrue(styles.contains("Bars"))
        assertTrue(styles.contains("Mirror"))
        assertTrue(styles.contains("Rounded"))
        assertTrue(styles.contains("Gradient"))
        assertTrue(styles.contains("Smooth"))
        assertTrue(styles.contains("Stepped"))
        assertTrue(styles.contains("Dots"))
        assertTrue(styles.contains("Wave"))
        assertTrue(styles.contains("Minimal"))
    }

    @Test
    fun testWaveformSeekerEnumCoversStyles() {
        assertEquals(9, WaveformStyle.entries.size)
        for (styleName in SuvMusicThemeEngine.WAVEFORM_STYLES) {
            val resolved = WaveformStyle.fromName(styleName)
            assertEquals(styleName, resolved.styleName)
        }
    }

    @Test
    fun testWaveformSeekerDurationGuardPreventsZeroOrNaN() {
        val posZero = SuvMusicThemeEngine.calculateWaveformPosition(0.5, 0L)
        assertEquals(0L, posZero)

        val posNegative = SuvMusicThemeEngine.calculateWaveformPosition(0.5, -5000L)
        assertEquals(0L, posNegative)

        val posNaN = SuvMusicThemeEngine.calculateWaveformPosition(Double.NaN, 180000L)
        assertEquals(0L, posNaN)
    }

    @Test
    fun testWaveformScrubberMapsFractionAccuratelyToMilliseconds() {
        val durationMs = 240000L // 4 minutes
        val pos = SuvMusicThemeEngine.calculateWaveformPosition(0.25, durationMs)
        assertEquals(60000L, pos)

        val half = SuvMusicThemeEngine.calculateWaveformPosition(0.5f, durationMs)
        assertEquals(120000L, half)
    }

    @Test
    fun testWaveformSeekerNegativeScrubClampsToZero() {
        val pos = SuvMusicThemeEngine.calculateWaveformPosition(-0.5, 180000L)
        assertEquals(0L, pos)
    }

    @Test
    fun testWaveformSeekerExcessiveScrubClampsToDuration() {
        val pos = SuvMusicThemeEngine.calculateWaveformPosition(1.5, 180000L)
        assertEquals(180000L, pos)
    }

    @Test
    fun testWaveformStyleFallbackDefaultsToBars() {
        val resolved = SuvMusicThemeEngine.resolveWaveformStyle("UnknownHologramStyle")
        assertEquals("Bars", resolved)

        val enumResolved = WaveformStyle.fromName("UnknownStyle")
        assertEquals(WaveformStyle.BARS, enumResolved)
    }

    @Test
    fun testFormatDurationFormatting() {
        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:00", formatDuration(-500L))
        assertEquals("0:09", formatDuration(9000L))
        assertEquals("1:15", formatDuration(75000L))
        assertEquals("3:45", formatDuration(225000L))
        assertEquals("1:00:00", formatDuration(3600000L))
    }
}
