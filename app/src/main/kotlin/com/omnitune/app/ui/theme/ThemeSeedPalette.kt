package com.omnitune.app.ui.theme

data class ThemeSeedPalette(
    val primary: Long,
    val secondary: Long,
    val tertiary: Long,
    val neutral: Long
)

object ThemeSeedPaletteCodec {
    fun encodeForPreference(palette: ThemeSeedPalette, name: String): String {
        return "$name:${palette.primary}:${palette.secondary}:${palette.tertiary}:${palette.neutral}"
    }
}
