/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import android.media.audiofx.Equalizer
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import timber.log.Timber

class EqualizerController(
    context: Context,
) {
    private val appContext = context.applicationContext
    private var systemEqualizer: Equalizer? = null

    fun setupIfNeeded(audioSessionId: Int) {
        if (systemEqualizer != null) return

        try {
            systemEqualizer?.release()
            systemEqualizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to initialize equalizer")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    appContext,
                    "System Equalizer is unavailable on this device.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun applyBands(bands: List<EqualizerBand>) {
        val eq = systemEqualizer ?: return
        try {
            bands.forEachIndexed { index, band ->
                if (index < eq.numberOfBands) {
                    val gainMillibels = (band.gainDb * 100).toInt().toShort()
                    eq.setBandLevel(index.toShort(), gainMillibels)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to apply EQ bands")
        }
    }

    fun release() {
        systemEqualizer?.release()
        systemEqualizer = null
    }
}
