package com.omnitune.app.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginWebViewPolicyTest {
    @Test
    fun `navigation allows only https Google and YouTube auth domains`() {
        assertTrue(LoginWebViewPolicy.isAllowedNavigation("https://accounts.google.com/ServiceLogin"))
        assertTrue(LoginWebViewPolicy.isAllowedNavigation("https://music.youtube.com/"))
        assertTrue(LoginWebViewPolicy.isAllowedNavigation("about:blank"))

        assertFalse(LoginWebViewPolicy.isAllowedNavigation("http://accounts.google.com/ServiceLogin"))
        assertFalse(LoginWebViewPolicy.isAllowedNavigation("https://youtube.com.evil.test/signin"))
        assertFalse(LoginWebViewPolicy.isAllowedNavigation("intent://accounts.google.com/ServiceLogin"))
    }

    @Test
    fun `login completion requires strict YouTube return URL and session cookie`() {
        val cookies = "SID=x; SAPISID=y; PREF=z"

        assertTrue(LoginWebViewPolicy.shouldCompleteLogin("https://music.youtube.com/", cookies))
        assertTrue(
            LoginWebViewPolicy.shouldCompleteLogin(
                "https://www.youtube.com/signin?action_handle_signin=true&next=https%3A%2F%2Fmusic.youtube.com%2F",
                cookies,
            ),
        )

        assertFalse(LoginWebViewPolicy.shouldCompleteLogin("https://accounts.google.com/ServiceLogin", cookies))
        assertFalse(LoginWebViewPolicy.shouldCompleteLogin("https://music.youtube.com.evil.test/", cookies))
        assertFalse(LoginWebViewPolicy.shouldCompleteLogin("https://music.youtube.com/", "PREF=z"))
    }
}
