/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import androidx.media3.common.C

data class PlayerProgressState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

internal object PlayerProgressMapper {
    fun snapshot(
        playerPositionMs: Long,
        playerDurationMs: Long,
        metadataDurationSeconds: Int?,
    ): PlayerProgressState {
        val positionMs = playerPositionMs.coerceAtLeast(0L)
        val durationMs = playerDurationMs
            .takeIf { it != C.TIME_UNSET && it > 0L }
            ?: metadataDurationSeconds
                ?.takeIf { it > 0 }
                ?.toLong()
                ?.times(1000L)
            ?: 0L

        return PlayerProgressState(
            positionMs = positionMs.coerceAtMost(durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE),
            durationMs = durationMs,
        )
    }
}
