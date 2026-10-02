package com.omnitune.shared.ui.theme

/**
 * Authoritative Theme Specification Engine for SuvMusic presentation layer.
 * Implements token resolution, pure black AMOLED adaptation, and waveform position mapping.
 */
object SuvMusicThemeEngine {

    const val SQUIRCLE_CORNER_SMOOTHING: Double = 0.6

    val TYPOGRAPHY_WEIGHTS: Map<String, Int> = mapOf(
        "Regular" to 400,
        "Medium" to 500,
        "SemiBold" to 600,
        "Bold" to 700,
        "ExtraBold" to 800
    )

    data class ResolvedTheme(
        val paletteName: String,
        val primary: String,
        val secondary: String,
        val tertiary: String,
        val surface: String,
        val background: String,
        val isPureBlack: Boolean
    ) {
        val primaryHex: String get() = primary
        val secondaryHex: String get() = secondary
        val tertiaryHex: String get() = tertiary
        val surfaceHex: String get() = surface
        val backgroundHex: String get() = background
    }

    val PALETTES: Map<String, ResolvedTheme> = mapOf(
        "DEFAULT" to ResolvedTheme(
            paletteName = "DEFAULT",
            primary = "#9C27B0",
            secondary = "#00BCD4",
            tertiary = "#E91E63",
            surface = "#121212",
            background = "#0A0A0A",
            isPureBlack = false
        ),
        "OCEAN" to ResolvedTheme(
            paletteName = "OCEAN",
            primary = "#1976D2",
            secondary = "#009688",
            tertiary = "#7B1FA2",
            surface = "#101720",
            background = "#080E18",
            isPureBlack = false
        ),
        "SUNSET" to ResolvedTheme(
            paletteName = "SUNSET",
            primary = "#FF5722",
            secondary = "#FFC107",
            tertiary = "#E91E63",
            surface = "#1C1310",
            background = "#120A08",
            isPureBlack = false
        ),
        "NATURE" to ResolvedTheme(
            paletteName = "NATURE",
            primary = "#388E3C",
            secondary = "#8BC34A",
            tertiary = "#00796B",
            surface = "#101812",
            background = "#08100A",
            isPureBlack = false
        ),
        "LOVE" to ResolvedTheme(
            paletteName = "LOVE",
            primary = "#E91E63",
            secondary = "#F48FB1",
            tertiary = "#FF5722",
            surface = "#1A0F14",
            background = "#12080D",
            isPureBlack = false
        )
    )

    val WAVEFORM_STYLES: List<String> = listOf(
        "Bars", "Mirror", "Rounded", "Gradient", "Smooth", "Stepped", "Dots", "Wave", "Minimal"
    )

    fun applyTheme(paletteName: String = "DEFAULT", pureBlack: Boolean = false): ResolvedTheme {
        val base = PALETTES[paletteName.uppercase()] ?: PALETTES["DEFAULT"]!!
        return if (pureBlack) {
            base.copy(
                background = "#000000",
                surface = "#000000",
                isPureBlack = true
            )
        } else {
            base
        }
    }

    fun calculateWaveformPosition(fraction: Double, durationMs: Long): Long {
        if (durationMs <= 0L || fraction.isNaN()) return 0L
        val clamped = fraction.coerceIn(0.0, 1.0)
        return (clamped * durationMs).toLong()
    }

    fun calculateWaveformPosition(fraction: Float, durationMs: Long): Long {
        return calculateWaveformPosition(fraction.toDouble(), durationMs)
    }

    fun resolveWaveformStyle(name: String): String {
        return if (WAVEFORM_STYLES.any { it.equals(name, ignoreCase = true) }) {
            WAVEFORM_STYLES.first { it.equals(name, ignoreCase = true) }
        } else {
            "Bars"
        }
    }
}
