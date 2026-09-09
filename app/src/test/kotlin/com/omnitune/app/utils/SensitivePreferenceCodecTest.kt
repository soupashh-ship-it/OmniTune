package com.omnitune.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SensitivePreferenceCodecTest {
    @Test
    fun `plaintext value is returned and marked for encrypted migration`() {
        val decoded = SensitivePreferenceCodec.decodeForRead(
            rawValue = "  plain-secret  ",
            isEncrypted = { it?.startsWith("enc:") == true },
            decryptOrPlain = { it.orEmpty() },
            encrypt = { "enc:$it" },
        )

        assertEquals("plain-secret", decoded.plainValue)
        assertEquals("enc:plain-secret", decoded.migratedStorageValue)
    }

    @Test
    fun `encrypted value is decrypted without requesting migration`() {
        val decoded = SensitivePreferenceCodec.decodeForRead(
            rawValue = "enc:secret",
            isEncrypted = { it?.startsWith("enc:") == true },
            decryptOrPlain = { it.orEmpty().removePrefix("enc:") },
            encrypt = { error("already encrypted") },
        )

        assertEquals("secret", decoded.plainValue)
        assertNull(decoded.migratedStorageValue)
    }

    @Test
    fun `blank secret stores as a removed preference`() {
        assertNull(SensitivePreferenceCodec.encodeForStorage("   ") { "enc:$it" })
    }

    @Test
    fun `storage encoding trims and encrypts nonblank values`() {
        assertEquals(
            "enc:secret",
            SensitivePreferenceCodec.encodeForStorage("  secret  ") { "enc:$it" },
        )
    }

    @Test
    fun `masked preview never exposes the full secret`() {
        assertEquals("", SensitivePreferenceCodec.maskedPreview(""))
        assertEquals("****", SensitivePreferenceCodec.maskedPreview("abcd"))
        assertEquals("****7890", SensitivePreferenceCodec.maskedPreview("secret-1234567890"))
    }
}
