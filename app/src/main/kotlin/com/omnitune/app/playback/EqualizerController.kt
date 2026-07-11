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
    private var enabled = false
    private var pendingBands: List<EqualizerBand> = emptyList()

    fun setupIfNeeded(audioSessionId: Int) {
        if (systemEqualizer != null) return

        try {
            systemEqualizer?.release()
            systemEqualizer = Equalizer(0, audioSessionId).apply {
                enabled = this@EqualizerController.enabled
            }
            applyBands(pendingBands)
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
        pendingBands = bands
        val eq = systemEqualizer ?: return
        try {
            val range = eq.bandLevelRange
            bands.forEachIndexed { index, band ->
                if (index < eq.numberOfBands) {
                    val gainMillibels = (band.gainDb * 100).toInt()
                        .coerceIn(range[0].toInt(), range[1].toInt())
                        .toShort()
                    eq.setBandLevel(index.toShort(), gainMillibels)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to apply EQ bands")
        }
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        runCatching { systemEqualizer?.enabled = enabled }
            .onFailure { Timber.w(it, "Failed to change equalizer state") }
    }

    fun release() {
        systemEqualizer?.release()
        systemEqualizer = null
    }
}
