/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.component.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.omnitune.app.constants.GlassNavigationBarKey
import com.omnitune.app.constants.LiquidGlassEnabledKey
import com.omnitune.app.constants.NavBarBlurKey
import com.omnitune.app.utils.rememberPreference

/**
 * App-wide Liquid Glass configuration.
 */
data class LiquidGlassConfig(
    val enabled: Boolean = false,
    val blur: Float = 60f,
    val intensity: Float = 1f,
    val isDarkTheme: Boolean = true
)

@Composable
fun rememberLiquidGlassConfig(): LiquidGlassConfig {
    val glassEnabled by rememberPreference(LiquidGlassEnabledKey, true)
    val navBarGlass by rememberPreference(GlassNavigationBarKey, false)
    val blur by rememberPreference(NavBarBlurKey, 60f)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return LiquidGlassConfig(
        enabled = glassEnabled || navBarGlass,
        blur = blur,
        intensity = 1f,
        isDarkTheme = isDark
    )
}

/**
 * A container that renders a frosted LiquidGlassSurface when glass is enabled,
 * and falls back to a normal Surface otherwise.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = Color.Unspecified,
    drawShadow: Boolean = true,
    drawRim: Boolean = true,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = rememberLiquidGlassConfig()
    if (glass.enabled) {
        LiquidGlassSurface(
            modifier = modifier,
            shape = shape,
            blurAmount = glass.blur,
            intensity = glass.intensity,
            tint = tint,
            isDarkTheme = glass.isDarkTheme,
            drawShadow = drawShadow,
            drawRim = drawRim,
            content = content
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = fallbackColor,
            contentColor = contentColor
        ) {
            Box(content = content)
        }
    }
}

/**
 * A ModalBottomSheet that frosts into glass when Liquid Glass is enabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    fallbackContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = rememberLiquidGlassConfig()
    if (!glass.enabled) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
            shape = shape,
            containerColor = fallbackContainerColor,
            contentColor = contentColor,
            content = content
        )
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LiquidGlassSurface(
                modifier = Modifier.matchParentSize(),
                shape = shape,
                blurAmount = glass.blur,
                intensity = glass.intensity,
                isDarkTheme = glass.isDarkTheme,
                drawShadow = false
            ) {}
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BottomSheetDefaults.DragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
                    content()
                }
            }
        }
    }
}
