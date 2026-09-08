package com.omnitune.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.omnitune.app.models.AppTheme
import com.omnitune.app.models.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OmniTuneThemeTest {

    @Test
    fun testAppThemeEnumValues() {
        assertEquals("Electric Purple", AppTheme.DEFAULT.label)
        assertEquals("Ocean Blue", AppTheme.OCEAN.label)
        assertEquals("Sunset Orange", AppTheme.SUNSET.label)
        assertEquals("Forest Green", AppTheme.NATURE.label)
        assertEquals("Passion Pink", AppTheme.LOVE.label)
    }

    @Test
    fun testThemeModeEnumValues() {
        assertEquals("Dark", ThemeMode.DARK.label)
        assertEquals("Light", ThemeMode.LIGHT.label)
        assertEquals("System default", ThemeMode.SYSTEM.label)
    }

    @Test
    fun testElevationTokens() {
        assertEquals(0.dp, ElevationTokens.Level0)
        assertEquals(1.dp, ElevationTokens.Level1)
        assertEquals(3.dp, ElevationTokens.Level2)
        assertEquals(6.dp, ElevationTokens.Level3)
        assertEquals(8.dp, ElevationTokens.Level4)
        assertEquals(12.dp, ElevationTokens.Level5)
    }

    @Test
    fun testSpacingTokens() {
        assertEquals(2.dp, SpacingTokens.Xxs)
        assertEquals(4.dp, SpacingTokens.Xs)
        assertEquals(8.dp, SpacingTokens.Sm)
        assertEquals(12.dp, SpacingTokens.Md)
        assertEquals(16.dp, SpacingTokens.Lg)
        assertEquals(24.dp, SpacingTokens.Xl)
        assertEquals(32.dp, SpacingTokens.Xxl)
        assertEquals(48.dp, SpacingTokens.Xxxl)
    }

    @Test
    fun testMotionTokens() {
        assertEquals(50, MotionTokens.DurationShort1)
        assertEquals(100, MotionTokens.DurationShort2)
        assertEquals(250, MotionTokens.DurationMedium1)
        assertEquals(300, MotionTokens.DurationMedium2)
        assertEquals(450, MotionTokens.DurationLong1)
        assertEquals(600, MotionTokens.DurationLong4)

        assertNotNull(MotionTokens.Emphasized)
        assertNotNull(MotionTokens.Standard)
    }

    @Test
    fun testColorTokens() {
        assertEquals(Color(0xFF1D0035), Purple10)
        assertEquals(Color(0xFFB970F2), Purple70)
        assertEquals(Color(0xFF00BDD6), Cyan70)
        assertEquals(Color(0xFFF058B6), Magenta70)
        assertEquals(Color(0xFF151218), SurfaceDim)
        assertEquals(Color(0xFF0F0D13), SurfaceContainerLowest)
        assertEquals(Color(0xFF0F0F0F), YtFlatBackground)

        // Palette presets
        assertEquals(Color(0xFF99CBFF), Blue80)
        assertEquals(Color(0xFF4CD9E2), Teal80)
        assertEquals(Color(0xFFFFB59D), Orange80)
        assertEquals(Color(0xFFF0C048), Gold80)
        assertEquals(Color(0xFF8CF7A9), Green80)
        assertEquals(Color(0xFFC7CD7A), Lime80)
        assertEquals(Color(0xFFFFB0CD), Pink80)
        assertEquals(Color(0xFFFFB2B9), Rose80)

        // Glassmorphism
        assertEquals(Color(0x1AFFFFFF), GlassWhite)
        assertEquals(Color(0x33000000), GlassBlack)
        assertEquals(Color(0x339D4EDD), GlassPurple)
    }

    @Test
    fun testShapes() {
        assertNotNull(Shapes)
        assertNotNull(Shapes.extraSmall)
        assertNotNull(Shapes.small)
        assertNotNull(Shapes.medium)
        assertNotNull(Shapes.large)
        assertNotNull(Shapes.extraLarge)

        assertNotNull(MusicCardShape)
        assertNotNull(PlayerCardShape)
        assertNotNull(AlbumArtShape)
        assertNotNull(PillShape)
        assertNotNull(SquircleShape)
        assertNotNull(QuickAccessShape)
        assertNotNull(NewReleaseCardShape)
        assertNotNull(CardShapeToken)
        assertNotNull(SheetShapeToken)
        assertNotNull(ChipShapeToken)

        // Expressive Shapes
        assertNotNull(ExpressiveShapes.AlbumArt)
        assertNotNull(ExpressiveShapes.Fab)
        assertNotNull(ExpressiveShapes.MiniPlayer)
        assertNotNull(ExpressiveShapes.NowPlaying)
        assertNotNull(ExpressiveShapes.ActionChip)
        assertNotNull(ExpressiveShapes.GenreCard)
        assertNotNull(ExpressiveShapes.Button)
        assertNotNull(ExpressiveShapes.ButtonPressed)
        assertNotNull(ExpressiveShapes.FabResting)
        assertNotNull(ExpressiveShapes.FabPressed)
    }

    @Test
    fun testTypographyScale() {
        assertNotNull(Outfit)
        assertNotNull(Typography)
        assertNotNull(Typography.displayLarge)
        assertNotNull(Typography.displayMedium)
        assertNotNull(Typography.displaySmall)
        assertNotNull(Typography.headlineLarge)
        assertNotNull(Typography.headlineMedium)
        assertNotNull(Typography.headlineSmall)
        assertNotNull(Typography.titleLarge)
        assertNotNull(Typography.titleMedium)
        assertNotNull(Typography.titleSmall)
        assertNotNull(Typography.bodyLarge)
        assertNotNull(Typography.bodyMedium)
        assertNotNull(Typography.bodySmall)
        assertNotNull(Typography.labelLarge)
        assertNotNull(Typography.labelMedium)
        assertNotNull(Typography.labelSmall)
    }
}
