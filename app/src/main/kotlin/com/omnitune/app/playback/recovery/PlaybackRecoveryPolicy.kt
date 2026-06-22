package com.omnitune.app.playback.recovery

import java.util.concurrent.ConcurrentHashMap

class PlaybackRecoveryPolicy(private val maxRetries: Int = 2) {
    private val retryCounts = ConcurrentHashMap<String, Int>()

    fun canRetry(mediaId: String, errorType: PlaybackErrorType): Boolean {
        // Some errors might be definitively unrecoverable, but usually YouTube stream URLs 
        // expiring just need a fresh fetch. We allow retries for all unless explicitly blocked.
        val currentRetries = retryCounts.getOrDefault(mediaId, 0)
        return currentRetries < maxRetries
    }

    fun incrementRetry(mediaId: String) {
        val current = retryCounts.getOrDefault(mediaId, 0)
        retryCounts[mediaId] = current + 1
    }

    fun resetRetry(mediaId: String) {
        retryCounts.remove(mediaId)
    }

    fun clear() {
        retryCounts.clear()
    }
}
