package com.omnitune.shared.data.innertube

import com.omnitune.shared.data.network.currentTimeMillis
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@Serializable
data class PoTokenVerificationResult(
    val valid: Boolean,
    val visitorData: String? = null,
    val timestamp: Long? = null,
    val ageSec: Long? = null,
    val error: String? = null,
)

@OptIn(ExperimentalEncodingApi::class)
object PoTokenGenerator {

    private const val DEFAULT_VISITOR_DATA = "CgtEUkVDQkExMlFBSRjA"

    fun generatePoToken(
        visitorData: String = DEFAULT_VISITOR_DATA,
        timestamp: Long = currentTimeMillis() / 1000,
        salt: ByteArray? = null,
    ): String {
        val actualSalt = salt ?: ByteArray(16).also { Random.nextBytes(it) }
        val visitorBytes = visitorData.encodeToByteArray()

        val headerLength = 2 + visitorBytes.size + 8 + actualSalt.size
        val header = ByteArray(headerLength)

        // UInt16BE visitor length
        header[0] = ((visitorBytes.size ushr 8) and 0xFF).toByte()
        header[1] = (visitorBytes.size and 0xFF).toByte()

        // visitor bytes
        visitorBytes.copyInto(header, destinationOffset = 2)

        // Int64BE timestamp
        val tsOffset = 2 + visitorBytes.size
        for (i in 0 until 8) {
            header[tsOffset + i] = ((timestamp ushr ((7 - i) * 8)) and 0xFF).toByte()
        }

        // Salt bytes
        val saltOffset = tsOffset + 8
        actualSalt.copyInto(header, destinationOffset = saltOffset)

        // XOR mask payload with salt cycle
        val masked = ByteArray(header.size) { i ->
            (header[i].toInt() xor actualSalt[i % actualSalt.size].toInt()).toByte()
        }

        // Compute HMAC-SHA256 integrity tag
        val hmac = PureSha256.hmac(actualSalt, masked)

        // Final packet: [Salt (16) | Masked Data | HMAC Tag (32)]
        val finalPacket = ByteArray(actualSalt.size + masked.size + hmac.size)
        actualSalt.copyInto(finalPacket, 0)
        masked.copyInto(finalPacket, actualSalt.size)
        hmac.copyInto(finalPacket, actualSalt.size + masked.size)

        return Base64.UrlSafe.encode(finalPacket).trimEnd('=')
    }

    fun verifyPoToken(token: String): PoTokenVerificationResult {
        return try {
            var b64 = token.replace('-', '+').replace('_', '/')
            while (b64.length % 4 != 0) {
                b64 += "="
            }
            val packet = Base64.Default.decode(b64)

            // Minimum valid length: 16 (salt) + 2 (len) + 0 (visitor) + 8 (ts) + 16 (salt) + 32 (hmac) = 74
            if (packet.size < 16 + 2 + 8 + 16 + 32) {
                return PoTokenVerificationResult(valid = false, error = "Token packet too short")
            }

            val salt = packet.copyOfRange(0, 16)
            val hmacTag = packet.copyOfRange(packet.size - 32, packet.size)
            val masked = packet.copyOfRange(16, packet.size - 32)

            // Verify HMAC
            val expectedHmac = PureSha256.hmac(salt, masked)
            if (!PureSha256.timingSafeEqual(hmacTag, expectedHmac)) {
                return PoTokenVerificationResult(valid = false, error = "HMAC verification failed")
            }

            // Unmask
            val unmasked = ByteArray(masked.size) { i ->
                (masked[i].toInt() xor salt[i % salt.size].toInt()).toByte()
            }

            val visitorLen = ((unmasked[0].toInt() and 0xFF) shl 8) or (unmasked[1].toInt() and 0xFF)
            if (2 + visitorLen + 8 + 16 > unmasked.size) {
                return PoTokenVerificationResult(valid = false, error = "Corrupt payload length")
            }

            val visitorData = unmasked.decodeToString(2, 2 + visitorLen)
            val tsOffset = 2 + visitorLen
            var ts = 0L
            for (i in 0 until 8) {
                ts = (ts shl 8) or (unmasked[tsOffset + i].toLong() and 0xFF)
            }

            val nowSec = currentTimeMillis() / 1000
            val ageSec = maxOf(0L, nowSec - ts)

            PoTokenVerificationResult(
                valid = true,
                visitorData = visitorData,
                timestamp = ts,
                ageSec = ageSec
            )
        } catch (e: Exception) {
            PoTokenVerificationResult(valid = false, error = e.message ?: "Failed to verify token")
        }
    }

