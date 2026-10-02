package com.omnitune.shared

import com.omnitune.shared.data.innertube.AudioFormatCandidate
import com.omnitune.shared.data.innertube.PoTokenGenerator
import com.omnitune.shared.data.innertube.PureSha256
import com.omnitune.shared.data.innertube.StreamResolver
import com.omnitune.shared.data.lyrics.LrcParser
import com.omnitune.shared.data.lyrics.ParsedLrc
import com.omnitune.shared.domain.models.LyricLine
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Adversarial stress harness and empirical challenge suite for Milestone 2.
 * Focuses on:
 * 1. PoToken determinism, random salt uniqueness, boundary timestamps, bit-flip tampering, and payload corruption.
 * 2. StreamResolver candidate format edge cases: empty list, WebM only, AAC fallback, zero bitrate, duplicate itags, unknown profiles, URL expiration boundaries.
 * 3. LrcParser malformed lines: missing brackets, non-numeric timestamps, empty lines, trailing spaces, out-of-order timestamps, large payloads, binary search edge cases.
 */
class M2AdversarialStressTest {

    // =========================================================================
    // 1. POTOKEN DETERMINISM & RANDOM SALT ROBUSTNESS
    // =========================================================================

    @Test
    fun testPoTokenDeterminismWithFixedSalt() {
        val visitor = "FixedVisitorDeterministic123"
        val timestamp = 1774900000L
        val fixedSalt = ByteArray(16) { it.toByte() }

        val baselineToken = PoTokenGenerator.generatePoToken(
            visitorData = visitor,
            timestamp = timestamp,
            salt = fixedSalt.copyOf()
        )

        // Repeat 500 times to verify absolute determinism with fixed salt
        for (i in 0 until 500) {
            val token = PoTokenGenerator.generatePoToken(
                visitorData = visitor,
                timestamp = timestamp,
                salt = fixedSalt.copyOf()
            )
            assertEquals(baselineToken, token, "PoToken must be strictly deterministic with identical inputs and salt (iteration $i)")
        }

        // Verify the deterministic token round-trips correctly
        val verified = PoTokenGenerator.verifyPoToken(baselineToken)
        assertTrue(verified.valid)
        assertEquals(visitor, verified.visitorData)
        assertEquals(timestamp, verified.timestamp)
    }

    @Test
    fun testPoTokenRandomSaltUniquenessAcrossRuns() {
        val visitor = "UniqueVisitorSaltTest"
        val timestamp = 1774900000L

        val tokenSet = mutableSetOf<String>()
        val count = 200

        for (i in 0 until count) {
            val token = PoTokenGenerator.generatePoToken(
                visitorData = visitor,
                timestamp = timestamp,
                salt = null // Triggers random salt generation
            )
            val added = tokenSet.add(token)
            assertTrue(added, "Token generated with random salt must be unique across runs (collision at index $i)")

            val verified = PoTokenGenerator.verifyPoToken(token)
            assertTrue(verified.valid, "Every token generated with random salt must verify cleanly")
            assertEquals(visitor, verified.visitorData)
            assertEquals(timestamp, verified.timestamp)
        }
        assertEquals(count, tokenSet.size)
    }

    @Test
    fun testPoTokenExtremeTimestamps() {
        val extremeTimestamps = listOf(
            0L,                       // Epoch
            1L,                       // Epoch + 1
            2147483647L,              // 32-bit signed int max (Year 2038)
            4102444800L,              // Year 2100
            0x7FFFFFFFFFFFFFFFL       // Long.MAX_VALUE
        )

        for (ts in extremeTimestamps) {
            val token = PoTokenGenerator.generatePoToken(visitorData = "TsBoundary", timestamp = ts)
            val verified = PoTokenGenerator.verifyPoToken(token)
            assertTrue(verified.valid, "Failed on timestamp $ts")
            assertEquals(ts, verified.timestamp, "Timestamp mismatch for $ts")
        }
    }

