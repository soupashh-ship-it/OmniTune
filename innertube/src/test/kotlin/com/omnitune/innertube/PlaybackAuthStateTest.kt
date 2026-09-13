package com.omnitune.innertube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackAuthStateTest {
    @Test
    fun secureSapisidCookieCountsAsLoginCookie() {
        val authState = PlaybackAuthState(
            cookie = "SID=one; __Secure-3PAPISID=secure-value",
            dataSyncId = "Vabc123",
        )

        assertTrue(authState.hasLoginCookie)
        assertTrue(authState.hasPlaybackLoginContext)
        assertEquals("secure-value", PlaybackAuthState.sapisidCookieValue(authState.cookie))
    }

    @Test
    fun missingSapisidCookieDoesNotCountAsLoginCookie() {
        val authState = PlaybackAuthState(cookie = "SID=one; PREF=two")

        assertFalse(authState.hasLoginCookie)
        assertFalse(authState.hasPlaybackLoginContext)
    }
}
