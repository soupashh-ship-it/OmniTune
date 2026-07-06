/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 *
 * Custom font family — Outfit, downloaded via Google Fonts provider.
 * A modern geometric sans-serif with distinctive character.
 * Falls back gracefully to system default if Play Services is unavailable.
 */

package com.omnitune.app.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.omnitune.app.R

/**
 * Google Fonts provider configuration.
 * Uses the standard Google Play Services font provider.
 */
private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/**
 * The Outfit font family with multiple weights.
 *
 * A clean, modern geometric sans-serif with more personality than Inter.
 * Loaded as downloadable font via Google Fonts provider.
 * Weights: Regular (400), Medium (500), SemiBold (600), Bold (700), ExtraBold (800).
 *
 * The Google Fonts API is entirely static — no [@Composable] context needed.
 * If Google Play Services is unavailable, falls back to [FontFamily.Default].
 */
val AppFontFamily: FontFamily = try {
    FontFamily(
        Font(googleFont = GoogleFont("Outfit"), fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
        Font(googleFont = GoogleFont("Outfit"), fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
        Font(googleFont = GoogleFont("Outfit"), fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = GoogleFont("Outfit"), fontProvider = GoogleFontProvider, weight = FontWeight.Bold),
        Font(googleFont = GoogleFont("Outfit"), fontProvider = GoogleFontProvider, weight = FontWeight.ExtraBold),
    )
} catch (_: Exception) {
    // Google Play Services not available — use system font
    FontFamily.Default
}

/** Backward-compatible alias for existing references. */
val InterFontFamily = AppFontFamily
