package com.omnitune.app.update

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GitHubReleaseApiTest {
    private val api = GitHubReleaseApi(OkHttpClient())

    @Test
    fun parseRelease_usesBundledParserAndMapsPublishedAsset() {
        val release = api.parseRelease(
            """
            {
              "tag_name": "v0.12.7",
              "name": "OmniTune 0.12.7",
              "body": "Playback and updater fixes",
              "published_at": "2026-07-11T17:38:56Z",
              "prerelease": false,
              "draft": false,
              "ignored_future_field": { "value": true },
              "assets": [{
                "name": "OmniTune-v0.12.7-release.apk",
                "browser_download_url": "https://example.test/omnitune.apk",
                "size": 23781615,
                "content_type": "application/vnd.android.package-archive",
                "digest": "sha256:4e943538"
              }]
            }
            """.trimIndent()
        )

        assertEquals("v0.12.7", release.tagName)
        assertFalse(release.draft)
        assertEquals(1, release.assets.size)
        assertEquals("OmniTune-v0.12.7-release.apk", release.assets.single().name)
        assertEquals(23781615L, release.assets.single().size)
        assertEquals("sha256:4e943538", release.assets.single().digest)
    }
}
