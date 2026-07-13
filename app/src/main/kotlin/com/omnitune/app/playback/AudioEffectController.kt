/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import timber.log.Timber

class AudioEffectController(
    context: Context,
) {
    private val appContext = context.applicationContext
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    fun setupIfNeeded(audioSessionId: Int) {
        if (bassBoost != null && virtualizer != null) return

        try {
            bassBoost?.release()
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = false
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to initialize BassBoost")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    appContext,
                    "BassBoost is unavailable on this device.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        try {
            virtualizer?.release()
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = false
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to initialize Virtualizer")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    appContext,
                    "Virtualizer is unavailable on this device.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        val bb = bassBoost ?: return
        try {
            bb.enabled = enabled
            Timber.d("BassBoost enabled: $enabled")
        } catch (e: Exception) {
            Timber.w(e, "Failed to set BassBoost enabled")
        }
    }

    fun setBassBoostStrength(strength: Int) {
        val bb = bassBoost ?: return
        try {
            val clamped = strength.coerceIn(0, 1000).toShort()
            bb.setStrength(clamped)
            Timber.d("BassBoost strength: $clamped")
        } catch (e: Exception) {
            Timber.w(e, "Failed to set BassBoost strength")
        }
    }

    fun setVirtualizerEnabled(enabled: Boolean) {
        val v = virtualizer ?: return
        try {
            v.enabled = enabled
            Timber.d("Virtualizer enabled: $enabled")
        } catch (e: Exception) {
            Timber.w(e, "Failed to set Virtualizer enabled")
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        val v = virtualizer ?: return
        try {
            val clamped = strength.coerceIn(0, 1000).toShort()
            v.setStrength(clamped)
            Timber.d("Virtualizer strength: $clamped")
        } catch (e: Exception) {
            Timber.w(e, "Failed to set Virtualizer strength")
        }
    }

    fun release() {
        bassBoost?.release()
        bassBoost = null
        virtualizer?.release()
        virtualizer = null
    }
}