    @Test
    fun testPoTokenExtremeVisitorData() {
        // Empty visitor data
        val emptyToken = PoTokenGenerator.generatePoToken(visitorData = "")
        val emptyVer = PoTokenGenerator.verifyPoToken(emptyToken)
        assertTrue(emptyVer.valid)
        assertEquals("", emptyVer.visitorData)

        // Unicode / Multi-byte characters
        val unicodeVisitor = "🎶 OmniTune 測試 🚀 日本語 العربية 🌍"
        val unicodeToken = PoTokenGenerator.generatePoToken(visitorData = unicodeVisitor)
        val unicodeVer = PoTokenGenerator.verifyPoToken(unicodeToken)
        assertTrue(unicodeVer.valid)
        assertEquals(unicodeVisitor, unicodeVer.visitorData)

        // Large visitor data (4096 characters)
        val largeVisitor = "X".repeat(4096)
        val largeToken = PoTokenGenerator.generatePoToken(visitorData = largeVisitor)
        val largeVer = PoTokenGenerator.verifyPoToken(largeToken)
        assertTrue(largeVer.valid)
        assertEquals(largeVisitor, largeVer.visitorData)
    }

    @Test
    fun testPoTokenBitFlipTamperingResistance() {
        val originalToken = PoTokenGenerator.generatePoToken()
        val verifiedOrig = PoTokenGenerator.verifyPoToken(originalToken)
        assertTrue(verifiedOrig.valid)

        // Decode URL-safe Base64 into bytes
        var b64 = originalToken.replace('-', '+').replace('_', '/')
        while (b64.length % 4 != 0) b64 += "="
        val rawBytes = kotlin.io.encoding.Base64.Default.decode(b64)

        // Flip 1 bit at every single byte position in the packet: salt, payload, and HMAC
        for (byteIdx in rawBytes.indices) {
            val tamperedBytes = rawBytes.copyOf()
            tamperedBytes[byteIdx] = (tamperedBytes[byteIdx].toInt() xor 0x01).toByte()

            val tamperedToken = kotlin.io.encoding.Base64.UrlSafe.encode(tamperedBytes).trimEnd('=')
            val tamperedVer = PoTokenGenerator.verifyPoToken(tamperedToken)
            assertFalse(tamperedVer.valid, "Tampered token at byte $byteIdx must fail verification")
        }
    }

    @Test
    fun testPoTokenMalformedAndTruncatedInputs() {
        val inputs = listOf(
            "",
            "A",
            "==",
            "!!!InvalidCharacters###",
            "AAAA".repeat(10), // Too short to contain [salt(16) + len(2) + ts(8) + salt(16) + hmac(32)] = 74 bytes
            "YWJj",
            "dG9vLXNob3J0LWZvci1wb3Rva2Vu",
            "aHR0cHM6Ly9leGFtcGxlLmNvbQ=="
        )

        for (input in inputs) {
            val result = PoTokenGenerator.verifyPoToken(input)
            assertFalse(result.valid, "Malformed input '$input' should fail verification gracefully without throwing")
        }
    }

    // =========================================================================
    // 2. STREAM RESOLVER ADVERSARIAL FORMAT SELECTION & STRESS
    // =========================================================================

    @Test
    fun testStreamResolverEmptyCandidateList() {
        val result = StreamResolver.selectBestAudioFormat(emptyList())
        assertNull(result, "Empty candidate list must return null safely")
    }

    @Test
    fun testStreamResolverWebmOnlyCandidates() {
        // When AAC (140/139) is absent, should pick highest bitrate WebM
        val candidates = listOf(
            AudioFormatCandidate(itag = 249, container = "webm", codec = "opus", bitrate = 50000, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 250, container = "webm", codec = "opus", bitrate = 70000, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 251, container = "webm", codec = "opus", bitrate = 160000, mimeType = "audio/webm")
        )

        val selected = StreamResolver.selectBestAudioFormat(candidates)
        assertNotNull(selected)
        assertEquals(251, selected.itag, "Should choose highest bitrate WebM (251) when no AAC candidate exists")
        assertEquals(160000, selected.bitrate)
    }

