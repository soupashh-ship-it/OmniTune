/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.palette.graphics.Palette
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeMonochrome
import com.kyant.m3color.scheme.SchemeNeutral
import com.kyant.m3color.scheme.SchemeTonalSpot
import timber.log.Timber
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf


val DefaultThemeColor = Color(0xFFE47A82)

/**
 * Dynamic accent colors used with CompositionLocal to trigger recomposition.
 */
data class OmniDynamicAccents(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val glow: Color,
    val soft: Color,
)

/**
 * CompositionLocal that provides dynamic accent colors.
 * Using compositionLocalOf ensures all consumers recompose when colors change.
 */
val LocalOmniAccents = compositionLocalOf {
    OmniDynamicAccents(
        primary = DefaultThemeColor,
        secondary = Color(0xFFFF7A6F),
        tertiary = Color(0xFFFFA15C),
        glow = DefaultThemeColor.copy(alpha = 0.30f),
        soft = DefaultThemeColor.copy(alpha = 0.12f),
    )
}

/**
 * Full seed palette containing all four Material3 tonal palette seeds.
 */
data class ThemeSeedPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val neutral: Color,
)

/**
 * OmniTune's main theme composable.
 */
@Composable
fun OmniTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    seedPalette: ThemeSeedPalette? = null,
    useSystemFont: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val useSystemDynamicColor =
        dynamicColor && seedPalette == null && themeColor == DefaultThemeColor &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val appFontFamily = remember {
        if (useSystemFont) FontFamily.Default else AppFontFamily
    }
    val typography = remember(appFontFamily) {
        omniTypography(fontFamily = appFontFamily)
    }

    val appColorScheme = remember(seedPalette, themeColor, darkTheme) {
        if (seedPalette != null) {
            exactPaletteColorScheme(
                palette = seedPalette,
                isDark = darkTheme,
            )
        } else {
            m3DynamicColorScheme(
                seedPalette = null,
                keyColor = themeColor,
                isDark = darkTheme,
            )
        }
    }

    val baseColorScheme = if (useSystemDynamicColor) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        appColorScheme
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) baseColorScheme.pureBlack(true) else baseColorScheme
    }

    val animatedColorScheme = animateColorScheme(colorScheme)

    // Semantic design-system scheme: derives from the same resolved/accented palette as
    // MaterialTheme so dynamic artwork palettes flow through both systems identically.
    val omniAccentAnimationSpec = spring<Color>(stiffness = Spring.StiffnessLow)
    val omniAccent = animateColorAsState(
        targetValue = colorScheme.primary,
        animationSpec = omniAccentAnimationSpec,
        label = "omni-scheme-accent",
    ).value
    val omniScheme = if (darkTheme) {
        OmniScheme.dark(accent = omniAccent, pureBlack = pureBlack)
    } else {
        OmniScheme.light(accent = omniAccent)
    }

    Timber.tag("OmniTuneTheme").d(
        "themeColor in theme: ${Integer.toHexString(themeColor.toArgb())}, scheme.primary: ${Integer.toHexString(colorScheme.primary.toArgb())}",
    )

    OmniColors.updateFromTheme(
        primary = colorScheme.primary,
        secondary = colorScheme.secondary,
        tertiary = colorScheme.tertiary,
        pureBlack = darkTheme && pureBlack,
    )

    val dynamicAccents = remember {
        OmniDynamicAccents(
            primary = Color(0xFFE47A82),
            secondary = Color(0xFFFF9AA2),
            tertiary = Color(0xFFF99392),
            glow = Color(0xFFE47A82).copy(alpha = 0.30f),
            soft = Color(0xFFE47A82).copy(alpha = 0.12f),
        )
    }

    CompositionLocalProvider(
        LocalOmniAccents provides dynamicAccents,
        LocalOmniColors provides omniScheme,
    ) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = typography,
            content = content,
        )
    }
}

