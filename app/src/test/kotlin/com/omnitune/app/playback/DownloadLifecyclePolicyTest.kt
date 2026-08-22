package com.omnitune.app.playback

import androidx.media3.exoplayer.offline.Download
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadLifecyclePolicyTest {
    @Test
    fun `preflight protects Wi-Fi-only and low-storage downloads`() {
        assertEquals(
            DownloadAdmission.Rejected("Download requires a Wi-Fi connection"),
            DownloadLifecyclePolicy.preflight(
                videoId = "fixtureId01",
                wifiOnly = true,
                connectedToWifi = false,
                availableBytes = DownloadLifecyclePolicy.MinimumAvailableBytes,
            ),
        )
        assertEquals(
            DownloadAdmission.Rejected("Download failed: not enough free storage"),
            DownloadLifecyclePolicy.preflight(
                videoId = "fixtureId01",
                wifiOnly = false,
                connectedToWifi = true,
                availableBytes = DownloadLifecyclePolicy.MinimumAvailableBytes - 1,
            ),
        )
        assertTrue(
            DownloadLifecyclePolicy.preflight(
                videoId = "fixtureId01",
                wifiOnly = true,
                connectedToWifi = true,
                availableBytes = DownloadLifecyclePolicy.MinimumAvailableBytes,
            ) is DownloadAdmission.Accepted,
        )
    }

    @Test
    fun `persisted download availability requires complete bytes`() {
        assertEquals(
            DownloadLifecyclePolicy.Completed,
            DownloadLifecyclePolicy.persistedState(Download.STATE_COMPLETED, removed = false, hasCompleteCache = true),
        )
        assertEquals(
            DownloadLifecyclePolicy.Unavailable,
            DownloadLifecyclePolicy.persistedState(Download.STATE_COMPLETED, removed = false, hasCompleteCache = false),
        )
        assertEquals(
            DownloadLifecyclePolicy.Failed,
            DownloadLifecyclePolicy.persistedState(Download.STATE_FAILED, removed = false, hasCompleteCache = false),
        )
        assertEquals(
            DownloadLifecyclePolicy.InProgress,
            DownloadLifecyclePolicy.persistedState(Download.STATE_DOWNLOADING, removed = false, hasCompleteCache = false),
        )
        assertEquals(
            DownloadLifecyclePolicy.Unavailable,
            DownloadLifecyclePolicy.persistedState(Download.STATE_COMPLETED, removed = true, hasCompleteCache = true),
        )
    }

    @Test
    fun `resolved stream retries are bounded and only apply to video ids`() {
        assertEquals(
            1,
            DownloadLifecyclePolicy.nextResolvedStreamRetry("fixtureId01", autoRetryEnabled = true, completedAttempts = 0),
        )
        assertEquals(
            DownloadLifecyclePolicy.MaxResolvedStreamRetries,
            DownloadLifecyclePolicy.nextResolvedStreamRetry("fixtureId01", autoRetryEnabled = true, completedAttempts = 1),
        )
        assertNull(
            DownloadLifecyclePolicy.nextResolvedStreamRetry("fixtureId01", autoRetryEnabled = true, completedAttempts = 2),
        )
        assertNull(
            DownloadLifecyclePolicy.nextResolvedStreamRetry("not-a-video-id", autoRetryEnabled = true, completedAttempts = 0),
        )
        assertNull(
            DownloadLifecyclePolicy.nextResolvedStreamRetry("fixtureId01", autoRetryEnabled = false, completedAttempts = 0),
        )
    }
}
