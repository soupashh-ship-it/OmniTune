/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.audio.ToFloatPcmAudioProcessor

class OmnituneRenderersFactory(context: Context) : DefaultRenderersFactory(context) {

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParams: Boolean,
    ): AudioSink {
        val silenceSkippingProcessor = SilenceSkippingAudioProcessor(
            SILENCE_MIN_DURATION_US,
            SILENCE_RETENTION_RATIO,
            SILENCE_MAX_KEEP_US,
            SILENCE_MIN_VOLUME_PERCENT,
            SILENCE_THRESHOLD_LEVEL,
        )

        val chain = DefaultAudioProcessorChain(
            arrayOf(ToFloatPcmAudioProcessor()),
            silenceSkippingProcessor,
            SonicAudioProcessor(),
        )

        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
            .setAudioProcessorChain(chain)
            .build()
    }

    companion object {
        private const val SILENCE_MIN_DURATION_US = 100_000L
        private const val SILENCE_RETENTION_RATIO = 0.2f
        private const val SILENCE_MAX_KEEP_US = 2_000_000L
        private const val SILENCE_MIN_VOLUME_PERCENT = 10
        private const val SILENCE_THRESHOLD_LEVEL: Short = 384
    }
}
