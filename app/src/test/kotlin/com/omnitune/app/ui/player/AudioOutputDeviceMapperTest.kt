package com.omnitune.app.ui.player

import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioOutputDeviceMapperTest {
    @Test
    fun `system default is selected when no explicit route is pinned`() {
        val devices = AudioOutputDeviceMapper.build(
            routes = listOf(route(7, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, "USB-C headphones")),
            selectedRouteId = null,
        )

        assertEquals(AudioOutputDeviceMapper.SYSTEM_DEFAULT_ID, devices.first().id)
        assertTrue(devices.first().isSelected)
        assertEquals(7, devices.single { it.routeId == 7 }.routeId)
    }

    @Test
    fun `audio device types map to donor device icons`() {
        val devices = AudioOutputDeviceMapper.build(
            routes = listOf(
                route(1, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, ""),
                route(2, AudioDeviceInfo.TYPE_WIRED_HEADSET, ""),
                route(3, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "Car audio"),
                route(4, AudioDeviceInfo.TYPE_HDMI, ""),
            ),
            selectedRouteId = 3,
        )

        assertEquals(DeviceType.PHONE, devices.single { it.routeId == 1 }.type)
        assertEquals(DeviceType.HEADPHONES, devices.single { it.routeId == 2 }.type)
        assertEquals(DeviceType.BLUETOOTH, devices.single { it.routeId == 3 }.type)
        assertEquals(DeviceType.SPEAKER, devices.single { it.routeId == 4 }.type)
        assertTrue(devices.single { it.routeId == 3 }.isSelected)
    }

    @Test
    fun `stale preferred route falls back to system default`() {
        val devices = AudioOutputDeviceMapper.build(
            routes = listOf(route(1, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "Speaker")),
            selectedRouteId = 99,
        )

        assertTrue(devices.first { it.id == AudioOutputDeviceMapper.SYSTEM_DEFAULT_ID }.isSelected)
        assertTrue(devices.none { it.routeId == 99 })
    }

    @Test
    fun `duplicate android route ids are collapsed`() {
        val devices = AudioOutputDeviceMapper.build(
            routes = listOf(
                route(2, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "Headphones"),
                route(2, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "Headphones duplicate"),
            ),
            selectedRouteId = 2,
        )

        assertEquals(1, devices.count { it.routeId == 2 })
    }

    private fun route(id: Int, type: Int, name: String): AudioOutputRoute =
        AudioOutputRoute(
            id = id,
            productName = name.ifBlank { null },
            type = type,
        )
}
