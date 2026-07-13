/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

/**
 * Represents a single EQ band.
 * [centerFrequencyHz] is the band center in Hz.
 * [gainDb] is the gain in decibels (-15.0 to +15.0).
 */
data class EqualizerBand(
    val centerFrequencyHz: Int,
    val gainDb: Float = 0f,
)

/**
 * A named EQ preset.
 */
data class EqualizerPreset(
    val name: String,
    val bands: List<EqualizerBand>,
)

/**
 * Built-in EQ presets.
 * Standard 5-band EQ: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
 */
object EqualizerPresets {
    val FLAT = EqualizerPreset("Flat", listOf(
        EqualizerBand(60, 0f), EqualizerBand(230, 0f),
        EqualizerBand(910, 0f), EqualizerBand(3600, 0f), EqualizerBand(14000, 0f)
    ))
    val BASS_BOOST = EqualizerPreset("Bass Boost", listOf(
        EqualizerBand(60, 6f), EqualizerBand(230, 4f),
        EqualizerBand(910, 0f), EqualizerBand(3600, -2f), EqualizerBand(14000, -2f)
    ))
    val TREBLE_BOOST = EqualizerPreset("Treble Boost", listOf(
        EqualizerBand(60, -2f), EqualizerBand(230, -2f),
        EqualizerBand(910, 0f), EqualizerBand(3600, 4f), EqualizerBand(14000, 6f)
    ))
    val VOCAL = EqualizerPreset("Vocal", listOf(
        EqualizerBand(60, -2f), EqualizerBand(230, 0f),
        EqualizerBand(910, 4f), EqualizerBand(3600, 4f), EqualizerBand(14000, 2f)
    ))
    val ELECTRONIC = EqualizerPreset("Electronic", listOf(
        EqualizerBand(60, 4f), EqualizerBand(230, 2f),
        EqualizerBand(910, -2f), EqualizerBand(3600, 2f), EqualizerBand(14000, 4f)
    ))

    val all = listOf(FLAT, BASS_BOOST, TREBLE_BOOST, VOCAL, ELECTRONIC)
}

fun encodeEqualizerBands(bands: List<EqualizerBand>): String =
    bands.joinToString(",") { (it.gainDb * 100).toInt().toString() }

fun decodeEqualizerBands(value: String): List<EqualizerBand>? {
    val levels = value.split(',').mapNotNull(String::toIntOrNull)
    if (levels.size != EqualizerPresets.FLAT.bands.size) return null
    return EqualizerPresets.FLAT.bands.mapIndexed { index, band ->
        band.copy(gainDb = (levels[index] / 100f).coerceIn(-15f, 15f))
    }
}