    @Test
    fun testStreamResolverAacOnlyWithoutStandardItags() {
        // AAC/MP4 formats that are not itag 140 or 139, but still mp4
        val candidates = listOf(
            AudioFormatCandidate(itag = 251, container = "webm", codec = "opus", bitrate = 160000, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 256, container = "m4a", codec = "mp4a.40.2", bitrate = 192000, mimeType = "audio/mp4"),
            AudioFormatCandidate(itag = 258, container = "m4a", codec = "mp4a.40.2", bitrate = 384000, mimeType = "audio/mp4")
        )

        val selected = StreamResolver.selectBestAudioFormat(candidates)
        assertNotNull(selected)
        assertEquals(258, selected.itag, "Should prioritize mp4 and select highest bitrate non-standard itag")
        assertEquals(384000, selected.bitrate)
    }

    @Test
    fun testStreamResolverZeroAndNegativeBitrates() {
        val candidates = listOf(
            AudioFormatCandidate(itag = 999, container = "webm", codec = "opus", bitrate = 0, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 998, container = "webm", codec = "opus", bitrate = -100, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 997, container = "webm", codec = "opus", bitrate = 1000, mimeType = "audio/webm")
        )

        val selected = StreamResolver.selectBestAudioFormat(candidates)
        assertNotNull(selected)
        assertEquals(997, selected.itag, "Should select positive bitrate over zero or negative")
    }

    @Test
    fun testStreamResolverDuplicateCandidatesDeterministicSelection() {
        val candidates = listOf(
            AudioFormatCandidate(itag = 140, container = "m4a", codec = "mp4a.40.2", bitrate = 128000, mimeType = "audio/mp4"),
            AudioFormatCandidate(itag = 140, container = "m4a", codec = "mp4a.40.2", bitrate = 128000, mimeType = "audio/mp4")
        )

        val selected = StreamResolver.selectBestAudioFormat(candidates)
        assertNotNull(selected)
        assertEquals(140, selected.itag)
    }

    @Test
    fun testStreamResolverMimeTypeCaseInsensitivity() {
        val candidates = listOf(
            AudioFormatCandidate(itag = 300, container = "m4a", codec = "mp4a.40.2", bitrate = 128000, mimeType = "AUDIO/MP4"),
            AudioFormatCandidate(itag = 301, container = "webm", codec = "opus", bitrate = 200000, mimeType = "audio/webm")
        )

        val selected = StreamResolver.selectBestAudioFormat(candidates)
        assertNotNull(selected)
        assertEquals(300, selected.itag, "Case-insensitive match on 'mp4' should prefer AUDIO/MP4 over audio/webm")
    }

    @Test
    fun testStreamResolverUnknownProfileThrows() {
        assertFailsWith<IllegalArgumentException> {
            StreamResolver.resolveStream("test_vid", clientProfileName = "UNKNOWN_PROFILE_XYZ")
        }
    }

    @Test
    fun testStreamResolverAllKnownProfilesResolveDirectAac() {
        val profiles = listOf("ANDROID_VR_NO_AUTH", "ANDROID_VR_1_61_48", "IPADOS", "IOS")
        for (profile in profiles) {
            val stream = StreamResolver.resolveStream("test_profile_vid", clientProfileName = profile)
            assertEquals("test_profile_vid", stream.videoId)
            assertEquals(140, stream.itag)
            assertEquals("audio/mp4", stream.mimeType)
            assertTrue(stream.streamUrl.contains("c=$profile"))
            assertTrue(stream.headers.containsKey("User-Agent"))
            assertFalse(stream.isExpired(com.omnitune.shared.data.network.currentTimeMillis()))
        }
    }

