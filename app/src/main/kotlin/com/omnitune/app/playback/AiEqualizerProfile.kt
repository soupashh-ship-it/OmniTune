/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import java.util.Locale

private const val MAX_AI_EQ_GAIN_DB = 12f

fun createAiEqualizerBands(
    prompt: String,
    trackHint: String? = null,
): List<EqualizerBand> {
    val text = listOf(prompt, trackHint.orEmpty())
        .joinToString(" ")
        .lowercase(Locale.US)
        .trim()

    if (text.isBlank()) return EqualizerPresets.FLAT.bands

    val gains = MutableList(EqualizerPresets.FREQUENCIES.size) { 0f }

    fun hasAny(vararg keywords: String): Boolean =
        keywords.any { keyword -> text.contains(keyword) }

    fun add(indices: IntRange, amount: Float) {
        indices.forEach { index ->
            if (index in gains.indices) {
                gains[index] = (gains[index] + amount).coerceIn(-MAX_AI_EQ_GAIN_DB, MAX_AI_EQ_GAIN_DB)
            }
        }
    }

    if (hasAny("flat", "neutral", "transparent", "reference")) {
        add(0..9, 0f)
    }
    if (hasAny("bass", "sub", "808", "deep", "club", "rumble")) {
        add(0..2, 4.5f)
        add(3..3, 2f)
    }
    if (hasAny("punch", "kick", "impact", "drive")) {
        add(1..3, 2.5f)
        add(4..5, -1f)
    }
    if (hasAny("warm", "vintage", "tube", "analog", "mellow")) {
        add(1..4, 2f)
        add(8..9, -1.5f)
    }
    if (hasAny("bright", "air", "sparkle", "shimmer", "treble", "crisp")) {
        add(6..9, 3f)
        add(0..1, -1f)
    }
    if (hasAny("vocal", "voice", "lyrics", "podcast", "speech", "clear")) {
        add(3..6, 3f)
        add(0..1, -2f)
        add(8..9, -1f)
    }
    if (hasAny("rock", "guitar", "metal", "live")) {
        add(0..2, 2.5f)
        add(4..5, -1.5f)
        add(6..9, 2.5f)
    }
    if (hasAny("pop", "dance", "edm", "electronic", "party")) {
        add(0..2, 3f)
        add(3..4, 1.5f)
        add(7..9, 2f)
    }
    if (hasAny("jazz", "classical", "acoustic", "orchestra", "piano")) {
        add(0..2, 1.5f)
        add(3..6, 1f)
        add(7..9, 1.5f)
    }
    if (hasAny("lofi", "lo-fi", "soft", "sleep", "relax", "smooth")) {
        add(0..3, 1f)
        add(6..9, -2f)
    }
    if (hasAny("thin", "tinny", "harsh", "sibilant")) {
        add(0..2, 2f)
        add(7..9, -3f)
    }

    return EqualizerPresets.FREQUENCIES.mapIndexed { index, frequency ->
        EqualizerBand(
            centerFrequencyHz = frequency,
            gainDb = gains[index].coerceIn(-MAX_AI_EQ_GAIN_DB, MAX_AI_EQ_GAIN_DB),
        )
    }
}

fun describeAiEqualizerProfile(prompt: String): String {
    val text = prompt.lowercase(Locale.US)
    return when {
        listOf("bass", "sub", "808", "club").any(text::contains) -> "Bass-forward"
        listOf("vocal", "voice", "lyrics", "speech").any(text::contains) -> "Vocal clarity"
        listOf("bright", "air", "sparkle", "treble").any(text::contains) -> "Bright detail"
        listOf("warm", "vintage", "tube", "analog").any(text::contains) -> "Warm analog"
        listOf("rock", "guitar", "metal").any(text::contains) -> "Rock contour"
        listOf("jazz", "classical", "acoustic").any(text::contains) -> "Natural acoustic"
        listOf("lofi", "lo-fi", "soft", "sleep").any(text::contains) -> "Soft focus"
        else -> "Balanced custom"
    }
}
