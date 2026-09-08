/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.models

import androidx.annotation.DrawableRes
import com.omnitune.app.R

enum class LogoVariant(
    val displayName: String,
    val conceptKey: String,
    val conceptLabel: String,
    val styleLabel: String,
    val description: String
) {
    DEFAULT("Default", "pulse", "Pulse", "Hero", "Primary brand logo"),
    PULSE("Pulse Hero", "pulse", "Pulse", "Hero", "Primary brand mark"),
    PULSE_APP_ICON("Pulse App Icon", "pulse", "Pulse", "App Icon", "App icon gradient style"),
    PULSE_MONO("Pulse Monochrome", "pulse", "Pulse", "Monochrome", "Clean black and white"),
    PULSE_LIGHT("Pulse Light", "pulse", "Pulse", "On Light", "Optimized for light surfaces"),
    PULSE_TONE("Pulse Single Tone", "pulse", "Pulse", "Single Tone", "Flat single accent tone"),
    RESONANCE("Resonance Hero", "resonance", "Resonance", "Hero", "Soundwave concentric style"),
    RESONANCE_APP_ICON("Resonance App Icon", "resonance", "Resonance", "App Icon", "App icon wave style"),
    RESONANCE_MONO("Resonance Monochrome", "resonance", "Resonance", "Monochrome", "Clean monochrome wave"),
    RESONANCE_LIGHT("Resonance Light", "resonance", "Resonance", "On Light", "Light surface wave"),
    RESONANCE_TONE("Resonance Single Tone", "resonance", "Resonance", "Single Tone", "Single tone wave"),
    AETHER("Aether Hero", "aether", "Aether", "Hero", "Atmospheric spatial style"),
    AETHER_APP_ICON("Aether App Icon", "aether", "Aether", "App Icon", "App icon spatial style"),
    AETHER_MONO("Aether Monochrome", "aether", "Aether", "Monochrome", "Clean monochrome aether"),
    AETHER_LIGHT("Aether Light", "aether", "Aether", "On Light", "Light surface aether"),
    AETHER_TONE("Aether Single Tone", "aether", "Aether", "Single Tone", "Single tone aether"),
    CLASSIC("Classic", "classic", "Classic", "Classic", "Original brand mark");

    @DrawableRes
    fun drawableRes(): Int = when (this) {
        DEFAULT, PULSE -> R.drawable.logo_pulse
        PULSE_APP_ICON -> R.drawable.logo_pulse_app_icon
        PULSE_MONO -> R.drawable.logo_pulse_mono
        PULSE_LIGHT -> R.drawable.logo_pulse_light
        PULSE_TONE -> R.drawable.logo_pulse_tone
        RESONANCE -> R.drawable.logo_resonance
        RESONANCE_APP_ICON -> R.drawable.logo_resonance_app_icon
        RESONANCE_MONO -> R.drawable.logo_resonance_mono
        RESONANCE_LIGHT -> R.drawable.logo_resonance_light
        RESONANCE_TONE -> R.drawable.logo_resonance_tone
        AETHER -> R.drawable.logo_aether
        AETHER_APP_ICON -> R.drawable.logo_aether_app_icon
        AETHER_MONO -> R.drawable.logo_aether_mono
        AETHER_LIGHT -> R.drawable.logo_aether_light
        AETHER_TONE -> R.drawable.logo_aether_tone
        CLASSIC -> R.drawable.logo
    }
}
