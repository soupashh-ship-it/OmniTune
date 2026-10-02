package com.omnitune.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ============================================================================
// SuvMusic Color Tokens
// ============================================================================

// Purple (DEFAULT)
val Purple10 = Color(0xFF1D0035)
val Purple20 = Color(0xFF2D0050)
val Purple30 = Color(0xFF42007A)
val Purple40 = Color(0xFF5A189A)
val Purple50 = Color(0xFF7B2CBF)
val Purple60 = Color(0xFF9D4EDD)
val Purple70 = Color(0xFFB970F2)
val Purple80 = Color(0xFFD194FF)
val Purple90 = Color(0xFFE8BFFF)

// Cyan
val Cyan10 = Color(0xFF001F26)
val Cyan20 = Color(0xFF003640)
val Cyan30 = Color(0xFF004D5C)
val Cyan40 = Color(0xFF00687A)
val Cyan70 = Color(0xFF00BDD6)
val Cyan80 = Color(0xFF4DD9F0)
val Cyan90 = Color(0xFFA5EFFF)

// Magenta
val Magenta10 = Color(0xFF2D0023)
val Magenta30 = Color(0xFF6A0050)
val Magenta70 = Color(0xFFF058B6)
val Magenta90 = Color(0xFFFFD7E9)

// Ocean Blue
val Blue10 = Color(0xFF001D32)
val Blue20 = Color(0xFF003355)
val Blue30 = Color(0xFF004B79)
val Blue40 = Color(0xFF00649F)
val Blue80 = Color(0xFF99CBFF)
val Blue90 = Color(0xFFD0E4FF)

val Teal10 = Color(0xFF002022)
val Teal20 = Color(0xFF00373A)
val Teal30 = Color(0xFF004F53)
val Teal40 = Color(0xFF006A6F)
val Teal80 = Color(0xFF4CD9E2)
val Teal90 = Color(0xFF9BF6FF)

// Sunset Orange
val Orange10 = Color(0xFF3E0500)
val Orange20 = Color(0xFF630E00)
val Orange30 = Color(0xFF8B1A00)
val Orange40 = Color(0xFFB52600)
val Orange80 = Color(0xFFFFB59D)
val Orange90 = Color(0xFFFFDBCF)

val Gold10 = Color(0xFF261900)
val Gold20 = Color(0xFF402D00)
val Gold30 = Color(0xFF5C4200)
val Gold40 = Color(0xFF7A5900)
val Gold80 = Color(0xFFF0C048)
val Gold90 = Color(0xFFFFDF9C)

// Nature Green
val Green10 = Color(0xFF00210B)
val Green20 = Color(0xFF003816)
val Green30 = Color(0xFF005223)
val Green40 = Color(0xFF006D31)
val Green80 = Color(0xFF8CF7A9)
val Green90 = Color(0xFFA9FBC2)

val Lime10 = Color(0xFF1A1D00)
val Lime20 = Color(0xFF2D3200)
val Lime30 = Color(0xFF434900)
val Lime40 = Color(0xFF5B6100)
val Lime80 = Color(0xFFC7CD7A)
val Lime90 = Color(0xFFE3E993)

// Love Pink
val Pink10 = Color(0xFF3E001D)
val Pink20 = Color(0xFF630031)
val Pink30 = Color(0xFF890046)
val Pink40 = Color(0xFFB0005C)
val Pink80 = Color(0xFFFFB0CD)
val Pink90 = Color(0xFFFFD8E4)

val Rose10 = Color(0xFF3F0010)
val Rose20 = Color(0xFF66001E)
val Rose30 = Color(0xFF8E002C)
val Rose40 = Color(0xFFB8003C)
val Rose80 = Color(0xFFFFB2B9)
val Rose90 = Color(0xFFFFDAD9)

// Neutrals
val Neutral10 = Color(0xFF1C1B1E)
val Neutral20 = Color(0xFF2E2C32)
val Neutral30 = Color(0xFF454248)
val Neutral80 = Color(0xFFC7C4C9)
val Neutral90 = Color(0xFFE3E1E5)
val Neutral99 = Color(0xFFFFFBFF)

val NeutralVar20 = Color(0xFF322F37)
val NeutralVar30 = Color(0xFF49454E)
val NeutralVar60 = Color(0xFF938F99)
val NeutralVar80 = Color(0xFFCAC4CF)
val NeutralVar90 = Color(0xFFE6E0EB)

// Error
val Error20 = Color(0xFF690005)
val Error30 = Color(0xFF93000A)
val Error40 = Color(0xFFBA1A1A)
val Error80 = Color(0xFFFFB4AB)
val Error90 = Color(0xFFFFDAD6)

// Base Backgrounds
val SurfaceDarkDefault = Color(0xFF121212)
val BackgroundDarkDefault = Color(0xFF0A0A0A)

// Dominant colors representation extracted from album art
data class DominantColors(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val onBackground: Color = Color.White
)

enum class SuvMusicPalette(val displayName: String) {
    DEFAULT("Default"),
    OCEAN("Ocean"),
    SUNSET("Sunset"),
    NATURE("Nature"),
    LOVE("Love");

