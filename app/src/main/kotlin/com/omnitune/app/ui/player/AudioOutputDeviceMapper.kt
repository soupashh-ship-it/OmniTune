/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player

import android.media.AudioDeviceInfo
import android.os.Build

internal data class AudioOutputRoute(
    val id: Int,
    val productName: String?,
    val type: Int,
    val address: String? = null,
)

internal object AudioOutputDeviceMapper {
    const val SYSTEM_DEFAULT_ID = "system_default"

    fun build(
        routes: List<AudioOutputRoute>,
        selectedRouteId: Int?,
    ): List<OutputDevice> {
        val uniqueRoutes = routes
            .filter { it.id > 0 }
            .distinctBy { it.id }
        val effectiveSelectedRouteId = selectedRouteId?.takeIf { selected ->
            uniqueRoutes.any { it.id == selected }
        }

        val systemDefault = OutputDevice(
            id = SYSTEM_DEFAULT_ID,
            name = "System default",
            type = DeviceType.PHONE,
            isSelected = effectiveSelectedRouteId == null,
            routeId = null,
            subtitle = "Let Android choose the active media route",
        )

        return listOf(systemDefault) + uniqueRoutes
            .sortedWith(compareBy<AudioOutputRoute> { sortRank(it.type) }.thenBy { displayName(it) })
            .map { route ->
                OutputDevice(
                    id = "audio_output_${route.id}",
                    name = displayName(route),
                    type = deviceType(route.type),
                    isSelected = route.id == effectiveSelectedRouteId,
                    routeId = route.id,
                    subtitle = routeSubtitle(route.type),
                )
            }
    }

    fun descriptorFrom(device: AudioDeviceInfo): AudioOutputRoute =
        AudioOutputRoute(
            id = device.id,
            productName = device.productName?.toString(),
            type = device.type,
            address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) device.address else null,
        )

    fun deviceType(type: Int): DeviceType {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
        ) {
            return DeviceType.BLUETOOTH
        }

        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
            -> DeviceType.PHONE
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            -> DeviceType.HEADPHONES
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_HEARING_AID,
            -> DeviceType.BLUETOOTH
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
            AudioDeviceInfo.TYPE_DOCK,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            -> DeviceType.SPEAKER
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> DeviceType.CAST
            else -> DeviceType.UNKNOWN
        }
    }

    private fun displayName(route: AudioOutputRoute): String =
        route.productName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: fallbackName(route.type)

    private fun fallbackName(type: Int): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when (type) {
                AudioDeviceInfo.TYPE_BLE_HEADSET -> return "Bluetooth headset"
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> return "Bluetooth speaker"
            }
        }

        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone speaker"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Phone earpiece"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth audio"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth headset"
            AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing aid"
            AudioDeviceInfo.TYPE_HDMI -> "HDMI audio"
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
            AudioDeviceInfo.TYPE_DOCK,
            -> "External speaker"
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> "Remote audio"
            else -> "Audio output"
        }
    }

    private fun routeSubtitle(type: Int): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when (type) {
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                -> return "Bluetooth route"
            }
        }

        return when (deviceType(type)) {
            DeviceType.PHONE -> "Built-in route"
            DeviceType.BLUETOOTH -> "Bluetooth route"
            DeviceType.HEADPHONES -> "Wired or USB route"
            DeviceType.SPEAKER -> "External audio route"
            DeviceType.CAST -> "Remote route"
            DeviceType.UNKNOWN -> "Android audio route"
        }
    }

    private fun sortRank(type: Int): Int =
        when (deviceType(type)) {
            DeviceType.PHONE -> 0
            DeviceType.HEADPHONES -> 1
            DeviceType.BLUETOOTH -> 2
            DeviceType.SPEAKER -> 3
            DeviceType.CAST -> 4
            DeviceType.UNKNOWN -> 5
        }
}
