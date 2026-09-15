package com.omnitune.app.utils

import com.omnitune.innertube.models.YouTubeClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YTPlayerUtilsTest {

    @Test
    fun `direct Android VR streams skip player JavaScript and range preflight`() {
        assertFalse(YTPlayerUtils.requiresSignatureTimestamp(YouTubeClient.ANDROID_VR_NO_AUTH))
        assertFalse(
            YTPlayerUtils.requiresStreamPreflight(
                clientName = YouTubeClient.ANDROID_VR_NO_AUTH.clientName,
                hasDirectUrl = true,
            ),
        )
    }

    @Test
    fun `web and ciphered streams retain their preflight safeguards`() {
        assertTrue(YTPlayerUtils.requiresSignatureTimestamp(YouTubeClient.WEB_REMIX))
        assertTrue(
            YTPlayerUtils.requiresStreamPreflight(
                clientName = YouTubeClient.WEB_REMIX.clientName,
                hasDirectUrl = true,
            ),
        )
        assertTrue(
            YTPlayerUtils.requiresStreamPreflight(
                clientName = YouTubeClient.ANDROID_VR_NO_AUTH.clientName,
                hasDirectUrl = false,
            ),
        )
    }
}
