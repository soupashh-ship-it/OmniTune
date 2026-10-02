package com.omnitune.shared

import com.omnitune.shared.data.innertube.AudioFormatCandidate
import com.omnitune.shared.data.innertube.PoTokenGenerator
import com.omnitune.shared.data.innertube.PureSha256
import com.omnitune.shared.data.innertube.StreamResolver
import com.omnitune.shared.data.lyrics.LrcParser
import com.omnitune.shared.domain.models.LyricLine
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Empirical Challenger M2 Stress Test Harness.
 * Uses JVM cryptographic oracles (MessageDigest, Mac) and differential fuzzing
 * to challenge PureSha256, PoTokenGenerator, LrcParser, and StreamResolver.
 */
class M2ChallengerStressTest {

    // =========================================================================
    // 1. DIFFERENTIAL TESTING: PureSha256 vs JVM MessageDigest Oracle
    // =========================================================================

    private fun jvmSha256(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data)
    }

    private fun jvmHmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data)
    }

    @Test
    fun testSha256PaddingBoundariesAgainstJvmOracle() {
        // Test key length boundaries:
        // SHA-256 block size = 64 bytes.
        // Padding adds 1 byte (0x80) + padding zeros + 8 bytes bit length.
        // Critical boundaries around 55, 56, 63, 64, 65, 119, 120, 128 bytes.
        val testLengths = listOf(0, 1, 54, 55, 56, 57, 63, 64, 65, 119, 120, 121, 127, 128, 129, 512, 1024, 65536)

        for (len in testLengths) {
            val data = ByteArray(len) { i -> (i and 0xFF).toByte() }
            val expected = jvmSha256(data)
            val actual = PureSha256.digest(data)
            assertTrue(
                expected.contentEquals(actual),
                "SHA-256 mismatch against JVM oracle at length $len bytes"
            )
        }
    }

    @Test
    fun testSha256FuzzAgainstJvmOracle() {
        val rng = Random(42)
        repeat(200) { iter ->
            val len = rng.nextInt(0, 2048)
            val data = ByteArray(len)
            rng.nextBytes(data)

            val expected = jvmSha256(data)
            val actual = PureSha256.digest(data)
            assertTrue(
                expected.contentEquals(actual),
                "Fuzz iteration $iter failed at length $len"
            )
        }
    }

    // =========================================================================
    // 2. DIFFERENTIAL TESTING: PureSha256.hmac vs JVM Mac Oracle
    // =========================================================================

    @Test
    fun testHmacSha256AgainstJvmOracleAcrossKeySizes() {
        // RFC 2104: Keys > 64 bytes must be hashed first by HMAC algorithm.
        val keySizes = listOf(1, 16, 32, 63, 64, 65, 80, 128, 256)
        val dataSizes = listOf(0, 1, 32, 64, 128, 1000)

        for (kSize in keySizes) {
            val key = ByteArray(kSize) { i -> (i * 7 and 0xFF).toByte() }
            for (dSize in dataSizes) {
                val data = ByteArray(dSize) { i -> (i * 13 and 0xFF).toByte() }

                val expected = jvmHmacSha256(key, data)
                val actual = PureSha256.hmac(key, data)
                assertTrue(
                    expected.contentEquals(actual),
                    "HMAC mismatch with key size $kSize and data size $dSize"
                )
            }
        }
    }

    @Test
    fun testHmacFuzzAgainstJvmOracle() {
        val rng = Random(1337)
        repeat(150) { iter ->
            val kSize = rng.nextInt(1, 200) // including >64 key sizes
            val dSize = rng.nextInt(0, 1000)
            val key = ByteArray(kSize)
            val data = ByteArray(dSize)
            rng.nextBytes(key)
            rng.nextBytes(data)

            val expected = jvmHmacSha256(key, data)
            val actual = PureSha256.hmac(key, data)
            assertTrue(
                expected.contentEquals(actual),
                "HMAC fuzz iteration $iter failed with kSize=$kSize, dSize=$dSize"
            )
        }
    }

    // =========================================================================
    // 3. ADVERSARIAL STRESS: PoTokenGenerator Fuzzing & Mutation
    // =========================================================================

    @Test
    fun testPoTokenUnicodeAndSpecialCharactersVisitorData() {
        val unicodeStrings = listOf(
            "日本語テストVisitor_🎵_12345",
            "مرحبا بالعالم - Visitor Data - 🚀",
            "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?`~",
            "Newline \n and \r\n and tabs \t",
            "Empty: ",
            "A".repeat(2000)
        )

        for (uStr in unicodeStrings) {
            val token = PoTokenGenerator.generatePoToken(visitorData = uStr)
            val result = PoTokenGenerator.verifyPoToken(token)
            assertTrue(result.valid, "Failed to verify token for visitorData: $uStr")
            assertEquals(uStr, result.visitorData)
        }
    }

    @Test
    fun testPoTokenExhaustiveBitFlipTamperDetection() {
        // Generate a valid token
        val token = PoTokenGenerator.generatePoToken(visitorData = "SecurePayload")
        val verifiedOriginal = PoTokenGenerator.verifyPoToken(token)
        assertTrue(verifiedOriginal.valid)

        // Decode packet bytes
        var b64 = token.replace('-', '+').replace('_', '/')
        while (b64.length % 4 != 0) b64 += "="
        val rawPacket = kotlin.io.encoding.Base64.Default.decode(b64)

        // Flip a bit in every single byte position from 0 to end
        // Every single tampered packet MUST fail verification
        for (i in rawPacket.indices) {
            val tampered = rawPacket.copyOf()
            tampered[i] = (tampered[i].toInt() xor 0x01).toByte()

            val tamperedToken = kotlin.io.encoding.Base64.UrlSafe.encode(tampered).trimEnd('=')
            val verified = PoTokenGenerator.verifyPoToken(tamperedToken)
            assertFalse(
                verified.valid,
                "Tampered bit at byte index $i did not trigger HMAC/integrity failure!"
            )
        }
    }

    @Test
    fun testPoTokenRapidBurstGeneration() {
        val count = 500
        val tokens = HashSet<String>(count)
        val startTime = System.currentTimeMillis()

        for (i in 0 until count) {
            val token = PoTokenGenerator.generatePoToken(visitorData = "BurstUser_$i")
            tokens.add(token)
        }
        val elapsedMs = System.currentTimeMillis() - startTime

        // All 500 tokens must be completely unique due to random salt
        assertEquals(count, tokens.size, "Generated tokens contained collisions!")
        assertTrue(elapsedMs < 3000, "500 token generation took too long: ${elapsedMs}ms")
    }

    // =========================================================================
    // 4. ADVERSARIAL STRESS: LrcParser Edge Cases & Anomalies
    // =========================================================================

    @Test
    fun testLrcParserWithUtf8BomAndMixedLineEndings() {
        // Byte Order Mark (\uFEFF) at start of file
        val lrcWithBom = "\uFEFF[00:01.50]Line with BOM\r\n[00:05.00]Line with CRLF\r[00:10.00]Line with CR\n[00:15.00]Line with LF"
        val result = LrcParser.parse(lrcWithBom)

        assertTrue(result.isSynced)
        assertEquals(4, result.lines.size)
        assertEquals(1500L, result.lines[0].timestampMs)
        assertEquals("Line with BOM", result.lines[0].text)
        assertEquals(5000L, result.lines[1].timestampMs)
        assertEquals(10000L, result.lines[2].timestampMs)
        assertEquals(15000L, result.lines[3].timestampMs)
    }

    @Test
    fun testLrcParserSubSecondPrecisionVariations() {
        // Standard centiseconds (2 digits), milliseconds (3 digits), and boundary cases
        val lrc = """
            [01:00.00]Zero
            [01:00.05]50 ms
            [01:00.50]500 ms
            [01:00.005]5 ms
            [01:00.999]999 ms
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertTrue(parsed.isSynced)
        // LrcParser sorts lines chronologically: 60000ms, 60005ms (5ms), 60050ms (50ms), 60500ms (500ms), 60999ms (999ms)
        assertEquals(5, parsed.lines.size)
        assertEquals(60000L, parsed.lines[0].timestampMs)
        assertEquals(60005L, parsed.lines[1].timestampMs)
        assertEquals("5 ms", parsed.lines[1].text)
        assertEquals(60050L, parsed.lines[2].timestampMs)
        assertEquals("50 ms", parsed.lines[2].text)
        assertEquals(60500L, parsed.lines[3].timestampMs)
        assertEquals("500 ms", parsed.lines[3].text)
        assertEquals(60999L, parsed.lines[4].timestampMs)
        assertEquals("999 ms", parsed.lines[4].text)
    }

    @Test
    fun testLrcParserFindActiveLineIndexStressScenarios() {
        val emptyLines = emptyList<LyricLine>()
        assertEquals(-1, LrcParser.findActiveLineIndex(emptyLines, 5000L))
        assertEquals(-1, LrcParser.findActiveLineIndex(emptyLines, -100L))

        val singleLine = listOf(LyricLine(timestampMs = 10000L, text = "Only line"))
        assertEquals(-1, LrcParser.findActiveLineIndex(singleLine, 0L))
        assertEquals(-1, LrcParser.findActiveLineIndex(singleLine, 9999L))
        assertEquals(0, LrcParser.findActiveLineIndex(singleLine, 10000L))
        assertEquals(0, LrcParser.findActiveLineIndex(singleLine, 999999L))

        // Identical timestamps on consecutive lines
        val duplicateTimestamps = listOf(
            LyricLine(timestampMs = 5000L, text = "Chorus 1"),
            LyricLine(timestampMs = 5000L, text = "Chorus 2"),
            LyricLine(timestampMs = 10000L, text = "Outro")
        )
        // Should find one of the duplicate lines (index 0 or 1) without crashing or looping
        val activeIdx = LrcParser.findActiveLineIndex(duplicateTimestamps, 5000L)
        assertTrue(activeIdx in 0..1, "Active index for duplicate timestamps must be 0 or 1, was $activeIdx")
    }

    // =========================================================================
    // 5. ADVERSARIAL STRESS: StreamResolver Edge Cases
    // =========================================================================

    @Test
    fun testStreamResolverSpecialCharactersInVideoId() {
        val videoIds = listOf(
            "dQw4w9WgXcQ",
            "special_chars-123",
            "abc def", // space
            "video/slash",
            "video?query=param&another=value",
            "emoji_🎵_video"
        )

        for (vid in videoIds) {
            val stream = StreamResolver.resolveStream(vid)
            assertNotNull(stream.streamUrl)
            assertTrue(stream.streamUrl.contains("id=$vid"))
            assertEquals(vid, stream.videoId)
            assertTrue(stream.bitrate > 0)
            assertEquals("audio/mp4", stream.mimeType)
        }
    }

    @Test
    fun testStreamResolverFormatPreferenceOrdering() {
        // Test format selection when candidate order is scrambled or missing preferred itags
        val candidate1 = AudioFormatCandidate(itag = 251, container = "webm", codec = "opus", bitrate = 160000, mimeType = "audio/webm")
        val candidate2 = AudioFormatCandidate(itag = 139, container = "m4a", codec = "mp4a.40.2", bitrate = 48000, mimeType = "audio/mp4")
        val candidate3 = AudioFormatCandidate(itag = 140, container = "m4a", codec = "mp4a.40.2", bitrate = 128000, mimeType = "audio/mp4")

        // 140 should ALWAYS win over 251 even if 251 has higher bitrate
        val selected = StreamResolver.selectBestAudioFormat(listOf(candidate1, candidate2, candidate3))
        assertEquals(140, selected?.itag)

        // If 140 is missing, 139 should win because it is direct AAC
        val selectedWithout140 = StreamResolver.selectBestAudioFormat(listOf(candidate1, candidate2))
        assertEquals(139, selectedWithout140?.itag)

        // If both 140 and 139 are missing, pick mp4 over webm, or highest bitrate
        val candidateMp4Generic = AudioFormatCandidate(itag = 999, container = "mp4", codec = "aac", bitrate = 64000, mimeType = "audio/mp4")
        val selectedFallback = StreamResolver.selectBestAudioFormat(listOf(candidate1, candidateMp4Generic))
        assertEquals(999, selectedFallback?.itag)
    }

    @Test
    fun testStreamResolverExpirationExactBoundaries() {
        val now = 1000000L
        val urlFuture = "https://rr1---sn.googlevideo.com/videoplayback?expire=${now + 60}"
        val urlCurrent = "https://rr1---sn.googlevideo.com/videoplayback?expire=$now"
        val urlPast = "https://rr1---sn.googlevideo.com/videoplayback?expire=${now - 1}"
        val urlMalformed = "https://rr1---sn.googlevideo.com/videoplayback?other=123"

        assertFalse(StreamResolver.isExpired(urlFuture, currentTimeSec = now))
        // At exact expiration boundary, considered expired
        assertTrue(StreamResolver.isExpired(urlCurrent, currentTimeSec = now))
        assertTrue(StreamResolver.isExpired(urlPast, currentTimeSec = now))
        assertTrue(StreamResolver.isExpired(urlMalformed, currentTimeSec = now))
    }
}
