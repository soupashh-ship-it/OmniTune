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

    fun decodeFromPreference(value: String): ThemeSeedPalette? {
        val parts = value.split(":")
        if (parts.size == 5) {
            return ThemeSeedPalette(
                primary = parts[1].toLongOrNull() ?: return null,
                secondary = parts[2].toLongOrNull() ?: return null,
                tertiary = parts[3].toLongOrNull() ?: return null,
                neutral = parts[4].toLongOrNull() ?: return null
            )
        }
        return null
    }

    fun extractNameFromPreference(value: String): String {
        val parts = value.split(":")
        if (parts.size == 5) return parts[0]
        return ""
    }

    fun encodeAsJson(palette: ThemeSeedPalette, name: String): String {
        return "{\"name\":\"$name\",\"primary\":${palette.primary},\"secondary\":${palette.secondary},\"tertiary\":${palette.tertiary},\"neutral\":${palette.neutral}}"
    }

    fun decodeFromJson(json: String): ThemeSeedPalette? {
        try {
            val primary = Regex("\"primary\"\\s*:\\s*(-?\\d+)").find(json)?.groupValues?.get(1)?.toLong() ?: return null
            val secondary = Regex("\"secondary\"\\s*:\\s*(-?\\d+)").find(json)?.groupValues?.get(1)?.toLong() ?: return null
            val tertiary = Regex("\"tertiary\"\\s*:\\s*(-?\\d+)").find(json)?.groupValues?.get(1)?.toLong() ?: return null
            val neutral = Regex("\"neutral\"\\s*:\\s*(-?\\d+)").find(json)?.groupValues?.get(1)?.toLong() ?: return null
            return ThemeSeedPalette(primary, secondary, tertiary, neutral)
        } catch (e: Exception) {
            return null
        }
    }

    fun extractNameFromJsonOrNull(json: String): String? {
        return Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
    }
}
