/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import androidx.datastore.preferences.core.Preferences
import com.omnitune.app.constants.AIEqualizerAutoModeKey
import com.omnitune.app.constants.AIEqualizerPromptKey
import com.omnitune.app.constants.EqualizerBandLevelsMbKey
import com.omnitune.app.constants.EqualizerBassBoostEnabledKey
import com.omnitune.app.constants.EqualizerBassBoostStrengthKey
import com.omnitune.app.constants.EqualizerEnabledKey
import com.omnitune.app.constants.EqualizerPreampLevelMbKey
import com.omnitune.app.constants.EqualizerVirtualizerEnabledKey
import com.omnitune.app.constants.EqualizerVirtualizerStrengthKey
import com.omnitune.app.models.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
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
    private val currentMediaMetadata: Flow<MediaMetadata?> = flowOf(null),
) {
    private var jobs = mutableListOf<Job>()

    fun start() {
        stop()

        jobs += scope.launch {
            combine(
                preferences.map {
                    EqualizerPreferenceState(
                        enabled = it[EqualizerEnabledKey] ?: false,
                        levels = it[EqualizerBandLevelsMbKey].orEmpty(),
                        preampLevelMb = it[EqualizerPreampLevelMbKey] ?: 0,
                        autoModeEnabled = it[AIEqualizerAutoModeKey] ?: false,
                        aiPrompt = it[AIEqualizerPromptKey].orEmpty(),
                    )
                }.distinctUntilChanged(),
                currentMediaMetadata.distinctUntilChanged(),
            ) { preferenceState, metadata -> preferenceState to metadata }
                .collect { (preferenceState, metadata) ->
                    val autoBands = preferenceState.aiPrompt.takeIf {
                        preferenceState.autoModeEnabled && it.isNotBlank()
                    }?.let { prompt ->
                        createAiEqualizerBands(prompt, metadata?.toEqualizerHint())
                    }
                    val shouldEnable = preferenceState.enabled || autoBands != null
                    val preampDb = preferenceState.preampLevelMb / 100f

                    equalizerController.setEnabled(shouldEnable)
                    (autoBands ?: decodeEqualizerBands(preferenceState.levels))
                        ?.withPreamp(preampDb)
                        ?.let(equalizerController::applyBands)
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

private data class EqualizerPreferenceState(
    val enabled: Boolean,
    val levels: String,
    val preampLevelMb: Int,
    val autoModeEnabled: Boolean,
    val aiPrompt: String,
)

private fun MediaMetadata.toEqualizerHint(): String =
    buildString {
        append(title)
        album?.title?.let { append(' ').append(it) }
        artists.forEach { append(' ').append(it.name) }
    }
