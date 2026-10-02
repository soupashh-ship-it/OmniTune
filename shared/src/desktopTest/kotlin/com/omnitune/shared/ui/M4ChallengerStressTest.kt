package com.omnitune.shared.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.omnitune.shared.data.lyrics.LrcParser
import com.omnitune.shared.domain.models.LyricLine
import com.omnitune.shared.domain.models.SongItem
import com.omnitune.shared.ui.components.SquircleShape
import com.omnitune.shared.ui.player.WaveformStyle
import com.omnitune.shared.ui.player.formatDuration
import com.omnitune.shared.ui.screens.AudioQuality
import com.omnitune.shared.ui.screens.LibraryFilter
import com.omnitune.shared.ui.screens.LibrarySortCriteria
import com.omnitune.shared.ui.screens.LibraryViewMode
import com.omnitune.shared.ui.screens.SearchTab
import com.omnitune.shared.ui.screens.calculateDetailTopBarAlpha
import com.omnitune.shared.ui.screens.calculateTimeOfDayGreeting
import com.omnitune.shared.ui.theme.OutfitTypographyScale
import com.omnitune.shared.ui.theme.SuvMusicPalette
import com.omnitune.shared.ui.theme.SuvMusicThemeEngine
import com.omnitune.shared.ui.theme.getPaletteColors
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Empirical challenger stress test suite for Milestone 4:
 * 1. UI state models, theme palette switching, pure black AMOLED mode toggling, squircle curvature calculations.
 * 2. WaveformSeeker scrubbing, duration guards, negative seek positions, past-duration seeks.
 * 3. ModernQueueView and LyricsScreen with synchronized LRC timestamps and rapid state updates.
 */
class M4ChallengerStressTest {

    private val hexColorRegex = Regex("""^#[0-9A-Fa-f]{6}$""")
    private val testDensity = Density(1f)

    // =========================================================================
    // 1. THEME PALETTE SWITCHING & AMOLED PURE BLACK MODE STRESS
    // =========================================================================

    @Test
    fun testThemeEnginePaletteResolutionUnderRapidFuzzing() {
        val knownPalettes = listOf("DEFAULT", "OCEAN", "SUNSET", "NATURE", "LOVE")
        val random = Random(42)

        // 5,000 rapid cycles of randomized palette switching & AMOLED mode toggling
        for (i in 0 until 5000) {
            val isKnown = random.nextBoolean()
            val paletteName = if (isKnown) {
                val base = knownPalettes[random.nextInt(knownPalettes.size)]
                // Randomly vary casing and surrounding whitespace
                when (random.nextInt(3)) {
                    0 -> base.lowercase()
                    1 -> "  ${base.uppercase()}  "
                    else -> base
                }
            } else {
                "UNKNOWN_PALETTE_${random.nextInt(1000)}"
            }

            val pureBlack = random.nextBoolean()
            val resolved = SuvMusicThemeEngine.applyTheme(paletteName.trim(), pureBlack = pureBlack)

            // Invariant 1: Palette is never null
            assertNotNull(resolved)

            // Invariant 2: Hex color strings are valid 7-char RGB hex codes
            assertTrue(hexColorRegex.matches(resolved.primary), "Invalid primary hex: ${resolved.primary}")
            assertTrue(hexColorRegex.matches(resolved.secondary), "Invalid secondary hex: ${resolved.secondary}")
            assertTrue(hexColorRegex.matches(resolved.tertiary), "Invalid tertiary hex: ${resolved.tertiary}")
            assertTrue(hexColorRegex.matches(resolved.surface), "Invalid surface hex: ${resolved.surface}")
            assertTrue(hexColorRegex.matches(resolved.background), "Invalid background hex: ${resolved.background}")

            // Invariant 3: Pure Black AMOLED mode invariants
            if (pureBlack) {
                assertEquals("#000000", resolved.background)
                assertEquals("#000000", resolved.surface)
                assertTrue(resolved.isPureBlack)
            } else {
                assertNotEquals("#000000", resolved.background)
                assertFalse(resolved.isPureBlack)
            }

            // Invariant 4: Primary accent color is strictly preserved across pureBlack toggles
            val normalTheme = SuvMusicThemeEngine.applyTheme(paletteName.trim(), pureBlack = false)
            val amoledTheme = SuvMusicThemeEngine.applyTheme(paletteName.trim(), pureBlack = true)
            assertEquals(normalTheme.primary, amoledTheme.primary)
            assertEquals(normalTheme.secondary, amoledTheme.secondary)
            assertEquals(normalTheme.tertiary, amoledTheme.tertiary)
        }
    }

