package com.omnitune.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DisplayModeSelectorTest {
    @Test
    fun highestRefreshRateMode_prefersCurrentResolution() {
        val current = DisplayModeCandidate(modeId = 1, width = 1080, height = 2400, refreshRate = 60f)
        val selected = DisplayModeSelector.highestRefreshRateMode(
            currentMode = current,
            supportedModes = listOf(
                current,
                DisplayModeCandidate(modeId = 2, width = 1080, height = 2400, refreshRate = 120f),
                DisplayModeCandidate(modeId = 3, width = 1440, height = 3200, refreshRate = 90f),
            )
        )

        assertEquals(2, selected?.modeId)
    }

    @Test
    fun highestRefreshRateMode_fallsBackWhenCurrentResolutionMissing() {
        val selected = DisplayModeSelector.highestRefreshRateMode(
            currentMode = DisplayModeCandidate(modeId = 1, width = 1080, height = 2400, refreshRate = 60f),
            supportedModes = listOf(
                DisplayModeCandidate(modeId = 4, width = 720, height = 1600, refreshRate = 90f),
                DisplayModeCandidate(modeId = 5, width = 720, height = 1600, refreshRate = 120f),
            )
        )

        assertEquals(5, selected?.modeId)
    }

    @Test
    fun highestRefreshRateMode_ignoresInvalidModes() {
        val selected = DisplayModeSelector.highestRefreshRateMode(
            currentMode = null,
            supportedModes = listOf(
                DisplayModeCandidate(modeId = 0, width = 1080, height = 2400, refreshRate = 144f),
                DisplayModeCandidate(modeId = 6, width = 1080, height = 2400, refreshRate = 90f),
            )
        )

        assertEquals(6, selected?.modeId)
    }

    @Test
    fun highestRefreshRateMode_returnsNullForNoValidModes() {
        val selected = DisplayModeSelector.highestRefreshRateMode(
            currentMode = null,
            supportedModes = listOf(
                DisplayModeCandidate(modeId = 0, width = 0, height = 0, refreshRate = 0f),
            )
        )

        assertNull(selected)
    }
}
