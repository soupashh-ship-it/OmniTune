/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Midnight Prism Glass color scheme — always dark-first
private val MidnightDarkScheme = darkColorScheme(
    primary = OmniColors.Primary,
    onPrimary = Color.White,
    primaryContainer = OmniColors.Primary.copy(alpha = 0.15f),
    onPrimaryContainer = OmniColors.PrimaryLight,

    secondary = OmniColors.Secondary,
    onSecondary = Color.Black,
    secondaryContainer = OmniColors.Secondary.copy(alpha = 0.15f),
    onSecondaryContainer = OmniColors.SecondaryLight,

    tertiary = OmniColors.Hot,
    onTertiary = Color.White,
    tertiaryContainer = OmniColors.Hot.copy(alpha = 0.15f),
    onTertiaryContainer = OmniColors.HotLight,

    background = OmniColors.Background,
    onBackground = OmniColors.TextPrimary,

    surface = OmniColors.Surface,
    onSurface = OmniColors.TextPrimary,

    surfaceVariant = OmniColors.SurfaceElevated,
    onSurfaceVariant = OmniColors.TextSecondary,

    surfaceTint = OmniColors.Primary,

    outline = OmniColors.GlassBorder,
    outlineVariant = OmniColors.GlassBorderLight,

    error = OmniColors.Error,
    onError = Color.White,
)

@Composable
fun OmniTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled — use our Midnight Prism Glass theme
    content: @Composable () -> Unit,
) {
    // Always use the dark Midnight Prism Glass scheme
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        dynamicDarkColorScheme(context)
    } else {
        MidnightDarkScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OmniTypography,
        content = content,
    )
}