    @Test
    fun testStreamResolverIsExpiredBoundaries() {
        val now = 1774900000L
        assertTrue(StreamResolver.isExpired("https://example.com/stream?expire=${now}&id=1", now))
        assertTrue(StreamResolver.isExpired("https://example.com/stream?expire=${now - 1}&id=1", now))
        assertFalse(StreamResolver.isExpired("https://example.com/stream?expire=${now + 1}&id=1", now))

        // Missing expire param
        assertTrue(StreamResolver.isExpired("https://example.com/stream?id=1", now))
        // Malformed expire param
        assertTrue(StreamResolver.isExpired("https://example.com/stream?expire=NOT_A_NUMBER&id=1", now))
        assertTrue(StreamResolver.isExpired("https://example.com/stream?expire=&id=1", now))
    }

    // =========================================================================
    // 3. LRC PARSER ADVERSARIAL MALFORMED LINES & EDGE CASES
    // =========================================================================

    @Test
    fun testLrcParserMissingBrackets() {
        val lrc = """
            01:23.45 Missing opening bracket
            [01:25.50 Missing closing bracket
            [01:28.00]Valid Bracket Line
            01:30.00]Missing opening only
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        // Only valid line should be treated as synced
        assertTrue(parsed.isSynced)
        assertEquals(1, parsed.lines.size)
        assertEquals(88000L, parsed.lines[0].timestampMs)
        assertEquals("Valid Bracket Line", parsed.lines[0].text)
    }

    @Test
    fun testLrcParserNonNumericTimestamps() {
        val lrc = """
            [aa:bb.cc]Alphabetical timestamp
            [--:--.--]Dashes timestamp
            [1a:2b.3c]Alphanumeric timestamp
            [::]Empty colons
            []Empty brackets
            [00:10.00]Valid timestamp
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertTrue(parsed.isSynced)
        assertEquals(1, parsed.lines.size)
        assertEquals(10000L, parsed.lines[0].timestampMs)
        assertEquals("Valid timestamp", parsed.lines[0].text)
    }

    @Test
    fun testLrcParserWhitespaceAndEmptyLines() {
        val lrc = """

            [00:01.00]Line 1
            
               
            [00:02.00]Line 2   
            	
            [00:03.00]   Line 3 with leading space in text
            
        """

        val parsed = LrcParser.parse(lrc)
        assertTrue(parsed.isSynced)
        assertEquals(3, parsed.lines.size)
        assertEquals(1000L, parsed.lines[0].timestampMs)
        assertEquals("Line 1", parsed.lines[0].text)
        assertEquals(2000L, parsed.lines[1].timestampMs)
        assertEquals("Line 2", parsed.lines[1].text)
        assertEquals(3000L, parsed.lines[2].timestampMs)
        assertEquals("Line 3 with leading space in text", parsed.lines[2].text)
    }

    @Test
    fun testLrcParserOutOrOrderTimestampsSorted() {
        val lrc = """
            [00:30.00]Third Line
            [00:10.00]First Line
            [00:20.00]Second Line
            [00:05.50]Zeroth Line
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertTrue(parsed.isSynced)
        assertEquals(4, parsed.lines.size)
        assertEquals(5500L, parsed.lines[0].timestampMs)
        assertEquals("Zeroth Line", parsed.lines[0].text)
        assertEquals(10000L, parsed.lines[1].timestampMs)
        assertEquals("First Line", parsed.lines[1].text)
        assertEquals(20000L, parsed.lines[2].timestampMs)
        assertEquals("Second Line", parsed.lines[2].text)
        assertEquals(30000L, parsed.lines[3].timestampMs)
        assertEquals("Third Line", parsed.lines[3].text)
    }

    @Test
    fun testLrcParserDuplicateTimestampsPreserved() {
        val lrc = """
            [00:10.00]Voice 1
            [00:10.00]Voice 2 Harmony
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertTrue(parsed.isSynced)
        assertEquals(2, parsed.lines.size)
        assertEquals(10000L, parsed.lines[0].timestampMs)
        assertEquals("Voice 1", parsed.lines[0].text)
        assertEquals(10000L, parsed.lines[1].timestampMs)
        assertEquals("Voice 2 Harmony", parsed.lines[1].text)
    }