@Composable
private fun animateColorScheme(targetColorScheme: ColorScheme): ColorScheme {
    val animationSpec = spring<Color>(stiffness = Spring.StiffnessLow)
    return ColorScheme(
        primary = animateColorAsState(targetColorScheme.primary, animationSpec, label = "primary").value,
        onPrimary = animateColorAsState(targetColorScheme.onPrimary, animationSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(targetColorScheme.primaryContainer, animationSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(targetColorScheme.onPrimaryContainer, animationSpec, label = "onPrimaryContainer").value,
        primaryFixed = animateColorAsState(targetColorScheme.primaryFixed, animationSpec, label = "primaryFixed").value,
        primaryFixedDim = animateColorAsState(targetColorScheme.primaryFixedDim, animationSpec, label = "primaryFixedDim").value,
        onPrimaryFixed = animateColorAsState(targetColorScheme.onPrimaryFixed, animationSpec, label = "onPrimaryFixed").value,
        onPrimaryFixedVariant = animateColorAsState(targetColorScheme.onPrimaryFixedVariant, animationSpec, label = "onPrimaryFixedVariant").value,
        inversePrimary = animateColorAsState(targetColorScheme.inversePrimary, animationSpec, label = "inversePrimary").value,
        secondary = animateColorAsState(targetColorScheme.secondary, animationSpec, label = "secondary").value,
        onSecondary = animateColorAsState(targetColorScheme.onSecondary, animationSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(targetColorScheme.secondaryContainer, animationSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(targetColorScheme.onSecondaryContainer, animationSpec, label = "onSecondaryContainer").value,
        secondaryFixed = animateColorAsState(targetColorScheme.secondaryFixed, animationSpec, label = "secondaryFixed").value,
        secondaryFixedDim = animateColorAsState(targetColorScheme.secondaryFixedDim, animationSpec, label = "secondaryFixedDim").value,
        onSecondaryFixed = animateColorAsState(targetColorScheme.onSecondaryFixed, animationSpec, label = "onSecondaryFixed").value,
        onSecondaryFixedVariant = animateColorAsState(targetColorScheme.onSecondaryFixedVariant, animationSpec, label = "onSecondaryFixedVariant").value,
        tertiary = animateColorAsState(targetColorScheme.tertiary, animationSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(targetColorScheme.onTertiary, animationSpec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(targetColorScheme.tertiaryContainer, animationSpec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(targetColorScheme.onTertiaryContainer, animationSpec, label = "onTertiaryContainer").value,
        tertiaryFixed = animateColorAsState(targetColorScheme.tertiaryFixed, animationSpec, label = "tertiaryFixed").value,
        tertiaryFixedDim = animateColorAsState(targetColorScheme.tertiaryFixedDim, animationSpec, label = "tertiaryFixedDim").value,
        onTertiaryFixed = animateColorAsState(targetColorScheme.onTertiaryFixed, animationSpec, label = "onTertiaryFixed").value,
        onTertiaryFixedVariant = animateColorAsState(targetColorScheme.onTertiaryFixedVariant, animationSpec, label = "onTertiaryFixedVariant").value,
        background = animateColorAsState(targetColorScheme.background, animationSpec, label = "background").value,
        onBackground = animateColorAsState(targetColorScheme.onBackground, animationSpec, label = "onBackground").value,
        surface = animateColorAsState(targetColorScheme.surface, animationSpec, label = "surface").value,
        onSurface = animateColorAsState(targetColorScheme.onSurface, animationSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, animationSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, animationSpec, label = "onSurfaceVariant").value,
        surfaceTint = animateColorAsState(targetColorScheme.surfaceTint, animationSpec, label = "surfaceTint").value,
        inverseSurface = animateColorAsState(targetColorScheme.inverseSurface, animationSpec, label = "inverseSurface").value,
        inverseOnSurface = animateColorAsState(targetColorScheme.inverseOnSurface, animationSpec, label = "inverseOnSurface").value,
        error = animateColorAsState(targetColorScheme.error, animationSpec, label = "error").value,
        onError = animateColorAsState(targetColorScheme.onError, animationSpec, label = "onError").value,
        errorContainer = animateColorAsState(targetColorScheme.errorContainer, animationSpec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(targetColorScheme.onErrorContainer, animationSpec, label = "onErrorContainer").value,
        outline = animateColorAsState(targetColorScheme.outline, animationSpec, label = "outline").value,
        outlineVariant = animateColorAsState(targetColorScheme.outlineVariant, animationSpec, label = "outlineVariant").value,
        scrim = animateColorAsState(targetColorScheme.scrim, animationSpec, label = "scrim").value,
        surfaceBright = animateColorAsState(targetColorScheme.surfaceBright, animationSpec, label = "surfaceBright").value,
        surfaceDim = animateColorAsState(targetColorScheme.surfaceDim, animationSpec, label = "surfaceDim").value,
        surfaceContainer = animateColorAsState(targetColorScheme.surfaceContainer, animationSpec, label = "surfaceContainer").value,
        surfaceContainerLow = animateColorAsState(targetColorScheme.surfaceContainerLow, animationSpec, label = "surfaceContainerLow").value,
        surfaceContainerLowest = animateColorAsState(targetColorScheme.surfaceContainerLowest, animationSpec, label = "surfaceContainerLowest").value,
        surfaceContainerHigh = animateColorAsState(targetColorScheme.surfaceContainerHigh, animationSpec, label = "surfaceContainerHigh").value,
        surfaceContainerHighest = animateColorAsState(targetColorScheme.surfaceContainerHighest, animationSpec, label = "surfaceContainerHighest").value,
    )
}

private fun exactPaletteColorScheme(
    palette: ThemeSeedPalette,
    isDark: Boolean,
): ColorScheme {
    val primaryScheme = m3Scheme(palette.primary, isDark, 0.0)
    val secondaryScheme = m3Scheme(palette.secondary, isDark, 0.0)
    val tertiaryScheme = m3Scheme(palette.tertiary, isDark, 0.0)
    val neutralScheme = m3Scheme(palette.neutral, isDark, 0.0)

    return ColorScheme(
        primary = primaryScheme.primary.toComposeColor(),
        onPrimary = primaryScheme.onPrimary.toComposeColor(),
        primaryContainer = primaryScheme.primaryContainer.toComposeColor(),
        onPrimaryContainer = primaryScheme.onPrimaryContainer.toComposeColor(),
        primaryFixed = primaryScheme.primaryContainer.toComposeColor(),
        primaryFixedDim = primaryScheme.primary.toComposeColor(),
        onPrimaryFixed = primaryScheme.onPrimaryContainer.toComposeColor(),
        onPrimaryFixedVariant = primaryScheme.onPrimary.toComposeColor(),
        inversePrimary = primaryScheme.inversePrimary.toComposeColor(),
        secondary = secondaryScheme.primary.toComposeColor(),
        onSecondary = secondaryScheme.onPrimary.toComposeColor(),
        secondaryContainer = secondaryScheme.primaryContainer.toComposeColor(),
        onSecondaryContainer = secondaryScheme.onPrimaryContainer.toComposeColor(),
        secondaryFixed = secondaryScheme.primaryContainer.toComposeColor(),
        secondaryFixedDim = secondaryScheme.primary.toComposeColor(),
        onSecondaryFixed = secondaryScheme.onPrimaryContainer.toComposeColor(),
        onSecondaryFixedVariant = secondaryScheme.onPrimary.toComposeColor(),
        tertiary = tertiaryScheme.primary.toComposeColor(),
        onTertiary = tertiaryScheme.onPrimary.toComposeColor(),
        tertiaryContainer = tertiaryScheme.primaryContainer.toComposeColor(),
        onTertiaryContainer = tertiaryScheme.onPrimaryContainer.toComposeColor(),
        tertiaryFixed = tertiaryScheme.primaryContainer.toComposeColor(),
        tertiaryFixedDim = tertiaryScheme.primary.toComposeColor(),
        onTertiaryFixed = tertiaryScheme.onPrimaryContainer.toComposeColor(),
        onTertiaryFixedVariant = tertiaryScheme.onPrimary.toComposeColor(),
        background = neutralScheme.background.toComposeColor(),
        onBackground = neutralScheme.onBackground.toComposeColor(),
        surface = neutralScheme.surface.toComposeColor(),
        onSurface = neutralScheme.onSurface.toComposeColor(),
        surfaceVariant = neutralScheme.surfaceVariant.toComposeColor(),
        onSurfaceVariant = neutralScheme.onSurfaceVariant.toComposeColor(),
        inverseSurface = neutralScheme.inverseSurface.toComposeColor(),
        inverseOnSurface = neutralScheme.inverseOnSurface.toComposeColor(),
        surfaceBright = neutralScheme.surfaceBright.toComposeColor(),
        surfaceDim = neutralScheme.surfaceDim.toComposeColor(),
        surfaceContainer = neutralScheme.surfaceContainer.toComposeColor(),
        surfaceContainerLow = neutralScheme.surfaceContainerLow.toComposeColor(),
        surfaceContainerLowest = neutralScheme.surfaceContainerLowest.toComposeColor(),
        surfaceContainerHigh = neutralScheme.surfaceContainerHigh.toComposeColor(),
        surfaceContainerHighest = neutralScheme.surfaceContainerHighest.toComposeColor(),
        outline = neutralScheme.outline.toComposeColor(),
        outlineVariant = neutralScheme.outlineVariant.toComposeColor(),
        error = primaryScheme.error.toComposeColor(),
        onError = primaryScheme.onError.toComposeColor(),
        errorContainer = primaryScheme.errorContainer.toComposeColor(),
        onErrorContainer = primaryScheme.onErrorContainer.toComposeColor(),
        scrim = neutralScheme.scrim.toComposeColor(),
        surfaceTint = primaryScheme.surfaceTint.toComposeColor(),
    )
}

private fun m3DynamicColorScheme(
    seedPalette: ThemeSeedPalette?,
    keyColor: Color,
    isDark: Boolean,
    contrastLevel: Double = 0.0,
): ColorScheme {
    val primarySeed = seedPalette?.primary ?: keyColor
    val secondarySeed = seedPalette?.secondary ?: primarySeed
    val tertiarySeed = seedPalette?.tertiary ?: primarySeed
    val neutralSeed = seedPalette?.neutral ?: primarySeed

    val primaryScheme = m3Scheme(primarySeed, isDark, contrastLevel)
    val secondaryScheme = m3Scheme(secondarySeed, isDark, contrastLevel)
    val tertiaryScheme = m3Scheme(tertiarySeed, isDark, contrastLevel)
    val neutralScheme = m3Scheme(neutralSeed, isDark, contrastLevel)

    return ColorScheme(
        primary = primaryScheme.primary.toComposeColor(),
        onPrimary = primaryScheme.onPrimary.toComposeColor(),
        primaryContainer = primaryScheme.primaryContainer.toComposeColor(),
        onPrimaryContainer = primaryScheme.onPrimaryContainer.toComposeColor(),
        primaryFixed = primaryScheme.primaryContainer.toComposeColor(),
        primaryFixedDim = primaryScheme.primary.toComposeColor(),
        onPrimaryFixed = primaryScheme.onPrimaryContainer.toComposeColor(),
        onPrimaryFixedVariant = primaryScheme.onPrimary.toComposeColor(),
        inversePrimary = primaryScheme.inversePrimary.toComposeColor(),
        secondary = secondaryScheme.primary.toComposeColor(),
        onSecondary = secondaryScheme.onPrimary.toComposeColor(),
        secondaryContainer = secondaryScheme.primaryContainer.toComposeColor(),
        onSecondaryContainer = secondaryScheme.onPrimaryContainer.toComposeColor(),
        secondaryFixed = secondaryScheme.primaryContainer.toComposeColor(),
        secondaryFixedDim = secondaryScheme.primary.toComposeColor(),
        onSecondaryFixed = secondaryScheme.onPrimaryContainer.toComposeColor(),
        onSecondaryFixedVariant = secondaryScheme.onPrimary.toComposeColor(),
        tertiary = tertiaryScheme.primary.toComposeColor(),
        onTertiary = tertiaryScheme.onPrimary.toComposeColor(),
        tertiaryContainer = tertiaryScheme.primaryContainer.toComposeColor(),
        onTertiaryContainer = tertiaryScheme.onPrimaryContainer.toComposeColor(),
        tertiaryFixed = tertiaryScheme.primaryContainer.toComposeColor(),
        tertiaryFixedDim = tertiaryScheme.primary.toComposeColor(),
        onTertiaryFixed = tertiaryScheme.onPrimaryContainer.toComposeColor(),
        onTertiaryFixedVariant = tertiaryScheme.onPrimary.toComposeColor(),
        background = neutralScheme.background.toComposeColor(),
        onBackground = neutralScheme.onBackground.toComposeColor(),
        surface = neutralScheme.surface.toComposeColor(),
        onSurface = neutralScheme.onSurface.toComposeColor(),
        surfaceVariant = neutralScheme.surfaceVariant.toComposeColor(),
        onSurfaceVariant = neutralScheme.onSurfaceVariant.toComposeColor(),
        inverseSurface = neutralScheme.inverseSurface.toComposeColor(),
        inverseOnSurface = neutralScheme.inverseOnSurface.toComposeColor(),
        surfaceBright = neutralScheme.surfaceBright.toComposeColor(),
        surfaceDim = neutralScheme.surfaceDim.toComposeColor(),
        surfaceContainer = neutralScheme.surfaceContainer.toComposeColor(),
        surfaceContainerLow = neutralScheme.surfaceContainerLow.toComposeColor(),
        surfaceContainerLowest = neutralScheme.surfaceContainerLowest.toComposeColor(),
        surfaceContainerHigh = neutralScheme.surfaceContainerHigh.toComposeColor(),
        surfaceContainerHighest = neutralScheme.surfaceContainerHighest.toComposeColor(),
        outline = neutralScheme.outline.toComposeColor(),
        outlineVariant = neutralScheme.outlineVariant.toComposeColor(),
        error = primaryScheme.error.toComposeColor(),
        onError = primaryScheme.onError.toComposeColor(),
        errorContainer = primaryScheme.errorContainer.toComposeColor(),
        onErrorContainer = primaryScheme.onErrorContainer.toComposeColor(),
        scrim = neutralScheme.scrim.toComposeColor(),
        surfaceTint = primaryScheme.surfaceTint.toComposeColor(),
    )
}

private fun m3Scheme(seedColor: Color, isDark: Boolean, contrastLevel: Double) =
    Hct.fromInt(seedColor.toArgb()).let { hct ->
        when {
            hct.chroma < 4.0 -> SchemeMonochrome(hct, isDark, contrastLevel)
            hct.chroma < 12.0 -> SchemeNeutral(hct, isDark, contrastLevel)
            else -> SchemeTonalSpot(hct, isDark, contrastLevel)
        }
    }

private fun Int.toComposeColor(): Color = Color(this.toLong() and 0xFFFFFFFFL)

fun Bitmap.extractThemeColor(): Color {
    val palette = Palette.from(this)
        .maximumColorCount(16)
        .generate()

    val swatch =
        palette.vibrantSwatch
            ?: palette.dominantSwatch
            ?: palette.mutedSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.lightMutedSwatch
            ?: palette.darkMutedSwatch

    return swatch?.rgb?.toComposeColor() ?: DefaultThemeColor
}

/**
 * Boost saturation of an artwork color while keeping brightness controlled.
 */
fun Color.boostSaturation(factor: Float = 1.3f, minSaturation: Float = 0.55f): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    // Boost saturation
    hsv[1] = (hsv[1] * factor).coerceAtMost(1.0f)
    hsv[1] = hsv[1].coerceAtLeast(minSaturation)
    // Keep brightness in optimal range
    hsv[2] = (hsv[2] * 1.02f).coerceIn(0.32f, 0.88f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun Bitmap.extractGradientColors(): List<Color> {
    val palette = Palette.from(this)
        .maximumColorCount(48)
        .generate()

    val swatches = palette.swatches
        .filter { it.population > 0 }
        .sortedByDescending { it.population }

    if (swatches.isEmpty()) {
        return listOf(Color(0xFF595959), Color(0xFF0D0D0D))
    }

    val first = swatches.first()
    val firstHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(first.rgb, firstHsv)

    val second =
        swatches
            .drop(1)
            .maxByOrNull { candidate ->
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(candidate.rgb, hsv)
                val hueDiffRaw = kotlin.math.abs(hsv[0] - firstHsv[0])
                val hueDiff = kotlin.math.min(hueDiffRaw, 360f - hueDiffRaw) / 180f
                val satDiff = kotlin.math.abs(hsv[1] - firstHsv[1])
                val valueDiff = kotlin.math.abs(hsv[2] - firstHsv[2])
                hueDiff * 0.65f + satDiff * 0.2f + valueDiff * 0.15f
            }
            ?: first

    return listOf(first.rgb.toComposeColor(), second.rgb.toComposeColor())
        .sortedByDescending { it.luminance() }
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black,
    ) else this
