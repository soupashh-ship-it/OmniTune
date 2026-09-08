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
 * Donor-style 10-band EQ. Android devices expose different physical EQ band
 * counts, so EqualizerController applies as many of these levels as supported.
 */
object EqualizerPresets {
    val FREQUENCIES = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

    private fun preset(name: String, gains: List<Float>) =
        EqualizerPreset(
            name = name,
            bands = FREQUENCIES.mapIndexed { index, frequency ->
                EqualizerBand(frequency, gains.getOrElse(index) { 0f })
            },
        )

    val FLAT = EqualizerPreset("Flat", listOf(
        EqualizerBand(31, 0f), EqualizerBand(62, 0f), EqualizerBand(125, 0f),
        EqualizerBand(250, 0f), EqualizerBand(500, 0f), EqualizerBand(1000, 0f),
        EqualizerBand(2000, 0f), EqualizerBand(4000, 0f), EqualizerBand(8000, 0f),
        EqualizerBand(16000, 0f)
    ))
    val BASS_BOOST = preset("Bass Boost", listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f))
    val TREBLE_BOOST = preset("Treble Boost", listOf(0f, 0f, 0f, 0f, 0f, 2f, 4f, 5f, 6f, 7f))
    val ROCK = preset("Rock", listOf(4f, 3f, 2f, -1f, -2f, -1f, 1f, 2f, 3f, 4f))
    val POP = preset("Pop", listOf(-1f, 1f, 2f, 3f, 2f, 0f, -1f, -1f, -1f, -1f))
    val JAZZ = preset("Jazz", listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f))
    val CLASSICAL = preset("Classical", listOf(4f, 3f, 2f, 1f, 0f, 0f, 1f, 2f, 3f, 4f))
    val VOCAL = preset("Vocal", listOf(-2f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f, -2f))
    val ELECTRONIC = preset("Electronic", listOf(5f, 4f, 1f, 0f, -2f, 2f, 1f, 3f, 4f, 5f))

    val all = listOf(FLAT, BASS_BOOST, TREBLE_BOOST, ROCK, POP, JAZZ, CLASSICAL, VOCAL, ELECTRONIC)
}

fun encodeEqualizerBands(bands: List<EqualizerBand>): String =
    bands.joinToString(",") { (it.gainDb * 100).toInt().toString() }

fun decodeEqualizerBands(value: String): List<EqualizerBand>? {
    val levels = value.split(',').mapNotNull(String::toIntOrNull)
    val gains = when (levels.size) {
        EqualizerPresets.FLAT.bands.size -> levels
        5 -> listOf(
            levels[0],
            levels[0],
            levels[1],
            levels[1],
            0,
            levels[2],
            levels[3],
            levels[3],
            levels[4],
            levels[4],
        )
        else -> return null
    }
    return EqualizerPresets.FLAT.bands.mapIndexed { index, band ->
        band.copy(gainDb = (gains[index] / 100f).coerceIn(-15f, 15f))
    }
}

fun List<EqualizerBand>.withPreamp(preampDb: Float): List<EqualizerBand> =
    map { band -> band.copy(gainDb = (band.gainDb + preampDb).coerceIn(-15f, 15f)) }