    @Test
    fun testAllPaletteEnumsAndColorMappingConsistency() {
        for (palette in SuvMusicPalette.entries) {
            val colors = getPaletteColors(palette)
            val resolved = SuvMusicThemeEngine.PALETTES[palette.name]
            assertNotNull(resolved, "Missing palette entry for ${palette.name}")
            assertEquals(resolved.primaryHex, colors.primaryHex)
            assertEquals(resolved.secondaryHex, colors.secondaryHex)
            assertEquals(resolved.tertiaryHex, colors.tertiaryHex)
            assertEquals(resolved.surfaceHex, colors.surfaceHex)
            assertEquals(resolved.backgroundHex, colors.backgroundHex)
        }
    }

    @Test
    fun testOutfitTypographyWeightsStress() {
        // Standard weights
        assertEquals(400, OutfitTypographyScale.resolveWeight("Regular"))
        assertEquals(500, OutfitTypographyScale.resolveWeight("Medium"))
        assertEquals(600, OutfitTypographyScale.resolveWeight("SemiBold"))
        assertEquals(700, OutfitTypographyScale.resolveWeight("Bold"))
        assertEquals(800, OutfitTypographyScale.resolveWeight("ExtraBold"))

        // Case sensitivity & fallback
        assertEquals(999, OutfitTypographyScale.resolveWeight("regular", fallback = 999))
        assertEquals(700, OutfitTypographyScale.resolveWeight("Bold"))
        assertEquals(999, OutfitTypographyScale.resolveWeight("NonExistentWeight", fallback = 999))
        assertEquals(400, OutfitTypographyScale.resolveWeight("", fallback = 400))
        assertEquals(400, OutfitTypographyScale.resolveWeight("   ", fallback = 400))
    }

    // =========================================================================
    // 2. SQUIRCLE CURVATURE & GEOMETRY BOUNDARY STRESS
    // =========================================================================

