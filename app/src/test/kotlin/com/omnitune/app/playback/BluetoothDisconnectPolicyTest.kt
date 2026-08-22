package com.omnitune.app.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothDisconnectPolicyTest {
    @Test
    fun `pauses active playback only after its last bluetooth output is removed`() {
        assertTrue(
            shouldPauseForBluetoothDisconnect(
                isPlaying = true,
                removedBluetoothOutput = true,
                hasRemainingBluetoothOutput = false,
            ),
        )
        assertFalse(
            shouldPauseForBluetoothDisconnect(
                isPlaying = false,
                removedBluetoothOutput = true,
                hasRemainingBluetoothOutput = false,
            ),
        )
        assertFalse(
            shouldPauseForBluetoothDisconnect(
                isPlaying = true,
                removedBluetoothOutput = false,
                hasRemainingBluetoothOutput = false,
            ),
        )
        assertFalse(
            shouldPauseForBluetoothDisconnect(
                isPlaying = true,
                removedBluetoothOutput = true,
                hasRemainingBluetoothOutput = true,
            ),
        )
    }
}
