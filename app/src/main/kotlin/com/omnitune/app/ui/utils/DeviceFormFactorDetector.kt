package com.omnitune.app.ui.utils

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.FoldingFeature
import com.omnitune.app.utils.TvUtils

/**
 * Detector for device form factor based on multiple signals.
 */
@Stable
class DeviceFormFactorDetector(
    private val context: Context,
) {
    /**
     * Primary detection — combines 4 signals:
     *  1. Smallest-width resource qualifier (sw<N>dp)
     *  2. Physical screen diagonal in inches
     *  3. WindowSizeClass width
     *  4. TV mode check
     */
    fun detect(
        windowSizeClass: WindowSizeClass,
        foldingFeature: FoldingFeature? = null,
    ): DeviceFormFactor {
        // Signal 0: TV Check
        if (TvUtils.isTv(context)) {
            return DeviceFormFactor.TV
        }

        // Signal 1: foldable first
        if (foldingFeature != null) {
            val posture = when {
                foldingFeature.state == FoldingFeature.State.HALF_OPENED -> FoldablePosture.HalfOpen
                foldingFeature.state == FoldingFeature.State.FLAT -> FoldablePosture.Flat
                else -> FoldablePosture.Unknown
            }
            return DeviceFormFactor.Foldable(posture)
        }

        // Signal 2: smallest-width
        val smallestWidthDp = context.resources.configuration.smallestScreenWidthDp

        // Signal 3: physical diagonal
        val diagonalInches = physicalDiagonalInches()

        return when {
            (smallestWidthDp >= TABLET_SW_DP && diagonalInches >= TABLET_MIN_DIAGONAL_INCHES) ||
            smallestWidthDp >= TABLET_LARGE_SW_DP ->
                DeviceFormFactor.Tablet

            smallestWidthDp >= LARGE_PHONE_SW_DP || diagonalInches >= LARGE_PHONE_MIN_DIAGONAL_INCHES ->
                DeviceFormFactor.LargePhone

            else -> DeviceFormFactor.Phone
        }
    }

    private fun physicalDiagonalInches(): Float {
        val dm = context.resources.displayMetrics
        val widthInches = dm.widthPixels / dm.xdpi
        val heightInches = dm.heightPixels / dm.ydpi
        return Math.sqrt(
            (widthInches * widthInches + heightInches * heightInches).toDouble()
        ).toFloat()
    }

    companion object {
        private const val TABLET_SW_DP = 600
        private const val TABLET_LARGE_SW_DP = 720
        private const val TABLET_MIN_DIAGONAL_INCHES = 7.0f
        private const val LARGE_PHONE_SW_DP = 480
        private const val LARGE_PHONE_MIN_DIAGONAL_INCHES = 6.4f
    }
}
