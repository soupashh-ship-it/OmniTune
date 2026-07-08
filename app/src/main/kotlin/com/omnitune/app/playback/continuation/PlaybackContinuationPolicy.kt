/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback.continuation

object AutoplayRetryPolicy {
    const val MAX_STREAM_RESOLUTION_ATTEMPTS = 3
}

object PlaybackContinuationPolicy {
    fun shouldRunAutoplay(
        autoplayEnabled: Boolean,
        playbackContext: PlaybackContext,
        hasNextItem: Boolean,
    ): Boolean =
        autoplayEnabled && playbackContext.allowAutoplay && !hasNextItem
}

object TasteSignalClassifier {
    const val POSITIVE_LISTEN_MS = 60_000L
    const val QUICK_SKIP_MS = 15_000L
    private const val POSITIVE_DURATION_RATIO = 0.4f

    fun isPositiveListen(
        listenedMillis: Long,
        durationMillis: Long?,
    ): Boolean =
        listenedMillis >= POSITIVE_LISTEN_MS ||
            (durationMillis != null && listenedMillis >= (durationMillis * POSITIVE_DURATION_RATIO).toLong())

    fun isQuickSkip(
        listenedMillis: Long,
        completed: Boolean,
    ): Boolean =
        !completed && listenedMillis in 1 until QUICK_SKIP_MS
}

