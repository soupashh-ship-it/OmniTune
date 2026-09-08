package com.omnitune.app.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.FoldingFeature

/**
 * CompositionLocal for [DeviceFormFactor].
 */
val LocalDeviceFormFactor = compositionLocalOf<DeviceFormFactor> {
    DeviceFormFactor.Phone
}

/**
 * Remembers the current [DeviceFormFactor] based on window metrics and folding features.
 */
@Composable
fun rememberDeviceFormFactor(
    windowSizeClass: WindowSizeClass = WindowSizeClass.compute(
        LocalConfiguration.current.screenWidthDp.toFloat(),
        LocalConfiguration.current.screenHeightDp.toFloat()
    ),
    foldingFeature: FoldingFeature? = null,
): DeviceFormFactor {
    val context = LocalContext.current
    val detector = remember(context) { DeviceFormFactorDetector(context) }

    val configuration = LocalConfiguration.current
    return remember(configuration, windowSizeClass, foldingFeature) {
        detector.detect(windowSizeClass, foldingFeature)
    }
}