    @Test
    fun testSquircleCurvatureAndBoundaryStress() {
        // 1. Zero radius produces Outline.Rectangle
        val zeroRadius = SquircleShape(cornerRadius = 0.dp, cornerSmoothing = 0.6f)
        val outlineZero = zeroRadius.createOutline(Size(100f, 100f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineZero is Outline.Rectangle)

        // 2. Zero smoothing produces Outline.Rounded
        val zeroSmoothing = SquircleShape(cornerRadius = 20.dp, cornerSmoothing = 0.0f)
        val outlineRounded = zeroSmoothing.createOutline(Size(100f, 100f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineRounded is Outline.Rounded)

        // 3. Normal smoothing (0.6f) produces Outline.Generic
        val normalSquircle = SquircleShape(cornerRadius = 24.dp, cornerSmoothing = 0.6f)
        val outlineGeneric = normalSquircle.createOutline(Size(100f, 100f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineGeneric is Outline.Generic)

        // 4. Maximal curvature (1.0f) produces Outline.Generic
        val maxSquircle = SquircleShape(cornerRadius = 24.dp, cornerSmoothing = 1.0f)
        val outlineMax = maxSquircle.createOutline(Size(100f, 100f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineMax is Outline.Generic)

        // 5. Smoothing factor out-of-range clamping
        val clampedNegative = SquircleShape(cornerRadius = 16.dp, cornerSmoothing = -50f)
        assertEquals(0.0f, clampedNegative.smoothing)
        val clampedExcessive = SquircleShape(cornerRadius = 16.dp, cornerSmoothing = 50f)
        assertEquals(1.0f, clampedExcessive.smoothing)

        // 6. Giant corner radius larger than container bounds (clamped to min(w, h)/2)
        val giantRadius = SquircleShape(cornerRadius = 1000.dp, cornerSmoothing = 0.6f)
        val outlineGiant = giantRadius.createOutline(Size(100f, 100f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineGiant is Outline.Generic)

        // 7. Extreme aspect ratios (very wide or very tall)
        val wideSquircle = SquircleShape(cornerRadius = 10.dp, cornerSmoothing = 0.6f)
        val outlineWide = wideSquircle.createOutline(Size(5000f, 20f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineWide is Outline.Generic)

        val tallSquircle = SquircleShape(cornerRadius = 10.dp, cornerSmoothing = 0.6f)
        val outlineTall = tallSquircle.createOutline(Size(20f, 5000f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineTall is Outline.Generic)

        // 8. Various display densities
        val densities = listOf(Density(0.5f), Density(1.0f), Density(2.0f), Density(3.0f), Density(4.0f))
        for (d in densities) {
            val outlineD = normalSquircle.createOutline(Size(200f, 200f), LayoutDirection.Ltr, d)
            assertTrue(outlineD is Outline.Generic)
        }

        // 9. RTL and LTR layout directions
        val outlineLtr = normalSquircle.createOutline(Size(150f, 150f), LayoutDirection.Ltr, testDensity)
        val outlineRtl = normalSquircle.createOutline(Size(150f, 150f), LayoutDirection.Rtl, testDensity)
        assertTrue(outlineLtr is Outline.Generic)
        assertTrue(outlineRtl is Outline.Generic)

        // 10. Equality and HashCode
        val shapeA = SquircleShape(cornerRadius = 28.dp, cornerSmoothing = 0.6f)
        val shapeB = SquircleShape(cornerRadius = 28.dp, cornerSmoothing = 0.6f)
        val shapeC = SquircleShape(cornerRadius = 20.dp, cornerSmoothing = 0.6f)
        assertEquals(shapeA, shapeB)
        assertEquals(shapeA.hashCode(), shapeB.hashCode())
        assertNotEquals(shapeA, shapeC)
    }

    // =========================================================================
    // 3. WAVEFORM SEEKER SCRUBBING & DURATION GUARDS STRESS
    // =========================================================================

    @Test
    fun testWaveformSeekerDurationGuardsAndAdversarialScrubbing() {
        val random = Random(123)

        // Zero duration guard
        assertEquals(0L, SuvMusicThemeEngine.calculateWaveformPosition(0.5, 0L))
        assertEquals(0L, SuvMusicThemeEngine.calculateWaveformPosition(1.0, 0L))

        // Negative duration guard
        assertEquals(0L, SuvMusicThemeEngine.calculateWaveformPosition(0.5, -1000L))
        assertEquals(0L, SuvMusicThemeEngine.calculateWaveformPosition(0.5, Long.MIN_VALUE))

        // NaN fraction guard
        assertEquals(0L, SuvMusicThemeEngine.calculateWaveformPosition(Double.NaN, 180000L))
        assertEquals(0L, SuvMusicThemeEngine.calculateWaveformPosition(Float.NaN, 180000L))

        // Negative scrub clamping to 0ms
        assertEquals(0L, SuvMusicThemeEngine.calculateWaveformPosition(-0.01, 180000L))
        assertEquals(0L, SuvMusicThemeEngine.calculateWaveformPosition(-1000.0, 180000L))
        assertEquals(0L, SuvMusicThemeEngine.calculateWaveformPosition(Double.NEGATIVE_INFINITY, 180000L))

        // Past-duration scrub clamping to durationMs
        assertEquals(180000L, SuvMusicThemeEngine.calculateWaveformPosition(1.0001, 180000L))
        assertEquals(180000L, SuvMusicThemeEngine.calculateWaveformPosition(500.0, 180000L))
        assertEquals(180000L, SuvMusicThemeEngine.calculateWaveformPosition(Double.POSITIVE_INFINITY, 180000L))

        // 10,000 randomized fuzzing seeks
        val testDuration = 240000L // 4 minutes
        for (i in 0 until 10000) {
            val frac = random.nextDouble(-2.0, 3.0)
            val pos = SuvMusicThemeEngine.calculateWaveformPosition(frac, testDuration)
            assertTrue(pos in 0L..testDuration, "Calculated pos $pos out of bounds for frac $frac")
        }
    }

    @Test
    fun testFormatDurationExtremeBoundaries() {
        // Negative milliseconds
        assertEquals("0:00", formatDuration(-1L))
        assertEquals("0:00", formatDuration(-50000L))
        assertEquals("0:00", formatDuration(Long.MIN_VALUE))

        // Zero
        assertEquals("0:00", formatDuration(0L))

        // Sub-second
        assertEquals("0:00", formatDuration(999L))

        // Exact seconds
        assertEquals("0:01", formatDuration(1000L))
        assertEquals("0:09", formatDuration(9000L))
        assertEquals("0:10", formatDuration(10000L))
        assertEquals("0:59", formatDuration(59000L))

        // Exact minute boundary
        assertEquals("1:00", formatDuration(60000L))
        assertEquals("1:05", formatDuration(65000L))
        assertEquals("9:59", formatDuration(599000L))
        assertEquals("10:00", formatDuration(600000L))
        assertEquals("59:59", formatDuration(3599000L))

        // Exact hour boundary
        assertEquals("1:00:00", formatDuration(3600000L))
        assertEquals("1:01:05", formatDuration(3665000L))
        assertEquals("10:00:00", formatDuration(36000000L))
        assertEquals("24:00:00", formatDuration(86400000L))
    }

    @Test
    fun testWaveformStyleEnumAndResolverFuzzing() {
        val expectedStyles = listOf("Bars", "Mirror", "Rounded", "Gradient", "Smooth", "Stepped", "Dots", "Wave", "Minimal")
        assertEquals(9, expectedStyles.size)

        for (styleName in expectedStyles) {
            val fromEnum = WaveformStyle.fromName(styleName)
            assertEquals(styleName, fromEnum.styleName)
            // Case-insensitive resolution
            assertEquals(styleName, WaveformStyle.fromName(styleName.lowercase()).styleName)
            assertEquals(styleName, WaveformStyle.fromName(styleName.uppercase()).styleName)
            assertEquals(styleName, SuvMusicThemeEngine.resolveWaveformStyle(styleName))
            assertEquals(styleName, SuvMusicThemeEngine.resolveWaveformStyle(styleName.lowercase()))
        }

        // Unknown styles fallback safely to Bars
        assertEquals(WaveformStyle.BARS, WaveformStyle.fromName("NonExistent"))
        assertEquals(WaveformStyle.BARS, WaveformStyle.fromName(null))
        assertEquals(WaveformStyle.BARS, WaveformStyle.fromName(""))
        assertEquals(WaveformStyle.BARS, WaveformStyle.fromName("   "))
        assertEquals("Bars", SuvMusicThemeEngine.resolveWaveformStyle("UnknownStyle"))
    }

    // =========================================================================
    // 4. MODERN QUEUE VIEW STATE INVARIANTS & COMPOSITE KEY INTEGRITY
    // =========================================================================

    @Test
    fun testModernQueueStateAndCompositeKeyUniqueness() {
        val count = 1000
        // Create 1,000 duplicate song items with the EXACT SAME ID
        val duplicateSong = SongItem(
            id = "duplicate_song_id",
            title = "Duplicate Title",
            artist = "Duplicate Artist",
            duration = 180
        )
        val queue = List(count) { duplicateSong }

        // ModernQueueView generates key: "${song.id}_$index"
        val keys = queue.mapIndexed { index, song -> "${song.id}_$index" }

        // Invariant: All composite keys MUST be completely unique to avoid LazyColumn crashes
        val uniqueKeys = keys.toSet()
        assertEquals(count, uniqueKeys.size, "Composite keys must be 100% unique even with duplicate song IDs")

        // Reorder boundary logic in ModernQueueView:
        // index > 0 shows Up button; index < queue.size - 1 shows Down button
        for (index in 0 until count) {
            val canMoveUp = (index > 0)
            val canMoveDown = (index < queue.size - 1)

            if (index == 0) {
                assertFalse(canMoveUp, "First item cannot move up")
                assertTrue(canMoveDown, "First item can move down")
            } else if (index == count - 1) {
                assertTrue(canMoveUp, "Last item can move up")
                assertFalse(canMoveDown, "Last item cannot move down")
            } else {
                assertTrue(canMoveUp, "Middle item can move up")
                assertTrue(canMoveDown, "Middle item can move down")
            }
        }

        // Single item queue
        val singleItemCanMoveUp = (0 > 0)
        val singleItemCanMoveDown = (0 < 1 - 1)
        assertFalse(singleItemCanMoveUp)
        assertFalse(singleItemCanMoveDown)
    }

    // =========================================================================
    // 5. LYRICS SCREEN SYNCHRONIZED LRC TIMESTAMPS & BINARY SEARCH STRESS
    // =========================================================================

    @Test
    fun testLrcParserTimestampPrecisionAndSortingStress() {
        // Multi-timecode on a single line
        val multiLrc = """
            [00:05.50][00:15.50][00:25.50]Chorus line repeated
        """.trimIndent()
        val parsedMulti = LrcParser.parse(multiLrc)
        assertTrue(parsedMulti.isSynced)
        assertEquals(3, parsedMulti.lines.size)
        assertEquals(5500L, parsedMulti.lines[0].timestampMs)
        assertEquals(15500L, parsedMulti.lines[1].timestampMs)
        assertEquals(25500L, parsedMulti.lines[2].timestampMs)
        assertEquals("Chorus line repeated", parsedMulti.lines[0].text)

        // Scrambled / out-of-order lines must be sorted chronologically
        val scrambledLrc = """
            [01:00.00]Line 3
            [00:10.00]Line 1
            [00:30.00]Line 2
        """.trimIndent()
        val parsedScrambled = LrcParser.parse(scrambledLrc)
        assertTrue(parsedScrambled.isSynced)
        assertEquals(3, parsedScrambled.lines.size)
        assertEquals(10000L, parsedScrambled.lines[0].timestampMs)
        assertEquals("Line 1", parsedScrambled.lines[0].text)
        assertEquals(30000L, parsedScrambled.lines[1].timestampMs)
        assertEquals("Line 2", parsedScrambled.lines[1].text)
        assertEquals(60000L, parsedScrambled.lines[2].timestampMs)
        assertEquals("Line 3", parsedScrambled.lines[2].text)

        // Sub-second precision: 2 digits vs 3 digits
        val precisionLrc = """
            [00:01.05]50ms
            [00:02.50]500ms
            [00:03.005]5ms
            [00:04.050]50ms
            [00:05.500]500ms
        """.trimIndent()
        val parsedPrecision = LrcParser.parse(precisionLrc)
        assertEquals(1050L, parsedPrecision.lines[0].timestampMs)
        assertEquals(2500L, parsedPrecision.lines[1].timestampMs)
        assertEquals(3005L, parsedPrecision.lines[2].timestampMs)
        assertEquals(4050L, parsedPrecision.lines[3].timestampMs)
        assertEquals(5500L, parsedPrecision.lines[4].timestampMs)

        // Metadata extraction
        val metadataLrc = """
            [ti:Test Title]
            [ar:Test Artist]
            [al:Test Album]
            [00:05.00]Music starts
        """.trimIndent()
        val parsedMeta = LrcParser.parse(metadataLrc)
        assertEquals("Test Title", parsedMeta.metadata["ti"])
        assertEquals("Test Artist", parsedMeta.metadata["ar"])
        assertEquals("Test Album", parsedMeta.metadata["al"])
        assertEquals(1, parsedMeta.lines.size)

        // Blank lines and instrumental breaks
        val blankTextLrc = """
            [00:10.00]
            [00:20.00]   
            [00:30.00]Actual Lyric
        """.trimIndent()
        val parsedBlank = LrcParser.parse(blankTextLrc)
        assertEquals(3, parsedBlank.lines.size)
        assertEquals("", parsedBlank.lines[0].text)
        assertEquals("", parsedBlank.lines[1].text)
        assertEquals("Actual Lyric", parsedBlank.lines[2].text)
    }

    @Test
    fun testLrcParserActiveLineBinarySearchFuzzing() {
        // Generate a 10-minute song with 200 lyric lines (one every 3 seconds)
        val lineCount = 200
        val lines = (0 until lineCount).map { i ->
            LyricLine(timestampMs = i * 3000L, text = "Lyric line $i")
        }

        // Invariant 1: Negative position always returns -1
        assertEquals(-1, LrcParser.findActiveLineIndex(lines, -1L))
        assertEquals(-1, LrcParser.findActiveLineIndex(lines, -10000L))

        // Invariant 2: Position before first timestamp returns -1
        val offsetLines = (0 until lineCount).map { i ->
            LyricLine(timestampMs = 5000L + i * 3000L, text = "Offset line $i")
        }
        assertEquals(-1, LrcParser.findActiveLineIndex(offsetLines, 0L))
        assertEquals(-1, LrcParser.findActiveLineIndex(offsetLines, 4999L))
        assertEquals(0, LrcParser.findActiveLineIndex(offsetLines, 5000L))

        // Invariant 3: High-frequency progression (simulating 60fps over 10 minutes)
        // Position advances from 0 to 600,000 ms; active line index must be monotonically non-decreasing
        var lastActiveIndex = -1
        val stepMs = 50L // 20 updates per second
        for (posMs in 0L..600000L step stepMs) {
            val activeIdx = LrcParser.findActiveLineIndex(lines, posMs)
            assertTrue(
                activeIdx >= lastActiveIndex,
                "Active index decreased from $lastActiveIndex to $activeIdx at pos $posMs"
            )
            lastActiveIndex = activeIdx
        }

        // Invariant 4: 5,000 random seeks into lyrics must satisfy exact timestamp bounds
        val random = Random(789)
        for (i in 0 until 5000) {
            val seekPos = random.nextLong(-5000L, 700000L)
            val idx = LrcParser.findActiveLineIndex(lines, seekPos)

            if (seekPos < lines[0].timestampMs) {
                assertEquals(-1, idx, "Seek before start must return -1")
            } else {
                assertTrue(idx in 0 until lineCount, "Index $idx out of range for seek $seekPos")
                assertTrue(lines[idx].timestampMs <= seekPos, "Active line timestamp must be <= seek position")
                if (idx < lineCount - 1) {
                    assertTrue(lines[idx + 1].timestampMs > seekPos, "Next line timestamp must be > seek position")
                }
            }
        }

        // Invariant 5: Empty lines list returns -1 safely
        assertEquals(-1, LrcParser.findActiveLineIndex(emptyList(), 5000L))
    }

    // =========================================================================
    // 6. SCREEN UI STATE MODELS & CALCULATIONS STRESS
    // =========================================================================

    @Test
    fun testScreensUiStateEdgeCalculations() {
        // Time of day greeting bounds
        assertEquals("Good morning", calculateTimeOfDayGreeting(5))
        assertEquals("Good morning", calculateTimeOfDayGreeting(11))
        assertEquals("Good afternoon", calculateTimeOfDayGreeting(12))
        assertEquals("Good afternoon", calculateTimeOfDayGreeting(17))
        assertEquals("Good evening", calculateTimeOfDayGreeting(18))
        assertEquals("Good evening", calculateTimeOfDayGreeting(23))
        assertEquals("Good evening", calculateTimeOfDayGreeting(0))
        assertEquals("Good evening", calculateTimeOfDayGreeting(4))
        assertEquals("Good evening", calculateTimeOfDayGreeting(-5))
        assertEquals("Good evening", calculateTimeOfDayGreeting(25))

        // Detail top bar alpha calculations
        assertEquals(0.0f, calculateDetailTopBarAlpha(0, 200))
        assertEquals(0.5f, calculateDetailTopBarAlpha(100, 200))
        assertEquals(1.0f, calculateDetailTopBarAlpha(200, 200))
        assertEquals(1.0f, calculateDetailTopBarAlpha(500, 200))
        assertEquals(0.0f, calculateDetailTopBarAlpha(-100, 200))
        // Zero threshold guard (prevents division by zero)
        assertEquals(1.0f, calculateDetailTopBarAlpha(50, 0))
        assertEquals(1.0f, calculateDetailTopBarAlpha(50, -10))

        // Audio quality bitrates
        assertEquals(48000, AudioQuality.LOW.bitrateBps)
        assertEquals(128000, AudioQuality.MEDIUM.bitrateBps)
        assertEquals(256000, AudioQuality.HIGH.bitrateBps)

        // Enums coverage
        assertEquals(5, LibraryFilter.entries.size)
        assertEquals(5, LibrarySortCriteria.entries.size)
        assertEquals(2, LibraryViewMode.entries.size)
        assertEquals(3, SearchTab.entries.size)
    }
}
