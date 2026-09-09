package com.omnitune.app.utils

data class DisplayModeCandidate(
    val modeId: Int,
    val width: Int,
    val height: Int,
    val refreshRate: Float,
)

object DisplayModeSelector {
    fun highestRefreshRateMode(
        currentMode: DisplayModeCandidate?,
        supportedModes: List<DisplayModeCandidate>,
    ): DisplayModeCandidate? {
        val validModes = supportedModes.filter { mode ->
            mode.modeId > 0 && mode.width > 0 && mode.height > 0 && mode.refreshRate > 0f
        }
        if (validModes.isEmpty()) return null

        val currentSizeModes = currentMode?.let { current ->
            validModes.filter { it.width == current.width && it.height == current.height }
        }.orEmpty()

        val candidateModes = currentSizeModes.ifEmpty { validModes }
        return candidateModes.maxWithOrNull(
            compareBy<DisplayModeCandidate> { it.refreshRate }
                .thenBy { it.width.toLong() * it.height.toLong() }
                .thenBy { it.modeId }
        )
    }
}
