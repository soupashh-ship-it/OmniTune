/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.datastore.preferences.core.Preferences
import com.omnitune.app.constants.AutoStartOnBluetoothKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Registers Bluetooth audio listeners and applies the auto-start / pause-on-disconnect
 * policies (see [shouldPauseForBluetoothDisconnect]) when Bluetooth outputs appear or vanish.
 *
 * Extracted from MusicService as part of the playback coordinator decomposition. Playback
 * actions are injected as callbacks so this class stays free of player/queue coupling.
 */
class BluetoothAudioHandler(
    private val context: Context,
    private val preferences: Flow<Preferences>,
    private val scope: CoroutineScope,
    private val isPlayingProvider: () -> Boolean,
    private val onAutoStartPlayback: () -> Boolean,
    private val onPauseForDisconnect: () -> Unit,
    private val onNotificationFallback: (reason: String, force: Boolean) -> Unit,
) {
    private var receiver: BroadcastReceiver? = null
    private var deviceCallback: AudioDeviceCallback? = null

    fun start() {
        stop()

        val filter = IntentFilter().apply {
            addAction(android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(
                            android.bluetooth.BluetoothProfile.EXTRA_STATE,
                            android.bluetooth.BluetoothProfile.STATE_DISCONNECTED,
                        )
                        when (state) {
                            android.bluetooth.BluetoothProfile.STATE_CONNECTED -> onConnected()
                            android.bluetooth.BluetoothProfile.STATE_DISCONNECTED -> onDisconnected()
                        }
                    }
                    android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED -> onConnected()
                    android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED -> onDisconnected()
                }
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                if (addedDevices.any { it.isBluetoothOutput() }) {
                    onConnected()
                }
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                if (removedDevices.any { it.isBluetoothOutput() }) {
                    onDisconnected()
                }
            }
        }.also { callback ->
            context.getSystemService(AudioManager::class.java)?.registerAudioDeviceCallback(callback, null)
        }
    }

    fun stop() {
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
        deviceCallback?.let { callback ->
            context.getSystemService(AudioManager::class.java)?.unregisterAudioDeviceCallback(callback)
        }
        deviceCallback = null
    }

    private fun onConnected() {
        scope.launch {
            val autoStart = preferences.first()[AutoStartOnBluetoothKey] ?: false
            if (!autoStart) {
                return@launch
            }
            val started = onAutoStartPlayback()
            if (!started) {
                return@launch
            }
            onNotificationFallback("bluetooth-connect", true)
        }
    }

    private fun onDisconnected() {
        scope.launch {
            val audioManager = context.getSystemService(AudioManager::class.java)
            val hasRemainingBluetoothOutput = audioManager
                ?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                ?.any { it.isBluetoothOutput() }
                ?: false
            if (
                shouldPauseForBluetoothDisconnect(
                    isPlaying = isPlayingProvider(),
                    removedBluetoothOutput = true,
                    hasRemainingBluetoothOutput = hasRemainingBluetoothOutput,
                )
            ) {
                onPauseForDisconnect()
                onNotificationFallback("bluetooth-disconnect", true)
            }
        }
    }

    companion object {
        internal fun AudioDeviceInfo.isBluetoothOutput(): Boolean {
            return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
        }
    }
}