    @Test
    fun testLrcParserSingleDigitMinutesAndSeconds() {
        val lrc = """
            [1:23.45]Single digit minute
            [01:2.45]Single digit second should not match 2-digit second regex
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertTrue(parsed.isSynced)
        assertEquals(1, parsed.lines.size)
        assertEquals(83450L, parsed.lines[0].timestampMs)
        assertEquals("Single digit minute", parsed.lines[0].text)
    }

    @Test
    fun testLrcParserAllMalformedYieldsUnsynced() {
        val lrc = """
            Just regular text without any LRC tags
            Another line of plain text
            Third line of plain text
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertFalse(parsed.isSynced)
        assertEquals(3, parsed.lines.size)
        assertTrue(parsed.lines.all { it.timestampMs == -1L })
    }

    @Test
    fun testLrcParserBlankOrNullInput() {
        val nullResult = LrcParser.parse(null)
        assertFalse(nullResult.isSynced)
        assertTrue(nullResult.lines.isEmpty())
        assertTrue(nullResult.metadata.isEmpty())

        val blankResult = LrcParser.parse("   \n\t  \r\n   ")
        assertFalse(blankResult.isSynced)
        assertTrue(blankResult.lines.isEmpty())
        assertTrue(blankResult.metadata.isEmpty())
    }

    @Test
    fun testLrcParserFindActiveLineIndexExhaustive() {
        val lines = listOf(
            LyricLine(1000L, "First"),
            LyricLine(3000L, "Second"),
            LyricLine(5000L, "Third"),
            LyricLine(8000L, "Fourth")
        )

        // Empty list
        assertEquals(-1, LrcParser.findActiveLineIndex(emptyList(), 2000L))

        // Negative position
        assertEquals(-1, LrcParser.findActiveLineIndex(lines, -1L))
        assertEquals(-1, LrcParser.findActiveLineIndex(lines, -999999L))

        // Before first line
        assertEquals(-1, LrcParser.findActiveLineIndex(lines, 0L))
        assertEquals(-1, LrcParser.findActiveLineIndex(lines, 999L))

        // Exact hits
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 1000L))
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 3000L))
        assertEquals(2, LrcParser.findActiveLineIndex(lines, 5000L))
        assertEquals(3, LrcParser.findActiveLineIndex(lines, 8000L))

        // In-between hits
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 1001L))
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 2999L))
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 3500L))
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 4999L))
        assertEquals(2, LrcParser.findActiveLineIndex(lines, 7999L))

        // Far beyond last line
        assertEquals(3, LrcParser.findActiveLineIndex(lines, 8001L))
        assertEquals(3, LrcParser.findActiveLineIndex(lines, 1000000000L))
    }

    @Test
    fun testLrcParserLargeDocumentStress() {
        val sb = StringBuilder()
        sb.append("[ti:Stress Test Song]\n[ar:Stress Artist]\n")
        val lineCount = 2000
        for (i in 0 until lineCount) {
            val min = i / 60
            val sec = i % 60
            sb.append("[%02d:%02d.00] Lyric line index $i\n".format(min, sec))
        }

        val parsed = LrcParser.parse(sb.toString())
        assertTrue(parsed.isSynced)
        assertEquals("Stress Test Song", parsed.metadata["ti"])
        assertEquals("Stress Artist", parsed.metadata["ar"])
        assertEquals(lineCount, parsed.lines.size)

        // Verify binary search on 2000 lines
        assertEquals(0, LrcParser.findActiveLineIndex(parsed.lines, 0L))
        assertEquals(59, LrcParser.findActiveLineIndex(parsed.lines, 59000L))
        assertEquals(lineCount - 1, LrcParser.findActiveLineIndex(parsed.lines, 99999999L))
    }
}
