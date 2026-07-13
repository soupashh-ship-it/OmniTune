package com.omnitune.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EqualizerModelsTest {
    @Test
    fun `stored bands round trip and malformed values are rejected`() {
        val bands = EqualizerPresets.BASS_BOOST.bands
        assertEquals(bands, decodeEqualizerBands(encodeEqualizerBands(bands)))
        assertNull(decodeEqualizerBands("100,200"))
    }
}
