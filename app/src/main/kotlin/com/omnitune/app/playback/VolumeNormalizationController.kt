/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import com.omnitune.app.db.MusicDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.pow

/**
 * Computes and exposes the per-track loudness-normalization factor applied to playback volume.
 *
 * Extracted from MusicService as part of the playback coordinator decomposition. Consumers
 * (PlaybackPreferenceObserver, CrossfadePlaybackCoordinator) read [factor]; the enable switch
 * stays owned by the service's preference plumbing and is consulted through [isEnabled].
 */
class VolumeNormalizationController(
    private val database: MusicDatabase,
    private val scope: CoroutineScope,
    private val isEnabled: () -> Boolean,
) {
    private val _factor = MutableStateFlow(1f)
    val factor: StateFlow<Float> = _factor.asStateFlow()

    fun updateFor(mediaId: String?) {
        if (mediaId.isNullOrBlank()) {
            _factor.value = 1f
            return
        }
        scope.launch {
            val factor = withContext(Dispatchers.IO) {
                if (!isEnabled()) return@withContext 1f
                val format = database.format(mediaId).first()
                val loudness = format?.loudnessDb ?: format?.perceptualLoudnessDb ?: return@withContext 1f
                var f = 10f.pow((-loudness.toFloat()) / 20f)
                if (f > 1f) f = min(f, 3.16f)
                f
            }
            _factor.value = factor
        }
    }
}
