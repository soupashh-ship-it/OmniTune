package com.omnitune.app.playback

import androidx.media3.exoplayer.offline.Download

/**
 * Deterministic download decisions shared by the Android download manager and service. The
 * policy has no Android storage or network dependencies, so low-storage, state, and retry paths
 * can be covered without downloading live media in CI.
 */
internal object DownloadLifecyclePolicy {
    const val MinimumAvailableBytes: Long = 64L * 1024L * 1024L

    private val youTubeIdRegex = Regex("^[a-zA-Z0-9_-]{11}$")

    fun preflight(
        videoId: String,
        wifiOnly: Boolean,
        connectedToWifi: Boolean,
        availableBytes: Long,
    ): DownloadAdmission {
        return when {
            videoId.isBlank() -> DownloadAdmission.Rejected("Download failed: invalid song")
            wifiOnly && !connectedToWifi -> DownloadAdmission.Rejected("Download requires a Wi-Fi connection")
            availableBytes < MinimumAvailableBytes -> DownloadAdmission.Rejected(
                "Download failed: not enough free storage",
            )
            else -> DownloadAdmission.Accepted
        }
    }

    /** Maps a Media3 index record to the persisted availability state used by the library. */
    fun persistedState(
        downloadState: Int?,
        removed: Boolean,
        hasCompleteCache: Boolean,
    ): Int = when {
        removed || downloadState == null || downloadState == Download.STATE_REMOVING -> Unavailable
        downloadState == Download.STATE_COMPLETED && hasCompleteCache -> Completed
        downloadState == Download.STATE_COMPLETED -> Unavailable
        downloadState == Download.STATE_FAILED -> Failed
        else -> InProgress
    }

    /** Returns the next allowed resolved-stream retry attempt, or null when retrying must stop. */
    fun nextResolvedStreamRetry(
        videoId: String,
        autoRetryEnabled: Boolean,
        completedAttempts: Int,
    ): Int? {
        if (!autoRetryEnabled || !youTubeIdRegex.matches(videoId)) return null
        return (completedAttempts + 1).takeIf { it <= MaxResolvedStreamRetries }
    }

    const val Unavailable = 0
    const val InProgress = 1
    const val Completed = 2
    const val Failed = 3
    const val MaxResolvedStreamRetries = 2
}

internal sealed class DownloadAdmission {
    data object Accepted : DownloadAdmission()
    data class Rejected(val message: String) : DownloadAdmission()
}