    companion object {
        fun fromName(name: String?): SuvMusicPalette {
            if (name.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
        }
    }
}

data class PaletteColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val surface: Color,
    val background: Color,
    val primaryHex: String,
    val secondaryHex: String,
    val tertiaryHex: String,
    val surfaceHex: String,
    val backgroundHex: String
)

val PaletteDefault = PaletteColors(
    primary = Color(0xFF9C27B0),
    secondary = Color(0xFF00BCD4),
    tertiary = Color(0xFFE91E63),
    surface = Color(0xFF121212),
    background = Color(0xFF0A0A0A),
    primaryHex = "#9C27B0",
    secondaryHex = "#00BCD4",
    tertiaryHex = "#E91E63",
    surfaceHex = "#121212",
    backgroundHex = "#0A0A0A"
)

val PaletteOcean = PaletteColors(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF009688),
    tertiary = Color(0xFF7B1FA2),
    surface = Color(0xFF101720),
    background = Color(0xFF080E18),
    primaryHex = "#1976D2",
    secondaryHex = "#009688",
    tertiaryHex = "#7B1FA2",
    surfaceHex = "#101720",
    backgroundHex = "#080E18"
)

val PaletteSunset = PaletteColors(
    primary = Color(0xFFFF5722),
    secondary = Color(0xFFFFC107),
    tertiary = Color(0xFFE91E63),
    surface = Color(0xFF1C1310),
    background = Color(0xFF120A08),
    primaryHex = "#FF5722",
    secondaryHex = "#FFC107",
    tertiaryHex = "#E91E63",
    surfaceHex = "#1C1310",
    backgroundHex = "#120A08"
)

val PaletteNature = PaletteColors(
    primary = Color(0xFF388E3C),
    secondary = Color(0xFF8BC34A),
    tertiary = Color(0xFF00796B),
    surface = Color(0xFF101812),
    background = Color(0xFF08100A),
    primaryHex = "#388E3C",
    secondaryHex = "#8BC34A",
    tertiaryHex = "#00796B",
    surfaceHex = "#101812",
    backgroundHex = "#08100A"
)

val PaletteLove = PaletteColors(
    primary = Color(0xFFE91E63),
    secondary = Color(0xFFF48FB1),
    tertiary = Color(0xFFFF5722),
    surface = Color(0xFF1A0F14),
    background = Color(0xFF12080D),
    primaryHex = "#E91E63",
    secondaryHex = "#F48FB1",
    tertiaryHex = "#FF5722",
    surfaceHex = "#1A0F14",
    backgroundHex = "#12080D"
)

fun getPaletteColors(palette: SuvMusicPalette): PaletteColors = when (palette) {
    SuvMusicPalette.DEFAULT -> PaletteDefault
    SuvMusicPalette.OCEAN -> PaletteOcean
    SuvMusicPalette.SUNSET -> PaletteSunset
    SuvMusicPalette.NATURE -> PaletteNature
    SuvMusicPalette.LOVE -> PaletteLove
}

fun buildColorScheme(
    palette: SuvMusicPalette,
    darkTheme: Boolean = true,
    pureBlack: Boolean = false
): ColorScheme {
    val pc = getPaletteColors(palette)
    val bg = if (pureBlack && darkTheme) Color.Black else if (darkTheme) pc.background else Neutral99
    val sf = if (pureBlack && darkTheme) Color.Black else if (darkTheme) pc.surface else Neutral99

    return if (darkTheme) {
        darkColorScheme(
            primary = pc.primary,
            onPrimary = Color.White,
            primaryContainer = pc.primary.copy(alpha = 0.25f),
            onPrimaryContainer = Color.White,
            secondary = pc.secondary,
            onSecondary = Color.Black,
            secondaryContainer = pc.secondary.copy(alpha = 0.25f),
            onSecondaryContainer = Color.White,
            tertiary = pc.tertiary,
            onTertiary = Color.White,
            tertiaryContainer = pc.tertiary.copy(alpha = 0.25f),
            onTertiaryContainer = Color.White,
            background = bg,
            onBackground = Neutral90,
            surface = sf,
            onSurface = Neutral90,
            surfaceVariant = if (pureBlack) Color(0xFF111111) else NeutralVar30,
            onSurfaceVariant = NeutralVar80,
            error = Error80,
            onError = Error20,
            outline = NeutralVar60,
            outlineVariant = NeutralVar30,
            scrim = Color.Black
        )
    } else {
        lightColorScheme(
            primary = pc.primary,
            onPrimary = Color.White,
            primaryContainer = pc.primary.copy(alpha = 0.15f),
            onPrimaryContainer = Neutral10,
            secondary = pc.secondary,
            onSecondary = Color.White,
            secondaryContainer = pc.secondary.copy(alpha = 0.15f),
            onSecondaryContainer = Neutral10,
            tertiary = pc.tertiary,
            onTertiary = Color.White,
            tertiaryContainer = pc.tertiary.copy(alpha = 0.15f),
            onTertiaryContainer = Neutral10,
            background = bg,
            onBackground = Neutral10,
            surface = sf,
            onSurface = Neutral10,
            surfaceVariant = NeutralVar90,
            onSurfaceVariant = NeutralVar30,
            error = Error40,
            onError = Color.White,
            outline = NeutralVar60,
            outlineVariant = NeutralVar80,
            scrim = Color.Black
        )
    }
}
