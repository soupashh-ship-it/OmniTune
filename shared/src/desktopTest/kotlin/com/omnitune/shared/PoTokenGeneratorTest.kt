package com.omnitune.shared

import com.omnitune.shared.data.innertube.PoTokenGenerator
import com.omnitune.shared.data.network.currentTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PoTokenGeneratorTest {

    @Test
    fun testGeneratePoTokenFormat() {
        val token = PoTokenGenerator.generatePoToken()
        assertTrue(token.length > 30, "Token should be at least 30 chars, was ${token.length}")
        assertFalse(token.contains('+'), "URL-safe Base64 must not contain '+'")
        assertFalse(token.contains('/'), "URL-safe Base64 must not contain '/'")
        assertFalse(token.contains('='), "URL-safe Base64 must not contain padding '='")
    }

    @Test
    fun testEncodesVisitorDataAndTimestamp() {
        val visitor = "TestVisitor123"
        val ts = currentTimeMillis() / 1000
        val token = PoTokenGenerator.generatePoToken(visitorData = visitor, timestamp = ts)
        val verified = PoTokenGenerator.verifyPoToken(token)

        assertTrue(verified.valid, "Token should be valid")
        assertEquals(visitor, verified.visitorData)
        assertEquals(ts, verified.timestamp)
    }

    @Test
    fun testTamperingDetection() {
        val token = PoTokenGenerator.generatePoToken()
        val corruptToken = token.substring(0, token.length - 4) + "AAAA"
        val verified = PoTokenGenerator.verifyPoToken(corruptToken)
        assertFalse(verified.valid, "Tampered token must fail HMAC validation")
    }

    @Test
    fun testAgeCalculation() {
        val nowSec = currentTimeMillis() / 1000
        val pastTs = nowSec - 120
        val token = PoTokenGenerator.generatePoToken(visitorData = "AgeTest", timestamp = pastTs)
        val verified = PoTokenGenerator.verifyPoToken(token)

        assertTrue(verified.valid)
        val age = verified.ageSec
        assertNotNull(age)
        assertTrue(age in 118..130, "Age should be around 120s, was $age")
    }

    @Test
    fun testEmptyVisitorData() {
        val token = PoTokenGenerator.generatePoToken(visitorData = "")
        val verified = PoTokenGenerator.verifyPoToken(token)
        assertTrue(verified.valid)
        assertEquals("", verified.visitorData)
    }

    @Test
    fun testLongVisitorData() {
        val longVisitor = "V".repeat(512)
        val token = PoTokenGenerator.generatePoToken(visitorData = longVisitor)
        val verified = PoTokenGenerator.verifyPoToken(token)
        assertTrue(verified.valid)
        assertEquals(512, verified.visitorData?.length)
    }

    @Test
    fun testInvalidInputGracefulFailure() {
        val truncated = PoTokenGenerator.verifyPoToken("dG9vLXNob3J0")
        assertFalse(truncated.valid)

        val nonBase64 = PoTokenGenerator.verifyPoToken("!!!NOT_BASE64@@@")
        assertFalse(nonBase64.valid)
    }
}
