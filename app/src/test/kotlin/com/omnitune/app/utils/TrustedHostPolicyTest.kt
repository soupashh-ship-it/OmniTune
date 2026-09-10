package com.omnitune.app.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedHostPolicyTest {
    @Test
    fun exactOrSubdomainAcceptsOnlyDomainBoundaries() {
        assertTrue(TrustedHostPolicy.isExactHostOrSubdomain("googlevideo.com", "googlevideo.com"))
        assertTrue(TrustedHostPolicy.isExactHostOrSubdomain("rr1---sn.googlevideo.com", "googlevideo.com"))
        assertTrue(TrustedHostPolicy.isExactHostOrSubdomain("music.youtube.com.", "youtube.com"))

        assertFalse(TrustedHostPolicy.isExactHostOrSubdomain("evilgooglevideo.com", "googlevideo.com"))
        assertFalse(TrustedHostPolicy.isExactHostOrSubdomain("googlevideo.com.example.com", "googlevideo.com"))
        assertFalse(TrustedHostPolicy.isExactHostOrSubdomain("youtube.com.example.com", "youtube.com"))
    }

    @Test
    fun exactHostDoesNotAcceptSubdomains() {
        assertTrue(TrustedHostPolicy.isExactHost("youtu.be.", "youtu.be"))
        assertFalse(TrustedHostPolicy.isExactHost("sub.youtu.be", "youtu.be"))
    }
}
