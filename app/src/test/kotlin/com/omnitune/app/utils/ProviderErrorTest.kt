package com.omnitune.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class ProviderErrorTest {

    @Test
    fun `wrapped DNS failure keeps DNS and network guidance`() {
        val error = classifyProviderError(IOException("Unable to resolve host", UnknownHostException("music.youtube.com")))

        assertEquals(ProviderErrorType.NetworkUnavailable, error.type)
        assertTrue(error.message.contains("DNS"))
        assertTrue(error.message.contains("YouTube"))
    }

    @Test
    fun `wrapped TLS failure identifies filtering or interception`() {
        val error = classifyProviderError(IOException("Handshake failed", SSLHandshakeException("certificate rejected")))

        assertEquals(ProviderErrorType.NetworkUnavailable, error.type)
        assertTrue(error.message.contains("VPN"))
        assertTrue(error.message.contains("proxy"))
    }
}
