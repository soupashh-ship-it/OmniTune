package com.omnitune.shared.ui

import com.omnitune.shared.ui.theme.OutfitTypographyScale
import com.omnitune.shared.ui.theme.SuvMusicPalette
import com.omnitune.shared.ui.theme.SuvMusicThemeEngine
import com.omnitune.shared.ui.theme.getPaletteColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SuvMusicThemeTest {

    @Test
    fun testDefaultThemePaletteDefinesPurplePrimary() {
        val p = SuvMusicThemeEngine.PALETTES["DEFAULT"]!!
        assertEquals("#9C27B0", p.primary)
        assertEquals("#00BCD4", p.secondary)
    }

    @Test
    fun testSuvMusicSupportsFiveDistinctColorSchemes() {
        val names = SuvMusicThemeEngine.PALETTES.keys
        assertEquals(5, names.size)
        assertTrue(names.contains("DEFAULT"))
        assertTrue(names.contains("OCEAN"))
        assertTrue(names.contains("SUNSET"))
        assertTrue(names.contains("NATURE"))
        assertTrue(names.contains("LOVE"))
    }

    @Test
    fun testPureBlackModeConvertsBackgroundAndSurfaceToPureZero() {
        val darkTheme = SuvMusicThemeEngine.applyTheme("DEFAULT", pureBlack = false)
        assertNotEquals("#000000", darkTheme.background)

        val amoledTheme = SuvMusicThemeEngine.applyTheme("DEFAULT", pureBlack = true)
        assertEquals("#000000", amoledTheme.background)
        assertEquals("#000000", amoledTheme.surface)
    }

    @Test
    fun testPureBlackModePreservesPrimaryAccentColorUnchanged() {
        val normalOcean = SuvMusicThemeEngine.applyTheme("OCEAN", pureBlack = false)
        val amoledOcean = SuvMusicThemeEngine.applyTheme("OCEAN", pureBlack = true)
        assertEquals(normalOcean.primary, amoledOcean.primary)
        assertEquals("#000000", amoledOcean.background)
    }

    @Test
    fun testUnknownThemePaletteReturnsDefaultFallback() {
        val theme = SuvMusicThemeEngine.applyTheme("NON_EXISTENT_PALETTE")
        assertEquals(SuvMusicThemeEngine.PALETTES["DEFAULT"]!!.primary, theme.primary)
    }

    @Test
    fun testOutfitTypographyScaleWeights() {
        val weights = SuvMusicThemeEngine.TYPOGRAPHY_WEIGHTS
        assertEquals(400, weights["Regular"])
        assertEquals(500, weights["Medium"])
        assertEquals(600, weights["SemiBold"])
        assertEquals(700, weights["Bold"])
        assertEquals(800, weights["ExtraBold"])
    }

    @Test
    fun testTypographyWeightsLookupUndefinedFallback() {
        val w = OutfitTypographyScale.resolveWeight("Ultralight", fallback = 400)
        assertEquals(400, w)
    }

    @Test
    fun testSquircleCornerSmoothingFactorToken() {
        assertEquals(0.6, SuvMusicThemeEngine.SQUIRCLE_CORNER_SMOOTHING)
    }

    @Test
    fun testEnumPaletteColorsConsistency() {
        for (palette in SuvMusicPalette.entries) {
            val colors = getPaletteColors(palette)
            val resolved = SuvMusicThemeEngine.PALETTES[palette.name]
            assertEquals(resolved?.primaryHex, colors.primaryHex)
            assertEquals(resolved?.secondaryHex, colors.secondaryHex)
        }
    }
}
