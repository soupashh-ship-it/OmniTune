/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import androidx.datastore.preferences.core.Preferences
import com.omnitune.app.constants.EqualizerBandLevelsMbKey
import com.omnitune.app.constants.EqualizerBassBoostEnabledKey
import com.omnitune.app.constants.EqualizerBassBoostStrengthKey
import com.omnitune.app.constants.EqualizerEnabledKey
import com.omnitune.app.constants.EqualizerVirtualizerEnabledKey
import com.omnitune.app.constants.EqualizerVirtualizerStrengthKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Reactively applies equalizer, bass-boost and virtualizer preferences to their controllers.
 *
 * Extracted from MusicService as part of the playback coordinator decomposition (see
 * docs/architecture/music-service-decomposition-plan.md). The controllers stay lazily owned by
 * the service because they need a Context; this class only wires preference flows to them.
 */
class EqualizerEffectObserver(
    private val preferences: Flow<Preferences>,
    private val equalizerController: EqualizerController,
    private val audioEffectController: AudioEffectController,
    private val scope: CoroutineScope,
) {
    private var jobs = mutableListOf<Job>()

    fun start() {
        stop()

        jobs += scope.launch {
            combine(
                preferences.map { it[EqualizerEnabledKey] ?: false }.distinctUntilChanged(),
                preferences.map { it[EqualizerBandLevelsMbKey].orEmpty() }.distinctUntilChanged(),
            ) { enabled, levels -> enabled to levels }
                .collect { (enabled, levels) ->
                    equalizerController.setEnabled(enabled)
                    decodeEqualizerBands(levels)?.let(equalizerController::applyBands)
                }
        }

        jobs += scope.launch {
            combine(
                preferences.map { it[EqualizerBassBoostEnabledKey] ?: false }.distinctUntilChanged(),
                preferences.map { it[EqualizerBassBoostStrengthKey] ?: 500 }.distinctUntilChanged(),
                preferences.map { it[EqualizerVirtualizerEnabledKey] ?: false }.distinctUntilChanged(),
                preferences.map { it[EqualizerVirtualizerStrengthKey] ?: 500 }.distinctUntilChanged(),
            ) { bbEnabled, bbStrength, virtEnabled, virtStrength ->
                audioEffectController.setBassBoostEnabled(bbEnabled)
                audioEffectController.setBassBoostStrength(bbStrength)
                audioEffectController.setVirtualizerEnabled(virtEnabled)
                audioEffectController.setVirtualizerStrength(virtStrength)
            }.collect { }
        }
    }

    fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }
}