    fun generateColdStartToken(
        identifier: String,
        clientState: String = "",
    ): String = generatePoToken(identifier)

    fun generateSessionToken(
        identifier: String,
    ): String = generatePoToken(identifier)

    fun generateContentToken(
        identifier: String,
        videoId: String,
    ): String = generatePoToken(identifier)
}

object PureSha256 {
    private val K = intArrayOf(
        0x428a2f98, 0x71374491, 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
        0x3956c25b, 0x59f111f1, 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
        0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
        0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
        0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(), 0x92722c85.toInt(),
        0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(),
        0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814.toInt(), 0x8cc70208.toInt(),
        0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt()
    )

    fun digest(data: ByteArray): ByteArray {
        var h0 = 0x6a09e667
        var h1 = 0xbb67ae85.toInt()
        var h2 = 0x3c6ef372
        var h3 = 0xa54ff53a.toInt()
        var h4 = 0x510e527f
        var h5 = 0x9b05688c.toInt()
        var h6 = 0x1f83d9ab
        var h7 = 0x5be0cd19

        val bitLength = data.size.toLong() * 8L
        val paddingLength = ((56 - (data.size + 1) % 64) % 64 + 64) % 64
        val totalLength = data.size + 1 + paddingLength + 8
        val padded = ByteArray(totalLength)
        data.copyInto(padded)
        padded[data.size] = 0x80.toByte()

        for (i in 0 until 8) {
            padded[totalLength - 1 - i] = ((bitLength ushr (i * 8)) and 0xFF).toByte()
        }

        val w = IntArray(64)
        for (chunk in padded.indices step 64) {
            for (i in 0 until 16) {
                val j = chunk + i * 4
                w[i] = ((padded[j].toInt() and 0xFF) shl 24) or
                        ((padded[j + 1].toInt() and 0xFF) shl 16) or
                        ((padded[j + 2].toInt() and 0xFF) shl 8) or
                        (padded[j + 3].toInt() and 0xFF)
            }
            for (i in 16 until 64) {
                val s0 = (w[i - 15] ushr 7 or (w[i - 15] shl 25)) xor
                        (w[i - 15] ushr 18 or (w[i - 15] shl 14)) xor
                        (w[i - 15] ushr 3)
                val s1 = (w[i - 2] ushr 17 or (w[i - 2] shl 15)) xor
                        (w[i - 2] ushr 19 or (w[i - 2] shl 13)) xor
                        (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4
            var f = h5
            var g = h6
            var h = h7

            for (i in 0 until 64) {
                val s1 = (e ushr 6 or (e shl 26)) xor (e ushr 11 or (e shl 21)) xor (e ushr 25 or (e shl 7))
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = h + s1 + ch + K[i] + w[i]
                val s0 = (a ushr 2 or (a shl 30)) xor (a ushr 13 or (a shl 19)) xor (a ushr 22 or (a shl 10))
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj

                h = g
                g = f
                f = e
                e = d + temp1
                d = c
                c = b
                b = a
                a = temp1 + temp2
            }

            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
            h5 += f
            h6 += g
            h7 += h
        }

        val result = ByteArray(32)
        val hashWords = intArrayOf(h0, h1, h2, h3, h4, h5, h6, h7)
        for (i in 0 until 8) {
            val word = hashWords[i]
            result[i * 4] = ((word ushr 24) and 0xFF).toByte()
            result[i * 4 + 1] = ((word ushr 16) and 0xFF).toByte()
            result[i * 4 + 2] = ((word ushr 8) and 0xFF).toByte()
            result[i * 4 + 3] = (word and 0xFF).toByte()
        }
        return result
    }

    fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val blockSize = 64
        val paddedKey = if (key.size > blockSize) {
            digest(key)
        } else {
            key
        }
        val keyBlock = ByteArray(blockSize)
        paddedKey.copyInto(keyBlock)

        val oPad = ByteArray(blockSize) { i -> (keyBlock[i].toInt() xor 0x5c).toByte() }
        val iPad = ByteArray(blockSize) { i -> (keyBlock[i].toInt() xor 0x36).toByte() }

        val inner = digest(iPad + data)
        return digest(oPad + inner)
    }

    fun timingSafeEqual(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}
