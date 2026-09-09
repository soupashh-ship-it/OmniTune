/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.smallestScreenWidthDp >= 600
}

@Composable
fun isLargeTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.smallestScreenWidthDp >= 840
}

sealed class WindowSize {
    data object Compact : WindowSize()
    data object Medium : WindowSize()
    data object Expanded : WindowSize()
}

@Composable
fun rememberWindowSize(): WindowSize {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    return remember(screenWidthDp) {
        when {
            screenWidthDp >= 840.dp -> WindowSize.Expanded
            screenWidthDp >= 600.dp -> WindowSize.Medium
            else -> WindowSize.Compact
        }
    }
}
