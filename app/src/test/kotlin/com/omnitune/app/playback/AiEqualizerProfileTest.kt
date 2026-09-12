package com.omnitune.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiEqualizerProfileTest {
    @Test
    fun `blank prompt returns flat profile`() {
        val bands = createAiEqualizerBands("")

        assertEquals(EqualizerPresets.FLAT.bands, bands)
    }

    @Test
    fun `track hint participates in generated profile`() {
        val bands = createAiEqualizerBands(prompt = "balanced", trackHint = "deep club bass")

        assertTrue(bands[0].gainDb > 0f)
        assertTrue(bands[1].gainDb > 0f)
        assertTrue(bands[2].gainDb > 0f)
    }

    @Test
    fun `generated gains stay inside safe equalizer bounds`() {
        val bands = createAiEqualizerBands("bass sub 808 deep club rumble pop dance edm party bright crisp vocal clear")

        assertTrue(bands.all { it.gainDb in -12f..12f })
    }

    @Test
    fun `description reflects recognized prompt intent`() {
        assertEquals("Bass-forward", describeAiEqualizerProfile("more 808 club bass"))
        assertEquals("Vocal clarity", describeAiEqualizerProfile("clear vocals and speech"))
        assertEquals("Bright detail", describeAiEqualizerProfile("sparkle and air"))
        assertEquals("Balanced custom", describeAiEqualizerProfile("make this nicer"))
    }
}
