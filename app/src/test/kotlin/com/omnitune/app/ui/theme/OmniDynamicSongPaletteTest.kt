/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OmniDynamicSongPaletteTest {
    @Test
    fun artworkPaletteRejectsUnusableGrayWhiteAndBlackColors() {
        val fallback = Color(0xFF6F7DF5)

        val palette = OmniDynamicSongPalette.fromArtworkColors(
            colors = listOf(Color.White, Color.Black, Color.Gray),
            fallbackAccent = fallback,
        )
        val fallbackPalette = OmniDynamicSongPalette.fallback(fallback)

        assertEquals(fallbackPalette.accent, palette.accent)
    }

    @Test
    fun artworkPaletteKeepsBrightArtworkDarkSafe() {
        val palette = OmniDynamicSongPalette.fromArtworkColors(
            colors = listOf(Color(0xFFFFD21F), Color(0xFFE83E8C)),
            fallbackAccent = Color(0xFF6F7DF5),
        )

        assertNotEquals(Color(0xFFFFD21F), palette.background)
        assertTrue("Background should stay dark", palette.background.luminance() < 0.10f)
        assertTrue("Accent should avoid raw neon flood", palette.accent.luminance() < 0.72f)
        assertTrue(
            "Accent foreground should use a readable control color",
            palette.onAccent == Color.White || palette.onAccent == Color(0xFF05060A),
        )
    }
}
