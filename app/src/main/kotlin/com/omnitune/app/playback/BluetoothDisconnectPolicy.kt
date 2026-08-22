package com.omnitune.app.playback

/**
 * Decides whether a Bluetooth-output removal should pause playback.
 *
 * Do not pause for a disconnected non-audio peripheral or while another Bluetooth
 * output is still present (for example, an A2DP-to-LE handover).
 */
internal fun shouldPauseForBluetoothDisconnect(
    isPlaying: Boolean,
    removedBluetoothOutput: Boolean,
    hasRemainingBluetoothOutput: Boolean,
): Boolean = isPlaying && removedBluetoothOutput && !hasRemainingBluetoothOutput
