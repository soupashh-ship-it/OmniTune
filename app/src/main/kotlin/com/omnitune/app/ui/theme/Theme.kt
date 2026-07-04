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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private val MidnightDarkScheme = darkColorScheme(
    primary = OmniColors.OmniAccentPrimary,
    onPrimary = OmniColors.OmniAccentOnPrimary,
    primaryContainer = OmniColors.OmniAccentPrimary.copy(alpha = 0.18f),
    onPrimaryContainer = OmniColors.PrimaryLight,

    secondary = OmniColors.OmniAccentSecondary,
    onSecondary = OmniColors.OmniAccentOnPrimary,
    secondaryContainer = OmniColors.OmniAccentSecondary.copy(alpha = 0.16f),
    onSecondaryContainer = OmniColors.SecondaryLight,

    tertiary = OmniColors.Hot,
    onTertiary = OmniColors.TextPrimary,
    tertiaryContainer = OmniColors.Hot.copy(alpha = 0.15f),
    onTertiaryContainer = OmniColors.HotLight,

    background = OmniColors.OmniBackgroundBase,
    onBackground = OmniColors.TextPrimary,

    surface = OmniColors.Surface,
    onSurface = OmniColors.TextPrimary,

    surfaceVariant = OmniColors.OmniBackgroundElevated,
    onSurfaceVariant = OmniColors.TextSecondary,

    surfaceTint = OmniColors.OmniAccentPrimary,

    outline = OmniColors.OmniGlassBorderStrong,
    outlineVariant = OmniColors.OmniGlassBorderSubtle,

    error = OmniColors.Error,
    onError = OmniColors.TextPrimary,
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun OmniTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        dynamicDarkColorScheme(context)
    } else {
        MidnightDarkScheme
    }

    val typography = remember { omniTypography(fontFamily = InterFontFamily) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}
