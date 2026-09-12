package com.omnitune.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVerificationPolicyTest {
    private val validHash = "a".repeat(64)

    @Test
    fun `missing digest produces no trusted hash`() {
        assertNull(UpdateVerificationPolicy.sha256FromDigest(null))
        assertNull(UpdateVerificationPolicy.sha256FromDigest(""))
        assertNull(UpdateVerificationPolicy.sha256FromDigest("md5:$validHash"))
    }

    @Test
    fun `sha256 digest is normalized but still validated separately`() {
        assertEquals(validHash, UpdateVerificationPolicy.sha256FromDigest("SHA256:${"A".repeat(64)}"))
        assertFalse(UpdateVerificationPolicy.isValidSha256("a".repeat(63)))
        assertFalse(UpdateVerificationPolicy.isValidSha256("g".repeat(64)))
        assertTrue(UpdateVerificationPolicy.isValidSha256(validHash))
    }

    @Test
    fun `checksum file accepts first valid token only`() {
        assertEquals(validHash, UpdateVerificationPolicy.sha256FromChecksumFile("$validHash  OmniTune.apk"))
        assertEquals(validHash, UpdateVerificationPolicy.sha256FromChecksumFile("${"A".repeat(64)}\n"))
        assertNull(UpdateVerificationPolicy.sha256FromChecksumFile("not-a-hash OmniTune.apk"))
    }

    @Test
    fun `package identity and version checks fail closed`() {
        assertTrue(UpdateVerificationPolicy.isExpectedPackage("com.omnitune.app", "com.omnitune.app"))
        assertFalse(UpdateVerificationPolicy.isExpectedPackage("com.other.app", "com.omnitune.app"))
        assertFalse(UpdateVerificationPolicy.isExpectedPackage(null, "com.omnitune.app"))
        assertTrue(UpdateVerificationPolicy.isNewerVersion(101L, 100L))
        assertFalse(UpdateVerificationPolicy.isNewerVersion(100L, 100L))
        assertFalse(UpdateVerificationPolicy.isNewerVersion(99L, 100L))
    }
}
